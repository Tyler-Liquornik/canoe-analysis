package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import javafx.scene.shape.Rectangle;

/**
 * Parent interface for Beam & ClosedCurve, used to display the canoe hull
 */
public interface HullGraphic extends Graphic {
    Section getSection();
    BoundedUnivariateFunction getFunction();
    double getEndX();
    double getEndY();
    Rectangle getEncasingRectangle();
    double getHeight(double functionX);
}
