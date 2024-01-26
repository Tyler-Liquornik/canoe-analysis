package com.wecca.canoeanalysis;

public class PointLoad
{
    private double mag; // force magnitude (kN). + / - sign indicates up / down direction

    private double x; // force position on the canoe (m)


    public PointLoad(double mag, double x)
    {
        this.mag = mag; this.x = x;
    }

    public double getMag() {return mag;}
    public double getX() {return x;}

    public void setMag(double mag) {this.mag = mag;}
    public void setX(double x) {this.x = x;}

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return this.x / canoeLength * containerWidth;
    }

    @Override
    public String toString()
    {
        return String.format("Load: %.2fkN, %.2fm", mag, x);
    }
}
