package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A clickable button whose central feature is an icon
 */
@Getter
@Setter
public class IconButton extends Button {

    private FontAwesomeIcon icon;
    private FontAwesomeIcon badge;
    private double iconSize;

    /**
     * Constructs an IconButton with a Font Awesome icon, event handlers for press and release events, and initial CSS classes.
     * Private because only the factory methods should be used.
     *
     * @param iconGlyphName The icon to display on the button.
     * @param onClickFunction The action to perform when the button is pressed.
     * @param onReleaseFunction The action to perform when the button is released (optional).
     * @param cssClasses The initial CSS classes to apply to the button (optional)
     * @param iconSize The size of the icon
     */
    private IconButton(IconGlyphType iconGlyphName,
                       @Nullable IconGlyphType badgeGlyphName,
                       Consumer<MouseEvent> onClickFunction,
                       @Nullable Consumer<MouseEvent> onReleaseFunction,
                       @Nullable List<String> cssClasses, double iconSize) {
        FontAwesomeIcon icon = createIcon(iconGlyphName, iconSize);
        this.icon = icon;
        this.iconSize = iconSize;
        this.setGraphic(icon);
        this.setOnMouseClicked(onClickFunction::accept);
        if (onReleaseFunction != null) {
            this.setOnMouseClicked(null);
            this.setOnMousePressed(onClickFunction::accept);
            this.setOnMouseReleased(onReleaseFunction::accept);
        }
        if (cssClasses != null && !cssClasses.isEmpty())
            this.getStyleClass().addAll(cssClasses);
        if (badgeGlyphName != null)
            this.setBadgeIcon(badgeGlyphName, List.of("panel-button-badge"));
    }

    /**
     * @param iconGlyphName the name in the FontAwesomeFX library of the icon
     * @param size the size of the icon button
     * @return the icon to go in the button
     */
    private FontAwesomeIcon createIcon(IconGlyphType iconGlyphName, double size) {
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setFill(ColorPaletteService.getColor("white"));
        icon.setGlyphName(iconGlyphName.getGlyphName());
        icon.setSize(String.valueOf(size));
        return icon;
    }

    /**
     * Factory method to get a toolbar button (upload download, wrench, close button...)
     */
    public static IconButton getToolbarButton(IconGlyphType iconGlyphName, Consumer<MouseEvent> onClickFunction) {

        // Double underscore in the icon glyph name creates a badge with the icon glyph name of the chars before the double underscore
        IconGlyphType badgeGlyph = null;
        String glyphName = iconGlyphName.getGlyphName();
        int splitIndex = 0;
        if (glyphName.contains("__")) {
            splitIndex = glyphName.indexOf("__");
            badgeGlyph = IconGlyphType.valueOf(glyphName.substring(0, splitIndex));
            iconGlyphName = IconGlyphType.valueOf(glyphName.substring(splitIndex + 2));
        }

        IconButton button = new IconButton(iconGlyphName, badgeGlyph, onClickFunction, null,
                List.of("transparent-until-hover-button"), 25);

        // 1px difference is on purpose so the hover fill doesn't stick out of the toolbar
        double buttonHeight = 34.0;
        double buttonWidth = 35.0;
        button.setPrefHeight(buttonHeight);
        button.setPrefWidth(buttonWidth);
        button.setMaxHeight(buttonHeight);
        button.setMaxWidth(buttonWidth);
        button.setMinHeight(buttonHeight);
        button.setMinWidth(buttonWidth);
        return button;
    }

    /**
     * @param getPlus returns a plus button if true, otherwise a minus button
     * Factory method for the plus and minus buttons on the knobs in hull builder
     */
    public static IconButton getKnobPlusOrMinusButton(boolean getPlus, Consumer<MouseEvent> onPressFunction, Consumer<MouseEvent> onReleaseFunction, double iconSize) {
        IconGlyphType iconGlyphType = getPlus ? IconGlyphType.PLUS : IconGlyphType.MINUS;
        return new IconButton(iconGlyphType, null, onPressFunction, onReleaseFunction,
                List.of("transparent-until-hover-button", "transparent-on-hover-button"), iconSize);
    }

    /**
     * Factory method for the panel buttons (plus, switch panel) in hull builder
     */
    public static IconButton getPanelButton(IconGlyphType iconGlyphName, Consumer<MouseEvent> onClickFunction, double iconSize) {
        IconButton button = new IconButton(iconGlyphName, null, onClickFunction, null,
                List.of("panel-button"), iconSize);

        // 1px difference is on purpose so the hover fill doesn't stick out of the toolbar
        double buttonHeight = 25;
        double buttonWidth = 26;
        button.setPrefHeight(buttonHeight);
        button.setPrefWidth(buttonWidth);
        button.setMaxHeight(buttonHeight);
        button.setMaxWidth(buttonWidth);
        button.setMinHeight(buttonHeight);
        button.setMinWidth(buttonWidth);
        return button;
    }

    /**
     * Set the icon
     */
    public void setIcon(IconGlyphType iconGlyphName) {
        this.icon = createIcon(iconGlyphName, iconSize);
        this.setGraphic(icon);
    }

    /**
     * Set a badge, a small extra icon in the top-left corner of the main icon
     * @param badgeGlyphType the icon type
     * @param cssClasses styles if needed
     */
    public void setBadgeIcon(IconGlyphType badgeGlyphType, @Nullable List<String> cssClasses) {
        FontAwesomeIcon badgeIcon = createIcon(badgeGlyphType, this.iconSize / 2);
        badgeIcon.setGlyphName(badgeGlyphType.getGlyphName());
        badgeIcon.setFill(ColorPaletteService.getColor("white"));
        if (cssClasses != null && !cssClasses.isEmpty()) badgeIcon.getStyleClass().addAll(cssClasses);

        // Position the badge icon (top-left corner) and group it with the main icon
        badgeIcon.setTranslateX(-this.iconSize / 3);
        badgeIcon.setTranslateY(-this.iconSize / 3);
        this.setGraphic(new StackPane(this.icon, badgeIcon));
    }
}
