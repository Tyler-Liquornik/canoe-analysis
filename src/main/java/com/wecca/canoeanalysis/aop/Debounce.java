package com.wecca.canoeanalysis.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Debounce a method with a set amount of milliseconds
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Debounce {

    // The amount of milliseconds to debounce
    int ms();
}
