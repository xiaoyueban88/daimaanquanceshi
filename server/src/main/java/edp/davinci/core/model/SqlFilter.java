package edp.davinci.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONArray;

import edp.core.consts.Consts;
import edp.davinci.core.enums.SqlOperatorEnum;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class SqlFilter {

    private String name;

    private String type;

    private Object value;

    private String sqlType;

    private String operator;

    private List<SqlFilter> children;

    /**
     * 过滤器key
     */
    private String key;

    private static String pattern = "^'.*?'$";

    public SqlFilter copy() {
        SqlFilter sqlFilter = new SqlFilter();
        sqlFilter.setKey(key);
        sqlFilter.setName(name);
        sqlFilter.setOperator(operator);
        sqlFilter.setSqlType(sqlType);
        sqlFilter.setType(type);
        sqlFilter.setValue(value);
        if (null != children) {
            sqlFilter.setChildren(new ArrayList<>(children));
        }
        return sqlFilter;
    }

    public static class Type {
        public static final String filter = "filter";
        public static final String relation = "relation";
        public static final String and = "and";
        public static final String or = "or";
    }

    public enum NumericDataType {
        // mysql
        TINYINT("TINYINT"),
        SMALLINT("SMALLINT"),
        MEDIUMINT("MEDIUMINT"),
        INT("INT"),
        INTEGER("INTEGER"),
        BIGINT("BIGINT"),
        FLOAT("FLOAT"),
        DOUBLE("DOUBLE"),
        DECIMAL("DECIMAL"),
        NUMERIC("NUMERIC"),
        // clickhouse
        INT8("INT8"),
        INT16("INT16"),
        INT32("INT32"),
        INT64("INT64"),
        FLOAT32("FLOAT32"),
        FLOAT64("FLOAT64"),

        NULLABLE_INT8("NULLABLE(INT8)"),
        NULLABLE_INT16("NULLABLE(INT16)"),
        NULLABLE_INT32("NULLABLE(INT32)"),
        NULLABLE_INT64("NULLABLE(INT64)"),
        NULLABLE_FLOAT32("NULLABLE(FLOAT32)"),
        NULLABLE_FLOAT64("NULLABLE(FLOAT64)");

        private String type;

        NumericDataType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static String dealFilter(SqlFilter filter) {
        StringBuilder condition = new StringBuilder();
        String type = filter.getType();

        if (Type.filter.equalsIgnoreCase(type)) {
            condition.append(dealOperator(filter));
        }

        if (Type.relation.equalsIgnoreCase(type)) {
            List<SqlFilter> childs = filter.getChildren();
            condition.append(Consts.PARENTHESES_START);
            for (int i = 0; i < childs.size(); i++) {
                condition.append(i == 0 ? dealFilter(childs.get(i)) : Consts.SPACE + filter.getValue().toString() + Consts.SPACE + dealFilter(childs.get(i)));
            }
            condition.append(Consts.PARENTHESES_END);
        }

        return condition.toString();
    }

    private static String dealOperator(SqlFilter filter) {
        String name = filter.getName();
        Object value = filter.getValue();
        String operator = filter.getOperator();
        String sqlType = filter.getSqlType();

        Criterion criterion;
        if (SqlOperatorEnum.BETWEEN.getValue().equalsIgnoreCase(operator)) {
            JSONArray values = (JSONArray) value;
            criterion = new Criterion(name, operator, values.get(0), values.get(1), sqlType);
        } else {
            criterion = new Criterion(name, operator, value, sqlType);
        }

        return generator(criterion);
    }

    public static String generator(Criterion criterion) {
        StringBuilder whereClause = new StringBuilder();

        // REGEXP/NOT REGEXP: treat regex as SQL string literal to avoid SQL injection
        if (SqlOperatorEnum.REGEXP.getValue().equalsIgnoreCase(criterion.getOperator())
                || SqlOperatorEnum.NOTREGEXP.getValue().equalsIgnoreCase(criterion.getOperator())) {
            String value = criterion.getValue().toString();
            whereClause.append(criterion.getColumn())
                    .append(Consts.SPACE).append(criterion.getOperator()).append(Consts.SPACE)
                    .append(Consts.APOSTROPHE).append(value.replace(Consts.APOSTROPHE, Consts.APOSTROPHE + Consts.APOSTROPHE)).append(Consts.APOSTROPHE);
            return whereClause.toString();
        }

        if (criterion.isSingleValue()) {
            //column='value'
            String value = criterion.getValue().toString();
            whereClause.append(criterion.getColumn() + Consts.SPACE + criterion.getOperator() + Consts.SPACE);
            if (criterion.isNeedApostrophe() && !Pattern.matches(pattern, value)) {
                whereClause.append(Consts.APOSTROPHE + value + Consts.APOSTROPHE);
            } else {
                whereClause.append(value);
            }

        } else if (criterion.isBetweenValue()) {
            //column>='' and column<=''
            String value1 = criterion.getValue().toString();
            whereClause.append(Consts.PARENTHESES_START);
            whereClause.append(criterion.getColumn() + Consts.SPACE + SqlOperatorEnum.GREATERTHANEQUALS.getValue() + Consts.SPACE);
            if (criterion.isNeedApostrophe() && !Pattern.matches(pattern, value1)) {
                whereClause.append(Consts.APOSTROPHE + value1 + Consts.APOSTROPHE);
            } else {
                whereClause.append(value1);
            }
            whereClause.append(Consts.SPACE + SqlFilter.Type.and + Consts.SPACE);
            whereClause.append(criterion.getColumn() + Consts.SPACE + SqlOperatorEnum.MINORTHANEQUALS.getValue() + Consts.SPACE);
            String value2 = criterion.getSecondValue().toString();
            if (criterion.isNeedApostrophe() && !Pattern.matches(pattern, value2)) {
                whereClause.append(Consts.APOSTROPHE + value2 + Consts.APOSTROPHE);
            } else {
                whereClause.append(value2);
            }
            whereClause.append(Consts.PARENTHESES_END);

        } else if (criterion.isListValue()) {
            List values = (List) criterion.getValue();
            //column in ()
            whereClause.append(criterion.getColumn() + Consts.SPACE + criterion.getOperator() + Consts.SPACE);
            whereClause.append(Consts.PARENTHESES_START);
            if (criterion.isNeedApostrophe() && !Pattern.matches(pattern, values.get(0).toString())) {
                whereClause.append(Consts.APOSTROPHE +
                        StringUtils.join(values, Consts.APOSTROPHE + Consts.COMMA + Consts.APOSTROPHE) +
                        Consts.APOSTROPHE);
            } else {
                whereClause.append(StringUtils.join(values, Consts.COMMA));
            }
            whereClause.append(Consts.PARENTHESES_END);
        }
        return whereClause.toString();
    }


}
