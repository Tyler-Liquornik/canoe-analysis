package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.function.Function;

/**
 * For now, this will serve as the only function to define hull shape
 * More will be added later
 * The function modelled is a(x - h)^2 + k
 */
@Getter @Setter @EqualsAndHashCode
public class VertexFormParabola implements TypedUnivariateFunction {

    @JsonIgnore @Getter
    private final UnivariateFunctionType type = UnivariateFunctionType.VERTEX_FORM_PARABOLA;
    @JsonProperty("a")
    private double a;
    @JsonProperty("h")
    private double h;
    @JsonProperty("k")
    private double k;

    @JsonCreator
    public VertexFormParabola(@JsonProperty("a") double a,
                              @JsonProperty("h") double h,
                              @JsonProperty("k") double k) {
        super();
        this.a = a;
        this.h = h;
        this.k = k;
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
