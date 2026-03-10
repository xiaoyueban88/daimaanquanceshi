package edp.davinci.model;

import java.util.Date;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */
@Data
public class CollectPaperSubmit {
    private Long id;

    /**
     * 问卷id
     */
    private Long paperId;

    /**
     * 提交人
     */
    private String user;

    /**
     * 审批状态 0-未审批 1-已审批
     */
    private Short status = 0;

    /**
     * 提交时间
     */
    private Date createTime;

    /**
     * 是否失效
     */
    private Short isDelete = 0;


    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 审批人
     */
    private String approver;
}
