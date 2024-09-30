package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import com.wecca.canoeanalysis.models.function.FunctionSection;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.optim.MaxEval;
import java.util.ArrayList;
import java.util.List;

public class CalculusUtils
{
    // Provides integration
    public static SimpsonIntegrator integrator = new SimpsonIntegrator();

    /**
     * Returns the numerical derivative of a given function.
     * @param function the function to differentiate
     * @return the derivative function
     */
    public static BoundedUnivariateFunction differentiate(BoundedUnivariateFunction function)
    {
        double h = 1e-6; // Small value for h implies the limit as h -> 0
        return x -> (function.value(x + h) - function.value(x - h)) / h;
    }

    /**
     * @return the length of the curve function(x) on [a, b]
     */
    public static double getArcLength(BoundedUnivariateFunction function, double a, double b) {
        if (function == null)
            return 0;
        else
        {
            BoundedUnivariateFunction profileArcLengthElementFunction =
                    x -> Math.sqrt(1 + Math.pow(differentiate(function).value(x), 2));
            return integrator.integrate(MaxEval.unlimited().getMaxEval(), profileArcLengthElementFunction, a, b);
        }
    }

    /**
     * @param piecewise the function to check for symmetry on its section
     * @return if the function is symmetrical or not
     */
    public static boolean isSymmetrical(PiecewiseContinuousLoadDistribution piecewise) {
        BoundedUnivariateFunction f = piecewise.getPiecedFunction();
        double startX = piecewise.getX();
        double endX = piecewise.getPieces().lastKey().getRx();
        double midpoint = (startX + endX) / 2.0;
        double tolerance = 1e-6;
        double stepSize = 1e-3;

        for (double x = startX; x <= midpoint; x += stepSize) {
            double symmetricX = 2 * midpoint - x;
            if (Math.abs(f.value(x) - f.value(symmetricX)) > tolerance) {
                return false;
            }
        }
        return true;
    }

    /**
     * Factory method to join a list of points x_i such that 0 < x_(i-1) < x_i < ... < x_n into intervals (section objects)
     * Each section has endpoints [x_(i-1), x_i], so there's n-1 sections formed and packaged into a list
     * @param endpoints the points to turn into sections (endpoints in terms of the formed intervals)
     * @return the list of sections
     */
    public static List<FunctionSection> sectionsFromEndpoints(List<Double> endpoints) {
        if (endpoints.size() < 2)
            throw new IllegalArgumentException("Cannot form a section without at least two points");

        List<FunctionSection> sections = new ArrayList<>();

        for (int i = 0; i < endpoints.size() - 1; i++) {
            double curr = endpoints.get(i);
            double next = endpoints.get(i + 1);

            if (next - curr < 0.01)
                throw new IllegalArgumentException("All sections must be of width at least 0.01m");

            sections.add(new FunctionSection(curr, next));
        }

        return sections;
    }

    /**
     * Ensures a set of distributions will form a continuous curve when their intervals are joined
     * @param pieces the pieces to validate
     * @param sections the sections of the pieces
     */
    public static void validatePiecewiseContinuity(List<BoundedUnivariateFunction> pieces, List<FunctionSection> sections) {
        if (sections.size() != pieces.size())
            throw new IllegalArgumentException("Unequal amount of sections and pieces");

        for (int i = 1; i < pieces.size(); i++) {
            FunctionSection prevSec = sections.get(i - 1);
            FunctionSection currSec = sections.get(i);
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
    public static void validatePiecewiseAsUpOrDown(List<BoundedUnivariateFunction> pieces, List<FunctionSection> sections) {
        UnivariateSolver solver = new BrentSolver(1e-10, 1e-14);
        int numSamples = 1000;

        boolean allNonNegative = true;
        boolean allNonPositive = true;

        for (int p = 0; p < pieces.size(); p++) {
            BoundedUnivariateFunction piece = pieces.get(p);
            FunctionSection section = sections.get(p);

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

            double tolerance = 1e-3;
            for (int i = 0; i < zeros.size() - 1; i++) {
                double midpoint = (zeros.get(i) + zeros.get(i + 1)) / 2;
                double value = piece.value(midpoint);
                if (value < -tolerance)
                    allNonNegative = false;
                if (value > tolerance)
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
    public static void validateContinuity(BoundedUnivariateFunction function, FunctionSection section) {
        // Note: it's very possible this can cause false negatives and these numbers need to be tweaked in magnitude
        // It completely depends on how fast the function grows
        double tolerance = 1e-2;
        double numSamples = 10000.0;
        double step = section.getLength() / numSamples;

        double previousValue = function.value(section.getX());
        for (int i = 1; i < numSamples - 1; i++) {
            double x = section.getX() + i * step;
            double currentValue = function.value(x);
            if (Math.abs(currentValue - previousValue) > tolerance)
                throw new IllegalArgumentException("The function is not continuous within a reasonable tolerance");
            previousValue = currentValue;
        }
    }

    /**
     * Round a double to x digits after the decimal point.
     * @param num the number to round.
     * @param numDigits the number of digits to round to.
     * @return the rounded double.
     */
    public static double roundXDecimalDigits(double num, int numDigits) {
        double factor = Math.pow(10, numDigits);
        return Math.round(num * factor) / factor;
    }
}
