package com.wecca.canoeanalysis.models;

public class PointLoad implements Load
{
    // Fields
    private double mag; // force magnitude (kN). + / - sign indicates up / down direction
    private double x; // force position on the canoe (m)
    private boolean isSupport; // if false, the load is a regular point load and not a support


    // Constructor assumes the load is not a support (the more common case)
    public PointLoad(double mag, double x)
    {
        this.mag = mag; this.x = x; this.isSupport = false;
    }

    // Accessors
    public double getMag() {return mag;}
    public double getX() {return x;}
    public boolean getIsSupport() {return isSupport;}

    // Mutators
    public void setMag(double mag) {this.mag = mag;}
    public void setX(double x) {this.x = x;}
    public void setIsSupport(boolean isSupport) {this.isSupport = isSupport;}

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return this.x / canoeLength * containerWidth;
    }

    // Stringification distinguishes supports from regular loads
    @Override
    public String toString()
    {
        String label = isSupport ? "Support" : "Load";
        return String.format("%s: %.2fkN, %.2fm",label, mag, x);
    }
}
