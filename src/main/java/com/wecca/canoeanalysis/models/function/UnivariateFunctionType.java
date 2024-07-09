package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides metadata on the type of univariate function, may not always need to coincide directly with the actual subclass
 * Will eventually grow with more functions, staying at just one for now
 */
@Getter @AllArgsConstructor
public enum UnivariateFunctionType {

    VERTEX_FORM_PARABOLA("Vertex Form Parabola", "a(x - h)^2 + k");

    @JsonIgnore
    private final String type;
    @JsonIgnore
    private final String equation;
}
