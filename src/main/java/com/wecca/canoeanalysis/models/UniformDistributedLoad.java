package com.wecca.canoeanalysis.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class UniformDistributedLoad extends Load {

    private double rx;

    public UniformDistributedLoad(double mag, double x, double rx) {
        super("Distributed Load", mag, x);
        this.rx = rx;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", mag, x, rx);
    }
}
