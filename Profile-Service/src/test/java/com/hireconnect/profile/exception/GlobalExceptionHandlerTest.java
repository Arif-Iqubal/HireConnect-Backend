package com.hireconnect.profile.exception;

import com.hireconnect.profile.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsKnownExceptionsToExpectedStatusCodes() {
        ResponseEntity<ApiResponse<Void>> notFound = handler.handleNotFound(new ResourceNotFoundException("missing"));
        ResponseEntity<ApiResponse<Void>> duplicate = handler.handleDuplicate(new ProfileAlreadyExistsException(1L));
        ResponseEntity<ApiResponse<Void>> denied = handler.handleAccessDenied(new AccessDeniedException("denied"));
        ResponseEntity<ApiResponse<Void>> illegal = handler.handleIllegalArg(new IllegalArgumentException("bad input"));

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().getMessage()).isEqualTo("missing");
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(illegal.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(illegal.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    void mapsUnexpectedExceptionsToGenericServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
