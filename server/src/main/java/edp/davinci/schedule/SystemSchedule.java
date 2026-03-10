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

package edp.davinci.schedule;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.iflytek.edu.zx.redis.client.RedisClient;
import com.iflytek.edu.zx.table.admin.service.ReportService;

import edp.core.consts.Consts;
import edp.core.consts.RedisConsts;
import edp.core.exception.ServerException;
import edp.core.utils.CollectionUtils;
import edp.core.utils.DateUtils;
import edp.core.utils.FileUtils;
import edp.core.utils.QuartzHandler;
import edp.core.utils.RedisUtils;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.dao.CollectPaperMapper;
import edp.davinci.dao.CollectPaperSubmitMapper;
import edp.davinci.dao.CronJobMapper;
import edp.davinci.dao.DashboardMapper;
import edp.davinci.dao.DashboardPortalMapper;
import edp.davinci.dao.DashboardPublishMapper;
import edp.davinci.dao.MemDashboardWidgetMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.ProjectMapper;
import edp.davinci.dao.ShareDownloadRecordMapper;
import edp.davinci.dao.SourceMapper;
import edp.davinci.dao.SqlQueryInfoMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.model.CollectPaper;
import edp.davinci.model.CollectPaperSubmit;
import edp.davinci.model.CronJob;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.service.SqlQueryInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SystemSchedule {

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private CronJobMapper cronJobMapper;

    @Autowired
    private QuartzHandler quartzHandler;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ShareDownloadRecordMapper shareDownloadRecordMapper;

    @Autowired
    private SqlQueryInfoMapper sqlQueryInfoMapper;

    @Autowired
    private SqlQueryInfoService sqlQueryInfoService;

    @Autowired
    private DashboardPublishMapper dashboardPublishMapper;

    @Resource
    private ReportService reportService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private DashboardPortalMapper dashboardPortalMapper;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private SourceMapper sourceMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private CollectPaperMapper collectPaperMapper;

    @Autowired
    private CollectPaperSubmitMapper collectPaperSubmitMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Scheduled(cron = "0 0 1 * * *")
    public void clearTempDir() {

        //下载内容文件保留7天，记录保留1月
        String downloadDir = fileUtils.fileBasePath + Consts.DIR_DOWNLOAD + DateUtils.getTheDayBeforAWeekYYYYMMDD();
        String tempDir = fileUtils.fileBasePath + Consts.DIR_TEMPL + DateUtils.getTheDayBeforNowDateYYYYMMDD();
        String csvDir = fileUtils.fileBasePath + File.separator + FileTypeEnum.CSV.getType();

        final String download = fileUtils.formatFilePath(downloadDir);
        final String temp = fileUtils.formatFilePath(tempDir);
        final String csv = fileUtils.formatFilePath(csvDir);

        new Thread(() -> FileUtils.deleteDir(new File(download))).start();
        new Thread(() -> FileUtils.deleteDir(new File(temp))).start();
        new Thread(() -> FileUtils.deleteDir(new File(csv))).start();
    }

    @Scheduled(cron = "0 0/2 * * * *")
    public void stopCronJob() {

//        if (redisUtils.isRedisEnable()) {
//            return;
//        }
//
        List<CronJob> jobs = cronJobMapper.getStopedJob();
        if (!CollectionUtils.isEmpty(jobs)) {
            for (CronJob job : jobs) {
                try {
                    quartzHandler.removeJob(job);
                } catch (ServerException e) {
                }
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void clearShareDownloadRecord() {

        List<ShareDownloadRecord> records = shareDownloadRecordMapper.getShareDownloadRecords();    //deleting
        for (ShareDownloadRecord record : records) {
            deleteFile(new File(record.getPath()));
        }

        shareDownloadRecordMapper.deleteByCondition();
    }

    /**
     * 每天凌晨0点执行决策平台缓存信息的维护
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void clearCache() {
        List<String> davinciReportUrls = reportService.getSimpleDavinciReportUrl();
        Set<Long> dashboardIds = dashboardPublishMapper.getAllDashboardIds();
        davinciReportUrls.forEach(url -> {
            redisClient.del(RedisConsts.DATAV_UPDATE_REPORT_INFO, url);
        });
        dashboardIds.forEach(id -> {
            redisClient.del(RedisConsts.DAVINCI_DASHBOARD_UPDATE_INFO, id.toString());
        });
    }

    /**
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void collectPaperStatuCheck() {
        List<CollectPaper> papers = collectPaperMapper.getSimplesPapers();
        if (CollectionUtils.isEmpty(papers)) {
            return;
        }

        // 检查过期的paper
        List<Long> paperIds = Lists.newArrayList();
        papers.forEach(paper -> {
            paper.getDeadline();
            if (null != paper.getDeadline() && paper.getStatus() != 0) {
                if (paper.getDeadline().getTime() <= System.currentTimeMillis()) {
                    paperIds.add(paper.getId());
                }
            }
        });

        // 将状态置为结束征集
        Short status = 0; // 结束征集
        if (!CollectionUtils.isEmpty(paperIds)) {
            collectPaperMapper.updateStatus(paperIds, status);
        }
    }

    /**
     * 每天凌晨0点执行决策平台缓存信息的维护
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void submitStatuCheck() {
        // 获取数据有效期
        List<CollectPaper> papers = collectPaperMapper.getSimplesPapers();

        // 失效的提交集合
        List<Long> submitIds = Lists.newArrayList();
        if (CollectionUtils.isEmpty(papers)) {
            return;
        }

        papers.forEach(paper -> {
            if (paper.getExpires() != null) {
                List<CollectPaperSubmit> submits = collectPaperSubmitMapper.getSimpleSubmit();
                if (!CollectionUtils.isEmpty(submits)) {
                    submits.forEach(submit -> {
                        if (submit.getCreateTime().getTime() <= (System.currentTimeMillis() + 24 * 60 * 60 * 1000 * paper.getExpires())) {
                            submitIds.add(submit.getId());
                        }
                    });
                }
            }
        });

        if (!CollectionUtils.isEmpty(submitIds)) {
            collectPaperSubmitMapper.removeBatch(submitIds);
        }
    }

    /**
     * 每周日0点处理逻辑删除的project、dashboardportal、dashboard
     */
//    @Scheduled(cron = "0 0 0 ? * 1")
    public void handleLogicDeleted() {
        // @TODO 获取逻辑删除的project, 清理
        Set<Long> projectIds = projectMapper.getLogicDeletedIds();
        if (!CollectionUtils.isEmpty(projectIds)) {
            projectIds.forEach(id -> {
                projectMapper.deleteById(id);
                dashboardPortalMapper.deleteByProject(id);
                dashboardMapper.deleteByProject(id);
                dashboardPublishMapper.deleteByProject(id);
                widgetMapper.deleteByProject(id);
                widgetPublishMapper.deleteByProject(id);
                viewMapper.deleteByPorject(id);
                sourceMapper.deleteByProject(id);
            });
        }

        // @TODO 获取逻辑删除的dashboardportal, 清理
        Set<Long> portalIds = dashboardPortalMapper.getLogicDeletedIds();
        portalIds.forEach(id -> {
            dashboardPortalMapper.deleteById(id);
            dashboardMapper.deleteByPortalId(id);
            dashboardPublishMapper.deleteByPortalId(id);
            memDashboardWidgetMapper.deleteByPortalId(id);
            memDashboardWidgetPublishMapper.deleteByPortalId(id);
        });

        // @TODO 获取逻辑删除的dashboard, 清理
        Set<Long> dashboardIds = dashboardMapper.getLogicDeletedIds();
        dashboardIds.forEach(id -> {
            dashboardMapper.deleteById(id);
            dashboardMapper.deleteByParentId(id);
            dashboardPublishMapper.deleteById(id);
            memDashboardWidgetMapper.deleteByDashboardId(id);
            memDashboardWidgetPublishMapper.deleteByDashboardId(id);
        });

    }

    private void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            String fileName = file.getName();
            if ("download".equals(fileName)) {
                return;
            }

            File[] childs = file.listFiles();
            if (childs.length == 0) {
                file.delete();
                deleteFile(file.getParentFile());
            } else {
                return;
            }

        } else {
            File parentDir = file.getParentFile();
            File[] childs = parentDir.listFiles();
            if (childs.length == 1) {
                file.delete();
                deleteFile(parentDir);
            } else {
                file.delete();
            }
        }
    }

}
