package com.wecca.canoeanalysis.models;

import lombok.Getter;

import java.util.*;

@Getter
public class DiscreteLoadDistribution {
    private final double x;
    private final double rx;
    private final TreeMap<? extends Section, UniformDistributedLoad> sectionToLoadMap;

    // Constructor for hull sections (internal loading / self-weight distribution)
    public DiscreteLoadDistribution(List<HullSection> hullSections) {
        validateSectionsFormContinuousInterval(hullSections);

        List<UniformDistributedLoad> loads = new ArrayList<>();
        for (HullSection section : hullSections) {
            double mag = section.getWeight() / section.getLength();
            double x = section.getStart();
            double rx = section.getEnd();
            loads.add(new UniformDistributedLoad(mag, x, rx));
        }

        TreeMap<HullSection, UniformDistributedLoad> sectionToLoadMap = new TreeMap<>(Comparator.comparingDouble(Section::getStart));
        for (int i = 0; i < hullSections.size(); i++) {
            sectionToLoadMap.put(hullSections.get(i), loads.get(i));
        }

        this.sectionToLoadMap = sectionToLoadMap;
        this.x = hullSections.getFirst().getStart();
        this.rx = hullSections.getLast().getEnd();
    }

    // Constructor for non-hull sections (external load distributions)
    public DiscreteLoadDistribution(List<Section> sections, List<UniformDistributedLoad> loads) {
        validateSectionsAgainstLoads(sections, loads);
        validateSectionsAreNotHullSections(sections);

        this.x = sections.getFirst().getStart();
        this.rx = sections.getLast().getEnd();

        TreeMap<Section, UniformDistributedLoad> sectionToLoadMap = new TreeMap<>(Comparator.comparingDouble(Section::getStart));
        for (int i = 0; i < sections.size(); i++) {
            sectionToLoadMap.put(sections.get(i), loads.get(i));}
        this.sectionToLoadMap = sectionToLoadMap;
    }

    private void validateSectionsAreNotHullSections(List<Section> sections) {
        for (Section section : sections) {
            if (section instanceof HullSection) {
                throw new IllegalArgumentException("Sections should not be instances of HullSection.");
            }
        }
    }

    private void validateSectionsAgainstLoads(List<Section> sections, List<UniformDistributedLoad> loads) {
        if (sections.size() != loads.size())
            throw new IllegalArgumentException("Cannot map sections to loads, unequal amount of sections and loads");
        else {
            validateSectionsFormContinuousInterval(sections);

            TreeMap<Section, UniformDistributedLoad> sectionToLoadMap = new TreeMap<>(Comparator.comparingDouble(Section::getStart));
            for (int i = 0; i < sections.size(); i++) {
                sectionToLoadMap.put(sections.get(i), loads.get(i));}
            for (Map.Entry<Section, UniformDistributedLoad> sectionToLoadMapEntry : sectionToLoadMap.entrySet() ){
                validateSectionWeightDistributedAcrossEntireLength(sectionToLoadMapEntry);}
        }
    }

    private void validateSectionsFormContinuousInterval(List<? extends Section> sections) {
        for (int i = 0; i < sections.size() - 1; i++)
        {
            Section currentSection = sections.get(i);
            Section nextSection = sections.get(i + 1);

            if (currentSection.getEnd() != nextSection.getStart())
                throw new IllegalArgumentException("Sections should not have gaps between them");
        }
    }

    private void validateSectionWeightDistributedAcrossEntireLength(Map.Entry<Section, UniformDistributedLoad> sectionToLoadMapEntry) {
        double sectionStart = sectionToLoadMapEntry.getKey().getStart();
        double sectionEnd = sectionToLoadMapEntry.getKey().getEnd();

        double dLoadX = sectionToLoadMapEntry.getValue().getX();
        double dLoadRx = sectionToLoadMapEntry.getValue().getRx();

        if (sectionStart != dLoadX || sectionEnd != dLoadRx)
            throw new IllegalArgumentException("Ensure the dLoad interval matches the section interval");
    }
}
