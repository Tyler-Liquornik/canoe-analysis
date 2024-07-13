package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import java.util.List;

@Getter @EqualsAndHashCode(callSuper = true)
public class ContinuousLoadDistribution extends PiecewiseContinuousLoadDistribution {

    @JsonProperty("distribution")
    private final BoundedUnivariateFunction distribution; // in kN/m by default
    @JsonProperty("section")
    private final Section section;

    /**
     * @param distribution defines the distribution
     * @param section defines the distributions endpoints
     * Note: the constructor is private as it is used by factory methods
     */
    public ContinuousLoadDistribution(LoadType type, BoundedUnivariateFunction distribution, Section section) {
        super(type, List.of(distribution), List.of(section));
        CalculusUtils.validateContinuity(distribution, section);
        this.distribution = distribution;
        this.section = section;
    }

    @Override
    public double getForce() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), distribution, section.getX(), section.getRx());
    }

    @Override
    public double getX() {
        return section.getX();
    }

    /**
     * @param v the input value of the point to retrieve
     * @return y = distribution(x) at x = v
     */
    @JsonIgnore
    public double getValue(double v) {
        return distribution.value(v);
    }
}
