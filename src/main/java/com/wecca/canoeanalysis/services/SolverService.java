package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving system equations
 */
public class SolverService {

    // Offset of the stands from either end of the canoe (assume equal offset on either side)
    // Hard coded for now with stands right on the edges (0 offset), subject to change later
    private final static int STAND_OFFSET = 0;
    private final static SimpsonIntegrator integrator = new SimpsonIntegrator();

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
        double forceEnd = -1 * momentSum / canoe.getHull().getLength();
        double forceStart = -1 * sumOfPointLoads - forceEnd;

        // Create and return resulting loads
        PointLoad pLoadStart = new PointLoad(forceStart, STAND_OFFSET, true);
        PointLoad pLoadEnd = new PointLoad(forceEnd, canoe.getHull().getLength() - STAND_OFFSET, true);
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
     * Solve the overall floating case of the canoe
     * @param canoe the canoe with a give hull geometry, material densities, and external loading to solve
     * @return a linked list of dLoads corresponding to the distributed buoyancy for that section in equilibrium in kN/m
     */
    public static List<UniformDistributedLoad> solveFloatingSystem(Canoe canoe) {
        double waterLine = getEquilibriumWaterLine(canoe);
        List<Double> buoyantForces = getBuoyantForceOnAllSections(waterLine, canoe);
        List<UniformDistributedLoad> loads = new ArrayList<>();
        for (int i = 0; i < buoyantForces.size(); i++) {
            HullSection section = canoe.getHull().getHullSections().get(i);
            double sectionLength = section.getLength();
            double dLoadMag = buoyantForces.get(i) / sectionLength;
            loads.add(new UniformDistributedLoad(dLoadMag, section.getX(), section.getRx()));
        }
        return loads;
    }

    /**
     * Guess a waterline and get the volume of the submerged portion of the section
     * @param waterline the level below y = 0 of the waterline guess
     * @param section the section to calculate the buoyant force on
     * @return the submerged volume in m^3 at the given waterline guess
     */
    private static double getSubmergedVolume(double waterline, HullSection section) {

        double submergedArea = -integrator.integrate(1000, x -> (section.getProfileCurve().value(x) + waterline), section.getX(), section.getRx());
        return submergedArea * section.getWidth();
    }

    /**
     * Guess a waterline and get the buoyant force on the section at that waterline
     * @param waterline the level below y = 0 of the waterline guess
     * @param section the section to calculate the buoyant force on
     * @return the buoyant force in kN
     */
    private static double getBuoyantForceOnSection(double waterline, HullSection section) {
        return (PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() * getSubmergedVolume(waterline, section)) / 1000.0;
    }

    /**
     * Get the buoyant force on all section
     * @param waterLine the level below y = 0 of the waterline guess
     * @param canoe the canoe with sections to calculate the buoyancy forces of
     * @return a linked list in of buoyancy forces in the same order as the linked list of canoe sections
     */
    private static List<Double> getBuoyantForceOnAllSections(double waterLine, Canoe canoe) {
        List<Double> buoyantForces = new ArrayList<>();
        for (HullSection section : canoe.getHull().getHullSections()) {
            double force = getBuoyantForceOnSection(waterLine, section);
            buoyantForces.add(force);
        }
        return buoyantForces;
    }

    /**
     * Guess a waterline and get the buoyant force on the whole canoe
     * @param canoe the canoe with a given hull geometry
     * @param waterLine the level below y = 0 of the waterline guess
     * @return the total buoyant in kN at the given waterline guess
     */
    private static double getBuoyantForceOnCanoe(Canoe canoe, double waterLine) {
        List<HullSection> sections = canoe.getHull().getHullSections();
        double totalBuoyantForce = 0;
        for (HullSection section : sections) {totalBuoyantForce += getBuoyantForceOnSection(waterLine, section);}
        return totalBuoyantForce;
    }

    /**
     * Iteratively solve for the equilibrium of the floating canoe
     * This works by matching the total canoe internal/external weight with the buoyancy
     * @param canoe the canoe with defined internal/external loads to get the waterline height for
     * @return the waterline height of floating equilibrium
     */
    private static double getEquilibriumWaterLine(Canoe canoe) {
        double totalWeight = canoe.getTotalWeight();
        double totalBuoyancy = 0.0;
        double waterLine = 0.0;
        while (totalBuoyancy < totalWeight) {
            totalBuoyancy = getBuoyantForceOnCanoe(canoe, waterLine);
            waterLine += 0.01;
        }
        return waterLine;
    }

    // TODO: Consult Design and Analysis team for details. Strategy for this has not yet been developed.
    public static List<Load> solveSubmergedSystem(Canoe canoe) {
        List<Load> loads = new ArrayList<>();
        return null;
    }
}

