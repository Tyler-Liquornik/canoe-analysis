package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wecca.canoeanalysis.models.function.FunctionSection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @EqualsAndHashCode(callSuper = true)
public class UniformLoadDistribution extends LoadDistribution {

    @JsonProperty("magnitude")
    private double magnitude;

    @JsonCreator
    public UniformLoadDistribution(@JsonProperty("type") LoadType type,
                                   @JsonProperty("magnitude") double magnitude,
                                   @JsonProperty("section") FunctionSection section) {
        super(type, section);
        this.magnitude = magnitude;
    }

    public UniformLoadDistribution(LoadType type, double magnitude, double x, double rx) {
        super(type, new FunctionSection(x, rx));
        this.magnitude = magnitude;
        this.section = new FunctionSection(x, rx);
    }

    public UniformLoadDistribution(double magnitude, double x, double rx) {
        this(LoadType.UNIFORM_LOAD_DISTRIBUTION, magnitude, x, rx);
    }

    @JsonIgnore
    public double getForce() {
        return magnitude * section.getLength();
    }

    @JsonIgnore
    public double getMaxSignedValue() {
        return magnitude;
    }

    @Override
    public double getX() {
        return section.getX();
    }

    @JsonIgnore
    public double getRx() {
        return section.getRx();
    }
}
