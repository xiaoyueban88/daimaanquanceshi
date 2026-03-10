package edp.davinci.core.inteceptor;

import java.net.MalformedURLException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @Description 校验请求源Referer是否安全
 * @author zswu3
 * @date 2021/2/6
 */
public class RefererInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String referer = Optional.ofNullable(request.getHeader("referer"))
                .orElse(request.getHeader("referrer"));
        String host = request.getServerName();
        // referer 为空则返回400
        if(null == referer) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        java.net.URL url = null;
        try {
            url = new java.net.URL(referer);
        } catch (MalformedURLException e) {
            // URL解析异常，也置为404
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        if (!host.equals(url.getHost())) {
            // @TODO 校验白名单
        }

        return super.preHandle(request, response, handler);
    }
}
