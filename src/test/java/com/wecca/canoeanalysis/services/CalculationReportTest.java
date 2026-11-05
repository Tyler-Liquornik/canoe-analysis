package com.wecca.canoeanalysis.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculationReportTest {
    @Test void oneWayTraceUsesActualIntermediateResults() {
        CalculationReport report = new CalculationReport("One way");
        var result = PunchingShearService.calculateOneWay(12, 700, 30, report);
        assertEquals(3, report.steps().size());
        assertEquals(result.effectiveShearDepthMm(), report.steps().get(1).value());
        assertEquals(result.capacityN(), report.steps().get(2).value());
        assertTrue(report.html().contains("<msqrt>"));
        assertTrue(report.html().contains("<mfrac>"));
    }
    @Test void twoWayTraceIncludesEveryCandidateAndMinimum() {
        CalculationReport report = new CalculationReport("Two way");
        var result = PunchingShearService.calculateTwoWay(12, 30, 625, 900, report);
        assertEquals(11, report.steps().size());
        assertEquals(result.governingCapacityMpa(), report.steps().get(10).value());
        assertTrue(report.text().contains("MPa"));
    }
    @Test void stressTraceRetainsDistinctBendingAndShearInertias() {
        CalculationReport report = new CalculationReport("Stresses");
        var result = FailureEnvelopeService.calculateSectionStresses(.1, .2, .001, .0002, .0003, .01, 4, 2, report);
        assertEquals(3, report.steps().size());
        assertEquals(result.shearMpa(), report.steps().get(2).value());
        assertTrue(report.steps().get(2).substitution().contains(CalculationReport.number(.0003)));
    }
    @Test void exportEscapesTextAndSnapshotCannotBeMutatedThroughSteps() {
        CalculationReport report = new CalculationReport("<script>");
        report.add("x", "a / b", "4 / 2", 2, "mm");
        assertFalse(report.html().contains("<script>"));
        assertThrows(UnsupportedOperationException.class, () -> report.steps().clear());
        assertTrue(ReportMath.render("a / (b × c)").contains("<mfrac>"));
        assertTrue(ReportMath.render("√(a × b) / 2").contains("<msqrt>"));
    }
}
