package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

/**
 * For now, this will serve as the only function to define hull shape
 * More will be added later
 * The function modelled is a(x - h)^2 + k
 */
@Getter @Setter @EqualsAndHashCode
public class VertexFormParabola implements ParameterizedBoundedUnivariateFunction {

    @JsonIgnore @Getter
    private final FunctionType type = FunctionType.VERTEX_FORM_PARABOLA;
    @JsonProperty("a")
    private double a;
    @JsonProperty("h")
    private double h;
    @JsonProperty("k")
    private double k;

    @JsonCreator
    public VertexFormParabola(@JsonProperty("a") double a, @JsonProperty("h") double h, @JsonProperty("k") double k) {
        initialize(a, h, k);
    }

    @Override
    public void initialize(double... params) {
        if (params.length != 3)
            throw new IllegalArgumentException("VertexFormParabola requires exactly 3 parameters: a, h, and k.");
        this.a = params[0];
        this.h = params[1];
        this.k = params[2];
    }

    @JsonIgnore
    public Function<Double, Double> getFunction() {
        return x -> a * Math.pow((x - h), 2) + k;
    }

    @Override
    public double value(double v) {
        return getFunction().apply(v);
    }
}
