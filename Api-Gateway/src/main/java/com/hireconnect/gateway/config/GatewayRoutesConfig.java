package com.hireconnect.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                // ================= AUTH SERVICE =================
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://AUTH-SERVICE")
                )

                // Swagger Docs
                .route("auth-api-docs", r -> r
                        .path("/auth/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/auth/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                        .uri("lb://AUTH-SERVICE")
                )

                // Swagger UI
                .route("auth-swagger", r -> r
                        .path("/auth/swagger-ui/**")
                        .filters(f -> f.rewritePath("/auth/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://AUTH-SERVICE")
                )

                // ================= PROFILE SERVICE =================
                .route("profile-service", r -> r
                        .path("/profile/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PROFILE-SERVICE")
                )

                .route("profile-api-docs", r -> r
                        .path("/profile/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/profile/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                        .uri("lb://PROFILE-SERVICE")
                )

                .route("profile-swagger", r -> r
                        .path("/profile/swagger-ui/**")
                        .filters(f -> f.rewritePath("/profile/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://PROFILE-SERVICE")
                )

                // ================= JOB SERVICE =================
                .route("job-service", r -> r
                        .path("/job/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://JOB-SERVICE")
                )

                .route("job-api-docs", r -> r
                        .path("/job/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/job/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                        .uri("lb://JOB-SERVICE")
                )

                .route("job-swagger", r -> r
                        .path("/job/swagger-ui/**")
                        .filters(f -> f.rewritePath("/job/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://JOB-SERVICE")
                )
                
                // ================= APPLICATION SERVICE =================
                .route("application-service", r -> r
                        .path("/application/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://APPLICATION-SERVICE")
                )

                .route("application-api-docs", r -> r
                	    .path("/application/v3/api-docs", "/application/v3/api-docs/**")
                	    .filters(f -> f.rewritePath("/application/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                	    .uri("lb://APPLICATION-SERVICE")
                	)

                .route("application-swagger", r -> r
                        .path("/application/swagger-ui/**")
                        .filters(f -> f.rewritePath("/application/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://APPLICATION-SERVICE")
                )
                
                // ================= INTERVIEW SERVICE =================
                .route("interview-service", r -> r
                        .path("/interview/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://INTERVIEW-SERVICE")
                )

                .route("interview-api-docs", r -> r
                	    .path("/interview/v3/api-docs", "/interview/v3/api-docs/**")
                	    .filters(f -> f.rewritePath("/interview/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                	    .uri("lb://INTERVIEW-SERVICE")
                	)

                .route("interview-swagger", r -> r
                        .path("/interveiw/swagger-ui/**")
                        .filters(f -> f.rewritePath("/interview/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://INTERVIEW-SERVICE")
                )
                
             // ================= NOTIFICATION SERVICE =================
                .route("notification-service", r -> r
                        .path("/notification/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://NOTIFICATION-SERVICE")
                )

                .route("notification-api-docs", r -> r
                	    .path("/notification/v3/api-docs", "/notification/v3/api-docs/**")
                	    .filters(f -> f.rewritePath("/notification/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                	    .uri("lb://NOTIFICATION-SERVICE")
                	)

                .route("notification-swagger", r -> r
                        .path("/notification/swagger-ui/**")
                        .filters(f -> f.rewritePath("/notification/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://NOTIFICATION-SERVICE")
                )

             // ================= SUBSCRIPTION SERVICE =================
                .route("subscription-service", r -> r
                        .path("/subscription/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SUBSCRIPTION-SERVICE")
                )

                .route("subscription-api-docs", r -> r
                	    .path("/subscription/v3/api-docs", "/subscription/v3/api-docs/**")
                	    .filters(f -> f.rewritePath("/subscription/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                	    .uri("lb://SUBSCRIPTION-SERVICE")
                	)

                .route("subscription-swagger", r -> r
                        .path("/subscription/swagger-ui/**")
                        .filters(f -> f.rewritePath("/subscription/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://SUBSCRIPTION-SERVICE")
                )
                
             // ================= ANALYTICS SERVICE =================
                .route("analytics-service", r -> r
                        .path("/analytics/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ANALYTICS-SERVICE")
                )

                .route("analytics-api-docs", r -> r
                	    .path("/analytics/v3/api-docs", "/analytics/v3/api-docs/**")
                	    .filters(f -> f.rewritePath("/analytics/v3/api-docs(?<segment>/?.*)", "/v3/api-docs${segment}"))
                	    .uri("lb://ANALYTICS-SERVICE")
                	)

                .route("analytics-swagger", r -> r
                        .path("/analytics/swagger-ui/**")
                        .filters(f -> f.rewritePath("/analytics/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://ANALYTICS-SERVICE")
                )
                .build();
    }
}