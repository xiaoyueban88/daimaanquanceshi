package edp.davinci.common.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/6/14
 */
@Data
@Builder
public class GenerateTokenParam {
    // Long shareEntityId, String clientId, Long userId
    /**
     * 分享实体id
     */
    private String shareEntityId;

    /**
     * 第三方id
     */
    private String clientId;

    /**
     * 分享人id
     */
    private Long userId;

    /**
     * 分享额外信息，决策平台对应的为reportId(Integer)
     */
    private String extraInfo;
}
