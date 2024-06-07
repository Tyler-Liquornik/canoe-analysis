package com.wecca.canoeanalysis.components.diagrams;

/**
 * Inner class to hold load intervals (intermediate between point/distributed loads and diagram points).
 * Each interval is defined by its x coordinates (start - end), magnitude, and slope.
 * Solo point loads have a magnitude but no slope.
 * Solo distributed loads have a slope but no magnitude.
 * Combined loads may cause an interval to have both a magnitude and a slope.
 */
public class Interval
{
    public double startX;
    public double endX;
    public double magnitude;
    public double slope;

    public Interval(double sX, double eX, double magnitude, double slope)
    {
        this.startX = sX;
        this.endX = eX;
        this.magnitude = magnitude;
        this.slope = slope;
    }

    // For testing
    public String toString()
    {
        return "Interval from x = " + startX + " to x = "
                + endX + " with magnitude = " + magnitude + " and slope = " + slope;
    }
}
