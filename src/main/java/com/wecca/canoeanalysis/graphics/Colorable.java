package com.wecca.canoeanalysis.graphics;

import javafx.scene.paint.Color;

public interface Colorable {
    double LIGHTEN_FACTOR = 0.8;
    void recolor(Color color);
}
