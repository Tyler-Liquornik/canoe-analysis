package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Rectangular function with a magnitude 'a'
 * H(x) = 0 if x is outside (b, c), a if x is within (b, c)
 */
@Getter @Setter @EqualsAndHashCode
public class RectFunction implements ParameterizedUnivariateFunction {

    @JsonIgnore @Getter
    private final UnivariateFunctionType type = UnivariateFunctionType.RECT_FUNCTION;

    @JsonProperty("a")
    private double a;

    @JsonProperty("b")
    private double b;

    @JsonProperty("c")
    private double c;

    @JsonCreator
    public RectFunction(@JsonProperty("a") double a, @JsonProperty("b") double b, @JsonProperty("c") double c) {
        initialize(a, b, c);
    }

    @Override
    public void initialize(double... params) {
        if (params.length != 3)
            throw new IllegalArgumentException("RectFunction requires exactly 3 parameters: a, b, and c.");
        this.a = params[0];
        this.b = params[1];
        this.c = params[2];
    }

    @JsonIgnore
    public BoundedUnivariateFunction getFunction() {
        return x -> (x > b && x < c) ? a : 0;
    }

    @Override
    public double value(double v) {
        return getFunction().value(v);
    }
}
