package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;

/** GirRaft hull data retained for the Beam module's original preset workflow. */
public final class GirRaftPreset {

    /** Canoe name used by status messages. */
    public static final String NAME = "GirRaft";

    // Full-size length used when the Beam preset is selected before a manual length.
    public static final double LENGTH_M = HullLibrary.GIRRAFT_LENGTH;

    private GirRaftPreset() {
    }

    /** @return a new full-size GirRaft hull using the established PADDL model */
    public static Hull createHull() {
        return HullLibrary.generateGirRaftHullScaled(LENGTH_M);
    }
}
