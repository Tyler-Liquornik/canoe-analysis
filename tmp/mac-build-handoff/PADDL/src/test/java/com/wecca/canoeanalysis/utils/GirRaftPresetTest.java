package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.services.FailureEnvelopeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cross-module regression checks for the published 2025 GirRaft preset. */
class GirRaftPresetTest {

    @Test
    void fullSizeHullUsesTheDocumentedGeometry() {
        Hull firstHull = GirRaftPreset.createHull();
        Hull secondHull = CanoePreset.GIRRAFT_2025.createFullSizeHull();

        assertNotSame(firstHull, secondHull);
        assertEquals(5.71, firstHull.getLength(), 1.0e-9);
        assertEquals(0.011, firstHull.getMaxThickness(), 1.0e-9);
        assertEquals(0.600, firstHull.getMaxWidth(), 1.0e-3);
        assertEquals("GirRaft (2025)", CanoePreset.GIRRAFT_2025.getDisplayName());
    }

    @Test
    void failureEnvelopeInputsReproduceThePublishedStresses() {
        FailureEnvelopeService.Analysis analysis = FailureEnvelopeService.analyze(
                GirRaftPreset.COMPRESSIVE_STRENGTH_MPA,
                GirRaftPreset.TENSILE_STRENGTH_MPA,
                GirRaftPreset.COMPRESSION_EDGE_DISTANCE_M,
                GirRaftPreset.TENSION_EDGE_DISTANCE_M,
                GirRaftPreset.FIRST_MOMENT_OF_AREA_M3,
                GirRaftPreset.SECOND_MOMENT_OF_AREA_M4,
                GirRaftPreset.SECOND_MOMENT_OF_AREA_M4,
                GirRaftPreset.NOMINAL_STRUCTURAL_THICKNESS_M,
                GirRaftPreset.MAXIMUM_MOMENT_KN_M,
                GirRaftPreset.FAILURE_ENVELOPE_MAXIMUM_SHEAR_KN);

        assertEquals(0.485, analysis.stresses().compressionMpa(), 0.001);
        assertEquals(0.260, analysis.stresses().tensionMpa(), 0.001);
        assertEquals(0.256, analysis.stresses().shearMpa(), 0.001);
        // The report labels the tangent intercept as 2.208 MPa, but its own
        // displayed strengths (9.8 and 2.0 MPa) produce sqrt(fc*ft)/2 below.
        // PADDL must keep the mathematically consistent value.
        assertEquals(Math.sqrt(9.8 * 2.0) / 2.0, analysis.envelope().interceptMpa(), 1.0e-9);
        assertTrue(analysis.safe());
    }

    @Test
    void punchingShearFieldsMatchThePublishedPaddlOutput() {
        double thicknessMm = GirRaftPreset.structuralThicknessMm();
        double widthMm = GirRaftPreset.nominalMaximumWidthMm();
        double strengthMpa = GirRaftPreset.COMPRESSIVE_STRENGTH_MPA;

        // These are the equations used by the original module and visible in
        // the 2025 report screenshot.
        double oneWayCapacityN = 0.65 * 0.75 * Math.sqrt(strengthMpa) * widthMm * thicknessMm;
        double criticalPerimeterMm = 4.0 * (30.0 + 2.0 * (thicknessMm / 2.0));
        double criticalAreaMm2 = criticalPerimeterMm * thicknessMm;
        double demandMpa = 625.3875 / criticalAreaMm2;
        double commonFactor = 0.75 * 0.65 * Math.sqrt(strengthMpa);
        double capacity1Mpa = 3.0 * 0.19 * commonFactor;
        double capacity2Mpa = (0.19 + 4.0 * thicknessMm / criticalPerimeterMm) * commonFactor;
        double capacity3Mpa = 2.0 * 0.19 * commonFactor;

        assertEquals(GirRaftPreset.PUNCHING_ONE_WAY_CAPACITY_N, oneWayCapacityN, 0.01);
        assertEquals(GirRaftPreset.PUNCHING_CRITICAL_PERIMETER_MM, criticalPerimeterMm, 0.01);
        assertEquals(GirRaftPreset.PUNCHING_CRITICAL_AREA_MM2, criticalAreaMm2, 0.01);
        assertEquals(GirRaftPreset.PUNCHING_DEMAND_MPA, demandMpa, 0.01);
        assertEquals(GirRaftPreset.PUNCHING_CAPACITY_1_MPA, capacity1Mpa, 0.0001);
        assertEquals(GirRaftPreset.PUNCHING_CAPACITY_2_MPA, capacity2Mpa, 0.0001);
        assertEquals(GirRaftPreset.PUNCHING_CAPACITY_3_MPA, capacity3Mpa, 0.0001);
        assertEquals(GirRaftPreset.PUNCHING_GOVERNING_CAPACITY_MPA,
                Math.min(capacity1Mpa, Math.min(capacity2Mpa, capacity3Mpa)),
                0.0001);
    }

    @Test
    void percentOpenAreaKeepsThePublishedPassResult() {
        assertEquals(40.00, GirRaftPreset.PASSING_OPEN_AREA_PERCENT, 1.0e-9);
        assertEquals(45.65, GirRaftPreset.PADDL_OPEN_AREA_PERCENT, 1.0e-9);
        assertTrue(GirRaftPreset.PADDL_OPEN_AREA_PERCENT >= GirRaftPreset.PASSING_OPEN_AREA_PERCENT);
    }
}
