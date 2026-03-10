package edp.davinci.core.template.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edp.core.annotation.SourceTypeDiscirmination;
import edp.core.consts.Consts;
import edp.core.enums.DataTypeEnum;
import edp.core.utils.CollectionUtils;
import edp.core.utils.MatchUtils;
import edp.core.utils.STUtils;
import edp.core.utils.SqlUtils;
import edp.core.utils.UUIDUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.template.AbstractBuildQuerySqlTemplate;
import edp.davinci.core.udffunction.IUdfFunction;
import edp.davinci.dto.viewDto.Aggregator;
import edp.davinci.dto.viewDto.CrossOrder;
import edp.davinci.dto.viewDto.ExtraAggregator;
import edp.davinci.dto.viewDto.Order;
import edp.davinci.dto.viewDto.Param;
import edp.davinci.dto.viewDto.UdfFunctionDto;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.Source;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/8
 */
@Component
@SourceTypeDiscirmination(sourceType = "clickhouse")
public class ClickhouseSqlTemplate extends AbstractBuildQuerySqlTemplate {

    private final DataTypeEnum clickhouseEnum = DataTypeEnum.CLICKHOUSE;

    @Override
    public void build(List<String> querySqlList, Source source, ViewExecuteParam executeParam) {
        // 指标sql片段
        List<String> aggregtorSqls = Lists.newArrayList();
        // groupBy -> 额外指标集合
        Map<String, List<ExtraAggregator>> extraAggregatorMap = Maps.newHashMap();
        // 可在widget层完成sql拼接的额外指标集合
        List<ExtraAggregator> widgetExtraAgg = Lists.newArrayList();
        if (null != executeParam) {
            // 解析含有自定义函数的指标
            List<UdfFunctionDto> functionDtos = executeParam.resolveAggerators();

            for (UdfFunctionDto functionDto : functionDtos) {
                DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(source.getJdbcUrl());
                IUdfFunction udfFunctionBean = udfFunctionRegisterBean
                        .getUdfFunctionBean(functionDto.getName(), dataTypeEnum.getFeature());
                if (null == udfFunctionBean) {
                    continue;
                }
                Pair<ExtraAggregator, ExtraAggregator> preparePair = udfFunctionBean.prepare(functionDto);
                // widget层sql片段
                ExtraAggregator leftExtraAgg = preparePair.getLeft();
                if (null != leftExtraAgg) {
                    aggregtorSqls.add(leftExtraAgg.getExpression());
                    widgetExtraAgg.add(leftExtraAgg);
                }
                // 额外指标
                ExtraAggregator extraAggregator = preparePair.getRight();
                String groupBy = extraAggregator.getGroupBy();
                List<ExtraAggregator> extraAggregators = extraAggregatorMap.get(groupBy);
                if (CollectionUtils.isEmpty(extraAggregators)) {
                    extraAggregatorMap.put(groupBy, Lists.newArrayList(extraAggregator));
                } else {
                    extraAggregators.add(extraAggregator);
                }
            }
            if (!CollectionUtils.isEmpty(executeParam.getFinalAggregators())) {
                executeParam.getFinalAggregators().forEach(agg -> {
                    List<ExtraAggregator> temps = pickUpAggFunction(agg);
                    if (!CollectionUtils.isEmpty(temps)) {
                        widgetExtraAgg.addAll(temps);
                        temps.forEach(tmp -> {
                            aggregtorSqls.add(tmp.getExpression());
                        });
                    }
                });
            }

        }

        // 拼接widget层的sql
        buildQuerySql(querySqlList, source, executeParam, aggregtorSqls);

        // 拼接由自定义函数解析出来的指标
        for (int i = 0; i < querySqlList.size(); i++) {
            String querySql = querySqlList.get(i);
            // 在widget层外进行额外指标的sql拼接
            if (!CollectionUtils.isEmpty(extraAggregatorMap)) {
                String newQuerySql = buildSqlForExtraAgg(querySql, extraAggregatorMap, widgetExtraAgg, executeParam, source);
                if (!StringUtils.isEmpty(newQuerySql)) {
                    querySqlList.set(i, newQuerySql);
                }
            }
        }
    }

    private String buildSqlForExtraAgg(String querySql, Map<String, List<ExtraAggregator>> extraAggregatorMap,
                                       List<ExtraAggregator> widgetExtraAgg, ViewExecuteParam executeParam, Source source) {
        // 获取已经在widget层完成查询的指标别名
        Set<String> aliasSet = Sets.newHashSet();

        // 最终需要返回的指标
        Set<String> finalAlias = Sets.newHashSet();
        if (!CollectionUtils.isEmpty(widgetExtraAgg)) {
            widgetExtraAgg.forEach(agg -> aliasSet.add(agg.getAlias()));
        }
        if (null != executeParam && !CollectionUtils.isEmpty(executeParam.getAggregators())) {
            executeParam.getAggregators().forEach(agg -> {
                String alias = executeParam.formatAlias(agg.getColumn(), agg.getFunc(), source.getJdbcUrl(), source.getDbVersion());
                aliasSet.add(alias);
                finalAlias.add(alias);
            });
        }
        if (null != executeParam && !CollectionUtils.isEmpty(executeParam.getGroups())) {
            aliasSet.addAll(executeParam.getGroups());
        }

        List<Order> orders = null;
        if (null != executeParam && CollectionUtils.isEmpty(executeParam.getOrders())) {
            orders = executeParam.getOrders(source.getJdbcUrl(), source.getDbVersion());
        }


        // 拼接额外指标
        for (String groupBy : extraAggregatorMap.keySet()) {
            List<ExtraAggregator> extraAggregators = extraAggregatorMap.get(groupBy);
            STGroup stg = new STGroupFile(Constants.SQL_TEMPLATE);
            Set<String> newAliasSet = Sets.newHashSet();
            // 取得上层已完成计算的所有指标,用于select
            newAliasSet.addAll(aliasSet);
//            extraAggregators.forEach(ea -> newAliasSet.removeAll(ea.getUsedAliasList()));// 移除本层参与计算的指标,防止别名冲突

            // 需要进行列转行的指标
            Set<String> arrayJoins = Sets.newHashSet();
            arrayJoins.addAll(newAliasSet);
            extraAggregators.forEach(ea -> {
                if (ea.isArrayJoin()) {
                    arrayJoins.add(ea.getAlias());
                }
            });


            ST st = stg.getInstanceOf("buildExtraSql");
            st.add("groupBy", groupBy);
            st.add("extraAggs", extraAggregators); // 本次需要计算出来的额外指标
            st.add("querySql", querySql);
            st.add("aggAlias", newAliasSet); // //上层已经计算出来的指标
            st.add("arrayJoins", arrayJoins);
            st.add("orders", orders);
//            st.add("aliasPrefix", aliasPrefix);
//            st.add("aliasSuffix", aliasSuffix);
            querySql = st.render();
            extraAggregators.forEach(ea -> aliasSet.add(ea.getAlias())); // 添加已经拼接上去的指标别名
        }

        // 拼接最终指标
        if (null != executeParam && !CollectionUtils.isEmpty(executeParam.getFinalAggregators())) {
            List<String> finalAggregators = executeParam.getFinalAggregators(source.getJdbcUrl(), source.getDbVersion());
            STGroup stg = new STGroupFile(Constants.SQL_TEMPLATE);
            ST st = stg.getInstanceOf("buildFinalSql");
            st.add("querySql", querySql);
            st.add("groups", executeParam.getGroups());
            st.add("finalAlias", finalAlias);
            st.add("finalAggs", finalAggregators);
            querySql = st.render();
        }
        return querySql;
    }

    // 从final指标中提取出聚合函数
    private List<ExtraAggregator> pickUpAggFunction(Aggregator aggregator) {
        String calculateRules = aggregator.getCalculateRules();
        List<ExtraAggregator> result = Lists.newArrayList();
        for (String functionName : Consts.AGG_FUN_NAMES) {
            String pre = functionName + MatchUtils.parenthesesPre;
            List<String> functions = MatchUtils.getParenthesesMatchStr(calculateRules, pre);
            for (String function : functions) {
                // 生成别名
                String alias = UUIDUtils.generateShortUuid();
                calculateRules = calculateRules.replace(function, alias);
                // 生成widget层的sql片段
                String sql = new StringBuilder(function).append(" AS ").append(clickhouseEnum.getAliasPrefix())
                        .append(alias).append(clickhouseEnum.getAliasSuffix()).toString();
                ExtraAggregator widgetAggregator = ExtraAggregator.builder().expression(sql).alias(alias).build();
                result.add(widgetAggregator);
            }
        }
        aggregator.setCalculateRules(calculateRules);
        return result;
    }
}
