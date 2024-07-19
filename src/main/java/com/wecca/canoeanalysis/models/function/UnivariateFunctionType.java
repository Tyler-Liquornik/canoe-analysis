package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides metadata on the type of univariate function, may not always need to coincide directly with the actual subclass
 */
@Getter @AllArgsConstructor
public enum UnivariateFunctionType {

    VERTEX_FORM_PARABOLA("Vertex Form Parabola", "f(x) = a(x - h)^2 + k"),
    RECT_FUNCTION("Rect Function", "H(x) = 0 : x not in (b, c), a : x in (b, c)");

    @JsonIgnore
    private final String type;
    @JsonIgnore
    private final String equation;
}
