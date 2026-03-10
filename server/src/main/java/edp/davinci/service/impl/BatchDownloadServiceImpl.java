package edp.davinci.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.edu.elp.common.exception.ELPBizException;
import com.iflytek.edu.elp.common.util.JSONUtils;
import edp.core.consts.Consts;
import edp.core.utils.CollectionUtils;
import edp.davinci.core.enums.ActionEnum;
import edp.davinci.core.enums.DownloadTaskStatus;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.core.enums.SqlOperatorEnum;
import edp.davinci.core.model.Criterion;
import edp.davinci.core.model.SqlFilter;
import edp.davinci.dao.DashboardMapper;
import edp.davinci.dao.DownloadRecordMapper;
import edp.davinci.dao.WidgetContainerMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewWithSourceBaseInfo;
import edp.davinci.model.DashBoardConfigFilter;
import edp.davinci.model.DownloadRecord;
import edp.davinci.model.DownloadRecordBaseInfo;
import edp.davinci.model.RelatedView;
import edp.davinci.model.User;
import edp.davinci.service.BatchDownloadService;
import edp.davinci.service.DashboardDownloadTemplateService;
import edp.davinci.service.ViewService;
import edp.davinci.service.excel.DashBoardConfig;
import edp.davinci.service.excel.DownloadConfig;
import edp.davinci.service.excel.ExecutorUtil;
import edp.davinci.service.excel.MsgWrapper;
import edp.davinci.service.excel.WidgetContext;
import edp.davinci.service.excel.WorkBookContext;
import edp.davinci.service.excel.WorkbookBatchTaskWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author wghu
 */
@Service
@Slf4j
public class BatchDownloadServiceImpl extends DownloadCommonService implements BatchDownloadService {


    @Autowired
    private DashboardDownloadTemplateService dashboardDownloadTemplateService;

    @Autowired
    private ViewService viewService;

    @Autowired
    protected WidgetMapper widgetMapper;

    @Autowired
    protected WidgetContainerMapper widgetContainerMapper;

    @Autowired
    protected DashboardMapper dashboardMapper;

    @Autowired
    private DownloadRecordMapper downloadRecordMapper;

    @Value("${source.result-limit:1000000}")
    protected int resultLimit;

    private Integer batchDownloadSize = 1000;

    @Override
    public List<DownloadRecord> queryDownloadRecordPage(Long userId) {
        return null;
    }

    @Override
    public DownloadRecord downloadById(Long id, String token) {
        return null;
    }

    @Override
    public Boolean submit(DownloadType type, Long id, User user, List<DownloadViewExecuteParam> params) {
        DownloadRecord record = null;
        try {
            String downloadFileName = getDownloadFileName(type, id);
            record = new DownloadRecord();
            record.setName(downloadFileName);
            record.setUserId(user.getId());
            record.setCreateTime(new Date());
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            int insert = downloadRecordMapper.insert(record);

            record.setStatus(DownloadTaskStatus.FAILED.getStatus());


            submitTask(type, id, user, params, record, ActionEnum.DOWNLOAD, downloadFileName);

            log.info("Download task submit: {}", JSONUtils.toJSONString(record));
        } catch (Exception e) {
            log.error("submit download task error,e=", e);
            return false;
        }
        return true;
    }

    @Override
    public Future<Object> submitTask(DownloadType type, Long id, User user, List<DownloadViewExecuteParam> params, DownloadRecordBaseInfo record, ActionEnum actionEnum, String downloadFileName) throws IllegalAccessException, InvocationTargetException {
        log.info("submitTask task type:{} params: {}", type.name(), JSONUtils.toJSONString(params));
        MsgWrapper wrapper = new MsgWrapper(record, actionEnum, record.getId());
        List<WidgetContext> widgetList = getWidgetContexts(type, id, user, params);

        String config = widgetList.get(0).getDashboard().getConfig();
        DashBoardConfig dashBoardConfig = JSONUtils.parseObject(config, DashBoardConfig.class);
        DownloadConfig downloadConfig = dashBoardConfig.getDownloadConfig();
        // 校验是否开启批量下载
        if (null == downloadConfig
                || BooleanUtils.isFalse(downloadConfig.getOnOff())) {
            throw new ELPBizException(String.format("不支持批量下载,type:%s,id:%s,user:%s", type, id, null != user ? user.getId() : null));
        }
        // 分组生成多个workBookContext
        List<WorkBookContext> workBookContexts = groupWorkBookContexts(type, id, user, params, wrapper, widgetList, dashBoardConfig, downloadConfig);
        if (CollectionUtils.isEmpty(workBookContexts)) {
            throw new ELPBizException("workBookContexts is empty");
        }
        log.info(String.format("WorkbookBatchTaskWorker id=%s total=%s", id, workBookContexts.size()));
        // 异步生成压缩包
        WorkbookBatchTaskWorker taskWorker = new WorkbookBatchTaskWorker(workBookContexts, wrapper, downloadConfig, type, id, downloadFileName);
        return ExecutorUtil.submitWorkbookBatchTaskWorker(taskWorker, null);
    }

    private List<WorkBookContext> groupWorkBookContexts(DownloadType type, Long id, User user, List<DownloadViewExecuteParam> params, MsgWrapper wrapper, List<WidgetContext> widgetList, DashBoardConfig dashBoardConfig, DownloadConfig downloadConfig) throws IllegalAccessException, InvocationTargetException {
        // 控制器必选项
        List<String> requiredFilters = null == downloadConfig.getRequiredFilters() ? new ArrayList<>() : downloadConfig.getRequiredFilters();
        // 控制器组合项
        List<String> groupFilters = null == downloadConfig.getGroupFilters() ? new ArrayList<>() : downloadConfig.getGroupFilters();
        if (CollectionUtils.isEmpty(requiredFilters) && CollectionUtils.isEmpty(groupFilters)) {
            // 控制器全部为空的情况
            return Collections.singletonList(WorkBookContext.WorkBookContextBuilder.newBuildder()
                    .withWrapper(wrapper)
                    .withWidgets(widgetList)
                    .withUser(user)
                    .withResultLimit(resultLimit)
                    .withTaskKey("DownloadTask_" + id)
                    .build());
        }

        // 对筛选条件控制器去重
        List<SqlFilter> distinctFilters = getDistinctFilters(params);
        List<String> distinctFilterKeys = distinctFilters.stream().filter(filter -> !StringUtils.isBlank(getKey(filter)))
                .map(this::getKey).collect(Collectors.toList());
        if (!distinctFilterKeys.containsAll(requiredFilters)) {
            throw new ELPBizException(String.format("筛选器必选项不能为空,type:%s,id:%s,user:%s", type, id, null != user ? user.getId() : null));
        }
        // 填充未选择的组合项值
        handleNoSelectGroupFilters(groupFilters, distinctFilterKeys, dashBoardConfig, widgetList, distinctFilters, user);

        // 获取分组的控制器父子关系映射
        Map<String, Map<String, Map<Object, Set<Object>>>> keyCKeyPValPValC = getParentAndChilds(distinctFilters, dashBoardConfig, user, widgetList);

        List<List<SqlFilter>> resultList = new ArrayList<>();
        // 递归对筛选条件分组
        List<String> allGroupFilterKeys = getAllGroupFilterKeys(dashBoardConfig.getDownloadConfig());
        recursionFilters(resultList, distinctFilters, allGroupFilterKeys, keyCKeyPValPValC, 0, new ArrayList<>());
        // 单次最大一万份
        if (resultList.size() > batchDownloadSize) {
            resultList = resultList.subList(0, batchDownloadSize);
        }
        List<WorkBookContext> workBookContexts = new ArrayList<>();
        for (List<SqlFilter> sqlFilterList : resultList) {
            List<WidgetContext> contexts = new ArrayList<>();
            for (WidgetContext widget : widgetList) {
                WidgetContext context = new WidgetContext();
                BeanUtils.copyProperties(context, widget);
                if (null == widget.getExecuteParam()) {
                    contexts.add(context);
                    continue;
                }
                ViewExecuteParam param = new ViewExecuteParam();
                BeanUtils.copyProperties(param, widget.getExecuteParam());
                if (!CollectionUtils.isEmpty(param.getFilters())) {
                    for (int index = 0; index < param.getFilters().size(); index++) {
                        String filter = param.getFilters().get(index);
                        SqlFilter sqlFilter = JSONUtils.parseObject(filter, SqlFilter.class);
                        param.getFilters().set(index, JSONUtils.toJSONString(sqlFilterList.stream().filter(e -> ObjectUtils.equals(getKey(e), getKey(sqlFilter))).findFirst().orElse(null)));
                    }
                }
                context.setExecuteParam(param);
                contexts.add(context);
            }
            workBookContexts.add(WorkBookContext.WorkBookContextBuilder.newBuildder()
                    .withWrapper(wrapper)
                    .withWidgets(contexts)
                    .withUser(user)
                    .withResultLimit(resultLimit)
                    .withTaskKey("DownloadTask_" + id)
                    .build());
        }
        return workBookContexts;
    }

    private void handleNoSelectGroupFilters(List<String> groupFilters, List<String> distinctFilterKeys, DashBoardConfig dashBoardConfig, List<WidgetContext> widgetList, List<SqlFilter> distinctFilters, User user) {
        // 未选择的组合项key
        List<String> noSelectGroupFilterKeys = groupFilters.stream().filter(groupFilter -> !distinctFilterKeys.contains(groupFilter)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noSelectGroupFilterKeys)) {
            return ;
        }
        // dashboard的所有filter
        List<DashBoardConfigFilter> dashBoardConfigFilters = getDashBoardConfigFilters(dashBoardConfig);
        for (DashBoardConfigFilter dashBoardConfigFilter : dashBoardConfigFilters) {
            if (!noSelectGroupFilterKeys.contains(dashBoardConfigFilter.getKey())) {
                continue;
            }
            // 匹配dashboard的filter
            List<String> viewIds = new ArrayList<>(dashBoardConfigFilter.getRelatedViews().keySet());
            if (CollectionUtils.isEmpty(viewIds)) {
                // can not happen
                continue;
            }
            String viewId = viewIds.get(0);

            RelatedView oneRelatedView = dashBoardConfigFilter.getRelatedViews().get(viewId);

            SqlFilter parentFilter = getParentFilter(distinctFilters, dashBoardConfigFilters, dashBoardConfigFilter);
            ViewWithSourceBaseInfo view = viewService.getView(Long.parseLong(viewId), user);

            List<ViewDistinctParam> params = new ArrayList<>();
            for (String id : viewIds) {
                RelatedView relatedView = dashBoardConfigFilter.getRelatedViews().get(id);
                ViewDistinctParam param = new ViewDistinctParam();
                param.setViewId(Long.parseLong(id));
                param.setColumns(Collections.singletonList(relatedView.getName()));
                if (null != parentFilter) {
                    param.setFilters(Collections.singletonList(JSONUtils.toJSONString(parentFilter)));
                }
                params.add(param);
            }
            Set<Object> values = getDistinctValueSet(user, params);
            // 创建sqlFilter（筛选条件）
            SqlFilter sqlFilter = buildSqlFilter(dashBoardConfigFilter, oneRelatedView, values, view);
            // 把筛选条件添加到所有去重筛选条件中
            distinctFilters.add(sqlFilter);
            // 把sqlFilter添加到关联widget中
            for (WidgetContext context : widgetList) {
                if (viewIds.contains(String.valueOf(context.getWidget().getViewId()))) {
                    if (null == context.getExecuteParam().getFilters()) {
                        List<String> filters = new ArrayList<>();
                        filters.add(JSONUtils.toJSONString(sqlFilter));
                        context.getExecuteParam().setFilters(filters);
                    } else {
                        context.getExecuteParam().getFilters().add(JSONUtils.toJSONString(sqlFilter));
                    }
                }
            }
        }
    }

    private Map<String, Map<String, Map<Object, Set<Object>>>> getParentAndChilds(List<SqlFilter> distinctFilters, DashBoardConfig dashBoardConfig, User user, List<WidgetContext> widgetList) {
        // 父子关系映射: 子key-父key-父value-子value
        Map<String, Map<String, Map<Object, Set<Object>>>> keyCKeyPValPValC = new HashMap<>();
        // dashboard的所有filter
        List<DashBoardConfigFilter> dashBoardConfigFilters = getDashBoardConfigFilters(dashBoardConfig);

        List<String> allGroupFilterKeys = getAllGroupFilterKeys(dashBoardConfig.getDownloadConfig());

        for (DashBoardConfigFilter dashBoardConfigFilter : dashBoardConfigFilters) {
            // 不考虑父子控制器顺序，默认应该是父前子后
            if (!allGroupFilterKeys.contains(dashBoardConfigFilter.getKey())) {
                // 忽略非分组的控制器
                continue;
            }
            // 用户已选择的组合项或必选项,如果有父子关系，需要获取父子关系，用于分组拆分
            SqlFilter parentFilter = getGroupParentFilter(distinctFilters, dashBoardConfigFilters, dashBoardConfigFilter, allGroupFilterKeys);
            if (null != parentFilter) {
                // 匹配dashboard的filter
                List<String> viewIds = new ArrayList<>(dashBoardConfigFilter.getRelatedViews().keySet());
                if (CollectionUtils.isEmpty(viewIds)) {
                    // can not happen
                    continue;
                }
                String viewId = viewIds.get(0);
                RelatedView oneRelatedView = dashBoardConfigFilter.getRelatedViews().get(viewId);
                List<ViewDistinctParam> params = new ArrayList<>();
                for (String id : viewIds) {
                    RelatedView relatedView = dashBoardConfigFilter.getRelatedViews().get(id);
                    ViewDistinctParam param = new ViewDistinctParam();
                    param.setViewId(Long.parseLong(id));
                    param.setColumns(Collections.singletonList(relatedView.getName()));
                    if (null != parentFilter) {
                        param.setFilters(Collections.singletonList(JSONUtils.toJSONString(parentFilter)));
                    }
                    params.add(param);
                }

                ViewWithSourceBaseInfo view = viewService.getView(Long.parseLong(viewId), user);

                JSONObject models = JSONObject.parseObject(view.getModel());
                JSONObject model = JSONObject.parseObject(models.getString(oneRelatedView.getName()));
                String sqlType = model.getString("sqlType");
                Object value = parentFilter.getValue();
                if (value instanceof List<?>) {
                    for (Object name : (List) value) {
                        DistinctParam param = new DistinctParam();
                        param.setColumns(Collections.singletonList(oneRelatedView.getName()));
                        SqlFilter copyFilter = parentFilter.copy();
                        copyFilter.setValue(name);
                        copyFilter.setOperator(SqlOperatorEnum.EQUALSTO.getValue());
                        param.setFilters(Arrays.asList(JSONUtils.toJSONString(copyFilter)));
                        Set<Object> distinctValueSet = getDistinctValueSet(user, params);
                        add2ParentAndChildMap(dashBoardConfigFilter, parentFilter.getKey(), name, keyCKeyPValPValC, distinctValueSet, sqlType);
                    }
                } else {
                    DistinctParam param = new DistinctParam();
                    param.setColumns(Collections.singletonList(oneRelatedView.getName()));
                    param.setFilters(Arrays.asList(JSONUtils.toJSONString(parentFilter)));
                    Set<Object> distinctValueSet = getDistinctValueSet(user, params);
                    add2ParentAndChildMap(dashBoardConfigFilter, parentFilter.getKey(), parentFilter.getValue(), keyCKeyPValPValC, distinctValueSet, sqlType);
                }
            }
        }
        return keyCKeyPValPValC;
    }

    private SqlFilter getGroupParentFilter(List<SqlFilter> distinctFilters, List<DashBoardConfigFilter> dashBoardConfigFilters, DashBoardConfigFilter dashBoardConfigFilter, List<String> allGroupFilterKeys) {
        SqlFilter parentFilter = getParentFilter(distinctFilters, dashBoardConfigFilters, dashBoardConfigFilter);
        if (null == parentFilter) {
            return null;
        }
        if (allGroupFilterKeys.contains(parentFilter.getKey())) {
            return parentFilter;
        } else {
            return getGroupParentFilter(distinctFilters, dashBoardConfigFilters, dashBoardConfigFilters.stream().filter(e -> ObjectUtils.equals(e.getKey(), parentFilter.getKey())).findFirst().orElse(null), allGroupFilterKeys);
        }
    }

    private List<String> getAllGroupFilterKeys(DownloadConfig downloadConfig) {
        if (null == downloadConfig) {
            return new ArrayList<>();
        }
        List<String> allGroupFilters = new ArrayList<>();
        if (!CollectionUtils.isEmpty(downloadConfig.getGroupFilters())) {
            allGroupFilters.addAll(downloadConfig.getGroupFilters());
        }
        if (!CollectionUtils.isEmpty(downloadConfig.getRequiredFilters())) {
            allGroupFilters.addAll(downloadConfig.getRequiredFilters());
        }
        return allGroupFilters;
    }

    private void add2ParentAndChildMap(DashBoardConfigFilter dashBoardConfigFilter, String parentFilterKey, Object parentValue, Map<String, Map<String, Map<Object, Set<Object>>>> keyCKeyPValPValC, Set<Object> distinctValueSet, String sqlType) {
        if (!keyCKeyPValPValC.containsKey(dashBoardConfigFilter.getKey())) {
            keyCKeyPValPValC.put(dashBoardConfigFilter.getKey(), new HashMap<>());
        }
        if (!keyCKeyPValPValC.get(dashBoardConfigFilter.getKey()).containsKey(parentFilterKey)) {
            keyCKeyPValPValC.get(dashBoardConfigFilter.getKey()).put(parentFilterKey, new HashMap<>());
        }
        if (isNumericData(sqlType)) {
            keyCKeyPValPValC.get(dashBoardConfigFilter.getKey()).get(parentFilterKey).put(parentValue, distinctValueSet);
        } else {
            keyCKeyPValPValC.get(dashBoardConfigFilter.getKey()).get(parentFilterKey).put(parentValue, distinctValueSet.stream().map(e -> getNotNumericData(e.toString())).collect(Collectors.toSet()));
        }
    }

    private Set<Object> getDistinctValueSet(User user, List<ViewDistinctParam> params) {
        List<Map<String, Object>> distinctValue = viewService.getDistinctValueList(params, user);
        Set<Object> values = new HashSet<>();
        distinctValue.stream().map(Map::values).collect(Collectors.toList()).forEach(values::addAll);
        return values;
    }

    private SqlFilter buildSqlFilter(DashBoardConfigFilter dashBoardConfigFilter, RelatedView relatedView, Set<Object> distinctValue, ViewWithSourceBaseInfo view) {
        JSONObject models = JSONObject.parseObject(view.getModel());
        JSONObject model = JSONObject.parseObject(models.getString(relatedView.getName()));
        SqlFilter sqlFilter = new SqlFilter();
        sqlFilter.setName(relatedView.getName());
        sqlFilter.setSqlType(model.getString("sqlType"));
        sqlFilter.setOperator(SqlOperatorEnum.IN.getValue());
        sqlFilter.setType("filter");
        sqlFilter.setKey(dashBoardConfigFilter.getKey());
        if (isNumericData(sqlFilter.getSqlType())) {
            sqlFilter.setValue(distinctValue.stream().map(Object::toString).collect(Collectors.toList()));
        } else {
            sqlFilter.setValue(distinctValue.stream().map(o -> getNotNumericData(o.toString())).collect(Collectors.toList()));
        }
        return sqlFilter;
    }

    private boolean isNumericData(String sqlType) {
        return Arrays.stream(SqlFilter.NumericDataType.values())
                .anyMatch(value -> sqlType.equalsIgnoreCase(value.getType()));
    }

    private String getNotNumericData(String str) {
        return Consts.APOSTROPHE + str + Consts.APOSTROPHE;
    }

    private List<DashBoardConfigFilter> getDashBoardConfigFilters(DashBoardConfig dashBoardConfig) {
        if (null == dashBoardConfig || CollectionUtils.isEmpty(dashBoardConfig.getFilters())) {
            return new ArrayList<>();
        }
        return dashBoardConfig.getFilters().stream().map(filter -> JSONUtils.parseObject(filter, DashBoardConfigFilter.class)).collect(Collectors.toList());
    }

    /**
     * 从distinctFilters中,获取dashBoardConfigFilter的父节点,
     * 如果没有父节点,获取父节点的父节点..
     * @param distinctFilters
     * @param dashBoardFilters
     * @param dashBoardConfigFilter
     * @return 父节点/null
     */
    private SqlFilter getParentFilter(List<SqlFilter> distinctFilters, List<DashBoardConfigFilter> dashBoardFilters, DashBoardConfigFilter dashBoardConfigFilter) {
        if (StringUtils.isBlank(dashBoardConfigFilter.getParent())) {
            return null;
        }
        SqlFilter parentFilter = distinctFilters.stream().filter(e -> ObjectUtils.equals(dashBoardConfigFilter.getParent(), getKey(e))).findFirst().orElse(null);
        if (null != parentFilter) {
            // 如果筛选条件中有父级控制器，直接返回
            return parentFilter;
        }
        // 获取父级的父级控制器
        DashBoardConfigFilter parent = dashBoardFilters.stream().filter(filter -> ObjectUtils.equals(dashBoardConfigFilter.getParent(), filter.getKey())).findFirst().orElse(null);
        if (null == parent || StringUtils.isBlank(parent.getParent())) {
            return null;
        }
        return getParentFilter(distinctFilters, dashBoardFilters, parent);
    }

    private String getKey(SqlFilter sqlFilter) {
        return sqlFilter.getKey();
    }

    /**
     * 递归对distinctFilters按照allGroupFilters分组
     *
     * @param resultList
     * @param distinctFilters
     * @param allGroupFilters
     * @param keyCKeyPValPValC
     * @param index
     * @param result
     */
    private void recursionFilters(List<List<SqlFilter>> resultList, List<SqlFilter> distinctFilters, List<String> allGroupFilters, Map<String, Map<String, Map<Object, Set<Object>>>> keyCKeyPValPValC, int index, List<SqlFilter> result) {
        if (index >= distinctFilters.size()) {
            // 终止
            resultList.add(result);
            return;
        }
        if (index == 0) {
            // 开始
        }
        SqlFilter sqlFilter = distinctFilters.get(index);
        List<SqlFilter> groups;
        if (allGroupFilters.contains(getKey(sqlFilter))) {
            groups = group(sqlFilter, result, keyCKeyPValPValC);
        } else {
            groups = Collections.singletonList(sqlFilter);
        }
        if (CollectionUtils.isEmpty(groups)) {
            // 没有父级控制器对应的子级控制器value，无效的父级控制器值
            return;
        }
        index++;
        for (SqlFilter group : groups) {
            List<SqlFilter> list = new ArrayList<>(result);
            list.add(group);
            recursionFilters(resultList, distinctFilters, allGroupFilters, keyCKeyPValPValC, index, list);
        }
    }

    /**
     * 对sqlFilter分组, 按照keyCKeyPValPValC通过parents过滤
     * @param sqlFilter
     * @param parents
     * @param keyCKeyPValPValC
     * @return
     */
    private List<SqlFilter> group(SqlFilter sqlFilter, List<SqlFilter> parents, Map<String, Map<String, Map<Object, Set<Object>>>> keyCKeyPValPValC) {
        Criterion criterion;
        if (SqlOperatorEnum.BETWEEN.getValue().equalsIgnoreCase(sqlFilter.getOperator())) { // can not happen
            JSONArray values = (JSONArray) sqlFilter.getValue();
            criterion = new Criterion(sqlFilter.getName(), sqlFilter.getOperator(), values.get(0), values.get(1), sqlFilter.getSqlType());
        } else {
            criterion = new Criterion(sqlFilter.getName(), sqlFilter.getOperator(), sqlFilter.getValue(), sqlFilter.getSqlType());
        }
        List<SqlFilter> resultList = new ArrayList<>();
        if (criterion.isListValue()) {
            // 只有list支持分组
            List values = (List) criterion.getValue();
            for (Object value : values) {
                SqlFilter copy = sqlFilter.copy();
                copy.setValue(value);
                copy.setOperator(SqlOperatorEnum.EQUALSTO.getValue());
                resultList.add(copy);
            }
        } else {
            resultList.add(sqlFilter);
        }
        // 根据父子级关系过滤
        if (keyCKeyPValPValC.containsKey(sqlFilter.getKey())) {
            Map<String, Map<Object, Set<Object>>> keyPValPValC = keyCKeyPValPValC.get(sqlFilter.getKey());
            SqlFilter parent = parents.stream().filter(e -> keyPValPValC.containsKey(e.getKey())).findFirst().orElse(null);
            if (null != parent) {
                Map<Object, Set<Object>> valPValC = keyPValPValC.get(parent.getKey());
                if (null != valPValC) {
                    Set<Object> values = valPValC.get(parent.getValue());
                    resultList = resultList.stream().filter(e -> values.contains(e.getValue())).collect(Collectors.toList());
                }
            }
        }
        return resultList;
    }

    private List<SqlFilter> getDistinctFilters(List<DownloadViewExecuteParam> params) {
        List<SqlFilter> distinctFilters = new ArrayList<>(params.stream()
                .filter(param -> null != param.getParam() && null != param.getParam().getFilters())
                .flatMap(param -> param.getParam().getFilters().stream().map(filter -> JSONUtils.parseObject(filter, SqlFilter.class)))
                .collect(Collectors.toMap(this::getKey, o -> o, (o1, o2) -> o1))
                .values());
        return distinctFilters;
    }

}
