package com.wecca.canoeanalysis.models;

public enum AddLoadResult {
    ADDED,      //The load was directly added to the canoe
    COMBINED,   //The load was combined with an existing load
    REMOVED     //An existing load cancelled out completely with the new load
}