package com.eduflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI edupilotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduPilot API")
                        .description("Multi-tenant LMS platform API")
                        .version("v1.0.0"));
    }
}
