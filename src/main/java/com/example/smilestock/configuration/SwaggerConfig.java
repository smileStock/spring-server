package com.example.smilestock.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Couple App",
                description = "couple app api명세",
                version = "v1"))
@Configuration
public class SwaggerConfig {
    // http://localhost:8080/swagger-ui/index.html
    @Bean
    public GroupedOpenApi chatOpenApi() {
        String[] paths = {"/","/api/v1/chat-gpt"};

        return GroupedOpenApi.builder()
                .group("COUPLE API v1")
                .pathsToMatch(paths)
                .build();
    }
}
