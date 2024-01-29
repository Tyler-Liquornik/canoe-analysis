package com.wecca.canoeanalysis;

public class UniformDistributedLoad
{
    private double lX; // left bound of the distributed load
    private double rX; // right bound of the distributed load
    private double w; // load in kN/m

    public UniformDistributedLoad(double l, double r, double w)
    {
        this.lX = l; this.rX = r; this.w = w;
    }

    public double getLX() {return lX;}
    public double getRX() {return rX;}
    public double getW() {return w;}
    public void setL(double l) {this.lX = l;}
    public void setR(double r) {this.rX = r;}
    public void setW(double w) {this.w = w;}

    public double getTotalForce()
    {
        return w * (rX - lX);
    }

    public double getLXScaled(double containerWidth, double canoeLength)
    {
        return this.lX / canoeLength * containerWidth;
    }

    public double getRXScaled(double containerWidth, double canoeLength)
    {
        return this.rX / canoeLength * containerWidth;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", w, lX, rX);
    }
}
