package com.hireconnect.auth.config.oauth;

import com.hireconnect.auth.config.security.JwtService;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.repository.UserCredentialRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

@DisplayName("OAuth2SuccessHandler Tests")
class OAuth2SuccessHandlerTest {

    private UserCredentialRepository userRepository;

    private JwtService jwtService;

    private OAuth2SuccessHandler handler;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private Authentication authentication;

    private OAuth2User oauth2User;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserCredentialRepository.class);

        jwtService = mock(JwtService.class);

        handler = new OAuth2SuccessHandler(
                userRepository,
                jwtService
        );

        request = mock(HttpServletRequest.class);

        response = mock(HttpServletResponse.class);

        authentication = mock(Authentication.class);

        oauth2User = mock(OAuth2User.class);

        when(authentication.getPrincipal())
                .thenReturn(oauth2User);
    }

    @Test
    @DisplayName("should login existing OAuth user")
    void shouldLoginExistingUser() throws Exception {

        UserCredential user = UserCredential.builder()
                .userId(1L)
                .email("alice@example.com")
                .fullName("Alice")
                .provider(AuthProvider.GITHUB)
                .role(UserRole.CANDIDATE)
                .build();

        when(oauth2User.getAttribute("email"))
                .thenReturn("alice@example.com");

        when(oauth2User.getAttribute("name"))
                .thenReturn("Alice");

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token");

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(response).sendRedirect(
                contains("accessToken=access-token")
        );

        verify(userRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("should create new OAuth user")
    void shouldCreateNewOAuthUser() throws Exception {

        UserCredential savedUser = UserCredential.builder()
                .userId(2L)
                .email("new@example.com")
                .fullName("New User")
                .provider(AuthProvider.GITHUB)
                .role(UserRole.CANDIDATE)
                .build();

        when(oauth2User.getAttribute("email"))
                .thenReturn("new@example.com");

        when(oauth2User.getAttribute("name"))
                .thenReturn("New User");

        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(UserCredential.class)))
                .thenReturn(savedUser);

        when(jwtService.generateAccessToken(savedUser))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(savedUser))
                .thenReturn("refresh-token");

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(userRepository)
                .save(any(UserCredential.class));

        verify(response).sendRedirect(
                contains("refreshToken=refresh-token")
        );
    }

    @Test
    @DisplayName("should fallback to github login email")
    void shouldFallbackToGithubEmail() throws Exception {

        UserCredential user = UserCredential.builder()
                .userId(3L)
                .email("githubuser@github.com")
                .fullName("GitHub User")
                .provider(AuthProvider.GITHUB)
                .role(UserRole.CANDIDATE)
                .build();

        when(oauth2User.getAttribute("email"))
                .thenReturn(null);

        when(oauth2User.getAttribute("login"))
                .thenReturn("githubuser");

        when(oauth2User.getAttribute("name"))
                .thenReturn(null);

        when(userRepository.findByEmail("githubuser@github.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token");

        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(response).sendRedirect(
                contains("githubuser@github.com")
        );
    }

    @Test
    @DisplayName("should propagate redirect exception")
    void shouldThrowIOException() throws Exception {

        UserCredential user = UserCredential.builder()
                .userId(1L)
                .email("alice@example.com")
                .fullName("Alice")
                .provider(AuthProvider.GITHUB)
                .role(UserRole.CANDIDATE)
                .build();

        when(oauth2User.getAttribute("email"))
                .thenReturn("alice@example.com");

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user))
                .thenReturn("access");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh");

        doThrow(new IOException("Redirect failed"))
                .when(response)
                .sendRedirect(anyString());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                handler.onAuthenticationSuccess(
                        request,
                        response,
                        authentication
                )
        ).isInstanceOf(IOException.class);
    }
}