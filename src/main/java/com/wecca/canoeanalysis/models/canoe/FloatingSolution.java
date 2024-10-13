package com.wecca.canoeanalysis.models.canoe;

import com.wecca.canoeanalysis.models.load.PiecewiseContinuousLoadDistribution;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * All the necessary data from solving a floating load case
 */
@Data @AllArgsConstructor
public class FloatingSolution {
    PiecewiseContinuousLoadDistribution solvedBuoyancy;
    double solvedH; // h is the height of the waterline, -canoeMaxHeight <= h <= 0
    double solvedTheta; // theta is the angle of the waterline -maxTilt <= theta <= maxTilt
}
