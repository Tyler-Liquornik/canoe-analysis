package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving system equations
 */
public class BeamSolverService {

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
        if (canoe.getHull() != null && canoe.getHull().getSelfWeightDistribution() != null)
            pointLoads.addAll(distributedToPoint(canoe.getHull().getSelfWeightDistributionDiscretized().getLoads()));
        pointLoads.addAll(distributedToPoint(canoe.getAllLoadsOfType(UniformLoadDistribution.class)));
        pointLoads.addAll(canoe.getAllLoadsOfType(PointLoad.class));

        // Find the sum of moments from the start and the total magnitude of combined point loads
        double momentSum = 0;
        double sumOfPointLoads = 0;
        for (PointLoad pLoad : pointLoads) {
            sumOfPointLoads += pLoad.getMaxSignedValue();
            momentSum += (pLoad.getMaxSignedValue() * pLoad.getX());
        }

        // Resulting forces combine to counteract the moments and total combined point load
        double forceEnd = -1 * momentSum / canoe.getHull().getLength();
        double forceStart = -1 * sumOfPointLoads - forceEnd;

        // Create and return resulting loads
        PointLoad pLoadStart = new PointLoad(LoadType.POINT_LOAD_SUPPORT, forceStart, STAND_OFFSET, true);
        PointLoad pLoadEnd = new PointLoad(LoadType.POINT_LOAD_SUPPORT, forceEnd, canoe.getHull().getLength() - STAND_OFFSET, true);
        pointLoads.clear();
        pointLoads.addAll(Arrays.asList(pLoadStart, pLoadEnd));
        return pointLoads;
    }

    /**
     * Convert a list of UniformDistributedLoads to a list of PointLoads.
     * @param loads list of distributed loads.
     * @return list of point loads.
     */
    private static List<PointLoad> distributedToPoint(List<UniformLoadDistribution> loads) {
        List<PointLoad> pointLoads = new ArrayList<>();

        for (UniformLoadDistribution load : loads) {
            double dLoadLength = load.getRx() - load.getX();
            double pLoadMagnitude = load.getMagnitude() * dLoadLength;
            double pLoadPosition = load.getX() + (dLoadLength / 2);
            pointLoads.add(new PointLoad(pLoadMagnitude, pLoadPosition, false));
        }

        return pointLoads;
    }

    /**
     * Solve the overall floating case of the canoe
     * @param canoe the canoe with a give hull geometry, material densities, and external loading to solve
     * @return the buoyancy reaction load distribution
     */
    public static PiecewiseContinuousLoadDistribution solveFloatingSystem(Canoe canoe) {
        // Case where the canoe is already in equilibrium returns a zero-valued distribution with sections matching the hull
        if (canoe.getNetForce() == 0) {
            Hull hull = canoe.getHull();
            List<HullSection> hullSections = hull.getHullSections();
            List<Section> sections = hullSections.stream().map(hullSection -> (Section) hullSection).toList();

            // Create zero-valued functions for each section
            List<UnivariateFunction> pieces = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                pieces.add(x -> 0.0);
            }

            // Create the PiecewiseContinuousLoadDistribution with zero-valued functions
            return new PiecewiseContinuousLoadDistribution(LoadType.BUOYANCY, pieces, sections);
        }

        // Case where the hull has no weight (only exists to provide length)
        // This is nonsense because buoyancy cannot be distributed against only the discrete set of critical points provided by pLoads and dLoads
        // This exception should not be thrown, a check should be performed before entering this function
        if (canoe.getHull().getWeight() == 0) {
            throw new RuntimeException("Cannot solve a buoyancy distribution with no hull");
        }

        // Solve for the equilibrium waterline and get the buoyancy distribution at that waterline
        double waterLine = getEquilibriumWaterLine(canoe);
        return getBuoyancyDistribution(waterLine, canoe);
    }

    /**
     * Guess a waterline to get a function that describes the submerged cross-sectional area as a function of length x
     * @param waterline the level below y = 0 of the waterline guess
     * @param section the section to calculate the function for
     * @return the function A_submerged(x) in m^2
     */
    private static UnivariateFunction getSubmergedCrossSectionalAreaFunction(double waterline, HullSection section) {
        return x -> {
            double h = waterline - Math.min(section.getSideProfileCurve().value(x), waterline);
            return section.getCrossSectionalAreaFunction().value(x) * h * section.getCrossSectionalAreaAdjustmentFactorFunction().apply(h);
        };
    }

    /**
     * Guess a waterline and get the volume of the submerged portion of the section
     * @param waterline the level below y = 0 of the waterline guess
     * @param section the section to calculate the buoyant force on
     * @return the submerged volume in m^3 at the given waterline guess
     */
    private static double getSubmergedVolume(double waterline, HullSection section) {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getSubmergedCrossSectionalAreaFunction(waterline, section), section.getX(), section.getRx());
    }

    /**
     * Guess a waterline and get the buoyant force on the section at that waterline
     * @param waterline the level below y = 0 of the waterline guess
     * @param section the section to calculate the buoyant force on
     * @return the buoyant force in kN
     */
    private static double getBuoyancyOnSection(double waterline, HullSection section) {
        return (PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() * getSubmergedVolume(waterline, section)) / 1000.0;
    }

    /**
     * Guess a waterline and get the buoyant force on the whole canoe
     * @param canoe the canoe with a given hull geometry
     * @param waterLine the level below y = 0 of the waterline guess
     * @return the total buoyant in kN at the given waterline guess
     */
    public static double getTotalBuoyancy(Canoe canoe, double waterLine) {
        List<HullSection> sections = canoe.getHull().getHullSections();
        double totalBuoyantForce = 0;
        for (HullSection section : sections) {
            totalBuoyantForce += getBuoyancyOnSection(waterLine, section);
        }
        return totalBuoyantForce;
    }

    /**
     * Iteratively solve for the equilibrium of the floating canoe
     * This works by matching the total canoe internal/external weight with the buoyancy
     * @param canoe the canoe with defined internal/external loads to get the waterline height for
     * @return the waterline height of floating equilibrium
     */
    public static double getEquilibriumWaterLine(Canoe canoe) {
        double netForce = canoe.getNetForce();
        double minWaterLine = -canoe.getHull().getMaxHeight();
        double maxWaterLine = 0;
        double waterLine = (minWaterLine + maxWaterLine) / 2.0;
        double totalBuoyancy;

        // Iterate until equilibrium is reached within a reasonable tolerance
        // Cut the step size in half each time, giving O(log(n)) time
        while (maxWaterLine - minWaterLine > 1e-6) {
            totalBuoyancy = getTotalBuoyancy(canoe, waterLine);
            if (totalBuoyancy < Math.abs(netForce))
                minWaterLine = waterLine;
            else
                maxWaterLine = waterLine;
            waterLine = (minWaterLine + maxWaterLine) / 2.0;
        }
        return waterLine;
    }

    /**
     * @param waterline the level below y = 0 of the waterline (pass in equilibrium waterline)
     * @param canoe the canoe with sections to calculate the buoyancy forces of
     * @return the buoyancy distribution of the canoe at the given waterline in kN/m
     */
    private static PiecewiseContinuousLoadDistribution getBuoyancyDistribution(double waterline, Canoe canoe) {
        List<Section> sectionsToMapTo = CalculusUtils.sectionsFromEndpoints(canoe.getSectionEndpoints().stream().toList());
        List<HullSection> mappedHullSections = reformHullSections(canoe.getHull(), sectionsToMapTo).getHullSections();
        List<UnivariateFunction> buoyancyPieces = new ArrayList<>();
        List<Section> buoyancySections = new ArrayList<>();

        for (HullSection section : mappedHullSections) {
            UnivariateFunction buoyancyPiece = x -> getSubmergedCrossSectionalAreaFunction(waterline, section).value(x) * PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() / 1000.0;
            buoyancyPieces.add(buoyancyPiece);
            buoyancySections.add(section);
        }

        return new PiecewiseContinuousLoadDistribution(LoadType.BUOYANCY, buoyancyPieces, buoyancySections);
    }

    /**
     * Redefines the same hull in terms of different critical section points
     * @param hull the hull to redefine
     * @param sectionsToMapTo the sections which who's individual endpoint pairs all together for the new critical points set
     * @return the reformed hull
     */
    private static Hull reformHullSections(Hull hull, List<Section> sectionsToMapTo) {
        List<HullSection> newHullSections = new ArrayList<>();
        List<HullSection> originalSections = hull.getHullSections();

        for (Section newSection : sectionsToMapTo) {
            double newStart = newSection.getX();
            double newEnd = newSection.getRx();

            List<HullSection> overlappingSections = originalSections.stream()
                    .filter(hs -> hs.getRx() > newStart && hs.getX() < newEnd)
                    .toList();

            for (HullSection overlappingSection : overlappingSections) {
                double overlapStart = Math.max(newStart, overlappingSection.getX());
                double overlapEnd = Math.min(newEnd, overlappingSection.getRx());

                if (overlapEnd > overlapStart) {
                    // Create new HullSection based on overlapping portion
                    HullSection newHullSection = new HullSection(
                            overlappingSection.getSideProfileCurve(),
                            overlappingSection.getTopProfileCurve(),
                            overlapStart,
                            overlapEnd,
                            overlappingSection.getThickness(),
                            overlappingSection.isFilledBulkhead()
                    );
                    newHullSections.add(newHullSection);
                }
            }
        }

        return new Hull(hull.getConcreteDensity(), hull.getBulkheadDensity(), newHullSections);
    }

    // TODO: Consult Design and Analysis team for details. Strategy for this has not yet been developed.
    public static List<Load> solveSubmergedSystem(Canoe canoe) {
        List<Load> loads = new ArrayList<>();
        return null;
    }
}

