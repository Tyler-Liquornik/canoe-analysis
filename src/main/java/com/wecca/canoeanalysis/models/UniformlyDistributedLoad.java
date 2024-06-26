package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class UniformlyDistributedLoad extends Load {

    @JsonIgnore
    private double magnitude;
    private Section section;

    public UniformlyDistributedLoad(double magnitude, double x, double rx) {
        super("Distributed Load");
        this.magnitude = magnitude;
        this.section = new Section(x, rx);
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
