package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.utils.MathUtils;
import com.wecca.canoeanalysis.utils.PhysicalConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for solving system equations
 */
public class SolverService {

    // Offset of the stands from either end of the canoe (assume equal offset on either side)
    // Hard coded for now with stands right on the edges (0 offset), subject to change later
    private final static int STAND_OFFSET = 0;

    /**
     * Solve the "stand" system to find point loads at ends of canoe, assuming loads already on canoe.
     * Note: the weight of the canoe must be added as one/more distributed load(s).
     * @param canoe the canoe system to solve.
     * @return the resulting point loads.
     */
    public static List<PointLoad> solveStandSystem(Canoe canoe) {
        List<PointLoad> pointLoads = new ArrayList<>();
        // Transform the distributed loads into point loads
        pointLoads.addAll(distributedToPoint(canoe.getDLoads()));
        pointLoads.addAll(canoe.getPLoads());

        // Find the sum of moments from the start and the total magnitude of combined point loads
        double momentSum = 0;
        double sumOfPointLoads = 0;
        for (PointLoad pLoad : pointLoads) {
            sumOfPointLoads += pLoad.getMag();
            momentSum += (pLoad.getMag() * pLoad.getX());
        }

        // Resulting forces combine to counteract the moments and total combined point load
        double forceEnd = -1 * momentSum / canoe.getLength();
        double forceStart = -1 * sumOfPointLoads - forceEnd;

        // Create and return resulting loads
        PointLoad pLoadStart = new PointLoad(forceStart, STAND_OFFSET, true);
        PointLoad pLoadEnd = new PointLoad(forceEnd, canoe.getLength() - STAND_OFFSET, true);
        pointLoads.clear();
        pointLoads.addAll(Arrays.asList(pLoadStart, pLoadEnd));
        return pointLoads;
    }


    /**
     * Convert a list of UniformDistributedLoads to a list of PointLoads.
     * @param loads list of distributed loads.
     * @return list of point loads.
     */
    private static List<PointLoad> distributedToPoint(List<UniformDistributedLoad> loads) {
        List<PointLoad> pointLoads = new ArrayList<>();

        for (UniformDistributedLoad load : loads) {
            double dLoadLength = load.getRx() - load.getX();
            double pLoadMagnitude = load.getMag() * dLoadLength;
            double pLoadPosition = load.getX() + (dLoadLength / 2);
            pointLoads.add(new PointLoad(pLoadMagnitude, pLoadPosition, false));
        }

        return pointLoads;
    }

    /**
     * Solve the floating system to find the waterline depth and distribute buoyant forces across sections.
     * @param canoe the canoe system to solve.
     * @return list of loads including the distributed buoyant forces.
     */
    public static List<UniformDistributedLoad> solveFloatingSystem(Canoe canoe) {
        double waterlineDepth = calculateWaterlineDepth(canoe);
        List<Double> buoyantForces = calculateBuoyantForces(canoe, waterlineDepth);

        List<UniformDistributedLoad> loads = new ArrayList<>();
        for (int i = 0; i < buoyantForces.size(); i++) {
            CanoeSection section = canoe.getSections().get(i);
            double sectionLength = section.getLength();
            double udlMagnitude = buoyantForces.get(i) / sectionLength;
            loads.add(new UniformDistributedLoad(udlMagnitude, section.getStart(), section.getEnd()));
        }
        return loads;
    }

    // Hull shape function: Define the shape of the canoe's hull
    // This is gonna stay hard coded for now
    // TODO: increase the accuracy of the hardcoded function to have steeper edges
    private static double f(double x, double a, double b) {
        return a * (1 - Math.pow(x / b, 2));
    }

    // Function to calculate the submerged area A(h)
    private static double getSubmergedVolume(double h, CanoeSection section) {
        return MathUtils.integrate(x -> Math.min(section.getHullShapeFunction().apply(x), h), section.getStart(), section.getEnd()) * section.getWidth();
    }

    // Function to calculate the buoyant force for a given depth h
    private static double getBuoyantForceOnSection(double h, CanoeSection section) {
        double submergedVolume = getSubmergedVolume(h, section);
        return PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() * submergedVolume; // Buoyant force in Newtons
    }

    // Modified buoyantForce function to calculate total buoyant force for given depth h
    private static double getBuoyantForceOnSections(double h, List<CanoeSection> sections) {
        double totalBuoyantForce = 0;
        for (CanoeSection section : sections) {
            totalBuoyantForce += getBuoyantForceOnSection(h, section);
        }
        return totalBuoyantForce;
    }

    // Function to calculate the total load on the canoe
    private static double calculateTotalLoad(Canoe canoe) {
        double totalLoad = 0;
        // Self-weight
        for (CanoeSection section : canoe.getSections()) {
            totalLoad += section.getVolume() * section.getOverallDensity(canoe.getConcreteDensity(), canoe.getBulkheadDensity()) * PhysicalConstants.GRAVITY.getValue(); // Section weight in Newtons
        }
        // External loads
        for (PointLoad load : canoe.getPLoads()) {
            totalLoad += load.getMag();
        }
        for (UniformDistributedLoad udl : canoe.getDLoads()) {
            totalLoad += udl.getMag() * (udl.getRx() - udl.getX());
        }
        return totalLoad;
    }

    // Function to calculate the waterline depth considering total load
    private static double calculateWaterlineDepth(Canoe canoe) {
        double totalLoad = calculateTotalLoad(canoe);
        double depth = 0.0;
        while (getBuoyantForceOnSections(depth, canoe.getSections()) < totalLoad) {
            depth += 0.001; // Increment depth in small steps
        }
        return depth;
    }

    // Function to calculate the buoyant forces for each section at the given waterline depth
    private static List<Double> calculateBuoyantForces(Canoe canoe, double waterlineDepth) {
        List<Double> buoyantForces = new ArrayList<>();
        for (CanoeSection section : canoe.getSections()) {
            double force = getBuoyantForceOnSection(waterlineDepth, section);
            buoyantForces.add(force);
        }
        return buoyantForces;
    }

    // TODO: later
    public static List<Load> solveSubmergedSystem(Canoe canoe) {
        List<Load> loads = new ArrayList<>();
        return null;
    }
}

