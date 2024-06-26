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
    private final List<UniformlyDistributedLoad> loads;

    /**
     * @param loads the discretized distribution
     * Note: the constructor is private to enable factory pattern
     */
    private DiscreteLoadDistribution(List<UniformlyDistributedLoad> loads) {
        super("Distribution");
        this.loads = loads;
    }

    @Override
    public double getValue() {
        return loads.stream()
                .mapToDouble(UniformlyDistributedLoad::getValue)
                .max()
                .orElse(0.0);
    }

    @Override
    public double getForce() {
        return loads.stream().mapToDouble(UniformlyDistributedLoad::getValue).sum();
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
        validateSectionsFormContinuousInterval(hullSections);

        List<UniformlyDistributedLoad> loads = new ArrayList<>();
        for (HullSection section : hullSections) {
            double mag = section.getWeight() / section.getLength();
            double x = section.getX();
            double rx = section.getRx();
            loads.add(new UniformlyDistributedLoad(mag, x, rx));
        }
        return new DiscreteLoadDistribution(loads);
    }

    /**
     * Factory method to create a distribution from loads directly (used for external loading distributions)
     * @param dLoads make up the distribution
     */
    public static DiscreteLoadDistribution fromDistributedLoads(List<UniformlyDistributedLoad> dLoads) {
        dLoads.sort(Comparator.comparingDouble(Load::getX));
        List<Section> sections = dLoads.stream().map(UniformlyDistributedLoad::getSection).toList();
        validateSectionsFormContinuousInterval(sections);

        return new DiscreteLoadDistribution(dLoads);
    }

    /**
     * @return the section [x, rx] that the load distribution is on
     */
    @JsonIgnore
    public Section getSection() {
        return new Section(loads.getFirst().getX(), loads.getLast().getRx());
    }

    /**
     * A load distribution cannot have gaps, it must be defined everywhere on its interval
     * @param sections the sections to validate
     */
    private static void validateSectionsFormContinuousInterval(List<? extends Section> sections) {
        for (int i = 0; i < sections.size() - 1; i++)
        {
            Section currentSection = sections.get(i);
            Section nextSection = sections.get(i + 1);

            if (currentSection.getRx() != nextSection.getX())
                throw new IllegalArgumentException("Sections should not have gaps between them");
        }
    }
}
