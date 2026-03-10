package edp.core.utils;

import java.util.List;

import com.google.common.collect.Lists;

import edp.davinci.core.common.Constants;
import edp.davinci.dto.viewDto.Order;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/3/13
 */
public class STUtils {
    /**
     * 获取查询分组总数sql
     * @param srcSql 源sql
     * @param groups 分组维度
     * @param filters 过滤条件
     * @param havings having过滤条件
     * @return
     */
    public static String queryGroupCountSql(String srcSql, List<String> groups, List<String> filters, List<String> havings) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("queryGroupCountSql");
        st.add("srcSql", srcSql);
        st.add("groups", groups);
        st.add("filters",filters);
        st.add("havings", havings);
        return st.render();
    }

    /**
     * 白泽widget查询sql
     * @param nativeQuery
     * @param groups
     * @param aggregators
     * @param filters
     * @param havings
     * @param orders
     * @param sql
     * @param keywordPrefix
     * @param keywordSuffix
     * @return
     */
    public static String querySql(boolean nativeQuery, List<String> groups, List<String> aggregators,
                                  List<String> filters, List<String> havings, List<Order> orders, List<Order> outerOrders,
                                  String sql, String keywordPrefix, String keywordSuffix) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("querySql");
        st.add("nativeQuery", nativeQuery);
        st.add("groups", groups);
        st.add("aggregators", aggregators);
        st.add("filters", filters);
        st.add("havings", havings);
        st.add("orders", orders);
        st.add("outerOrders", outerOrders);
        st.add("sql", sql);
        st.add("keywordPrefix", keywordPrefix);
        st.add("keywordSuffix", keywordSuffix);
        return st.render();
    }

    /**
     * 获取交叉查询sql
     * @param srcSql
     * @param rowGroups
     * @param colGroups
     * @param aggregators
     * @param orders
     * @param aliasPrefix
     * @param aliasSuffix
     * @return
     */
    public static String queryCrossSql(String srcSql, List<String> rowGroups, List<String> colGroups,
                                       List<String> aggregators, List<Order> orders,
                                       String aliasPrefix, String aliasSuffix, String aggMethod) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("queryCrossSql");
        st.add("srcSql", srcSql);
        st.add("rowGroups", rowGroups);
        st.add("colGroups", colGroups);
        st.add("aggregators", CollectionUtils.isEmpty(aggregators) ? null : aggregators);
//        st.add("conditions", CollectionUtils.isEmpty(conditions) ? null : conditions);
        st.add("orders", CollectionUtils.isEmpty(orders) ? null : orders);
        st.add("aliasPrefix", aliasPrefix);
        st.add("aliasSuffix", aliasSuffix);
//        st.add("connector", connector);
        st.add("aggMethod", aggMethod);
        return st.render();
    }

    public static String queryCrossOrderSql(String srcSql, List<String> rowGroups, List<String> colGroups,
                                       List<String> aggregators, Order order, String condition,
                                       String aliasPrefix, String aliasSuffix, String keyPrefix,
                                       String keySuffix, String aggMethod, String sortKeyMethod) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("queryCrossOrderSql");
        st.add("srcSql", srcSql);
        st.add("rowGroups", rowGroups);
        st.add("colGroups", colGroups);
        st.add("aggregators",  CollectionUtils.isEmpty(aggregators) ? null : aggregators);
        st.add("condition", condition);
        st.add("order", order);
        st.add("aliasPrefix", aliasPrefix);
        st.add("aliasSuffix", aliasSuffix);
        st.add("aggMethod", aggMethod);
        st.add("keyPrefix", keyPrefix);
        st.add("keySuffix", keySuffix);
        st.add("sortKeyMethod", sortKeyMethod);
        return st.render();
    }

    /**
     * 获取去重查询sql
     * @param srcSql 源
     * @param columns 去重查询列
     * @param filters 过滤条件
     * @param orders 排序
     * @return
     */
    public static String newQueryDistinctSql(String srcSql, List<String> columns, List<String> filters,
                                             List<Order> orders, String keywordPrefix, String keywordSuffix) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf("newQueryDistinctSql");
        st.add("sql", srcSql);
        st.add("columns", columns);
        st.add("filters", filters);
        st.add("orders", orders);
        st.add("keywordPrefix", keywordPrefix);
        st.add("keywordSuffix", keywordSuffix);
        return st.render();
    }

    public static String queryUnionDistinctSql(List<String> distinctSqls, Integer limit, List<String> columns, String direction) {
        STGroupFile stg = new STGroupFile(Constants.SQL_TEMPLATE);
        ST st = stg.getInstanceOf(Constants.SQL_TEMPLATE_FUNCTION_UNIONDISTINCT);
        st.add("sqls", distinctSqls);
        st.add("limit", limit);
        st.add("columns", columns);
        st.add("direction", direction);
        return st.render();
    }
}

