package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.data.SolveType;
import com.wecca.canoeanalysis.models.load.PointLoad;

import java.util.List;

/**
 * Authoritative Raft Punk values shared by the preset button in each PADDL module.
 *
 * <p>Unless explicitly identified as a CAD measurement, every value below is
 * transcribed from Western University's 2026 Raft Punk project proposal. The
 * longitudinal geometry was independently checked against the supplied final
 * SolidWorks exports. Units are the SI units expected by PADDL.</p>
 */
public final class RaftPunkPreset {

    /** Display name used by module preset buttons and status messages. */
    public static final String NAME = "Raft Punk";

    // Table 5, Geometry Summary (report page 11 / printed page 7).
    public static final double LENGTH_M = HullLibrary.RAFT_PUNK_LENGTH;
    public static final double NOMINAL_MAX_WIDTH_M = 0.713;
    public static final double NOMINAL_MAX_DEPTH_M = 0.350;
    public static final double BOW_TO_MAX_WIDTH_M = 2.980;
    public static final double STERN_TO_MAX_WIDTH_M = 2.920;
    public static final double NOMINAL_STRUCTURAL_THICKNESS_M = 0.011;

    // Appendix A, SolidWorks section properties (report page 27 / printed page 23).
    public static final double BENDING_SECTION_X_M = 2.950;
    public static final double BENDING_COMPRESSION_EDGE_DISTANCE_M = 0.08924;
    public static final double BENDING_TENSION_EDGE_DISTANCE_M = 0.25076;
    public static final double BENDING_FIRST_MOMENT_OF_AREA_M3 = 2.716e-3;
    public static final double BENDING_SECOND_MOMENT_OF_AREA_M4 = 1.787e-4;

    public static final double SHEAR_SECTION_X_M = 4.000;
    public static final double SHEAR_SECTION_WIDTH_M = 0.420;
    public static final double SHEAR_COMPRESSION_EDGE_DISTANCE_M = 0.06800;
    public static final double SHEAR_TENSION_EDGE_DISTANCE_M = 0.24288;
    public static final double SHEAR_FIRST_MOMENT_OF_AREA_M3 = 2.479e-3;
    public static final double SHEAR_SECOND_MOMENT_OF_AREA_M4 = 1.713e-4;

    // Table 1 and Appendix A governing longitudinal actions.
    public static final double MAXIMUM_MOMENT_KN_M = 0.8293;
    public static final double MAXIMUM_SHEAR_KN = 0.7840;

    // Tables 2, 4, and 6: 28-day longitudinal material capacities.
    public static final double COMPRESSIVE_STRENGTH_MPA = 8.34;
    public static final double TENSILE_STRENGTH_MPA = 1.67;

    // Table 6 and Appendix B as-built/material properties.
    public static final double CURED_DENSITY_KG_PER_M3 = 832.11;
    public static final double WET_DENSITY_KG_PER_M3 = 1051.32;
    public static final double AS_BUILT_CANOE_MASS_KG = 89.09;
    public static final double MEASURED_CRITICAL_HULL_THICKNESS_M = 0.0129;
    public static final double RIB_RADIUS_M = 0.030;
    public static final double GUNWALE_RADIUS_M = 0.030;

    // Appendix B and Figure 14: Woman's Sprint beam-validation setup.
    // The report models each 62 kg female paddler as a rounded 608 N point load.
    public static final double WOMENS_SPRINT_PADDLER_LOAD_KN = 0.608;
    public static final double WOMENS_SPRINT_BOW_PADDLER_X_M = 1.00;
    public static final double WOMENS_SPRINT_STERN_PADDLER_X_M = 4.90;

    // Deterministic square-equivalent paddler contact patch used by the
    // punching-shear preset; the hull CAD does not define this load footprint.
    public static final double PADDLER_CONTACT_AREA_MM2 = 3000.0;

    // Percent-open-area report results (report page 31 / printed page 27).
    public static final double PASSING_OPEN_AREA_PERCENT = 40.0;
    public static final double PADDL_OPEN_AREA_PERCENT = 73.89;
    public static final double HAND_CALCULATED_OPEN_AREA_PERCENT = 75.13;

    // Published results retained for comparison with PADDL's calculations.
    public static final double REPORTED_TENSION_STRESS_MPA = 1.164;
    public static final double REPORTED_COMPRESSION_STRESS_MPA = 0.414;
    public static final double REPORTED_SHEAR_STRESS_MPA = 1.130;

    // Table 1: all six longitudinal load-case results recorded by the report.
    public static final List<LongitudinalLoadCase> LONGITUDINAL_LOAD_CASES = List.of(
            new LongitudinalLoadCase("Women's Sprint", 598.8, 1.00, 683.1, 2.95),
            new LongitudinalLoadCase("Men's Sprint", 747.2, 1.00, 829.3, 2.95),
            new LongitudinalLoadCase("Women's Time Trial", 551.2, 1.00, 428.6, 2.39),
            new LongitudinalLoadCase("Men's Time Trial", 685.7, 1.00, 505.0, 2.37),
            new LongitudinalLoadCase("Co-Ed Sprint", 784.0, 4.00, 757.9, 2.95),
            new LongitudinalLoadCase("On Stands", 285.0, 0.00, 454.9, 2.95));

    private RaftPunkPreset() {
    }

    /**
     * Creates a fresh copy of the CAD-derived hull so modules cannot share
     * mutable hull state with one another.
     *
     * @return an independent Raft Punk hull model
     */
    public static Hull createHull() {
        return HullLibrary.generateRaftPunkHull();
    }

    /**
     * Returns the CAD-derived hull with its modeled density calibrated so the
     * beam module's integrated self-weight equals the report's 89.09 kg total
     * as-built canoe mass (concrete, reinforcement, and fittings combined).
     */
    public static Hull createHullWithAsBuiltMass() {
        Hull hull = createHull();
        hull.setConcreteDensity(AS_BUILT_CANOE_MASS_KG / hull.getConcreteVolume());
        return hull;
    }

    /**
     * Build the Woman's Sprint floating load case shown in Figure 14 of the
     * report, before buoyancy is solved by the Beam module. This uses the
     * report's cured concrete density with the CAD-derived hull geometry.
     */
    public static Canoe createWomensSprintBeamCanoe() {
        Canoe canoe = new Canoe();
        canoe.setHull(createHull());
        canoe.addLoad(new PointLoad(-WOMENS_SPRINT_PADDLER_LOAD_KN,
                WOMENS_SPRINT_BOW_PADDLER_X_M, false));
        canoe.addLoad(new PointLoad(-WOMENS_SPRINT_PADDLER_LOAD_KN,
                WOMENS_SPRINT_STERN_PADDLER_X_M, false));
        return canoe;
    }

    /**
     * Build the governing Co-Ed Sprint canoe used by the Punching Shear preset.
     * The floating solver supplies the buoyancy distribution through the
     * module's normal {@code setValues} workflow.
     */
    public static Canoe createGoverningPunchingShearCanoe() {
        Canoe canoe = new Canoe();
        canoe.setHull(createHullWithAsBuiltMass());

        // Convert the four paddler masses to downward point loads in the kN
        // convention used throughout PADDL's beam and diagram services.
        double femalePaddlerLoadKn = -(62.0 * PhysicalConstants.GRAVITY.getValue()) / 1_000.0;
        double malePaddlerLoadKn = -(80.0 * PhysicalConstants.GRAVITY.getValue()) / 1_000.0;
        canoe.addLoad(new PointLoad(femalePaddlerLoadKn, 1.00, false));
        canoe.addLoad(new PointLoad(malePaddlerLoadKn, 2.00, false));
        canoe.addLoad(new PointLoad(malePaddlerLoadKn, 4.00, false));
        canoe.addLoad(new PointLoad(femalePaddlerLoadKn, 4.90, false));

        canoe.setSolveType(SolveType.FLOATING);
        canoe.setSessionMaxShear(MAXIMUM_SHEAR_KN);
        return canoe;
    }

    /** @return nominal structural shell thickness converted from metres to millimetres */
    public static double structuralThicknessMm() {
        return NOMINAL_STRUCTURAL_THICKNESS_M * 1_000.0;
    }

    /** @return nominal maximum hull width converted from metres to millimetres */
    public static double nominalMaximumWidthMm() {
        return NOMINAL_MAX_WIDTH_M * 1_000.0;
    }

    /** @return governing longitudinal shear converted from kilonewtons to newtons */
    public static double maximumShearN() {
        return MAXIMUM_SHEAR_KN * 1_000.0;
    }

    /**
     * One row of the longitudinal load-case summary. Force is stored in
     * newtons, moment in newton-metres, and both locations in metres from the
     * bow so consumers do not need to infer units from the table values.
     */
    public record LongitudinalLoadCase(
            String name,
            double maximumShearN,
            double maximumShearLocationM,
            double maximumMomentNm,
            double maximumMomentLocationM) {
    }
}
