package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * More specific than the actual type (class) of the load, gives more control for naming loads in the UI and YAML models
 * Descriptions are intended to be more simplified and give extra metadata/context that Load subclass names don't provide
 * Variables just assign a char to each load to use as a variable for the load should it be used in an equation
 */
@Getter @AllArgsConstructor
public enum LoadType {
    POINT_LOAD("Point Load", 'p'),
    POINT_LOAD_SUPPORT("Point Support", 'p'),
    UNIFORM_LOAD_DISTRIBUTION("Distributed Load", 'd'),
    DISCRETE_SECTION("Section", 'd'),
    DISCRETE_HULL_SECTION_HAS_BULKHEAD("Bulkhead Section", 'd'),
    HULL("Hull Weight", 'w'),
    BUOYANCY("Buoyancy", 'w');

    @JsonIgnore
    private final String description;
    @JsonIgnore
    private final char variable;
}
