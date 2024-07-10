package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.LoadUtils;
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
        LoadUtils.sortLoads(loads);
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

    /**
     * @return the maximum signed value of all loads (not including hull self-weight)
     */
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
     * @return a TreeSet of all the internal loading, external, loading, and hull sections endpoints
     */
    @JsonIgnore
    public TreeSet<Double> getSectionEndpoints() {
        TreeSet<Double> endPoints = new TreeSet<>();
        endPoints.addAll(hull.getHullSectionEndPoints());
        for (Load l : loads)
        {
            endPoints.add(l.getX());
            if (l instanceof LoadDistribution dist)
                endPoints.add(dist.getSection().getRx());
        }
        return endPoints;
    }

    /**
     * @return a sorted list including external pLoads and dLoads, and external loadDists and the hull loadDist
     */
    @JsonIgnore
    public List<Load> getAllLoads() {
        return LoadUtils.addHullPreserveLoadSorting(loads, hull);
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
     */
    @JsonIgnore
    public List<Load> getAllLoadsDiscretized() {
        return LoadUtils.discretizeLoads(LoadUtils.addHullPreserveLoadSorting(loads, hull));
    }

    /**
     * @return the net force of all external loads and the hull
     */
    @JsonIgnore
    public double getNetForce() {
        return getAllLoads().stream().mapToDouble(Load::getForce).sum();
    }
}