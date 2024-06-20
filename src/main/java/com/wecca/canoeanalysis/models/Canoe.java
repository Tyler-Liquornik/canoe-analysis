package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;

import java.util.*;
import java.util.function.Function;

@Getter @Setter @EqualsAndHashCode
public class Canoe {
    private double length;
    private final ArrayList<Load> loads;

    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    @JsonIgnore
    private List<HullSection> sections;

    public Canoe(double concreteDensity, double bulkheadDensity) {
        this.sections = new ArrayList<>();
        this.loads = new ArrayList<>();
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.length = 0;
        validateContinuousHullShape();
    }

    public Canoe(List<HullSection> sections, List<PointLoad> pointLoads, List<UniformDistributedLoad> udlLoads, double concreteDensity, double bulkheadDensity) {
        this.sections = sections;
        this.loads = new ArrayList<>();
        this.loads.addAll(pointLoads);
        this.loads.addAll(udlLoads);
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.length = sections.stream().mapToDouble(HullSection::getLength).sum();
        validateContinuousHullShape();
        validateWallThickness();
        validateFloorThickness();
    }

    private void validateContinuousHullShape() {
        for (int i = 0; i < sections.size() - 1; i++)
        {
            HullSection current = sections.get(i);
            HullSection next = sections.get(i + 1);
            double currentEnd = current.getEnd();
            double nextStart = next.getStart();
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
    private void validateFloorThickness() {

        double canoeHeight = getCanoeHeight();

        for (HullSection section : sections)
        {
            // This is chosen arbitrarily as a reasonable benchmark
            if (section.getFloorThickness() > canoeHeight / 4)
                throw new IllegalArgumentException("Hull floor thickness must not exceed 1/4 of the canoe's max height");
        }
    }

    /**
     * The two hull walls should not overlap and thus each hull wall can be at most half the canoes width
     * (Although realistically the number is way smaller this is the theoretical max)
     */
    public void validateWallThickness() {
        for (HullSection section : sections)
        {
            if (section.getWallThickness() > section.getWidth() / 2)
                throw new IllegalArgumentException("Hull walls would be greater than the width of the canoe");
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
    public void setSections(List<HullSection> sections) {
        double totalSectionLength = sections.stream().mapToDouble(HullSection::getLength).sum();
        if (totalSectionLength != this.length) {
            throw new IllegalArgumentException("Total length of sections must equal the length of the canoe.");
        }
        this.sections = sections;
        validateContinuousHullShape();
    }

    public double getCanoeHeight() {
        double canoeHeight = 0;
        for (HullSection section : sections) {
            // Convert the hullShapeFunction to UnivariateFunction for compatibility with Apache Commons Math
            // Need to negate the function as BrentOptimizer finds the min, and we want the max
            UnivariateFunction profileCurve = section.getProfileCurve();

            // Use BrentOptimizer to find the maximum value of the hull shape function on [start, end]
            UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
            UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(profileCurve);
            SearchInterval searchInterval = new SearchInterval(section.getStart(), section.getEnd());

            // Optimize (find minimum of the negated function, which corresponds to the maximum of the original function)
            UnivariatePointValuePair result = optimizer.optimize(
                    MaxEval.unlimited(),
                    objectiveFunction,
                    searchInterval
            );

            double sectionHeight = result.getValue();

            if (sectionHeight < canoeHeight)
                canoeHeight = sectionHeight;
        }
        return canoeHeight;
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

