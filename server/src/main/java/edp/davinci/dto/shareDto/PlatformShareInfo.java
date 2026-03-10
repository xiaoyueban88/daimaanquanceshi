package edp.davinci.dto.shareDto;

import edp.davinci.model.User;
import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/10
 */
public class PlatformShareInfo extends ShareInfo {

    /**
     * 被分享的第三方clientId
     */
    private String clientId;

    /**
     * 被分享的第三方id
     */
    private Long platformShareAuthId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Long getPlatformShareAuthId() {
        return platformShareAuthId;
    }

    public void setPlatformShareAuthId(Long platformShareAuthId) {
        this.platformShareAuthId = platformShareAuthId;
    }
}
