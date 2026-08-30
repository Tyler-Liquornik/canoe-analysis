package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.FloatingSolution;
import com.wecca.canoeanalysis.models.data.SolveType;
import com.wecca.canoeanalysis.models.load.PointLoad;
import com.wecca.canoeanalysis.services.BeamSolverService;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.services.FailureEnvelopeService;
import com.wecca.canoeanalysis.services.PunchingShearService;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-module regression checks for the shared preset. These tests protect
 * unit conversions, structural inputs, load placement, and diagram generation
 * so one module cannot silently diverge from the others.
 */
class RaftPunkPresetTest {

    @Test
    void sharedPresetProducesTheRecordedStructuralStresses() {
        // Use the distinct bending and shear inertias exactly as the preset UI does.
        FailureEnvelopeService.Analysis analysis = FailureEnvelopeService.analyze(
                RaftPunkPreset.COMPRESSIVE_STRENGTH_MPA,
                RaftPunkPreset.TENSILE_STRENGTH_MPA,
                RaftPunkPreset.BENDING_COMPRESSION_EDGE_DISTANCE_M,
                RaftPunkPreset.BENDING_TENSION_EDGE_DISTANCE_M,
                RaftPunkPreset.SHEAR_FIRST_MOMENT_OF_AREA_M3,
                RaftPunkPreset.BENDING_SECOND_MOMENT_OF_AREA_M4,
                RaftPunkPreset.SHEAR_SECOND_MOMENT_OF_AREA_M4,
                RaftPunkPreset.NOMINAL_STRUCTURAL_THICKNESS_M,
                RaftPunkPreset.MAXIMUM_MOMENT_KN_M,
                RaftPunkPreset.MAXIMUM_SHEAR_KN);

        assertEquals(0.414, analysis.stresses().compressionMpa(), 0.001);
        assertEquals(1.164, analysis.stresses().tensionMpa(), 0.001);
        assertEquals(1.031, analysis.stresses().shearMpa(), 0.001);
        assertNotEquals(RaftPunkPreset.REPORTED_SHEAR_STRESS_MPA,
                analysis.stresses().shearMpa(), 0.05,
                "The report's 1.130 MPa shear result does not follow from its listed V, Q, I, and t.");
        assertTrue(analysis.safe());
        assertEquals(-0.894, analysis.envelope().slope(), 0.001);
        assertEquals(1.866, analysis.envelope().interceptMpa(), 0.001);
    }

    @Test
    void moduleUnitConversionsStayConsistent() {
        assertEquals(11.0, RaftPunkPreset.structuralThicknessMm(), 1.0e-9);
        assertEquals(713.0, RaftPunkPreset.nominalMaximumWidthMm(), 1.0e-9);
        assertEquals(784.0, RaftPunkPreset.maximumShearN(), 1.0e-9);
        assertEquals(5.90, RaftPunkPreset.createHull().getLength(), 1.0e-9);
        assertEquals(6, RaftPunkPreset.LONGITUDINAL_LOAD_CASES.size());
        assertEquals("Men's Sprint", RaftPunkPreset.LONGITUDINAL_LOAD_CASES.get(1).name());
        assertEquals("Co-Ed Sprint", RaftPunkPreset.LONGITUDINAL_LOAD_CASES.get(4).name());
    }

    @Test
    void punchingShearPresetProducesCalculatedCapacities() {
        // Verify all intermediate values, not only the displayed governing result.
        PunchingShearService.OneWayResult oneWay = PunchingShearService.calculateOneWay(
                RaftPunkPreset.structuralThicknessMm(),
                RaftPunkPreset.nominalMaximumWidthMm(),
                RaftPunkPreset.COMPRESSIVE_STRENGTH_MPA);
        PunchingShearService.TwoWayResult twoWay = PunchingShearService.calculateTwoWay(
                RaftPunkPreset.structuralThicknessMm(),
                RaftPunkPreset.COMPRESSIVE_STRENGTH_MPA,
                RaftPunkPreset.maximumShearN(),
                RaftPunkPreset.PADDLER_CONTACT_AREA_MM2);

        assertEquals(7.92, oneWay.effectiveShearDepthMm(), 1.0e-9);
        assertEquals(1669.5200979515, oneWay.capacityN(), 1.0e-9);
        assertEquals(241.0890230021, twoWay.criticalPerimeterMm(), 1.0e-9);
        assertEquals(2651.9792530227, twoWay.criticalAreaMm2(), 1.0e-9);
        assertEquals(0.2956282554, twoWay.demandMpa(), 1.0e-9);
        assertEquals(0.8024768285, twoWay.capacityXMpa(), 1.0e-9);
        assertEquals(0.3959626208, twoWay.capacityYMpa(), 1.0e-9);
        assertEquals(0.5349845523, twoWay.capacityZMpa(), 1.0e-9);
        assertEquals(0.3959626208, twoWay.governingCapacityMpa(), 1.0e-9);
    }

    @Test
    void beamHullSelfWeightMatchesTheReportedAsBuiltCanoeMass() {
        double modeledMassKg = Math.abs(RaftPunkPreset.createHullWithAsBuiltMass().getWeight())
                * 1_000.0 / PhysicalConstants.GRAVITY.getValue();

        assertEquals(RaftPunkPreset.AS_BUILT_CANOE_MASS_KG, modeledMassKg, 0.01);
    }

    @Test
    void beamPresetBuildsAndSolvesTheReportedWomensSprintCase() {
        Canoe canoe = RaftPunkPreset.createWomensSprintBeamCanoe();
        List<PointLoad> paddlerLoads = canoe.getAllLoadsOfType(PointLoad.class);

        // The preset factory deliberately returns an unsolved canoe; the Beam
        // controller must exercise the same floating solver as a manual case.
        assertEquals(SolveType.UNSOLVED, canoe.getSolveType());
        assertEquals(5.90, canoe.getHull().getLength(), 1.0e-9);
        assertEquals(2, paddlerLoads.size());
        assertTrue(paddlerLoads.stream().anyMatch(load ->
                Math.abs(load.getX() - RaftPunkPreset.WOMENS_SPRINT_BOW_PADDLER_X_M) < 1.0e-9
                        && Math.abs(load.getForce() + RaftPunkPreset.WOMENS_SPRINT_PADDLER_LOAD_KN) < 1.0e-9));
        assertTrue(paddlerLoads.stream().anyMatch(load ->
                Math.abs(load.getX() - RaftPunkPreset.WOMENS_SPRINT_STERN_PADDLER_X_M) < 1.0e-9
                        && Math.abs(load.getForce() + RaftPunkPreset.WOMENS_SPRINT_PADDLER_LOAD_KN) < 1.0e-9));

        FloatingSolution solution = BeamSolverService.solveFloatingSystem(canoe);
        assertNotNull(solution);
        assertFalse(solution.isTippedOver());
        canoe.addLoad(solution.getSolvedBuoyancy());

        // Non-empty, non-flat diagrams confirm that preset loading survives the
        // entire solver-to-renderer path rather than only populating text fields.
        List<Point2D> sfdPoints = DiagramService.generateSfdPoints(canoe);
        List<Point2D> bmdPoints = DiagramService.generateBmdPoints(canoe);
        assertFalse(sfdPoints.isEmpty());
        assertFalse(bmdPoints.isEmpty());
        assertTrue(sfdPoints.stream().mapToDouble(point -> Math.abs(point.getY())).max().orElseThrow() > 0.5);
        assertTrue(bmdPoints.stream().mapToDouble(point -> Math.abs(point.getY())).max().orElseThrow() > 0.5);
    }

    @Test
    void punchingShearPresetBuildsTheGoverningFloatingLoadCase() {
        Canoe canoe = RaftPunkPreset.createGoverningPunchingShearCanoe();

        assertEquals(SolveType.FLOATING, canoe.getSolveType());
        assertEquals(RaftPunkPreset.MAXIMUM_SHEAR_KN, canoe.getSessionMaxShear(), 1.0e-9);
        assertEquals(4, canoe.getAllLoadsOfType(PointLoad.class).size());
        assertEquals(5.90, canoe.getHull().getLength(), 1.0e-9);

        FloatingSolution solution = BeamSolverService.solveFloatingSystem(canoe);
        assertNotNull(solution);
        assertFalse(solution.isTippedOver());
        canoe.addLoad(solution.getSolvedBuoyancy());

        // A visible response range guards the diagram behavior that previously
        // regressed when only scalar preset fields were populated.
        List<Point2D> diagramPoints = DiagramService.generateSfdPoints(canoe);
        assertFalse(diagramPoints.isEmpty());
        double shearRangeKn = diagramPoints.stream().mapToDouble(Point2D::getY).max().orElseThrow()
                - diagramPoints.stream().mapToDouble(Point2D::getY).min().orElseThrow();
        assertTrue(shearRangeKn > 0.5, "The preset shear diagram must contain a visible load response");
    }
}
