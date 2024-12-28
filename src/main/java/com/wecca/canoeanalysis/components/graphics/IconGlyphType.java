package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stores easy access of the FontAwesomeFX glyph names for different icons used for module toolbar buttons
 * Note: To get these names, go to SceneBuilder > Library Settings (teeny tiny gear icon) > JAR/FXML Manager > Pencil Icon for fontawesomefx.jar > Glyphs Browser
 * If you're not seeing the JAR in there you need to upload it. See setup steps in README.md
 */
@Getter @AllArgsConstructor
public enum IconGlyphType {
    LEFT("CHEVRON_LEFT"),
    RIGHT("CHEVRON_RIGHT"),
    SWITCH("EXCHANGE"),
    PLUS("PLUS"),
    MINUS("MINUS"),
    WRENCH("WRENCH"),
    SCISSORS("CUT"),
    CHAIN("CHAIN"),
    DOWNLOAD("ARROW_CIRCLE_O_DOWN"),
    UPLOAD("ARROW_CIRCLE_O_UP"),
    CIRCLE("CIRCLE"),
    HALF_FILLED_CIRCLE("ADJUST"),
    RING("CIRCLE_ALT");

    private final String glyphName;
}
