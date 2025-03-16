package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.optim.MaxEval;
import java.util.*;

@Getter @EqualsAndHashCode(callSuper = true)
public class ContinuousLoadDistribution extends PiecewiseContinuousLoadDistribution {

    /**
     * @param distribution defines the distribution
     * @param section defines the distributions endpoints
     * Note: the constructor is private as it is used by factory methods
     */
    public ContinuousLoadDistribution(LoadType type, BoundedUnivariateFunction distribution, Section section) {
        super(type, List.of(distribution), List.of(section));
        CalculusUtils.validateContinuity(distribution, section);
        TreeMap<Section, BoundedUnivariateFunction> pieces = new TreeMap<>(Comparator.comparingDouble(Section::getX));
        pieces.put(section, distribution);
        this.pieces = pieces;
        this.section = section;
    }

    @Override
    public double getForce() {
        return CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(), pieces.get(section), section.getX(), section.getRx());
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
        return pieces.get(section).value(v);
    }
}
