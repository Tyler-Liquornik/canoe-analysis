package com.wecca.canoeanalysis.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable tracing inputs and outputs of methods for debugging
 * Apply to a class to enable all methods in the class
 * Apply to select methods to enable just those select methods
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {
}
