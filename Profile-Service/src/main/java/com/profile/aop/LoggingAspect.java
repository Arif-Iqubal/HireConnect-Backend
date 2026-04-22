package com.profile.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // 🔹 Before method execution
    @Before("execution(* com.hireconnect.profile..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Entering: {} | Args: {}",
                joinPoint.getSignature(),
                joinPoint.getArgs());
    }

    // 🔹 After method execution
    @AfterReturning(value = "execution(* com.hireconnect.profile..*(..))", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        log.info("Exiting: {} | Response: {}",
                joinPoint.getSignature(),
                result);
    }

    // 🔹 Exception logging
    @AfterThrowing(value = "execution(* com.hireconnect.profile..*(..))", throwing = "ex")
    public void logException(JoinPoint joinPoint, Exception ex) {
        log.error("Exception in: {} | Message: {}",
                joinPoint.getSignature(),
                ex.getMessage());
    }
}
