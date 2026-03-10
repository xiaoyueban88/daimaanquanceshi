package edp.davinci.core.config;

import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edp.core.annotation.UdfFunctionDiscrimination;
import edp.davinci.core.udffunction.IUdfFunction;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description 自定义函数类bean工厂
 * @date 2020/9/7
 */
@Component
@Scope(value = "singleton")
public class UdfFunctionRegisterBean implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    // functionName, sourceType -> IUdfFunction
    private Table<String, String, IUdfFunction> udfFunctionBeanTable = HashBasedTable.create();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void init() {
        if (null == this.applicationContext) {
            return;
        }
        Map<String, Object> beansWithAnnotation = this.applicationContext.getBeansWithAnnotation(UdfFunctionDiscrimination.class);
        for (String key : beansWithAnnotation.keySet()) {
            IUdfFunction bean = (IUdfFunction) beansWithAnnotation.get(key);
            UdfFunctionDiscrimination annotation = bean.getClass().getAnnotation(UdfFunctionDiscrimination.class);
            if (null != annotation) {
                udfFunctionBeanTable.put(annotation.functionName(), annotation.sourceType(), bean);
            }
        }
    }

    public IUdfFunction getUdfFunctionBean(String functionName, String sourceType) {
        if (null == udfFunctionBeanTable || udfFunctionBeanTable.isEmpty()) {
            init();
        }
        return udfFunctionBeanTable.get(functionName, sourceType);
    }
}
