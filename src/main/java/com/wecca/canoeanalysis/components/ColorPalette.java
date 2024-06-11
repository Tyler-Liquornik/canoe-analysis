package com.wecca.canoeanalysis.components;

import javafx.scene.paint.Color;
import lombok.Getter;

@Getter
public enum ColorPalette {
    PRIMARY("#BB86FC"),
    PRIMARY_LIGHT("#D2A8FF"),
    PRIMARY_DESATURATED("#534B5E"),
    BACKGROUND("#121212"),
    SURFACE("#202020"),
    ABOVE_SURFACE("#282828"),
    DANGER("#D10647"),
    WHITE("#FFFFFF");

    private final Color color;

    ColorPalette(String hex) {
        this.color = Color.web(hex);
    }
}