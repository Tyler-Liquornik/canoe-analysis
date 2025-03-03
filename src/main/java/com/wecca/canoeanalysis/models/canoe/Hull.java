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
import com.wecca.canoeanalysis.utils.SectionPropertyMapEntry;
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
import java.util.stream.Stream;

/**
 * You may notice there is a new hull model/API built atop the old one
 * The goal was to decouple the side view curve segments from the front view
 * All while ensuring backwards compatability
 * Particularly the x-coordinates of their knot points.
 * The problem this solved was that this allows the user to edit the knot points in the side view without disrupting the top view geometry.
 */
@Getter @Setter @EqualsAndHashCode
public class Hull {

    // These fields are part of the legacy (old) API.
    @JsonIgnore
    private List<HullSection> hullSections;
    @JsonIgnore
    private double concreteDensity;
    @JsonIgnore
    private double bulkheadDensity;

    // New Model: encapsulated in HullProperties.
    @JsonProperty("hullProperties")
    private HullProperties hullProperties;
    @JsonProperty("sideViewSegments")
    private List<CubicBezierFunction> sideViewSegments;
    @JsonProperty("topViewSegments")
    private List<CubicBezierFunction> topViewSegments;

    // Flag to cheaply check which model is implemented
    @JsonProperty("isOldModel")
    private boolean isOldModel;

    /**
     * The new model
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
        this.isOldModel = false;

        // Compute and cache the legacy hullSections from the new model.
         this.hullSections = getHullSectionsReformedFromDeCasteljaus();

        // Validate new API fields.
        validateBasicValues(hullProperties.getThicknessMap(), hullProperties.getBulkheadMap(), sideViewSegments, topViewSegments);
        validateMaps(hullProperties.getThicknessMap(), hullProperties.getBulkheadMap(), sideViewSegments);

//        // Validate legacy fields.
//        validateBasicValuesOld(concreteDensity, bulkheadDensity, hullSections);
//        validateNoSectionGaps(hullSections);
//        validateFloorThickness(hullSections);
//        validateWallThickness(hullSections);
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
                buildThicknessList(sideView, thickness), buildBulkheadList(sideView)), sideView, topView);
    }

    private static List<SectionPropertyMapEntry> buildThicknessList(List<CubicBezierFunction> sideView, double thickness) {
        return IntStream.range(0, sideView.size())
                .boxed()
                .map(i -> new SectionPropertyMapEntry(sideView.get(i).getX1(), sideView.get(i).getX2(), String.valueOf(thickness)))
                .toList();
    }

    private static List<SectionPropertyMapEntry> buildBulkheadList(List<CubicBezierFunction> sideView) {
        return IntStream.range(0, sideView.size())
                .boxed()
                .map(i -> new SectionPropertyMapEntry(sideView.get(i).getX1(), sideView.get(i).getX2(), String.valueOf((i == 0 || i == sideView.size() - 1))))
                .toList();
    }

    private void validateBasicValues(List<SectionPropertyMapEntry> thicknessMap, List<SectionPropertyMapEntry> bulkheadMap, List<CubicBezierFunction> sideView, List<CubicBezierFunction> topView) {
        // Check for null in any list
        if (Stream.of(thicknessMap, bulkheadMap, sideView, topView).anyMatch(Objects::isNull))
            throw new IllegalArgumentException("sideView, topView, thicknessList, and bulkheadList must not be null");
        if (thicknessMap.size() < 2 || bulkheadMap.size() < 2)
            throw new IllegalArgumentException("There must be at least two sections in the side view");
        if (sideView.size() != topView.size() || thicknessMap.size() != sideView.size() || bulkheadMap.size() != sideView.size())
            throw new IllegalArgumentException("sideView, topView, thicknessList, and bulkheadList must have the same number of elements");
    }

    private void validateMaps(List<SectionPropertyMapEntry> thicknessMap,
                              List<SectionPropertyMapEntry> bulkheadMap,
                              List<CubicBezierFunction> sideView) {
        if (!IntStream.range(0, thicknessMap.size())
                .allMatch(i -> {
                    Section s1 = thicknessMap.get(i);
                    Section s2 = bulkheadMap.get(i);
                    return s1.getX() == s2.getX() && s1.getRx() == s2.getRx();
                }))
            throw new IllegalArgumentException("Thickness list and bulkhead list do not describe the same sections");

        // Validate that each section matches the corresponding side view curve boundaries.
        IntStream.range(0, thicknessMap.size())
                .forEach(i -> {
                    Section s = thicknessMap.get(i);
                    CubicBezierFunction side = sideView.get(i);
                    if (Math.abs(side.getX1() - s.getX()) > 1e-6 || Math.abs(side.getX2() - s.getRx()) > 1e-6)
                        throw new IllegalArgumentException(String.format(
                                "Section [%.6f, %.6f] does not match side view boundaries [%.6f, %.6f].",
                                s.getX(), s.getRx(), side.getX1(), side.getX2()));
                });
    }

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

    @JsonIgnore
    public double getLength() {
        if (sideViewSegments == null || sideViewSegments.isEmpty()) return 0;
        return sideViewSegments.getLast().getX2() - sideViewSegments.getFirst().getX1();
    }

    @JsonIgnore
    public Section getSection() {
        if (sideViewSegments == null || sideViewSegments.isEmpty()) return null;
        return new Section(sideViewSegments.getFirst().getX1(), sideViewSegments.getLast().getX2());
    }

    /**
     * Computes the list of HullSection objects from the new API fields.
     * For each Section from the side view, the corresponding top–view curve is reformed to match the x-coordinates of knot points
     * De Casteljau’s algorithm is used via HullGeometryService
     * @return the list of computed HullSection objects.
     */
    @JsonIgnore
    public List<HullSection> getHullSectionsReformedFromDeCasteljaus() {
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
            SectionPropertyMapEntry thicknessMapEntry = hullProperties.getThicknessMap().get(i);
            SectionPropertyMapEntry bulkheadMapEntry = hullProperties.getBulkheadMap().get(i);
            double thickness = Double.parseDouble(thicknessMapEntry.getValue());
            boolean isFilledBulkhead = Boolean.parseBoolean(bulkheadMapEntry.getValue());
            HullSection hs = new HullSection(sideViewSegments.get(i), convertedTopView.get(i), section.getX(), section.getRx(), thickness, isFilledBulkhead);
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
    public Hull(double concreteDensity, double bulkheadDensity, List<HullSection> hullSections) {
        hullSections.sort(Comparator.comparingDouble(Section::getX));
        validateBasicValuesOld(concreteDensity, bulkheadDensity, hullSections);
        validateNoSectionGaps(hullSections);
        // validateFloorThickness(hullSections);
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

//    /**
//     * As a reasonable benchmark (instead of a more complicated integral), an assumption is taken on for the floors
//     * We use wall thickness as wall and floor thickness is the same
//     * The floor should be no thicker than 25% of the canoe's height (this is already pretty generous realistically)
//     */
//    private void validateFloorThickness(List<HullSection> sections) {
//        double canoeHeight = getMaxHeightOldModel(sections);
//        for (HullSection section : sections) {
//            // This is chosen arbitrarily as a reasonable benchmark
//            if (section.getThickness() > canoeHeight / 4)
//                throw new IllegalArgumentException("Hull floor thickness must not exceed 1/4 of the canoe's max height");
//        }
//    }

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