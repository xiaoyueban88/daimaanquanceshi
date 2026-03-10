package edp.davinci.core.template;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import edp.core.enums.DataTypeEnum;
import edp.core.utils.CollectionUtils;
import edp.core.utils.STUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.common.utils.DruidSqlOptimizeUtil;
import edp.davinci.common.utils.SqlOptimizeUtil;
import edp.davinci.core.common.Constants;
import edp.davinci.core.config.UdfFunctionRegisterBean;
import edp.davinci.dto.viewDto.Aggregator;
import edp.davinci.dto.viewDto.CrossOrder;
import edp.davinci.dto.viewDto.Order;
import edp.davinci.dto.viewDto.Param;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.Source;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public abstract class AbstractBuildQuerySqlTemplate {
    @Autowired
    protected UdfFunctionRegisterBean udfFunctionRegisterBean;

    public abstract void build(List<String> querySqlList, Source source, ViewExecuteParam executeParam);

    /**
     * 交叉sql build
     * @param querySqlList
     * @param source
     * @param executeParam
     */
    public void crossSqlBuild(List<String> querySqlList, Source source, ViewExecuteParam executeParam) {
        // build 基础sql(交叉sql基于此
        // 当存在行维度高级排序,build基础sql时需要添加相应的指标
        List<Order> rowAliasOrders = Lists.newArrayList();
        if(!CollectionUtils.isEmpty(executeParam.getRowOrders())) {
            List<Aggregator> newAgg = Lists.newArrayList();
            executeParam.getRowOrders().forEach(order -> {
                if("ASC".equals(order.getDirection().toUpperCase())) {
                    newAgg.add(new Aggregator(order.getColumn(), "MIN"));
                    String orderColumn = new StringBuilder("MIN(").append(executeParam.formatAlias(order.getColumn(), "MIN",
                            source.getJdbcUrl(), source.getDbVersion())).append(")").toString();
                    rowAliasOrders.add(new Order(orderColumn, order.getDirection()));
                } else {
                    newAgg.add(new Aggregator(order.getColumn(), "MAX"));
                    String orderColumn = new StringBuilder("MAX(").append(executeParam.formatAlias(order.getColumn(), "MAX",
                            source.getJdbcUrl(), source.getDbVersion())).append(")").toString();
                    rowAliasOrders.add(new Order(orderColumn, order.getDirection()));
                }
            });
            if(CollectionUtils.isEmpty(executeParam.getAggregators())) {
                executeParam.setAggregators(newAgg);
            } else {
                executeParam.getAggregators().addAll(newAgg);
            }
        }

        build(querySqlList, source, executeParam);
        if(CollectionUtils.isEmpty(executeParam.getRowGroups())) {
            return;
        }

        CrossOrder crossOrder = executeParam.getCrossOrder();

        // 获取库类型
        DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(source.getJdbcUrl());

        // 行转列方法
        String aggMethod = DataTypeEnum.CLICKHOUSE.getFeature().equals(dataTypeEnum.getFeature())
                ? "groupArray" : "json_array";

        // 获取指标别名
        List<Aggregator> aggregators = executeParam.getAggregators();
        List<String> aggAlias = aggregators.stream().map(a ->
                executeParam.formatAlias(a.getColumn(), a.getFunc(), source.getJdbcUrl(), source.getDbVersion()))
                .collect(Collectors.toList());
        if (null == crossOrder || null == crossOrder.getOrder()
                || CollectionUtils.isEmpty(crossOrder.getConditions())
                || CollectionUtils.isEmpty(executeParam.getAggregators())) { // 不存在交叉排序或无效
            // 过滤出行维度排序
            List<Order> rowOrders = executeParam.getOrders().stream()
                    .filter(order -> executeParam.getRowGroups()
                            .contains(executeParam.getOriOrderColumn(order, dataTypeEnum)))
                    .collect(Collectors.toList());

            if(!CollectionUtils.isEmpty(rowAliasOrders)) {
                if(CollectionUtils.isEmpty(rowOrders)) {
                    rowOrders = rowAliasOrders;
                } else {
                    rowOrders.addAll(rowAliasOrders);
                }
            }
            rowOrders = CollectionUtils.isEmpty(rowOrders) ? null : rowOrders;
            String crossSql = STUtils.queryCrossSql(querySqlList.get(0), executeParam.getRowGroups(), executeParam.getColGroups(),
                    aggAlias, rowOrders, dataTypeEnum.getAliasPrefix(),
                    dataTypeEnum.getAliasSuffix(), aggMethod);
            querySqlList.set(0, crossSql);
        } else { // 实现交叉排序(按照某个具体列的一个指标进行排序)
            String condition = getMatchConditionList(crossOrder.getConditions());
            String sortKeyMethod = "ASC".equals(crossOrder.getOrder().getDirection()) ? "min" : "max";
            String crossSql = STUtils.queryCrossOrderSql(querySqlList.get(0),  executeParam.getRowGroups(), executeParam.getColGroups(),
                    aggAlias, crossOrder.getOrder(dataTypeEnum), condition, dataTypeEnum.getAliasPrefix(),
                    dataTypeEnum.getAliasSuffix(), dataTypeEnum.getKeywordPrefix(), dataTypeEnum.getKeywordSuffix(), aggMethod, sortKeyMethod);
            querySqlList.set(0, crossSql);
        }
    }

    protected void buildQuerySql(List<String> querySqlList, Source source, ViewExecuteParam executeParam, List<String> extraAggSql) {

        //构造参数， 原有的被传入的替换
        STGroup stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("querySql");
        st.add("nativeQuery", executeParam.isNativeQuery());
        st.add("groups", executeParam.getGroups());

        if (executeParam.isNativeQuery()) {
            st.add("aggregators", executeParam.getAggregators());
        } else {
            List<String> aggs = Lists.newArrayList();
            List<String> aggregators = executeParam.getAggregators(source.getJdbcUrl(), source.getDbVersion());
            if (!CollectionUtils.isEmpty(aggregators)) {
                aggs.addAll(aggregators);
            }
            if (!CollectionUtils.isEmpty(extraAggSql)) {
                aggs.addAll(extraAggSql);
            }
            st.add("aggregators", aggs);
        }

        List<Order> orders = executeParam.getOrders(source.getJdbcUrl(), source.getDbVersion());

        st.add("orders", orders);
        st.add("outerOrders", executeParam.getColOrders(source.getJdbcUrl(), source.getDbVersion()));
        st.add("filters", SqlUtils.convertFilters(executeParam.getFilters(), source));
        st.add("havings", SqlUtils.convertHaving(executeParam, source));
        st.add("keywordPrefix", SqlUtils.getKeywordPrefix(source.getJdbcUrl(), source.getDbVersion()));
        st.add("keywordSuffix", SqlUtils.getKeywordSuffix(source.getJdbcUrl(), source.getDbVersion()));

        for (int i = 0; i < querySqlList.size(); i++) {
            st.add("sql", querySqlList.get(i));
            String querySql = st.render();
            //优化sql
            if (source.getJdbcUrl().contains(DataTypeEnum.CLICKHOUSE.getDesc())
                    || source.getJdbcUrl().contains(DataTypeEnum.MYSQL.getDesc())) {
                querySql = SqlOptimizeUtil.optimizeCKSql(querySql);
//                String type = source.getJdbcUrl().contains(DataTypeEnum.CLICKHOUSE.getDesc()) ?
//                        DataTypeEnum.CLICKHOUSE.getDesc() : DataTypeEnum.MYSQL.getDesc();
//                DruidSqlOptimizeUtil.optimizeSql(querySql, type);
            }
            querySqlList.set(i, querySql);
        }
    }

    protected String getMatchConditionList(List<Param> conditions) {
        List<String> result = Lists.newArrayList();
        conditions.forEach(condition -> {
            String str = new StringBuilder("CT.").append(condition.getName())
                    .append("=").append(condition.getValue()).toString();
            result.add(str);
        });
        return String.join(" and ", result);
    }
}
