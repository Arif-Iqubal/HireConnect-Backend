package com.auth.security;


import com.auth.pojo.UserCredential;
import com.auth.repository.AuthRepository;
import com.auth.utils.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthRepository repository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String email = user.getAttribute("email");

        // 🔴 GitHub sometimes returns null email
        if (email == null) {
            email = user.getAttribute("login") + "@github.com";
        }

        // ✅ Check if user exists
        UserCredential existingUser = repository.findByEmail(email).orElse(null);

        if (existingUser == null) {
            existingUser = UserCredential.builder()
                    .email(email)
                    .passwordHash("OAUTH_USER")
                    .role("CANDIDATE")
                    .provider("GITHUB")
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(existingUser);
        }

        // 🔑 Generate JWT
        String token = jwtUtil.generateToken(email);

        // 🔁 Redirect to frontend with token
        String redirectUrl = "http://localhost:4200/oauth-success?token=" + token;

        response.sendRedirect(redirectUrl);
    }
}