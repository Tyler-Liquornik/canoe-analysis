package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.*;

/**
 * Represents a more complex loading with a distribution over some sub-interval of the hull [x, rx]
 *
 * Note: Written with the factory pattern as multiple constructor with a single list as a parameter is not allowed
 * This is due to the type erasure implementation of Java generics
 */
@Getter @EqualsAndHashCode(callSuper = true)
public class DiscreteLoadDistribution extends Load {
    @JsonProperty("loads")
    private final List<UniformLoadDistribution> loads;

    /**
     * @param loads the discretized distribution
     * Note: the constructor is private as it is used by factory methods
     */
    public DiscreteLoadDistribution(LoadType type, List<UniformLoadDistribution> loads) {
        super(type);
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
     * Factory method to create a self-weight distribution from a hull
     * Note: Only real difference in output with piecewise fromPiecewiseContinuous is the recognition and naming of bulkhead sections
     * @param hull the hull to get the self-weight distribution of
     */
    public static DiscreteLoadDistribution fromHull(Hull hull) {
        List<HullSection> hullSections = new ArrayList<>(hull.getHullSections());
        hullSections.sort(Comparator.comparingDouble(Section::getX));

        List<UniformLoadDistribution> loads = new ArrayList<>();
        for (HullSection section : hullSections) {
            double mag = section.getWeight() / section.getLength();
            double x = section.getX();
            double rx = section.getRx();
            LoadType type = section.isFilledBulkhead() ? LoadType.DISCRETE_HULL_SECTION_HAS_BULKHEAD : LoadType.DISCRETE_SECTION;
            loads.add(new UniformLoadDistribution(type, mag, x, rx));
        }
        return new DiscreteLoadDistribution(LoadType.HULL, loads);
    }

    /**
     * Factory method to create a distribution from a univariate function
     * Discretization implements a midpoint-based Riemann sum with intervals lengths (deltaX_i) matching intervals for pieces of the piecewise
     * @param piecewise the function to discretize with average values of piecewise intervals
     * @return a DiscreteLoadDistribution object
     */
    public static DiscreteLoadDistribution fromPiecewiseContinuous(LoadType type, PiecewiseContinuousLoadDistribution piecewise) {
        List<UniformLoadDistribution> loads = piecewise.getPieces().entrySet().stream()
                .map(piece -> {
                    Section section = piece.getKey();
                    double midpoint = (section.getX() + section.getRx()) / 2.0;
                    double mag = piece.getValue().value(midpoint);
                    return new UniformLoadDistribution(LoadType.DISCRETE_SECTION, mag, section.getX(), section.getRx());
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
    public static DiscreteLoadDistribution fromPiecewiseContinuous(LoadType type, PiecewiseContinuousLoadDistribution piecewise, int numIntervals) {
        UnivariateFunction piecedFunction = piecewise.getPiecedFunction();
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
}
