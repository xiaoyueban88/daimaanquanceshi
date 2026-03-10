package edp.davinci.core.template.impl;

import java.util.List;

import edp.core.annotation.SourceTypeDiscirmination;
import edp.davinci.core.template.AbstractBuildQuerySqlTemplate;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.Source;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/8
 */
@Component
@SourceTypeDiscirmination(sourceType = "default")
public class DefaultSqlTemplate extends AbstractBuildQuerySqlTemplate {
    @Override
    public void build(List<String> querySqlList, Source source, ViewExecuteParam executeParam) {
        buildQuerySql(querySqlList, source, executeParam, null);
    }
}
