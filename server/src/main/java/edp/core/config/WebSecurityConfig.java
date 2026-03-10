/**
 * spring security配置
 */
package edp.core.config;

import edp.davinci.core.common.Constants;
import edp.system.CasAuthenticationEntryPointNew;
import edp.system.service.MyUserDetailService;
import lombok.RequiredArgsConstructor;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * @author zhangkaixuan
 */

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final AccessDeniedHandler accessDeniedHandler;
    private final MyUserDetailService myUserDetailService;
    private final CasProperties casProperties;

    @Override
    @Autowired
    public void configure(AuthenticationManagerBuilder auth)
            throws Exception {
        super.configure(auth);
        // 将验证过程交给自定义的认证提供者
        auth.authenticationProvider(casAuthenticationProvider());

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 不拦截的静态资源
        web.ignoring().antMatchers("/static/**", "/**/*.html", "/**/*.js",
                "/**/*.css", "/**/*.ttf", "/**/*.png", "/**/*.jpg",
                "/**/*.jepg", "/**/*.less", "/**/*.woff", "/**/*.map",
                "/**/*.svg", "/**/*.eot", "/**/*.woff2", "/**/*.gif",
                "/**/*.icon");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String baseUrl = Constants.BASE_API_PATH + "/**";
        String authUrl = Constants.AUTH_API_PATH + "/**";
        // 需要通过cas拦截
        String tokenUrl = Constants.BASE_API_PATH + "/openapi/getUserInfo";
        String shareUrl = "/**/share/**";
        String openUrl = Constants.BASE_API_PATH + "/openapi/**";

        // 问卷题目信息不需要cas拦截/approve/{paperId}/submit
        String getPaperTopicUrl = Constants.BASE_API_PATH + "/collect/getTopics/**";
        String submitAnswerUrl = Constants.BASE_API_PATH + "/collect/submitAnswer/**";

        http.authorizeRequests()// 配置安全策略
                // .antMatchers("/**").permitAll()
//                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers(shareUrl).permitAll()
                .antMatchers((getPaperTopicUrl)).permitAll()
                .antMatchers(submitAnswerUrl).permitAll()
                .antMatchers(tokenUrl).authenticated()
//                .antMatchers(Constants.BASE_API_PATH+"/login").permitAll()
//                .antMatchers(openUrl).permitAll()
                .antMatchers(baseUrl).authenticated()
                .antMatchers(authUrl).authenticated()
                .antMatchers("/**").permitAll()

                .and()
                .logout()
                .permitAll()// 定义logout不需要验证
                .and()
                .csrf().disable()
                .formLogin();// 使用form表单登录

        http.authorizeRequests()
                .anyRequest().hasAuthority("BASE")
                .and()
                .exceptionHandling().accessDeniedPage("/nopower")
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(casAuthenticationEntryPoint())
                .and()
                .addFilter(casAuthenticationFilter())
                .addFilterBefore(casLogoutFilter(), LogoutFilter.class)
                .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class);
    }

    // @Bean
    // public ExceptionTranslationFilter exceptionTranslationFilter() throws Exception {
    //
    // }

    /**
     * CAS认证过滤器
     */
    @Bean
    public CasAuthenticationFilter casAuthenticationFilter() throws Exception {
        CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
        casAuthenticationFilter.setAuthenticationManager(authenticationManager());
        casAuthenticationFilter.setFilterProcessesUrl(casProperties.getDemoLoginUrl());
        return casAuthenticationFilter;
    }

    /**
     * 认证的入口
     */
    @Bean
    public CasAuthenticationEntryPointNew casAuthenticationEntryPoint() {
        CasAuthenticationEntryPointNew casAuthenticationEntryPoint = new CasAuthenticationEntryPointNew();
        casAuthenticationEntryPoint.setLoginUrl(casProperties.getCasServerLoginUrl());
        casAuthenticationEntryPoint.setServiceProperties(serviceProperties());
        return casAuthenticationEntryPoint;
    }

    @Bean
    public AuthenticationProvider casAuthenticationProvider() {
        CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider
                .setAuthenticationUserDetailsService(authenticationUserDetailsService());
        casAuthenticationProvider.setServiceProperties(serviceProperties());
        casAuthenticationProvider.setTicketValidator(cas20ServiceTicketValidator());
        casAuthenticationProvider.setKey("casAuthenticationProviderKey");
        return casAuthenticationProvider;
    }

    @Bean
    public AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService() {
        return new AuthenticationUserDetailsService<CasAssertionAuthenticationToken>() {

            @Override
            public UserDetails loadUserDetails(CasAssertionAuthenticationToken token)
                    throws UsernameNotFoundException {
                String name = token.getAssertion().getPrincipal().getName();
                return myUserDetailService.loadUserByUsername(name);
            }
        };
    }

    /**
     * 指定service相关信息
     */
    @Bean
    public ServiceProperties serviceProperties() {
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setService(casProperties.getDemoServerUrl() + casProperties.getDemoLoginUrl());
        serviceProperties.setAuthenticateAllArtifacts(true);
        return serviceProperties;
    }

    @Bean
    public Cas20ServiceTicketValidator cas20ServiceTicketValidator() {
        return new Cas20ServiceTicketValidator(casProperties.getCasServerUrl());
    }

    /**
     * 单点登出过滤器
     */
    @Bean
    public SingleSignOutFilter singleSignOutFilter() {
        SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
        singleSignOutFilter.setCasServerUrlPrefix(casProperties.getCasServerUrl());
        singleSignOutFilter.setIgnoreInitConfiguration(true);
        return singleSignOutFilter;
    }

    /**
     * 请求单点退出过滤器
     */
    @Bean
    public LogoutFilter casLogoutFilter() {
        LogoutFilter logoutFilter = new LogoutFilter(casProperties.getCasServerLogoutUrl(), new SecurityContextLogoutHandler());
        logoutFilter.setFilterProcessesUrl(casProperties.getDemoLogoutUrl());
        return logoutFilter;
    }
}
