package edp.davinci.core.model;

import java.util.List;

import com.alibaba.fastjson.JSONArray;

import edp.core.consts.Consts;
import edp.davinci.core.enums.SqlOperatorEnum;
import io.jsonwebtoken.lang.Collections;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/8/6
 */
@Data
public class SqlHaving {
    /**
     * 表达式
     */
    private String expression;

    /**
     * 操作符
     */
    private String operator;

    /**
     * having右值
     */
    private Object value;

    /**
     * 表达式值类型
     */
    private String sqlType;


    private String type;

    /**
     * 子过滤条件
     */
    private List<SqlHaving> children;


    public static void recursionSetAliasName(List<SqlHaving> sqlHavings, String alias) {
        sqlHavings.forEach(having -> {
            having.setExpression(alias);
            if (!Collections.isEmpty(having.getChildren())) {
                recursionSetAliasName(having.getChildren(), alias);
            }
        });
    }

    public static String dealHaving(SqlHaving sqlHaving) {
        StringBuilder condition = new StringBuilder();
        String type = sqlHaving.getType();
        if (SqlFilter.Type.filter.equalsIgnoreCase(type)) {
            condition.append(dealOperator(sqlHaving));
        }

        if (SqlFilter.Type.relation.equalsIgnoreCase(type)) {
            List<SqlHaving> childs = sqlHaving.getChildren();
            condition.append(Consts.PARENTHESES_START);
            for (int i = 0; i < childs.size(); i++) {
                condition.append(i == 0 ? dealHaving(childs.get(i)) : Consts.SPACE + sqlHaving.getValue().toString()
                        + Consts.SPACE + dealHaving(childs.get(i)));
            }
            condition.append(Consts.PARENTHESES_END);
        }
        return condition.toString();
    }

    public static String dealOperator(SqlHaving sqlHaving) {
        String expression = sqlHaving.getExpression();
        String operator = sqlHaving.getOperator();
        String sqlType = sqlHaving.getSqlType();
        Object value = sqlHaving.getValue();

        Criterion criterion;
        if (SqlOperatorEnum.BETWEEN.getValue().equalsIgnoreCase(operator)) {
            JSONArray values = (JSONArray) value;
            criterion = new Criterion(expression, operator, values.get(0), values.get(1), sqlType);
        } else {
            criterion = new Criterion(expression, operator, value, sqlType);
        }

        return SqlFilter.generator(criterion);
    }
}
