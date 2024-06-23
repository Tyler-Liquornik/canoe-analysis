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
public class DiscreteLoadDistribution {
    @JsonIgnore
    private final Section section;
    private final List<UniformDistributedLoad> loads;

    /**
     * @param x the left endpoint of the distribution
     * @param rx the right endpoint of the distribution
     * @param loads the discretized distribution
     * Note: the constructor is private to enable factory pattern
     */
    private DiscreteLoadDistribution(double x, double rx, List<UniformDistributedLoad> loads) {
        this.section = new Section(x, rx);
        this.loads = loads;
    }

    /**
     * Factory method to create a self-weight distribution from a hull
     * @param hull the hull to get the self-weight distribution of
     */
    public static DiscreteLoadDistribution fromHull(Hull hull) {
        List<HullSection> hullSections = hull.getHullSections();
        hullSections.sort(Comparator.comparingDouble(Section::getX));
        validateSectionsFormContinuousInterval(hullSections);

        List<UniformDistributedLoad> loads = new ArrayList<>();
        for (HullSection section : hullSections) {
            double mag = section.getWeight() / section.getLength();
            double x = section.getX();
            double rx = section.getRx();
            loads.add(new UniformDistributedLoad(mag, x, rx));
        }

        double x = hullSections.getFirst().getX();
        double rx = hullSections.getLast().getRx();
        return new DiscreteLoadDistribution(x, rx, loads);
    }

    /**
     * Factory method to create a distribution from loads directly (used for external loading distributions)
     * @param dLoads make up the distribution
     */
    public static DiscreteLoadDistribution fromDistributedLoads(List<UniformDistributedLoad> dLoads) {
        dLoads.sort(Comparator.comparingDouble(Load::getX));
        List<Section> sections = dLoads.stream().map(UniformDistributedLoad::getSection).toList();
        validateSectionsFormContinuousInterval(sections);

        double x = dLoads.getFirst().getX();
        double rx = dLoads.getLast().getRx();
        return new DiscreteLoadDistribution(x, rx, dLoads);
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
