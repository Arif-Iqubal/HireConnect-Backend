package com.job.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.hireconnect.job..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("➡️ {}", joinPoint.getSignature());
    }

    @AfterReturning(value = "execution(* com.hireconnect.job..*(..))", returning = "res")
    public void logAfter(JoinPoint joinPoint, Object res) {
        log.info("⬅️ {} | {}", joinPoint.getSignature(), res);
    }
}