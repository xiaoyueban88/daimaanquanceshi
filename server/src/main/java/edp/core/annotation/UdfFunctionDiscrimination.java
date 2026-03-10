package edp.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zswu3
 * @Description 自定义函数类识别注解
 * @date 2020/9/7
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UdfFunctionDiscrimination {
    /**
     * 自定义函数类对应的函数名
     *
     * @return
     */
    String functionName();

    /**
     * 自定义函数类对应的数据库类型
     *
     * @return
     */
    String sourceType();
}
