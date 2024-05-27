package com.wecca.canoeanalysis.graphics;

import javafx.scene.paint.Color;

public enum ColorPalette {
    PRIMARY("#BB86FC"),
    PRIMARY_VARIANT("#3700B3"),
    SECONDARY("#03DAC6"),
    BACKGROUND("#121212"),
    SURFACE("#202020"),
    ABOVE_SURFACE("#282828"),
    ERROR("#CF6679"),
    ICON("#000000"), // this is black here for now, white ugly need to fio
    TEXT_ON_SURFACE("#534B5E");

    private final Color color;

    ColorPalette(String hex) {
        this.color = Color.web(hex);
    }

    public Color getColor() {
        return color;
    }
}