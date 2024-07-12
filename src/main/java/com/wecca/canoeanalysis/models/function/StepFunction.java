package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

/**
 * Heaviside step function with a magnitude 'a'
 * H(x) = 0 if x <= c, a if x > c
 */
@Getter @Setter @EqualsAndHashCode
public class StepFunction implements ParameterizedUnivariateFunction {

    @JsonIgnore @Getter
    private final UnivariateFunctionType type = UnivariateFunctionType.STEP;
    @JsonProperty("a")
    private double a;
    @JsonProperty("c")
    private double c;

    @JsonCreator
    public StepFunction(@JsonProperty("a") double a, @JsonProperty("c") double c) {
        initialize(a, c);
    }

    @Override
    public void initialize(double... params) {
        if (params.length != 2)
            throw new IllegalArgumentException("StepFunction requires exactly 2 parameters: a and c.");
        this.a = params[0];
        this.c = params[1];
    }

    @JsonIgnore
    public Function<Double, Double> getFunction() {
        return x -> x <= c ? 0 : a;
    }

    @Override
    public double value(double v) {
        return getFunction().apply(v);
    }
}
