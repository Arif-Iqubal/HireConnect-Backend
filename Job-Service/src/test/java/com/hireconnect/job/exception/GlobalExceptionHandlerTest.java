package com.hireconnect.job.exception;

import com.hireconnect.job.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsKnownExceptionsToApiResponses() {
        assertThat(handler.handleNotFound(new ResourceNotFoundException("Job", 9L)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        var denied = handler.handleAccessDenied(new AccessDeniedException("blocked"));
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().getMessage()).isEqualTo("Access denied");

        var illegalState = handler.handleIllegalState(new IllegalStateException("limit reached"));
        assertThat(illegalState.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(illegalState.getBody().getMessage()).isEqualTo("limit reached");
    }

    @Test
    void mapsValidationErrorsByFieldName() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "must not be blank"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Map<String, String>> body = response.getBody();
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getData()).containsEntry("title", "must not be blank");
    }

    @Test
    void mapsGenericExceptionWithoutLeakingMessage() {
        var response = handler.handleGeneric(new RuntimeException("unexpected failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }
}
