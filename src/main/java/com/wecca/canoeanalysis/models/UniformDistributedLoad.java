package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class UniformDistributedLoad implements Load {
    private double x; // left bound of the distributed load
    private double rX; // right bound of the distributed load
    private double w; // load in kN/m

    public double getTotalForce()
    {
        return w * (rX - x);
    }

    public double getXScaled(double containerWidth, double canoeLength)
    {
        return this.x / canoeLength * containerWidth;
    }

    public double getRXScaled(double containerWidth, double canoeLength)
    {
        return this.rX / canoeLength * containerWidth;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", w, x, rX);
    }
}
