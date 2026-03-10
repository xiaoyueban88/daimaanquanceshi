package edp.davinci.model;

import java.util.Date;

import lombok.Data;

/**
 * 数据收集问卷
 *
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */
@Data
public class CollectPaper {

    private Long id;

    private String uuid;

    /**
     * 所在项目id
     */
    private Long projectId;

    /**
     * 问卷标题
     */
    private String title;

    /**
     * 问卷类型 0-匿名 1-署名登录的
     */
    private Short type;

    /**
     * 问卷当前状态 0-结束征集 1-征集中
     */
    private Short status;

    /**
     * 创建人域账号
     */
    private String createBy;

    /**
     * 截至日期
     */
    private Date deadline;

    /**
     * 数据失效时间 单位:天
     */
    private Integer expires;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 审批人
     */
    private String approver;

    /**
     * token
     */
    private String token;

    private Short isDelete = 0;
}
