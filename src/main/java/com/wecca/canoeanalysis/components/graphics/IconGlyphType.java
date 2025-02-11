package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stores easy access of the FontAwesomeFX glyph names for different icons used for module toolbar buttons
 * Note: To search for glyph names to use a new icon glyph, go to SceneBuilder > Library Settings (teeny tiny gear icon) > JAR/FXML Manager > Pencil Icon for fontawesomefx.jar > Glyphs Browser
 * If you're not seeing the JAR in there you need to upload it. See setup steps in README.md
 * Double underscore name signifies a badge by convention (small extra icon on top right)
 * For example: I wanted a pencil icon with an X badge on it to signify cancelling the effects of the pencil button
 * I added the following entries: X__PENCIL, X, PENCIL
 * If you add X__Y and either X or Y does not exist, or it's glyphName is not valid, X__Y will also not be valid and cause an error
 */
@Getter @AllArgsConstructor
public enum IconGlyphType {
    LEFT("CHEVRON_LEFT"),
    RIGHT("CHEVRON_RIGHT"),
    SWITCH("EXCHANGE"),
    PLUS("PLUS"),
    MINUS("MINUS"),
    WRENCH("WRENCH"),
    PENCIL("PENCIL"),
    DOWNLOAD("ARROW_CIRCLE_O_DOWN"),
    UPLOAD("ARROW_CIRCLE_O_UP"),
    CIRCLE("CIRCLE"),
    HALF_FILLED_CIRCLE("ADJUST"),
    RING("CIRCLE_ALT"),
    X("TIMES"),
    X__PENCIL("X__PENCIL"),
    BOOK("BOOK"),
    RESET("REFRESH");

    private final String glyphName;
}
