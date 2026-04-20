package com.hireconnect.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.hireconnect.gateway.util.JwtUtil;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

	private final JwtUtil jwtUtil;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getURI().getPath();

		// ✅ PUBLIC ENDPOINTS (VERY IMPORTANT)
		if (path.startsWith("/auth/login") || path.startsWith("/auth/register") || path.contains("/swagger-ui")
				|| path.contains("/v3/api-docs") || path.contains("/oauth2") || path.contains("/login/oauth2")) {

			return chain.filter(exchange);
		}

		// ✅ CHECK AUTH HEADER
		String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		String token = authHeader.substring(7);

		// ✅ VALIDATE TOKEN
		if (!jwtUtil.isValid(token)) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		return chain.filter(exchange);
	}
}