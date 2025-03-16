package com.wecca.canoeanalysis.services;

import Jama.Matrix;
import com.wecca.canoeanalysis.aop.TraceIgnore;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.FloatingSolution;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
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
            List<Section> sections = new ArrayList<>();
            for (CubicBezierFunction segment : hull.getSideViewSegments()) {
                sections.add(new Section(segment.getX1(), segment.getX2()));
            }

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
     * @param hull the hull to calculate the function for
     * @return the function A_submerged(x) in m^2
     */
    @TraceIgnore
    private static BoundedUnivariateFunction getSubmergedCrossSectionalAreaFunction(double h, double theta, double rotationX, Hull hull) {
        validateWaterLine(h);
        double thetaRadians = Math.toRadians(theta);
        return x -> {
            // Look up the side-view segment covering x.
            CubicBezierFunction side = hull.getSideViewSegments().stream()
                    .filter(seg -> x + 1e-9 >= seg.getX1() && x - 1e-9 <= seg.getX2())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No side-view segment covering x = " + x));
            double y = side.value(x);
            double adjustment = hull.getCrossSectionalAreaAdjustmentFactorFunction().value(Math.abs(y));

            // Look up the top-view segment covering x.
            CubicBezierFunction top = hull.getTopViewSegments().stream()
                    .filter(seg -> x + 1e-9 >= seg.getX1() && x - 1e-9 <= seg.getX2())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No top-view segment covers x = " + x));

            double w = 2 * top.value(x);
            double tiltedWaterline = h + (x - rotationX) * Math.tan(thetaRadians);
            double hSubmerged = tiltedWaterline - Math.min(y, tiltedWaterline);
            return Math.abs(w * hSubmerged * adjustment);
        };
    }

    private static double getSubmergedVolume(double waterline, double theta, double rotationX, Hull hull) {
        validateWaterLine(waterline);
        double totalVolume = 0.0;
        // Integrate over each side–view segment's x–domain.
        for (CubicBezierFunction seg : hull.getSideViewSegments()) {
            double xStart = seg.getX1();
            double xEnd = seg.getX2();
            totalVolume += CalculusUtils.integrator.integrate(
                    MaxEval.unlimited().getMaxEval(),
                    getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, hull),
                    xStart, xEnd);
        }
        return totalVolume;
    }

    /**
     * Guess a waterline and get the buoyant force on the section at that waterline
     * @param waterline the level below y = 0 of the waterline guess
     * @param theta the counterclockwise angle rotation of the canoe from flat
     * @param hull the hull to calculate the buoyant force on
     * @param rotationX the x coordinate of the point of rotation, on the same scale as the interval of the HullSection itself
     * @return the buoyant force in kN
     */
    public static double getBuoyancyForceOnHull(double waterline, double theta, double rotationX, Hull hull) {
        validateWaterLine(waterline);
        double volume = getSubmergedVolume(waterline, theta, rotationX, hull);
        return (PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() * volume) / 1000.0;
    }

    /**
     * Guess a waterline and get the moment on the section at that waterline.
     * @param waterline the level below y = 0 of the waterline guess.
     * @param theta the counterclockwise angle rotation of the canoe from flat.
     * @param hull the hull to calculate the buoyant moment on.
     * @return the moment in kN * m.
     */
    @TraceIgnore
    private static double getBuoyancyMomentOnHull(double waterline, double theta, Hull hull) {
        validateWaterLine(waterline);
        double rotationX = hull.getLength() / 2;
        double totalMoment = 0.0;
        BoundedUnivariateFunction submergedArea = getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, hull);
        for (CubicBezierFunction seg : hull.getSideViewSegments()) {
            double xStart = seg.getX1();
            double xEnd = seg.getX2();
            totalMoment += CalculusUtils.integrator.integrate(
                    MaxEval.unlimited().getMaxEval(),
                    x -> {
                        double xSec = submergedArea.value(x);
                        double buoyantForceAtX = xSec * PhysicalConstants.DENSITY_OF_WATER.getValue() * PhysicalConstants.GRAVITY.getValue() / 1000.0;
                        double leverArm = x - rotationX;
                        return buoyantForceAtX * leverArm;
                    },
                    xStart, xEnd);
        }
        return totalMoment;
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
            totalBuoyancy = getBuoyancyForceOnHull(h, 0, rotationX, canoe.getHull());
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
        BivariateFunction forceBalance = (hGuess, thetaGuess) -> getBuoyancyForceOnHull(hGuess, thetaGuess, rotationX, canoe.getHull()) + netForce;
        BivariateFunction momentBalance = (hGuess, thetaGuess) -> getBuoyancyMomentOnHull(hGuess, thetaGuess, canoe.getHull()) + netMoment;

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
     * @param theta the counterclockwise angle rotation of the canoe from flat in degrees
     * @param canoe the canoe with a Hull (using Bézier segments) from which to calculate buoyancy forces
     * @return the buoyancy distribution of the canoe at the given waterline in kN/m
     */
    private static PiecewiseContinuousLoadDistribution getBuoyancyForceDistribution(double waterline, double theta, Canoe canoe) {
        Hull hull = canoe.getHull();
        double rotationX = hull.getLength() / 2;

        // Build the submerged cross-sectional area function over the full hull.
        BoundedUnivariateFunction submergedAreaFunction = getSubmergedCrossSectionalAreaFunction(waterline, theta, rotationX, hull);

        // Partition the hull's domain into sections using the critical points.
        List<Double> critPointList = new ArrayList<>(canoe.getCriticalPointSet());
        List<Section> buoyancySections = CalculusUtils.sectionsFromEndpoints(critPointList);
        List<BoundedUnivariateFunction> buoyancyPieces = new ArrayList<>();
        // For each section, define a buoyancy piece as the area function times density and gravity.
        for (Section ignored : buoyancySections) {
            BoundedUnivariateFunction piece = x -> submergedAreaFunction.value(x)
                    * PhysicalConstants.DENSITY_OF_WATER.getValue()
                    * PhysicalConstants.GRAVITY.getValue() / 1000.0;
            buoyancyPieces.add(piece);
        }
        return new PiecewiseContinuousLoadDistribution(LoadType.BUOYANCY, buoyancyPieces, buoyancySections);
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

