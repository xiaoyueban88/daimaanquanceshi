package edp.davinci.model;

import java.util.Date;

import lombok.Data;

/**
 * @author zswu3
 * @Description 第三方平台接入分享页面权限
 * @date 2020/2/4
 */
@Data
public class PlatformShareAuth {

    private Long id;

    private String clientId;

    private String clientSecret;

    private String description;

    private Date createTime;

    private String publicKey;

    private Long createBy;

    private String widgetSkipConfigUrl;
}
