package edp.davinci.common.utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLExprUtils;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edp.core.consts.Consts;
import edp.core.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * @author zswu3
 * @Description 利用druid的sql解析器进行sql的解析优化
 * @date 2021/1/8
 */
@Slf4j
public class DruidSqlOptimizeUtil {

    /**
     * sql优化
     * 1、消费掉select *
     * 2、从顶向下优化掉未用到的select字段
     * 3、尽可能降低select层数
     * 4、过滤重复筛选条件
     * 注：该方法仅面向白泽sql环境
     *
     * @param sql
     * @param dbType 数据库类型
     * @return
     */
    public static String optimizeSql(String sql, String dbType) {
        try {
            SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(sql, dbType);
            SQLStatement sqlStatement = sqlStatementParser.parseStatement();
            // 仅针对select语句进行优化
            if (!(sqlStatement instanceof SQLSelectStatement)) {
                return sql;
            }
            log.info("优化前:" + sql);
            SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) sqlStatement;
            // 递归优化查询sql
            loopOptimizeSqlSelect(sqlSelectStatement.getSelect(), null, null, dbType);
            log.info("优化后:" + sqlSelectStatement.toString());
            return sqlSelectStatement.toString();
        } catch (Exception e) {
            log.error("优化异常:" + sql);
            return sql;
        }

    }

    public static void main(String[] args) {
        String sql = "SELECT\n" +
                "\tcount(\n" +
                "\t\tDISTINCT\n" +
                "\t\tIF (\n" +
                "\t\t\texam_id IS NOT NULL,\n" +
                "\t\t\tconcat(school_id, grade_code),\n" +
                "\t\t\tNULL\n" +
                "\t\t)\n" +
                "\t) AS \"customize(考试学校数)\"\n" +
                "FROM\n" +
                "\t(\n" +
                "\t\tSELECT\n" +
                "\t\t\tCASE\n" +
                "\t\tWHEN exam_type_code = 'weeklyExam' THEN\n" +
                "\t\t\t'周考'\n" +
                "\t\tWHEN exam_type_code = 'monthlyExam' THEN\n" +
                "\t\t\t'月考'\n" +
                "\t\tWHEN exam_type_code = 'midtermExam' THEN\n" +
                "\t\t\t'期中考试'\n" +
                "\t\tWHEN exam_type_code = 'terminalExam' THEN\n" +
                "\t\t\t'期末考试'\n" +
                "\t\tELSE\n" +
                "\t\t\t'其他'\n" +
                "\t\tEND exam_type_name_new,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.`program_type`,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.`scan_min_time`,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.school_id,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.`province_name`,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.grade_code,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.`city_name`,\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route.`city_type`\n" +
                "\tFROM\n" +
                "\t\tsh_dv_area_school_exam_info_ck_route\n" +
                "\tWHERE\n" +
                "\t\tscan_min_time != 'bg-1'\n" +
                "\tAND exam_type_code IN (\n" +
                "\t\t'weeklyExam',\n" +
                "\t\t'monthlyExam',\n" +
                "\t\t'midtermExam',\n" +
                "\t\t'terminalExam'\n" +
                "\t)\n" +
                "\tAND `program_type` = '校园运营'\n" +
                "\tAND `city_type` = '2类'\n" +
                "\tAND `province_name` = '湖南省'\n" +
                "\tAND `city_name` = '长沙市'\n" +
                "\tAND `scan_min_time` >= '2021-01-01'\n" +
                "\tAND `scan_min_time` <= '2021-01-31'\n" +
                "\tAND `exam_type_name_new` IN ('期末考试', '月考')\n" +
                "\t) T";
        String newSql = optimizeSql(sql, "clickhouse");
        System.out.println(newSql);
    }

    /**
     * @param sqlSelect 当前select 解析sql
     * @param columns   上层select使用到的的字段
     */
    private static void loopOptimizeSqlSelect(SQLSelect sqlSelect, Set<String> columns, SQLSelect parentSqlSelect, String dbType) {
        if (null == sqlSelect) {
            return;
        }

        SQLSelectQueryBlock queryBlock = sqlSelect.getQueryBlock();
        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        SQLExpr where = queryBlock.getWhere();
        // where过滤条件去重
        loopDistinctWhere(where);


        // 本层select是否可以从sql树中移除, 判断依据有：
        // 1、 当前是否是最顶层，最顶层不可省略
        // 2、 上层使用到的查询字段是否可以在本层直接查询出来(不使用别名)
        boolean ignorable = null != parentSqlSelect;

        // 多余的查询列
        Set<SQLSelectItem> removeSet = Sets.newHashSet();
        // 本层使用到的字段
        Set<String> usedColumns = Sets.newHashSet();

        for (SQLSelectItem selectItem : selectList) {
            SQLExpr expr = selectItem.getExpr();
            // 本层查询结果列
            String column = selectItem.getAlias();

            // 单个字段
            if (expr instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr identifierExpr = (SQLIdentifierExpr) expr;
                column = StringUtils.isEmpty(column) ? identifierExpr.getName() : column;
                if (columns == null || columns.contains(column)) {
                    usedColumns.add(identifierExpr.getName());
                }
            }
            // select *
            else if (expr instanceof SQLAllColumnExpr) {
                removeSet.add(selectItem);
            }
            // 其他情况按函数处理
            else {
                loopQueryColumnsFromFunction(Lists.newArrayList(expr), usedColumns, dbType);
            }

            if (null == columns) {
                continue;
            }

            if (StringUtils.isNotEmpty(column) && isUsedAlias(column, columns)) {
                // 如果字段设置了别名且columns使用到了这个别名，则本层查询不能被省略
                ignorable = StringUtils.isEmpty(selectItem.getAlias()) && ignorable;
                // columns 移除从本层中查询出来的列
                columns.remove(column);
            } else {
                removeSet.add(selectItem);
            }
        }


        SQLSelect nextParentSqlSelect = sqlSelect;
        // 移除本层
        if (ignorable) {
            nextParentSqlSelect = parentSqlSelect;
            SQLSelectQueryBlock parentQueryBlock = parentSqlSelect.getQueryBlock();
            // 本层的where条件移到上一层
            if (!SQLExprUtils.equals(parentQueryBlock.getWhere(), where)) {
                parentQueryBlock.addWhere(where);
            }
            queryBlock.getFrom().setAlias(parentQueryBlock.getFrom().getAlias());
            parentQueryBlock.setFrom(queryBlock.getFrom());

            if (!CollectionUtils.isEmpty(columns)) {
                usedColumns.addAll(columns);
            }
        } else {
            // 移除本层多余的查询列，同时添加上层使用到而本层未直接查询出出来的字段
            selectList.removeAll(removeSet);
            if (!CollectionUtils.isEmpty(columns)) {
                columns.forEach(column -> {
                    queryBlock.addSelectItem(new SQLIdentifierExpr(column));
                    usedColumns.add(column);
                });
            }
        }

        SQLTableSource fromTable = queryBlock.getFrom();
        if (fromTable instanceof SQLSubqueryTableSource) {
            loopOptimizeSqlSelect(((SQLSubqueryTableSource) fromTable).getSelect(), usedColumns, nextParentSqlSelect, dbType);
        }
    }

    /**
     * 递归获取sql函数中使用到的字段
     *
     * @param exprs
     * @param columns
     * @param dbType
     * @return
     */
    private static Set<String> loopQueryColumnsFromFunction(List<SQLExpr> exprs, Set<String> columns, String dbType) {
        for (SQLExpr expr : exprs) {
            List<SQLExpr> parameters = null;
            if (expr instanceof SQLIdentifierExpr) {
                columns.add(((SQLIdentifierExpr) expr).getName());
            } else if (expr instanceof SQLMethodInvokeExpr) {
                SQLMethodInvokeExpr sqlMethodInvokeExpr = (SQLMethodInvokeExpr) expr;
                parameters = getSqlMethodParamtersExLambda(sqlMethodInvokeExpr);
            } else if (expr instanceof SQLAggregateExpr) {
                SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
                parameters = aggregateExpr.getArguments();
            } else if (expr instanceof SQLBinaryOpExpr) {
                SQLBinaryOpExpr binaryOpExpr = (SQLBinaryOpExpr) expr;
                parameters = Lists.newArrayList(binaryOpExpr.getLeft(), binaryOpExpr.getRight());
            } else if (expr instanceof SQLCaseExpr) {
                SQLCaseExpr sqlCaseExpr = (SQLCaseExpr) expr;
                List<SQLCaseExpr.Item> items = sqlCaseExpr.getItems();
                parameters = Lists.newArrayList();
                for (SQLCaseExpr.Item item : items) {
                    parameters.add(item.getConditionExpr());
                    parameters.add(item.getValueExpr());
                }
            }

            if (!CollectionUtils.isEmpty(parameters)) {
                loopQueryColumnsFromFunction(parameters, columns, dbType);
            }
        }
        return columns;
    }

    /**
     * 获取方法参数, 排除lambda参数
     *
     * @param expr
     * @return
     */
    private static List<SQLExpr> getSqlMethodParamtersExLambda(SQLMethodInvokeExpr expr) {
        List<SQLExpr> parameters = expr.getParameters();
        if (!Consts.CLICKHOUSE_HIGHER_ORDER_FUNCTIONS.contains(expr.getMethodName().toUpperCase())) {
            return parameters;
        }
        int startIndex = 0;
        for (int i = 0; i < parameters.size(); i++) {
            SQLExpr sqlExpr = parameters.get(i);
            // 判断是否为lambda表达式, 以是否包含 -> 作为判断依据
            boolean isLambda = Optional.ofNullable(sqlExpr)
                    .map(s -> s instanceof SQLBinaryOpExpr ? ((SQLBinaryOpExpr) s).getLeft() : null)
                    .map(l -> l instanceof SQLBinaryOpExpr ? (SQLBinaryOpExpr) l : null)
                    .map(o -> SQLBinaryOperator.SubGt.equals(o.getOperator()) ? true : false).orElse(false);
            startIndex = isLambda ? i + 1 : startIndex;
        }
        return parameters.subList(startIndex, parameters.size());
    }

    /**
     * where 过滤条件去重
     *
     * @param where 条件表达式
     */
    private static SQLExpr loopDistinctWhere(SQLExpr where) {
        boolean whereIsBinary = where instanceof SQLBinaryOpExpr;
        if (!whereIsBinary) {
            return where;
        }
        SQLBinaryOpExpr binary = (SQLBinaryOpExpr) where;
        boolean leftIsBinary = binary.getLeft() instanceof SQLBinaryOpExpr;
        boolean rightIsBinary = binary.getRight() instanceof SQLBinaryOpExpr;
        // 条件去重
        boolean repeat = Optional.ofNullable(binary)
                .map(s -> leftIsBinary && rightIsBinary && SQLExprUtils.equals(s.getLeft(), s.getRight()))
                .orElse(false);
        binary.setLeft(loopDistinctWhere(binary.getLeft()));
        // 未重复
        if (!repeat) {
            binary.setRight(loopDistinctWhere(binary.getRight()));
            return where;
        } else {
            return binary.getLeft();
        }
    }


    /**
     * 是否使用了别名
     *
     * @param column
     * @param columnNames
     * @return
     */
    private static boolean isUsedAlias(String column, Set<String> columnNames) {
        boolean result = false;
        if (!StringUtils.isEmpty(column)) {
            String aliasName = column.trim();
            String extraAliasName = aliasName;
            if (extraAliasName.indexOf("`") > -1) {
                extraAliasName = extraAliasName.replace("`", "");
            } else {
                extraAliasName = "`" + extraAliasName + "`";
            }
            if (columnNames.contains(aliasName) || columnNames.contains(extraAliasName)) {
                columnNames.remove(aliasName);
                columnNames.remove(extraAliasName);
                result = true;
            }
        }
        return result;
    }
}
