package edp.davinci.dto.viewDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iflytek.edu.elp.common.util.StringUtils;

import edp.core.consts.Consts;
import edp.core.exception.ServerException;
import edp.core.utils.MatchUtils;
import edp.core.utils.UUIDUtils;
import lombok.Builder;
import lombok.Data;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/4
 */
@Data
@Builder
public class UdfFunctionDto {
    /**
     * 自定义函数名
     */
    private String name;

    /**
     * 自定义函数要计算查询的字段或表达式
     */
    private String expression;

    /**
     * 自定义函数计算基于的分组
     */
    private String groupArray;

    /**
     * 排序反转, 默认为false
     */
    private boolean reverse = false;

    /**
     * 过滤条件
     */
    private String filter;

    /**
     * 自定义函数查询结果别名
     */
    private String alias;


    public static UdfFunctionDto buildUdfFunctionDto(String functionStr, String functionName) {
        // 生成别名
        String alias = UUIDUtils.generateShortUuid();
        String paramStr = MatchUtils.getInnerParenthesesStr(functionStr);
        if (StringUtils.isEmpty(paramStr)) {
            return null;
        }
        List<String> params = MatchUtils.getParams(paramStr);
        if (StringUtils.isEmpty(params.get(0))) {
            throw new ServerException("自定义函数解析失败, expression参数不能为空");
        }
        UdfFunctionDto udfFunctionDto = UdfFunctionDto.builder().alias(alias.trim())
                .name(functionName.trim()).expression(params.get(0).trim()).build();
        if (params.size() >= 2) {
            String groupBy = params.get(1).trim();
            if (!"null".equals(groupBy.toLowerCase())) {
                udfFunctionDto.setGroupArray(groupBy);
            }
        }
        if (params.size() >= 3) {
            String reverse = params.get(2).trim();
            if ("true".equals(reverse.toLowerCase())) {
                udfFunctionDto.setReverse(true);
            }
        }
        if (params.size() == 4) {
            String filter = params.get(3).trim();
            udfFunctionDto.setFilter(filter);
        }
        return udfFunctionDto;
    }

    private final List<String> OPERATOR_LIST = Lists.newArrayList(">", "<", ">=", "==", ">=", "<=", "!=", "=", "like", "not like");


    public String buildExpression(String column) {
        if (StringUtils.isEmpty(this.filter)) {
            return column;
        }
        // 拟拼接sql语句
        String sql = "select * from table where " + this.filter;
        // lambda args -> groupArrayName
        Map<String, String> map = Maps.newHashMap();
        String lambdaExpression = null;
        try {
            Statement parse = CCJSqlParserUtil.parse(sql);
            Select s = (Select) parse;
            SelectBody sb = s.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) sb;
            Expression where = plainSelect.getWhere();
            loopHandleWhere(Lists.newArrayList(where), map);
            lambdaExpression = where.toString().replace("null().", "") + "? arg : 0";
        } catch (JSQLParserException e) {
            throw new ServerException("自定义函数过滤表达式解析失败：" + this.filter);
        }

        // 拼接arrayMap函数表达式
        StringBuilder expression = new StringBuilder("arrayMap(arg");
        Set<String> keys = map.keySet();
        for (String key : keys) {
            expression.append(",").append(map.get(key));
        }
        expression.append(" -> ").append(lambdaExpression).append(",");
        expression.append(column).append(",");
        expression.append(StringUtils.join(keys, ","));
        expression.append(")");
        return expression.toString();
    }

    public static void loopHandleWhere(List<Expression> expressions, Map<String, String> groupArrayMap) {
        for (Expression expression : expressions) {
            if (expression instanceof BinaryExpression) {
                BinaryExpression newExpression = (BinaryExpression) expression;
                loopHandleWhere(Lists.newArrayList(newExpression.getLeftExpression(),
                        newExpression.getRightExpression()), groupArrayMap);
            } else if (expression instanceof Column) {
                Column column = (Column) expression;
                String columnName = column.getColumnName().trim();
                if (!groupArrayMap.containsKey(columnName)) {
                    groupArrayMap.put(columnName, Consts.LETTER_LIST.get(groupArrayMap.size()));
                }
                column.setColumnName(groupArrayMap.get(columnName));
            } else if (expression instanceof Function) {
                Function function = (Function) expression;
                if (Consts.CLICKHOUSE_ARRAY_FUNCTION.containsKey(function.getName().trim())) {
                    function.setName(Consts.CLICKHOUSE_ARRAY_FUNCTION.get(function.getName()));
                }
                String columnName = function.toString();
                if (!groupArrayMap.containsKey(columnName)) {
                    groupArrayMap.put(columnName, Consts.LETTER_LIST.get(groupArrayMap.size()));
                }
                function.setAttribute(new Column(null, groupArrayMap.get(columnName)));
                function.setName(null);
                function.setParameters(null);
            }
        }
    }
}
