package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/** Beam-hull regression checks for the retained 2025 GirRaft preset. */
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
}
