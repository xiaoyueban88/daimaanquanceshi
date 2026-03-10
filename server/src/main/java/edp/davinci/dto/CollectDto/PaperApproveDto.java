package edp.davinci.dto.CollectDto;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/13
 */
@Data
public class PaperApproveDto {
    /**
     * 问卷id
     */
    Long paperId;

    /**
     * 问卷名称
     */
    String title;

    /**
     * 未审批数量
     */
    Integer unApproveCount;

    /**
     * 已审批数量
     */
    Integer approvedCount;
}
