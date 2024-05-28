package com.wecca.canoeanalysis.graphics;

import javafx.scene.paint.Color;
import lombok.Getter;

@Getter
public enum ColorPalette {
    PRIMARY("#BB86FC"),
    PRIMARY_VARIANT("#D2A8FF"),
    SECONDARY("#03DAC6"),
    BACKGROUND("#121212"),
    SURFACE("#202020"),
    ABOVE_SURFACE("#282828"),
    ERROR("#CF6679"),
    ICON("#FFFFFF"),
    TEXT_ON_SURFACE("#534B5E");

    private final Color color;

    ColorPalette(String hex) {
        this.color = Color.web(hex);
    }
}