package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.Section;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Parent interface for Beam & ClosedCurve, used to display the canoe hull
 */
public interface CurvedProfile {
    Section getSection();
    UnivariateFunction getFunction();
    double getLength();
    double getHeight(double x);
}
