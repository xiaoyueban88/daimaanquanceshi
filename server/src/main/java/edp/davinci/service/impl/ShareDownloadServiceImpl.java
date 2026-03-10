/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.iflytek.edu.elp.common.util.JSONUtils;
import com.iflytek.edu.elp.common.util.StringUtils;
import com.iflytek.edu.zx.filecloud.service.FileCloudService;
import edp.core.consts.Consts;
import edp.core.utils.CollectionUtils;
import edp.core.utils.MatchUtils;
import edp.davinci.common.utils.StringUtil;
import edp.davinci.core.enums.ActionEnum;
import edp.davinci.core.enums.DownloadTaskStatus;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.core.model.SqlFilter;
import edp.davinci.dao.ShareDownloadRecordMapper;
import edp.davinci.dto.shareDto.PlatformShareInfo;
import edp.davinci.dto.shareDto.ShareInfo;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.model.DashBoardConfig;
import edp.davinci.model.DashBoardConfigFilter;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.BatchDownloadService;
import edp.davinci.service.DashboardDownloadTemplateService;
import edp.davinci.service.ShareDownloadService;
import edp.davinci.service.ShareService;
import edp.davinci.service.excel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class ShareDownloadServiceImpl extends DownloadCommonService implements ShareDownloadService {

    @Autowired
    private ShareDownloadRecordMapper shareDownloadRecordMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private PlatformShareServiceImpl platformShareService;

    @Autowired
    private DashboardDownloadTemplateService dashboardDownloadTemplateService;

    @Autowired
    private BatchDownloadService batchDownloadService;

    private final FileCloudService fileCloudService;

    @Value("${oss.filecloud.appId}")
    private String appId;

    @Override
    public Pair<Boolean, Long> submit(DownloadType downloadType, String uuid, String token, User user,
                                      List<DownloadViewExecuteParam> params, Boolean isAuthority) {
        // 解析token获取分享信息
        ShareInfo shareInfo = Boolean.TRUE.equals(isAuthority)
                ? platformShareService.getShareInfo(token) // 第三方授权分享
                : shareService.getShareInfo(token, user); // 普通分享
        Long shareId = shareInfo.getShareId();
        try {
            ShareDownloadRecord record = new ShareDownloadRecord();
            record.setUuid(uuid);
            record.setConfig(JSONArray.toJSONString(params));
            record.setName(getDownloadFileName(downloadType, shareId));
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            record.setCreateTime(new Date());
            shareDownloadRecordMapper.insertSelective(record);
            Long taskId = record.getId();
            return Pair.of(true, taskId);
        } catch (Exception e) {
            log.error("submit download task error,e=", e);
            return Pair.of(false, null);
        }
    }


    @Override
    public List<ShareDownloadRecord> queryDownloadRecordPage(String uuid, String token, User user, Integer downloadType) {
        List<ShareDownloadRecord> shareDownloadRecords = shareDownloadRecordMapper.getShareDownloadRecordsByUuid(uuid, downloadType);
        if (!CollectionUtils.isEmpty(shareDownloadRecords)) {
            shareDownloadRecords.stream()
                    .filter(record -> null != record.getPath())
                    .forEach(record -> record.setPath(fileCloudService.getFullUrl(appId, record.getPath())));
        }
        return shareDownloadRecords;
    }

    @Override
    public ShareDownloadRecord downloadById(String id, String uuid, String token, User user,
                                            Boolean isAuthority, String waterMarkText, DownloadType type) {
        // 解析token获取分享信息
        ShareInfo shareInfo = Boolean.TRUE.equals(isAuthority)
                ? platformShareService.getShareInfo(token) // 第三方授权分享
                : shareService.getShareInfo(token, user); // 普通分享
        User shareUser = shareInfo.getShareUser();
        Long shareId = shareInfo.getShareId();
        try {
            ShareDownloadRecord record = shareDownloadRecordMapper.getShareDownloadRecordBy(Long.valueOf(id), uuid);
            List<DownloadViewExecuteParam> params = Lists.newArrayList();
            JSONArray jsonArray = JSONArray.parseArray(record.getConfig());
            // paramIdSet, 用于去重
            Set<Long> paramIdSet = Sets.newHashSet();
            jsonArray.forEach(json -> {
                DownloadViewExecuteParam param = JSONObject.parseObject(json.toString(), DownloadViewExecuteParam.class);
                if (paramIdSet.add(param.getId())) {
                    params.add(param);
                }
            });

            List<WidgetContext> widgetList = getWidgetContexts(type, shareId, user == null ? shareUser : user, params);

            MsgWrapper<ShareDownloadRecord> wrapper = new MsgWrapper<>(record, ActionEnum.SHAREDOWNLOAD, uuid);
            WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuildder()
                    .withWrapper(wrapper)
                    .withWidgets(widgetList)
                    .withUser(shareUser)
                    .withWaterMarkText(waterMarkText)
                    .withResultLimit(resultLimit)
                    .withTaskKey("ShareDownload_" + uuid)
                    .build();
            // 如果下载的是dashboard,需要查看是否存在下载模板
            if (type == DownloadType.DashBoard) {
                InputStream downloadTemplate = dashboardDownloadTemplateService.getDownloadTemplate(shareId);
                if (null != downloadTemplate) {
                    workBookContext.setTemplate(downloadTemplate);
                }
            }

            Future<String> future = ExecutorUtil.submitWorkbookTask(workBookContext, null);

            String path = future.get();
            log.info("下载任务id:" + id +", 下载路径为:" + path);
            if (path != null && path.equals(record.getPath())) {
                record.setStatus(DownloadTaskStatus.DOWNLOADED.getStatus());
                shareDownloadRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.error("获取记录异常", e);
        }
        return shareDownloadRecordMapper.getShareDownloadRecordBy(Long.valueOf(id), uuid);
    }

    @Override
    public Pair<Boolean, Long> submitBatchTask(DownloadType downloadType, String uuid, String token, User user, List<DownloadViewExecuteParam> params, boolean isAuthority) {
        // 解析token获取分享信息
        ShareInfo shareInfo = Boolean.TRUE.equals(isAuthority)
                ? platformShareService.getShareInfo(token) // 第三方授权分享
                : shareService.getShareInfo(token, user); // 普通分享
        User shareUser = shareInfo.getShareUser();
        Long shareId = shareInfo.getShareId();
        try {
            String downloadFileName = getDownloadFileName(downloadType, shareId);
            ShareDownloadRecord record = new ShareDownloadRecord();
            record.setUuid(uuid);
            record.setConfig(JSONArray.toJSONString(params));
            record.setName(downloadFileName);
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            record.setCreateTime(new Date());
            record.setDownloadType(2);
            shareDownloadRecordMapper.insertSelective(record);
            Long taskId = record.getId();
            // 生成批量下载文件
            batchDownloadService.submitTask(downloadType, shareId, null != user ? user : shareUser, params, record, ActionEnum.SHAREDOWNLOAD, downloadFileName);

            return Pair.of(true, taskId);
        } catch (Exception e) {
            log.error("submit download task error,e=", e);
            return Pair.of(false, null);
        }
    }

    /**
     * 读取dashbaord下载文件名称配置, 并解析出对应的文件名称。 仅针对dashboard下载使用
     * @param context
     * @return
     */
    private String getFileName(WorkBookContext context) {
        try {
            List<SqlFilter> distinctFilters = new ArrayList<>(context.getWidgets().stream()
                    .filter(widget -> null != widget.getExecuteParam() && null != widget.getExecuteParam().getFilters())
                    .flatMap(widget -> widget.getExecuteParam().getFilters().stream().map(filter -> JSONUtils.parseObject(filter, SqlFilter.class)))
                    .collect(Collectors.toMap(SqlFilter::getKey, o -> o, (o1, o2) -> o1))
                    .values());
            List<WidgetContext> widgetList = context.getWidgets();
            if(CollectionUtils.isEmpty(widgetList)) {
                return null;
            }
            if(null == widgetList.get(0).getDashboard()) {
                return null;
            }
            String config = widgetList.get(0).getDashboard().getConfig();
            edp.davinci.service.excel.DashBoardConfig onceDashBoardConfig = JSONUtils.parseObject(config, edp.davinci.service.excel.DashBoardConfig.class);
            if(onceDashBoardConfig == null) {
                return null;
            }
            DownloadConfig downloadConfig = onceDashBoardConfig.getDownloadConfig();
            if(downloadConfig == null) {
                return null;
            }
            String result = downloadConfig.getOnceNameTemplate();
            if(StringUtils.isEmpty(result)) {
                return null;
            }
            edp.davinci.model.DashBoardConfig dashBoardConfig = JSONObject.parseObject(config, DashBoardConfig.class);
            int replaceNum = 0;
            for (DashBoardConfigFilter filter : dashBoardConfig.getFilters()) {
                SqlFilter sqlFilter = distinctFilters.stream().filter(f -> ObjectUtils.equals(f.getKey(), filter.getKey())).findFirst().orElse(null);
                if (null != sqlFilter) {
                    Object value = sqlFilter.getValue();
                    String name;
                    if (value instanceof List<?>) {
                        name = ((List) value).get(0).toString();
                    } else {
                        name = value.toString();
                    }
                    if (name.startsWith(Consts.APOSTROPHE) && name.endsWith(Consts.APOSTROPHE)) {
                        name = name.substring(1, name.length() - 1);
                    }
                    String key = "${" + filter.getKey() + "}";
                    if(result.contains(key)) {
                        result = result.replace(key, name);
                        replaceNum ++;
                    }

                }
            }
            if(replaceNum == 0) {
                return null;
            }
            if(StringUtils.isNotEmpty(result)) {
                result = result.replaceAll("\\$", "");
                result = MatchUtils.clearBracket(result, '{', '}');
            }
            return result;
        } catch (Exception e) {
            log.error("获取fileName异常", e);
            return null;
        }

    }
}
