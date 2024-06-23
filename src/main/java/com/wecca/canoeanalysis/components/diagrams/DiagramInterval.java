package com.wecca.canoeanalysis.components.diagrams;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class to hold load intervals (intermediate between point/distributed loads and diagram points).
 * Each interval is defined by its x coordinates (start - end), magnitude, and slope.
 * Solo point loads have a magnitude but no slope.
 * Solo distributed loads have a slope but no magnitude.
 * Combined loads may cause an interval to have both a magnitude and a slope.
 * Note: this is separate from the rest of the canoe model as it's rules for sectioning / loading differ
 */
@Data @AllArgsConstructor
public class DiagramInterval
{
    private double x;
    private double rx;
    private double magnitude;
    private double slope;

    public double getLength() {
        return rx - x;
    }
}
