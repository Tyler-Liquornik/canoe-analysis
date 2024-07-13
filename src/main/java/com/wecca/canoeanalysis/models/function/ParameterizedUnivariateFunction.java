package com.wecca.canoeanalysis.models.function;

public interface ParameterizedUnivariateFunction extends BoundedUnivariateFunction {
    void initialize(double... parameters);
}
