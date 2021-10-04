package com.sample.api.server.test;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TestAspect {

	public static boolean aspectWorked = false;

	@Around("execution(* *(..)) && @annotation(com.sample.api.server.test.TestAspectAnnotation)")
	public Object eventDispatchThread(final ProceedingJoinPoint pjp) throws Throwable {
		aspectWorked = true;
		return pjp.proceed();
	}

}
