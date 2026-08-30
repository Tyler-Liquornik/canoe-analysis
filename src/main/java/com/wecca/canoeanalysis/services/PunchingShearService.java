package com.wecca.canoeanalysis.services;

/**
 * Pure engineering calculations used by the Punching Shear module.
 * Length inputs are in millimetres, force inputs are in newtons, and
 * strength inputs and outputs are in megapascals.
 */
public final class PunchingShearService {

    // Coefficients used by PADDL's existing one-way and two-way concrete shear
    // equations. Keeping them named here makes each term visible and testable.
    private static final double CONCRETE_RESISTANCE_FACTOR = 0.65;
    private static final double LIGHTWEIGHT_CONCRETE_FACTOR = 0.75;
    private static final double ONE_WAY_SHEAR_COEFFICIENT = 0.21;
    private static final double TWO_WAY_SHEAR_COEFFICIENT = 0.19;
    private static final double COLUMN_ASPECT_RATIO = 1.0;
    private static final double INTERNAL_COLUMN_FACTOR = 4.0;

    private PunchingShearService() {
    }

    /**
     * Calculates the one-way shear capacity used by the module.
     *
     * <p>The flexural depth is half the shell thickness. The effective shear
     * depth is the larger of 0.9d and 0.72h, and the resulting capacity is in
     * newtons because MPa is equivalent to N/mm^2.</p>
     *
     * @param hullThicknessMm shell thickness h in mm
     * @param hullWidthMm effective resisting width in mm
     * @param compressiveStrengthMpa concrete compressive strength in MPa
     * @return intermediate depths and one-way capacity
     */
    public static OneWayResult calculateOneWay(
            double hullThicknessMm,
            double hullWidthMm,
            double compressiveStrengthMpa) {
        requirePositive(hullThicknessMm, "Hull thickness");
        requirePositive(hullWidthMm, "Hull width");
        requirePositive(compressiveStrengthMpa, "Compressive strength");

        // Determine the effective depth before applying the material and
        // resistance factors to the concrete shear expression.
        double flexuralDepthMm = hullThicknessMm / 2.0;
        double effectiveShearDepthMm = Math.max(
                0.9 * flexuralDepthMm,
                0.72 * hullThicknessMm);
        double capacityN = CONCRETE_RESISTANCE_FACTOR
                * LIGHTWEIGHT_CONCRETE_FACTOR
                * ONE_WAY_SHEAR_COEFFICIENT
                * Math.sqrt(compressiveStrengthMpa)
                * hullWidthMm
                * effectiveShearDepthMm;

        return new OneWayResult(flexuralDepthMm, effectiveShearDepthMm, capacityN);
    }

    /**
     * Calculates two-way punching demand and the three candidate capacities.
     *
     * <p>The supplied contact area is represented by an equal-area square. Its
     * perimeter is expanded by half the flexural depth on each side to define
     * the critical section used by all three capacity checks.</p>
     *
     * @param hullThicknessMm shell thickness in mm
     * @param compressiveStrengthMpa concrete compressive strength in MPa
     * @param appliedShearN applied punching force in N
     * @param contactAreaMm2 loaded contact area in mm^2
     * @return critical geometry, demand, candidate capacities, and governing capacity
     */
    public static TwoWayResult calculateTwoWay(
            double hullThicknessMm,
            double compressiveStrengthMpa,
            double appliedShearN,
            double contactAreaMm2) {
        requirePositive(hullThicknessMm, "Hull thickness");
        requirePositive(compressiveStrengthMpa, "Compressive strength");
        requirePositive(appliedShearN, "Applied shear");
        requirePositive(contactAreaMm2, "Contact area");

        // Convert the area into a square-equivalent footprint, then construct
        // the critical perimeter and resisting area around that footprint.
        double flexuralDepthMm = hullThicknessMm / 2.0;
        double equivalentContactWidthMm = Math.sqrt(contactAreaMm2);
        double criticalPerimeterMm = 4.0
                * (equivalentContactWidthMm + 2.0 * (flexuralDepthMm / 2.0));
        double criticalAreaMm2 = criticalPerimeterMm * hullThicknessMm;
        double demandMpa = appliedShearN / criticalAreaMm2;
        double sizeEffectFactor = Math.min(1.0, 1300.0 / (1000.0 + flexuralDepthMm));
        double commonFactor = sizeEffectFactor
                * LIGHTWEIGHT_CONCRETE_FACTOR
                * CONCRETE_RESISTANCE_FACTOR
                * Math.sqrt(compressiveStrengthMpa);

        // Evaluate every candidate expression explicitly; the minimum is the
        // capacity displayed by the original module's governing-result field.
        double capacityXMpa = (1.0 + 2.0 / COLUMN_ASPECT_RATIO)
                * TWO_WAY_SHEAR_COEFFICIENT
                * commonFactor;
        double capacityYMpa = (INTERNAL_COLUMN_FACTOR * flexuralDepthMm / criticalPerimeterMm
                + TWO_WAY_SHEAR_COEFFICIENT)
                * commonFactor;
        double capacityZMpa = 2.0 * TWO_WAY_SHEAR_COEFFICIENT * commonFactor;
        double governingCapacityMpa = Math.min(
                capacityXMpa,
                Math.min(capacityYMpa, capacityZMpa));

        return new TwoWayResult(
                criticalPerimeterMm,
                criticalAreaMm2,
                demandMpa,
                capacityXMpa,
                capacityYMpa,
                capacityZMpa,
                governingCapacityMpa);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be a finite positive number");
        }
    }

    /** One-way effective-depth calculation and force capacity. */
    public record OneWayResult(
            double flexuralDepthMm,
            double effectiveShearDepthMm,
            double capacityN) {
    }

    /** Two-way critical geometry, applied stress, and candidate stress capacities. */
    public record TwoWayResult(
            double criticalPerimeterMm,
            double criticalAreaMm2,
            double demandMpa,
            double capacityXMpa,
            double capacityYMpa,
            double capacityZMpa,
            double governingCapacityMpa) {
    }
}
