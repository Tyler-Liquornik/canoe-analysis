package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stores easy access of the fontFXNames used for different buttons
 */
@Getter @AllArgsConstructor
public enum IconGlyphName {
    WRENCH("WRENCH"),
    DOWNLOAD("ARROW_CIRCLE_O_DOWN"),
    UPLOAD("ARROW_CIRCLE_O_UP");

    private final String glyphName;
}
