package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.request.*;
import com.hireconnect.auth.dto.response.*;
import com.hireconnect.auth.enums.AuthProvider;
import com.hireconnect.auth.enums.UserRole;
import com.hireconnect.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController controller;

    private final AuthResponse authResponse = AuthResponse.builder()
            .userId(1L)
            .email("user@example.com")
            .role(UserRole.CANDIDATE)
            .provider(AuthProvider.LOCAL)
            .accessToken("access")
            .refreshToken("refresh")
            .tokenType("Bearer")
            .build();

    private final UserInfoResponse userInfo = UserInfoResponse.builder()
            .userId(1L)
            .email("user@example.com")
            .role(UserRole.CANDIDATE)
            .provider(AuthProvider.LOCAL)
            .isActive(true)
            .build();

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).contains("Registration successful");
        assertThat(response.getBody().getData()).isEqualTo(authResponse);
    }

    @Test
    void authEndpointsDelegateAndWrapResponses() {
        LoginRequest login = new LoginRequest();
        ForgotPasswordRequest forgot = new ForgotPasswordRequest();
        ResetPasswordRequest reset = new ResetPasswordRequest();
        RefreshTokenRequest refresh = new RefreshTokenRequest();
        ChangePasswordRequest change = new ChangePasswordRequest();
        ForgotPasswordResponse forgotResponse = ForgotPasswordResponse.builder().resetToken("token").build();
        when(authService.login(login)).thenReturn(authResponse);
        when(authService.forgotPassword(forgot)).thenReturn(forgotResponse);
        when(authService.refreshToken(refresh)).thenReturn(authResponse);
        when(authService.validateToken("jwt")).thenReturn(true);

        assertThat(controller.login(login).getBody().getData()).isEqualTo(authResponse);
        assertThat(controller.forgotPassword(forgot).getBody().getData()).isEqualTo(forgotResponse);
        assertThat(controller.refreshToken(refresh).getBody().getData()).isEqualTo(authResponse);
        assertThat(controller.validateToken("jwt").getBody().getData()).isTrue();

        controller.resetPassword(reset);
        controller.logout(1L);
        controller.changePassword(1L, change);

        verify(authService).resetPassword(reset);
        verify(authService).logout(1L);
        verify(authService).changePassword(1L, change);
    }

    @Test
    void userAdminEndpointsDelegateToService() {
        Page<UserInfoResponse> page = new PageImpl<>(List.of(userInfo));
        when(authService.getUserInfo(1L)).thenReturn(userInfo);
        when(authService.getUsers(any(), eq("CANDIDATE"), eq(true), eq("user"))).thenReturn(page);
        when(authService.updateUserActiveStatus(1L, false)).thenReturn(userInfo);
        when(authService.getActiveAdminIds()).thenReturn(List.of(99L));

        assertThat(controller.getMe(1L).getBody().getData()).isEqualTo(userInfo);
        assertThat(controller.getUserById(1L).getBody().getData()).isEqualTo(userInfo);
        assertThat(controller.getUsers(2, 25, "email", "asc", "CANDIDATE", true, "user").getBody().getData().getContent()).containsExactly(userInfo);
        assertThat(controller.updateUserActiveStatus(1L, false).getBody().getMessage()).isEqualTo("User suspended successfully");
        assertThat(controller.getActiveAdminIds().getBody().getData()).containsExactly(99L);

        controller.deleteUser(1L);
        verify(authService).deleteUser(1L);
    }
}
