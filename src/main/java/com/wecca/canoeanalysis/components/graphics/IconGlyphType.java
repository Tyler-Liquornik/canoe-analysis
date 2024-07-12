package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stores easy access of the FontAwesomeFX glyph names for different icons used for module toolbar buttons
 */
@Getter @AllArgsConstructor
public enum IconGlyphType {
    WRENCH("WRENCH"),
    DOWNLOAD("ARROW_CIRCLE_O_DOWN"),
    UPLOAD("ARROW_CIRCLE_O_UP");

    private final String glyphName;
}
