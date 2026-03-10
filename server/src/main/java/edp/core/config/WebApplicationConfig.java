/*
 *
 * Copyright (c) 2018. www.zhixue.com All rights Reserved.
 *
 */

package edp.core.config;

import org.springframework.context.annotation.Configuration;

/**
 * webapp的跨域配置
 * 
 * @author kxzhang
 */
@Configuration
public class WebApplicationConfig {

	// @Bean
	// public FilterRegistrationBean corsFilter() {
	//// String allowedOrigins =
	// PropertiesConfigurationFactoryBean.getPropertiesConfiguration().getString("whiteList.url.monitor.auth","http://localhost:9529");
	// String allowedOrigins =
	// "http://test.zhixue.com,http://www.zhixue.com,http://onpre.zhixue.com,http://10.5.10.13:2324";
	// UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	// CorsConfiguration config = new CorsConfiguration();
	// config.setAllowCredentials(true);
	// config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
	// config.addAllowedHeader("*");
	// config.addAllowedMethod("*");
	// source.registerCorsConfiguration("/**", config);
	// FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
	// bean.setOrder(2);
	// return bean;
	// }
}
