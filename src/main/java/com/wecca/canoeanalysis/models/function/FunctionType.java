package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Provides metadata on the type of univariate function, may not always need to coincide directly with the actual subclass
 */
@Getter @AllArgsConstructor
public enum FunctionType {

    VERTEX_FORM_PARABOLA("Vertex Form Parabola", "f(x) = a(x - h)^2 + k"),
    RECT_FUNCTION("Rect Function", "H(x) = 0 : x not in (b, c), a : x in (b, c)"),
    CUBIC_BEZIER_FUNCTION("Cubic Bezier Function",
            "B_x(t) = (1 - t)^3 * x_1 + 3 * t * (1 - t)^2 * x_2 + 3 * t^2 * (1 - t) * x_3 + t^3 * x_4, " +
                    "B_y(t) = (1 - t)^3 * y_1 + 3 * t * (1 - t)^2 * y_2 + 3 * t^2 * (1 - t) * y_3 + t^3 * y_4, " +
                    "where 0 <= t <= 1, (x, y) = (B_x(t), B_y(t)),  and for each x, there exists exactly one y");

    @JsonIgnore
    private final String type;
    @JsonIgnore
    private final String equation;
}
