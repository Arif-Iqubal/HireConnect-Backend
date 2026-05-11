package com.hireconnect.notification.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("LoggingAspect Tests")
class LoggingAspectTest {

	private LoggingAspect loggingAspect;

	private ProceedingJoinPoint proceedingJoinPoint;

	private Signature signature;

	@BeforeEach
	void setUp() {

		loggingAspect = new LoggingAspect();

		proceedingJoinPoint = mock(ProceedingJoinPoint.class);

		signature = mock(Signature.class);

		when(proceedingJoinPoint.getSignature()).thenReturn(signature);

		when(signature.getDeclaringTypeName()).thenReturn("com.hireconnect.notification.service.NotificationService");

		when(signature.getName()).thenReturn("sendNotification");
	}

	@Test
	@DisplayName("should proceed successfully")
	void shouldProceedSuccessfully() throws Throwable {

		when(proceedingJoinPoint.proceed()).thenReturn("SUCCESS");

		Object result = loggingAspect.logAround(proceedingJoinPoint);

		assertThat(result).isEqualTo("SUCCESS");

		verify(proceedingJoinPoint).proceed();
	}

	@Test
	@DisplayName("should rethrow exception")
	void shouldRethrowException() throws Throwable {

		RuntimeException exception = new RuntimeException("Notification failed");

		when(proceedingJoinPoint.proceed()).thenThrow(exception);

		assertThatThrownBy(() -> loggingAspect.logAround(proceedingJoinPoint)).isInstanceOf(RuntimeException.class)
				.hasMessage("Notification failed");

		verify(proceedingJoinPoint).proceed();
	}
}