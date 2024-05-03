package com.wecca.canoeanalysis.utility;

import com.wecca.canoeanalysis.models.Canoe;
import com.wecca.canoeanalysis.models.PointLoad;
import com.wecca.canoeanalysis.models.UniformDistributedLoad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving system equations
 */
public final class SystemSolver {

    // Offset of the stands from either end of the canoe (assume equal offset on either side)
    // Hard coded for now with stands right on the edges (0 offset), subject to change later
    private final static int STAND_OFFSET = 0;

    // Private constructor to enable utility class
    private SystemSolver() {}

    /**
     * Convert a list of UniformDistributedLoads to a list of PointLoads.
     * @param loads list of distributed loads.
     * @return list of point loads.
     */
    private static List<PointLoad> distributedToPoint(List<UniformDistributedLoad> loads) {
        List<PointLoad> pointLoads = new ArrayList<>();

        for (UniformDistributedLoad load : loads) {
            double dLoadLength = load.getRX() - load.getLX();
            double pLoadMagnitude = load.getW() * dLoadLength;
            double pLoadPosition = load.getLX() + (dLoadLength / 2);
            pointLoads.add(new PointLoad(pLoadMagnitude, pLoadPosition));
        }

        return pointLoads;
    }

    /**
     * Solve the "stand" system to find point loads at ends of canoe, assuming loads already on canoe.
     * Note: the weight of the canoe must be added as one/more distributed load(s).
     * @param canoe the canoe system to solve.
     * @return the resulting point loads.
     */
    public static List<PointLoad> solveStandSystem(Canoe canoe) {
        List<PointLoad> pointLoads = new ArrayList<>();
        // Transform the distributed loads into point loads
        pointLoads.addAll(distributedToPoint(canoe.dLoads));
        pointLoads.addAll(canoe.pLoads);

        // Find the sum of moments from the start and the total magnitude of combined point loads
        double momentSum = 0;
        double sumOfPointLoads = 0;
        for (PointLoad pLoad : pointLoads) {
            sumOfPointLoads += pLoad.getMag();
            momentSum += (pLoad.getMag() * pLoad.getX());
        }

        // Resulting forces combine to counteract the moments and total combined point load
        double forceEnd = -1 * momentSum / canoe.getLen();
        double forceStart = -1 * sumOfPointLoads - forceEnd;

        // Create and return resulting loads
        PointLoad pLoadStart = new PointLoad(forceStart, STAND_OFFSET);
        PointLoad pLoadEnd = new PointLoad(forceEnd, canoe.getLen() - STAND_OFFSET);
        pointLoads.clear();
        pointLoads.addAll(Arrays.asList(pLoadStart, pLoadEnd));

        return pointLoads;
    }

    //TODO: later
    public static List<PointLoad> solveFloatingSystem(Canoe canoe) {
        List<PointLoad> pointLoads = new ArrayList<>();
        pointLoads.addAll(distributedToPoint(canoe.dLoads));
        pointLoads.addAll(canoe.pLoads);

        return pointLoads;
    }
}
