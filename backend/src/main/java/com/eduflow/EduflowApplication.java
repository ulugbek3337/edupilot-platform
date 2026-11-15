package com.eduflow;

import com.eduflow.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class EduflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduflowApplication.class, args);
    }
}
