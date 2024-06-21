package com.wecca.canoeanalysis.utils;

import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.function.Function;

public class MathUtils {
    /**
     * Returns the numerical derivative of a given function.
     *
     * @param function the function to differentiate
     * @return the derivative function
     */
    public static UnivariateFunction derivative(UnivariateFunction function) {
        double h = 1e-5; // Small value for h implies the limit as h -> 0
        return x -> (function.value(x + h) - function.value(x - h)) / (2 * h);
    }
}
