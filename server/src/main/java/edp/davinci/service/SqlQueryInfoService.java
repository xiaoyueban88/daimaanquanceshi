package edp.davinci.service;

import java.util.Map;

import edp.davinci.model.SqlQueryInfo;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/2
 */
public interface SqlQueryInfoService {
    /**
     * hset redis for widget data
     *
     * @param map md5 -> key
     */
    void setDataRedis(Map<String, SqlQueryInfo> map);

    /**
     * hset redis for distinct value
     *
     * @param map md5 -> key
     */
    void setDistinctRedis(Map<String, SqlQueryInfo> map);
}
