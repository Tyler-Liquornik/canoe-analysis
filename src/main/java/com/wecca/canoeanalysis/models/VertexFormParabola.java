package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.function.Function;

/**
 * For now, this will serve as the only function to define hull shape
 * More will be added later
 * The function a(x - h)^2 + k
 */
@Getter @Setter @EqualsAndHashCode @AllArgsConstructor
public class VertexFormParabola implements UnivariateFunction {

    @JsonProperty("function")
    private final String functionString = "a(x - h)^2 + k";
    @JsonProperty("a")
    private double a;
    @JsonProperty("h")
    private double h;
    @JsonProperty("k")
    private double k;

    @JsonIgnore
    public Function<Double, Double> getFunction() {
        return x -> a * Math.pow((x - h), 2) + k;
    }

    @Override
    public double value(double v) {
        return getFunction().apply(v);
    }
}
