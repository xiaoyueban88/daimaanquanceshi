package edp.system;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.druid.support.json.JSONUtils;

import edp.davinci.core.common.ResultMap;
import org.jasig.cas.client.util.CommonUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class CasAuthenticationEntryPointNew implements AuthenticationEntryPoint, InitializingBean {

    private ServiceProperties serviceProperties;
    private String loginUrl;
    private boolean encodeServiceUrlWithSessionId = true;

    public CasAuthenticationEntryPointNew() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasLength(this.loginUrl, "loginUrl must be specified");
        Assert.notNull(this.serviceProperties, "serviceProperties must be specified");
        Assert.notNull(this.serviceProperties.getService(), "serviceProperties.getService() cannot be null.");
    }

    @Override
    public final void commence(HttpServletRequest servletRequest, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String nextPage = request.getParameter("nextPage");
        if (!StringUtils.isEmpty(nextPage)) {
            HttpSession session = request.getSession();
            session.setAttribute("nextPage", nextPage);
        }
        String urlEncodedService = this.createServiceUrl(servletRequest, response);
        String redirectUrl = this.createRedirectUrl(urlEncodedService);
        this.preCommence(servletRequest, response);
        response.setStatus(200);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Headers", "Origin,X-Requested-With,Content-Type,Accept,Authorization");
        response.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE,PUT");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "1800");
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
//        String result = String.format("{\"code\":%s,\"msg\":\"%s\"}", 302, redirectUrl);
        ResultMap result = new ResultMap().fail(302).payload(redirectUrl);
        PrintWriter out = null;
        out = response.getWriter();
        out.append(JSONUtils.toJSONString(result));
        out.flush();
        out.close();
//        response.sendRedirect(redirectUrl);
    }

    protected String createServiceUrl(HttpServletRequest request, HttpServletResponse response) {
        return CommonUtils.constructServiceUrl((HttpServletRequest) null, response, this.serviceProperties.getService(), (String) null, this.serviceProperties.getArtifactParameter(), this.encodeServiceUrlWithSessionId);
    }

    protected String createRedirectUrl(String serviceUrl) {
        return CommonUtils.constructRedirectUrl(this.loginUrl, this.serviceProperties.getServiceParameter(), serviceUrl, this.serviceProperties.isSendRenew(), false);
    }

    protected void preCommence(HttpServletRequest request, HttpServletResponse response) {
    }

    public final String getLoginUrl() {
        return this.loginUrl;
    }

    public final ServiceProperties getServiceProperties() {
        return this.serviceProperties;
    }

    public final void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public final void setServiceProperties(ServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    public final void setEncodeServiceUrlWithSessionId(boolean encodeServiceUrlWithSessionId) {
        this.encodeServiceUrlWithSessionId = encodeServiceUrlWithSessionId;
    }

    protected boolean getEncodeServiceUrlWithSessionId() {
        return this.encodeServiceUrlWithSessionId;
    }
}
