/*
 *
 * Copyright (c) 2018. www.zhixue.com All rights Reserved.
 *
 */

package edp.davinci.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * 从xml中实例化bean
 * 
 * @author
 *
 */
@Configuration
@ImportResource(locations = {"classpath:config/applicationContext_service.xml", "classpath:config/applicationContext_dubboConsumer.xml"})
public class XmlBeanConfig {

}
