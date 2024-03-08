package com.wecca.canoeanalysis.diagrams;

//Class for points on the diagrams
public class DiagramPoint {

    //Simple x and y coordinates
    private double x;
    private double y;

    public DiagramPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    //For debugging
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
