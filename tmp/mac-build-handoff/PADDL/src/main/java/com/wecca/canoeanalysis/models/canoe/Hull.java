package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.aop.TraceIgnore;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.load.DiscreteLoadDistribution;
import com.wecca.canoeanalysis.models.load.LoadType;
import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import com.wecca.canoeanalysis.utils.SectionPropertyMapEntry;
import com.wecca.canoeanalysis.utils.HullLibrary;
import javafx.geometry.Point2D;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * The Hull class represents a canoe’s hull using a new model that decouples the side‐view and top‐view
 * curve segments from the legacy HullSection concept. In this design, the hull is modeled as a composite
 * of discrete sections—each defined by a pair of Bézier curves (one for the side view and one for the top view)
 * that capture the canoe’s vertical profile and width respectively.
 * -----------------------------------------------------------------------------------------------------------------
 * The following are conventions which should be used for dimensioning (see diagram of a section of the hull):
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
 * -----------------------------------------------------------------------------------------------------------------
 * - "Length" refers to the x-direction (left/right).
 * - "Height" refers to the y-direction (up/down) and is defined by the side profile curve.
 * - "Width" refers to the z-direction (into/out of the screen) and is defined by the top profile curve.
 * - "Thickness" refers to the wall thickness (or floor thickness) measured normal to the surface.
 * -----------------------------------------------------------------------------------------------------------------
 * In the new model:
 * - The sideViewSegments provide the vertical profile of the hull.
 * - The topViewSegments define the bottom half of the horizontal profile. the top half is always a symmetrical reflection about y = 0
 * -----------------------------------------------------------------------------------------------------------------
 * The overall hull geometry is constructed by stitching together these segments into a piecewise-smooth C1
 * continuous curve called a spline. This is also called a bezier spline, as the curves we use here are special parametric curves called Bézier curves
 * Utility methods are provided to compute physical properties such as volume, mass, and self‐weight
 * by integrating across the hull’s sections.
 */
@Getter @Setter @EqualsAndHashCode @Traceable
public class Hull {

    @JsonProperty("concreteDensity")
    private double concreteDensity;
    @JsonProperty("bulkheadDensity")
    private double bulkheadDensity;
    @JsonProperty("hullProperties")
    private HullProperties hullProperties;
    @JsonProperty("sideViewSegments")
    private List<CubicBezierFunction> sideViewSegments;
    @JsonProperty("topViewSegments")
    private List<CubicBezierFunction> topViewSegments;

    /**
     * Note: Was "lifted" up in the inheritance tree from HullSection in the old model
     * Adjusts for difference in area of the section's curvature of the front profile view at a given height h
     * See: https://www.desmos.com/calculator/9ookcwgenx
     * TLDR: Uses the front profile of 2024's Shark Bait as a rough approximation for all reasonable future front profiles
     * The hull curve is scaled based on the length of the user's hull design compared to Shark Bait
     * -----------------------------------------------------------------------------------------------------------------
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
     * The new model's constructor for serialization and storage
     * @param concreteDensity  the uniform concrete destiny of the hull
     * @param bulkheadDensity the uniform bulkhead density across all bulkhead material
     * @param hullProperties the hull properties (including densities and section maps)
     * @param sideViewSegments the side-view Bézier curves
     * @param topViewSegments the top-view Bézier curves
     */
    @JsonCreator
    public Hull(@JsonProperty("concreteDensity") double concreteDensity,
                @JsonProperty("bulkheadDensity") double bulkheadDensity,
                @JsonProperty("hullProperties") HullProperties hullProperties,
                @JsonProperty("sideViewSegments") List<CubicBezierFunction> sideViewSegments,
                @JsonProperty("topViewSegments") List<CubicBezierFunction> topViewSegments) {
        this.hullProperties = hullProperties;
        this.sideViewSegments = sideViewSegments;
        this.topViewSegments = topViewSegments;
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;

        // Validators
        validateBasicValues();
        validateMaps();
        validateFloorThickness();
        validateWallThickness();
        validateC1Continuity();
    }

    /**
     * Convenient constructor for uniform thickness and standard bulkheads (bulkheaded edge sections).
     * @param concreteDensity the concrete density
     * @param bulkheadDensity the bulkhead density
     * @param sideView the side-view Bézier curves
     * @param topView the top-view Bézier curves
     * @param thickness the uniform wall thickness
     */
    public Hull(double concreteDensity, double bulkheadDensity, List<CubicBezierFunction> sideView,
                List<CubicBezierFunction> topView, double thickness) {
        this(concreteDensity, bulkheadDensity, new HullProperties(
                HullLibrary.buildUniformThicknessList(sideView, thickness), HullLibrary.buildDefaultBulkheadList(sideView)), sideView, topView);
    }

    /**
     * Basic constructor for a simple model, useful before the user defines detailed geometry/material properties.
     * Models the hull as a rectangular prism (beam) using a single side–view and a single top–view Bézier segment.
     * The side–view curve is nearly constant at –height and the top–view curve is nearly constant at width/2.
     * Control points are offset by ε = 0.001 so that the curve is a numerical approximation of a step function.
     * @param length the length of the hull
     * @param height the constant height (depth) of the hull
     * @param width  the overall width of the hull (used to compute the top–view curve at width/2)
     * @param thickness the uniform thickness of the hull in metres
     */
    public Hull(double length, double height, double width, double thickness) {
        this(
                0, // concreteDensity
                0, // bulkheadDensity
                new HullProperties(
                        HullLibrary.buildUniformThicknessList(
                                List.of(
                                        new CubicBezierFunction(
                                                0, -height,                 // start point (x1, y1)
                                                0.001, -height,             // control point 1 (offset by ε)
                                                length - 0.001, -height,      // control point 2 (offset by ε)
                                                length, -height             // end point (x2, y2)
                                        )
                                ),
                                thickness
                        ),
                        HullLibrary.buildDefaultBulkheadList(
                                List.of(
                                        new CubicBezierFunction(
                                                0, -height,
                                                0.001, -height,
                                                length - 0.001, -height,
                                                length, -height
                                        )
                                )
                        )
                ),
                List.of(
                        new CubicBezierFunction(
                                0, -height,
                                0.001, -height,
                                length - 0.001, -height,
                                length, -height
                        )
                ),
                List.of(
                        new CubicBezierFunction(
                                0, width / 2.0,
                                0.001, width / 2.0,
                                length - 0.001, width / 2.0,
                                length, width / 2.0
                        )
                )
        );
    }

    /**
     * Copy constructor for deep cloning.
     * @param src the source Hull to copy
     */
    public Hull(Hull src) {
        this.concreteDensity  = src.concreteDensity;
        this.bulkheadDensity  = src.bulkheadDensity;
        this.hullProperties   = new HullProperties(src.getHullProperties());
        this.sideViewSegments = src.sideViewSegments
                .stream()
                .map(CubicBezierFunction::new)
                .collect(Collectors.toCollection(ArrayList::new));
        this.topViewSegments = src.topViewSegments
                .stream()
                .map(CubicBezierFunction::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Validates basic non-null and size conditions for hull properties and curve segments.
     */
    private void validateBasicValues() {
        // Check for null in any list
        if (Stream.of(hullProperties.getThicknessMap(), hullProperties.getBulkheadMap(), sideViewSegments, topViewSegments).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("sideView, topView, thicknessList, and bulkheadList must not be null");
        if (hullProperties.getThicknessMap().isEmpty() || hullProperties.getBulkheadMap().isEmpty())
            throw new IllegalArgumentException("There must be at least one section in the side view");
        if (sideViewSegments.size() != topViewSegments.size() || hullProperties.getThicknessMap().size() != sideViewSegments.size() || hullProperties.getBulkheadMap().size() != sideViewSegments.size())
            throw new IllegalArgumentException("sideView, topView, thicknessList, and bulkheadList must have the same number of elements");
    }

    /**
     * Validates that the thickness and bulkhead maps define the same sections and match the boundaries of the side view curves.
     */
    private void validateMaps() {
        if (!IntStream.range(0, hullProperties.getThicknessMap().size())
                .allMatch(i -> {
                    Section s1 = hullProperties.getThicknessMap().get(i);
                    Section s2 = hullProperties.getBulkheadMap().get(i);
                    return s1.getX() == s2.getX() && s1.getRx() == s2.getRx();
                }))
            throw new IllegalArgumentException("Thickness list and bulkhead list do not describe the same sections");

        // Validate that each section matches the corresponding side view curve boundaries.
        IntStream.range(0, hullProperties.getThicknessMap().size())
                .forEach(i -> {
                    Section s = hullProperties.getThicknessMap().get(i);
                    CubicBezierFunction side = sideViewSegments.get(i);
                    if (Math.abs(side.getX1() - s.getX()) > 1e-3 || Math.abs(side.getX2() - s.getRx()) > 1e-3)
                        throw new IllegalArgumentException(String.format(
                                "Section [%.3f, %.3f] does not match side view boundaries [%.3f, %.3f].",
                                s.getX(), s.getRx(), side.getX1(), side.getX2()));
                });
    }

    /**
     * Validates that the floor thickness (i.e. the wall thickness, which is used as floor thickness)
     * does not exceed 25% of the canoe's maximum height.
     * In the new model, wall thickness is defined in the thickness map inside hullProperties.
     */
    private void validateFloorThickness() {
        double canoeHeight = getMaxHeight();
        hullProperties.getThicknessMap().forEach(entry -> {
            double thickness = Double.parseDouble(entry.getValue());
            if (thickness > canoeHeight / 4) {
                throw new IllegalArgumentException("Hull floor thickness must not exceed 1/4 of the canoe's max height");
            }
        });
    }

    /**
     * Validates that the side–view Bézier segments cover a continuous sub-interval of R^+ with C¹ continuity.
     * This method checks that:
     *  - The first segment starts at x = 0.
     *  - Adjacent segments have matching endpoints (x and y).
     *  - The slopes at the junction (computed from the endpoint control point of the current segment and
     *    the starting control point of the next segment) match within a small tolerance.
     * Note this is an "upgrade" from the old models validateNoSectionGaps which only checks C0 (C1 includes C0 too!)
     */
    private void validateC1Continuity() {
        List<CubicBezierFunction> segments = this.sideViewSegments;
        double tol = 1e-6;
        // Ensure the first segment starts at x = 0.
        if (Math.abs(segments.getFirst().getX1()) > tol)
            throw new IllegalArgumentException("The hull should start at x = 0");

        for (int i = 0; i < segments.size() - 1; i++) {
            CubicBezierFunction current = segments.get(i);
            CubicBezierFunction next = segments.get(i + 1);

            // Check that the x-coordinate of current segment's end equals the next segment's start.
            if (Math.abs(current.getX2() - next.getX1()) > tol)
                throw new IllegalArgumentException("Hull segments do not join continuously in x: "
                        + current.getX2() + " vs " + next.getX1());

            // Check that the y-values at the boundary are continuous.
            double currentEndY = current.value(current.getX2());
            double nextStartY = next.value(next.getX1());
            if (Math.abs(currentEndY - nextStartY) > tol)
                throw new IllegalArgumentException("Hull segments do not join continuously in y: "
                        + currentEndY + " vs " + nextStartY);

            // Compute the slope at the end of the current segment.
            double currentSlope = CalculusUtils.computeSlope(current.getControlX2(), current.getControlY2(),
                    current.getX2(), current.getY2(), tol);
            // Compute the slope at the start of the next segment.
            double nextSlope = CalculusUtils.computeSlope(next.getX1(), next.getY1(),
                    next.getControlX1(), next.getControlY1(), tol);
            if (Math.abs(currentSlope - nextSlope) > tol)
                throw new IllegalArgumentException("Hull segments do not join with C¹ continuity: slopes "
                        + currentSlope + " vs " + nextSlope);
        }
    }

    /**
     * Validates that the hull walls (new model) do not overlap.
     * For each section defined in the thickness map, the wall thickness must not exceed half
     * of that section's width (computed from the corresponding top-view Bézier segment).
     */
    private void validateWallThickness() {
        double tol = 1e-6;
        List<SectionPropertyMapEntry> thicknessMap = hullProperties.getThicknessMap();
        for (int i = 0; i < thicknessMap.size(); i++) {
            SectionPropertyMapEntry entry = thicknessMap.get(i);
            double currentThickness = Double.parseDouble(entry.getValue());
            CubicBezierFunction topCurve = topViewSegments.get(i);
            UnivariateObjectiveFunction obj = new UnivariateObjectiveFunction(x -> Math.abs(topCurve.value(x)));
            double maxTop = new BrentOptimizer(1e-10, 1e-14)
                    .optimize(MaxEval.unlimited(), obj, new SearchInterval(topCurve.getX1(), topCurve.getX2()))
                    .getValue();
            double sectionWidth = 2 * maxTop;
            if (currentThickness > (sectionWidth / 2) + tol) {
                throw new IllegalArgumentException(String.format(
                        "Hull walls would be greater than the width of the canoe. Thickness: %.4f, Allowed max: %.4f",
                        currentThickness, sectionWidth / 2));
            }
        }
    }

    /**
     * Max canoe height Computed by finding the minimum y-value across all side-view segments (the lowest point), then converting it to a positive height value.
     * @return the maximum height (in meters) of the canoe.
     */
    @JsonIgnore
    public double getMaxHeight() {
        double globalMinY = 0;
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);

        // Check each bezier for it's min
        for (CubicBezierFunction bezier : sideViewSegments) {
            double start = bezier.getX1();
            double end = bezier.getX2();
            UnivariateObjectiveFunction objective = new UnivariateObjectiveFunction(x -> -bezier.value(x));
            SearchInterval interval = new SearchInterval(start, end);
            UnivariatePointValuePair result = optimizer.optimize(MaxEval.unlimited(), objective, interval);
            double segmentMinY = -result.getValue();

            // Check for and store the global min if found
            if (segmentMinY < globalMinY) globalMinY = segmentMinY;
        }

        // The maximum height is the absolute distance from 0 down to the lowest point.
        return CalculusUtils.roundXDecimalDigits(-globalMinY, 10);
    }

    /**
     * @return the length (in meters) of the canoe hull.
     */
    @JsonIgnore
    public double getLength() {
        if (sideViewSegments == null || sideViewSegments.isEmpty()) return 0;
        return sideViewSegments.getLast().getX2() - sideViewSegments.getFirst().getX1();
    }

    /**
     * @return a Section with start equal to the first segment's x1 and end equal to the last segment's x2.
     */
    @JsonIgnore
    public Section getSection() {
        if (sideViewSegments == null || sideViewSegments.isEmpty()) return null;
        return new Section(sideViewSegments.getFirst().getX1(), sideViewSegments.getLast().getX2());
    }

    /**
     * Returns the maximum width of the hull computed directly from the top-view segments.
     * For each top-view segment, it finds the maximum absolute value over its domain and doubles it,
     * then returns the largest width among all segments.
     * @return the maximum hull width.
     */
    @JsonIgnore
    public double getMaxWidth() {
        double maxWidth = 0;
        UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        for (CubicBezierFunction seg : topViewSegments) {
            double start = seg.getX1();
            double end = seg.getX2();
            UnivariateObjectiveFunction objective = new UnivariateObjectiveFunction(x -> Math.abs(seg.value(x)));
            double segMax = optimizer.optimize(MaxEval.unlimited(), objective, new SearchInterval(start, end)).getValue();
            double segWidth = 2 * segMax;
            if (segWidth > maxWidth) {
                maxWidth = segWidth;
            }
        }
        return maxWidth;
    }

    /**
     * @return the maximum wall thickness of the canoe,
     * computed directly from the thickness map in hullProperties.
     */
    @JsonIgnore
    public double getMaxThickness() {
        return hullProperties.getThicknessMap().stream()
                .mapToDouble(entry -> Double.parseDouble(entry.getValue()))
                .max()
                .orElse(0);
    }

    /**
     * Returns a function A(x) which models the cross-sectional area of the canoe as a function of length x.
     * where side(x) is obtained from the sideViewSegments, top(x) from topViewSegments,
     * and the adjustment factor accounts for the difference between the actual front profile
     * and its encasing rectangle.
     * @return the cross-sectional area function A(x)
     */
    @JsonIgnore @TraceIgnore
    public BoundedUnivariateFunction getCrossSectionalAreaFunction() {
        return x -> {
            double sideVal = Math.abs(CalculusUtils.getSplineY(sideViewSegments, x));
            double topVal  = 2 * Math.abs(CalculusUtils.getSplineY(topViewSegments, x));
            return sideVal * topVal * crossSectionalAreaAdjustmentFactorFunction.value(sideVal);
        };
    }

    /**
     * @return the total volume of the canoe by numerically integrating the cross-sectional area function A(x) over the full hull x-domain.
     */
    @JsonIgnore
    public double getTotalVolume() {
        Section full = getSection();
        return CalculusUtils.integrator.integrate(
                MaxEval.unlimited().getMaxEval(),
                getCrossSectionalAreaFunction(),
                full.getX(), full.getRx());
    }

    /**
     * Defines a function A_inner(x) which models the inner (cavity) cross-sectional area
     * of the canoe as a function of x. The inner area is computed by subtracting wall thickness
     * from both the height and width. For each x the wall thickness and bulkhead flag are
     * looked up from the hullProperties maps.
     * @return the function A_inner(x)
     */
    @JsonIgnore @TraceIgnore
    public BoundedUnivariateFunction getInnerCrossSectionalAreaFunction() {
        return x -> {
            double sideVal = Math.abs(CalculusUtils.getSplineY(sideViewSegments, x));
            double topVal  = 2 * Math.abs(CalculusUtils.getSplineY(topViewSegments, x));
            double t = hullProperties.getThicknessMap().stream()
                    .filter(entry -> entry.getX() <= x && x <= entry.getRx())
                    .findFirst()
                    .map(entry -> Double.parseDouble(entry.getValue()))
                    .orElseThrow(() -> new RuntimeException("No thickness entry for x = " + x));
            boolean fillBulkhead = hullProperties.getBulkheadMap().stream()
                    .filter(entry -> entry.getX() <= x && x <= entry.getRx())
                    .findFirst()
                    .map(entry -> Boolean.parseBoolean(entry.getValue()))
                    .orElse(false);
            int numWalls = fillBulkhead ? 2 : 1;
            double innerSide = Math.max(sideVal - numWalls * t, 0);
            double innerTop  = Math.max(topVal - 2 * t, 0);
            return innerSide * innerTop * crossSectionalAreaAdjustmentFactorFunction.value(sideVal);
        };
    }

    /**
     * @return the bulkhead volume of the canoe by integrating the inner cross-sectional area A_inner(x) over each interval specified in the bulkhead map that is flagged true.
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        double bulkVol = 0;
        for (SectionPropertyMapEntry entry : hullProperties.getBulkheadMap()) {
            if (Boolean.parseBoolean(entry.getValue())) {
                double xStart = entry.getX();
                double xEnd   = entry.getRx();
                bulkVol += CalculusUtils.integrator.integrate(
                        MaxEval.unlimited().getMaxEval(),
                        getInnerCrossSectionalAreaFunction(),
                        xStart, xEnd);
            }
        }
        return bulkVol;
    }

    /**
     * Defines a function A_concrete(x) which models the cross-sectional area of the concrete
     * (i.e. the hull walls) as a function of x. This is given by subtracting the inner (cavity) area from the outer area.
     * @return the function A_concrete(x)
     */
    @JsonIgnore @TraceIgnore
    public BoundedUnivariateFunction getConcreteCrossSectionalAreaFunction() {
        BoundedUnivariateFunction outer = getCrossSectionalAreaFunction();
        BoundedUnivariateFunction inner = getInnerCrossSectionalAreaFunction();
        return x -> outer.value(x) - inner.value(x);
    }

    /**
     * @return the total concrete volume of the canoe by integrating the concrete cross-sectional area function A_concrete(x) over the full hull x-domain.
     */
    @JsonIgnore
    public double getConcreteVolume() {
        Section full = getSection();
        return CalculusUtils.integrator.integrate(
                MaxEval.unlimited().getMaxEval(),
                getConcreteCrossSectionalAreaFunction(),
                full.getX(), full.getRx());
    }

    /**
     * Defines a function m(x) which models the mass distribution along the canoe (kg/m).
     * The mass per unit length is computed from the concrete area, and if a bulkhead is present
     * at x, the inner (cavity) mass (using bulkhead density) is also included.
     * @return the mass distribution function m(x)
     */
    @JsonIgnore @TraceIgnore
    public BoundedUnivariateFunction getMassDistributionFunction() {
        double concreteDens = concreteDensity;
        double bulkheadDens = bulkheadDensity;
        return x -> {
            double concreteMass = getConcreteCrossSectionalAreaFunction().value(x) * concreteDens;
            boolean fillBulkhead = hullProperties.getBulkheadMap().stream()
                    .filter(entry -> entry.getX() <= x && x <= entry.getRx())
                    .findFirst()
                    .map(entry -> Boolean.parseBoolean(entry.getValue()))
                    .orElse(false);
            if (fillBulkhead) {
                double bulkheadMass = getInnerCrossSectionalAreaFunction().value(x) * bulkheadDens;
                return concreteMass + bulkheadMass;
            } else return concreteMass;
        };
    }

    /**
     * @return the total mass of the canoe (in kg) by integrating the mass distribution m(x)over the full hull x-domain.
     */
    @JsonIgnore
    public double getMass() {
        Section full = getSection();
        return CalculusUtils.integrator.integrate(
                MaxEval.unlimited().getMaxEval(),
                getMassDistributionFunction(),
                full.getX(), full.getRx());
    }

    /**
     * @return a composite function representing the stitched-together side profile curves of all hull sections
     * Note that this returned function has been shifted so that it's bottom is at y = 0 instead of its top at y = 0
     */
    @JsonIgnore
    public BoundedUnivariateFunction getPiecedSideProfileCurveShiftedAboveYAxis() {
        List<BoundedUnivariateFunction> functions = sideViewSegments.stream().map(seg -> (BoundedUnivariateFunction) seg).toList();
        List<Section> sections = sideViewSegments.stream()
                .map(seg -> new Section(seg.getX1(), seg.getX2()))
                .toList();
        return CalculusUtils.createCompositeFunctionShiftedPositive(functions, sections, false);
    }

    /**
     * @return the self weight distribution
     */
    @JsonIgnore
    public PiecewiseContinuousLoadDistribution getSelfWeightDistribution() {
        return (getTotalVolume() == 0) ?  null : PiecewiseContinuousLoadDistribution.fromHull(this);
    }

    /**
     * @return the self weight distribution discretized into intervals based on length
     */
    @JsonIgnore
    public DiscreteLoadDistribution getSelfWeightDistributionDiscretized() {
        return DiscreteLoadDistribution.fromPiecewise(LoadType.HULL, getSelfWeightDistribution(), (int) (getSection().getLength() * 100));
    }

    /**
     * New method in the new model
     * Returns the mass (kg) over the specified section by integrating m(x).
     * Validates that section.getX() > 0 and section.getRx() < getLength().
     * @param section the section to integrate over.
     * @return the mass of the section.
     */
    public double getSectionVolume(Section section) {
        if (section.getX() < 0)
            throw new IllegalArgumentException("Section start x (" + section.getX() + ") must be > 0.");
        if (section.getRx() > getLength())
            throw new IllegalArgumentException("Section end x (" + section.getRx() + ") must be < hull length (" + getLength() + ").");
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(),
                getCrossSectionalAreaFunction(), section.getX(), section.getRx());
    }

    /**
     * New method in the new model
     * Returns the maximum height (in m) for the specified section by optimizing the side–view function.
     * Validates that section.getX() > 0 and section.getRx() < getLength().
     * @param section the section over which to determine the height.
     * @return the maximum height of the section.
     */
    @JsonIgnore
    public double getSectionSideViewCurveHeight(Section section) {
        if (section.getX() < 0)
            throw new IllegalArgumentException("Section start x (" + section.getX() + ") must be > 0.");
        if (section.getRx() > getLength())
            throw new IllegalArgumentException("Section end x (" + section.getRx() + ") must be < hull length (" + getLength() + ").");

        // Search for the side-view segment that covers x
        UnivariateObjectiveFunction objective = new UnivariateObjectiveFunction(x -> {
            for (CubicBezierFunction seg : sideViewSegments) {
                if (seg.getX1() <= x && x <= seg.getX2()) return -seg.value(x);
            }
            throw new IllegalArgumentException("x = " + x + " is out of bounds of the side view segments.");
        });

        SearchInterval interval = new SearchInterval(section.getX(), section.getRx());
        UnivariatePointValuePair result = new BrentOptimizer(1e-10, 1e-14).optimize(MaxEval.unlimited(), objective, interval);
        return result.getValue();
    }

    /**
     * New method in the new model
     * Returns the mass (kg) over the specified section by integrating m(x).
     * Validates that section.getX() > 0 and section.getRx() < getLength().
     * @param section the section to integrate over.
     * @return the mass of the section.
     */
    public double getSectionMass(Section section) {
        if (section.getX() < 0)
            throw new IllegalArgumentException("Section start x (" + section.getX() + ") must be > 0.");
        if (section.getRx() > getLength())
            throw new IllegalArgumentException("Section end x (" + section.getRx() + ") must be < hull length (" + getLength() + ").");
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(),
                getMassDistributionFunction(), section.getX(), section.getRx());
    }

    /**
     * Defines a function w(x) which models the load (weight) distribution along the canoe in kN/m.where g is the gravitational acceleration.
     * @return the weight distribution function w(x)
     */
    @JsonIgnore
    public BoundedUnivariateFunction getWeightDistributionFunction() {
        double g = PhysicalConstants.GRAVITY.getValue();
        return x -> -getMassDistributionFunction().value(x) * g / 1000.0;
    }

    /**
     * Returns the total self-weight of the canoe (in kN) by integrating the weight distribution w(x)
     * over the full hull x-domain. The negative sign indicates a downward load.
     * @return the total weight.
     */
    @JsonIgnore
    public double getWeight() {
        Section full = getSection();
        return CalculusUtils.integrator.integrate(
                MaxEval.unlimited().getMaxEval(),
                getWeightDistributionFunction(),
                full.getX(), full.getRx());
    }

    /**
     * @return a lst of all the knot points in the hull
     */
    @JsonIgnore
    public List<Point2D> getSideViewKnotPoints() {
        List<Point2D> knots = new ArrayList<>();
        for (CubicBezierFunction segment : sideViewSegments) {
            knots.add(new Point2D(segment.getX1(), segment.getY1()));
        }
        CubicBezierFunction lastSegment = sideViewSegments.getLast();
        knots.add(new Point2D(lastSegment.getX2(), lastSegment.getY2()));
        return knots;
    }
}