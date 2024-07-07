package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.LoadType;
import com.wecca.canoeanalysis.models.PiecewiseContinuousLoadDistribution;
import com.wecca.canoeanalysis.models.Section;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculusUtils
{
    // Provides integration
    public static SimpsonIntegrator integrator = new SimpsonIntegrator();

    /**
     * Returns the numerical derivative of a given function.
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

    /**
     * Ensures a set of distributions will form a continuous curve when their intervals are joined
     * @param pieces the pieces to validate
     * @param sections the sections of the pieces
     */
    public static void validatePiecewiseContinuity(List<UnivariateFunction> pieces, List<Section> sections) {
        if (sections.size() != pieces.size())
            throw new IllegalArgumentException("Unequal amount of sections and pieces");

        for (int i = 1; i < pieces.size(); i++) {
            Section prevSec = sections.get(i - 1);
            Section currSec = sections.get(i);
            if (prevSec.getRx() != currSec.getX())
                throw new IllegalArgumentException("Sections do not form a continuous interval.");
        }

            // Validate each piece for continuity on it's section
        for (int i = 0; i < pieces.size(); i++) {
           validateContinuity(pieces.get(i), sections.get(i));
        }
    }

    /**
     * Ensures the formed piecewise is either entirely non-negative or non-positive (i.e. can be 0 but never cross the x-axis)
     * @param pieces the pieces to validate
     * @param sections the sections of the pieces
     */
    public static void validatePiecewiseAsUpOrDown(List<UnivariateFunction> pieces, List<Section> sections) {
        UnivariateSolver solver = new BrentSolver(1e-10, 1e-14);
        int numSamples = 10000;

        boolean allNonNegative = true;
        boolean allNonPositive = true;

        for (int p = 0; p < pieces.size(); p++) {
            UnivariateFunction piece = pieces.get(p);
            Section section = sections.get(p);

            // Find zeros within the section
            double step = section.getLength() / (double) numSamples;
            List<Double> zeros = new ArrayList<>();
            double currentX = section.getX();
            for (int i = 1; i <= numSamples; i++) {
                double nextX = section.getX() + i * step;
                try {
                    double zero = solver.solve(1000, piece, currentX, nextX);
                    if (!Double.isNaN(zero)) {
                        zeros.add(zero);
                    }
                } catch (Exception ignored) {}
                currentX = nextX;
            }

            // Check the function value at a point within each interval (midpoint chosen arbitrarily)
            for (int i = 0; i < zeros.size() - 1; i++) {
                double midpoint = (zeros.get(i) + zeros.get(i + 1)) / 2;
                double value = piece.value(midpoint);
                if (value < 0)
                    allNonNegative = false;
                if (value > 0)
                    allNonPositive = false;
            }
        }

        if (!(allNonNegative || allNonPositive))
            throw new IllegalArgumentException("The piecewise function must be entirely non-negative or non-positive.");
    }

    /**
     * Validates that the distribution is continuous over the section within a specified tolerance.
     * Note: tolerance and numSamples may need to be tweaked to avoid false invalidations
     */
    public static void validateContinuity(UnivariateFunction function, Section section) {
        // Note: it's very possible this can cause false negatives and these numbers need to be tweaked in magnitude
        // It completely depends on how fast the function grows
        double tolerance = 1e-3;
        double numSamples = 10000.0;
        double step = section.getLength() / numSamples;

        double previousValue = function.value(section.getX());
        for (int i = 1; i < numSamples - 1; i++) {
            double x = section.getX() + i * step;
            double currentValue = function.value(x);
            if (Math.abs(currentValue - previousValue) > tolerance)
                throw new IllegalArgumentException("The distribution is not continuous within a reasonable tolerance");
            previousValue = currentValue;
        }
    }

    /**
     * Get the maximum or minimum value of a single piece based on the provided flag.
     * @param function the univariate function to analyze
     * @param section the section over which to analyze the function
     * @param findMax true to find the maximum value, false to find the minimum value
     * @return the maximum or minimum value
     */
    public static double getMaxOrMinValue(UnivariateFunction function, Section section, boolean findMax) {
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        GoalType goalType = findMax ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        UnivariatePointValuePair result = optimizer.optimize(
                MaxEval.unlimited(),
                new UnivariateObjectiveFunction(function),
                goalType,
                new SearchInterval(section.getX(), section.getRx())
        );
        return function.value(result.getPoint());
    }

    /**
     * Get the maximum signed value of a single piece (i.e. the highest positive high or lowest negative low value or 0)
     * @param function the univariate function to analyze
     * @param section the section over which to analyze the function
     * @return the maximum signed value
     */
    public static double getMaxSignedValue(UnivariateFunction function, Section section) {
        double maxValue = getMaxOrMinValue(function, section, true);
        double minValue = getMaxOrMinValue(function, section, false);
        return Math.abs(maxValue) >= Math.abs(minValue) ? maxValue : minValue;
    }

    /**
     * Get the maximum signed value of the piecewise (i.e. the highest positive high or lowest negative low value or 0)
     * @param piecewise the function to analyze
     * @return the maximum signed value
     */
    public static double getMaxSignedValue(PiecewiseContinuousLoadDistribution piecewise) {
        double maxAbsValue = 0;
        double signedMaxValue = 0;
        for (Map.Entry<Section, UnivariateFunction> piece : piecewise.getPieces().entrySet()) {
            double valueAtMax = getMaxSignedValue(piece.getValue(), piece.getKey());
            if (Math.abs(valueAtMax) > maxAbsValue) {
                maxAbsValue = Math.abs(valueAtMax);
                signedMaxValue = valueAtMax;
            }
        }
        return signedMaxValue;
    }
}
