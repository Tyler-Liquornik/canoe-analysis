package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class UniformDistributedLoad extends Load {

    private Section section;

    public UniformDistributedLoad(double mag, double x, double rx) {
        super("Distributed Load", mag);
        this.section = new Section(x, rx);
    }

    @Override
    public String toString() {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", mag, getX(), getRx());
    }

    @Override
    public double getX() {
        return section.getX();
    }

    public double getRx() {
        return section.getRx();
    }
}
