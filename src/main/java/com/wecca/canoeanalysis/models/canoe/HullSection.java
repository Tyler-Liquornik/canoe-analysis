package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.wecca.canoeanalysis.models.load.ContinuousLoadDistribution;
import com.wecca.canoeanalysis.models.load.LoadType;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.function.VertexFormParabola;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
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
@Getter @Setter @EqualsAndHashCode(callSuper = true)
public class HullSection extends Section
{
    @JsonProperty("sideProfileCurve")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VertexFormParabola.class, name = "VertexFormParabola")
    })
    private UnivariateFunction sideProfileCurve;
    @JsonProperty("topProfileCurve")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VertexFormParabola.class, name = "VertexFormParabola")
    })
    private UnivariateFunction topProfileCurve;
    @JsonProperty("thickness")
    private double thickness;
    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    @JsonProperty("isFilledBulkhead")
    private boolean isFilledBulkhead;
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    /**
     * Adjusts for difference in area of the section's curvature of the front profile view at a given height h
     * See: https://www.desmos.com/calculator/9ookcwgenx
     * TLDR: Uses the front profile of 2024's Shark Bait as a rough approximation for all reasonable future front profiles
     * The hull curve is scaled based on the length of the user's hull design compared to Shark Bait
     *
     * This function encodes a relationship on the difference between a hull front profile, and it's encasing rectangle
     * This idea is then extended into a function of waterline height h to get an integral defined function
     * This is inefficient because this function is called for every h in the iterative floating case solution algorithm
     * Instead I have built a polynomial regression fit for it which is faster to evaluate
     * The 7th degree polynomial has R^2 = 0.9967 for the fully accurate function and was solved on Desmos
     */
    @JsonIgnore
    private final Function<Double, Double> crossSectionalAreaAdjustmentFactorFunction = h -> {
        double[] coefficients = new double[] {0, 17.771, -210.367, 1409.91, -5420.6, 11769.4, -13242.7, 5880.62};
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = coefficients[i] / Math.pow(SharkBaitHullLibrary.scalingFactor, i);
        }
        PolynomialFunction regressionFit = new PolynomialFunction(coefficients);
        if (0 <= h && h <= 0.4 * SharkBaitHullLibrary.scalingFactor)
            return regressionFit.value(h);
        else if (h > 0.4 * SharkBaitHullLibrary.scalingFactor)
            return regressionFit.value(0.4 * SharkBaitHullLibrary.scalingFactor);
        else
            throw new IllegalArgumentException("Function undefined for negative values");
    };

    /**
     * @param x the start x position of the section's profile curve interval
     * @param rx the end x position of the section's profile curve interval
     * @param hasBulkhead whether the empty space between the inner hull walls should be filled with a styrofoam bulkhead
     * @param sideProfileCurve the function that defines the shape of the hull in this section in the xy-plane bounded above by y = 0
     * @param topProfileCurve the function that defines half the shape of the hull in the xz-plane bounded below by y = 0 (reflects across the line x = 0 to give the other half)
     */
    public HullSection(@JsonProperty("sideProfileCurve") UnivariateFunction sideProfileCurve,
                       @JsonProperty("topProfileCurve") UnivariateFunction topProfileCurve,
                       @JsonProperty("x") double x,
                       @JsonProperty("rx") double rx,
                       @JsonProperty("thickness") double thickness,
                       @JsonProperty("hasBulkhead") boolean hasBulkhead) {
        super(x, rx);
        validateSign(sideProfileCurve::value, false);
        validateSign(topProfileCurve::value, true);
        this.sideProfileCurve = sideProfileCurve;
        this.topProfileCurve = topProfileCurve;
        this.thickness = thickness;
        this.isFilledBulkhead = hasBulkhead;
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
            int numTopAndBottomWalls = isFilledBulkhead ? 2 : 1; // Include a top wall (ceiling) to cover the bulkhead (in addition to floor which is always present)
            return ((height - numTopAndBottomWalls * thickness) * (width - (2 * thickness))) * crossSectionalAreaAdjustmentFactorFunction.apply(height); // inner area doesnt include walls or floor / ceiling
        };
    }

    /**
     * @return the integral of the x-sectional bulkhead area over the section's length
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        return isFilledBulkhead ? CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getInnerCrossSectionalAreaFunction(), x, rx) : 0;
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
        return isFilledBulkhead ? x -> getConcreteCrossSectionalAreaFunction().value(x) * concreteDensity + getInnerCrossSectionalAreaFunction().value(x) * bulkheadDensity
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
        return new ContinuousLoadDistribution(LoadType.DISCRETE_SECTION, distribution, new Section(x, rx));
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