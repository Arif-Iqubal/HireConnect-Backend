package com.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.auth.security.OAuthSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	 private final OAuthSuccessHandler successHandler;
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable()).cors(cors -> {})
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/v3/api-docs/**","/oauth2/**",  "/swagger-ui/**", "/swagger-ui.html")
								.permitAll().requestMatchers("/auth/**").permitAll().anyRequest().authenticated()).oauth2Login(oauth -> oauth
						                .successHandler(successHandler)
							            ); // keep simple

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}