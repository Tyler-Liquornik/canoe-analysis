package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.models.functions.StringableUnivariateFunction;
import com.wecca.canoeanalysis.models.functions.VertexFormParabola;
import com.wecca.canoeanalysis.utils.CalculusUtils;
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
 * Validation is then required when constructing a hull from hull sections to ensure the overall hull geometry
 * Is a piecewise-smooth C^1 curve
 *
 * Let the following conventions by applied for dimensioning purposes for language consistency
 *
 *         7 Z
 *       /
 *     /
 *    +-----------> X
 *   |               Length
 *   |         <------------------->
 *   V Y        ________-------‾‾‾‾|
 *         |   | \                |
 *         |   |  |`----......___|      7 Width
 *         |   𝘓_|______------‾‾|     /
 *         |   \|              |    /
 *  Height V    `----......___|   /
 *
 * "Length" refers to the x direction (left / right)
 * "Height" refers to the y direction (up / down) (defined by sideProfileCurve)
 * "Width" refers to the z direction (into / out of the screen) (defined by topProfileCurve)
 * "Thickness" refers to the normal direction of a surface to provide thickness to (+/- orientation is context dependent)
 */
@Getter @Setter
public class HullSection extends Section
{
    private StringableUnivariateFunction sideProfileCurve;
    private StringableUnivariateFunction topProfileCurve;
    private double wallsThickness;
    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    private boolean hasBulkhead;
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    // Adjusts for difference in area of the section's curvature of the front profile view at a given height h
    // See: https://www.desmos.com/calculator/mroxy1kc2t
    // TLDR: Uses the front profile of 2024's Shark Bait as a rough approximation for all reasonable future front profiles
    // Extrapolation occurs at heights greater than Shark Bait's max height of 0.4m
    @JsonIgnore
    private final Function<Double, Double> crossSectionalAreaAdjustmentFactorFunction = h -> {
        if (0 <= h && h < 0.0404)
            return 12 * h;
        else if (0.0404 <= h && h < 0.15)
            return -18.1233 * Math.pow((h - 0.15), 2) + 0.7025;
        else if (0.15 <= h && h < 0.4)
            return -1.50222 * Math.pow((h - 0.45), 2) + 0.8377;
        else if (h >= 0.4) // Extrapolation
            return (0.9 / (1 + Math.exp(-7 * (h - 0.05)))) + 0.005439;
        else
            return 0.0;
    };

    /**
     * @param start the start x position of the section's profile curve interval
     * @param end the end x position of the section's profile curve interval
     * @param hasBulkhead whether the empty space between the inner hull walls should be filled with a styrofoam bulkhead
     * @param sideProfileCurve the function that defines the shape of the hull in this section in the xy-plane bounded above by y = 0
     * @param topProfileCurve the function that defines half the shape of the hull in the xz-plane bounded below by y = 0 (reflects across the line x = 0 to give the other half)
     */
    public HullSection(StringableUnivariateFunction sideProfileCurve, StringableUnivariateFunction topProfileCurve, double start, double end, double thickness, boolean hasBulkhead) {
        super(start, end);
        validateSign(sideProfileCurve.getFunction(), false);
        validateSign(topProfileCurve.getFunction(), true);
        this.sideProfileCurve = sideProfileCurve;
        this.topProfileCurve = topProfileCurve;
        this.wallsThickness = thickness;
        this.hasBulkhead = hasBulkhead;
    }

    /**
     * Basic constructor for a simple model, useful before the user defines geometry / material properties
     * Models the hull as a rectangular prism
     * @param length the length of the hull
     * @param height the constant height of the hull
     * @param width the constant width of the hull
     */
    public HullSection(double length, double height, double width) {
        super(0, length);
        this.sideProfileCurve = new VertexFormParabola(0, 0, -height);
        this.topProfileCurve = new VertexFormParabola(0, 0, width / 2.0);
    }

    /**
     * Defines a function A(x) which models the cross-sectional area of the canoe as a function of length x
     * A(x) for a given x is the area of the cross-sectional area element with thickness dx
     * At the moment (until a frontProfileCurve is defined in the model) area elements are rectangles
     * @return the function A(x)
     */
    @JsonIgnore
    public UnivariateFunction getCrossSectionalAreaFunction() {
        return x -> {
            double height = Math.abs(sideProfileCurve.value(x));
            double width = 2 * Math.abs(topProfileCurve.value(x)); // assuming this profile is symmetrical in the current model
            return height * width * crossSectionalAreaAdjustmentFactorFunction.apply(height);
        };
    }

    /**
     * @return the integral of the x-sectional area over the section's length
     */
    @JsonIgnore
    public double getVolume() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getCrossSectionalAreaFunction(), x, rx);
    }

    /**
     * Defines a function A_inner(x) which models the cross-sectional area interior of the canoe as a function of length x
     * In essence, this is the inside section of the canoe hollowed to create hull walls of the specified hullThickness of this section
     * @return the function A_inner(x)
     */
    @JsonIgnore
    public UnivariateFunction getInnerCrossSectionalAreaFunction() {
        return x -> {
            double height = Math.abs(sideProfileCurve.value(x));
            double width = 2 * Math.abs(topProfileCurve.value(x)); // assuming this profile is symmetrical in the current model
            int numTopAndBottomWalls = hasBulkhead ? 2 : 1; // Include a top wall (ceiling) to cover the bulkhead (in addition to floor which is always present)
            return ((height - numTopAndBottomWalls * wallsThickness) * (width - (2 * wallsThickness))) * crossSectionalAreaAdjustmentFactorFunction.apply(height); // inner area doesnt include walls or floor / ceiling
        };
    }

    /**
     * @return the integral of the x-sectional bulkhead area over the section's length
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        return hasBulkhead ? CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getInnerCrossSectionalAreaFunction(), x, rx) : 0;
    }

    /**
     * Defines a function A_concrete(x) which models the cross-sectional area of the hollowed canoe as a function of length x
     * A_hollowed(x) for a given x is the area of the cross-sectional area element excluding inner volume (that may or may not be occupied by bulkhead material)
     * @return the function A_concrete(x)
     */
    @JsonIgnore
    public UnivariateFunction getConcreteCrossSectionalAreaFunction() {
        return x -> getCrossSectionalAreaFunction().value(x) - getInnerCrossSectionalAreaFunction().value(x);
    }

    /**
     * @return the integral of the x-sectional concrete area over the section's length
     */
    @JsonIgnore
    public double getConcreteVolume() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getConcreteCrossSectionalAreaFunction(), x, rx);
    }

    /**
     * Defines a function m(x) which models the mass as a function of length x
     * md(x) for a given x is the mass of the x-sectional area element dA (comprises concrete and bulkhead if applicable)
     * @return the function A_concrete(x)
     */
    @JsonIgnore
    public UnivariateFunction getMassDistributionFunction() {
        return hasBulkhead ? x -> getConcreteCrossSectionalAreaFunction().value(x) * concreteDensity + getInnerCrossSectionalAreaFunction().value(x) * bulkheadDensity
                : x -> getConcreteCrossSectionalAreaFunction().value(x) * concreteDensity;
    }

    /**
     * @return the integral of the mass distribution over the section's length
     */
    @JsonIgnore
    public double getMass() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getMassDistributionFunction(), x, rx);
    }

    /**
     * Defines a function w(x) which models the load of the hull section along its length (negative for download load)
     * @return the function w(x)
     */
    @JsonIgnore
    public ContinuousLoadDistribution getWeightDistributionFunction() {
        UnivariateFunction distribution = x -> -getMassDistributionFunction().value(x) * PhysicalConstants.GRAVITY.getValue() / 1000.0;
        return new ContinuousLoadDistribution("Section Weight", distribution, new Section(x, rx));
    }

    /**
     * @return the integral of the weight distribution over the section's length
     */
    @JsonIgnore
    public double getWeight() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getWeightDistributionFunction().getDistribution(), x, rx);
    }

    /**
     * @return the maximum width of the hull section based on the top profile curve
     */
    @JsonIgnore
    public double getMaxWidth() {
        UnivariateFunction topProfileFunction = topProfileCurve;
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(topProfileFunction);
        SearchInterval searchInterval = new SearchInterval(x, rx);
        return 2 * optimizer.optimize(MaxEval.unlimited(), objectiveFunction, searchInterval).getValue();
    }

    /**
     * Validates that the hull shape function is non-positive on its domain [start, end]
     * This convention allows waterline height y = h (downward is +y)
     * Note that this means the topmost point of the hull on the y-axis is y = 0
     */
    private void validateSign(Function<Double, Double> profileCurve, boolean positive)
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

        // Validate the extreme value based on the sign
        if (positive && maxValue < 0)
            throw new IllegalArgumentException("Hull shape function must be positive on its domain [start, end]");
        else if (!positive && maxValue > 0)
            throw new IllegalArgumentException("Hull shape function must be non-positive on its domain [start, end]");
    }
}