package edp.davinci.service;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/12
 */
public interface PlatformShareRpcService {
    /**
     * 第三方获取鉴权token
     *
     * @param clientSecret
     * @param shareToken
     * @return
     */
    String getAuthToken(String clientSecret, String shareToken);
}
