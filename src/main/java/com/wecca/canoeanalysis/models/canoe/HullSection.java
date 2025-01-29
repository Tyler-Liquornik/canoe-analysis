package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.load.ContinuousLoadDistribution;
import com.wecca.canoeanalysis.models.load.LoadType;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.function.VertexFormParabolaFunction;
import com.wecca.canoeanalysis.services.LoggerService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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
            @JsonSubTypes.Type(value = VertexFormParabolaFunction.class, name = "VertexFormParabola"),
            @JsonSubTypes.Type(value = CubicBezierFunction.class, name = "CubicBezierFunction")
    })
    private BoundedUnivariateFunction sideProfileCurve;
    @JsonProperty("topProfileCurve")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VertexFormParabolaFunction.class, name = "VertexFormParabola"),
            @JsonSubTypes.Type(value = CubicBezierFunction.class, name = "CubicBezierFunction")
    })
    private BoundedUnivariateFunction topProfileCurve;
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
    @JsonIgnore @EqualsAndHashCode.Exclude
    private final BoundedUnivariateFunction crossSectionalAreaAdjustmentFactorFunction = h -> {
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
    public HullSection(@JsonProperty("sideProfileCurve") BoundedUnivariateFunction sideProfileCurve,
                       @JsonProperty("topProfileCurve") BoundedUnivariateFunction topProfileCurve,
                       @JsonProperty("x") double x,
                       @JsonProperty("rx") double rx,
                       @JsonProperty("thickness") double thickness,
                       @JsonProperty("hasBulkhead") boolean hasBulkhead) {
        super(x, rx);
        validateSign(sideProfileCurve, false);
        validateSign(topProfileCurve, null);
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
        this.sideProfileCurve = new VertexFormParabolaFunction(0, 0, -height);
        this.topProfileCurve = new VertexFormParabolaFunction(0, 0, width / 2.0);
    }

    /**
     * Defines a function A(x) which models the cross-sectional area of the canoe as a function of length x
     * A(x) for a given x is the area of the cross-sectional area element with thickness dx
     * An adjustment factor is applied to take into account the geometry of the front profile
     * The adjustment factor adjusts for the difference in area between the front profile and its encasing rectangle
     * @return the function A(x)
     */
    @JsonIgnore
    public BoundedUnivariateFunction getCrossSectionalAreaFunction() {
        return x -> {
            double height = Math.abs(sideProfileCurve.value(x));
            double width = 2 * Math.abs(topProfileCurve.value(x)); // assuming this profile is symmetrical in the current model
            return height * width * crossSectionalAreaAdjustmentFactorFunction.value(height);
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
    public BoundedUnivariateFunction getInnerCrossSectionalAreaFunction() {
        return x -> {
            double height = Math.abs(sideProfileCurve.value(x));
            double width = 2 * Math.abs(topProfileCurve.value(x)); // assuming this profile is symmetrical in the current model
            int numTopAndBottomWalls = isFilledBulkhead ? 2 : 1; // Include a top wall (ceiling) to cover the bulkhead (in addition to floor which is always present)
            return ((height - numTopAndBottomWalls * thickness) * (width - (2 * thickness))) * crossSectionalAreaAdjustmentFactorFunction.value(height); // inner area doesnt include walls or floor / ceiling
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
    public BoundedUnivariateFunction getConcreteCrossSectionalAreaFunction() {
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
     * m(x) for a given x is the mass of the x-sectional area element dA (comprises concrete and bulkhead if applicable)
     * @return the function m(x)
     */
    @JsonIgnore
    public BoundedUnivariateFunction getMassDistributionFunction() {
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
        BoundedUnivariateFunction distribution = x -> -getMassDistributionFunction().value(x) * PhysicalConstants.GRAVITY.getValue() / 1000.0;
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
    public double getMaxWidth() {
        BoundedUnivariateFunction absTopProfileFunction = x -> Math.abs(topProfileCurve.value(x));
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(absTopProfileFunction);
        SearchInterval searchInterval = new SearchInterval(x, rx);
        return 2 * optimizer.optimize(MaxEval.unlimited(), objectiveFunction, searchInterval).getValue();
    }

    @JsonIgnore
    public double getHeight()
    {
        // Find the sections minimum
        // Function is negated as BrentOptimizer looks for the maximum
        UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(x -> -this.sideProfileCurve.value(x));
        SearchInterval searchInterval = new SearchInterval(this.getX(), this.getRx());
        UnivariatePointValuePair result = (new BrentOptimizer(1e-10, 1e-14)).optimize(
                MaxEval.unlimited(),
                objectiveFunction,
                searchInterval
        );

        return result.getValue();

    }

    /**
     * Validates that the hull shape function does not cross zero beyond a small threshold.
     * If `expectPositive == true`, function must be non-negative (can touch zero but not go below `-1e-6`).
     * If `expectPositive == false`, function must be non-positive (can touch zero but not go above `1e-6`).
     * If `expectPositive == null`, function can be positive or negative, but **must not cross zero by more than `1e-6`**.
     */
    private void validateSign(BoundedUnivariateFunction curve, Boolean expectPositive) {
        double maxVal = curve.getMaxValue(new Section(x, rx));
        double minVal = curve.getMinValue(new Section(x, rx));
        if (expectPositive == null) {
            if (minVal < -1e-6 && maxVal > 1e-6) throw new IllegalArgumentException("Curve crosses zero by more than ±1e-6.");
        } else if (expectPositive) {
            if (minVal < -1e-6) throw new IllegalArgumentException("Curve crosses below zero by more than 1e-6.");
        } else {
            if (maxVal > 1e-6) throw new IllegalArgumentException("Curve crosses above zero by more than 1e-6.");
        }
    }
}