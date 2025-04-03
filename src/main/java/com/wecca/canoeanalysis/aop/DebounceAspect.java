package com.wecca.canoeanalysis.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class DebounceAspect {

    /**
     * Intercepts any execution of a method annotated with @Debounce, wrapped the executive with a debouncer
     */
    @Around("execution(@com.wecca.canoeanalysis.aop.Debounce * *(..)) && @annotation(debounceAnnotation)")
    public Object debounceAdvice(ProceedingJoinPoint pjp, Debounce debounceAnnotation) {
        int delayMs = debounceAnnotation.ms();
        final Object[] args = pjp.getArgs();
        Debouncer.debounceConsumer((Object[] latestArgs) -> {
            try {
                pjp.proceed(latestArgs);
            } catch (Throwable t) {
                throw new RuntimeException("Exception in debounced method", t);
            }
        }, args, delayMs);

        return null;
    }
}