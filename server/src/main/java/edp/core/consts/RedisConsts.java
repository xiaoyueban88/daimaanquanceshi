package edp.core.consts;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/3/31
 */
public class RedisConsts {
    /**
     * getData getDistinctvalue对应的数据
     * davinci:view:{md5Key}
     */
    public static final String DAVINCI_VIEW_CACHE = "d:v:{md5Key}";

    public static final String DAVINCI_VIEW_CACHE_INTERVAL = "d:v:i:{interval}:{md5key}";

    public static final String WIDGET_VIEW_RECORD_COUNT = "w:v:r:c:{widgetId}:{viewId}";

    /**
     * dashboard本日内已更新的关联表
     * davinci:dashboard:update:{dashboardId}
     */
    public static final String DAVINCI_DASHBOARD_UPDATE_INFO = "d:d:u:{dashboardId}";

    /**
     * 决策平台看板包含的dashboard更新信息
     * datav:report:updateDashboardId:{reportUrl}
     */
    public static final String DATAV_UPDATE_REPORT_INFO = "d:r:u:{reportUrl}";

    /**
     * 数据表更新时间
     * table:updateTime:{tableName}
     */
    public static final String TABLE_UPDATETIME = "t:u:{tableName}";

    /**
     * getData getDistinctvalue 缓存更新时间
     * md5Key:updateTime:{md5Key}
     */
    public static final String MD5KEY_UPDATETIME = "m:u:{md5Key}";

    /**
     * 当天已完成更新的dashboard;
     */
    public static final String CURRENT_DAY_UPDATED_DASHBOARD = "c:d:u:d";

    /**
     * 定时邮件到时未成功发送的任务id列表
     */
    public static final String EMAIL_UNABLE_SEND_LIST = "e:u:s:l";

    /**
     * 邮件任务执行锁
     */
    public static final String EMAIL_EXECUTE_LOCK = "e:e:l:{jobId}";

    /**
     * sql对应的记录总数
     */
    public static final String SQL_RECORD_COUNT = "w:v:r:c:{sqlkey}";


    /**
     * 控制下载得并发数
     */
    public static final String CONCURRENT_LIMITER_COUNT = "con:limiter";
}
