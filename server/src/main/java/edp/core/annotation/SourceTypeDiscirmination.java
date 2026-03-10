package edp.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zswu3
 * @Description 区分适用的数据库类型注解
 * @date 2020/9/8
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SourceTypeDiscirmination {
    /**
     * 数据库类型
     *
     * @return
     */
    String sourceType();
}
