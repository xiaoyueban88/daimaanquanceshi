package edp.davinci.core.config;

import java.util.Map;

import com.google.common.collect.Maps;

import edp.core.annotation.SourceTypeDiscirmination;
import edp.core.utils.CollectionUtils;
import edp.davinci.core.template.AbstractBuildQuerySqlTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/8
 */
@Component
@Scope(value = "singleton")
public class BuildSqlTemplateRegister implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Map<String, AbstractBuildQuerySqlTemplate> map = Maps.newHashMap();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void init() {
        if (null == this.applicationContext) {
            return;
        }
        Map<String, Object> beansWithAnnotation = this.applicationContext.getBeansWithAnnotation(SourceTypeDiscirmination.class);
        for (String key : beansWithAnnotation.keySet()) {
            AbstractBuildQuerySqlTemplate bean = (AbstractBuildQuerySqlTemplate) beansWithAnnotation.get(key);
            SourceTypeDiscirmination annotation = bean.getClass().getAnnotation(SourceTypeDiscirmination.class);
            if (null != annotation) {
                map.put(annotation.sourceType(), bean);
            }
        }
    }

    public AbstractBuildQuerySqlTemplate getBuildSqlTemplate(String sourceType) {
        if (CollectionUtils.isEmpty(map)) {
            init();
        }
        AbstractBuildQuerySqlTemplate template = map.get(sourceType);
        if (null == template) {
            return map.get("default");
        }
        return template;
    }
}
