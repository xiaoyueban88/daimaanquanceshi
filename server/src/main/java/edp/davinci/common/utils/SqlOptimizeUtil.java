package edp.davinci.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.util.JdbcConstants;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edp.core.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * Created by admin on 2020-01-15.
 */
@Slf4j
public class SqlOptimizeUtil {

    public static String optimizeCKSql(String sql) {
        Statement st = null;
        try {
            st = CCJSqlParserUtil.parse(sql);
//            new MySqlStatementParser()
            if (st instanceof Select) {
                Select s = (Select) st;
                SelectBody sb = s.getSelectBody();
                PlainSelect plainSelect = (PlainSelect) sb;
                List<SelectItem> selectItemLists = plainSelect.getSelectItems();
                // 获取外层select别名
                Set<String> outSelectAlias = Sets.newHashSet();
                selectItemLists.forEach(item -> {
                    if(item instanceof SelectExpressionItem) {
                        SelectExpressionItem expressionItem = (SelectExpressionItem) item;
                        if(null != expressionItem.getAlias()) {
                            outSelectAlias.add(expressionItem.getAlias().getName());
                        }
                    }
                });
                List<Expression> parExressions = getExpressionsBySelect(selectItemLists);
                // 获取最上层select所有columnName
                Set<String> columnNames = Sets.newHashSet();
                getColummsNameFromExpression(parExressions, columnNames);

                // 获取最外层where字段
                getColummsNameFromExpression(Lists.newArrayList(plainSelect.getWhere()), columnNames);


                // 获取最外层group包含的字段
                if (null != plainSelect.getGroupBy()) {
                    getColummsNameFromExpression(plainSelect.getGroupBy().getGroupByExpressions(), columnNames);
                }

                // 获取最外层排序字段名称
                List<Expression> orderExpressions = Lists.newArrayList();
                List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
                if (!CollectionUtils.isEmpty(orderByElements)) {
                    orderByElements.forEach(order -> {
                        orderExpressions.add(order.getExpression());
                    });
                }
                getColummsNameFromExpression(orderExpressions, columnNames);
                // 移除columnNames与outSelectAlias冲突的
                columnNames = distinctSource(columnNames, outSelectAlias);

                FromItem item = plainSelect.getFromItem();
                if (item instanceof SubSelect) {
                    SubSelect subSelect = (SubSelect) item;
                    Alias alias = subSelect.getAlias();
                    SelectBody innerSb = subSelect.getSelectBody();
                    PlainSelect innerPlainSelect = (PlainSelect) innerSb;

                    // 处理计算字段及冗余字段问题
                    List<SelectItem> selectItems = innerPlainSelect.getSelectItems();

                    if (null != innerPlainSelect.getGroupBy()) {
                        getColummsNameFromExpression(innerPlainSelect.getGroupBy().getGroupByExpressions(), columnNames);
                    }
                    // 内存where使用到的字段
                    Set<String> whereUsedColumns = Sets.newHashSet();
                    if(null != innerPlainSelect.getWhere()) {
                        getColummsNameFromExpression(Lists.newArrayList(innerPlainSelect.getWhere()), whereUsedColumns);
                    }

                    // 取出whereUsedColumns用到的select别名
                    for (SelectItem si : selectItems) {
                        if (si instanceof SelectExpressionItem) {
                            SelectExpressionItem selectExpressionItem = (SelectExpressionItem) si;
                            if(isUsedAlias(selectExpressionItem, whereUsedColumns)) {
                                columnNames.add(selectExpressionItem.getAlias().getName());
                            }
                        }
                    }

                    // 子select语句是否需要忽略
                    boolean ignoreSub = true;
                    // 遍历子查询语句中的selectItem,并移除未使用的
                    Iterator<SelectItem> selectItemsIterator = selectItems.iterator();
                    while (selectItemsIterator.hasNext()) {
                        SelectItem selectItem = selectItemsIterator.next();
                        if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
                            selectItemsIterator.remove();
                            continue;
                        }
                        if (selectItem instanceof SelectExpressionItem) {
                            SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                            if (isUsedAlias(selectExpressionItem, columnNames)) {
                                ignoreSub = false;
                            } else {
                                selectItemsIterator.remove();
                            }
                        }
                    }

                    Expression where = innerPlainSelect.getWhere();
                    FromItem innerItem = innerPlainSelect.getFromItem();

                    Table tb = ((Table) innerItem);
                    String tableName = tb.getFullyQualifiedName();
                    Table tableItem = new Table(tableName);
                    if (alias != null) {
                        tableItem.setAlias(alias);
                    }

                    // 如果子查询语句不能忽略，补充父查询语句中使用到而子查询语句中未进行查询的字段
                    if (!ignoreSub) {
                        if (!CollectionUtils.isEmpty(columnNames)) {
                            columnNames.forEach(colName -> {
                                Column column = new Column();
                                column.setColumnName(colName);
                                column.setTable(tb);
                                column.setASTNode(tb.getASTNode());
                                selectItems.add(new SelectExpressionItem(column));
                            });
                        }
                        innerPlainSelect.setSelectItems(selectItems);
                        if (where != null && plainSelect.getWhere() != null && innerPlainSelect.getGroupBy() == null) {
                            AndExpression andExpression = new AndExpression(where, plainSelect.getWhere());
                            innerPlainSelect.setWhere(andExpression);
                            plainSelect.setWhere(null);
                        }
                        return st.toString();
                    }


                    plainSelect.setFromItem(tableItem);
                    if (where != null) {
                        if (plainSelect.getWhere() != null) {
                            AndExpression andExpression = new AndExpression(where, plainSelect.getWhere());
                            plainSelect.setWhere(andExpression);
                        } else {
                            plainSelect.setWhere(where);
                        }
                    }
                }
            }
            return st.toString();
        } catch (JSQLParserException e) {
            log.error("sql解析异常：" + sql);
            return sql;
        } catch (Exception ex) {
            log.error("sql解析异常：" + sql);
            return sql;
        }
    }


    private static String getTableName(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table) {
            Table tab = (Table) fromItem;
            return tab.getName();
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            SelectBody selectBody = subSelect.getSelectBody();
            return getTableName((PlainSelect) selectBody);
        } else {
            return null;
        }
    }

    /**
     * 是否使用了别名
     *
     * @param selectItem
     * @param columnNames
     * @return
     */
    private static boolean isUsedAlias(SelectExpressionItem selectItem, Set<String> columnNames) {
        boolean result = false;
        Alias alias = selectItem.getAlias();
        if (null != alias) {
            String aliasName = alias.getName();
            String extraAliasName = aliasName;
            if (extraAliasName.contains("`")) {
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

    /**
     * 获取表达式中的字段columnName
     *
     * @param expressions
     * @param columnNames
     */
    private static void getColummsNameFromExpression(List<Expression> expressions, Set<String> columnNames) {
        if (!CollectionUtils.isEmpty(expressions)) {
            for (Expression expression : expressions) {
                if (null == expression) {
                    continue;
                }
                if (expression instanceof Column) {
                    Column column = (Column) expression;
                    String extracolumnName = column.getColumnName();
                    if (extracolumnName.contains("`")) {
                        extracolumnName = extracolumnName.replace("`", "");
                    } else {
                        extracolumnName = "`" + extracolumnName + "`";
                    }
                    if (!columnNames.contains(extracolumnName)) {
                        columnNames.add(((Column) expression).getColumnName());
                    }
                } else if (expression instanceof Function) {
                    Function function = (Function) expression;
                    if (null == function.getParameters()) {
                        continue;
                    }
                    getColummsNameFromExpression(function.getParameters().getExpressions(), columnNames);
                } else if (expression instanceof Division) {
                    Division division = (Division) expression;
                    Expression leftExpression = division.getLeftExpression();
                    Expression rightExpression = division.getRightExpression();
                    getColummsNameFromExpression(Lists.newArrayList(leftExpression, rightExpression), columnNames);
                } else if (expression instanceof AndExpression) {
                    AndExpression andExpression = (AndExpression) expression;
                    Expression leftExpression = andExpression.getLeftExpression();
                    Expression rightExpression = andExpression.getRightExpression();
                    getColummsNameFromExpression(Lists.newArrayList(leftExpression, rightExpression), columnNames);
                } else if (expression instanceof InExpression) {
                    InExpression inExpression = (InExpression) expression;
                    getColummsNameFromExpression(Lists.newArrayList(inExpression.getLeftExpression()), columnNames);
                } else if (expression instanceof CaseExpression) {
                    CaseExpression caseExpression = (CaseExpression) expression;
                    List<WhenClause> whenClauses = caseExpression.getWhenClauses();
                    List<Expression> caseExpressionList = Lists.newArrayList();
                    whenClauses.forEach(when -> {
                        caseExpressionList.add(when.getThenExpression());
                        caseExpressionList.add(when.getWhenExpression());
                    });
                    caseExpressionList.add(caseExpression.getElseExpression());
                    caseExpressionList.add(caseExpression.getSwitchExpression());
                    getColummsNameFromExpression(caseExpressionList, columnNames);
                } else if (expression instanceof BinaryExpression) {
                    BinaryExpression binaryExpression = (BinaryExpression) expression;
                    Expression leftExpression = binaryExpression.getLeftExpression();
                    Expression rightExpression = binaryExpression.getRightExpression();
                    getColummsNameFromExpression(Lists.newArrayList(leftExpression, rightExpression), columnNames);
                } else if (expression instanceof Parenthesis) {
                    Parenthesis parenthesis = (Parenthesis) expression;
                    getColummsNameFromExpression(Lists.newArrayList(parenthesis.getExpression()), columnNames);
                } else if (expression instanceof CastExpression) {
                    CastExpression castExpression = (CastExpression) expression;
                    Expression leftExpression = castExpression.getLeftExpression();
                    getColummsNameFromExpression(Lists.newArrayList(leftExpression), columnNames);
                }
            }
        }
    }

    public static List<Expression> getExpressionsBySelect(List<SelectItem> selectItems) {
        List<Expression> expressions = Lists.newArrayList();
        for (SelectItem select : selectItems) {
            if (!(select instanceof AllColumns) && !(select instanceof AllTableColumns)) {
                SelectExpressionItem expressionItem = (SelectExpressionItem) select;
                Expression expression = expressionItem.getExpression();
                if (expression != null) {
                    expressions.add(expression);
                }
            }
        }
        return expressions;
    }

    public static Set<String> distinctSource(Set<String> source, Set<String> sets) {
        Set<String> distinctSets = Sets.newHashSet();
        source.forEach(s -> {
            String temp = s.replaceAll("`", "");
            String alias = new StringBuilder("\"").append(temp).append("\"").toString();
            if(!sets.contains(temp) && !sets.contains(alias)) {
                distinctSets.add(s);
            }
        });
        return distinctSets;
    }

    public static void main(String[] args) {
        String sql = "SELECT\n" +
                "\t`student_name` AS \"original(student_name)\",\n" +
                "\t`student_status` AS \"original(student_status)\"\n" +
                "FROM\n" +
                "\t(\n" +
                "\t\tSELECT\n" +
                "\n" +
                "\t\tIF (\n" +
                "\t\t\tcount(student_id) = 3,\n" +
                "\t\t\t'优秀',\n" +
                "\t\t\t'不优秀'\n" +
                "\t\t) AS student_status,\n" +
                "\t\tCASE\n" +
                "\tWHEN '2' = '2' THEN\n" +
                "\t\tcity_id\n" +
                "\tWHEN '2' = '3' THEN\n" +
                "\t\tdistrict_id\n" +
                "\tEND AS range_detail_id,\n" +
                "\tdws_qyjy_student_subject_fact.phase_code,\n" +
                "\tdws_qyjy_student_subject_fact.subject_code,\n" +
                "\tdws_qyjy_student_subject_fact.teacher_name,\n" +
                "\tdws_qyjy_student_subject_fact.class_id,\n" +
                "\tdws_qyjy_student_subject_fact.school_name,\n" +
                "\tdws_qyjy_student_subject_fact.student_id,\n" +
                "\tdws_qyjy_student_subject_fact.student_code,\n" +
                "\tdws_qyjy_student_subject_fact.province_name,\n" +
                "\tdws_qyjy_student_subject_fact.district_name,\n" +
                "\tdws_qyjy_student_subject_fact.city_name,\n" +
                "\tdws_qyjy_student_subject_fact.school_id,\n" +
                "\tdws_qyjy_student_subject_fact.subject_name,\n" +
                "\tdws_qyjy_student_subject_fact.`student_name`,\n" +
                "\tdws_qyjy_student_subject_fact.phase_name,\n" +
                "\tdws_qyjy_student_subject_fact.grade_code,\n" +
                "\tdws_qyjy_student_subject_fact.class_name,\n" +
                "\tdws_qyjy_student_subject_fact.grade_name\n" +
                "FROM\n" +
                "\tdws_qyjy_student_subject_fact\n" +
                "WHERE\n" +
                "\ttoInt64 (student_rank) <= 100\n" +
                "AND range_detail_id = '882'\n" +
                "AND dws_qyjy_student_subject_fact.exam_id IN (\n" +
                "\t'f1afa59e-dcff-4564-b1e4-2a0455ba402a',\n" +
                "\t'ce521226-140d-4a3e-ba7d-68a1cb003f8c',\n" +
                "\t'6e404c1f-62d3-481d-be93-70ff89b33ea8'\n" +
                ")\n" +
                "AND phase_code IN ('05')\n" +
                "AND `city_name` = '南京市'\n" +
                "AND `subject_name` = '总分'\n" +
                "GROUP BY\n" +
                "\tprovince_name,\n" +
                "\tcity_name,\n" +
                "\tdistrict_name,\n" +
                "\tschool_id,\n" +
                "\tschool_name,\n" +
                "\tclass_id,\n" +
                "\tclass_name,\n" +
                "\tphase_code,\n" +
                "\tphase_name,\n" +
                "\tsubject_code,\n" +
                "\tsubject_name,\n" +
                "\tgrade_code,\n" +
                "\tgrade_name,\n" +
                "\tteacher_name,\n" +
                "\tstudent_code,\n" +
                "\tstudent_id,\n" +
                "\tstudent_name,\n" +
                "\trange_detail_id\n" +
                "\t) T\n" +
                "where `student_status` = '优秀'";
        String s = optimizeCKSql(sql);
        System.out.println(s);
    }
}
