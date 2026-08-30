package com.wecca.canoeanalysis.services;

/**
 * Performs the calculations used by the failure-envelope module.
 *
 * <p>The sign convention is tension positive and compression negative. Material
 * strengths and calculated stresses are expressed in MPa.</p>
 */
public final class FailureEnvelopeService {

    // Numerical allowance used only when classifying a point on the envelope;
    // it prevents floating-point roundoff from turning an exact limit into a failure.
    private static final double EPSILON = 1.0e-9;

    private FailureEnvelopeService() {
    }

    /**
     * Analyzes one section when bending and shear use the same second moment
     * of area. This overload preserves the module's original single-I input.
     *
     * @param compressiveStrengthMpa material compressive strength in MPa
     * @param tensileStrengthMpa material tensile strength in MPa
     * @param compressionEdgeDistanceM neutral-axis distance to the compression edge in m
     * @param tensionEdgeDistanceM neutral-axis distance to the tension edge in m
     * @param firstMomentOfAreaM3 first moment of area Q at the shear section in m^3
     * @param secondMomentOfAreaM4 second moment of area I in m^4
     * @param thicknessM effective shear thickness in m
     * @param maximumMomentKnM applied maximum moment in kN*m
     * @param maximumShearKn applied maximum shear in kN
     * @return the strength circles, demand circles, tangent envelope, and utilization result
     */
    public static Analysis analyze(
            double compressiveStrengthMpa,
            double tensileStrengthMpa,
            double compressionEdgeDistanceM,
            double tensionEdgeDistanceM,
            double firstMomentOfAreaM3,
            double secondMomentOfAreaM4,
            double thicknessM,
            double maximumMomentKnM,
            double maximumShearKn) {

        return analyze(
                compressiveStrengthMpa,
                tensileStrengthMpa,
                compressionEdgeDistanceM,
                tensionEdgeDistanceM,
                firstMomentOfAreaM3,
                secondMomentOfAreaM4,
                secondMomentOfAreaM4,
                thicknessM,
                maximumMomentKnM,
                maximumShearKn);
    }

    /**
     * Builds an analysis when bending and shear govern at different sections.
     * All stresses produced by this method use the tension-positive,
     * compression-negative convention documented on this class.
     *
     * @param compressiveStrengthMpa material compressive strength in MPa
     * @param tensileStrengthMpa material tensile strength in MPa
     * @param compressionEdgeDistanceM neutral-axis distance to the compression edge in m
     * @param tensionEdgeDistanceM neutral-axis distance to the tension edge in m
     * @param firstMomentOfAreaM3 first moment of area Q at the shear section in m^3
     * @param bendingSecondMomentOfAreaM4 bending-section second moment of area in m^4
     * @param shearSecondMomentOfAreaM4 shear-section second moment of area in m^4
     * @param thicknessM effective shear thickness in m
     * @param maximumMomentKnM applied maximum moment in kN*m
     * @param maximumShearKn applied maximum shear in kN
     * @return the complete combined failure-envelope analysis
     */
    public static Analysis analyze(
            double compressiveStrengthMpa,
            double tensileStrengthMpa,
            double compressionEdgeDistanceM,
            double tensionEdgeDistanceM,
            double firstMomentOfAreaM3,
            double bendingSecondMomentOfAreaM4,
            double shearSecondMomentOfAreaM4,
            double thicknessM,
            double maximumMomentKnM,
            double maximumShearKn) {

        requirePositive("Compressive strength", compressiveStrengthMpa);
        requirePositive("Tensile strength", tensileStrengthMpa);
        requirePositive("Compression edge distance", compressionEdgeDistanceM);
        requirePositive("Tension edge distance", tensionEdgeDistanceM);
        requirePositive("First moment of area", firstMomentOfAreaM3);
        requirePositive("Bending second moment of area", bendingSecondMomentOfAreaM4);
        requirePositive("Shear second moment of area", shearSecondMomentOfAreaM4);
        requirePositive("Thickness", thicknessM);
        requireNonNegative("Maximum moment", maximumMomentKnM);
        requireNonNegative("Maximum shear", maximumShearKn);

        StressResults stresses = calculateSectionStresses(
                compressionEdgeDistanceM,
                tensionEdgeDistanceM,
                firstMomentOfAreaM3,
                bendingSecondMomentOfAreaM4,
                shearSecondMomentOfAreaM4,
                thicknessM,
                maximumMomentKnM,
                maximumShearKn);

        // Uniaxial strengths map to circles spanning [-fc, 0] and [0, ft].
        MohrCircle compressionCircle = new MohrCircle(
                -compressiveStrengthMpa / 2.0,
                compressiveStrengthMpa / 2.0);
        MohrCircle tensionCircle = new MohrCircle(
                tensileStrengthMpa / 2.0,
                tensileStrengthMpa / 2.0);
        // Applied flexural extremes define the demand circle diameter; pure
        // shear is centred at zero with radius equal to the shear stress.
        MohrCircle bendingCircle = new MohrCircle(
                (stresses.tensionMpa() - stresses.compressionMpa()) / 2.0,
                (stresses.tensionMpa() + stresses.compressionMpa()) / 2.0);
        MohrCircle shearCircle = new MohrCircle(0.0, stresses.shearMpa());

        // The common external tangent to the compression and tension circles.
        double rootStrengthProduct = Math.sqrt(compressiveStrengthMpa * tensileStrengthMpa);
        EnvelopeLine envelope = new EnvelopeLine(
                (tensileStrengthMpa - compressiveStrengthMpa) / (2.0 * rootStrengthProduct),
                rootStrengthProduct / 2.0);

        // A circle's utilization is its normalized support-function demand
        // against the common tangent. The larger demand governs the result.
        double envelopeUtilization = circleUtilization(bendingCircle, envelope);
        double shearUtilization = circleUtilization(shearCircle, envelope);
        double governingUtilization = Math.max(envelopeUtilization, shearUtilization);
        GoverningDemand governingDemand = envelopeUtilization >= shearUtilization
                ? GoverningDemand.BENDING
                : GoverningDemand.SHEAR;
        double factorOfSafety = governingUtilization <= EPSILON
                ? Double.POSITIVE_INFINITY
                : 1.0 / governingUtilization;

        return new Analysis(
                compressionCircle,
                tensionCircle,
                bendingCircle,
                shearCircle,
                envelope,
                stresses,
                envelopeUtilization,
                shearUtilization,
                governingUtilization,
                governingDemand,
                factorOfSafety,
                governingUtilization <= 1.0 + EPSILON);
    }

    /**
     * Calculates section stresses when bending and shear share the same second
     * moment of area.
     *
     * @return positive magnitudes for compression, tension, and shear in MPa
     */
    public static StressResults calculateSectionStresses(
            double compressionEdgeDistanceM,
            double tensionEdgeDistanceM,
            double firstMomentOfAreaM3,
            double secondMomentOfAreaM4,
            double thicknessM,
            double maximumMomentKnM,
            double maximumShearKn) {

        return calculateSectionStresses(
                compressionEdgeDistanceM,
                tensionEdgeDistanceM,
                firstMomentOfAreaM3,
                secondMomentOfAreaM4,
                secondMomentOfAreaM4,
                thicknessM,
                maximumMomentKnM,
                maximumShearKn);
    }

    /**
     * Calculates flexural stress with the bending-section inertia and shear
     * stress with the shear-section inertia.
     *
     * <p>The implemented equations are sigma = M*y/I and tau = V*Q/(I*t).
     * Input actions are in kN-based units and geometry is in metres.</p>
     *
     * @return positive magnitudes for compression, tension, and shear in MPa
     */
    public static StressResults calculateSectionStresses(
            double compressionEdgeDistanceM,
            double tensionEdgeDistanceM,
            double firstMomentOfAreaM3,
            double bendingSecondMomentOfAreaM4,
            double shearSecondMomentOfAreaM4,
            double thicknessM,
            double maximumMomentKnM,
            double maximumShearKn) {

        requirePositive("Compression edge distance", compressionEdgeDistanceM);
        requirePositive("Tension edge distance", tensionEdgeDistanceM);
        requirePositive("First moment of area", firstMomentOfAreaM3);
        requirePositive("Bending second moment of area", bendingSecondMomentOfAreaM4);
        requirePositive("Shear second moment of area", shearSecondMomentOfAreaM4);
        requirePositive("Thickness", thicknessM);
        requireNonNegative("Maximum moment", maximumMomentKnM);
        requireNonNegative("Maximum shear", maximumShearKn);

        // M is entered in kN*m and V in kN. Dividing the resulting Pa values by
        // 1,000,000 gives the compact forms below in MPa.
        double compressionMpa = maximumMomentKnM * compressionEdgeDistanceM
                / (1_000.0 * bendingSecondMomentOfAreaM4);
        double tensionMpa = maximumMomentKnM * tensionEdgeDistanceM
                / (1_000.0 * bendingSecondMomentOfAreaM4);
        double shearMpa = maximumShearKn * firstMomentOfAreaM3
                / (1_000.0 * shearSecondMomentOfAreaM4 * thicknessM);

        return new StressResults(compressionMpa, tensionMpa, shearMpa);
    }

    /**
     * Returns the fraction of the symmetric upper/lower tangent envelope occupied
     * by a Mohr circle. Values at or below one are inside the envelope.
     */
    public static double circleUtilization(MohrCircle circle, EnvelopeLine envelope) {
        double normalLength = Math.sqrt(1.0 + envelope.slope() * envelope.slope());
        double demand = Math.abs(envelope.slope() * circle.centerMpa())
                + circle.radiusMpa() * normalLength;
        return demand / envelope.interceptMpa();
    }

    /**
     * Finds the point on a Mohr circle touched by the upper envelope line. The
     * result is the perpendicular projection of the circle centre onto that line.
     *
     * @return upper-envelope tangency coordinates in MPa
     */
    public static TangencyPoint upperTangencyPoint(MohrCircle circle, EnvelopeLine envelope) {
        double denominator = 1.0 + envelope.slope() * envelope.slope();
        double signedDistanceNumerator = envelope.slope() * circle.centerMpa()
                + envelope.interceptMpa();
        return new TangencyPoint(
                circle.centerMpa() - envelope.slope() * signedDistanceNumerator / denominator,
                signedDistanceNumerator / denominator);
    }

    /**
     * Reflects the upper tangency point across the normal-stress axis because
     * the lower failure envelope is symmetric in shear.
     *
     * @return lower-envelope tangency coordinates in MPa
     */
    public static TangencyPoint lowerTangencyPoint(MohrCircle circle, EnvelopeLine envelope) {
        TangencyPoint upper = upperTangencyPoint(circle, envelope);
        return new TangencyPoint(upper.normalStressMpa(), -upper.shearStressMpa());
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be greater than zero.");
        }
    }

    private static void requireNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " cannot be negative.");
        }
    }

    /** A Mohr circle expressed by its normal-stress centre and non-negative radius. */
    public record MohrCircle(double centerMpa, double radiusMpa) {
        public MohrCircle {
            if (!Double.isFinite(centerMpa) || !Double.isFinite(radiusMpa) || radiusMpa < 0.0) {
                throw new IllegalArgumentException("A Mohr circle requires a finite center and non-negative radius.");
            }
        }

        /** @return leftmost normal stress on the circle in MPa */
        public double minimumNormalStressMpa() {
            return centerMpa - radiusMpa;
        }

        /** @return rightmost normal stress on the circle in MPa */
        public double maximumNormalStressMpa() {
            return centerMpa + radiusMpa;
        }
    }

    /** One of the two symmetric linear tangents forming the failure envelope. */
    public record EnvelopeLine(double slope, double interceptMpa) {
        /** @return upper-envelope shear stress at the supplied normal stress */
        public double upperShearMpa(double normalStressMpa) {
            return slope * normalStressMpa + interceptMpa;
        }

        /** @return lower-envelope shear stress at the supplied normal stress */
        public double lowerShearMpa(double normalStressMpa) {
            return -upperShearMpa(normalStressMpa);
        }
    }

    /** A normal/shear coordinate at which an envelope is tangent to a circle. */
    public record TangencyPoint(double normalStressMpa, double shearStressMpa) {
    }

    /** Positive stress magnitudes calculated from the section actions and properties. */
    public record StressResults(double compressionMpa, double tensionMpa, double shearMpa) {
    }

    /** Identifies which applied demand circle has the larger utilization. */
    public enum GoverningDemand {
        BENDING,
        SHEAR
    }

    /**
     * Immutable output consumed by both the chart and the displayed numerical
     * results. A safe result has a governing utilization no greater than one.
     */
    public record Analysis(
            MohrCircle compressionStrengthCircle,
            MohrCircle tensionStrengthCircle,
            MohrCircle bendingStressCircle,
            MohrCircle shearStressCircle,
            EnvelopeLine envelope,
            StressResults stresses,
            double envelopeUtilization,
            double shearUtilization,
            double governingUtilization,
            GoverningDemand governingDemand,
            double factorOfSafety,
            boolean safe) {
    }
}
