package com.wecca.canoeanalysis.components.graphics;

public interface Graphic {
    void draw();
    void recolor(boolean setColored);
    double getX();
    boolean isColored();
}
