package com.wecca.canoeanalysis.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TracingAspect {

    private final TraceBuilder logBuilder = new TraceBuilder();

    /**
     * Pointcuts are weaved into methods as follows:
     * All methods in classes marked @Traceable
     * All methods marked @Traceable except those marked @TraceIgnore
     * No lambda methods or auto-generated synthetic methods within @Traceable methods
     */
    @Pointcut("(@within(com.wecca.canoeanalysis.aop.Traceable) " +
            "|| @annotation(com.wecca.canoeanalysis.aop.Traceable)) " +
            "&& execution(* com.wecca.canoeanalysis..*(..)) " +
            "&& !@annotation(com.wecca.canoeanalysis.aop.TraceIgnore) " +
            "&& !execution(* lambda*(..)) " +
            "&& !execution(* $*(..))")
    public void pointcut() {
    }

    /**
     * Apply the LogBuilder advice before the method is invoked
     */
    @Before("pointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        logBuilder.beforeAdviceMethod(joinPoint);
    }

    /**
     * Apply the LogBuilder advice after the method returns
     */
    @AfterReturning(pointcut = "pointcut()", returning = "response")
    public void logger(JoinPoint joinPoint, Object response){
        logBuilder.afterAdviceMethod(joinPoint, response);
    }
}
