package edp.davinci.core.model;

import java.util.List;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/8/10
 */
@Data
public class SqlHavingResource {
    /**
     * 计算规则，仅当func = 'customize'时有效
     */
    private String calculateRules;

    /**
     * 字段名/别名
     */
    private String column;

    /**
     * 函数
     */
    private String func;

    /**
     * 配置
     */
    private List<SqlHaving> config;
}
