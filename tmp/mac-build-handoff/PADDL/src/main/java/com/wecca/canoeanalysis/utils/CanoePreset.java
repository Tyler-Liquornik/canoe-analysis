package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;

/**
 * Identifies the yearly canoe data sets exposed by PADDL's shared preset
 * chooser. Each call to {@link #createFullSizeHull()} returns a fresh hull so
 * modules never share mutable geometry.
 */
public enum CanoePreset {
    GIRRAFT_2025("GirRaft", 2025),
    RAFT_PUNK_2026("Raft Punk", 2026);

    private final String canoeName;
    private final int year;

    CanoePreset(String canoeName, int year) {
        this.canoeName = canoeName;
        this.year = year;
    }

    /** @return the canoe's display name without its year */
    public String getCanoeName() {
        return canoeName;
    }

    /** @return the competition/report year associated with the preset */
    public int getYear() {
        return year;
    }

    /** @return the label shown in the shared preset popup */
    public String getDisplayName() {
        return canoeName + " (" + year + ")";
    }

    /**
     * Creates the canoe's hull at its documented full-size length.
     *
     * @return a new, independently mutable hull
     */
    public Hull createFullSizeHull() {
        return switch (this) {
            case GIRRAFT_2025 -> GirRaftPreset.createHull();
            case RAFT_PUNK_2026 -> RaftPunkPreset.createHull();
        };
    }
}
