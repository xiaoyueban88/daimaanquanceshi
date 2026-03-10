package edp.system;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/***
 * 处理session超时
 * @author zhangkaixuan
 *
 */
public class MongoLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
	
	private Logger logger = LoggerFactory.getLogger(MyAccessDeniedHandlerImpl.class);
	
	public MongoLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		logger.info("session超时...");
		// 处理ajax请求登录超时
		boolean isAjax = "XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"));
		if (isAjax) {
			String jsonObject = "{\"message\":\"会话超时.\"," + "\"access-denied\":true,\"cause\":\"timeout\"}";
			String contentType = "application/json";
			response.setContentType(contentType);
			PrintWriter out = response.getWriter();
			out.print(jsonObject);
			out.flush();
			out.close();
			return;
		} else {
			String path = request.getContextPath();
			String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path + "/";
			response.sendRedirect(basePath + "login");
		}
	}
}