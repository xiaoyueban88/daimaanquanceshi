package edp.davinci.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.redis.client.RedisClient;

import edp.core.consts.Consts;
import edp.core.consts.RedisConsts;
import edp.core.model.PaginateWithQueryColumns;
import edp.core.utils.CollectionUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.dao.SourceMapper;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.Source;
import edp.davinci.model.SqlQueryInfo;
import edp.davinci.service.SqlQueryInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/2
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class SqlQueryInfoServiceImpl implements SqlQueryInfoService {

    @Autowired
    private SourceMapper sourceMapper;

    @Autowired
    private SqlUtils sqlUtils;

    private final RedisClient redisClient;

    @Value("${default.cache.time}")
    private Integer cacheTime;

    @Override
    public void setDataRedis(Map<String, SqlQueryInfo> map) {
//        map.forEach((md5Key, sqlQueryInfo) -> {
//            String sql = sqlQueryInfo.getSql();
//            Long sourceId = sqlQueryInfo.getSourceId();
//            Source source = sourceMapper.getById(sourceId);
//            SqlUtils sqlUtils = this.sqlUtils.init(source);
//            try {
//                PaginateWithQueryColumns paginate = sqlUtils.syncQuery4Paginate(
//                        sql,
//                        sqlQueryInfo.getPageNo(),
//                        sqlQueryInfo.getPageSize(),
//                        0,
//                        executeParam.getLimit(),
//                        excludeColumns);
//                if (null != paginate && !CollectionUtils.isEmpty(paginate.getResultList())) {
//                    String value = JSONObject.toJSONStringWithDateFormat(paginate, null,
//                            SerializerFeature.WriteMapNullValue);
//                    redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE, cacheTime, value, sqlQueryInfo.getMd5Key());
//                }
//
//            } catch (Exception e) {
//                log.error("redis hset error", e);
//            }
//        });
    }

    @Override
    public void setDistinctRedis(Map<String, SqlQueryInfo> map) {
//        map.forEach((md5Key, sqlQueryInfo) -> {
//            String detail = sqlQueryInfo.getDetail();
//            JSONObject jsonObject = JSONObject.parseObject(detail);
//            String sql = (String) jsonObject.get(Consts.STRING_SQL);
//            Long sourceId = sqlQueryInfo.getSourceId();
//            Source source = sourceMapper.getById(sourceId);
//            SqlUtils sqlUtils = this.sqlUtils.init(source);
//
//            // 设置redis缓存
//            try {
//                List<Map<String, Object>> list = sqlUtils.query4List(sql, -1);
//                String value = JSON.toJSONString(list);
//                redisClient.setex(RedisConsts.DAVINCI_VIEW_CACHE, cacheTime, value, sqlQueryInfo.getMd5Key());
//            } catch (Exception e) {
//                log.error("redis hset error");
//            }
//        });
    }
}
