package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UniformDistributedLoad extends Load {

    private double rX; // right bound of the distributed load

    public UniformDistributedLoad(double x, double rX, double mag) {
        super(x, mag);
        this.rX = rX;
    }

    public double getRXScaled(double containerWidth, double canoeLength)
    {
        return this.rX / canoeLength * containerWidth;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", mag, x, rX);
    }
}
