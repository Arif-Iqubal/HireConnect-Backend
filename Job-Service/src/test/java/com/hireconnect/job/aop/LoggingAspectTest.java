package com.hireconnect.job.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingAspectTest {

    private final LoggingAspect aspect = new LoggingAspect();

    @Test
    void logAroundReturnsProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("result");

        assertThat(aspect.logAround(joinPoint)).isEqualTo("result");
    }

    @Test
    void logAroundRethrowsProceedException() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.logAround(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    private ProceedingJoinPoint joinPoint(Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringTypeName()).thenReturn("JobService");
        when(signature.getName()).thenReturn("createJob");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }
}
