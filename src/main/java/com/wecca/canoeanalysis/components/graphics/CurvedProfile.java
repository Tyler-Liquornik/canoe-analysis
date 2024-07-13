package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;

/**
 * Parent interface for Beam & ClosedCurve, used to display the canoe hull
 */
public interface CurvedProfile extends Graphic {
    Section getSection();
    BoundedUnivariateFunction getFunction();
    double getStartX();
    double getEndX();
    double getStartY();
    double getEndY();
    double getLength();
    double getHeight(double functionX);
}
