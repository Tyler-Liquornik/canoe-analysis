package com.wecca.canoeanalysis.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TracingAspect {

    private final TraceBuilder logBuilder = new TraceBuilder();

    @Pointcut("@within(com.wecca.canoeanalysis.aop.Traceable) && execution(* *(..)) && !execution(* lambda*(..)) && !execution(* $*(..))")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        logBuilder.beforeAdviceMethod(joinPoint);
    }

    @AfterReturning(pointcut = "pointcut()", returning = "response")
    public void logger(JoinPoint joinPoint, Object response){
        logBuilder.afterAdviceMethod(joinPoint, response);
    }
}
