package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.*;
import java.util.*;

@Getter @Setter @EqualsAndHashCode
public class Canoe
{
    private double length;
    private final ArrayList<Load> externalLoads;
    @JsonIgnore // TODO: Implement YAML Model
    private List<DiscreteLoadDistribution> externalLoadDistributions;
    @JsonIgnore // TODO: Implement YAML Model
    private DiscreteLoadDistribution selfWeightDistribution;
    @JsonIgnore
    private double concreteDensity; // [kg/m^3]
    @JsonIgnore
    private double bulkheadDensity; // [kg/m^3]

    public Canoe(double concreteDensity, double bulkheadDensity) {
        this.selfWeightDistribution = null;
        this.externalLoadDistributions = new ArrayList<>();
        this.externalLoads = new ArrayList<>();
        this.concreteDensity = concreteDensity;
        this.bulkheadDensity = bulkheadDensity;
        this.length = 0;
    }

    // TODO: ideally this should also check that the derivative of the section endpoints at each piecewise function is equal to guarantee smoothness
    // Need to update the hard-coded 2024-Shark-Bait model to this first
    private void validateContinuousHullShape(List<HullSection> sections) {

        for (int i = 0; i < sections.size() - 1; i++)
        {
            HullSection current = sections.get(i);
            HullSection next = sections.get(i + 1);
            double currentEnd = current.getEnd();
            double nextStart = next.getStart();
            double currentEndDepth = current.getProfileCurveXYPlane().value(currentEnd);
            double nextStartDepth = next.getProfileCurveXYPlane().value(nextStart);
            if (Math.abs(currentEndDepth - nextStartDepth) > 1e-6) // small tolerance for discontinuities in case of floating point errors
                throw new IllegalArgumentException("Hull shape functions must form a continuous curve at section boundaries.");
        }
    }

//    private void validateConcaveUpHullShape() {
//        for (HullSection current : sections)
//        {
//            // Get endpoints and their derivatives
//            double start = current.getX();
//            double end = current.getRX();
//            UnivariateFunction firstDerivative = MathUtils.differentiate(current.getProfileCurveXYPlane());
//            UnivariateFunction secondDerivative = MathUtils.differentiate(firstDerivative);
//
//            // Check second derivative positivity at endpoints
//            if (secondDerivative.value(start) < 0 || secondDerivative.value(end) < 0) {
//                throw new IllegalArgumentException("Second derivative must be positive at section endpoints.");
//            }
//
//            // Use Brent's optimizer to find where the overall second derivative might have zeros
//            BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
//            SearchInterval searchInterval = new SearchInterval(start, end);
//            double concaveDownPoint = optimizer.optimize(
//                    new MaxEval(100),
//                    new UnivariateObjectiveFunction(firstDerivative),
//                    searchInterval
//            ).getPoint();
//
//            // Check overall concavity condition
//            if (secondDerivative.value(concaveDownPoint) > 0) {
//                throw new IllegalArgumentException("Hull shape must be concave down at section boundaries.");
//            }
//        }
//    }

    /**
     * Validates that the hull shape function is non-positive on its domain [start, end]
     * This convention allows waterline height h for a floating hull to be a distance below the top of the null at h = 0
     * Uses calculus to avoid checking all points individually by checking only critical points and domain endpoints
     */
    private void validateFloorThickness(List<HullSection> sections) {

        double canoeHeight = getMaxHeight();

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
    public void validateWallThickness(List<HullSection> sections) {
        for (HullSection section : sections)
        {
            if (section.getThickness() > section.getZWidth() / 2)
                throw new IllegalArgumentException("Hull walls would be greater than the width of the canoe");
        }
    }

    public AddLoadResult addLoad(Load l) {
        if (l instanceof PointLoad) {
            // Do not add the load if it is zero valued unless if is a support
            // Zero-valued supports are still added as markers for the model and ListView
            if (l.getMag() == 0)
                if (!((PointLoad) l).isSupport())
                    return AddLoadResult.ADDED;
                else
                    l.setMag(0.00); // In case mag is -0 so that the negative doesn't display to the user

            // Search for other loads at the same position, and combine their magnitudes
            for (PointLoad pLoad : getPLoads()) {
                if (pLoad.getX() == l.getX() && !((PointLoad) l).isSupport()) {
                    double newMag = pLoad.getMag() + l.getMag();
                    if (newMag == 0) {
                        removeLoad(externalLoads.indexOf(pLoad));
                        return AddLoadResult.REMOVED;
                    }
                    externalLoads.get(externalLoads.indexOf(pLoad)).setMag(newMag);
                    pLoad.setMag(newMag);
                    return AddLoadResult.COMBINED;
                }
            }
            externalLoads.add(l);
            externalLoads.sort(Comparator.comparingDouble(Load::getX));
        }

        else if (l instanceof UniformDistributedLoad) {
            externalLoads.add(l);
            externalLoads.sort(Comparator.comparingDouble(Load::getX));
        }

        return AddLoadResult.ADDED;
    }

    public void removeLoad(int index) {
        externalLoads.remove(index);
    }

    public void clearLoads() {
        externalLoads.clear();
    }

    @JsonIgnore
    public int getMaxLoadIndex() {
        if (externalLoads.isEmpty()) {
            return -1;
        }

        int maxIndex = 0;
        double max = 0;
        for (int i = 0; i < externalLoads.size(); i++) {
            double mag = Math.abs(externalLoads.get(i).getMag());
            if (mag > max) {
                max = mag;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    @JsonIgnore
    public List<PointLoad> getPLoads() {
        List<PointLoad> pLoads = new ArrayList<>();
        for (Load load : externalLoads) {
            if (load instanceof PointLoad pLoad)
                pLoads.add(pLoad);
        }
        return pLoads;
    }

    @JsonIgnore
    public List<UniformDistributedLoad> getDLoads() {
        List<UniformDistributedLoad> dLoads = new ArrayList<>();
        for (Load load : externalLoads) {
            if (load instanceof UniformDistributedLoad dLoad)
                dLoads.add(dLoad);
        }
        return dLoads;
    }

    @JsonIgnore
    public double getMaxHeight() {
        double canoeHeight = 0;
        for (HullSection section : getInternalSections())
        {
            // Find the sections minimum
            // Function is negated as BrentOptimizer looks for the maximum
            UnivariateObjectiveFunction objectiveFunction = new UnivariateObjectiveFunction(x -> -section.getProfileCurveXYPlane().value(x));
            SearchInterval searchInterval = new SearchInterval(section.getStart(), section.getEnd());
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
     * @return the canoe hull self-weight, in kN with a negative sign representing the downward load
     * Note: this includes bulkheads weight if specified with fillBulkhead
     */
    @JsonIgnore
    public double getSelfWeight() {
        double selfWeight = 0;
        for (HullSection section : getInternalSections()) {
            selfWeight -= section.getWeight();
        }
        return selfWeight;
    }

    /**
     * @return the weight of all point loads and uniform distributed loads which doesn't include the canoe's self-weight
     */
    @JsonIgnore
    public double getExternalWeight() {

        double externalWeight = 0;
        // External loads (sign included in magnitude)
        for (PointLoad pLoad : getPLoads()) {
            externalWeight += pLoad.getMag();
        }
        for (UniformDistributedLoad dLoad : getDLoads()) {
            externalWeight += dLoad.getMag() * (dLoad.getRx() - dLoad.getX());
        }
        return externalWeight;
    }

    /**
     * @return the total weight of the canoe including self-weight and external loading
     */
    @JsonIgnore
    public double getTotalWeight() {
        return getSelfWeight() + getExternalWeight();
    }

    /**
     * @return the total volume of the canoe by summing up the volumes of all sections.
     */
    @JsonIgnore
    public double getTotalVolume() {
        return getInternalSections().stream().mapToDouble(HullSection::getVolume).sum();
    }

    /**
     * @return the total concrete volume of the canoe by summing up the concrete volumes of all sections.
     */
    @JsonIgnore
    public double getConcreteVolume() {
        return getInternalSections().stream().mapToDouble(HullSection::getConcreteVolume).sum();
    }

    /**
     * @return the total bulkhead volume of the canoe by summing up the bulkhead volumes of all sections.
     */
    @JsonIgnore
    public double getBulkheadVolume() {
        return getInternalSections().stream().mapToDouble(HullSection::getBulkheadVolume).sum();
    }


    public List<HullSection> getInternalSections() {
        return selfWeightDistribution.getSectionToLoadMap()
                .keySet()
                .stream()
                .map(HullSection.class::cast)
                .toList();
    }

    /**
     * @return the canoe lengthwise endpoints [0, L] always included in case they were missed earlier
     */
    public TreeSet<Double> getEndpoints() {
        TreeSet<Double> s = new TreeSet<>();
        s.add(0.0);
        s.add(length);
        return s;
    }

    /**
     * @return a TreeSet (sorted ascending, no duplicates) of endpoints for loads external to the canoe's self-weight
     * Note: results are x-coordinates in metres
     */
    @JsonIgnore
    public TreeSet<Double> getExternalSectionEndPoints() {

        // Add all point load x coords and endpoints of distributed loads
        TreeSet<Double> s = new TreeSet<>();
        for (Load l : externalLoads)
        {
            s.add(l.getX());
            if (l instanceof UniformDistributedLoad distributedLoad)
                s.add(distributedLoad.getRx());
        }

        return s;
    }

    /**
     * @return a TreeSet of endpoints for internally defined endpoints.
     * This can be theoretically scaled to a max to approach the true accuracy of the actual canoe
     */
    @JsonIgnore
    public TreeSet<Double> getInternalSectionEndPoints() {

        // Add endpoints of hull sections if they are defined
        TreeSet<Double> s = new TreeSet<>();
        if (!getInternalSections().isEmpty())
        {
            for (HullSection section : getInternalSections()) {s.add(section.getStart());}
            s.add(getInternalSections().getLast().getEnd());
        }

        return s;
    }

    /**
     * @return a TreeSet of all the internal loading, external, loading, and canoe endpoints
     */
    public TreeSet<Double> getAllEndpoints() {
        TreeSet<Double> s = new TreeSet<>();
        s.addAll(getInternalSectionEndPoints());
        s.addAll(getExternalSectionEndPoints());
        s.addAll(getEndpoints());
        return s;
    }

    /**
     * Setting the length will automatically reset self-weight as otherwise section lengths may not add up to the total canoe length
     * @param length the length to set
     */
    public void setLength(double length){
        this.length = length;
        this.selfWeightDistribution = null;
    }

    /**
     * @param hullSections the sections to set the self weight distribution from
     */
    public void setSelfWeightDistribution(List<HullSection> hullSections) {
        DiscreteLoadDistribution discreteLoadDistribution = new DiscreteLoadDistribution(hullSections);
        if (discreteLoadDistribution.getX() != 0 || discreteLoadDistribution.getX() != length)
            throw new IllegalArgumentException("Self-weight distribution should cover the entire canoe length");
        else
            this.selfWeightDistribution = new DiscreteLoadDistribution(hullSections);
    }
}

