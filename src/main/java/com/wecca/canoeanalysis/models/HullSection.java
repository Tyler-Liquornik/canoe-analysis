package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.models.functions.StringableUnivariateFunction;
import com.wecca.canoeanalysis.utils.MathUtils;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;
import java.util.function.Function;

/**
 * In order ot apply calculus for precise, real-world representative modelling, we need to divide things into discrete sections
 * This class serves to apply this principle to the canoe hull
 * A key advantage of this is that each hull section can have its own geometry, allowing the use of piecewise functions
 * This is implemented with each hull section having its own geometry defining curves
 * Validation is then required when constructing a hull from hull sections to ensure the overall hull geometry is made up of smooth, C^1 curves
 *
 * Let the following conventions by applied for dimensioning purposes for language consistency
 *
 *         7 Z
 *       /
 *     /
 *    +-----------> X
 *   |               Length
 *   |         <------------------->
 *   V Y        ___________________
 *         |   | \                |
 *         |   |  |`----......___|      7 Width
 *         |   𝘓_|______________|     /
 *         |   \|              |    /
 *  Height V    `----......___|   /
 *
 * "Length" refers to the x direction (left / right)
 * "Height" refers to the y direction (up / down)
 * "Width" refers to the z direction (into / out of the screen)
 * "Thickness" refers to the normal direction of a surface to provide thickness to (+/- orientation is context dependent)
 */
@Getter @Setter
public class HullSection extends Section
{
    private StringableUnivariateFunction profileCurve;
    private double width;
    private double wallsThickness;
    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    private boolean hasBulkhead;
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    /**
     * @param start the start x position of the section's profile curve interval
     * @param end the end x position of the section's profile curve interval
     * @param width the width of the section in meters in the z-direction ("into/out of the screen" direction)
     * @param hasBulkhead whether the empty space between the inner hull walls should be filled with a styrofoam bulkhead
     * @param profileCurve the function that defines the shape of the hull in this section in the xy-plane bounded above by y = 0
     */
    public HullSection(StringableUnivariateFunction profileCurve, double start, double end, double width, double thickness, boolean hasBulkhead) {
        super(start, end);
        validateProfileCurve(profileCurve.getFunction());
        this.profileCurve = profileCurve;
        this.width = width;
        this.wallsThickness = thickness;
        this.hasBulkhead = hasBulkhead;
    }

    /**
     * Basic constructor for a simple model, useful before the user defines geometry / material properties
     * @param start the start x position of the section's profile curve interval
     * @param end the end x position of the section's profile curve interval
     */
    public HullSection(double start, double end) {
        super(start, end);
    }

    /**
     * @return the length of the curve profileCurve(x) on [x, rX]
     */
    @JsonIgnore
    public double getXYProfileArcLength() {
        UnivariateFunction profileArcLengthElementFunction =
                x -> Math.sqrt(1 + Math.pow(MathUtils.differentiate(profileCurve).value(x), 2));
        return MathUtils.integrator.integrate(1000, profileArcLengthElementFunction, x, rx);
    }

    /**
     * @return the area underneath y = 0 to y = profileCurve(x)
     */
    @JsonIgnore
    public double getXYProfileArea() {
        return -MathUtils.integrator.integrate(1000, profileCurve, x, rx);
    }

    /**
     * @return the full volume of the section (includes empty space if no bulkhead fill)
     */
    @JsonIgnore
    public double getVolume() {
        return getXYProfileArea() * width;
    }

    /**
     * Calculates the approximate volume of the section excluding inner volume between inner hull walls
     * Note that inner volume may be empty or filled with a styrofoam bulkhead
     * @return the volume of concrete of the section in cubic meters
     */
    @JsonIgnore
    public double getConcreteVolume() {
        double wallsVolume = 2 * getXYProfileArea() * wallsThickness;
        double floorVolume = getXYProfileArcLength() * wallsThickness * (width - (2 * wallsThickness));
        double bulkheadTopCoverVolume = hasBulkhead ? getLength() * wallsThickness * width : 0;
        return wallsVolume + floorVolume + bulkheadTopCoverVolume;
    }

    /**
     * Calculates the volume of the bulkhead that fills the section between hull walls and the hull floor
     * If fillBulkhead is set to false this is automatically 0
     * @return the bulkhead volume
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        return hasBulkhead ? getVolume() - getConcreteVolume() : 0;
    }

    /**
     * @return the mass of the section
     */
    @JsonIgnore
    public double getMass() {
        return getConcreteVolume() * concreteDensity + getBulkheadVolume() * getBulkheadDensity();
    }

    /**
     * @return the weight of the section under earth's gravity
     */
    @JsonIgnore
    public double getWeight() {
        return getMass() * PhysicalConstants.GRAVITY.getValue();
    }
    /**
     * Validates that the hull shape function is non-positive on its domain [start, end]
     * This convention allows waterline height y = h (downward is +y)
     * Note that this means the topmost point of the hull on the y-axis is y = 0
     */
    private void validateProfileCurve(Function<Double, Double> profileCurve)
    {
        // Convert the hullShapeFunction to UnivariateFunction for compatibility with Apache Commons Math
        // Need to negate the function as BrentOptimizer finds the min, and we want the max
        UnivariateFunction profileCurveAsUnivariateFunction = profileCurve::apply;
        UnivariateFunction negatedProfileCurve = x -> -profileCurveAsUnivariateFunction.value(x);

        // Use BrentOptimizer to find the maximum value of the hull shape function on [start, end]
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(negatedProfileCurve);
        SearchInterval searchInterval = new SearchInterval(x, rx);

        // Optimize (find minimum of the negated function, which corresponds to the maximum of the original function)
        UnivariatePointValuePair result = optimizer.optimize(MaxEval.unlimited(), objectiveFunction, searchInterval);

        // Negate the result to get the maximum value
        double maxValue = -result.getValue();

        // Check if the maximum value is non-positive
        if (maxValue > 0)
            throw new IllegalArgumentException("Hull shape function must be non-positive on its domain [start, end]");
    }

}