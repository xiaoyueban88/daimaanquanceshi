package edp.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
@Slf4j
@Component("springSessionDefaultRedisSerializer")
public class CustomSessionDefaultRedisSerializer extends JdkSerializationRedisSerializer {

    @Override
    public Object deserialize(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        } else {
            Object deserialObj = null;
            try {
                deserialObj = super.deserialize(bytes);
            } catch (Exception var3) {
//                log.warn("Cannot deserialize");
            }
            return deserialObj;
        }
    }
}
