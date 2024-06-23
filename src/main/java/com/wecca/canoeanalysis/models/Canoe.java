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
@Getter @Setter @EqualsAndHashCode
public class Canoe
{
    private final ArrayList<Load> externalLoads;
    private List<DiscreteLoadDistribution> externalLoadDistributions;
    private Hull hull;

    public Canoe() {
        this.hull = null;
        this.externalLoadDistributions = new ArrayList<>();
        this.externalLoads = new ArrayList<>();
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
            if (l instanceof UniformDistributedLoad distributedLoad)
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
}

