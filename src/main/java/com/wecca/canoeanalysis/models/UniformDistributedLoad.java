package com.wecca.canoeanalysis.models;

public class UniformDistributedLoad implements Load {
    private double x; // left bound of the distributed load
    private double rX; // right bound of the distributed load
    private double w; // load in kN/m

    public UniformDistributedLoad(double x, double r, double w)
    {
        this.x = x; this.rX = r; this.w = w;
    }


    public double getX() {return x;}
    public double getRX() {return rX;}
    public double getW() {return w;}
    public void setX(double x) {this.x = x;}
    public void setR(double r) {this.rX = r;}
    public void setW(double w) {this.w = w;}

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
