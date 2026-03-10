/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.dto.viewDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edp.core.consts.Consts;
import edp.core.enums.DataTypeEnum;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.utils.AESUtils;
import edp.core.utils.CollectionUtils;
import edp.core.utils.MatchUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.common.utils.EncryptUtil;
import edp.davinci.common.utils.StringUtil;
import edp.davinci.model.Widget;
import lombok.Data;

import static edp.core.consts.Consts.EMPTY;
import static edp.core.consts.Consts.PARENTHESES_END;
import static edp.core.consts.Consts.PARENTHESES_START;
import static edp.core.consts.Consts.PATTERN_SQL_AGGREGATE;
import static edp.core.consts.Consts.SPACE;

@Data
public class ViewExecuteParam {
    private List<String> groups;
    private List<Aggregator> aggregators;
    private List<Order> orders;
    /**
     * where 过滤条件
     */
    private List<String> filters;

    /**
     * having 过滤条件
     */
    private List<String> havings;
    private List<Param> params;
    private Boolean cache;
    private Long expired;
    private Long widgetId;
    private Boolean flush = false;
    private int limit = 0;
    private int pageNo = -1;
    private int pageSize = -1;
    private int totalCount = 0;
    private String type;

    /**
     * 刷新间隔, 小于或等于0无效
     */
    private int flushInterval = 0;


    /**
     * 最终指标集合(最后一层进行计算),解析包含自定义函数的指标时产生
     */
    private List<Aggregator> finalAggregators;

    private boolean nativeQuery = false;

    /**
     * 列维度,制作交叉表时使用
     */
    private List<String> colGroups;

    /**
     * 行维度,制作交叉表时使用
     */
    private List<String> rowGroups;

    /**
     * 交叉排序, 用于透视表中某一列指标的排序
     */
    private CrossOrder crossOrder;

    private List<Order> colOrders;

    private List<Order> rowOrders;

    /**
     * widget信息
     */
    private Widget widget;

    private String nonce;

    private Long t;

    private String sign;



    public ViewExecuteParam() {
    }


    public ViewExecuteParam(int limit, int pageNo, int pageSize, int totalCount) {
        this.limit = limit;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public ViewExecuteParam(List<String> groupList,
                            List<Aggregator> aggregators,
                            List<Order> orders,
                            List<String> filterList,
                            List<Param> params,
                            Boolean cache,
                            Long expired,
                            Boolean nativeQuery) {
        this.groups = groupList;
        this.aggregators = aggregators;
        this.orders = orders;
        this.filters = filterList;
        this.params = params;
        this.cache = cache;
        this.expired = expired;
        this.nativeQuery = nativeQuery;
    }

    public ViewExecuteParam(List<String> groupList,
                            List<Aggregator> aggregators,
                            List<Order> orders,
                            List<String> filterList,
                            List<Param> params,
                            Boolean cache,
                            Long expired,
                            Boolean nativeQuery,
                            String type) {
        this.groups = groupList;
        this.aggregators = aggregators;
        this.orders = orders;
        this.filters = filterList;
        this.params = params;
        this.cache = cache;
        this.expired = expired;
        this.nativeQuery = nativeQuery;
        this.type = type;
    }

    public List<String> getGroups() {
        if (!CollectionUtils.isEmpty(this.groups)) {
            this.groups = groups.stream().filter(g -> !StringUtils.isEmpty(g)).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(this.groups)) {
            return null;
        }

        return this.groups;
    }

    public List<String> getFilters() {
        if (!CollectionUtils.isEmpty(this.filters)) {
            this.filters = filters.stream().filter(f -> !StringUtils.isEmpty(f)).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(this.filters)) {
            return null;
        }

        return this.filters;
    }

    public List<Order> getOrders(String jdbcUrl, String dbVersion) {
        return SqlUtils.getOrders(this.orders, jdbcUrl, dbVersion);
    }

    public List<Order> getColOrders(String jdbcUrl, String dbVersion) {
        List<Order> orders = SqlUtils.getOrders(this.colOrders, jdbcUrl, dbVersion);
        if(!CollectionUtils.isEmpty(orders)) {
            orders.forEach(order -> {
                if("ASC".equals(order.getDirection().toUpperCase())) {
                    order.setColumn(new StringBuilder("min(").append(order.getColumn()).append(")").toString());
                } else {
                    order.setColumn(new StringBuilder("max(").append(order.getColumn()).append(")").toString());
                }
            });
        }
        return orders;
    }

    public String getOriOrderColumn(Order order, DataTypeEnum dataTypeEnum) {
        String prefix = dataTypeEnum.getKeywordPrefix();
        return order.getColumn().replace(prefix, "");
    }

    public void addExcludeColumn(Set<String> excludeColumns, String jdbcUrl, String dbVersion) {
        if (!CollectionUtils.isEmpty(excludeColumns) && !CollectionUtils.isEmpty(aggregators)) {
            excludeColumns.addAll(this.aggregators.stream()
                    .filter(a -> !CollectionUtils.isEmpty(excludeColumns) && excludeColumns.contains(a.getColumn()))
                    .map(a -> formatColumn(a.getColumn(), a.getFunc(), jdbcUrl, dbVersion, true))
                    .collect(Collectors.toSet())
            );
        }
    }

    public List<String> getAggregators(String jdbcUrl, String dbVersion) {
        if (!CollectionUtils.isEmpty(aggregators)) {
            return this.aggregators.stream().map(a -> formatColumn(a.getColumn(), a.getFunc(), a.getCalculateRules(), jdbcUrl, dbVersion, false)).collect(Collectors.toList());
        }
        return null;
    }

    public List<String> getFinalAggregators(String jdbcUrl, String dbVersion) {
        if (!CollectionUtils.isEmpty(this.finalAggregators)) {
            return this.finalAggregators.stream().map(a -> formatColumn(a.getColumn(), a.getFunc(), a.getCalculateRules(), jdbcUrl, dbVersion, false)).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 获取签名字符串
     * @return
     */
    public boolean checkSign() {
        // 校验时间戳
        long time = new Date().getTime();
        // 不超过5分钟
        if(Math.abs(time - t) > 5 * 60 * 1000) {
            return false;
        }
        JSONObject object = new JSONObject();
        object.put("groups", groups);
        object.put("aggregators", aggregators);
        object.put("filters", filters);
        object.put("havings", havings);
        object.put("params", params);
        String paramString = StringUtil.getNormalString(JSON.toJSONString(object));
        String encodeParam = EncryptUtil.encodeSHA(paramString+nonce+t.toString());
        return encodeParam.equals(sign);
    }



    private String formatColumn(String column, String func, String jdbcUrl, String dbVersion, boolean isLable) {
        if (isLable) {
            return String.join(EMPTY, func.trim(), PARENTHESES_START, column.trim(), PARENTHESES_END);
        } else {
            StringBuilder sb = new StringBuilder();
            if ("COUNTDISTINCT".equals(func.trim().toUpperCase())) {
                sb.append("COUNT").append(PARENTHESES_START).append("DISTINCT").append(SPACE);
                sb.append(ViewExecuteParam.getField(column, jdbcUrl, dbVersion));
                sb.append(PARENTHESES_END);
                sb.append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion)).append("COUNTDISTINCT").append(PARENTHESES_START);
                sb.append(column);
                sb.append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            } else if ("VAR".equals(func.trim().toUpperCase())) {
                DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(jdbcUrl);
                // clickhouse 求方差函数与sqlserver区分开来
                if (dataTypeEnum == DataTypeEnum.CLICKHOUSE) {
                    sb.append("varPop").append(PARENTHESES_START);
                } else {
                    sb.append("VAR_POP").append(PARENTHESES_START);
                }
                sb.append(ViewExecuteParam.getField(column, jdbcUrl, dbVersion));
                sb.append(PARENTHESES_END);
                sb.append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion));
                sb.append(func.trim()).append(PARENTHESES_START);
                sb.append(column);
                sb.append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            } else if ("STDDEV".equals(func.trim().toUpperCase())) {
                DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(jdbcUrl);
                // clickhouse 求标准差函数与sqlserver区分开来
                if (dataTypeEnum == DataTypeEnum.CLICKHOUSE) {
                    sb.append("stddevPop").append(PARENTHESES_START);
                } else {
                    sb.append("STDDEV_POP").append(PARENTHESES_START);
                }
                sb.append(ViewExecuteParam.getField(column, jdbcUrl, dbVersion));
                sb.append(PARENTHESES_END);
                sb.append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion));
                sb.append(func.trim()).append(PARENTHESES_START);
                sb.append(column);
                sb.append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            }
            // 原始值
            else if ("ORIGINAL".equals(func.trim().toUpperCase())) {
                sb.append(ViewExecuteParam.getField(column, jdbcUrl, dbVersion));
                sb.append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion));
                sb.append(func.trim()).append(PARENTHESES_START);
                sb.append(column);
                sb.append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            } else {
                sb.append(func.trim()).append(PARENTHESES_START);
                sb.append(ViewExecuteParam.getField(column, jdbcUrl, dbVersion));
                sb.append(PARENTHESES_END);
                sb.append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion));
                sb.append(func.trim()).append(PARENTHESES_START);
                sb.append(column);
                sb.append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            }

            return sb.toString();
        }
    }

    private String formatColumn(String column, String func, String calculateRules, String jdbcUrl, String dbVersion, boolean isLable) {
        if ("customize".equals(func)) {
            StringBuilder sb = new StringBuilder();
            sb.append(calculateRules).append(" AS ").append(SqlUtils.getAliasPrefix(jdbcUrl, dbVersion));
            sb.append(func.trim()).append(PARENTHESES_START).append(column).append(PARENTHESES_END).append(SqlUtils.getAliasSuffix(jdbcUrl, dbVersion));
            return sb.toString();
        } else {
            return formatColumn(column, func, jdbcUrl, dbVersion, isLable);
        }
    }

    public String getMetricKey(String column, String func, String calculateRules, String jdbcUrl, String dbVersion, boolean isLable) {
        if ("customize".equals(func)) {
            StringBuilder sb = new StringBuilder();
            sb.append(func.trim()).append(PARENTHESES_START).append(column).append(PARENTHESES_END);
            return sb.toString();
        } else {
            if (isLable) {
                return String.join(EMPTY, func.trim(), PARENTHESES_START, column.trim(), PARENTHESES_END);
            } else {
                StringBuilder sb = new StringBuilder();
                if ("COUNTDISTINCT".equals(func.trim().toUpperCase())) {
                    sb.append(column);
                    sb.append(PARENTHESES_END);
                } else if ("VAR".equals(func.trim().toUpperCase())) {
                    sb.append(func.trim()).append(PARENTHESES_START);
                    sb.append(column);
                    sb.append(PARENTHESES_END);
                } else if ("STDDEV".equals(func.trim().toUpperCase())) {
                    sb.append(func.trim()).append(PARENTHESES_START);
                    sb.append(column);
                    sb.append(PARENTHESES_END);
                }
                // 原始值
                else if ("ORIGINAL".equals(func.trim().toUpperCase())) {
                    sb.append(func.trim()).append(PARENTHESES_START);
                    sb.append(column);
                    sb.append(PARENTHESES_END);
                } else {
                    sb.append(func.trim()).append(PARENTHESES_START);
                    sb.append(column);
                    sb.append(PARENTHESES_END);
                }

                return sb.toString();
            }
        }
    }

    public String formatAlias(String column, String func, String jdbcUrl, String dbVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append(SqlUtils.getKeywordPrefix(jdbcUrl, dbVersion));
        if ("COUNTDISTINCT".equals(func.trim().toUpperCase())) {
            sb.append("COUNTDISTINCT").append(PARENTHESES_START);
        } else {
            sb.append(func.trim()).append(PARENTHESES_START);
        }
        sb.append(column);
        sb.append(PARENTHESES_END).append(SqlUtils.getKeywordSuffix(jdbcUrl, dbVersion));
        return sb.toString();
    }


    /**
     * 解析指标,使用与clickhouse的部分自定义函数
     */
    public List<UdfFunctionDto> resolveAggerators() {
        // 当前指标
        List<Aggregator> aggregatorList = this.getAggregators();
        if(CollectionUtils.isEmpty(aggregatorList)) {
            return Lists.newArrayList();
        }
        Iterator<Aggregator> iterator = aggregatorList.iterator();
        List<UdfFunctionDto> result = Lists.newArrayList();
        while (iterator.hasNext()) {
            Aggregator aggregator = iterator.next();
            if ("customize".equals(aggregator.getFunc())) {
                List<UdfFunctionDto> udfFunctionDto = getUdfFunctionDto(aggregator);
                if (CollectionUtils.isEmpty(udfFunctionDto)) {
                    continue;
                }
                for (UdfFunctionDto functionDto : udfFunctionDto) {
                    String groupArray = functionDto.getGroupArray();
                    // 自定义方法的groupArray参数要求是维度的元素
                    if (!StringUtils.isEmpty(groupArray)) {
                        if (CollectionUtils.isEmpty(this.getGroups()) || !this.groups.contains(groupArray)) {
                            throw new ServerException("自定义方法" + functionDto.getName() + " groupArray参数异常");
                        }
                    }
                }
                result.addAll(udfFunctionDto);
                iterator.remove();

                if (CollectionUtils.isEmpty(this.finalAggregators)) {
                    this.finalAggregators = Lists.newArrayList(aggregator);
                } else {
                    this.finalAggregators.add(aggregator);
                }
            }
        }
        return result;
    }

    public static List<UdfFunctionDto> getUdfFunctionDto(Aggregator aggregator) {
        String calculateRules = aggregator.getCalculateRules();
        List<UdfFunctionDto> functionDtos = Lists.newArrayList();
        if (com.iflytek.edu.elp.common.util.StringUtils.isEmpty(calculateRules)) {
            return null;
        }
//        StringBuilder bracketsRegex = new StringBuilder("[\\(][^\\)\\}]*[\\)]");
        for (String functionName : Consts.UDF_FUNCTION_NAMES) {
            String pre = functionName + MatchUtils.parenthesesPre;
            List<String> parenthesesMatchStr = MatchUtils.getParenthesesMatchStr(calculateRules, pre);
            if (CollectionUtils.isEmpty(parenthesesMatchStr)) {
                continue;
            }
            for (String p : parenthesesMatchStr) {
                UdfFunctionDto udfFunctionDto = UdfFunctionDto.buildUdfFunctionDto(p, functionName);
                if (null != udfFunctionDto) {
                    functionDtos.add(udfFunctionDto);
                    calculateRules = calculateRules.replace(p, udfFunctionDto.getAlias());
                } else {
                    calculateRules = calculateRules.replace(p, "");
                }
            }
        }
        aggregator.setCalculateRules(calculateRules);
        return functionDtos;
    }

    public static String getField(String field, String jdbcUrl, String dbVersion) {
        String keywordPrefix = SqlUtils.getKeywordPrefix(jdbcUrl, dbVersion);
        String keywordSuffix = SqlUtils.getKeywordSuffix(jdbcUrl, dbVersion);
        if (!StringUtils.isEmpty(keywordPrefix) && !StringUtils.isEmpty(keywordSuffix)) {
            return keywordPrefix + field + keywordSuffix;
        }
        return field;
    }

    /**
     * 对加密参数解密
     *
     * @param executeParam
     */
    public static void decryptParam(ViewExecuteParam executeParam) {
        List<Param> params = executeParam.getParams();
        if (!CollectionUtils.isEmpty(params)) {
            Set<Param> paramSet = Sets.newHashSet();
            params.forEach(p -> {
                String value = p.getValue();
                if (p.getEncry() != null && p.getEncry() == 1) {
                    if (!StringUtils.isEmpty(value)) {
                        String decrypt = AESUtils.decrypt(value.replace("'", ""), null);
                        if (decrypt == null) {
                            throw new NotFoundException("param is null");
                        }
                        p.setValue(decrypt);
                    }
                }
                paramSet.add(p);
            });
            executeParam.setParams(Lists.newArrayList(paramSet));
        }

        List<String> filters = executeParam.getFilters();
        if (!CollectionUtils.isEmpty(filters)) {
            List<String> filterList = Lists.newArrayList();
            filters.forEach(f -> {
                JSONObject jsonObject = JSONObject.parseObject(f);
                String valueStr = jsonObject.get("value").toString();
                Object operator = jsonObject.get("operator");
                Integer encry = (Integer) jsonObject.get("encry");
                if (null != encry && 1 == encry) {
                    if ("in".equals(operator.toString()) || "not in".equals(operator.toString())) {
                        JSONArray valueArray = JSONArray.parseArray(valueStr);
                        JSONArray newValueArray = new JSONArray();
                        valueArray.forEach(v -> {
                            String decrypt = AESUtils.decrypt(v.toString().replace("'", ""), null);
                            if (decrypt != null) {
                                decrypt = "'" + decrypt + "'";
                                newValueArray.add(decrypt);
                            } else {
                                throw new NotFoundException("param is null");
                            }
                        });
                        jsonObject.put("value", newValueArray);
                    } else {
                        String value = "";
                        String decrypt = AESUtils.decrypt(valueStr.replace("'", ""), null);
                        if (decrypt != null) {
                            value = "'" + decrypt + "'";
                        } else {
                            throw new NotFoundException("param is null");
                        }
                        jsonObject.put("value", value);
                    }

                }

                filterList.add(jsonObject.toJSONString());
            });
            executeParam.setFilters(filterList);
        }
    }
}
