package edp.davinci.service.impl;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.redis.client.RedisClient;

import edp.core.consts.Consts;
import edp.core.consts.RedisConsts;
import edp.core.enums.DataTypeEnum;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.model.PaginateWithQueryColumns;
import edp.core.utils.CollectionUtils;
import edp.core.utils.DateUtils;
import edp.core.utils.MD5Util;
import edp.core.utils.STUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.common.utils.SqlOptimizeUtil;
import edp.davinci.core.common.Constants;
import edp.davinci.core.config.BuildSqlTemplateRegister;
import edp.davinci.core.enums.LogNameEnum;
import edp.davinci.core.enums.SqlVariableTypeEnum;
import edp.davinci.core.enums.SqlVariableValueTypeEnum;
import edp.davinci.core.enums.UserPermissionEnum;
import edp.davinci.core.model.SqlEntity;
import edp.davinci.core.template.AbstractBuildQuerySqlTemplate;
import edp.davinci.core.utils.SqlParseUtils;
import edp.davinci.dao.RelRoleViewMapper;
import edp.davinci.dao.SourceMapper;
import edp.davinci.dao.SqlQueryInfoMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.sourceDto.SourceBaseInfo;
import edp.davinci.dto.viewDto.AuthParamValue;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.DistinctQueryParam;
import edp.davinci.dto.viewDto.Order;
import edp.davinci.dto.viewDto.Param;
import edp.davinci.dto.viewDto.RelRoleViewDto;
import edp.davinci.dto.viewDto.ViewBaseInfo;
import edp.davinci.dto.viewDto.ViewCreate;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewExecuteSql;
import edp.davinci.dto.viewDto.ViewUpdate;
import edp.davinci.dto.viewDto.ViewWithSource;
import edp.davinci.dto.viewDto.ViewWithSourceBaseInfo;
import edp.davinci.model.RelRoleView;
import edp.davinci.model.Source;
import edp.davinci.model.SqlQueryInfo;
import edp.davinci.model.SqlVariable;
import edp.davinci.model.User;
import edp.davinci.model.View;
import edp.davinci.model.Widget;
import edp.davinci.service.ProjectService;
import edp.davinci.service.ViewService;
import edp.davinci.service.excel.SQLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import static edp.core.consts.Consts.BASE_DATA_TYPE_LIST;
import static edp.core.consts.Consts.COMMA;
import static edp.core.consts.Consts.MINUS;
import static edp.davinci.core.common.Constants.NO_AUTH_PERMISSION;
import static edp.davinci.core.enums.SqlVariableTypeEnum.AUTHVARE;
import static edp.davinci.core.enums.SqlVariableTypeEnum.QUERYVAR;

@Slf4j
@Service("viewService")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class ViewServiceImpl implements ViewService {

    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    private static ExecutorService executorService = new ThreadPoolExecutor(4, 4,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private SourceMapper sourceMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private RelRoleViewMapper relRoleViewMapper;

    @Autowired
    private SqlUtils sqlUtils;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SqlParseUtils sqlParseUtils;

    private final RedisClient redisClient;

    @Autowired
    private SqlQueryInfoMapper sqlQueryInfoMapper;

    private  final BuildSqlTemplateRegister buildSqlTemplateRegister;

    @Value("${default.cache.time}")
    private Integer cacheTime;

    @Value("${sql_template_delimiter:$}")
    private String sqlTempDelimiter;

    @Value("${default.distinct.limit:2000}")
    private Integer limit;

    @Value("${flush.view.list}")
    private String flushViewList;

    private static final String SQL_VARABLE_KEY = "name";

    @Override
    public synchronized boolean isExist(String name, Long id, Long projectId) {
        Long viewId = viewMapper.getByNameWithProjectId(name, projectId);
        if (null != id && null != viewId) {
            return !id.equals(viewId);
        }
        return null != viewId && viewId > 0L;
    }

    /**
     * 获取View列表
     *
     * @param projectId
     * @param user
     * @return
     */
    @Override
    public List<ViewBaseInfo> getViews(Long projectId, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(projectId, user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            return null;
        }

        List<ViewBaseInfo> views = viewMapper.getViewBaseInfoByProject(projectId);

        if (null != views) {
            ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
            if (projectPermission.getVizPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                    projectPermission.getWidgetPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                    projectPermission.getViewPermission() == UserPermissionEnum.HIDDEN.getPermission()) {
                return null;
            }
        }

        return views;
    }

    @Override
    public ViewWithSourceBaseInfo getView(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        ViewWithSourceBaseInfo view = viewMapper.getViewWithSourceBaseInfo(id);
        if (null == view) {
            throw new NotFoundException("view is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(view.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getVizPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                projectPermission.getWidgetPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                projectPermission.getViewPermission() == UserPermissionEnum.HIDDEN.getPermission()) {
            throw new UnAuthorizedException("Insufficient permissions");
        }

        List<RelRoleView> relRoleViews = relRoleViewMapper.getByView(view.getId());
        view.setRoles(relRoleViews);

        return view;
    }

    @Override
    public SQLContext getSQLContext(boolean isMaintainer, ViewWithSource viewWithSource, ViewExecuteParam executeParam, User user) {
        if (null == executeParam || (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators()))) {
            return null;
        }
        if (null == viewWithSource.getSource()) {
            throw new NotFoundException("source is not found");
        }
        if (StringUtils.isEmpty(viewWithSource.getSql())) {
            throw new NotFoundException("sql is not found");
        }

        SQLContext context = new SQLContext();
        //解析变量
        List<SqlVariable> variables = viewWithSource.getVariables();
        //解析sql
        SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter);
        //列权限（只记录被限制访问的字段）
        Set<String> excludeColumns = new HashSet<>();

        packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables, executeParam.getParams(), excludeColumns, user);

        String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

        context.setSrcSql(srcSql);
        context.setExecuteSql(sqlParseUtils.getSqls(srcSql, Boolean.FALSE));

        List<String> querySqlList = sqlParseUtils.getSqls(srcSql, Boolean.TRUE);
        if (!CollectionUtils.isEmpty(querySqlList)) {
            Source source = viewWithSource.getSource();
            DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(source.getJdbcUrl());
            AbstractBuildQuerySqlTemplate buildSqlTemplate = buildSqlTemplateRegister.getBuildSqlTemplate(dataTypeEnum.getFeature());
            if (null == buildSqlTemplate) {
                throw new NotFoundException("未找到对应数据库类型的sql模板：" + dataTypeEnum.getFeature());
            }
            buildSqlTemplate.build(querySqlList, source, executeParam);
            executeParam.addExcludeColumn(excludeColumns, source.getJdbcUrl(), source.getDbVersion());
            context.setQuerySql(querySqlList);
            context.setViewExecuteParam(executeParam);
        }
        if (!CollectionUtils.isEmpty(excludeColumns)) {
            List<String> excludeList = new ArrayList<>(excludeColumns);
            context.setExcludeColumns(excludeList);
        }
        return context;
    }

    @Override
    public List getDistinctValueList(List<ViewDistinctParam> params, User user) {
        List<String> distinctSqls = Lists.newArrayList();
        Set<String> tableNameSet = Sets.newHashSet();
        Source source = null;
        for (ViewDistinctParam param : params) {
            if(params.size() > 1) {
                param.setOrderColumn(null);
            }
            ViewWithSource viewWithSource = viewMapper.getViewWithSource(param.getViewId());
            if (null == viewWithSource) {
                log.info("view (:{}) not found", param.getViewId());
                throw new NotFoundException("view is not found, viewId: " + param.getViewId());
            }
            if (source != null && !source.getId().equals(viewWithSource.getSource().getId())) {
                throw new ServerException("数据源不一致");
            } else {
                source = viewWithSource.getSource();
            }

            ProjectDetail projectDetail = projectService.getProjectDetail(viewWithSource.getProjectId(), user, false);
            boolean allowGetData = projectService.allowGetData(projectDetail, user);

            if (!allowGetData) {
                throw new UnAuthorizedException("UnAuthorized, viewId: " + param.getViewId());
            }
            tableNameSet.addAll(this.sqlUtils.getTableNames(viewWithSource.getId()));
            distinctSqls.add(getDistinctSql(projectService.isMaintainer(projectDetail, user), viewWithSource, param, user));
        }

        List result = Lists.newArrayList();

        if (CollectionUtils.isEmpty(distinctSqls)) {
            return result;
        }
        // 拼接union sql
        String querySql = distinctSqls.get(0);
        if(distinctSqls.size() > 1) {
            querySql = STUtils.queryUnionDistinctSql(distinctSqls, limit, params.get(0).getColumns(),
                    params.get(0).getDirection());
        }

        String md5Key = MD5Util.getMD5(params.get(0).getType().toUpperCase() + querySql, true, 32);
        boolean cacheAbleFlag = judgeCacheAble(md5Key, tableNameSet);
        if (cacheAbleFlag) {
            try {
                String cache = redisClient.get(RedisConsts.DAVINCI_VIEW_CACHE, md5Key);
                if (cache != null) {
                    result = Lists.newArrayList(JSON.parseArray(cache));
                    return result;
                }
            } catch (Exception e) {
                log.warn("get distinct value by cache: {}", e.getMessage());
            }
        }

        try {
            SqlUtils sqlUtils = this.sqlUtils.init(source);
            result = sqlUtils.query4List(querySql, limit);

            String value = JSON.toJSONString(result);
            // 查询结果缓存
            redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE, cacheTime, value, md5Key);
            String nowDate = DateUtils.toDateString(new Date());
            redisClient.setex(RedisConsts.MD5KEY_UPDATETIME, cacheTime, nowDate, md5Key);
        } catch (Exception e) {
            log.error("sql执行异常" + e.getMessage() + ";执行sql:" + querySql);
        }
        return result;
    }

    @Override
    public String getDistinctSql(boolean isMaintainer, ViewWithSource viewWithSource, DistinctParam param, User user) {
        if (StringUtils.isEmpty(viewWithSource.getSql())) {
            return null;
        }

        List<SqlVariable> variables = viewWithSource.getVariables();
        SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter);
        packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables, param.getParams(), null, user);

        String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

        Source source = viewWithSource.getSource();

        SqlUtils sqlUtils = this.sqlUtils.init(source);

        List<String> executeSqlList = sqlParseUtils.getSqls(srcSql, false);
        if (!CollectionUtils.isEmpty(executeSqlList)) {
            executeSqlList.forEach(sqlUtils::execute);
        }

        List<String> querySqlList = sqlParseUtils.getSqls(srcSql, true);
        String sql = null;
        if (!CollectionUtils.isEmpty(querySqlList)) {
            STGroup stg = new STGroupFile(Constants.SQL_TEMPLATE);
            // 支持取最大值或最小值
            ST st = stg.getInstanceOf("queryDistinctSql");
            switch (param.getType()) {
                case "max":
                case "min":
                    st = stg.getInstanceOf("queryMaxOrMinSql");
                    String order = "max".equals(param.getType().toLowerCase()) ? "DESC" : "ASC";
                    st.add("order", order);
                    break;
                default:
                    break;
            }
            st.add("columns", param.getColumns());
            st.add("filters", SqlUtils.convertFilters(param.getFilters(), source));
            st.add("sql", querySqlList.get(querySqlList.size() - 1));
            st.add("keywordPrefix", SqlUtils.getKeywordPrefix(source.getJdbcUrl(), source.getDbVersion()));
            st.add("keywordSuffix", SqlUtils.getKeywordSuffix(source.getJdbcUrl(), source.getDbVersion()));
            st.add("direction", param.getDirection());
            st.add("orderColumn", param.getOrderColumn());
            sql = st.render();
            // sql优化
            if (viewWithSource.getSource().getJdbcUrl().contains(DataTypeEnum.CLICKHOUSE.getDesc()) ||
                    viewWithSource.getSource().getJdbcUrl().contains(DataTypeEnum.MYSQL.getDesc())) {
                sql = SqlOptimizeUtil.optimizeCKSql(sql);
            }
        }
        return sql;
    }

    /**
     * 交叉数据查询
     * @param isMaintainer
     * @param viewWithSource
     * @param executeParam
     * @param user
     * @return
     * @throws ServerException
     * @throws SQLException
     */
    @Override
    public PaginateWithQueryColumns getCrossResultDataList(boolean isMaintainer, ViewWithSource viewWithSource,
                                                           ViewExecuteParam executeParam, User user) throws ServerException, SQLException {
        PaginateWithQueryColumns paginate = null;
        if (null == executeParam) {
            return null;
        }

        if (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators())) {
            return null;
        }

        if (StringUtils.isEmpty(viewWithSource.getSql())) {
            return null;
        }

        if (null == viewWithSource.getSource()) {
            throw new NotFoundException("source is not found");
        }

        try {
            // 解析变量
            List<SqlVariable> variables = viewWithSource.getVariables();
            // 解析sql
            SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter);
            // 列权限（只记录被限制访问的字段）
            Set<String> excludeColumns = new HashSet<>();
            packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables,
                    executeParam.getParams(), excludeColumns, user);
            String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(),
                    sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);
            SqlUtils sqlUtils = this.sqlUtils.init(viewWithSource.getSource());
            JdbcTemplate jdbcTemplate = sqlUtils.jdbcTemplate();

            // 获取totalCount
            Integer totalCount = 0;
            if(!CollectionUtils.isEmpty(executeParam.getRowGroups())) {
                // 获取查询行维度记录totalCount的sql
                String countSql = STUtils.queryGroupCountSql(srcSql, executeParam.getRowGroups(),
                        SqlUtils.convertFilters(executeParam.getFilters(), viewWithSource.getSource()),
                        SqlUtils.convertHaving(executeParam, viewWithSource.getSource()));

                Object o = jdbcTemplate.queryForObject(SqlOptimizeUtil.optimizeCKSql(countSql), Object.class);
                totalCount = Integer.parseInt(String.valueOf(o));
                executeParam.setTotalCount(totalCount);
                // 如果totalcount大于100但未分页, 提示必须分页
                if(totalCount > 100 && (executeParam.getPageNo() < 1 || executeParam.getPageSize() < 1)
                        && executeParam.getLimit() < 1) {
                    throw new ServerException("记录条数超过100, 请开启分页");
                }
            }

            Source source = viewWithSource.getSource();

            List<Order> orders = executeParam.getOrders();
            // 获取关于列维度的排序
            List<Order> colOrders = orders.stream().filter(o ->
                    !CollectionUtils.isEmpty(executeParam.getColGroups())
                            && executeParam.getColGroups().contains(o.getColumn()))
                    .collect(Collectors.toList());

            // 列维度结果集
            List<Map<String, Object>> colMapList = null;
            String queryColSql = null;
            if (!CollectionUtils.isEmpty(executeParam.getColGroups())) {
                // 获取查询列维度的sql
                queryColSql = STUtils.querySql(false, executeParam.getColGroups(), null,
                        SqlUtils.convertFilters(executeParam.getFilters(), source),
                        SqlUtils.convertHaving(executeParam, source),
                        SqlUtils.getOrders(colOrders, source.getJdbcUrl(), source.getDbVersion()),
                        executeParam.getColOrders(source.getJdbcUrl(), source.getDbVersion()),
                        srcSql, SqlUtils.getKeywordPrefix(source.getJdbcUrl(), source.getDbVersion()),
                        SqlUtils.getKeywordPrefix(source.getJdbcUrl(), source.getDbVersion()));
                // 透视表仅在确定列维度时需要列排序字段
                executeParam.setColOrders(null);
            }

            // 获取交叉查询sql
            DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(source.getJdbcUrl());
            AbstractBuildQuerySqlTemplate buildSqlTemplate = buildSqlTemplateRegister.getBuildSqlTemplate(dataTypeEnum.getFeature());
            List<String> queryList = Lists.newArrayList(srcSql);
            buildSqlTemplate.crossSqlBuild(queryList, source, executeParam);
            String crossSql = queryList.get(0);

            // 生成MD5key作为rediskey
            StringBuilder slatBuilder = new StringBuilder();
            slatBuilder.append(executeParam.getPageNo());
            slatBuilder.append(MINUS);
            slatBuilder.append(executeParam.getLimit());
            slatBuilder.append(MINUS);
            slatBuilder.append(executeParam.getPageSize());
            slatBuilder.append(MINUS);
            excludeColumns.forEach(slatBuilder::append);
            slatBuilder.append(MINUS);
            slatBuilder.append(crossSql);
            if(null != queryColSql) {
                slatBuilder.append(MINUS);
                slatBuilder.append(queryColSql);
            }
            String md5Key = MD5Util.getMD5(slatBuilder.toString() + crossSql, true, 32);

            // 判断是否走缓存
            paginate = getRedisCachePaginate(viewWithSource.getId(), md5Key, executeParam.getFlush(), executeParam.getFlushInterval(), jdbcTemplate);
            if(null != paginate) {
                return paginate;
            }

            paginate = sqlUtils.syncQuery4Paginate(crossSql, executeParam.getPageNo(), executeParam.getPageSize(),
                    executeParam.getTotalCount(), executeParam.getLimit(), excludeColumns);
            if(null != queryColSql) {
                colMapList = jdbcTemplate.queryForList(SqlOptimizeUtil.optimizeCKSql(queryColSql));
                paginate.setHeaders(colMapList);
            }
            List<Map<String, Object>> resultList = paginate.getResultList();
            if(CollectionUtils.isEmpty(resultList)) {
               return paginate;
            }

            // 数据转化
            List<Map<String, Object>> newResultList = Lists.newArrayList();

            boolean isArray = false;
            // 行维度如果为空，则列维度及指标未进行行转列, 需要对返回结果进行行转列转化
            if(CollectionUtils.isEmpty(executeParam.getRowGroups())) {
                Map<String, Object> groupMap = Maps.newHashMap();
                // 按照行维度或指标对返回结果进行分组
                for (Map<String, Object> map : resultList) {
                    map.keySet().forEach(k -> {
                        List<Object> list = (List<Object>) groupMap.get(k);
                        if(null == list) {
                            groupMap.put(k, Lists.newArrayList(map.get(k)));
                        } else {
                            list.add(map.get(k));
                        }
                    });
                }
                resultList = Lists.newArrayList(groupMap);
                isArray = true;
            }
            for (Map<String, Object> r : resultList) {
                Map<String, Object> map = Maps.newHashMap();
                // 行维度
                if(!CollectionUtils.isEmpty(executeParam.getRowGroups())) {
                    map = executeParam.getRowGroups().stream().collect(Collectors.toMap(m -> m, m -> r.get(m)));
                }
                // 列维度及指标
                List<Map<String, Object>> cellMap = Lists.newArrayList();
                map.put("_cols_", cellMap);
                newResultList.add(map);

                // 过滤出行转列指标
                Map<String, Object> aggCollect = r.keySet().stream()
                        .filter(key -> null != executeParam.getRowGroups() && !executeParam.getRowGroups().contains(key)
                                && !key.contains("_sortkey_"))
                        .collect(Collectors.toMap(k -> k, k -> r.get(k)));

                // 行维度转化
                if (CollectionUtils.isEmpty(aggCollect)) {
                    continue;
                }
                if(CollectionUtils.isEmpty(colMapList)) { // 不存在列维度, 最多仅有1个指标
                    // 此时仅有指标进行行转列操作, 且转的列数组最多仅有1个值(基于行列维度分组，指标聚合的基础上再对行维度分组，指标聚合)
                    Map<String, Object> tempMap = Maps.newHashMap();
                    for (String s : aggCollect.keySet()) {
                        List o = crossResultObjToList(aggCollect.get(s), dataTypeEnum, isArray);
                        tempMap.put(s, CollectionUtils.isEmpty(o) ? null : o.get(0));
                    }
                    cellMap.add(tempMap);
                } else {
                    // 指标行转列map
                    Map<String, Object> metricCollect = aggCollect.keySet().stream()
                            .filter(key -> null != executeParam.getColGroups() && !executeParam.getColGroups().contains(key))
                            .collect(Collectors.toMap(k -> k, k -> aggCollect.get(k)));

                    List<String> colGroups = executeParam.getColGroups();
                    //  行转列后，同一行的数组长度相同
                    List array = crossResultObjToList(r.get(colGroups.get(0)), dataTypeEnum, isArray);
                    int size = CollectionUtils.isEmpty(array) ? 0 : array.size();
                    // 对aggCollect进行处理,转成 colGroup -> key, agg -> value 的List<Map<key, vlaue>>结构
                    Map<String, Map<String, Object>> colToMetricMap = Maps.newHashMap();
                    for (int i = 0; i < size; i++) {
                        // key
                        List<String> colKeyArr = Lists.newArrayList();
                        for (String colGroup : colGroups) {
                            List colGroupArr = crossResultObjToList(aggCollect.get(colGroup), dataTypeEnum, isArray);
                            colKeyArr.add(colGroupArr.get(i) != null ? colGroupArr.get(i).toString() : "null");
                        }
                        Map<String, Object> valueMap = Maps.newHashMap();
                        for (String mcKey : metricCollect.keySet()) {
                            List mcList = crossResultObjToList(metricCollect.get(mcKey), dataTypeEnum, isArray);
                            valueMap.put(mcKey, mcList.get(i));
                        }
                        colToMetricMap.put(String.join(",", colKeyArr), valueMap);
                    }

                    for (Map<String, Object> colMap : colMapList) {
                        // key
                        List<String> colKeyArr = Lists.newArrayList();
                        for (String colGroup : colGroups) {
                            colKeyArr.add(colMap.get(colGroup) != null ? colMap.get(colGroup).toString() : "null");
                        }
                        String colKey = String.join(",", colKeyArr);
                        cellMap.add(colToMetricMap.get(colKey));
                    }
                }
            }
            paginate.setTotalCount(totalCount);
            paginate.setResultList(newResultList);
            updateDataPaginateRedisCache(paginate, md5Key, executeParam.getFlushInterval());
        } catch (Exception e) {
            throw new ServerException(e.getMessage(), e);
        }
        return paginate;
    }

    @Override
    public Pair<String, String> getPivotGroupSqlContext(boolean isMaintainer, ViewWithSource viewWithSource,
                                                        ViewExecuteParam executeParam, User user) {
        //获取colSql
        DistinctQueryParam colDistinctQueryParam = new DistinctQueryParam();
        colDistinctQueryParam.setColumns(executeParam.getColGroups());
        colDistinctQueryParam.setOrders(executeParam.getColOrders());
        colDistinctQueryParam.setFilters(executeParam.getFilters());
        colDistinctQueryParam.setParams(executeParam.getParams());
        String colSql = getDistinctQuerySql(isMaintainer, viewWithSource, colDistinctQueryParam, user);
        //获取rowSql
        DistinctQueryParam rowDistinctQueryParam = new DistinctQueryParam();
        rowDistinctQueryParam.setColumns(executeParam.getRowGroups());
        rowDistinctQueryParam.setFilters(executeParam.getFilters());
        rowDistinctQueryParam.setOrders(executeParam.getRowOrders());
        rowDistinctQueryParam.setParams(executeParam.getParams());
        String rowSql = getDistinctQuerySql(isMaintainer, viewWithSource, rowDistinctQueryParam, user);
        return Pair.of(colSql, rowSql);
    }

    @Override
    public String getDistinctQuerySql(boolean isMaintainer, ViewWithSource viewWithSource, DistinctQueryParam param, User user) {
        if (StringUtils.isEmpty(viewWithSource.getSql()) || param == null) {
            return null;
        }

        List<SqlVariable> variables = viewWithSource.getVariables();
        SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter);
        packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables, param.getParams(), null, user);

        String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

        Source source = viewWithSource.getSource();

        SqlUtils sqlUtils = this.sqlUtils.init(source);

        List<String> executeSqlList = sqlParseUtils.getSqls(srcSql, false);
        if (!CollectionUtils.isEmpty(executeSqlList)) {
            executeSqlList.forEach(sqlUtils::execute);
        }

        List<String> querySqlList = sqlParseUtils.getSqls(srcSql, true);
        String sql = null;
        if (!CollectionUtils.isEmpty(querySqlList)) {
            // 去重查询基于的sql
            String basesql = querySqlList.get(querySqlList.size() - 1);
            List<String> filters = SqlUtils.convertFilters(param.getFilters(), source);
            List<Order> orders = SqlUtils.getOrders(param.getOrders(), source.getJdbcUrl(), source.getDbVersion());
            String keywordPrefix = SqlUtils.getKeywordPrefix(source.getJdbcUrl(), source.getDbVersion());
            String keywordSuffix = SqlUtils.getKeywordSuffix(source.getJdbcUrl(), source.getDbVersion());
            sql = STUtils.newQueryDistinctSql(basesql, param.getColumns(), filters,
                    orders, keywordPrefix, keywordSuffix);
            // sql优化
            if (viewWithSource.getSource().getJdbcUrl().contains(DataTypeEnum.CLICKHOUSE.getDesc()) ||
                    viewWithSource.getSource().getJdbcUrl().contains(DataTypeEnum.MYSQL.getDesc())) {
                sql = SqlOptimizeUtil.optimizeCKSql(sql);
            }
        }
        return sql;
    }


    /**
     * 新建View
     *
     * @param viewCreate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public ViewWithSourceBaseInfo createView(ViewCreate viewCreate, User user) throws
            NotFoundException, UnAuthorizedException, ServerException {
        ProjectDetail projectDetail = projectService.getProjectDetail(viewCreate.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        if (projectPermission.getViewPermission() < UserPermissionEnum.WRITE.getPermission()) {
            throw new UnAuthorizedException("you have not permission to create view");
        }

        if (isExist(viewCreate.getName(), null, viewCreate.getProjectId())) {
            log.info("the view {} name is already taken", viewCreate.getName());
            throw new ServerException("the view name is already taken");
        }

        Source source = sourceMapper.getById(viewCreate.getSourceId());
        if (null == source) {
            log.info("source (:{}) not found", viewCreate.getSourceId());
            throw new NotFoundException("source is not found");
        }

        //测试连接
        boolean testConnection = sqlUtils.init(source).testConnection();

        if (testConnection) {
            View view = new View().createdBy(user.getId());
            BeanUtils.copyProperties(viewCreate, view);

            int insert = viewMapper.insert(view);
            if (insert > 0) {
                optLogger.info("view ({}) is create by user (:{})", view.toString(), user.getId());
                if (!CollectionUtils.isEmpty(viewCreate.getRoles()) && !StringUtils.isEmpty(viewCreate.getVariable())) {
                    checkAndInsertRoleParam(viewCreate.getVariable(), viewCreate.getRoles(), user, view);
                }

                SourceBaseInfo sourceBaseInfo = new SourceBaseInfo();
                BeanUtils.copyProperties(source, sourceBaseInfo);

                ViewWithSourceBaseInfo viewWithSource = new ViewWithSourceBaseInfo();
                BeanUtils.copyProperties(view, viewWithSource);
                viewWithSource.setSource(sourceBaseInfo);
                return viewWithSource;
            } else {
                throw new ServerException("create view fail");
            }
        } else {
            throw new ServerException("get source connection fail");
        }
    }


    /**
     * 更新View
     *
     * @param viewUpdate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean updateView(ViewUpdate viewUpdate, User user) throws
            NotFoundException, UnAuthorizedException, ServerException {

        View view = viewMapper.getById(viewUpdate.getId());
        if (null == view) {
            throw new NotFoundException("view is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(view.getProjectId(), user, false);

        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getViewPermission() < UserPermissionEnum.WRITE.getPermission()) {
            throw new UnAuthorizedException("you have not permission to update this view");
        }

        if (isExist(viewUpdate.getName(), viewUpdate.getId(), view.getProjectId())) {
            log.info("the view {} name is already taken", viewUpdate.getName());
            throw new ServerException("the view name is already taken");
        }

        Source source = sourceMapper.getById(viewUpdate.getSourceId());
        if (null == source) {
            log.info("source not found");
            throw new NotFoundException("source is not found");
        }

        //测试连接
        boolean testConnection = sqlUtils.init(source).testConnection();

        if (testConnection) {

            String originStr = view.toString();
            BeanUtils.copyProperties(viewUpdate, view);
            view.updatedBy(user.getId());

            int update = viewMapper.update(view);
            sqlUtils.refreshTableNames(view.getId());
            if (update > 0) {
                optLogger.info("view ({}) is updated by user(:{}), origin: ({})", view.toString(), user.getId(), originStr);
                if (CollectionUtils.isEmpty(viewUpdate.getRoles())) {
                    relRoleViewMapper.deleteByViewId(viewUpdate.getId());
                } else if (!StringUtils.isEmpty(viewUpdate.getVariable())) {
                    checkAndInsertRoleParam(viewUpdate.getVariable(), viewUpdate.getRoles(), user, view);
                }

                return true;
            } else {
                throw new ServerException("update view fail");
            }
        } else {
            throw new ServerException("get source connection fail");
        }
    }


    /**
     * 删除View
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteView(Long id, User user) throws
            NotFoundException, UnAuthorizedException, ServerException {

        View view = viewMapper.getById(id);

        if (null == view) {
            log.info("view (:{}) not found", id);
            throw new NotFoundException("view is not found");
        }

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(view.getProjectId(), user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            throw new UnAuthorizedException("you have not permission to delete this view");
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getViewPermission() < UserPermissionEnum.DELETE.getPermission()) {
            throw new UnAuthorizedException("you have not permission to delete this view");
        }

        List<Widget> widgets = widgetMapper.getWidgetsByWiew(id);
        if (!CollectionUtils.isEmpty(widgets)) {
            throw new ServerException("The current view has been referenced, please delete the reference and then operate");
        }

        int i = viewMapper.deleteById(id);
        if (i > 0) {
            optLogger.info("view ( {} ) delete by user( :{} )", view.toString(), user.getId());
            relRoleViewMapper.deleteByViewId(id);
        }

        return true;
    }


    /**
     * 执行sql
     *
     * @param executeSql
     * @param user
     * @return
     */
    @Override
    public PaginateWithQueryColumns executeSql(ViewExecuteSql executeSql, User user) throws
            NotFoundException, UnAuthorizedException, ServerException {

        Source source = sourceMapper.getById(executeSql.getSourceId());
        if (null == source) {
            throw new NotFoundException("source is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(source.getProjectId(), user, false);

        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        if (projectPermission.getSourcePermission() == UserPermissionEnum.HIDDEN.getPermission()
                || projectPermission.getViewPermission() < UserPermissionEnum.WRITE.getPermission()) {
            throw new UnAuthorizedException("you have not permission to execute sql");
        }

        //结构化Sql
        PaginateWithQueryColumns paginateWithQueryColumns = null;
        try {
            SqlEntity sqlEntity = sqlParseUtils.parseSql(executeSql.getSql(), executeSql.getVariables(), sqlTempDelimiter);
            if (null != sqlUtils && null != sqlEntity) {
                if (!StringUtils.isEmpty(sqlEntity.getSql())) {

                    if (isMaintainer(user, projectDetail)) {
                        sqlEntity.setAuthParams(null);
                    }

                    if (!CollectionUtils.isEmpty(sqlEntity.getQuaryParams())) {
                        sqlEntity.getQuaryParams().forEach((k, v) -> {
                            if (v instanceof List && ((List) v).size() > 0) {
                                v = ((List) v).stream().collect(Collectors.joining(COMMA)).toString();
                            }
                            sqlEntity.getQuaryParams().put(k, v);
                        });
                    }

                    String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

                    SqlUtils sqlUtils = this.sqlUtils.init(source);

                    List<String> executeSqlList = sqlParseUtils.getSqls(srcSql, false);

                    List<String> querySqlList = sqlParseUtils.getSqls(srcSql, true);

                    if (!CollectionUtils.isEmpty(executeSqlList)) {
                        executeSqlList.forEach(sql -> sqlUtils.execute(sql));
                    }
                    if (!CollectionUtils.isEmpty(querySqlList)) {
                        for (String sql : querySqlList) {
                            paginateWithQueryColumns = sqlUtils.syncQuery4Paginate(sql, executeSql.getPageNo(),
                                    executeSql.getPageSize(), null, executeSql.getLimit(), null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ServerException(e.getMessage());
        }
        return paginateWithQueryColumns;
    }

    private boolean isMaintainer(User user, ProjectDetail projectDetail) {
        return projectService.isMaintainer(projectDetail, user);
    }

    /**
     * 返回view源数据集
     *
     * @param id
     * @param executeParam
     * @param user
     * @return
     */
    @Override
    public Paginate<Map<String, Object>> getData(Long id, ViewExecuteParam executeParam, User user) throws
            NotFoundException, UnAuthorizedException, ServerException, SQLException {
        if (null == executeParam || (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators()))) {
            return null;
        }

        ViewWithSource viewWithSource = viewMapper.getViewWithSource(id);
        if (null == viewWithSource) {
            log.info("view (:{}) not found", id);
            throw new NotFoundException("view is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(viewWithSource.getProjectId(), user, false);

        boolean allowGetData = projectService.allowGetData(projectDetail, user);

        if (!allowGetData) {
            throw new UnAuthorizedException("you have not permission to get data");
        }

        boolean maintainer = projectService.isMaintainer(projectDetail, user);
        if (CollectionUtils.isEmpty(executeParam.getRowGroups()) && CollectionUtils.isEmpty(executeParam.getColGroups())) {
            // 只取最后一条作为排序条件
            List<Order> orders = executeParam.getOrders();
            if (!CollectionUtils.isEmpty(orders)) {
                executeParam.setOrders(Lists.newArrayList(orders.get(orders.size() - 1)));
            }
            return getResultDataList(maintainer, viewWithSource, executeParam, user);
        } else { // 交叉数据查询分支
            return getCrossResultDataList(maintainer, viewWithSource, executeParam, user);
        }

    }

    /**
     * 获取结果集
     *
     * @param isMaintainer
     * @param viewWithSource
     * @param executeParam
     * @param user
     * @return
     * @throws ServerException
     */
    @Override
    public PaginateWithQueryColumns getResultDataList(boolean isMaintainer, ViewWithSource
            viewWithSource, ViewExecuteParam executeParam, User user) throws ServerException, SQLException {
        PaginateWithQueryColumns paginate = null;
        if (null == executeParam) {
            return null;
        }

        if (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators())) {
            return null;
        }

        if (null == viewWithSource.getSource()) {
            throw new NotFoundException("source is not found");
        }

        try {

            if (!StringUtils.isEmpty(viewWithSource.getSql())) {
                //解析变量
                List<SqlVariable> variables = viewWithSource.getVariables();
                //解析sql
                SqlEntity sqlEntity = sqlParseUtils.parseSql(viewWithSource.getSql(), variables, sqlTempDelimiter);
                //列权限（只记录被限制访问的字段）
                Set<String> excludeColumns = new HashSet<>();
                packageParams(isMaintainer, viewWithSource.getId(), sqlEntity, variables, executeParam.getParams(), excludeColumns, user);
                String srcSql = sqlParseUtils.replaceParams(sqlEntity.getSql(), sqlEntity.getQuaryParams(), sqlEntity.getAuthParams(), sqlTempDelimiter);

                Source source = viewWithSource.getSource();

                SqlUtils sqlUtils = this.sqlUtils.init(source);


                List<String> executeSqlList = sqlParseUtils.getSqls(srcSql, false);
                if (!CollectionUtils.isEmpty(executeSqlList)) {
                    executeSqlList.forEach(sql -> sqlUtils.execute(sql));
                }


                List<String> querySqlList = sqlParseUtils.getSqls(srcSql, true);
                if (!CollectionUtils.isEmpty(querySqlList)) {
                    DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(source.getJdbcUrl());
                    AbstractBuildQuerySqlTemplate buildSqlTemplate = buildSqlTemplateRegister.getBuildSqlTemplate(dataTypeEnum.getFeature());
                    if (null == buildSqlTemplate) {
                        throw new NotFoundException("未找到对应数据库类型的sql模板：" + dataTypeEnum.getFeature());
                    }
                    buildSqlTemplate.build(querySqlList, source, executeParam);
                    executeParam.addExcludeColumn(excludeColumns, source.getJdbcUrl(), source.getDbVersion());

                    // 生成MD5key
                    StringBuilder slatBuilder = new StringBuilder();
                    slatBuilder.append(executeParam.getPageNo());
                    slatBuilder.append(MINUS);
                    slatBuilder.append(executeParam.getLimit());
                    slatBuilder.append(MINUS);
                    slatBuilder.append(executeParam.getPageSize());
                    excludeColumns.forEach(slatBuilder::append);

                    String md5Key = MD5Util.getMD5(slatBuilder.toString() + querySqlList.get(querySqlList.size() - 1), true, 32);

                    paginate = getRedisCachePaginate(viewWithSource.getId(), md5Key, executeParam.getFlush(), executeParam.getFlushInterval(), sqlUtils.jdbcTemplate());
                    if(null != paginate) {
                        return paginate;
                    }

                    for (String sql : querySqlList) {
                        paginate = sqlUtils.syncQuery4Paginate(
                                sql,
                                executeParam.getPageNo(),
                                executeParam.getPageSize(),
                                executeParam.getTotalCount(),
                                executeParam.getLimit(),
                                excludeColumns);
                    }

                    // 更新redis缓存
                    updateDataPaginateRedisCache(paginate, md5Key, executeParam.getFlushInterval());
                }
            }
        } catch (Exception e) {
            throw new ServerException("query data error, view id is " + viewWithSource.getId() + ": " + e.getMessage(), e);
        }

        return paginate;
    }


    @Override
    @Deprecated
    public List<Map<String, Object>> getDistinctValue(Long id, DistinctParam param, User user) throws
            NotFoundException, ServerException, UnAuthorizedException {
        ViewWithSource viewWithSource = viewMapper.getViewWithSource(id);
        if (null == viewWithSource) {
            log.info("view (:{}) not found", id);
            throw new NotFoundException("view is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(viewWithSource.getProjectId(), user, false);

        boolean allowGetData = projectService.allowGetData(projectDetail, user);

        if (!allowGetData) {
            throw new UnAuthorizedException();
        }

        return getDistinctValueData(projectService.isMaintainer(projectDetail, user), viewWithSource, param, user);
    }


    @Override
    @Deprecated
    public List<Map<String, Object>> getDistinctValueData(boolean isMaintainer, ViewWithSource
            viewWithSource, DistinctParam param, User user) throws ServerException {
        List<Map<String, Object>> distinctList = null;
        try {
            String sql = getDistinctSql(isMaintainer, viewWithSource, param, user);
            if (!StringUtils.isEmpty(sql)) {
                Set<String> tableNameSet = this.sqlUtils.getTableNames(viewWithSource.getId());
                String md5Key;
                if (!"max".equals(param.getType()) && !"min".equals(param.getType())) {
                    md5Key = MD5Util.getMD5("DISTINCI" + sql, true, 32);
                } else {
                    md5Key = MD5Util.getMD5(param.getType().toUpperCase() + sql, true, 32);
                }
                boolean cacheAbleFlag = judgeCacheAble(md5Key, tableNameSet);
                if (cacheAbleFlag) {
                    try {
                        String cache = redisClient.get(RedisConsts.DAVINCI_VIEW_CACHE, md5Key);
                        List result = null;
                        if (cache != null) {
                            result = Lists.newArrayList(JSON.parseArray(cache));
                        }
                        if (null != result) {
                            return result;
                        }
                    } catch (Exception e) {
                        log.warn("get distinct value by cache: {}", e.getMessage());
                    }
                }
                SqlUtils sqlUtils = this.sqlUtils.init(viewWithSource.getSource());
                distinctList = sqlUtils.query4List(sql, -1);
                try {
                    String value = JSON.toJSONString(distinctList);
                    redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE, cacheTime, value, md5Key);
                    String nowDate = DateUtils.toDateString(new Date());
                    redisClient.setex(RedisConsts.MD5KEY_UPDATETIME, cacheTime, nowDate, md5Key);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            optLogger.error(e.getMessage(), e);
            throw new ServerException(e.getMessage());
        }
        return distinctList;
    }


    private Set<String> getExcludeColumnsViaOneView(List<RelRoleView> roleViewList) {
        if (!CollectionUtils.isEmpty(roleViewList)) {
            Set<String> columns = new HashSet<>();
            boolean isFullAuth = false;
            for (RelRoleView r : roleViewList) {
                if (!StringUtils.isEmpty(r.getColumnAuth())) {
                    columns.addAll(JSONObject.parseArray(r.getColumnAuth(), String.class));
                } else {
                    isFullAuth = true;
                    break;
                }
            }
            return isFullAuth ? null : columns;
        }
        return null;
    }


    private List<SqlVariable> getQueryVariables(List<SqlVariable> variables) {
        if (!CollectionUtils.isEmpty(variables)) {
            return variables.stream().filter(v -> QUERYVAR == SqlVariableTypeEnum.typeOf(v.getType())).collect(Collectors.toList());
        }
        return null;
    }

    private List<SqlVariable> getAuthVariables(List<RelRoleView> roleViewList, List<SqlVariable> variables) {
        if (!CollectionUtils.isEmpty(variables)) {

            List<SqlVariable> list = new ArrayList<>();

            variables.forEach(v -> {
                if (null != v.getChannel()) {
                    list.add(v);
                }
            });

            if (!CollectionUtils.isEmpty(roleViewList)) {
                Map<String, SqlVariable> map = new HashMap<>();

                List<SqlVariable> authVarables = variables.stream().filter(v -> AUTHVARE == SqlVariableTypeEnum.typeOf(v.getType())).collect(Collectors.toList());
                authVarables.forEach(v -> map.put(v.getName(), v));
                List<SqlVariable> dacVars = authVarables.stream().filter(v -> null != v.getChannel() && !v.getChannel().getBizId().equals(0L)).collect(Collectors.toList());

                roleViewList.forEach(r -> {
                    if (!StringUtils.isEmpty(r.getRowAuth())) {
                        List<AuthParamValue> authParamValues = JSONObject.parseArray(r.getRowAuth(), AuthParamValue.class);
                        authParamValues.forEach(v -> {
                            if (map.containsKey(v.getName())) {
                                SqlVariable sqlVariable = map.get(v.getName());
                                if (v.isEnable()) {
                                    if (CollectionUtils.isEmpty(v.getValues())) {
                                        List values = new ArrayList<>();
                                        values.add(NO_AUTH_PERMISSION);
                                        sqlVariable.setDefaultValues(values);
                                    } else {
                                        List<Object> values = sqlVariable.getDefaultValues() == null ? new ArrayList<>() : sqlVariable.getDefaultValues();
                                        values.addAll(v.getValues());
                                        sqlVariable.setDefaultValues(values);
                                    }
                                } else {
                                    sqlVariable.setDefaultValues(new ArrayList<>());
                                }
                                list.add(sqlVariable);
                            }
                        });
                    } else {
                        dacVars.forEach(v -> list.add(v));
                    }
                });
            }
            return list;
        }
        return null;
    }


    private void packageParams(boolean isProjectMaintainer, Long viewId, SqlEntity
            sqlEntity, List<SqlVariable> variables, List<Param> paramList, Set<String> excludeColumns, User user) {

        List<SqlVariable> queryVariables = getQueryVariables(variables);
        List<SqlVariable> authVariables = null;

        if (!isProjectMaintainer) {
            List<RelRoleView> roleViewList = relRoleViewMapper.getByUserAndView(user.getId(), viewId);
            authVariables = getAuthVariables(roleViewList, variables);
            if (null != excludeColumns) {
                Set<String> eclmns = getExcludeColumnsViaOneView(roleViewList);
                if (!CollectionUtils.isEmpty(eclmns)) {
                    excludeColumns.addAll(eclmns);
                }
            }
        }

        //查询参数
        if (!CollectionUtils.isEmpty(queryVariables) && !CollectionUtils.isEmpty(sqlEntity.getQuaryParams())) {
            if (!CollectionUtils.isEmpty(paramList)) {
                Map<String, List<SqlVariable>> map = queryVariables.stream().collect(Collectors.groupingBy(SqlVariable::getName));
                paramList.forEach(p -> {
                    if (map.containsKey(p.getName())) {
                        List<SqlVariable> list = map.get(p.getName());
                        if (!CollectionUtils.isEmpty(list)) {
                            SqlVariable v = list.get(list.size() - 1);
                            if (null == sqlEntity.getQuaryParams()) {
                                sqlEntity.setQuaryParams(new HashMap<>());
                            }
//                            String[] split = p.getValue().split("&");
//                            List<Object> params = Lists.newArrayList();
//                            for (int i = 0; i < split.length; i++) {
//                                Object value = SqlVariableValueTypeEnum.getValue(v.getValueType(), split[i], v.isUdf());
//                                params.add(value);
//                            }

                            sqlEntity.getQuaryParams().put(p.getName().trim(), SqlVariableValueTypeEnum.getValue(v.getValueType(), p.getValue(), v.isUdf()));
//                            sqlEntity.getQuaryParams().put(p.getName().trim(), params);
                        }
                    }
                });
            }

            sqlEntity.getQuaryParams().forEach((k, v) -> {
                if (v instanceof List && ((List) v).size() > 0) {
                    v = ((List) v).stream().collect(Collectors.joining(COMMA)).toString();
                }
                sqlEntity.getQuaryParams().put(k, v);
            });
        }

        //如果当前用户是project的维护者，直接不走行权限
        if (isProjectMaintainer) {
            sqlEntity.setAuthParams(null);
            return;
        }

        //权限参数
        if (!CollectionUtils.isEmpty(authVariables)) {
            CountDownLatch countDownLatch = new CountDownLatch(authVariables.size());
            Map<String, Set<String>> map = new Hashtable<>();
            List<Future> futures = new ArrayList<>(authVariables.size());
            try {
                authVariables.forEach(sqlVariable -> {
                    try {
                        futures.add(executorService.submit(() -> {
                            if (null != sqlVariable) {
                                Set<String> vSet = null;
                                if (map.containsKey(sqlVariable.getName().trim())) {
                                    vSet = map.get(sqlVariable.getName().trim());
                                } else {
                                    vSet = new HashSet<>();
                                }

                                List<String> values = sqlParseUtils.getAuthVarValue(sqlVariable, user.getEmail());
                                if (null == values) {
                                    vSet.add(NO_AUTH_PERMISSION);
                                } else if (!values.isEmpty()) {
                                    vSet.addAll(values);
                                }
                                map.put(sqlVariable.getName().trim(), vSet);
                            }
                        }));
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                try {
                    for (Future future : futures) {
                        future.get();
                    }
                    countDownLatch.await();
                } catch (ExecutionException e) {
                    executorService.shutdownNow();
                    throw (ServerException) e.getCause();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                executorService.shutdown();
            }

            if (!CollectionUtils.isEmpty(map)) {
                if (null == sqlEntity.getAuthParams()) {
                    sqlEntity.setAuthParams(new HashMap<>());
                }
                map.forEach((k, v) -> sqlEntity.getAuthParams().put(k, new ArrayList<String>(v)));
            }
        } else {
            sqlEntity.setAuthParams(null);
        }
    }


    private void checkAndInsertRoleParam(String sqlVarible, List<RelRoleViewDto> roles, User user, View view) {
        List<SqlVariable> variables = JSONObject.parseArray(sqlVarible, SqlVariable.class);

        if (CollectionUtils.isEmpty(roles)) {
            relRoleViewMapper.deleteByViewId(view.getId());
        } else {
            new Thread(() -> {
                Set<String> vars = null, columns = null;

                if (!CollectionUtils.isEmpty(variables)) {
                    vars = variables.stream().map(SqlVariable::getName).collect(Collectors.toSet());
                }
                if (!StringUtils.isEmpty(view.getModel())) {
                    columns = JSONObject.parseObject(view.getModel(), HashMap.class).keySet();
                }

                Set<String> finalColumns = columns;
                Set<String> finalVars = vars;

                List<RelRoleView> relRoleViews = new ArrayList<>();
                roles.forEach(r -> {
                    if (r.getRoleId().longValue() > 0L) {
                        String rowAuth = null, columnAuth = null;
                        if (!StringUtils.isEmpty(r.getRowAuth())) {
                            JSONArray rowAuthArray = JSONObject.parseArray(r.getRowAuth());
                            if (!CollectionUtils.isEmpty(rowAuthArray)) {
                                JSONArray newArray = new JSONArray();
                                for (int i = 0; i < rowAuthArray.size(); i++) {
                                    JSONObject jsonObject = rowAuthArray.getJSONObject(i);
                                    String name = jsonObject.getString(SQL_VARABLE_KEY);
                                    if (finalVars.contains(name)) {
                                        newArray.add(jsonObject);
                                    }
                                }
                                rowAuth = newArray.toJSONString();
                                newArray.clear();
                            }
                        }

                        if (null != finalColumns && !StringUtils.isEmpty(r.getColumnAuth())) {
                            List<String> clms = JSONObject.parseArray(r.getColumnAuth(), String.class);
                            List<String> collect = clms.stream().filter(c -> finalColumns.contains(c)).collect(Collectors.toList());
                            columnAuth = JSONObject.toJSONString(collect);
                        }

                        RelRoleView relRoleView = new RelRoleView(view.getId(), r.getRoleId(), rowAuth, columnAuth)
                                .createdBy(user.getId());
                        relRoleViews.add(relRoleView);
                    }
                });
                if (!CollectionUtils.isEmpty(relRoleViews)) {
                    relRoleViewMapper.insertBatch(relRoleViews);
                }
            }).start();
        }
    }

    /**
     * @param executeParam
     * @param querySqlList
     * @param excludeColumns
     * @return
     */
    private SqlQueryInfo buildQueryInfo(ViewExecuteParam
                                                executeParam, List<String> querySqlList, Set<String> excludeColumns, Long viewId) {
        StringBuilder slatBuilder = new StringBuilder();
        slatBuilder.append(executeParam.getPageNo());
        slatBuilder.append(MINUS);
        slatBuilder.append(executeParam.getLimit());
        slatBuilder.append(MINUS);
        slatBuilder.append(executeParam.getPageSize());
        excludeColumns.forEach(slatBuilder::append);

        String md5Key = MD5Util.getMD5(slatBuilder.toString() + querySqlList.get(querySqlList.size() - 1), true, 32);
        String sql = querySqlList.get(querySqlList.size() - 1);
        // 获取sql对应的tablename
        String tableName = JSONObject.toJSONString(sqlUtils.getTableNames(viewId));

        ViewExecuteParam viewExecuteParam = new ViewExecuteParam(executeParam.getLimit(), executeParam.getPageNo(),
                executeParam.getPageSize(), executeParam.getTotalCount());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(Consts.STRING_VIEWEXECUTEPARAM, viewExecuteParam);
        jsonObject.put(Consts.STRING_SQL, sql);
        jsonObject.put(Consts.STRING_EXCLUDECOLUMNS, excludeColumns);
        return null;
//        return new SqlQueryInfo(tableName, jsonObject.toJSONString(), md5Key, Consts.STRING_DATA);
    }

    /**
     * 用户判断是否走缓存
     * 走缓存的条件：查询缓存更新时间大于关联的数据表更新时间
     *
     * @param md5Key   查询语句的md5key
     * @param tableSet 查询管理的表集合
     * @return
     */
    private boolean judgeCacheAble(String md5Key, Set<String> tableSet) {
        boolean cacheFlag = true;
        try {
            // 数据缓存时间
            String timeStr = redisClient.get(RedisConsts.MD5KEY_UPDATETIME, md5Key);
            if (StringUtils.isEmpty(timeStr)) {
                cacheFlag = false;
            } else {
                // 缓存时间时间戳
                long redisTime = DateUtils.toDate(timeStr).getTime();

                for (String name : tableSet) {
                    // 数据库表更新时间
                    String updateTimeStr = redisClient.get(RedisConsts.TABLE_UPDATETIME, name);
                    if (updateTimeStr == null) {
                        continue;
                    }
                    // 数据库表更新时间时间戳
                    long updateTime = DateUtils.toDate(updateTimeStr).getTime();

                    if (updateTime > redisTime) {
                        cacheFlag = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return cacheFlag;
    }

    private List crossResultObjToList(Object object, DataTypeEnum dataTypeEnum, boolean isArray) {
        if(null == object) {
            return null;
        }
        if(isArray) {
            return (ArrayList) object;
        }
        if (DataTypeEnum.CLICKHOUSE.getFeature().equals(dataTypeEnum.getFeature())) {
            return convertObjToList(object);
        } else {
            return Lists.newArrayList(JSONArray.parseArray(object.toString()));
        }
    }

    /**
     * 对象转数据, object必须是数组类型
     * @param object
     * @return
     */
    private List convertObjToList(Object object) {
        if(null == object) {
            return null;
        }
        Class<?> componentClazz = object.getClass().getComponentType();
        if(null == componentClazz) {
            return null;
        }

        if(BASE_DATA_TYPE_LIST.contains(componentClazz.getTypeName())) { //基本数据类型
            List array = Lists.newArrayList();
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                array.add(Array.get(object, i));
            }
            return array;
        } else {
            return Lists.newArrayList((Object[]) object);
        }
    }

    private PaginateWithQueryColumns getRedisCachePaginate(Long viewId, String redisKey, boolean flush, Integer flushInterval, JdbcTemplate jdbcTemplate) {
        log.info("getRedisCachePaginate, viewId="+viewId);
        Set<String> tableSet = this.sqlUtils.getTableNames(viewId);
        if(flush || !judgeCacheAble(redisKey, tableSet)) {
            return null;
        };

        // viewId刷新列表
        if(!StringUtils.isEmpty(flushViewList)) {
            log.info("flushViewList=" + flushViewList);
            String[] split = flushViewList.split(",");
            List<String> flushViewIds = Lists.newArrayList(split);
            if(flushViewIds.contains(viewId.toString())) {
                log.info("flush success, viewId=" + viewId);
                return null;
            }
        }

        // 仅间隔时间内的缓存有效
        if(flushInterval > 0) {
            String intervalValue = redisClient.get(RedisConsts.DAVINCI_VIEW_CACHE_INTERVAL, flushInterval.toString(), redisKey);
            if(intervalValue == null) {
                String countCacheStr = redisClient.get(RedisConsts.SQL_RECORD_COUNT, redisKey);
                View view = viewMapper.getById(viewId);
                Integer count = 0;
                try {
                    String countSql = this.sqlUtils.getCountSql(view.getSql());
                    Object o = jdbcTemplate.queryForObject(countSql, Object.class);
                    count = Integer.parseInt(String.valueOf(o));
                } catch (Exception e) {
                    optLogger.error(e.getMessage());
                }
                redisClient.setex(RedisConsts.SQL_RECORD_COUNT, cacheTime, count.toString(), redisKey);
                if(countCacheStr == null || count != Integer.parseInt(countCacheStr)) {
                    return null;
                }
                redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE_INTERVAL, flushInterval, "true", flushInterval.toString(), redisKey);

            }
        }
        PaginateWithQueryColumns paginate = null;
        try {
            JSONObject object = null;
            String cache = redisClient.get(RedisConsts.DAVINCI_VIEW_CACHE, redisKey);
            if (cache != null) {
                object = JSONObject.parseObject(cache);
            }
            if (null != object) {
                paginate = object.toJavaObject(PaginateWithQueryColumns.class);

            }
        } catch (Exception e) {
            log.warn("get data by cache: {}", e.getMessage());
            throw new ServerException(e.getMessage(), e);
        }
        return paginate;
    }

    private void updateDataPaginateRedisCache(PaginateWithQueryColumns paginate ,String redisKey, Integer flushInterval) {
        if (!CollectionUtils.isEmpty(paginate.getResultList())) {
            String value = JSONObject.toJSONStringWithDateFormat(paginate, null,
                    SerializerFeature.WriteMapNullValue);
            redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE, cacheTime, value, redisKey);
            String nowDate = DateUtils.toDateString(new Date());
            redisClient.setex(RedisConsts.MD5KEY_UPDATETIME, cacheTime, nowDate, redisKey);
            if(flushInterval > 0) {
                redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE_INTERVAL, flushInterval, "true", flushInterval.toString(), redisKey);
            }
        }
    }

}

