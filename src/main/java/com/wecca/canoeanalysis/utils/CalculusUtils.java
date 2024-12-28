package com.wecca.canoeanalysis.utils;

import Jama.Matrix;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import com.wecca.canoeanalysis.models.function.Section;
import javafx.geometry.Point2D;
import org.apache.commons.math3.analysis.BivariateFunction;
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
        return x -> (function.value(x + h) - function.value(x - h)) / (2 * h);
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
     * Converts polar coordinates to Cartesian coordinates with respect to a given origin.
     * @param polarPoint the point in polar form where x represents the radius (r) and y represents the angle (theta) in degrees
     * @param origin the reference point to use as the origin
     * @return a Point2D in Cartesian form (x, y) relative to the origin
     */
    public static Point2D toCartesian(Point2D polarPoint, Point2D origin) {
        double r = polarPoint.getX();
        double theta = Math.toRadians(polarPoint.getY() + 90);
        double x = r * Math.cos(theta) + origin.getX();
        double y = r * Math.sin(theta) + origin.getY();
        return new Point2D(x, y);
    }

    /**
     * Converts Cartesian coordinates to polar coordinates with respect to a given origin.
     * Note: the zero degree line shifted ninety degrees to the vertical upward ray from y = 0 to y = inf
     * This is because control points which use the polar system are either bounded to the left or right half-plane around their knot
     * Otherwise, the right half would be theta in {[0, 90] U [270, 360]} which cannot go on a knob as multiple joined intervals
     * @param cartesianPoint the point in Cartesian form (x, y)
     * @param origin the reference point to use as the origin
     * @return a Point2D where x represents the radius (r) and y represents the angle (theta) in degrees (0 <= theta < 360)
     */
    public static Point2D toPolar(Point2D cartesianPoint, Point2D origin) {
        double x = cartesianPoint.getX() - origin.getX();
        double y = cartesianPoint.getY() - origin.getY();
        double r = Math.sqrt(x * x + y * y);
        double theta = Math.toDegrees(Math.atan2(y, x)) - 90;

        // Normalize theta to a 360 degree range
        return new Point2D(r, theta > 0 ? theta : theta + 360);
    }

    /**
     * @deprecated by new floating solver algorithm which can solve asymmetrical load cases so we no longer need to check for symmetry
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
    public static List<Section> sectionsFromEndpoints(List<Double> endpoints) {
        if (endpoints.size() < 2)
            throw new IllegalArgumentException("Cannot form a section without at least two points");

        List<Section> sections = new ArrayList<>();

        for (int i = 0; i < endpoints.size() - 1; i++) {
            double curr = endpoints.get(i);
            double next = endpoints.get(i + 1);

            if (next - curr < 0.01)
                throw new IllegalArgumentException("All sections must be of width at least 0.01m");

            sections.add(new Section(curr, next));
        }

        return sections;
    }

    /**
     * Ensures a set of distributions will form a continuous curve when their intervals are joined
     * @param pieces the pieces to validate
     * @param sections the sections of the pieces
     */
    public static void validatePiecewiseContinuity(List<BoundedUnivariateFunction> pieces, List<Section> sections) {
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
    public static void validatePiecewiseAsUpOrDown(List<BoundedUnivariateFunction> pieces, List<Section> sections) {
        UnivariateSolver solver = new BrentSolver(1e-10, 1e-14);
        int numSamples = 100;

        boolean allNonNegative = true;
        boolean allNonPositive = true;

        for (int p = 0; p < pieces.size(); p++) {
            BoundedUnivariateFunction piece = pieces.get(p);
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

            double tolerance = 1e-2;
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
    public static void validateContinuity(BoundedUnivariateFunction function, Section section) {
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

    /**
     * @param x value of the first variable to evaluate the Jacobian for
     * @param y value of the second variable to evaluate the Jacobian for
     * @param f1  The first equation in the system, f1(x, y)
     * @param f2  The first equation in the system, f2(x, y)
     * @return the Jacobian matrix J for the system of equations [f1(x, y) = 0, f2(x, y) = 0] in R^2
     */
    public static Matrix evaluateR2Jacobian(double x, double y, BivariateFunction f1, BivariateFunction f2) {
        // Partial derivatives of f1 and f2
        BoundedUnivariateFunction df1_dx = differentiate(X -> f1.value(X, y));
        BoundedUnivariateFunction df1_dy = differentiate(Y -> f1.value(x, Y));
        BoundedUnivariateFunction df2_dx = differentiate(X -> f2.value(X, y));
        BoundedUnivariateFunction df2_dy = differentiate(Y -> f2.value(x, Y));

        // Construct the Jacobian
        Matrix jacobian = new Matrix(2, 2);
        jacobian.set(0, 0, df1_dx.value(x)); // ∂f1 / ∂x evaluated at x
        jacobian.set(0, 1, df1_dy.value(y)); // ∂f1 / ∂y evaluated at y
        jacobian.set(1, 0, df2_dx.value(x)); // ∂f2 / ∂x evaluated at x
        jacobian.set(1, 1, df2_dy.value(y)); // ∂f2 / ∂y evaluated at y
        return jacobian;
    }

    /**
     * Creates a composite function from a list of bounded univariate functions
     * along with their corresponding sections.
     * Shifts the result so that its minimum y value is at y = 0.
     *
     * @param functions The list of bounded univariate functions.
     * @param sections  The list of sections that correspond to each function.
     * @return The shifted composite function.
     */
    public static BoundedUnivariateFunction createCompositeFunctionShiftedPositive(List<BoundedUnivariateFunction> functions, List<Section> sections) {
        if (functions.size() != sections.size())
            throw new IllegalArgumentException("The number of functions must match the number of sections.");

        // Create the composite function that checks each function's section before evaluation
        BoundedUnivariateFunction f = x -> {
            for (int i = 0; i < functions.size(); i++) {
                BoundedUnivariateFunction func = functions.get(i);
                Section section = sections.get(i);
                if (section.getX() <= x && x <= section.getRx())
                    return func.value(x);
            }
            throw new IllegalArgumentException("x is out of bounds of the provided functions.");
        };

        Section fullSection = new Section(sections.getFirst().getX(), sections.getLast().getRx());
        return x -> f.value(x) - f.getMinValue(fullSection);
    }

    /**
     * Essentially an overload of createCompositeFunctionShiftedPositive
     * @param functions the cubic bezier functions which have the section encoded into their constructions points
     * @return the shifted bezier spline based function.
     */
    public static BoundedUnivariateFunction createBezierSplineFunctionShiftedPositive(List<CubicBezierFunction> functions) {
        List<BoundedUnivariateFunction> functionsMapped = functions.stream().map(CubicBezierFunction::getFunction).toList();
        List<Section> sections = functions.stream().map(f -> new Section(f.getX1(), f.getX2())).toList();
        return createCompositeFunctionShiftedPositive(functionsMapped, sections);
    }
}
