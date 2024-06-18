package com.wecca.canoeanalysis.utils;

import java.util.function.Function;

public class MathUtils {
    // Numerical integration using the trapezoidal rule
    public static double integrate(Function<Double, Double> func, double start, double end) {
        int numSteps = 1000;
        double stepSize = (end - start) / numSteps;
        double sum = 0.0;
        for (int i = 0; i < numSteps; i++) {
            double x1 = start + i * stepSize;
            double x2 = start + (i + 1) * stepSize;
            sum += 0.5 * (func.apply(x1) + func.apply(x2)) * stepSize;
        }
        return sum;
    }
}
