package com.wecca.canoeanalysis;

public class UniformDistributedLoad
{
    private double l; // left bound of the distributed load
    private double r; // right bound of the distributed load
    private double w; // load in kN/m

    public UniformDistributedLoad(double l, double r, double w)
    {
        this.l = l; this.r = r; this.w = w;
    }

    public double getL() {return l;}
    public double getR() {return r;}
    public double getW() {return w;}
    public void setL(double l) {this.l = l;}
    public void setR(double r) {this.r = r;}
    public void setW(double w) {this.w = w;}

    public double getTotalForce()
    {
        return w * (r - l);
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN/m, [%.2fm, %.2fm]", w, l, r);
    }
}
