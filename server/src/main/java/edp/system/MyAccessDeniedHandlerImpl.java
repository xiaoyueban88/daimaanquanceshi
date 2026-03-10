/**
 * 实现spring security的访问拒绝后处理逻辑
 */
package edp.system;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component("accessDeniedHandler")
public class MyAccessDeniedHandlerImpl implements AccessDeniedHandler {
	
	private Logger logger = LoggerFactory.getLogger(MyAccessDeniedHandlerImpl.class);
	
	public MyAccessDeniedHandlerImpl() {
		
	}

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse resp, AccessDeniedException reason)
			throws ServletException, IOException {
		boolean isAjax = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
		logger.info("无权限访问处理器...");
		// 如果是ajax请求
		if (isAjax) {
			String jsonObject = "{\"message\":\"您没有权限访问.\"," + "\"access-denied\":true,\"cause\":\"noright\"}";
			String contentType = "application/json";
			resp.setContentType(contentType);
			PrintWriter out = resp.getWriter();
			out.print(jsonObject);
			out.flush();
			out.close();
			return;
		} else {
			//页面
			String path = req.getContextPath();
			String basePath = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + path + "/";
			resp.sendRedirect(basePath + "nopower");
		}

	}
}
