package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;

/**
 * GirRaft values transcribed from Western University's 2025 project proposal.
 * Geometry is stored in SI units, while punching-shear inputs retain the
 * millimetre/newton units displayed by that PADDL module.
 */
public final class GirRaftPreset {

    /** Canoe name used by status messages. */
    public static final String NAME = "GirRaft";

    // Geometry Summary, Table 3 (proposal page 12 / printed page 8).
    public static final double LENGTH_M = HullLibrary.GIRRAFT_LENGTH;
    public static final double NOMINAL_MAX_WIDTH_M = 0.600;
    public static final double NOMINAL_MAX_DEPTH_M = 0.396;
    public static final double NOMINAL_STRUCTURAL_THICKNESS_M = 0.011;

    // Critical-section calculation (proposal pages 31-32 / printed pages 28-29).
    public static final double COMPRESSION_EDGE_DISTANCE_M = 0.25802;
    public static final double TENSION_EDGE_DISTANCE_M = 0.13798;
    public static final double FIRST_MOMENT_OF_AREA_M3 = 0.0010517;
    public static final double SECOND_MOMENT_OF_AREA_M4 = 2.077e-4;
    public static final double MAXIMUM_MOMENT_KN_M = 0.3907;
    public static final double FAILURE_ENVELOPE_MAXIMUM_SHEAR_KN = 0.5567;

    // Material strengths used by the proposal's failure-envelope plots.
    public static final double COMPRESSIVE_STRENGTH_MPA = 9.8;
    public static final double TENSILE_STRENGTH_MPA = 2.0;

    // PADDL Punching Shear screenshot (proposal page 32 / printed page 29).
    public static final double PUNCHING_MAXIMUM_SHEAR_N = 551.91;
    public static final double PUNCHING_ONE_WAY_CAPACITY_N = 10072.37;
    public static final double PUNCHING_CRITICAL_PERIMETER_MM = 164.00;
    public static final double PUNCHING_CRITICAL_AREA_MM2 = 1804.00;
    public static final double PUNCHING_DEMAND_MPA = 0.35;
    public static final double PUNCHING_CAPACITY_1_MPA = 0.8699;
    public static final double PUNCHING_CAPACITY_2_MPA = 0.6994;
    public static final double PUNCHING_CAPACITY_3_MPA = 0.5799;
    public static final double PUNCHING_GOVERNING_CAPACITY_MPA = 0.5799;

    // Percent Open Area result (proposal page 35 / printed page 32).
    public static final double PASSING_OPEN_AREA_PERCENT = 40.00;
    public static final double PADDL_OPEN_AREA_PERCENT = 45.65;

    private GirRaftPreset() {
    }

    /** @return a new full-size GirRaft hull using the established PADDL model */
    public static Hull createHull() {
        return HullLibrary.generateGirRaftHullScaled(LENGTH_M);
    }

    /** @return nominal shell thickness converted from metres to millimetres */
    public static double structuralThicknessMm() {
        return NOMINAL_STRUCTURAL_THICKNESS_M * 1_000.0;
    }

    /** @return documented maximum hull width converted to millimetres */
    public static double nominalMaximumWidthMm() {
        return NOMINAL_MAX_WIDTH_M * 1_000.0;
    }
}
