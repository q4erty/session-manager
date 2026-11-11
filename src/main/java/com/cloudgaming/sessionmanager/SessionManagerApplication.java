package com.cloudgaming.sessionmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.cloudgaming.sessionmanager.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SessionManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionManagerApplication.class, args);
    }

}
