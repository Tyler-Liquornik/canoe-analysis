package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
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

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Getter @Setter @EqualsAndHashCode
public class Hull {
    @JsonProperty("concreteDensity")
    private double concreteDensity; // [kg/m^3]
    @JsonProperty("bulkheadDensity")
    private double bulkheadDensity; // [kg/m^3]
    @JsonProperty("hullSections")
    private List<HullSection> hullSections;

    @JsonCreator
    public Hull(@JsonProperty("concreteDensity") double concreteDensity,
                @JsonProperty("bulkheadDensity") double bulkheadDensity,
                @JsonProperty("hullSections") List<HullSection> hullSections) {

        hullSections.sort(Comparator.comparingDouble(Section::getX));
        validateBasicValues(concreteDensity, bulkheadDensity, hullSections);
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
    }

    private void validateBasicValues(double concreteDensity, double bulkheadDensity, List<HullSection> hullSections) {
        if (concreteDensity <= 0)
            throw new IllegalArgumentException("concreteDensity must be greater than zero");
        if (bulkheadDensity <= 0)
            throw new IllegalArgumentException("bulkheadDensity must be greater than zero");
        if (hullSections.isEmpty())
            throw new IllegalArgumentException("At least one hull section required");
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
        for (HullSection section : sections)
        {
            // Find the sections minimum
            // Function is negated as BrentOptimizer looks for the maximum
            UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(x -> -section.getSideProfileCurve().value(x));
            SearchInterval searchInterval = new SearchInterval(section.getX(), section.getRx());
            UnivariatePointValuePair result = (new BrentOptimizer(1e-10, 1e-14)).optimize(
                    MaxEval.unlimited(),
                    objectiveFunction,
                    searchInterval
            );

            double sectionHeight = -result.getValue();
            if (sectionHeight < canoeHeight) {
                canoeHeight = sectionHeight;
            }
        }

        // canoe height is distance down from 0, so it must be negated
        // rounding addresses a floating point error in HullBuildController::getThetaBounds
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
        if (getTotalVolume() == 0)
            return null;
        else
            return PiecewiseContinuousLoadDistribution.fromHull(this);
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
        if (getHullSections() == null || getHullSections().isEmpty())
            return 0;
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
        if (!getHullSections().isEmpty())
        {
            for (HullSection section : getHullSections()) {s.add(section.getX());}
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
        return CalculusUtils.createCompositeFunctionShiftedPositive(functions, sections);
    }

    /**
     * Validates that the sections provided cover without discontinuities some sub-interval of R^+
     * @param sections the sections to validate before forming a hull
     */
    // TODO: ideally this should also check that the derivative of the section endpoints at each piecewise function is equal to guarantee smoothness
    private void validateNoSectionGaps(List<HullSection> sections) {

        if (sections.getFirst().getX() != 0) {
            throw new IllegalArgumentException("The hull should start at x = 0");
        }

        for (int i = 0; i < sections.size() - 1; i++)
        {
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

        for (HullSection section : sections)
        {
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
        for (HullSection section : sections)
        {
            if (section.getThickness() > section.getMaxWidth() / 2)
                throw new IllegalArgumentException("Hull walls would be greater than the width of the canoe");
        }
    }
}
