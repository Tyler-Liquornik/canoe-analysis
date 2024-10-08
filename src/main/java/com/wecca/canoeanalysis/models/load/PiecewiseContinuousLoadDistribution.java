package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.FunctionSection;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.optim.MaxEval;

import java.util.*;

@Getter @Setter @EqualsAndHashCode(callSuper = true)
public class PiecewiseContinuousLoadDistribution extends LoadDistribution {

    @JsonProperty("pieces")
    private TreeMap<FunctionSection, BoundedUnivariateFunction> pieces;

    public PiecewiseContinuousLoadDistribution(LoadType type, List<BoundedUnivariateFunction> pieces, List<FunctionSection> subSections) {
        super(type, new FunctionSection(subSections.getFirst().getX(), subSections.getLast().getRx()));
        CalculusUtils.validatePiecewiseContinuity(pieces, subSections);
        this.pieces = new TreeMap<>(Comparator.comparingDouble(FunctionSection::getX));
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
        List<FunctionSection> sections = hullSections.stream().map(hullSection -> (FunctionSection) hullSection).toList();

        List<BoundedUnivariateFunction> pieces = new ArrayList<>();
        for (HullSection section : hullSections) {
            pieces.add(section.getWeightDistributionFunction().getDistribution());
        }

        CalculusUtils.validatePiecewiseContinuity(pieces, sections);
        CalculusUtils.validatePiecewiseAsUpOrDown(pieces, sections);
        return new PiecewiseContinuousLoadDistribution(LoadType.HULL, pieces, sections);
    }

    /**
     * @return the function with pieces stitched together into one function
     */
    @JsonIgnore
    public BoundedUnivariateFunction getPiecedFunction() {
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
                (piece -> CalculusUtils.integrator.integrate(MaxEval.unlimited().getMaxEval(),
                        piece.getValue(), piece.getKey().getX(), piece.getKey().getRx())).sum();
    }

    @Override
    public double getX() {
        return getSection().getX();
    }

    /**
     * @return the signed maximum absolute value (i.e. the lowest minimum or highest maximum) of the distribution, in kN/m
     */
    @Override
    public double getMaxSignedValue() {
        return getPiecedFunction().getMaxSignedValue(section);
    }

    @JsonIgnore
    public FunctionSection getSection() {
        return new FunctionSection(pieces.firstKey().getX(), pieces.lastKey().getRx());
    }

}
