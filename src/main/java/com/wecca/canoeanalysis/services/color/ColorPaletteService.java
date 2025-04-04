package com.wecca.canoeanalysis.services.color;

import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the current color palette
 */
public class ColorPaletteService {
    private static final Map<String, Color> COLORS = new HashMap<>();

    // Base palette
    // TODO: implement dynamically changeable base palette between dark and light
    static {
        COLORS.put("background", Color.web("#121212"));
        COLORS.put("surface", Color.web("#202020"));
        COLORS.put("above-surface", Color.web("#282828"));
        COLORS.put("danger", Color.web("#D10647"));
        COLORS.put("white", Color.web("#FFFFFF"));
    }

    public static void putColor(String name, String colorHex) {
        COLORS.put(name, Color.web(colorHex));
    }

    public static Color getColor(String name) {
        return COLORS.get(name);
    }

    public static Map<String, Color> getColors() {
        return new HashMap<>(COLORS);
    }
}
