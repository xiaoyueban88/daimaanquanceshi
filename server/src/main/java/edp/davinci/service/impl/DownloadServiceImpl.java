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
import java.sql.Connection;
import java.util.Date;
import java.util.List;

import com.alibaba.druid.util.StringUtils;

import com.mchange.v1.db.sql.ConnectionUtils;
import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.davinci.core.enums.ActionEnum;
import edp.davinci.core.enums.DownloadTaskStatus;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.dao.DownloadRecordMapper;
import edp.davinci.dao.UserMapper;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.model.DownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.DashboardDownloadTemplateService;
import edp.davinci.service.DownloadService;
import edp.davinci.service.excel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author daemon
 * @Date 19/5/28 10:04
 * To change this template use File | Settings | File Templates.
 */
@Service
@Slf4j
public class DownloadServiceImpl extends DownloadCommonService implements DownloadService {

    @Autowired
    private DownloadRecordMapper downloadRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DashboardDownloadTemplateService dashboardDownloadTemplateService;

    @Override
    public List<DownloadRecord> queryDownloadRecordPage(Long userId) {
        return downloadRecordMapper.getDownloadRecordsByUser(userId);
    }

    @Override
    public DownloadRecord downloadById(Long id, String token) throws UnAuthorizedException {
        if (StringUtils.isEmpty(token)) {
            throw new UnAuthorizedException();
        }

        String username = tokenUtils.getUsername(token);
        if (StringUtils.isEmpty(username)) {
            throw new UnAuthorizedException();
        }

        User user = userMapper.selectByUsername(username);
        if (null == user) {
            throw new UnAuthorizedException();
        }

        DownloadRecord record = downloadRecordMapper.getById(id);

        if (!record.getUserId().equals(user.getId())) {
            throw new UnAuthorizedException();
        }

        record.setLastDownloadTime(new Date());
        record.setStatus(DownloadTaskStatus.DOWNLOADED.getStatus());
        downloadRecordMapper.updateById(record);
        return record;
    }

    @Override
    public Boolean submit(DownloadType type, Long id, User user, List<DownloadViewExecuteParam> params) {
        try {
            List<WidgetContext> widgetList = getWidgetContexts(type, id, user, params);
            DownloadRecord record = new DownloadRecord();
            record.setName(getDownloadFileName(type, id));
            record.setUserId(user.getId());
            record.setCreateTime(new Date());
            record.setStatus(DownloadTaskStatus.PROCESSING.getStatus());
            int insert = downloadRecordMapper.insert(record);
            MsgWrapper wrapper = new MsgWrapper(record, ActionEnum.DOWNLOAD, record.getId());
            WorkBookContext workBookContext = WorkBookContext.WorkBookContextBuilder.newBuildder()
                    .withWrapper(wrapper)
                    .withWidgets(widgetList)
                    .withUser(user)
                    .withResultLimit(resultLimit)
                    .withTaskKey("DownloadTask_" + id)
                    .build();
            // 如果type是dashboard,需要查询dashboard是否有模板
            if (DownloadType.DashBoard == type) {
                InputStream downloadTemplate = dashboardDownloadTemplateService.getDownloadTemplate(id);
                workBookContext.setTemplate(downloadTemplate);
            }

            ExecutorUtil.submitWorkbookTask(workBookContext, null);

            log.info("Download task submit: {}", wrapper);
        } catch (Exception e) {
            log.error("submit download task error,e=", e);
            return false;
        }
        return true;
    }
}
