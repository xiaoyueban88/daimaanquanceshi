package edp.davinci.kafka;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.iflytek.edu.zx.redis.client.RedisClient;

import edp.core.consts.RedisConsts;
import edp.core.utils.DateUtils;
import edp.davinci.dao.SqlQueryInfoMapper;
import edp.davinci.service.SqlQueryInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/1
 */
@Component
@Slf4j
public class KafkaConsumer {

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private SqlQueryInfoMapper sqlQueryInfoMapper;

    @Autowired
    private SqlQueryInfoService sqlQueryInfoService;

    private static final String KEY_TABLENAME = "tableName";

    private static final String KEY_UPDATETIME = "updateTime";

    private static final String KEY_STATUS = "status";

    private static final String KEY_FROM = "from";

    @Value("${default.cache.time}")
    private Integer cacheTime;

    @Autowired
    private KafkaProducer kafkaProducer;


    /**
     * 接受clickhouse table更新消息，并将clickhouse表更新时间同步到redis中
     *
     * @param msg
     */
    @KafkaListener(topics = "clickhouseUpdate")
    public void clickhouseListenerConsumer(ConsumerRecord<String, String> msg) {
        Map<String, String> map = (Map) JSON.parse(msg.value());
        // 获取clickhouse更新的table name
        String tableName = map.get(KEY_TABLENAME);
        String updateTime = map.get(KEY_UPDATETIME);
        String status = map.get(KEY_STATUS);
        String type = map.get(KEY_FROM);

        // 记录更新时间
        String cache = redisClient.get(RedisConsts.TABLE_UPDATETIME, tableName);
        int seconds = DateUtils.getSeconds();
        if (cache == null) {
            redisClient.setex(RedisConsts.TABLE_UPDATETIME, seconds, updateTime, tableName);
            // 向kafka发送报表更新消息
            kafkaProducer.sendRefreshReportMsg(tableName, updateTime, status, type);
        } else {
            try {
                long cTime = DateUtils.toDate(cache).getTime();
                long uTime = DateUtils.toDate(updateTime).getTime();
                if (cTime < uTime) {
                    redisClient.setex(RedisConsts.TABLE_UPDATETIME, seconds, updateTime, tableName);
                    // 向kafka发送报表更新消息
                    kafkaProducer.sendRefreshReportMsg(tableName, updateTime, status, type);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
