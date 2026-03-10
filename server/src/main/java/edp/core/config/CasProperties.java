package edp.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @description
 * @author: clyang11
 * @date 2024/8/1
 */
@Component
public class CasProperties {
    @Value("${cas.server.host.url}")
    private String casServerUrl;

    @Value("${cas.server.host.login_url}")
    private String casServerLoginUrl;

    @Value("${cas.server.host.logout_url}")
    private String casServerLogoutUrl;

    @Value("${demo.server.host.url}")
    private String demoServerUrl;

    @Value("${demo.login.url}")
    private String demoLoginUrl;

    @Value("${demo.logout.url}")
    private String demoLogoutUrl;

    public String getCasServerUrl() {
        return casServerUrl;
    }

    public void setCasServerUrl(String casServerUrl) {
        this.casServerUrl = casServerUrl;
    }

    public String getCasServerLoginUrl() {
        return casServerLoginUrl;
    }

    public void setCasServerLoginUrl(String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }

    public String getCasServerLogoutUrl() {
        return casServerLogoutUrl;
    }

    public void setCasServerLogoutUrl(String casServerLogoutUrl) {
        this.casServerLogoutUrl = casServerLogoutUrl;
    }

    public String getDemoServerUrl() {
        return demoServerUrl;
    }

    public void setDemoServerUrl(String demoServerUrl) {
        this.demoServerUrl = demoServerUrl;
    }

    public String getDemoLoginUrl() {
        return demoLoginUrl;
    }

    public void setDemoLoginUrl(String demoLoginUrl) {
        this.demoLoginUrl = demoLoginUrl;
    }

    public String getDemoLogoutUrl() {
        return demoLogoutUrl;
    }

    public void setDemoLogoutUrl(String demoLogoutUrl) {
        this.demoLogoutUrl = demoLogoutUrl;
    }

}
