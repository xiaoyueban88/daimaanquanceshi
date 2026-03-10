/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.core.inteceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;

import edp.core.annotation.AuthIgnore;
import edp.core.annotation.AuthShare;
import edp.core.annotation.PaperShare;
import edp.core.enums.HttpCodeEnum;
import edp.core.exception.ServerException;
import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dao.CollectPaperMapper;
import edp.davinci.dao.PlatformShareAuthMapper;
import edp.davinci.model.CollectPaper;
import edp.davinci.model.PlatformShareAuth;
import edp.davinci.model.User;
import edp.davinci.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private CollectPaperMapper collectPaperMapper;

    @Autowired
    private PlatformShareAuthMapper platformShareAuthMapper;

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
        //注解不需要验证token
        if (handler instanceof HandlerMethod && null != ignoreAuthMethod) {
            return true;
        }


        // 问卷分享校验
        PaperShare paperShareMethod = method.getAnnotation(PaperShare.class);
        if (null != paperShareMethod) {
            StringBuffer requestURL = request.getRequestURL();
            String[] split = requestURL.toString().split("/");
            String paperToken = split[split.length - 1];
            if (StringUtils.isEmpty(paperToken)) {
                throw new ServerException("参数异常");
            }
            String paperIdStr = tokenUtils.getUsername(paperToken);
            if (StringUtils.isEmpty(paperIdStr)) {
                throw new ServerException("地址过期");
            }
            CollectPaper paper = collectPaperMapper.getById(Long.parseLong(paperIdStr));

            if (paper == null) {
                throw new ServerException("问卷不存在");
            }
            if (0 == paper.getType()) {
                return true;
            }
            JSONObject object = JSONObject.parseObject(tokenUtils.getPassword(paperToken));
            Short type = Short.parseShort(object.get("type").toString());
            if (type < 3 && !Objects.equals(type, paper.getType())) {
                throw new ServerException("地址过期");
            }

            // 如果是第三方平台继续校验
            if (2 == type) {
                String clientId = object.get("clientId").toString();
                PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
                if (platformShareAuth != null) {
                    return true;
                }
            }
        }

        String token = request.getHeader(Constants.TOKEN_HEADER_STRING);

        AuthShare authShareMethoed = method.getAnnotation(AuthShare.class);
        if (null != authShareMethoed) {
            if (!StringUtils.isEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
                String username = tokenUtils.getUsername(token);
                User user = userService.getByUsername(username);
                request.setAttribute(Constants.CURRENT_USER, user);
            }
            return true;
        }

        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX)) {
            if (!request.getServletPath().endsWith("/download/page")) {
                log.info("{} : Unknown token", request.getServletPath());
            }
            response.setStatus(HttpCodeEnum.FORBIDDEN.getCode());
            response.getWriter().print("The resource requires authentication, which was not supplied with the request");
            return false;
        }
        String username = tokenUtils.getUsername(token);
        User user = userService.getByUsername(username);
        if (null == user) {
            log.info("{} : token user not found", request.getServletPath());
            response.setStatus(HttpCodeEnum.FORBIDDEN.getCode());
            response.getWriter().print("ERROR Permission denied");
            return false;

        }
        if (!tokenUtils.validateToken(token, user)) {
            log.info("{} : token validation fails", request.getServletPath());
            response.setStatus(HttpCodeEnum.FORBIDDEN.getCode());
            response.getWriter().print("Invalid token ");
            return false;
        }

        if (!request.getServletPath().contains("/user/active") && !user.getActive()) {
            if (request.getServletPath().contains("/user/sendmail")) {
                request.setAttribute(Constants.CURRENT_USER, user);
                return true;
            }
            log.info("current user is not activated, username: {}", user.getUsername());
            response.setStatus(HttpCodeEnum.FAIL.getCode());
            ResultMap resultMap = new ResultMap(tokenUtils);
            response.getWriter().print(JSONObject.toJSONString(resultMap.failAndRefreshToken(request).message("Account not active yet. Please check your email to activate your account")));
            return false;
        }
        request.setAttribute(Constants.CURRENT_USER, user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

    private void loginErrHandle(HttpServletRequest request, HttpServletResponse response) {
        String redirectUrl = "http://192.168.57.238:8024/cas/login?service=http%3A%2F%2Flocalhost%3A8080%2Fdavinci%2Flogin";
        response.setStatus(200);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Headers", "Origin,X-Requested-With,Content-Type,Accept,Authorization");
        response.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE,PUT");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "1800");
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        ResultMap result = new ResultMap().fail(302).payload(redirectUrl);
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
        }
        out.append(JSONUtils.toJSONString(result));
        out.flush();
        out.close();
    }
}
