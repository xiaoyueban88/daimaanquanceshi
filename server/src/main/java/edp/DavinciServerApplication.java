package edp;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
@EnableScheduling
@EnableCaching
@Slf4j
@ComponentScan(value = {"edp.davinci.service","edp.davinci.core.utils",
        "edp.core","edp.core.common","edp.factory.widgetpower"})
@MapperScan("edp.davinci.dao")
public class DavinciServerApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(DavinciServerApplication.class, args);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

}

