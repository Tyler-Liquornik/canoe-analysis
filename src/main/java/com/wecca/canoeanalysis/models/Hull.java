package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import java.util.List;
import java.util.TreeSet;

@Getter @Setter
public class Hull {
    private double concreteDensity; // [kg/m^3]
    private double bulkheadDensity; // [kg/m^3]
    private List<HullSection> hullSections;

    public Hull(double concreteDensity, double bulkheadDensity, List<HullSection> hullSections) {
        validateContinuousHullShape(hullSections);
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

    /**
     * Basic constructor for a simple model, useful before the user defines geometry / material properties
     * Models the hull as a single section which is a 1D line of the given length
     * @param length the length
     */
    public Hull(double length) {
        this.hullSections = List.of(new HullSection(0, length));
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
            UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(x -> -section.getProfileCurve().value(x));
            SearchInterval searchInterval = new SearchInterval(section.getX(), section.getRx());
            UnivariatePointValuePair result = (new BrentOptimizer(1e-10, 1e-14)).optimize(
                    MaxEval.unlimited(),
                    objectiveFunction,
                    searchInterval
            );

            double sectionHeight = -result.getValue();
            if (sectionHeight < canoeHeight)
                canoeHeight = sectionHeight;
        }
        return -canoeHeight; // canoe height is distance down from 0, so it must be negated
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
    @JsonIgnore
    public double getSelfWeight() {
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
    public DiscreteLoadDistribution getSelfWeightDistribution() {
        if (getTotalVolume() == 0)
            return null;
        else
            return DiscreteLoadDistribution.fromHull(this);
    }

    /**
     * @return the total volume of the canoe by summing up the volumes of all sections.
     */
    @JsonIgnore
    public double getTotalVolume() {
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

    // TODO: ideally this should also check that the derivative of the section endpoints at each piecewise function is equal to guarantee smoothness
    private void validateContinuousHullShape(List<HullSection> sections) {

        for (int i = 0; i < sections.size() - 1; i++)
        {
            HullSection current = sections.get(i);
            HullSection next = sections.get(i + 1);
            double currentEnd = current.getRx();
            double nextStart = next.getX();
            double currentEndDepth = current.getProfileCurve().value(currentEnd);
            double nextStartDepth = next.getProfileCurve().value(nextStart);
            if (Math.abs(currentEndDepth - nextStartDepth) > 1e-6) // small tolerance for discontinuities in case of floating point errors
                throw new IllegalArgumentException("Hull shape functions must form a continuous curve at section boundaries.");
        }
    }

    /**
     * Validates that the hull shape function is non-positive on its domain [start, end]
     * This convention allows waterline height h for a floating hull to be a distance below the top of the null at h = 0
     * Uses calculus to avoid checking all points individually by checking only critical points and domain endpoints
     */
    private void validateFloorThickness(List<HullSection> sections) {

        double canoeHeight = getMaxHeight(sections);

        for (HullSection section : sections)
        {
            // This is chosen arbitrarily as a reasonable benchmark
            if (section.getWallsThickness() > canoeHeight / 4)
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
            if (section.getWallsThickness() > section.getWidth() / 2)
                throw new IllegalArgumentException("Hull walls would be greater than the width of the canoe");
        }
    }
}
