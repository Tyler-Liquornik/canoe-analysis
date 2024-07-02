package com.wecca.canoeanalysis.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.optim.MaxEval;

public class MathUtils
{

    // Provides integration
    public static SimpsonIntegrator integrator = new SimpsonIntegrator();

    /**
     * Returns the numerical derivative of a given function.
     *
     * @param function the function to differentiate
     * @return the derivative function
     */
    public static UnivariateFunction differentiate(UnivariateFunction function)
    {
        double h = 1e-5; // Small value for h implies the limit as h -> 0
        return x -> (function.value(x + h) - function.value(x - h)) / (2 * h);
    }

    /**
     * @return the length of the curve function(x) on [a, b]
     */
    @JsonIgnore
    public static double getArcLength(UnivariateFunction function, double a, double b) {
        if (function == null)
            return 0;
        else
        {
            UnivariateFunction profileArcLengthElementFunction =
                    x -> Math.sqrt(1 + Math.pow(differentiate(function).value(x), 2));
            return integrator.integrate(MaxEval.unlimited().getMaxEval(), profileArcLengthElementFunction, a, b);
        }
    }
}
