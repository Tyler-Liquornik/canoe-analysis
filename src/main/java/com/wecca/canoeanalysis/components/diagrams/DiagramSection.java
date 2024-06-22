package com.wecca.canoeanalysis.components.diagrams;

import com.wecca.canoeanalysis.models.Section;
import lombok.Getter;
import lombok.Setter;

/**
 * Class to hold load intervals (intermediate between point/distributed loads and diagram points).
 * Each interval is defined by its x coordinates (start - end), magnitude, and slope.
 * Solo point loads have a magnitude but no slope.
 * Solo distributed loads have a slope but no magnitude.
 * Combined loads may cause an interval to have both a magnitude and a slope.
 */
@Getter @Setter
public class DiagramSection
{
    private Section section;
    private double magnitude;
    private double slope;

    public DiagramSection(double x, double rx, double magnitude, double slope)
    {
        this.section = new Section(x, rx);
        this.magnitude = magnitude;
        this.slope = slope;
    }
}
