package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.utils.MathUtils;
import lombok.Getter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.*;
import java.util.List;

@Getter
public class ContinuousLoadDistribution extends PiecewiseContinuousLoadDistribution {

    private final UnivariateFunction distribution; // in kN/m by default
    private final Section section;

    /**
     * @param distribution defines the distribution
     * @param section defines the distributions endpoints
     * Note: the constructor is private as it is used by factory methods
     */
    public ContinuousLoadDistribution(String type, UnivariateFunction distribution, Section section) {
        super(type, List.of(distribution), List.of(section));
        validateContinuity(distribution, section);
        this.distribution = distribution;
        this.section = section;
    }

    @Override
    public double getForce() {
        return MathUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), distribution, section.x, section.rx);
    }

    @Override
    public double getX() {
        return section.x;
    }

    /**
     * @return the maximum value of the overall distribution which is considered to be it's maximum absolute value
     */
    @Override
    public double getValue() {
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
        UnivariatePointValuePair result = optimizer.optimize(
                MaxEval.unlimited(),
                new UnivariateObjectiveFunction(x -> Math.abs(distribution.value(x))),
                GoalType.MAXIMIZE,
                new SearchInterval(section.getX(), section.getRx())
        );
        return result.getValue();
    }

    /**
     * @param v the input value of the point to retrieve
     * @return y = distribution(x) at x = v
     */
    @JsonIgnore
    public double getValue(double v) {
        return distribution.value(v);
    }

    /**
     * Validates that the distribution is continuous over the section within a specified tolerance.
     * Note: tolerance and numSamples may need to be tweaked to avoid false invalidations
     */
    private void validateContinuity(UnivariateFunction distribution, Section section) {
        // Note: it's very possible this can cause false negatives and these numbers need to be tweaked in magnitude
        double tolerance = 1e-3;
        double numSamples = 10000.0;
        double step = section.getLength() / numSamples;

        double previousValue = distribution.value(section.getX());
        for (int i = 1; i < numSamples; i++) {
            double x = section.getX() + i * step;
            double currentValue = distribution.value(x);
            if (Math.abs(currentValue - previousValue) > tolerance)
                throw new IllegalArgumentException("The distribution is not continuous within a reasonable tolerance");
            previousValue = currentValue;
        }
    }
}
