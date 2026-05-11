package com.hireconnect.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigTest {

    @Test
    void corsWebFilterShouldApplyFrontendCorsPolicy() {
        var filter = new GatewayConfig().corsWebFilter();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.OPTIONS, "http://localhost:8080/api/v1/jobs")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .build());

        filter.filter(exchange, chainExchange -> {
            throw new AssertionError("Preflight requests should be handled by CORS filter");
        }).block();

        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:4200");
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowMethods())
                .contains(HttpMethod.PUT, HttpMethod.OPTIONS);
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowCredentials()).isTrue();
    }
}
