package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * The canoe model consists of a hull, defining the properties of the canoe itself
 * In addition, the canoe may have any amount of external loads on it
 * External loads in typically real-world cases include paddlers, cargo, stand that hold the canoe up, buoyancy, etc.
 * External loads may be individual loads (point load or uniformly distributed loads), or a discrete distribution
 */

@Getter @EqualsAndHashCode
public class Canoe
{
    private final ArrayList<Load> externalLoads;
    private final ArrayList<DiscreteLoadDistribution> externalLoadDistributions;
    @Setter
    private Hull hull;

    public Canoe() {
        this.hull = null;
        this.externalLoads = new ArrayList<>();
        this.externalLoadDistributions = new ArrayList<>();
    }

    public AddLoadResult addLoad(Load load) {
        if (load instanceof PointLoad pLoad) {
            // Do not add the load if it is zero valued unless if is a support
            // Zero-valued supports are still added as markers for the model and ListView
            if (pLoad.getValue() == 0)
                if (!pLoad.isSupport())
                    return AddLoadResult.ADDED;
                else
                    pLoad.setForce(0.00); // In case mag is -0 so that the negative doesn't display to the user

            // Search for other loads at the same position, and combine their magnitudes
            for (PointLoad existingPLoad : getPLoads()) {
                if (existingPLoad.getX() == pLoad.getX() && !pLoad.isSupport()) {
                    double newForce = existingPLoad.getValue() + pLoad.getValue();
                    if (newForce == 0) {
                        removeLoad(externalLoads.indexOf(existingPLoad));
                        return AddLoadResult.REMOVED;
                    }
                    ((PointLoad) (externalLoads.get(externalLoads.indexOf(existingPLoad)))).setForce(newForce);
                    existingPLoad.setForce(newForce);
                    return AddLoadResult.COMBINED;
                }
            }
        }

        externalLoads.add(load);
        externalLoads.sort(Comparator.comparingDouble(Load::getX));
        return AddLoadResult.ADDED;
    }

    public void removeLoad(int index) {
        externalLoads.remove(index);
    }

    @JsonIgnore
    public int getMaxLoadIndex() {
        if (externalLoads.isEmpty()) {
            return -1;
        }

        int maxIndex = 0;
        double max = 0;
        for (int i = 0; i < externalLoads.size(); i++) {
            double mag = Math.abs(externalLoads.get(i).getValue());
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
    public List<UniformlyDistributedLoad> getDLoads() {
        List<UniformlyDistributedLoad> dLoads = new ArrayList<>();
        for (Load load : externalLoads) {
            if (load instanceof UniformlyDistributedLoad dLoad)
                dLoads.add(dLoad);
        }
        return dLoads;
    }

    /**
     * @return the weight of all point loads and uniform distributed loads which doesn't include the canoe's self-weight
     */
    @JsonIgnore
    public double getExternalWeight() {

        double externalWeight = 0;
        // External loads (sign included in magnitude)
        for (PointLoad pLoad : getPLoads()) {
            externalWeight += pLoad.getValue();
        }
        for (UniformlyDistributedLoad dLoad : getDLoads()) {
            externalWeight += dLoad.getMagnitude() * (dLoad.getRx() - dLoad.getX());
        }
        return externalWeight;
    }

    /**
     * @return the total weight of the canoe including self-weight and external loading
     */
    @JsonIgnore
    public double getTotalWeight() {
        return hull.getSelfWeight() + getExternalWeight();
    }


    /**
     * @return a TreeSet (sorted ascending, no duplicates) of endpoints for loads external to the canoe's self-weight
     * Note: results are x-coordinates in metres
     */
    @JsonIgnore
    public TreeSet<Double> getExternalSectionEndpoints() {

        // Add all point load x coords and endpoints of distributed loads
        TreeSet<Double> s = new TreeSet<>();
        for (Load l : externalLoads)
        {
            s.add(l.getX());
            if (l instanceof UniformlyDistributedLoad distributedLoad)
                s.add(distributedLoad.getRx());
        }

        return s;
    }

    /**
     * @return a TreeSet of all the internal loading, external, loading, and canoe endpoints
     */
    @JsonIgnore
    public TreeSet<Double> getAllEndpoints() {
        TreeSet<Double> s = new TreeSet<>();
        s.addAll(hull.getHullSectionEndPoints());
        s.addAll(getExternalSectionEndpoints());
        s.add(hull.getSection().getX());
        s.add(hull.getSection().getRx());
        return s;
    }

    /**
     * @return a sorted list of all external point loads and uniformly distributed loads, and external load distributions
     */
    @JsonIgnore
    public List<Load> getAllLoads() {
        List<Load> loads = new ArrayList<>(externalLoads);
        if (hull.getSelfWeightDistribution() != null)
            loads.add(hull.getSelfWeightDistribution());
        loads.addAll(externalLoadDistributions);

        // Define the order to sort by type
        Map<Class<? extends Load>, Integer> classOrder = new HashMap<>();
        classOrder.put(PointLoad.class, 0);
        classOrder.put(DiscreteLoadDistribution.class, 1);
        classOrder.put(UniformlyDistributedLoad.class, 2);

        // Sort by type, and then by x
        loads.sort(Comparator.comparingInt(load -> classOrder.getOrDefault(load.getClass(), -1)));
        loads.sort(Comparator.comparingDouble(Load::getX));

        return loads;
    }
}

