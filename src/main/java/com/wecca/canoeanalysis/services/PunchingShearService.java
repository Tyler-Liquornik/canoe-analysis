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
        return calculateOneWay(hullThicknessMm, hullWidthMm, compressiveStrengthMpa, null);
    }

    public static OneWayResult calculateOneWay(
            double hullThicknessMm,
            double hullWidthMm,
            double compressiveStrengthMpa, CalculationReport report) {
        requirePositive(hullThicknessMm, "Hull thickness");
        requirePositive(hullWidthMm, "Hull width");
        requirePositive(compressiveStrengthMpa, "Compressive strength");

        // Determine the effective depth before applying the material and
        // resistance factors to the concrete shear expression.
        double flexuralDepthMm = hullThicknessMm / 2.0;
        if (report != null) report.add("d", "h / 2.0", "" + CalculationReport.number(hullThicknessMm) + " / 2.0", flexuralDepthMm, "mm");
        double effectiveShearDepthMm = Math.max(
                0.9 * flexuralDepthMm,
                0.72 * hullThicknessMm);
        if (report != null) report.add("dv", "max( 0.9 × d, 0.72 × h)", "max( 0.9 × " + CalculationReport.number(flexuralDepthMm) + ", 0.72 × " + CalculationReport.number(hullThicknessMm) + ")", effectiveShearDepthMm, "mm");
        double capacityN = CONCRETE_RESISTANCE_FACTOR
                * LIGHTWEIGHT_CONCRETE_FACTOR
                * ONE_WAY_SHEAR_COEFFICIENT
                * Math.sqrt(compressiveStrengthMpa)
                * hullWidthMm
                * effectiveShearDepthMm;
        if (report != null) report.add("Vc", "0.65 × 0.75 × 0.21 × √(f′c) × b × dv", "" + CalculationReport.number(CONCRETE_RESISTANCE_FACTOR) + " × " + CalculationReport.number(LIGHTWEIGHT_CONCRETE_FACTOR) + " × " + CalculationReport.number(ONE_WAY_SHEAR_COEFFICIENT) + " × √(" + CalculationReport.number(compressiveStrengthMpa) + ") × " + CalculationReport.number(hullWidthMm) + " × " + CalculationReport.number(effectiveShearDepthMm) + "", capacityN, "N");

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
        return calculateTwoWay(hullThicknessMm, compressiveStrengthMpa, appliedShearN, contactAreaMm2, null);
    }

    public static TwoWayResult calculateTwoWay(
            double hullThicknessMm,
            double compressiveStrengthMpa,
            double appliedShearN,
            double contactAreaMm2, CalculationReport report) {
        requirePositive(hullThicknessMm, "Hull thickness");
        requirePositive(compressiveStrengthMpa, "Compressive strength");
        requirePositive(appliedShearN, "Applied shear");
        requirePositive(contactAreaMm2, "Contact area");

        // Convert the area into a square-equivalent footprint, then construct
        // the critical perimeter and resisting area around that footprint.
        double flexuralDepthMm = hullThicknessMm / 2.0;
        if (report != null) report.add("d", "h / 2.0", "" + CalculationReport.number(hullThicknessMm) + " / 2.0", flexuralDepthMm, "mm");
        double equivalentContactWidthMm = Math.sqrt(contactAreaMm2);
        if (report != null) report.add("a", "√(Acontact)", "√(" + CalculationReport.number(contactAreaMm2) + ")", equivalentContactWidthMm, "mm");
        double criticalPerimeterMm = 4.0
                * (equivalentContactWidthMm + 2.0 * (flexuralDepthMm / 2.0));
        if (report != null) report.add("b₀", "4.0 × (a + 2.0 × (d / 2.0))", "4.0 × (" + CalculationReport.number(equivalentContactWidthMm) + " + 2.0 × (" + CalculationReport.number(flexuralDepthMm) + " / 2.0))", criticalPerimeterMm, "mm");
        double criticalAreaMm2 = criticalPerimeterMm * hullThicknessMm;
        if (report != null) report.add("Acrit", "b₀ × h", "" + CalculationReport.number(criticalPerimeterMm) + " × " + CalculationReport.number(hullThicknessMm) + "", criticalAreaMm2, "mm²");
        double demandMpa = appliedShearN / criticalAreaMm2;
        if (report != null) report.add("vf", "Vf / Acrit", "" + CalculationReport.number(appliedShearN) + " / " + CalculationReport.number(criticalAreaMm2) + "", demandMpa, "MPa");
        double sizeEffectFactor = Math.min(1.0, 1300.0 / (1000.0 + flexuralDepthMm));
        if (report != null) report.add("β", "min(1.0, 1300.0 / (1000.0 + d))", "min(1.0, 1300.0 / (1000.0 + " + CalculationReport.number(flexuralDepthMm) + "))", sizeEffectFactor, "");
        double commonFactor = sizeEffectFactor
                * LIGHTWEIGHT_CONCRETE_FACTOR
                * CONCRETE_RESISTANCE_FACTOR
                * Math.sqrt(compressiveStrengthMpa);
        if (report != null) report.add("K", "β × 0.75 × 0.65 × √(f′c)", "" + CalculationReport.number(sizeEffectFactor) + " × " + CalculationReport.number(LIGHTWEIGHT_CONCRETE_FACTOR) + " × " + CalculationReport.number(CONCRETE_RESISTANCE_FACTOR) + " × √(" + CalculationReport.number(compressiveStrengthMpa) + ")", commonFactor, "");

        // Evaluate every candidate expression explicitly; the minimum is the
        // capacity displayed by the original module's governing-result field.
        double capacityXMpa = (1.0 + 2.0 / COLUMN_ASPECT_RATIO)
                * TWO_WAY_SHEAR_COEFFICIENT
                * commonFactor;
        if (report != null) report.add("vc₁", "(1.0 + 2.0 / 1.0) × 0.19 × K", "(1.0 + 2.0 / " + CalculationReport.number(COLUMN_ASPECT_RATIO) + ") × " + CalculationReport.number(TWO_WAY_SHEAR_COEFFICIENT) + " × " + CalculationReport.number(commonFactor) + "", capacityXMpa, "MPa");
        double capacityYMpa = (INTERNAL_COLUMN_FACTOR * flexuralDepthMm / criticalPerimeterMm
                + TWO_WAY_SHEAR_COEFFICIENT)
                * commonFactor;
        if (report != null) report.add("vc₂", "(4.0 × d / b₀ + 0.19) × K", "(" + CalculationReport.number(INTERNAL_COLUMN_FACTOR) + " × " + CalculationReport.number(flexuralDepthMm) + " / " + CalculationReport.number(criticalPerimeterMm) + " + " + CalculationReport.number(TWO_WAY_SHEAR_COEFFICIENT) + ") × " + CalculationReport.number(commonFactor) + "", capacityYMpa, "MPa");
        double capacityZMpa = 2.0 * TWO_WAY_SHEAR_COEFFICIENT * commonFactor;
        if (report != null) report.add("vc₃", "2.0 × 0.19 × K", "2.0 × " + CalculationReport.number(TWO_WAY_SHEAR_COEFFICIENT) + " × " + CalculationReport.number(commonFactor) + "", capacityZMpa, "MPa");
        double governingCapacityMpa = Math.min(
                capacityXMpa,
                Math.min(capacityYMpa, capacityZMpa));
        if (report != null) report.add("vc,min", "min( vc₁, min(vc₂, vc₃))", "min( " + CalculationReport.number(capacityXMpa) + ", min(" + CalculationReport.number(capacityYMpa) + ", " + CalculationReport.number(capacityZMpa) + "))", governingCapacityMpa, "MPa");

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
