package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

@Getter @Setter @EqualsAndHashCode
public class Canoe {
    private double length;
    private final ArrayList<Load> loads;

    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    @JsonIgnore
    private List<CanoeSection> sections;

    public Canoe(double concreteDensity, double bulkheadDensity) {
        this.sections = new ArrayList<>();
        this.loads = new ArrayList<>();
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.length = 0;
        assertContinuousHullShape();
    }

    public Canoe(List<CanoeSection> sections, List<PointLoad> pointLoads, List<UniformDistributedLoad> udlLoads, double concreteDensity, double bulkheadDensity) {
        this.sections = sections;
        this.loads = new ArrayList<>();
        this.loads.addAll(pointLoads);
        this.loads.addAll(udlLoads);
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.length = sections.stream().mapToDouble(CanoeSection::getLength).sum();
        assertContinuousHullShape();
    }

    private void assertContinuousHullShape() {
        for (int i = 0; i < sections.size() - 1; i++)
        {
            CanoeSection current = sections.get(i);
            CanoeSection next = sections.get(i + 1);
            double currentEnd = current.getEnd();
            double nextStart = next.getStart();
            double currentEndDepth = current.getHullShapeFunction().apply(currentEnd);
            double nextStartDepth = next.getHullShapeFunction().apply(nextStart);
            if (Math.abs(currentEndDepth - nextStartDepth) > 1e-6) // small tolerance for discontinuities in case of floating point errors
                throw new IllegalArgumentException("Hull shape functions must form a continuous curve at section boundaries.");
        }
    }

    public AddLoadResult addLoad(Load l)
    {
        if (l instanceof PointLoad)
        {
            // Do not add the load if it is zero valued unless if is a support
            // Zero-valued supports are still added as markers for the model and ListView
            if (l.getMag() == 0)
                if (!((PointLoad) l).isSupport())
                    return AddLoadResult.ADDED;
                else
                    l.setMag(0.00); // In case mag is -0 so that the negative doesn't display to the user

            // Search for other loads at the same position, and combine their magnitudes
            for (PointLoad pLoad : getPLoads()) {
                if (pLoad.getX() == l.getX() && !((PointLoad) l).isSupport())
                {
                    double newMag = pLoad.getMag() + l.getMag();
                    if (newMag == 0)
                    {
                        removeLoad(loads.indexOf(pLoad));
                        return AddLoadResult.REMOVED;
                    }
                    loads.get(loads.indexOf(pLoad)).setMag(newMag);
                    pLoad.setMag(newMag);
                    return AddLoadResult.COMBINED;
                }
            }
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        else if (l instanceof UniformDistributedLoad)
        {
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        return AddLoadResult.ADDED;
    }

    @JsonIgnore
    public int getMaxLoadIndex() {
        if (loads.isEmpty()) {
            return -1;
        }

        int maxIndex = 0;
        double max = 0;
        for (int i = 0; i < loads.size(); i++) {
            double mag = Math.abs(loads.get(i).getMag());
            if (mag > max) {
                max = mag;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public void removeLoad(int index) {
        loads.remove(index);
    }

    public void clearLoads() {
        loads.clear();
    }

    @JsonIgnore
    public List<PointLoad> getPLoads() {
        List<PointLoad> pLoads = new ArrayList<>();
        for (Load load : loads) {
            if (load instanceof PointLoad pLoad) {
                pLoads.add(pLoad);
            }
        }
        return pLoads;
    }

    @JsonIgnore
    public List<UniformDistributedLoad> getDLoads() {
        List<UniformDistributedLoad> dLoads = new ArrayList<>();
        for (Load load : loads) {
            if (load instanceof UniformDistributedLoad dLoad) {
                dLoads.add(dLoad);
            }
        }
        return dLoads;
    }

    // Setting the length clears the sections
    public void setLength(double length){
        this.length = length;
        this.sections = new ArrayList<>();
    }

    // Setting the sections must match the length
    public void setSections(List<CanoeSection> sections) {
        double totalSectionLength = sections.stream().mapToDouble(CanoeSection::getLength).sum();
        if (totalSectionLength != this.length) {
            throw new IllegalArgumentException("Total length of sections must equal the length of the canoe.");
        }
        this.sections = sections;
        assertContinuousHullShape();
    }


    @JsonIgnore
    public TreeSet<Double> getSectionEndPoints() {
        TreeSet<Double> s = new TreeSet<>();
        for (Load l : loads) {
            s.add(l.getX());
            if (l instanceof UniformDistributedLoad distributedLoad) {
                s.add(distributedLoad.getRx());
            }
        }
        s.add(0.0);
        s.add(length);
        return s;
    }
}

