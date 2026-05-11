package com.hireconnect.interview.exception;

import com.hireconnect.interview.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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
	@DisplayName("should handle ResourceNotFoundException")
	void shouldHandleResourceNotFound() {

		ResourceNotFoundException ex = new ResourceNotFoundException("Interview not found");

		ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(404);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getMessage()).isEqualTo("Interview not found");
	}

	@Test
	@DisplayName("should handle AccessDeniedException")
	void shouldHandleAccessDenied() {

		AccessDeniedException ex = new AccessDeniedException("Access denied");

		ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(403);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
	}

	@Test
	@DisplayName("should handle IllegalStateException")
	void shouldHandleIllegalState() {

		IllegalStateException ex = new IllegalStateException("Interview already completed");

		ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(400);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getMessage()).isEqualTo("Interview already completed");
	}

	@Test
	@DisplayName("should handle validation exception")
	void shouldHandleValidationException() {

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");

		bindingResult.addError(new FieldError("object", "candidateName", "Candidate name is required"));

		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(400);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getData()).containsEntry("candidateName", "Candidate name is required");
	}

	@Test
	@DisplayName("should handle unreadable request body")
	void shouldHandleUnreadableBody() {

		HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON");

		ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableBody(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(400);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body");
	}

	@Test
	@DisplayName("should handle generic exception")
	void shouldHandleGenericException() {

		Exception ex = new Exception("Unexpected error");

		ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

		assertThat(response.getStatusCode().value()).isEqualTo(500);

		assertThat(response.getBody()).isNotNull();

		assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
	}
}