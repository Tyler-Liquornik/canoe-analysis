package com.wecca.canoeanalysis.models;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class UniformDistributedLoad extends Load {

    private double rx; // right bound of the distributed load

    public UniformDistributedLoad(double mag, double x, double rx) {
        super("Distributed Load", mag, x);
        this.rx = rx;
    }

    public double getRxScaled(double containerWidth, double canoeLength)
    {
        return this.rx / canoeLength * containerWidth;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", mag, x, rx);
    }
}
