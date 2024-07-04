package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.utils.MathUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import java.util.*;

@Getter @Setter
public class PiecewiseContinuousLoadDistribution extends LoadDistribution {

    private TreeMap<Section, UnivariateFunction> pieces;

    public PiecewiseContinuousLoadDistribution(String type, List<UnivariateFunction> pieces, List<Section> subSections) {
        super(type);
        validatePiecewiseContinuity(pieces, subSections);
        this.pieces = new TreeMap<>(Comparator.comparingDouble(Section::getX));
        for (int i = 0; i < pieces.size(); i++) {
            this.pieces.put(subSections.get(i), pieces.get(i));
        }
    }

    /**
     * Factory method to create a self-weight distribution from a hull
     * @param hull the hull to get the self-weight distribution of
     */
    public static PiecewiseContinuousLoadDistribution fromHull(Hull hull) {
        List<HullSection> hullSections = hull.getHullSections();
        List<Section> sections = hullSections.stream().map(hullSection -> (Section) hullSection).toList();
        hullSections.sort(Comparator.comparingDouble(Section::getX));

        List<UnivariateFunction> pieces = new ArrayList<>();
        for (HullSection section : hullSections) {
            pieces.add(section.getWeightDistributionFunction().getDistribution());
        }

        validatePiecewiseContinuity(pieces, sections);
        return new PiecewiseContinuousLoadDistribution("Hull Weight", pieces, sections);
    }

    /**
     * @return the function with pieces stitched together into a UnivariateFunction object
     */
    @JsonIgnore
    public UnivariateFunction getPiecedFunction() {
        return x -> pieces.entrySet().stream()
                .filter(piece -> x >= piece.getKey().getX() && x <= piece.getKey().getRx())
                .findFirst()
                .map(piece -> piece.getValue().value(x))
                .orElseThrow(() -> new IllegalArgumentException("x value out of bounds"));
    }

    /**
     * @return the integral over the distribution, a force in kN
     */
    @Override
    public double getForce() {
        return pieces.entrySet().stream().mapToDouble
                (piece -> MathUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(),
                        piece.getValue(), piece.getKey().getX(), piece.getKey().getRx())).sum();
    }

    @Override
    public double getX() {
        return getSection().getX();
    }

    /**
     * @return the maximum absolute value of the distribution, in kN/m
     */
    @Override
    public double getValue() {
        double maxValue = 0;
        for (Map.Entry<Section, UnivariateFunction> piece : pieces.entrySet()) {
            BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
            UnivariatePointValuePair result = optimizer.optimize(
                    MaxEval.unlimited(),
                    new UnivariateObjectiveFunction(x -> Math.abs(piece.getValue().value(x))),
                    GoalType.MAXIMIZE,
                    new SearchInterval(piece.getKey().getX(), piece.getKey().getRx())
            );
            if (result.getValue() > maxValue)
                maxValue = result.getValue();
        }
        return maxValue;
    }

    @JsonIgnore
    public Section getSection() {
        return new Section(pieces.firstKey().getX(), pieces.lastKey().getRx());
    }

    /**
     * Ensures a set of distributions will form a continuous curve when their intervals are joined
     * @param pieces the curves to validate
     */
    private static void validatePiecewiseContinuity(List<UnivariateFunction> pieces, List<Section> subSections) {
        if (subSections.size() != pieces.size())
            throw new IllegalArgumentException("Unequal amount of sections and pieces");

        for (int i = 1; i < pieces.size(); i++) {
            Section prevSec = subSections.get(i - 1);
            Section currSec = subSections.get(i);
            if (prevSec.getRx() != currSec.getX())
                throw new IllegalArgumentException("Sections do not form a continuous interval.");
        }
    }
}
