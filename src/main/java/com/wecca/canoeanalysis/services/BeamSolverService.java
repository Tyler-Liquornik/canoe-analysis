package com.wecca.canoeanalysis.services;

import Jama.Matrix;
import com.wecca.canoeanalysis.aop.TraceIgnore;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.FloatingSolution;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.PhysicalConstants;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.optim.MaxEval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Solves load cases
 */
public class BeamSolverService {
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
        PointLoad pLoadStart = new PointLoad(LoadType.POINT_LOAD_SUPPORT, forceStart, 0, true);
        PointLoad pLoadEnd = new PointLoad(LoadType.POINT_LOAD_SUPPORT, forceEnd, canoe.getHull().getLength(), true);
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
     * @return the buoyancy force reaction load distribution
     */
    public static FloatingSolution solveFloatingSystem(Canoe canoe) {
        // Case where the canoe is already in equilibrium returns a zero-valued distribution with sections matching the hull
        if (canoe.getNetForce() == 0) {
            Hull hull = canoe.getHull();
            List<HullSection> hullSections = hull.getHullSections();
            List<Section> sections = hullSections.stream().map(hullSection -> (Section) hullSection).toList();

            // Create zero-valued functions for each section
            List<BoundedUnivariateFunction> pieces = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                pieces.add(x -> 0.0);
            }

            // Create the PiecewiseContinuousLoadDistribution with zero-valued functions
            PiecewiseContinuousLoadDistribution buoyancyForce = new PiecewiseContinuousLoadDistribution(LoadType.BUOYANCY, pieces, sections);
            return new FloatingSolution(buoyancyForce, canoe.getHull().getMaxHeight(), 0, false);
        }

        // Case where the hull has no weight (only exists to provide length)
        // This is nonsense because buoyancy cannot be distributed against only the discrete set of critical points provided by pLoads and dLoads
        // This exception should not be thrown, a check should be performed before entering this function
        if (canoe.getHull().getWeight() == 0)
            throw new RuntimeException("Cannot solve a buoyancy distribution with no hull");

        // Solve for the equilibrium waterline and get the buoyancy force distribution at that waterline
        double[] waterLine = getEquilibriumWaterLine(canoe);
        if (waterLine == null) return null;
        else {
            double h = waterLine[0];
            double theta = waterLine[1];
            double hTilt = (canoe.getHull().getLength() / 2) * Math.tan(Math.toRadians(theta));
            boolean isTippedOver =  Math.abs(hTilt) >= Math.abs(h);
            return new FloatingSolution(getBuoyancyForceDistribution(h, theta, canoe), h, theta, isTippedOver);
        }
    }

    /**
     * Guess a waterline to get a function that describes the submerged cross-sectional area as a function of length x
     * @param h the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat in degrees
     * @param rotationX the x coordinate of the point of rotation, on the same scale as the interval of the HullSection itself
     * @param section the section to calculate the function for
     * @return the function A_submerged(x) in m^2
     */
    @TraceIgnore
    private static BoundedUnivariateFunction getSubmergedCrossSectionalAreaFunction(double h, double theta, double rotationX, HullSection section) {
        validateWaterLine(h);
        double thetaRadians = Math.toRadians(theta);
        return x -> {
            double tiltedWaterline = h + (x - rotationX) * Math.tan(thetaRadians);
            double y = section.getSideProfileCurve().value(x);
            double adjustment = section.getCrossSectionalAreaAdjustmentFactorFunction().value(Math.abs(y));
            double hSubmerged = tiltedWaterline - Math.min(y, tiltedWaterline);
            double w = 2 * section.getTopProfileCurve().value(x);
            return Math.abs(w * hSubmerged * adjustment);
        };
    }

    /**
     * Guess a waterline and get the volume of the submerged portion of the section
     * @param waterline the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat
     * @param section the section to calculate the buoyant force on
     * @param rotationX the x coordinate of the point of rotation, on the same scale as the interval of the HullSection itself
     * @return the submerged volume in m^3 at the given waterline guess
     */
    private static double getSubmergedVolume(double waterline, double theta, double rotationX, HullSection section) {
        validateWaterLine(waterline);
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, section), section.getX(), section.getRx());
    }

    /**
     * Guess a waterline and get the buoyant force on the section at that waterline
     * @param waterline the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat
     * @param section the section to calculate the buoyant force on
     * @param rotationX the x coordinate of the point of rotation, on the same scale as the interval of the HullSection itself
     * @return the buoyant force in kN
     */
    @TraceIgnore
    private static double getBuoyancyForceOnSection(double waterline, double theta, double rotationX, HullSection section) {
        validateWaterLine(waterline);
        return (PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() * getSubmergedVolume(waterline, theta, rotationX, section)) / 1000.0;
    }

    /**
     * Guess a waterline and get the moment on the section at that waterline.
     * @param waterline the level below y = 0 of the waterline guess.
     * @param theta the counterclockwise angle rotation of the canoe from flat.
     * @param section the section to calculate the buoyant force on.
     * @param rotationX the x-coordinate of the point of rotation, on the same scale as the interval of the HullSection.
     * @return the moment in kN * m.
     */
    @TraceIgnore
    private static double getBuoyancyMomentOnSection(double waterline, double theta, double rotationX, HullSection section) {
        validateWaterLine(waterline);
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), x -> {
            double xSec = getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, section).value(x);
            double buoyantForceAtX = xSec * PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() / 1000.0;
            double leverArm = x - rotationX;  // Distance from the rotation point
            return buoyantForceAtX * leverArm;
        }, section.getX(), section.getRx());
    }

    /**
     * Guess a waterline and get the buoyant force on the whole canoe
     * @param canoe the canoe with a given hull geometry
     * @param waterLine the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat in degrees
     * @param rotationX the x coordinate of the point of rotation, on the same scale as the interval of the HullSection itself
     * @return the total buoyant in kN at the given waterline guess
     */
    @Traceable
    public static double getTotalBuoyancyForce(Canoe canoe, double waterLine, double rotationX, double theta) {
        validateWaterLine(waterLine);
        return canoe.getHull().getHullSections().stream()
                .mapToDouble(hullSection -> getBuoyancyForceOnSection(waterLine, theta, rotationX, hullSection)).sum();
    }

    /**
     * Guess a waterline and get the buoyancy moment on the whole canoe
     * @param canoe the canoe with a given hull geometry
     * @param waterLine the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat in degrees
     * @return the total buoyancy moment in kN * m at the given waterline guess
     */
    public static double getTotalBuoyancyMoment(Canoe canoe, double waterLine, double theta) {
        validateWaterLine(waterLine);
        double rotationX = canoe.getHull().getLength() / 2; // rotation pivot assumed at L/2 for now
        return canoe.getHull().getHullSections().stream()
                .mapToDouble(hullSection -> getBuoyancyMomentOnSection(waterLine, theta, rotationX, hullSection)).sum();
    }

    /**
     * @deprecated by getEquilibriumWaterLine which is more general than this
     * Iteratively solve for the force equilibrium of the floating canoe
     * This works by matching the total canoe internal/external weight with the buoyancy
     * Doesn't consider moment and only works for symmetrical load cases where no moments are involved
     * @param canoe the canoe with defined internal/external loads to get the waterline height for
     * @return the equilibrium waterline as [h, theta], theta is always 0 in the symmetrical case with no moment
     */
    @Deprecated
    public static double[] getEquilibriumWaterLineSymmetrical(Canoe canoe) {
        double netForce = canoe.getNetForce();
        double minWaterLine = -canoe.getHull().getMaxHeight();
        double maxWaterLine = 0;
        double h = (minWaterLine + maxWaterLine) / 2.0;
        double totalBuoyancy;

        // Binary search until equilibrium is reached within a reasonable tolerance
        while (maxWaterLine - minWaterLine > 1e-6) {
            double rotationX = canoe.getHull().getLength() / 2;
            totalBuoyancy = getTotalBuoyancyForce(canoe, h, rotationX, 0);
            if (totalBuoyancy < Math.abs(netForce))
                minWaterLine = h;
            else
                maxWaterLine = h;
            h = (minWaterLine + maxWaterLine) / 2.0;
        }
        validateWaterLine(h);
        return new double[]{h, 0};
    }

    /**
     * Iteratively solve for the equilibrium of the floating canoe using 2D Newton-Raphson
     * This works by matching the total canoe internal/external weight/moment with the reactionary buoyancy force/moment
     * @param canoe the canoe with defined internal/external loads to get the waterline height for
     * @return the equilibrium waterline as [h, theta]
     */
    public static double[] getEquilibriumWaterLine(Canoe canoe) {
        double netForce = canoe.getNetForce();
        double netMoment = canoe.getNetMoment();
        double minWaterLine = -canoe.getHull().getMaxHeight();
        double maxWaterLine = 0;
        double rotationX = canoe.getHull().getLength() / 2;

        // Initial guesses for h and theta
        double h = (minWaterLine + maxWaterLine) / 2.0;
        double theta = 0.0;
        double tolerance = 1e-6;
        double regularization = 1e-6;

        // [F(h, theta) M(h, theta)] = [0, 0] (move everything in the force and moment equations to one side)
        BivariateFunction forceBalance = (hGuess, thetaGuess) -> getTotalBuoyancyForce(canoe, hGuess, rotationX, thetaGuess) + netForce;
        BivariateFunction momentBalance = (hGuess, thetaGuess) -> getTotalBuoyancyMoment(canoe, hGuess, thetaGuess) + netMoment;

        // Iterate using 2D Newton-Raphson algorithm to solve for both h and theta
        int maxIterations = 1000;
        for (int iter = 0; iter < maxIterations; iter++) {

            double systemNetForce = forceBalance.value(h, theta);
            double systemNetMoment = momentBalance.value(h, theta);

            // Check if the solution is within tolerance for both force and moment balance
            if (Math.abs(systemNetForce) < tolerance && Math.abs(systemNetMoment) < tolerance)
                return new double[]{h, theta};;

            // Compute the Jacobian matrix, adding a regularization term to avoid singularity
            // Regularization is 1e-6 * I_2
            Matrix jacobian = CalculusUtils.evaluateR2Jacobian(h, theta, forceBalance, momentBalance);
            Matrix inverseJacobian;
            try {
                inverseJacobian = jacobian.inverse();
            } catch (Exception ignoredSingularMatrixException) {
                Matrix regularizedJacobian = jacobian.plus(Matrix.identity(2, 2).times(regularization));
                inverseJacobian = regularizedJacobian.inverse();
            }

            // Create a matrix of the force and moment balances equal 0 (all terms on one side)
            Matrix F = new Matrix(2, 1);
            F.set(0, 0, systemNetForce);
            F.set(1, 0, systemNetMoment);

            // Update the guesses for h and theta with delta = -J_inverse * F
            // Delta is the column matrix [h, theta]
            Matrix delta = inverseJacobian.times(F).times(-1);
            h += delta.get(0, 0);
            theta += delta.get(1, 0);

            // Check if h guess is diverging out of bounds
            if (h < minWaterLine || h > maxWaterLine)
                return null;
        }

        // No convergence after max amount of allowed iterations
        return null;
    }

    /**
     * @param waterline the level below y = 0 of the waterline (pass in equilibrium waterline)
     * @param canoe the canoe with sections to calculate the buoyancy forces of
     * @param theta the counterclockwise angle rotation of the canoe from flat in degrees
     * @return the buoyancy distribution of the canoe at the given waterline in kN/m
     */
    private static PiecewiseContinuousLoadDistribution getBuoyancyForceDistribution(double waterline, double theta, Canoe canoe) {
        List<Section> sectionsToMapTo = CalculusUtils.sectionsFromEndpoints(canoe.getSectionEndpoints().stream().toList());
        List<HullSection> mappedHullSections = reformHullSections(canoe.getHull(), sectionsToMapTo).getHullSections();
        List<BoundedUnivariateFunction> buoyancyPieces = new ArrayList<>();
        List<Section> buoyancySections = new ArrayList<>();

        double rotationX = canoe.getHull().getLength() / 2;
        for (HullSection section : mappedHullSections) {
            BoundedUnivariateFunction buoyancyPiece = x -> getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, section).value(x) * PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() / 1000.0;
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

    /**
     * Ensure a waterline is not greater than zero (since waterline is measured as below y=0)
     * @param waterLine to validate
     */
    @TraceIgnore
    private static void validateWaterLine(double waterLine) {
        if (waterLine > 0)
            throw new IllegalArgumentException("Waterline must NOT be greater than zero");
    }

    // TODO: Consult Design and Analysis team for details. Strategy for this has not yet been developed.
    public static List<Load> solveSubmergedSystem(Canoe canoe) {
        List<Load> loads = new ArrayList<>();
        return null;
    }
}

