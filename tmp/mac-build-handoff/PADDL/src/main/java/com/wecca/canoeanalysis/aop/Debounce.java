package com.wecca.canoeanalysis.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Debounce a method with a set amount of milliseconds
 * The purpose of this is to improve the performance of a high frequency stream of method calls
 * This occurs with things like moving the mouse: events are fired per few pixels the mouse moves
 * ----------------------------------------------------------------------------------------------
 * Debouncing will ignore all method calls except the last call in each x ms time frame
 * Use carefully, this could either improve or hurt performance if not used correctly
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Debounce {

    // The amount of milliseconds to debounce
    int ms();
}
