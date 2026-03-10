package edp.davinci.core.inteceptor;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.druid.util.StringUtils;

import edp.core.annotation.AuthIgnore;
import edp.core.enums.HttpCodeEnum;
import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dao.PlatformShareAuthMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/4
 */
@Slf4j
public class PlatformShareAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private PlatformShareAuthMapper platformShareAuthMapper;

    @Autowired
    private TokenUtils tokenUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HandlerMethod handlerMethod = null;

        try {
            handlerMethod = (HandlerMethod) handler;
        } catch (Exception e) {
            response.setStatus(HttpCodeEnum.NOT_FOUND.getCode());
            return false;
        }

        Method method = handlerMethod.getMethod();

        AuthIgnore ignoreAuthMethod = method.getAnnotation(AuthIgnore.class);
        if (handler instanceof HandlerMethod && null != ignoreAuthMethod) {
            return true;
        }

        String token = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX)) {
            response.setStatus(HttpCodeEnum.FORBIDDEN.getCode());
            response.getWriter().print("The resource requires authentication, which was not supplied with the request");
            return false;
        }

        if (tokenUtils.isExpiredForPlatformShareToken(token)) {
            ResultMap result = new ResultMap().fail(1001).payload("token timeout");
            response.getWriter().print(JSONUtils.toJSONString(result));
            return false;
        }

        String clientId = tokenUtils.getPassword(token);
        if (StringUtils.isEmpty(clientId) || null == platformShareAuthMapper.selectByClientId(clientId)) {
            response.setStatus(HttpCodeEnum.FORBIDDEN.getCode());
            response.getWriter().print("The resource requires authentication, which was not supplied with the request");
            return false;
        }
        return true;
    }
}
