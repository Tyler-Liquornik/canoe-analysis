package com.wecca.canoeanalysis.models.canoe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.data.SolveType;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.LoadUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

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
    private List<Load> loads;
    @JsonProperty("hull")
    private Hull hull;

    // Caching this for each access within each session
    @Setter
    private double sessionMaxShear;

    @Setter
    private SolveType solveType;

    public Canoe() {
        this.hull = null;
        this.loads = new ArrayList<>();
        this.sessionMaxShear = 0.0;
        this.solveType = SolveType.UNSOLVED;
    }

    public void setHull(Hull hull) {
        if (hull.getLength() < 2)
            throw new IllegalArgumentException("Hull must be at least 2m in length");
        this.hull = hull;
    }

    /**
     * Add a load to the canoe, combining pLoads and dLoads where their x/rx match up (complex distributions added with no extra logic)
     * @param load the load to add
     * @return the resultof adding the load (ADDED | REMOVED | COMBINED)
     */
    public AddLoadResult addLoad(Load load) {
        if (!isLoadWithinCanoeLength(load))
            throw new IllegalArgumentException("Load must be contained inside the canoe's length");

        // Combine pLoads of the same x value
        if (load instanceof PointLoad pLoad) {
            // Do not add the load if it is zero valued unless it's a support (we don't want to ignore zero-valued solutions)
            if (pLoad.getMaxSignedValue() == 0)
                if (!pLoad.isSupport())
                    return AddLoadResult.ADDED;
                else
                    pLoad.setForce(0.00); // In case mag is -0 so that the negative doesn't display to the user

            for (PointLoad existingPLoad : getAllLoadsOfType(PointLoad.class)) {
                if (existingPLoad.getX() == pLoad.getX() && !pLoad.isSupport()) {
                    double newForce = existingPLoad.getForce() + pLoad.getForce();
                    if (newForce == 0) {
                        loads.remove(existingPLoad);
                        return AddLoadResult.REMOVED;
                    }
                    existingPLoad.setForce(newForce);
                    return AddLoadResult.COMBINED;
                }
            }
        }
        // Combine dLoads at the on the same interval [x, rx]
        if (load instanceof UniformLoadDistribution dLoad) {
            for (UniformLoadDistribution existingDLoad : getAllLoadsOfType(UniformLoadDistribution.class)) {
                if (existingDLoad.getX() == dLoad.getX() && existingDLoad.getRx() == dLoad.getRx()) {
                    double newMag = existingDLoad.getMagnitude() + dLoad.getMagnitude();
                    if (newMag == 0) {
                        loads.remove(existingDLoad);
                        return AddLoadResult.REMOVED;
                    }
                    existingDLoad.setMagnitude(newMag);
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
        List<Load> allLoads = getAllLoads();
        if (allLoads.isEmpty())
            return -1;
        double maxValue = 0;
        for (Load load : allLoads) {
            double currValue = Math.abs(load.getMaxSignedValue());
            if (currValue > maxValue)
                maxValue = currValue;
        }
        return maxValue;
    }

    /**
     * Critical points are points of interest which can be visualized as points where the equation of the load distribution changes
     * @return a TreeSet of all the internal loading, external loading, and hull sections endpoints
     */
    @JsonIgnore
    public TreeSet<Double> getCriticalPointSet() {
        TreeSet<Double> endPoints = new TreeSet<>();
        if (!hull.getSideViewSegments().isEmpty()) {
            for (CubicBezierFunction section : hull.getSideViewSegments()) {
                endPoints.add(section.getX());
            }
            endPoints.add(hull.getSideViewSegments().getLast().getRx());
        }
        for (Load l : loads) {
            endPoints.add(l.getX());
            if (l instanceof LoadDistribution dist)
                endPoints.add(dist.getSection().getRx());
        }
        endPoints.forEach(x -> CalculusUtils.roundXDecimalDigits(x, 10));
        return endPoints;
    }

    /**
     * @return a sorted list including external pLoads and dLoads, and external loadDists and the hull loadDist
     */
    @JsonIgnore
    public List<Load> getAllLoads() {
        return LoadUtils.addHullAsLoad(loads, hull);
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
     * Note: discretization also separates out LoadTypes
     */
    @JsonIgnore
    public List<Load> getAllLoadsDiscretized() {
        return LoadUtils.discretizeLoads(LoadUtils.addHullAsLoad(loads, hull), hull.getHullProperties().getBulkheadMap());
    }

    /**
     * @return the net force of all external loads and the hull
     */
    @JsonIgnore
    public double getNetForce() {
        return getAllLoads().stream().mapToDouble(Load::getForce).sum();
    }

    /**
     * @return the net moment generated by all external loads and the hull's self-weight
     * Moments are calculated about the canoe's midpoint (i.e., xRotation = getHull().getLength() / 2)
     */
    @JsonIgnore
    public double getNetMoment() {
        double xRotation = getHull().getLength() / 2;
        return getAllLoads().stream().mapToDouble(load -> load.getMoment(xRotation)).sum();
    }

    /**
     * @deprecated by new floating solver algorithm which can solve asymmetrical load cases so we no longer need to check for symmetry
     * @return if the canoe, including the hull self-weight and external loads are symmetrical about the canoes lengthwise midpoints
     */
    @Deprecated @JsonIgnore
    public boolean isSymmetricallyLoaded() {
        // Check if the hull's self-weight distribution is symmetrical
        double hullLength = hull.getLength();
        double midpoint = hullLength / 2.0;
        boolean isHullSymmetrical = CalculusUtils.isSymmetrical(hull.getSelfWeightDistribution());
        if (!isHullSymmetrical)
            return false;

        // Split loads into left and right halves
        List<Load> leftHalf = new ArrayList<>();
        List<Load> rightHalf = new ArrayList<>();
        for (Load load : loads) {
            if ((load instanceof PointLoad pLoad && pLoad.getX() <= midpoint) || (load instanceof UniformLoadDistribution dLoad && dLoad.getX() < midpoint))
                leftHalf.add(load);
            if (load instanceof PointLoad pLoad && pLoad.getX() >= midpoint || load instanceof UniformLoadDistribution dLoad && dLoad.getRx() > midpoint)
                rightHalf.add(load);
        }
        LoadUtils.sortLoads(leftHalf);
        if (leftHalf.size() != rightHalf.size())
            return false;
        List<Load> flippedRightHalf = LoadUtils.flipLoadsFromRightHalf(rightHalf, hullLength);
        LoadUtils.sortLoads(flippedRightHalf);

        // Check that the right half reflected about the midpoint should be the same as the left half for symmetry
        return IntStream.range(0, leftHalf.size()).allMatch(i -> {
            Load lLoad = leftHalf.get(i);
            Load rLoad = flippedRightHalf.get(i);

            // Do not need to deal with piecewise distributions, the user cannot add them as external loads and this case is complicated
            if (lLoad instanceof PointLoad lpLoad && rLoad instanceof PointLoad rpLoad)
                return lpLoad.equals(rpLoad);
            else if (lLoad instanceof UniformLoadDistribution leftDistLoad && rLoad instanceof UniformLoadDistribution rightDistLoad)
                return leftDistLoad.equals(rightDistLoad);
            else if (!((lLoad instanceof PointLoad) || (lLoad instanceof UniformLoadDistribution)))
                throw new IllegalArgumentException("Cannot process loads of type: " + lLoad.getClass());
            return false;
        });
    }
}