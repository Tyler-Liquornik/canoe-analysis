package com.wecca.canoeanalysis.models.load;

/**
 * Represents different results of adding a load to the canoe
 * PADDL will automatically combine points loads at the same x
 */
public enum AddLoadResult {
    ADDED,      // The load was directly added to the canoe
    COMBINED,   // The load was combined with an existing load
    REMOVED     // An existing load cancelled out completely with the new load
}
