package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.load.DiscreteLoadDistribution;
import com.wecca.canoeanalysis.models.load.LoadType;
import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import com.wecca.canoeanalysis.services.HullGeometryService;
import java.util.*;
import java.util.stream.IntStream;

/**
 * You may notice there is a new hull model/API built atop the old one
 * The goal was to decouple the side view curve segments from the front view
 * All while ensuring backwards compatability
 * Particularly the x-coordinates of their knot points.
 * The problem this solved was that this allows the user to edit the knot points in the side view without disrupting the top view geometry.
 */
@Getter @Setter @EqualsAndHashCode
public class Hull {

    @JsonProperty("concreteDensity")
    private double concreteDensity; // [kg/m^3]
    @JsonProperty("bulkheadDensity")
    private double bulkheadDensity; // [kg/m^3]

    // Old API, this is the preferred data model for serializing and storing hull data
    // The new constructor will still compute and store this model of the data, thus this field acts as a cache in that case
    // When using the new model and computing this rather than passing in into the constructor there is an important caveat,
    // the computation may produce a geometry that is very much valid but with crazy numbers for the control points
    // which is computed from this::computeHullSection to convert from this form of the model.
    // When in this state, this not meant to display in the UI (not a valid model state for the user to manipulate)
    // it's only meant for internal use for calculations.
    // The possibility of these cases arises because the new model holds valid for more geometries since the side and top view are decoupled.
    @JsonProperty("hullSections")
    private List<HullSection> hullSections;

    // This extra flag will be serialized
    // it can be used as a quick check for which model was used,
    // so you can see right away if using the new model and hullSections geometry may not be displayable
    @JsonProperty("isOldModel")
    private boolean isOldModel;

    // New API used to construct hullSections but not serialized.
    // TODO when using the old API, we map also want to cache the data in this version of the it
    // TODO build CubicBezierSpline to wrap List<CubicBezierFunction> and associated validations
    @JsonIgnore
    private Map<Section, Double> thicknessMap;
    @JsonIgnore
    private Map<Section, Boolean> bulkheadMap;
    @JsonIgnore
    private List<CubicBezierFunction> sideViewSegments;
    @JsonIgnore
    private List<CubicBezierFunction> topViewSegments;

    /**
     * This is the new Hull API, created with the goal of decoupling the side view from the top view
     * It is intended to build upon the old API, and both should work interchangeably ( I hope D: )
     * @param concreteDensity the density of concrete, automatically set to be uniform across the hull
     * @param bulkheadDensity the density of the bulkheads (typically styrofoam), automatically set to be uniform across the hull
     * @param sideView a list of curves that makeup the spline for the side view
     * @param topView a list of curves that makeup the spline for the top view
     * @param thicknessMap a map of <[x, rx] : thickness> to describe the thickness per section similar to the old API
     */
    public Hull(double concreteDensity, double bulkheadDensity,
                List<CubicBezierFunction> sideView, List<CubicBezierFunction> topView,
                Map<Section, Double> thicknessMap, Map<Section, Boolean> bulkheadMap) {
        // Set fields
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.isOldModel = false;
        this.thicknessMap = thicknessMap;
        this.bulkheadMap = bulkheadMap;
        this.sideViewSegments = sideView;
        this.topViewSegments = topView;
        this.hullSections = getHullSectionsReformedFromDeCasteljaus();

        // Validate that the new API fields
        validateBasicValues(thicknessMap, bulkheadMap, sideView, topView);
        validateMaps(thicknessMap, bulkheadMap, sideView);

        // Convert the new model to the old model and validate it
        validateBasicValuesOld(concreteDensity, bulkheadDensity, hullSections);
        validateNoSectionGaps(hullSections);
        validateFloorThickness(hullSections);
        validateWallThickness(hullSections);

        // Set densities in each section.
        for (HullSection section : hullSections) {
            section.setConcreteDensity(concreteDensity);
            section.setBulkheadDensity(bulkheadDensity);
        }
    }

    /**
     * A more convenient version of the fully detailed new API constructor with uniform hull thickness and bulkheads preset on the first and last sections
     * @param thickness the uniform hull wall thickness
     */
    public Hull(double concreteDensity, double bulkheadDensity, List<CubicBezierFunction> sideView, List<CubicBezierFunction> topView, double thickness) {
        this(concreteDensity, bulkheadDensity, sideView, topView,
                // Build the thickness with constant thickness across sections
                IntStream.range(0, sideView.size())
                        .boxed()
                        .collect(HashMap::new,
                                (map, i) -> map.put(new Section(sideView.get(i).getX1(), sideView.get(i).getX2()), thickness),
                                HashMap::putAll),
                // Build the bulkhead map so that the first and last sections are marked as true.
                IntStream.range(0, sideView.size())
                        .boxed()
                        .collect(HashMap::new,
                                (map, i) -> map.put(new Section(sideView.get(i).getX1(), sideView.get(i).getX2()),
                                        (i == 0 || i == sideView.size() - 1)),
                                HashMap::putAll)
        );
    }

    /**
     * Validates that thicknessMap, sideView, and topView are non-null and have the same number of elements.
     * @param thicknessMap the thickness map to validate
     * @param bulkheadMap the bulkhead map to validate
     * @param sideView the side view curves to validate
     * @param topView the top view curves to validate
     */
    private void validateBasicValues(Map<Section, Double> thicknessMap, Map<Section, Boolean> bulkheadMap, List<CubicBezierFunction> sideView, List<CubicBezierFunction> topView) {
        if (thicknessMap == null || thicknessMap.size() < 2 || bulkheadMap == null || bulkheadMap.size() < 2)
            throw new IllegalArgumentException("There must be at least two sections in the side view");
        if (sideView == null || topView == null)
            throw new IllegalArgumentException("sideView and topView must not be null");
        if (sideView.size() != topView.size() || thicknessMap.size() != sideView.size() || bulkheadMap.size() != sideView.size())
            throw new IllegalArgumentException("sideView, topView, thicknessMap, and bulkheadMap must have the same number of elements");
    }

    /**
     * Validates that each Section key in the thicknessMap has boundaries that match the corresponding side-view curve's boundaries.
     * @param thicknessMap the thickness map whose keys (Sections) are validated
     * @param bulkheadMap the bulkhead map whose keys (Sections) are validated
     * @param sideView the list of side-view cubic Bézier functions
     */
    private void validateMaps(Map<Section, Double> thicknessMap, Map<Section, Boolean> bulkheadMap, List<CubicBezierFunction> sideView) {
        List<Section> sections = new ArrayList<>(thicknessMap.keySet());
        List<Section> sectionsShouldBeTheSame = new ArrayList<>(bulkheadMap.keySet());
        if (!sections.equals(sectionsShouldBeTheSame)) throw new IllegalArgumentException("Thickness map and bulkhead map are not describing the same sections");
        sections.sort(Comparator.comparingDouble(Section::getX));
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            CubicBezierFunction sideCurve = sideView.get(i);
            if (Math.abs(sideCurve.getX1() - sec.getX()) >  1e-6 || Math.abs(sideCurve.getX2() - sec.getRx()) >  1e-6) {
                throw new IllegalArgumentException(String.format(
                        "Thickness map section [%.6f, %.6f] does not match the side view curve boundaries [%.6f, %.6f].",
                        sec.getX(), sec.getRx(), sideCurve.getX1(), sideCurve.getX2()));
            }
        }
    }

    /**
     * Computes the list of HullSection objects from the new API fields.
     * For each Section from the side view, the corresponding top–view curve is reformed to match the x-coordinates of knot points
     * De Casteljau’s algorithm is used via HullGeometryService
     * @return the list of computed HullSection objects.
     */
    @JsonIgnore
    private List<HullSection> getHullSectionsReformedFromDeCasteljaus() {
        List<Section> sections = sideViewSegments.stream()
                .map(bezier -> new Section(bezier.getX1(), bezier.getX2()))
                .toList();

        // Convert top–view curves to match side view knot x-coords using de Casteljau's algorithm
        List<CubicBezierFunction> convertedTopView = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            double desiredLeftX = sec.getX();
            double desiredRightX = sec.getRx();
            CubicBezierFunction originalTop = topViewSegments.get(i);
            // Compute parameter values for the new boundaries.
            double t0 = (desiredLeftX - originalTop.getX1()) / (originalTop.getX2() - originalTop.getX1());
            double t1 = (desiredRightX - originalTop.getX1()) / (originalTop.getX2() - originalTop.getX1());
            // Extract the subsegment that exactly spans [desiredLeftX, desiredRightX].
            CubicBezierFunction newTopCurve = HullGeometryService.extractBezierSegment(originalTop, t0, t1);
            convertedTopView.add(newTopCurve);
        }

        // Build HullSection objects using the side–view curves and the reformed top–view curves
        List<HullSection> computedSections = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            double thickness = thicknessMap.get(section);
            boolean isFilledBulkhead = bulkheadMap.get(section);
            HullSection hs = new HullSection(sideViewSegments.get(i), convertedTopView.get(i),
                    section.getX(), section.getRx(), thickness, isFilledBulkhead);
            computedSections.add(hs);
        }
        return computedSections;
    }

    // End New API ▲ ▲ ▲ (Please include getters / validators that work directly with Bézier curves here in the future)
    // ==================================================================================
    // Begin Old API ▼ ▼ ▼ (Please include getters / validators that work with the hullSections based model here in the future)

    /**
     * This is the old Hull model, modelled with hull sections.
     * It is intended to fully maintain all functionality of this API
     * The new parts of the model should ensure this part works the same
     * For now, this model is used for serialization and storage still
     * @param concreteDensity the density of concrete, automatically set to be uniform across the hull
     * @param bulkheadDensity the density of the bulkheads (typically styrofoam), automatically set to be uniform across the hull
     * @param hullSections the list of sections that comprises the hull
     */
    @JsonCreator
    public Hull(@JsonProperty("concreteDensity") double concreteDensity,
                @JsonProperty("bulkheadDensity") double bulkheadDensity,
                @JsonProperty("hullSections") List<HullSection> hullSections) {

        hullSections.sort(Comparator.comparingDouble(Section::getX));
        validateBasicValuesOld(concreteDensity, bulkheadDensity, hullSections);
        validateNoSectionGaps(hullSections);
        validateFloorThickness(hullSections);
        validateWallThickness(hullSections);

        for (HullSection hullSection : hullSections) {
            hullSection.setConcreteDensity(concreteDensity);
            hullSection.setBulkheadDensity(bulkheadDensity);
        }

        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.hullSections = hullSections;
        this.isOldModel = true;
    }

    private void validateBasicValuesOld(double concreteDensity, double bulkheadDensity, List<HullSection> hullSections) {
        if (concreteDensity <= 0)
            throw new IllegalArgumentException("concreteDensity must be greater than zero");
        if (bulkheadDensity <= 0)
            throw new IllegalArgumentException("bulkheadDensity must be greater than zero");
        if (hullSections.size() <= 2)
            throw new IllegalArgumentException("At least two hull sections required");
    }

    /**
     * Basic constructor for a simple model, useful before the user defines geometry / material properties
     * Models the hull as a single section which is a 1D line of the given length
     * @param length the length
     */
    public Hull(double length, double height, double width) {
        this.hullSections = List.of(new HullSection(length, height, width));
        this.concreteDensity = 0;
        this.bulkheadDensity = 0;
        this.isOldModel = true;
    }

    /**
     * @return the length of the hull
     */
    @JsonIgnore
    public double getLength() {
        return hullSections.getLast().getRx();
    }

    /**
     * @return the hull lengthwise endpoints [0, L] as a section
     */
    @JsonIgnore
    public Section getSection() {
        return new Section(0.0, getLength());
    }

    /**
     * Gets the maximum hull height by checking the height of a list of sections
     * @param sections the sections to check
     * @return the maximum height of the given sections
     */
    @JsonIgnore
    private double getMaxHeight(List<HullSection> sections) {
        double canoeHeight = 0;
        for (HullSection section : sections) {
            // Find the section's minimum
            // Function is negated as BrentOptimizer looks for the maximum
            UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(x -> -section.getSideProfileCurve().value(x));
            SearchInterval searchInterval = new SearchInterval(section.getX(), section.getRx());
            UnivariatePointValuePair result = (new BrentOptimizer(1e-10, 1e-14)).optimize(
                    MaxEval.unlimited(),
                    objectiveFunction,
                    searchInterval
            );
            double sectionHeight = -result.getValue();
            if (sectionHeight < canoeHeight) canoeHeight = sectionHeight;
        }

        // canoe height is distance down from 0, so it must be negated
        // rounding addresses a floating point error which is (I think?) in HullGeometryService::calculateThetaBounds
        return CalculusUtils.roundXDecimalDigits(-canoeHeight, 10);
    }

    /**
     * @return the maximum height of the hull
     * Note: two separate getMaxHeight methods are provided for a specific reason
     * one for internal validation purposes (hence, private) one for public use
     */
    @JsonIgnore
    public double getMaxHeight() {
        return getMaxHeight(this.hullSections);
    }

    /**
     * @return the canoe hull self-weight, in kN (returns with a negative sign representing the downward load)
     * Note: this includes bulkheads weight if specified with fillBulkhead
     */
    @JsonIgnore @Traceable
    public double getWeight() {
        return getHullSections().stream().mapToDouble(HullSection::getWeight).sum();
    }

    /**
     * @return the mass of the hull in kg
     */
    @JsonIgnore
    public double getMass() {
        return getHullSections().stream().mapToDouble(HullSection::getMass).sum();
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
        return DiscreteLoadDistribution.fromPiecewiseContinuous(LoadType.HULL, getSelfWeightDistribution(), (int) (getSection().getLength() * 100));
    }

    /**
     * @return the total volume of the canoe by summing up the volumes of all sections.
     */
    @JsonIgnore
    public double getTotalVolume() {
        if (getHullSections() == null || getHullSections().isEmpty()) return 0;
        return getHullSections().stream().mapToDouble(HullSection::getVolume).sum();
    }

    /**
     * @return the total concrete volume of the canoe by summing up the concrete volumes of all sections.
     */
    @JsonIgnore
    public double getConcreteVolume() {
        return getHullSections().stream().mapToDouble(HullSection::getConcreteVolume).sum();
    }

    /**
     * @return the total bulkhead volume of the canoe by summing up the bulkhead volumes of all sections.
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        return getHullSections().stream().mapToDouble(HullSection::getBulkheadVolume).sum();
    }

    /**
     * @return a TreeSet of endpoints for internally defined endpoints.
     * This can be theoretically scaled to a max to approach the true accuracy of the actual canoe
     */
    @JsonIgnore
    public TreeSet<Double> getHullSectionEndPoints() {
        // Add endpoints of hull sections if they are defined
        TreeSet<Double> s = new TreeSet<>();
        if (!getHullSections().isEmpty()) {
            for (HullSection section : getHullSections()) {
                s.add(section.getX());
            }
            s.add(getHullSections().getLast().getRx());
        }
        return s;
    }

    /**
     * @return a composite function representing the stitched-together side profile curves of all hull sections
     * Note that this returned function has been shifted so that it's bottom is at y = 0 instead of its top at y = 0
     */
    @JsonIgnore
    public BoundedUnivariateFunction getPiecedSideProfileCurveShiftedAboveYAxis() {
        List<BoundedUnivariateFunction> functions = hullSections.stream().map(HullSection::getSideProfileCurve).toList();
        List<Section> sections = hullSections.stream().map(sec -> (Section) sec).toList();
        return CalculusUtils.createCompositeFunctionShiftedPositive(functions, sections, false);
    }

    /**
     * Redefines the same hull in terms of different sections from a list of critical points' x-coordinates
     * Accomplishes this by duplicating the existing curves and splitting up intervals strategically
     * For example, consider a side view curve segment C defined for the hull model on [x, rx]
     * A point load (marks a critical point) is added at position k such that x < k < rx, and we want to reform the hull sections split by k
     * This method would duplicate it and create C1 = C on [x, k], C2 = C on [k, rx] implying C = C1 ∪ C2 exactly so geometry stays consistent
     * Note that this form of the hull is for internal use in the floating solver algorithm, and it is not intended to work when displayed textually or graphically in the frontend
     * @param criticalPoints the points which form the intervals to reform the hull under
     */
    @JsonIgnore
    public List<HullSection> getHullSectionsReformedBySegmentDuplication(List<Double> criticalPoints) {
        List<Section> sectionsToMapTo = CalculusUtils.sectionsFromEndpoints(criticalPoints);
        List<HullSection> newHullSections = new ArrayList<>();
        List<HullSection> originalSections = getHullSections();

        for (Section newSection : sectionsToMapTo) {
            double newStart = newSection.getX();
            double newEnd = newSection.getRx();

            List<HullSection> overlappingSections = originalSections.stream()
                    .filter(hs -> hs.getRx() > newStart && hs.getX() < newEnd)
                    .toList();

            for (HullSection overlappingSection : overlappingSections) {
                double overlapStart = Math.max(newStart, overlappingSection.getX());
                double overlapEnd = Math.min(newEnd, overlappingSection.getRx());

                // Create new HullSection based on overlapping portion
                if (overlapEnd > overlapStart) {
                    HullSection newHullSection = new HullSection(
                            overlappingSection.getSideProfileCurve(),
                            overlappingSection.getTopProfileCurve(),
                            overlapStart,
                            overlapEnd,
                            overlappingSection.getThickness(),
                            overlappingSection.isFilledBulkhead()
                    );
                    newHullSections.add(newHullSection);
                }
            }
        }
        return newHullSections;
    }

    /**
     * Validates that the sections provided cover without discontinuities some sub-interval of R^+
     * @param sections the sections to validate before forming a hull
     */
    // TODO: ideally this should also check that the derivative of the section endpoints at each piecewise function is equal to guarantee smoothness
    // TODO: this and the above line's validations should be moved to a new class CubicBezierSpline which wraps List<CubicBezierFunction>
    private void validateNoSectionGaps(List<HullSection> sections) {
        if (sections.getFirst().getX() != 0) throw new IllegalArgumentException("The hull should start at x = 0");

        for (int i = 0; i < sections.size() - 1; i++) {
            HullSection current = sections.get(i);
            HullSection next = sections.get(i + 1);
            double currentEnd = current.getRx();
            double nextStart = next.getX();
            double currentEndDepth = current.getSideProfileCurve().value(currentEnd);
            double nextStartDepth = next.getSideProfileCurve().value(nextStart);
            if (Math.abs(currentEndDepth - nextStartDepth) > 1e-6) // small tolerance for discontinuities in case of floating point errors
                throw new IllegalArgumentException("Hull shape functions must form a continuous curve at section boundaries.");
        }
    }

    /**
     * As a reasonable benchmark (instead of a more complicated integral), an assumption is taken on for the floors
     * We use wall thickness as wall and floor thickness is the same
     * The floor should be no thicker than 25% of the canoe's height (this is already pretty generous realistically)
     */
    private void validateFloorThickness(List<HullSection> sections) {
        double canoeHeight = getMaxHeight(sections);
        for (HullSection section : sections) {
            // This is chosen arbitrarily as a reasonable benchmark
            if (section.getThickness() > canoeHeight / 4)
                throw new IllegalArgumentException("Hull floor thickness must not exceed 1/4 of the canoe's max height");
        }
    }

    /**
     * The two hull walls should not overlap and thus each hull wall can be at most half the canoes width
     * (Although realistically the number is way smaller this is the theoretical max)
     */
    private void validateWallThickness(List<HullSection> sections) {
        for (HullSection section : sections) {
            if (section.getThickness() > section.getMaxWidth() / 2) {
                throw new IllegalArgumentException(String.format(
                        "Hull walls would be greater than the width of the canoe. " +
                                "Thickness: %.4f, Allowed max: %.4f",
                        section.getThickness(), section.getMaxWidth() / 2
                ));
            }
        }
    }
}