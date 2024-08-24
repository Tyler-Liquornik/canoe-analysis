package com.wecca.canoeanalysis.aop;

import com.wecca.canoeanalysis.models.data.DevConfig;
import com.wecca.canoeanalysis.services.YamlMarshallingService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.io.IOException;

@Aspect
public class TracingAspect {

    private static final TraceBuilder logBuilder = new TraceBuilder();

    /**
     * Pointcuts defined for all methods satisfying:
     * 1. in package com.wecca.canoeanalysis
     * 2. marked @Traceable, or in a class marked @Traceable
     * 3. not marked @TraceIgnore in a class make @Traceable
     * 4. not a lambda or synthetic method
     */
    @Pointcut("execution(* com.wecca.canoeanalysis..*(..)) " +
            "&& (@annotation(com.wecca.canoeanalysis.aop.Traceable) || @within(com.wecca.canoeanalysis.aop.Traceable)) " +
            "&& !@annotation(com.wecca.canoeanalysis.aop.TraceIgnore) " +
            "&& !execution(* lambda*(..)) " +
            "&& !execution(* $*(..))")
    public void pointcut() {
    }

    /**
     * Apply the LogBuilder advice before the method is invoked
     * Weaves in a join point as the first line of the method
     */
    @Before("pointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        if (YamlMarshallingService.TRACING)
            logBuilder.beforeAdviceMethod(joinPoint);
    }

    /**
     * Apply the LogBuilder advice after the method returns
     * Weaves in a join point as the last line of the method before the return
     */
    @AfterReturning(pointcut = "pointcut()", returning = "response")
    public void logger(JoinPoint joinPoint, Object response) {
        if (YamlMarshallingService.TRACING)
            logBuilder.afterAdviceMethod(joinPoint, response);
    }
}
