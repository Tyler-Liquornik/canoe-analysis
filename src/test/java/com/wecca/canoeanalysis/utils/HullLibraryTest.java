package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.load.Load;
import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Geometry and self-weight regressions for the CAD-derived Raft Punk hull. */
class HullLibraryTest {

    @Test
    void raftPunkPresetMatchesCadEnvelopeAndStructuralZones() {
        Hull hull = HullLibrary.generateRaftPunkHull();

        // Envelope, volume, and segment counts protect the imported CAD fit.
        assertEquals(5.90, hull.getLength(), 1e-9);
        assertEquals(0.3605, hull.getMaxHeight(), 0.001);
        assertEquals(0.7220, hull.getMaxWidth(), 0.002);
        assertEquals(0.080881, hull.getConcreteVolume(), 0.002);
        assertEquals(23, hull.getSideViewSegments().size());
        assertEquals(23, hull.getTopViewSegments().size());

        // Exactly five sections receive the combined shell-plus-rib thickness.
        long ribSections = hull.getHullProperties().getThicknessMap().stream()
                .filter(section -> Math.abs(Double.parseDouble(section.getValue()) - 0.0429) < 1e-9)
                .count();
        assertEquals(5, ribSections);
        assertTrue(hull.getHullProperties().getThicknessMap().stream()
                .allMatch(section -> {
                    double thickness = Double.parseDouble(section.getValue());
                    return thickness == 0.007 || thickness == 0.0129 || thickness == 0.0429;
                }));
        assertFalse(hull.getHullProperties().getBulkheadMap().stream()
                .anyMatch(section -> Boolean.parseBoolean(section.getValue())));
        assertTrue(Math.abs(hull.getWeight()) > 0);
        assertEquals(RaftPunkPreset.CURED_DENSITY_KG_PER_M3, hull.getConcreteDensity(), 1e-9);
    }

    @Test
    void raftPunkPresetBuildsRenderableSelfWeightAcrossThicknessChanges() {
        Canoe canoe = new Canoe();
        canoe.setHull(HullLibrary.generateRaftPunkHull());

        // Section thickness steps must remain valid piece boundaries when the
        // canoe converts hull weight into a renderable load distribution.
        List<Load> loads = assertDoesNotThrow(canoe::getAllLoads);

        assertEquals(1, loads.size());
        PiecewiseContinuousLoadDistribution selfWeight =
                assertInstanceOf(PiecewiseContinuousLoadDistribution.class, loads.getFirst());
        assertEquals(canoe.getHull().getWeight(), selfWeight.getForce(), 1e-4);
    }
}
