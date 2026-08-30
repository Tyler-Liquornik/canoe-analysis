package com.wecca.canoeanalysis.components.graphics;

import javafx.scene.Node;

public interface Graphic {
    void draw();
    void recolor(boolean setColored);
    double getX();
    boolean isColored();

    default Node getNode() {
        if (this instanceof Node node)
            return node;
        else
            throw new RuntimeException("This Graphic is not an instance of Node");
    }
}
