package com.hireconnect.auth.exception;

import com.hireconnect.auth.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("should handle UserAlreadyExistsException")
    void shouldHandleUserAlreadyExists() {

        UserAlreadyExistsException ex =
                new UserAlreadyExistsException("User already exists");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUserAlreadyExists(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
        .contains("User already exists");
    }

    @Test
    @DisplayName("should handle InvalidCredentialsException")
    void shouldHandleInvalidCredentials() {

        InvalidCredentialsException ex =
                new InvalidCredentialsException("Invalid credentials");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("should handle InvalidTokenException")
    void shouldHandleInvalidToken() {

        InvalidTokenException ex =
                new InvalidTokenException("Invalid token");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidToken(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid token");
    }

    @Test
    @DisplayName("should handle ResourceNotFoundException")
    void shouldHandleResourceNotFound() {

        ResourceNotFoundException ex =
                new ResourceNotFoundException("Resource not found");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFound(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("should handle AccessDeniedException")
    void shouldHandleAccessDenied() {

        AccessDeniedException ex =
                new AccessDeniedException("Access denied");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Access denied");
    }

    @Test
    @DisplayName("should handle BadCredentialsException")
    void shouldHandleBadCredentials() {

        BadCredentialsException ex =
                new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("should handle validation exception")
    void shouldHandleValidationException() {

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "object");

        bindingResult.addError(
                new FieldError(
                        "object",
                        "email",
                        "Email is required"
                )
        );

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().getData())
                .containsEntry("email", "Email is required");
    }

    @Test
    @DisplayName("should handle IllegalStateException")
    void shouldHandleIllegalState() {

        IllegalStateException ex =
                new IllegalStateException("Illegal state");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalState(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);

        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().getMessage())
                .isEqualTo("Illegal state");
    }

    @Test
    @DisplayName("should handle generic exception")
    void shouldHandleGenericException() {

        Exception ex =
                new Exception("Unexpected");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);

        assertThat(response.getBody()).isNotNull();

        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred");
    }
}