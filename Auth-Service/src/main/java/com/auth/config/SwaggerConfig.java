package com.auth.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				 // ✅ ADD THIS (IMPORTANT)
                .servers(List.of(
                        new Server().url("/auth")
                ))
				.info(new Info().title("HireConnect Auth Service API") 
				.version("1.0").description("Authentication & Authorization APIs"));
	}
}