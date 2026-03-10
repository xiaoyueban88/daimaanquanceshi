package edp.davinci.model;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */
@Data
public class CollectPaperApprover {

    private Long id;

    /**
     * 问卷id
     */
    private Long paperId;

    /**
     * 审批人域账号
     */
    private String user;

    /**
     * 是否删除
     */
    private Short isDelete = 0;

}
