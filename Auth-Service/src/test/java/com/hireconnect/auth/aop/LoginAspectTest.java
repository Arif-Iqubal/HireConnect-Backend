package com.hireconnect.auth.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LoggingAspect Tests")
class LoggingAspectTest {

    private LoggingAspect loggingAspect;

    private ProceedingJoinPoint proceedingJoinPoint;

    private JoinPoint joinPoint;

    private Signature signature;

    @BeforeEach
    void setUp() {

        loggingAspect = new LoggingAspect();

        proceedingJoinPoint = mock(ProceedingJoinPoint.class);

        joinPoint = mock(JoinPoint.class);

        signature = mock(Signature.class);

        when(proceedingJoinPoint.getSignature())
                .thenReturn(signature);

        when(joinPoint.getSignature())
                .thenReturn(signature);

        when(signature.getDeclaringTypeName())
                .thenReturn("com.hireconnect.auth.service.AuthService");

        when(signature.getName())
                .thenReturn("login");
    }

    @Test
    @DisplayName("should proceed and return result")
    void shouldProceedSuccessfully() throws Throwable {

        when(proceedingJoinPoint.proceed())
                .thenReturn("SUCCESS");

        Object result =
                loggingAspect.logAround(proceedingJoinPoint);

        assertThat(result)
                .isEqualTo("SUCCESS");

        verify(proceedingJoinPoint)
                .proceed();
    }

    @Test
    @DisplayName("should rethrow exception from proceeding join point")
    void shouldRethrowException() throws Throwable {

        RuntimeException exception =
                new RuntimeException("Something failed");

        when(proceedingJoinPoint.proceed())
                .thenThrow(exception);

        assertThatThrownBy(() ->
                loggingAspect.logAround(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Something failed");

        verify(proceedingJoinPoint)
                .proceed();
    }

    @Test
    @DisplayName("should execute logAfterThrowing with cause")
    void shouldExecuteLogAfterThrowingWithCause() {

        RuntimeException cause =
                new RuntimeException("Database down");

        RuntimeException exception =
                new RuntimeException("Wrapped", cause);

        assertThatCode(() ->
                loggingAspect.logAfterThrowing(joinPoint, exception))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should execute logAfterThrowing without cause")
    void shouldExecuteLogAfterThrowingWithoutCause() {

        RuntimeException exception =
                new RuntimeException("Simple error");

        assertThatCode(() ->
                loggingAspect.logAfterThrowing(joinPoint, exception))
                .doesNotThrowAnyException();
    }
}