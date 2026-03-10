package edp.core.utils;

import java.util.Set;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/10/28
 */
public class CommonUtils {
    public static Set<Long> getIdSetByCache(String cache) {
        Set<Long> result = Sets.newHashSet();
        if (StringUtils.isEmpty(cache)) {
            return result;
        }
        Set<Integer> cacheIds = JSONObject.parseObject(cache, Set.class);
        //Integer -> Long
        cacheIds.forEach(id -> {
            result.add(id.longValue());
        });
        return result;
    }
}
