package com.wecca.canoeanalysis.components.graphics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Stores easy access of the FontAwesomeFX glyph names for different icons used for module toolbar buttons
 * Note: To get these names, go to SceneBuilder > Library Settings (teeny tiny gear icon) > JAR/FXML Manager > Pencil Icon for fontawesomefx.jar > Glyphs Browser
 * If you're not seeing the JAR in there you need to upload it. See setup steps in README.md
 *
 * Note: double underscore name signifies a badge by convention (small extra icon on top right)
 * You must use this to create a badge, and you must also create an entry for the badge icon itself
 * For example: I wanted a pencil icon with an x on it to signify cancelling the effects of the button
 * I added the following two entries: X__PENCIL, X
 *
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
    X__PENCIL("X__PENCIL");

    private final String glyphName;
}
