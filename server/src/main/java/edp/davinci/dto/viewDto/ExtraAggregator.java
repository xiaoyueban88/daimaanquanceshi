package edp.davinci.dto.viewDto;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/7
 */
@Data
@Builder
public class ExtraAggregator {
    private String function;

    private String functionPrefix;

    private String functionSuffix;

    private String expression;

    /**
     * 表达式中使用到的别名
     */
    private Set<String> usedAliasList;

    private String groupBy;

    private String alias;

    private boolean arrayJoin;

    // 是否可转换为widget层的sql
    private boolean convertWidgetSql;
}
