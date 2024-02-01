package com.wecca.canoeanalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving system equations
 */
public final class SystemSolver {

    //Offset of the stands from either end of the canoe (assume equal offset on either side)
    private final static int STAND_OFFSET = 0;

    //Private constructor to enable utility class
    private SystemSolver() {}

    /**
     * Solve the "stand" system to find point loads at ends of canoe, assuming loads already on canoe.
     * Note: the weight of the canoe must be added as one/more distributed load(s).
     * @param canoe the canoe system to solve.
     * @return the resulting point loads.
     */
    public static List<PointLoad> solveStandSystem(Canoe canoe) {
        List<PointLoad> pointLoads = new ArrayList<>();
        //Transform the distributed loads into point loads
        for (UniformDistributedLoad dLoad : canoe.dLoads) {
            double dLoadLength = dLoad.getRX() - dLoad.getLX();
            double pLoadMagnitude = dLoad.getW() * dLoadLength;
            double pLoadPosition = dLoad.getLX() + (dLoadLength / 2);
            pointLoads.add(new PointLoad(pLoadMagnitude, pLoadPosition));
        }
        pointLoads.addAll(canoe.pLoads);

        //Find the sum of moments from the start and the total magnitude of combined point loads
        double momentSum = 0;
        double sumOfPointLoads = 0;
        for (PointLoad pLoad : pointLoads) {
            sumOfPointLoads += pLoad.getMag();
            momentSum += (pLoad.getMag() * pLoad.getX());
        }

        //Resulting forces combine to counteract the moments and total combined point load
        double forceEnd = -1 * momentSum / canoe.getLen();
        double forceStart = -1 * sumOfPointLoads - forceEnd;

        //Create and return resulting loads
        PointLoad pLoadStart = new PointLoad(forceStart, STAND_OFFSET);
        PointLoad pLoadEnd = new PointLoad(forceEnd, canoe.getLen() - STAND_OFFSET);
        pointLoads.clear();
        pointLoads.addAll(Arrays.asList(pLoadStart, pLoadEnd));

        return pointLoads;
    }
}
