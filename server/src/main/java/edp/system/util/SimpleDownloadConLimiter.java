package edp.system.util;

import com.iflytek.edu.zx.redis.client.RedisClient;
import edp.core.consts.RedisConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author haofei3
 */
@Component
@Slf4j
public class SimpleDownloadConLimiter implements InitializingBean {

    @Value("${download.excel.concurrent.limit}")
    private Long limit;

    @Value("${download.excel.max.lock.time:600}")
    private Integer maxLockTime;

    @Autowired
    private RedisClient redisClient;

    public static SimpleDownloadConLimiter INSTANCE;



    /**
     * obtain permit
     * @return
     */
    public boolean obtain() {
        if (StringUtils.isBlank(redisClient.get(RedisConsts.CONCURRENT_LIMITER_COUNT))
                || Long.parseLong(redisClient.get(RedisConsts.CONCURRENT_LIMITER_COUNT)) < limit) {
            long count = redisClient.incr(RedisConsts.CONCURRENT_LIMITER_COUNT);
            redisClient.expire(RedisConsts.CONCURRENT_LIMITER_COUNT, maxLockTime);
            log.info("获取锁资源，并发下载excel计数：{}", count);
            return true;
        }
        return false;
    }


    /**
     * release
     */
    public void release() {
        String curCount = redisClient.get(RedisConsts.CONCURRENT_LIMITER_COUNT);
        if (StringUtils.isNotBlank(curCount)
                && Long.parseLong(curCount)  > 0 ) {
            long count = redisClient.decr(RedisConsts.CONCURRENT_LIMITER_COUNT);
            log.info("释放锁资源，当前并发下载excel计数：{}，修改之前计数： {}", count, curCount);
        }
    }

    @Override
    public void afterPropertiesSet() {
        // 初始化清楚当前并发下载数量
        redisClient.del(RedisConsts.CONCURRENT_LIMITER_COUNT);
        INSTANCE = this;
        log.info("==================初始化redis限制文件下载计数器 ： {},持有锁最长时间：{} =============", limit, maxLockTime);
    }

}
