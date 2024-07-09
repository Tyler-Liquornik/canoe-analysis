package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.utils.SortingUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;

/**
 * The canoe model consists of a hull, defining the properties of the canoe itself including geometry and material properties
 * The hull has an associated weight distribution to give the canoe mass
 * In addition, the canoe may have any amount of external loads on it
 * External loads in typically real-world cases include paddlers, cargo, stand that hold the canoe up, buoyancy, etc.
 * External loads may be any type of load from simple point loads to complex distributions
 */

@Getter @EqualsAndHashCode
public class Canoe
{
    @JsonProperty("loads")
    private final ArrayList<Load> loads;
    @JsonProperty("hull")
    private Hull hull;

    public Canoe() {
        this.hull = null;
        this.loads = new ArrayList<>();
    }

    public void setHull(Hull hull) {
        if (hull.getLength() < 0.01)
            throw new IllegalArgumentException("Hull must be at least 0.01m in length");
        this.hull = hull;
    }

    public AddLoadResult addLoad(Load load) {
        if (!isLoadWithinCanoeLength(load))
            throw new IllegalArgumentException("Load must be contained inside the canoe's length");

        if (load instanceof PointLoad pLoad) {
            // Do not add the load if it is zero valued unless if is a support
            // Zero-valued supports are still added as markers for the model and ListView
            if (pLoad.getMaxSignedValue() == 0)
                if (!pLoad.isSupport())
                    return AddLoadResult.ADDED;
                else
                    pLoad.setForce(0.00); // In case mag is -0 so that the negative doesn't display to the user

            // Search for other loads at the same position, and combine their magnitudes
            for (PointLoad existingPLoad : getAllLoadsOfType(PointLoad.class)) {
                if (existingPLoad.getX() == pLoad.getX() && !pLoad.isSupport()) {
                    double newForce = existingPLoad.getMaxSignedValue() + pLoad.getMaxSignedValue();
                    if (newForce == 0) {
                        loads.remove(existingPLoad);
                        return AddLoadResult.REMOVED;
                    }
                    ((PointLoad) (loads.get(loads.indexOf(existingPLoad)))).setForce(newForce);
                    existingPLoad.setForce(newForce);
                    return AddLoadResult.COMBINED;
                }
            }
        }

        loads.add(load);
        SortingUtils.sortLoads(loads);
        return AddLoadResult.ADDED;
    }

    /**
     * Check that a load is positioned within the canoe's hull
     * @param load the load to check
     * @return if the validation is passed
     */
    private boolean isLoadWithinCanoeLength(Load load) {
        double hullLength = getHull().getLength();

        // Check if a point load has its position inside the hull length
        if (load instanceof PointLoad pLoad) {
            return 0 <= pLoad.getX() && pLoad.getX() <= hullLength;
        }

        // Check if a load distribution has its full interval inside the hull length
        if (load instanceof LoadDistribution dist) {
            boolean isDistStartWithinBounds = 0 <= dist.getX() && dist.getX() <= hullLength;
            boolean isDistEndWithinBounds = 0 <= dist.getSection().getRx() && dist.getSection().getRx() <= hullLength;
            return isDistStartWithinBounds || isDistEndWithinBounds;
        }

        return false;
    }

    @JsonIgnore
    public double getMaxLoadValue() {
        if (getAllLoads().isEmpty()) {
            return -1;
        }
        
        double maxValue = 0;
        for (Load load : getAllLoads()) {
            double currValue = Math.abs(load.getMaxSignedValue());
            if (currValue > maxValue) {
                maxValue = currValue;
            }
        }
        return maxValue;
    }

    /**
     * @return the weight of all point loads and uniform distributed loads which doesn't include the canoe's self-weight
     */
    @JsonIgnore
    public double getExternalWeight() {

        double externalWeight = 0;
        // External loads (sign included in magnitude)
        for (PointLoad pLoad : getAllLoadsOfType(PointLoad.class)) {
            externalWeight += pLoad.getMaxSignedValue();
        }
        for (UniformLoadDistribution dLoad : getAllLoadsOfType(UniformLoadDistribution.class)) {
            externalWeight += dLoad.getMagnitude() * (dLoad.getRx() - dLoad.getX());
        }
        return externalWeight;
    }

    /**
     * @return the total weight of the canoe including self-weight and external loading
     */
    @JsonIgnore
    public double getTotalWeight() {
        return hull.getWeight() + getExternalWeight();
    }


    /**
     * @return a TreeSet (sorted ascending, no duplicates) of endpoints for loads external to the canoe's self-weight
     * Note: results are x-coordinates in metres
     */
    @JsonIgnore
    public TreeSet<Double> getExternalSectionEndpoints() {

        // Add all point load x coords and endpoints of distributed loads
        TreeSet<Double> s = new TreeSet<>();
        for (Load l : loads)
        {
            s.add(l.getX());
            if (l instanceof UniformLoadDistribution distributedLoad)
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
     * @return a sorted list including external pLoads and dLoads, and external loadDists and the hull loadDist
     */
    @JsonIgnore
    public List<Load> getAllLoads() {
        List<Load> loads = new ArrayList<>(this.loads);
        if (hull != null && hull.getSelfWeightDistribution() != null)
            loads.add(hull.getSelfWeightDistribution());
        SortingUtils.sortLoads(loads);
        return loads;
    }

    /**
     * @param <T> the type of load
     * @param clazz the class literal for type T
     * @return a list of all loads of specified subtype (i.e. PointLoad, UniformLoadDistribution, etc.)
     */
    @JsonIgnore
    public <T extends Load> List<T> getAllLoadsOfType(Class<T> clazz) {
        return getAllLoads().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }

    /**
     * @return all loads with piecewise continuous loads discretized
     * This is for the TreeView, which will show the user loads discretized to avg values
     */
    @JsonIgnore
    public List<Load> getAllLoadsDiscretized() {
        List<Load> loads = new ArrayList<>(this.loads.stream()
                .map(load -> load instanceof PiecewiseContinuousLoadDistribution piecewise ?
                        DiscreteLoadDistribution.fromPiecewiseContinuous(piecewise.type, piecewise) : load)
                .toList());

        // Add the hull self-weight load separately
        if (hull != null && hull.getSelfWeightDistribution() != null) {
            loads.add(DiscreteLoadDistribution.fromHull(hull));
        }
        SortingUtils.sortLoads(loads);
        return loads;
    }

    /**
     * @return the net force of all external loads and the hull
     */
    @JsonIgnore
    public double getNetForce() {
        return getAllLoads().stream().mapToDouble(Load::getForce).sum();
    }
}

