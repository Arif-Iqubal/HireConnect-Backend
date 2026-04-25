package com.job.config;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // ✅ ADD THIS (IMPORTANT)
                .servers(List.of(
                        new Server().url("/job")
                ))

                // ✅ KEEP YOUR EXISTING INFO
                .info(new Info()
                        .title("HireConnect Job Service API")
                        .version("1.0")
                        .description("The Job-Service handles the full lifecycle of job postings — creation, retrieval, search, filtering, update, and deletion.")
                );
    }
}