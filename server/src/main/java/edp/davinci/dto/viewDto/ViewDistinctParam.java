package edp.davinci.dto.viewDto;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/12/23
 */
@Data
public class ViewDistinctParam extends DistinctParam {
    /**
     * view id
     */
    @NotNull(message = "distinct column cannot be NULL")
    private Long viewId;
}
