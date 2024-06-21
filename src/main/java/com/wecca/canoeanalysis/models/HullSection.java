package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.utils.MathUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;

import java.util.function.Function;

/**
 * Represents a section of the canoe with start and end positions, width, and bulkhead properties.
 */
@Getter @Setter @NoArgsConstructor
public class HullSection {

    /**
     * Let the following conventions by applied for dimensioning the canoe.
     *
     *        7 +Z
     *      /
     *    /
     *  +-----------> +X
     *  |
     *  |
     *  |
     *  V +Y
     *
     * "Length" refers to the +x direction
     * "Height" refers to the to the +y direction
     * "Width" refers to the +z direction
     * "Thickness" refers to the positively oriented normal direction of the surface in discussion (i.e. k-hat > 0)
     *
     */

    private UnivariateFunction profileCurve;
    private double start;
    private double end;
    private double width;
    private double thickness;
    private boolean fillBulkhead;
    SimpsonIntegrator integrator;

    /**
     * Constructs a HullSection with specified dimensions and bulkhead properties.
     *
     * @param start the start x position of the section's profile curve interval
     * @param end the end x position of the section's profile curve interval
     * @param width the width of the section in meters in the z-direction ("into/out of the screen" direction)
     * @param fillBulkhead whether the empty space between the inner hull walls should be filled with a styrofoam bulkhead
     *                     Note: filling with bulkhead
     * @param profileCurve the function that defines the shape of the hull in this section in the xy-plane
     */
    public HullSection(Function<Double, Double> profileCurve, double start, double end, double width, double thickness, boolean fillBulkhead) {
        this.profileCurve = profileCurve::apply;
        this.start = start;
        this.end = end;
        this.width = width;
        this.thickness = thickness;
        this.fillBulkhead = fillBulkhead;
        this.integrator = new SimpsonIntegrator();

        validateProfileCurve(profileCurve);
    }

    public double getLength() {
        return end - start;
    }


    /**
     * Calculates the approximate volume of the section excluding inner volume between inner hull walls
     * Note that inner volume may be empty or filled with a styrofoam bulkhead
     * @return the volume of concrete of the section in cubic meters
     */
    public double getConcreteVolume() {
        double hullSectionProfileArea = -integrator.integrate(1000, profileCurve, start, end); // Negated as area is below y = 0 and is thus negative area
        double wallsVolume = hullSectionProfileArea * thickness * 2;
        UnivariateFunction slopeFunction = x -> Math.sqrt(1 + Math.pow(MathUtils.derivative(profileCurve).value(x), 2));
        double floorVolume = thickness * width * integrator.integrate(1000, slopeFunction, thickness, width - thickness);

        // Bulkheads covered by a flat top which matches floor thickness and extra wall facing into the center of the canoe
        double casingWallHeight = Math.max(profileCurve.value(start), profileCurve.value(end));
        double bulkheadCasingVolume = fillBulkhead ? thickness * width * getLength(): 0;

        return wallsVolume + floorVolume + bulkheadCasingVolume;
    }

    /**
     * Calculates the volume of the bulkhead that fills the section between hull walls and the hull floor
     * If fillBulkhead is set to false this is automatically 0
     * @return the bulkhead volume
     */
    public double getBulkheadVolume() {
        if (!fillBulkhead)
            return 0;
        else {
            double hullSectionProfileArea = -integrator.integrate(1000, profileCurve, start, end);
            double fullSectionVolume = hullSectionProfileArea * width;
            return fullSectionVolume - getConcreteVolume();
        }
    }

    /**
     * Validates that the hull shape function is non-positive on its domain [start, end]
     * This convention allows waterline height h for a floating hull to be a distance below the top of the null at h = 0
     * Uses calculus to avoid checking all points individually by checking only critical points and domain endpoints
     */
    private void validateProfileCurve(Function<Double, Double> profileCurve) {
        // Convert the hullShapeFunction to UnivariateFunction for compatibility with Apache Commons Math
        // Need to negate the function as BrentOptimizer finds the min, and we want the max
        UnivariateFunction profileCurveAsUnivariateFunction = profileCurve::apply;
        UnivariateFunction negatedProfileCurve = x -> -profileCurveAsUnivariateFunction.value(x);

        // Use BrentOptimizer to find the maximum value of the hull shape function on [start, end]
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(negatedProfileCurve);
        SearchInterval searchInterval = new SearchInterval(start, end);

        // Optimize (find minimum of the negated function, which corresponds to the maximum of the original function)
        UnivariatePointValuePair result = optimizer.optimize(
                MaxEval.unlimited(),
                objectiveFunction,
                searchInterval
        );

        double maxValue = -result.getValue(); // Negate the result to get the maximum value

        // Check if the maximum value is non-positive
        if (maxValue > 0) {
            throw new IllegalArgumentException("Hull shape function must be non-positive on its domain [start, end]");
        }
    }
}