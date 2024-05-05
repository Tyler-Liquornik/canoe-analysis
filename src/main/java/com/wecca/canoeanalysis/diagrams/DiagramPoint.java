package com.wecca.canoeanalysis.diagrams;

// Class for points on the diagrams
public class DiagramPoint
{

    // Fields
    private double x;
    private double y;

    // Constructor
    public DiagramPoint(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    // Accessors & Mutators
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public void setX(double x) {this.x = x;}
    public void setY(double y) {this.y = y;}

    // String representation for debugging
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
