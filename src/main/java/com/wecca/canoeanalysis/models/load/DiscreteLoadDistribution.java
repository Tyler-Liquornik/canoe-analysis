package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.SectionPropertyMapEntry;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;

/**
 * Represents a more complex loading with a distribution over some sub-interval of the hull [x, rx]
 * Note: Written with the factory pattern as multiple constructor with a single list as a parameter is not allowed
 * This is due to the type erasure implementation of Java generics
 */
@Getter @EqualsAndHashCode(callSuper = true)
public class DiscreteLoadDistribution extends LoadDistribution {
    @JsonProperty("loads")
    private final List<UniformLoadDistribution> loads;

    /**
     * @param loads the discretized distribution
     * Note: the constructor is private as it is used by factory methods
     */
    public DiscreteLoadDistribution(LoadType type, List<UniformLoadDistribution> loads) {
        super(type, new Section(loads.getFirst().getX(), loads.getLast().getRx()));
        this.loads = loads;
    }

    /**
     * @return the maximum child dLoad value
     */
    @Override
    public double getMaxSignedValue() {
        return loads.stream()
                .mapToDouble(UniformLoadDistribution::getMaxSignedValue)
                .max()
                .orElse(0.0);
    }

    @Override
    public double getForce() {
        return loads.stream().mapToDouble(UniformLoadDistribution::getForce).sum();
    }

    @Override
    public double getX() {
        return loads.getFirst().getX();
    }

    /**
     * Factory method to create a distribution from a univariate function
     * Sections are typed as DISCRETE_SECTION or DISCRETE_HULL_SECTION_HAS_BULKHEAD
     * Discretization implements a midpoint-based Riemann sum with intervals lengths (deltaX_i) matching intervals for pieces of the piecewise
     * @param type the type of the load distribution
     * @param piecewise the function to discretize with average values of piecewise intervals
     * @return a DiscreteLoadDistribution object
     */
    public static DiscreteLoadDistribution fromPiecewiseTyped(LoadType type, PiecewiseContinuousLoadDistribution piecewise, List<SectionPropertyMapEntry> bulkheadMap) {
        List<UniformLoadDistribution> loads = piecewise.getPieces().entrySet().stream()
                .map(piece -> {
                    Section section = piece.getKey();
                    double midpoint = (section.getX() + section.getRx()) / 2.0;
                    double mag = piece.getValue().value(midpoint);

                    LoadType sectionType;
                    if (type == LoadType.HULL) {
                        boolean isFilledBulkhead = bulkheadMap.stream()
                                .filter(entry -> entry.getX() <= section.getX() && section.getRx() <= entry.getRx())
                                .findFirst()
                                .map(entry -> Boolean.parseBoolean(entry.getValue()))
                                .orElse(false);
                        sectionType = isFilledBulkhead ? LoadType.DISCRETE_HULL_SECTION_HAS_BULKHEAD : LoadType.DISCRETE_SECTION;
                    } else {
                        sectionType = LoadType.DISCRETE_SECTION;
                    }
                    return new UniformLoadDistribution(sectionType, mag, section.getX(), section.getRx());
                })
                .toList();
        return new DiscreteLoadDistribution(type, loads);
    }

    /**
     * Factory method to create a distribution from a univariate function
     * Discretization implements a midpoint-based Riemann sum with a custom defined number of intervals regardless of the sections of pieces
     * @param type the type of the load distribution
     * @param piecewise the function to discretize with average values of piecewise intervals
     * @param numIntervals the number of subsections to split the function into
     * @return a DiscreteLoadDistribution object
     */
    public static DiscreteLoadDistribution fromPiecewise(LoadType type, PiecewiseContinuousLoadDistribution piecewise, int numIntervals) {
        BoundedUnivariateFunction piecedFunction = piecewise.getPiecedFunction();
        double start = piecewise.getSection().getX();
        double end = piecewise.getSection().getRx();
        double step = (end - start) / (double) numIntervals;

        List<UniformLoadDistribution> loads = new ArrayList<>();
        for (int i = 0; i < numIntervals; i++) {
            double sectionStart = start + i * step;
            double sectionEnd = sectionStart + step;
            double midpoint = (sectionStart + sectionEnd) / 2.0;
            double mag = piecedFunction.value(midpoint);
            loads.add(new UniformLoadDistribution(LoadType.DISCRETE_SECTION, mag, sectionStart, sectionEnd));
        }

        return new DiscreteLoadDistribution(type, loads);
    }

    /**
     * @return the section [x, rx] that the load distribution is on
     */
    @JsonIgnore
    public Section getSection() {
        return new Section(loads.getFirst().getX(), loads.getLast().getRx());
    }

    /**
     * Calculates the moment generated by the entire discrete load distribution about (x = rotationX, y = 0).
     * @param rotationX the x co-ordinate of the point of rotation
     * @return sum of moments of each UniformLoadDistribution in the list.
     */
    @Override @JsonIgnore
    public double getMoment(double rotationX) {
        return loads.stream()
                .mapToDouble(load -> load.getMoment(rotationX))
                .sum();
    }
}
