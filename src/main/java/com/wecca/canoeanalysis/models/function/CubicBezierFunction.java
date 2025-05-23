package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.geometry.Point2D;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import math.geom2d.spline.CubicBezierCurve2D;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.optim.MaxEval;
import java.util.Arrays;
import java.util.List;

/**
 * A Cubic Bézier curve that passes the vertical line test, therefore can be represented as a function.
 */
@Getter @Setter @EqualsAndHashCode
public class CubicBezierFunction implements ParameterizedBoundedUnivariateFunction {

    @JsonProperty("x1")
    private double x1;
    @JsonProperty("y1")
    private double y1;
    @JsonProperty("controlX1")
    private double controlX1;
    @JsonProperty("controlY1")
    private double controlY1;
    @JsonProperty("controlX2")
    private double controlX2;
    @JsonProperty("controlY2")
    private double controlY2;
    @JsonProperty("x2")
    private double x2;
    @JsonProperty("y2")
    private double y2;

    @JsonIgnore
    private final FunctionType type = FunctionType.CUBIC_BEZIER_FUNCTION;
    @JsonIgnore
    private final UnivariateSolver solver = new BrentSolver(1e-6);

    @JsonIgnore @Getter(AccessLevel.NONE)
    private final double T_MIN = 0.0;
    @JsonIgnore @Getter(AccessLevel.NONE)
    private final double T_MAX = 1.0;


    /**
     * Constructor for JSON deserialization
     */
    @JsonCreator
    public CubicBezierFunction(
            @JsonProperty("x1") double x1, @JsonProperty("y1") double y1,
            @JsonProperty("controlX1") double controlX1, @JsonProperty("controlY1") double controlY1,
            @JsonProperty("controlX2") double controlX2, @JsonProperty("controlY2") double controlY2,
            @JsonProperty("x2") double x2, @JsonProperty("y2") double y2
    ) {
        initialize(x1, y1, controlX1, controlY1, controlX2, controlY2, x2, y2);
    }

    /**
     * Copy constructor for deep cloning.
     * @param src the source CubicBezierFunction to copy
     */
    public CubicBezierFunction(CubicBezierFunction src) {
        this.x1 = src.x1; this.y1 = src.y1;
        this.controlX1 = src.controlX1; this.controlY1 = src.controlY1;
        this.controlX2 = src.controlX2; this.controlY2 = src.controlY2;
        this.x2 = src.x2; this.y2 = src.y2;
    }

    @Override
    public void initialize(double... params) {
        if (params.length != 8) {
            throw new IllegalArgumentException("CubicBezierFunction requires exactly 8 parameters: x1, y1, x2, y2, x3, y3, x4, y4.");
        }

        // Assign parameters to fields
        this.x1 = params[0];
        this.y1 = params[1];
        this.controlX1 = params[2];
        this.controlY1 = params[3];
        this.controlX2 = params[4];
        this.controlY2 = params[5];
        this.x2 = params[6];
        this.y2 = params[7];

        // validateAsFunction();
    }

    @JsonIgnore
    private CubicBezierCurve2D getCubicBezierCurve2D() {
        return new CubicBezierCurve2D(x1, y1, controlX1, controlY1, controlX2, controlY2, x2, y2);
    }

    @JsonIgnore
    public BoundedUnivariateFunction getFunction() {
        return x -> {
            double t = getT(x);
            return getCubicBezierCurve2D().point(t).y();
        };
    }

    /**
     * Returns the y value for a given x.
     * @param x the x value
     * @return the corresponding y value on the curve.
     */
    @Override
    public double value(double x) {
        return getFunction().value(x);
    }

    /**
     * Computes the derivative dx/dt at parameter t.
     * @param t parameter in [0,1]
     * @return dx/dt
     */
    public double derivativeX(double t) {
        double oneMinusT = 1 - t;
        return 3 * oneMinusT * oneMinusT * (controlX1 - x1)
                + 6 * oneMinusT * t * (controlX2 - controlX1)
                + 3 * t * t * (x2 - controlX2);
    }

    /**
     * Root-finding method to solve for the parameter 't' where the x-coordinate of the Bézier curve
     * at the point corresponding to 't' matches the given x-value.
     * Works by numerically solving the equation B_x(t) - x = 0, where B_x(t) is the
     * x-coordinate of the cubic Bézier curve at parameter 't'.
     *
     * @param x The x-coordinate for which we want to find the corresponding parameter 't'.
     * @return The value of the parameter 't' (in the range [0, 1]) where the x-coordinate of the Bézier curve matches the input 'x'.
     */
    public double getT(double x) {
        // Round the input x and the endpoints (using 6 decimal digits; adjust if needed).
        double roundedX = CalculusUtils.roundXDecimalDigits(x, 6);
        double roundedMin = CalculusUtils.roundXDecimalDigits(getX(), 6);
        double roundedMax = CalculusUtils.roundXDecimalDigits(getRx(), 6);
        if (roundedX < roundedMin - 1e-3 || roundedX > roundedMax + 1e-3)
            throw new IllegalArgumentException("x = " + roundedX + " is out of bounds for this Bézier curve [" + roundedMin + ", " + roundedMax + "]");

        // If x out of bounds, clamp it to the respective bound
        if (roundedX < roundedMin) roundedX = roundedMin;
        if (roundedX > roundedMax) roundedX = roundedMax;

        // Define and root-solve the function for which we want to find the root:
        // f(t) = (rounded Bézier x-value at t) - (rounded target x)
        double finalRoundedX = roundedX;
        UnivariateFunction xFunc = t -> CalculusUtils.roundXDecimalDigits(getCubicBezierCurve2D().point(t).x(), 6) - finalRoundedX;
        try {
            return solver.solve(MaxEval.unlimited().getMaxEval(), xFunc, T_MIN, T_MAX);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to solve for t given x = " + roundedX + " within bounds [" + roundedMin + ", " + roundedMax + "]", e);
        }
    }

    /**
     * @return x, the left endpoint of the section which this curve is on
     */
    @JsonIgnore
    public double getX() {
        double xAtTMin = getCubicBezierCurve2D().point(T_MIN).x();
        double xAtTMax = getCubicBezierCurve2D().point(T_MAX).x();
        return Math.min(xAtTMin, xAtTMax);
    }

    /**
     * @return rX, the left endpoint of the section which this curve is on
     */
    @JsonIgnore
    public double getRx() {
        double xAtTMin = getCubicBezierCurve2D().point(T_MIN).x();
        double xAtTMax = getCubicBezierCurve2D().point(T_MAX).x();
        return Math.max(xAtTMin, xAtTMax);
    }

    /**
     * Validates that the cubic Bézier curve passes the vertical line test.
     * Samples points along the curve and checks if the x-coordinates are strictly increasing or decreasing
     * As we increment t through (0, 1] with small steps
     * Throws an exception if the curve does not pass the vertical line test.
     */
    private void validateAsFunction() {
        CubicBezierCurve2D curve = getCubicBezierCurve2D();
        double prevX = curve.point(T_MIN).x();
        boolean xIsIncreasingWithT = true;
        boolean xIsDecreasingWithT = true;
        int numSteps = 500;
        double step = (T_MAX - T_MIN) / numSteps;

        for (double t = step; t <= T_MAX; t += step) {
            double currentX = curve.point(t).x();
            if (currentX > prevX)
                xIsDecreasingWithT = false;
            else if (currentX < prevX)
                xIsIncreasingWithT = false;
            prevX = currentX;
        }

        if (!xIsIncreasingWithT && !xIsDecreasingWithT)
            throw new IllegalArgumentException("The Bézier curve fails the vertical line test and cannot be represented as a function.");
    }

    /**
     * @return a list of Point2D objects for each of the knot and control points
     */
    @JsonIgnore
    public List<Point2D> getKnotAndControlPoints() {
        Point2D p1 = new Point2D(x1, y1);
        Point2D cp1 = new Point2D(controlX1, controlY1);
        Point2D cp2 = new Point2D(controlX2, controlY2);
        Point2D p2 = new Point2D(x2, y2);
        return Arrays.asList(p1, cp1, cp2, p2);
    }

    /**
     * @return a list of Point2D objects for each of the knot points
     */
    @JsonIgnore
    public List<Point2D> getKnotPoints() {
        Point2D p1 = new Point2D(x1, y1);
        Point2D p2 = new Point2D(x2, y2);
        return Arrays.asList(p1, p2);
    }

    /**
     * @return a list of Point2D objects for each of the control points
     */
    @JsonIgnore
    public List<Point2D> getControlPoints() {
        Point2D p1 = new Point2D(controlX1, controlY1);
        Point2D p2 = new Point2D(controlX2, controlY2);
        return Arrays.asList(p1, p2);
    }
}
