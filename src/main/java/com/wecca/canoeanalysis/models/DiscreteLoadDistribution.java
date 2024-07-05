package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.*;

/**
 * Represents a more complex loading with a distribution over some sub-interval of the hull [x, rx]
 *
 * Note: Written with the factory pattern as multiple constructor with a single list as a parameter is not allowed
 * This is due to the type erasure implementation of Java generics
 */
@Getter
public
class DiscreteLoadDistribution extends Load {
    private final List<UniformLoadDistribution> loads;

    /**
     * @param loads the discretized distribution
     * Note: the constructor is private as it is used by factory methods
     */
    public DiscreteLoadDistribution(String type, List<UniformLoadDistribution> loads) {
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
     * @param hull the hull to get the self-weight distribution of
     */
    public static DiscreteLoadDistribution fromHull(Hull hull) {
        List<HullSection> hullSections = hull.getHullSections();
        hullSections.sort(Comparator.comparingDouble(Section::getX));

        List<UniformLoadDistribution> loads = new ArrayList<>();
        for (HullSection section : hullSections) {
            double mag = section.getWeight() / section.getLength();
            double x = section.getX();
            double rx = section.getRx();
            loads.add(new UniformLoadDistribution(mag, x, rx));
        }
        return new DiscreteLoadDistribution("Hull Load", loads);
    }

    /**
     * Factory method to create a distribution from a univariate function
     * Discretization implements a midpoint-based Riemann sum with intervals lengths (deltaX_i) matching intervals for pieces of the piecewise
     * @param piecewise the function to discretize with average values of piecewise intervals
     * @return a DiscreteLoadDistribution object
     */
    public static DiscreteLoadDistribution fromPiecewiseContinuous(String type, PiecewiseContinuousLoadDistribution piecewise) {
        List<UniformLoadDistribution> loads = piecewise.getPieces().entrySet().stream()
                .map(piece -> {
                    Section section = piece.getKey();
                    double midpoint = (section.getX() + section.getRx()) / 2.0;
                    double mag = piece.getValue().value(midpoint);
                    return new UniformLoadDistribution("Section", mag, section.getX(), section.getRx());
                })
                .toList();

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
