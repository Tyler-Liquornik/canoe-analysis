package com.wecca.canoeanalysis.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public final class ColorPalette {
    private static ColorPalette colorPalette = null;

    // Default color palette
    private Color primary = Color.web("#BB86FC");
    private Color primaryLight = Color.web("#D2A8FF");
    private Color primaryDesaturated = Color.web("#534B5E");
    private Color background = Color.web("#121212");
    private Color surface = Color.web("#202020");
    private Color aboveSurface = Color.web("#282828");
    private Color danger = Color.web("#D10647");
    private Color white = Color.web("#FFFFFF");

    // Private constructor to prevent instantiation
    private ColorPalette() {}

    // Static method to provide access to the single instance
    public static ColorPalette getInstance() {
        if (colorPalette == null)
            colorPalette = new ColorPalette();
        return colorPalette;
    }
}