package edp.davinci.model;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/28
 */
@Data
public class CustomizeMetricsFav {
    private Long id;


    private Long viewId;

    /**
     * 类型
     */
    private String visualType;

    /**
     * 计算规则
     */
    private String calculationRules;

    /**
     * 别名
     */
    private String alias;

    /**
     * 描述信息
     */
    private String desc;

    /**
     * 创建者域账号
     */
    private String creator;

    /**
     * 创建时间
     */
    private String createTime;

}
