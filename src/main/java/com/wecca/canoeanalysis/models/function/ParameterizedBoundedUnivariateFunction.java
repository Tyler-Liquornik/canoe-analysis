package com.wecca.canoeanalysis.models.function;

public interface ParameterizedBoundedUnivariateFunction extends BoundedUnivariateFunction {
    void initialize(double... parameters);
}
