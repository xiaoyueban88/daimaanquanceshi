package edp.davinci.kafka;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.redis.client.RedisClient;
import com.iflytek.edu.zx.table.admin.service.ReportService;

import edp.core.consts.RedisConsts;
import edp.core.utils.CollectionUtils;
import edp.core.utils.CommonUtils;
import edp.core.utils.DateUtils;
import edp.davinci.dao.DashboardPublishMapper;
import edp.davinci.service.DashboardService;
import edp.davinci.service.impl.EmailScheduleServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/9
 */

@Component
@Slf4j
public class KafkaProducer {
    @Autowired
    private DashboardService dashboardService;

    @Resource
    private ReportService reportService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private DashboardPublishMapper dashboardPublishMapper;

    @Autowired
    private EmailScheduleServiceImpl emailScheduleService;

    private static ExecutorService executorService = new ThreadPoolExecutor(4, 4,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    private static final Lock lock = new ReentrantLock();

    /**
     * @param tableName
     * @param updateTime
     * @param status
     * @param type
     */
    public void sendRefreshReportMsg(String tableName, String updateTime, String status, String type) {
        List<String> davinciReportUrls = reportService.getSimpleDavinciReportUrl();
//        // 获取决策平台看板包含的所有dashboardId
//        Set<Long> datavDashboardIds = Sets.newHashSet();
//        davinciReportUrls.forEach(url -> {
//            Set<Long> dashboardIds = getDashboardIdFromReportUrl(url);
//            datavDashboardIds.addAll(dashboardIds);
//
//        }

        // 获取所有dashboards
        Set<Long> allDashboardIds = dashboardPublishMapper.getAllDashboardIds();

        // 获取与更新的table相关联的dashboardId
        Set<Long> dashboardIds = dashboardService.getDashboardIdsByTableName(allDashboardIds, tableName);
        if (CollectionUtils.isEmpty(dashboardIds)) {
            return;
        }

        // 将已完成更新的dashboardId加入缓存
        String currentDayCache = redisClient.get(RedisConsts.CURRENT_DAY_UPDATED_DASHBOARD);
        int shouldCacheTime = DateUtils.getSeconds();
        if (StringUtils.isEmpty(currentDayCache)) {
            redisClient.setex(RedisConsts.CURRENT_DAY_UPDATED_DASHBOARD, shouldCacheTime, JSONObject.toJSONString(dashboardIds));
        } else {
            Set<Long> cacheIds = Sets.newHashSet();
            try {
                Set<Long> currentDayDashboardIds = JSONObject.parseObject(currentDayCache, Set.class);
                if (!CollectionUtils.isEmpty(currentDayDashboardIds)) {
                    cacheIds.addAll(currentDayDashboardIds);
                }
            } catch (Exception e) {

            }
            cacheIds.addAll(dashboardIds);
            redisClient.setex(RedisConsts.CURRENT_DAY_UPDATED_DASHBOARD, shouldCacheTime, JSONObject.toJSONString(cacheIds));
        }

        String cacheJobList = redisClient.get(RedisConsts.EMAIL_UNABLE_SEND_LIST);
        Set<Long> jobSets = CommonUtils.getIdSetByCache(cacheJobList);
        jobSets.forEach(jobId -> {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (emailScheduleService.shouldExecute(jobId)) {
                        lock.lock();
                        try {
                            // 执行后从redis缓存中移除
                            String cacheStr = redisClient.get(RedisConsts.EMAIL_UNABLE_SEND_LIST);
                            Set<Long> newIds = CommonUtils.getIdSetByCache(cacheStr);
                            if (newIds.remove(jobId)) {
                                redisClient.setex(RedisConsts.EMAIL_UNABLE_SEND_LIST, DateUtils.getSeconds(), JSONObject.toJSONString(newIds));
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        } finally {
                            lock.unlock();
                        }
                        try {
                            emailScheduleService.execute(jobId);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            });
        });


        // 判断是否是实时更新
        boolean flowFlag = false;
        if (!StringUtils.isEmpty(type)) {
            flowFlag = "flow".equals(type);
        }

        // 迭代遍历davinci reportUrls，筛选出reportUrl包含的dashboardId在一天内均已经更新的url
        Iterator<String> iterator = davinciReportUrls.iterator();
        while (iterator.hasNext() && !flowFlag) {
            String url = iterator.next();
            if (StringUtils.isEmpty(url)) {
                iterator.remove();
                continue;
            }

            // 存放reportUrl中包含的dashboardId
            Set<Long> containIds = getDashboardIdFromReportUrl(url);

            if (CollectionUtils.isEmpty(containIds)) {
                iterator.remove();
                continue;
            }

            boolean completeUpdate = false;

            // 取containIds和dashboardIds的交集
            Set<Long> intersection = Sets.newHashSet();
            intersection.addAll(dashboardIds);
            intersection.retainAll(containIds);
            String cache = redisClient.get(RedisConsts.DATAV_UPDATE_REPORT_INFO, url);
            Set tempSet = JSONObject.parseObject(cache, Set.class);
            // 处理泛型问题
            Set<Long> cacheSet = Sets.newHashSet();
            if (tempSet != null) {
                tempSet.forEach(temp -> {
                    cacheSet.add(Long.parseLong(temp.toString()));
                });
            }

            // 判断决策平台report包含的所有看板是否已经在当天全部完成更新
            if (cacheSet == null) {
                if (intersection.containsAll(containIds)) {
                    completeUpdate = true;
                } else {
                    redisClient.setex(RedisConsts.DATAV_UPDATE_REPORT_INFO, shouldCacheTime, JSONObject.toJSONString(intersection), url);
                }
            } else {
                cacheSet.addAll(intersection);
                if (cacheSet.containsAll(containIds)) {
                    completeUpdate = true;
                    redisClient.del(RedisConsts.DATAV_UPDATE_REPORT_INFO, url);
                } else {
                    redisClient.setex(RedisConsts.DATAV_UPDATE_REPORT_INFO, shouldCacheTime, JSONObject.toJSONString(cacheSet), url);
                }
            }

            if (!completeUpdate) {
                iterator.remove();
            }
        }

        // 向kafka发送消息
        davinciReportUrls.forEach(url -> {
            Map<String, String> message = Maps.newHashMap();
            message.put("report_name", url);
            message.put("update_time", updateTime);
            message.put("begin_time", updateTime);
            message.put("end_time", updateTime);
            message.put("schedule_id", UUID.randomUUID().toString());
            message.put("refresh_record_id", "0");
            message.put("status", status);

            kafkaTemplate.send("reportRefreshTopic", JSONObject.toJSONString(message));
        });
    }

    private Set<Long> getDashboardIdFromReportUrl(String url) {
        Set<Long> dashboardIds = Sets.newHashSet();
        String[] split = url.split(",");
        for (String s : split) {
            String[] dashboardInfo = s.split(":");
            if (dashboardInfo.length == 2) {
                Long dashboardId = Long.parseLong(dashboardInfo[1]);
                dashboardIds.add(dashboardId);
            }
        }
        return dashboardIds;
    }
}
