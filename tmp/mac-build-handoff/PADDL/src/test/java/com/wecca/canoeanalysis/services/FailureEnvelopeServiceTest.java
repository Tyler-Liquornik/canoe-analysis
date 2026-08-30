package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.services.FailureEnvelopeService.Analysis;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.MohrCircle;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.TangencyPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formula-level regression checks for the combined Mohr-circle envelope. */
class FailureEnvelopeServiceTest {

    private static final double TOLERANCE = 1.0e-9;

    @Test
    void buildsBothStrengthAndBothDemandCirclesWithTheCorrectSignConvention() {
        Analysis analysis = reportScaleAnalysis();

        assertCircle(analysis.compressionStrengthCircle(), -4.9, 4.9);
        assertCircle(analysis.tensionStrengthCircle(), 1.0, 1.0);
        assertCircle(analysis.bendingStressCircle(), -0.1125, 0.3725);
        assertCircle(analysis.shearStressCircle(), 0.0, 0.256);
        assertEquals(-0.485, analysis.bendingStressCircle().minimumNormalStressMpa(), TOLERANCE);
        assertEquals(0.260, analysis.bendingStressCircle().maximumNormalStressMpa(), TOLERANCE);
    }

    @Test
    void calculatesSectionStressesInMpaFromDisplayedUnits() {
        Analysis analysis = reportScaleAnalysis();

        assertEquals(0.485, analysis.stresses().compressionMpa(), TOLERANCE);
        assertEquals(0.260, analysis.stresses().tensionMpa(), TOLERANCE);
        assertEquals(0.256, analysis.stresses().shearMpa(), TOLERANCE);
    }

    @Test
    void commonEnvelopeLineIsTangentToBothStrengthCircles() {
        Analysis analysis = reportScaleAnalysis();
        double slope = analysis.envelope().slope();
        double intercept = analysis.envelope().interceptMpa();
        double lineNormalLength = Math.sqrt(1.0 + slope * slope);

        // Perpendicular distance from each centre to a true tangent line must
        // equal that circle's radius.
        double compressionDistance = Math.abs(
                slope * analysis.compressionStrengthCircle().centerMpa() + intercept)
                / lineNormalLength;
        double tensionDistance = Math.abs(
                slope * analysis.tensionStrengthCircle().centerMpa() + intercept)
                / lineNormalLength;

        assertEquals(analysis.compressionStrengthCircle().radiusMpa(), compressionDistance, TOLERANCE);
        assertEquals(analysis.tensionStrengthCircle().radiusMpa(), tensionDistance, TOLERANCE);
        assertEquals(Math.sqrt(9.8 * 2.0) / 2.0, intercept, TOLERANCE);

        TangencyPoint compressionPoint = FailureEnvelopeService.upperTangencyPoint(
                analysis.compressionStrengthCircle(), analysis.envelope());
        TangencyPoint lowerCompressionPoint = FailureEnvelopeService.lowerTangencyPoint(
                analysis.compressionStrengthCircle(), analysis.envelope());
        assertEquals(
                analysis.envelope().upperShearMpa(compressionPoint.normalStressMpa()),
                compressionPoint.shearStressMpa(),
                TOLERANCE);
        assertEquals(
                analysis.envelope().lowerShearMpa(lowerCompressionPoint.normalStressMpa()),
                lowerCompressionPoint.shearStressMpa(),
                TOLERANCE);
        assertPointOnCircle(compressionPoint, analysis.compressionStrengthCircle());
        assertPointOnCircle(lowerCompressionPoint, analysis.compressionStrengthCircle());
        assertEquals(compressionPoint.normalStressMpa(), lowerCompressionPoint.normalStressMpa(), TOLERANCE);
        assertEquals(compressionPoint.shearStressMpa(), -lowerCompressionPoint.shearStressMpa(), TOLERANCE);
    }

    @Test
    void reportsWhetherAppliedDemandFitsInsideEnvelope() {
        Analysis safeAnalysis = reportScaleAnalysis();
        Analysis unsafeAnalysis = FailureEnvelopeService.analyze(
                9.8,
                2.0,
                0.0485,
                0.0260,
                0.000256,
                0.0001,
                0.01,
                10.0,
                10.0);

        assertTrue(safeAnalysis.safe());
        assertTrue(safeAnalysis.governingUtilization() < 1.0);
        assertTrue(!unsafeAnalysis.safe());
        assertTrue(unsafeAnalysis.governingUtilization() > 1.0);
    }

    @Test
    void rejectsZeroGeometryAndNegativeActions() {
        assertThrows(IllegalArgumentException.class, () -> FailureEnvelopeService.analyze(
                9.8, 2.0, 0.0, 0.026, 0.000256, 0.0001, 0.01, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> FailureEnvelopeService.analyze(
                9.8, 2.0, 0.0485, 0.026, 0.000256, 0.0001, 0.01, -1.0, 1.0));
    }

    private Analysis reportScaleAnalysis() {
        // Compact values make the expected stresses and circle geometry easy
        // to verify independently by hand.
        return FailureEnvelopeService.analyze(
                9.8,
                2.0,
                0.0485,
                0.0260,
                0.000256,
                0.0001,
                0.01,
                1.0,
                1.0);
    }

    private void assertCircle(MohrCircle circle, double expectedCenter, double expectedRadius) {
        assertEquals(expectedCenter, circle.centerMpa(), TOLERANCE);
        assertEquals(expectedRadius, circle.radiusMpa(), TOLERANCE);
    }

    private void assertPointOnCircle(TangencyPoint point, MohrCircle circle) {
        // Euclidean distance in the normal/shear plane must equal the radius.
        double distanceFromCenter = Math.hypot(
                point.normalStressMpa() - circle.centerMpa(),
                point.shearStressMpa());
        assertEquals(circle.radiusMpa(), distanceFromCenter, TOLERANCE);
    }
}
