package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class UniformLoadDistribution extends LoadDistribution {

    @JsonIgnore
    private double magnitude;
    private Section section;

    public UniformLoadDistribution(String type, double magnitude, double x, double rx) {
        super(type);
        this.magnitude = magnitude;
        this.section = new Section(x, rx);
    }

    public UniformLoadDistribution(double magnitude, double x, double rx) {
        this("Uniform Load Distribution", magnitude, x, rx);
    }

    @Override
    public String toString() {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", magnitude, getX(), getRx());
    }

    @JsonIgnore
    public double getForce() {
        return magnitude * section.getLength();
    }

    @JsonIgnore
    public double getValue() {
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
