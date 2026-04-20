package com.auth.aop;

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

	@Before("execution(* com.hireconnect.auth..*(..))")
	public void logBefore(JoinPoint joinPoint) {
		log.info("Entering: {}", joinPoint.getSignature());
	}

	@AfterReturning(value = "execution(* com.hireconnect.auth..*(..))", returning = "result")
	public void logAfter(JoinPoint joinPoint, Object result) {
		log.info("Exiting: {} | Response: {}", joinPoint.getSignature(), result);
	}
}