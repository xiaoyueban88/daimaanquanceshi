package edp.davinci.core.udffunction;

import com.google.common.collect.Sets;

import edp.core.annotation.UdfFunctionDiscrimination;
import edp.core.enums.DataTypeEnum;
import edp.core.utils.UUIDUtils;
import edp.davinci.dto.viewDto.ExtraAggregator;
import edp.davinci.dto.viewDto.UdfFunctionDto;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/7
 */
@Component
@UdfFunctionDiscrimination(functionName = "groupCount", sourceType = "clickhouse")
public class ClickhouseGroupCount implements IUdfFunction {


    @Override
    public Pair<ExtraAggregator, ExtraAggregator> prepare(UdfFunctionDto udfFunctionDto) {
        String expression = udfFunctionDto.getExpression();
        String groupArray = udfFunctionDto.getGroupArray();
        DataTypeEnum clickhouse = DataTypeEnum.CLICKHOUSE;

        String subAlias = UUIDUtils.generateShortUuid();
        // 生成widget层的sql片段
        String sql = new StringBuilder("count(").append(expression).append(")")
                .append(" AS ").append(clickhouse.getAliasPrefix()).append(subAlias)
                .append(clickhouse.getAliasSuffix()).toString();
        // 可转化为widget层sql的额外指标
        ExtraAggregator leftExtraAggregator = ExtraAggregator.builder().expression(sql).alias(subAlias)
                .convertWidgetSql(true).build();

        // 不可转化为widget层sql的额外指标
        ExtraAggregator extraAggregator = ExtraAggregator.builder().function("arraySum").expression(subAlias)
                .groupBy(groupArray).alias(udfFunctionDto.getAlias()).convertWidgetSql(false).functionPrefix("(")
                .functionSuffix(")").usedAliasList(Sets.newHashSet(subAlias)).arrayJoin(false).build();
        return Pair.of(leftExtraAggregator, extraAggregator);
    }

    @Override
    public void post() {

    }
}
