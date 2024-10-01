package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import javafx.scene.shape.Rectangle;

/**
 * A graphic with a curve defined by some function
 */
public interface FunctionGraphic extends Graphic {
    Section getSection();
    BoundedUnivariateFunction getFunction();
    double getEndX();
    double getEndY();
    Rectangle getEncasingRectangle();
    double getHeight(double functionX);
}
