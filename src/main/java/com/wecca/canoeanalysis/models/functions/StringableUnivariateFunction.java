package com.wecca.canoeanalysis.models.functions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.math3.analysis.UnivariateFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Allows the hull YAML model to parse string representations of functions that extends this class
 * There will be a set amount of function forms to choose from, which will grow as the model develops
 */
public interface StringableUnivariateFunction extends UnivariateFunction {
    @JsonIgnore
    Function<Double, Double> getFunction();
    @JsonIgnore
    Pattern getFunctionPattern();
    String getStringRepresentation();
}
