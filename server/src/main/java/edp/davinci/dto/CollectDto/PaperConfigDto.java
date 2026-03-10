package edp.davinci.dto.CollectDto;

import java.util.Date;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/10
 */
@Data
public class PaperConfigDto {
    /**
     * 问卷id
     */
    private Long paperId;

    /**
     * 问卷审批人
     */
    private String approver;

    /**
     * 截止日期
     */
    private Date deadline;

    /**
     * 数据失效时间
     */
    private Integer expires;

    /**
     * 问卷状态
     */
    private Integer status;

    /**
     * 问卷类型
     */
    private Integer type;

    /**
     * 更新时间
     */
    private Date updateTime;
}
