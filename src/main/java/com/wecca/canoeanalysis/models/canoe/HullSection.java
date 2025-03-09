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
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.HullLibrary;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;

/**
 * @deprecated legacy code kept as a reference to the old HullSection model.
 * This is for a clearer picture of previous development for newer developers who may join in the future
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

    @JsonIgnore @EqualsAndHashCode.Exclude
    private final BoundedUnivariateFunction crossSectionalAreaAdjustmentFactorFunction = h -> {
        double[] coefficients = new double[] {0, 17.771, -210.367, 1409.91, -5420.6, 11769.4, -13242.7, 5880.62};
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = coefficients[i] / Math.pow(HullLibrary.scalingFactor, i);
        }
        PolynomialFunction regressionFit = new PolynomialFunction(coefficients);
        if (0 <= h && h <= 0.4 * HullLibrary.scalingFactor)
            return regressionFit.value(h);
        else if (h > 0.4 * HullLibrary.scalingFactor)
            return regressionFit.value(0.4 * HullLibrary.scalingFactor);
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
     * @deprecated since the Hull counterpart constructor that calls this was deleted as the new model used the name method signature
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
    @JsonIgnore
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