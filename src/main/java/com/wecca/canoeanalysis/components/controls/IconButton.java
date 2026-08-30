package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
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

    private Node icon;
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
        Node icon = createIcon(iconGlyphName, iconSize);
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
    private Node createIcon(IconGlyphType iconGlyphName, double size) {
        if (iconGlyphName == IconGlyphType.CANOE) {
            return createCanoeIcon(size);
        }

        return createFontAwesomeIcon(iconGlyphName, size);
    }

    /** Creates a normal FontAwesome glyph for all non-canoe buttons. */
    private FontAwesomeIcon createFontAwesomeIcon(IconGlyphType iconGlyphName, double size) {
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setFill(ColorPaletteService.getColor("white"));
        icon.setGlyphName(iconGlyphName.getGlyphName());
        icon.setSize(String.valueOf(size));
        return icon;
    }

    /**
     * Draws a compact two-paddler canoe inspired by the supplied team artwork.
     * Vector strokes keep the toolbar graphic crisp on both standard and
     * high-DPI Windows/macOS displays.
     */
    private Node createCanoeIcon(double size) {
        Color strokeColor = ColorPaletteService.getColor("white");

        // The upper curve is the gunwale and the lower curve forms the hull.
        SVGPath hull = strokedPath(
                "M1.5,13 C5.0,15.0 20.0,15.0 23.5,13 "
                        + "M1.5,13 C2.6,18.2 5.2,20.2 12.5,20.2 "
                        + "C19.8,20.2 22.4,18.2 23.5,13",
                strokeColor,
                1.7);

        // Simple body, arm, and paddle strokes remain readable at 25 pixels.
        SVGPath paddlers = strokedPath(
                "M6.8,9.2 C5.8,10.3 5.8,12.2 6.2,14.1 "
                        + "M9.0,9.3 L11.0,11.1 M10.8,9.2 L7.6,18.0 "
                        + "M15.8,9.2 C14.8,10.3 14.8,12.2 15.2,14.1 "
                        + "M18.0,9.3 L20.0,11.1 M19.8,9.2 L16.8,18.0",
                strokeColor,
                1.45);

        Circle bowPaddlerHead = outlinedCircle(7.9, 6.5, 1.8, strokeColor);
        Circle sternPaddlerHead = outlinedCircle(16.9, 6.5, 1.8, strokeColor);

        Group canoe = new Group(hull, paddlers, bowPaddlerHead, sternPaddlerHead);
        double scale = size / 25.0;
        canoe.setScaleX(scale);
        canoe.setScaleY(scale);
        return canoe;
    }

    /** Creates one transparent, rounded vector path for the canoe artwork. */
    private SVGPath strokedPath(String content, Color strokeColor, double strokeWidth) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(strokeColor);
        path.setStrokeWidth(strokeWidth);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    /** Creates an outlined head circle matching the canoe's line-art style. */
    private Circle outlinedCircle(double centerX, double centerY, double radius, Color strokeColor) {
        Circle circle = new Circle(centerX, centerY, radius, Color.TRANSPARENT);
        circle.setStroke(strokeColor);
        circle.setStrokeWidth(1.45);
        return circle;
    }

    /**
     * Factory method to get a toolbar button (upload download, wrench, close button...)
     */
    public static IconButton getToolbarButton(IconGlyphType iconGlyphName, Consumer<MouseEvent> onClickFunction) {

        // Double underscore in the icon glyph name creates a badge with the icon glyph name of the chars before the double underscore
        IconGlyphType badgeGlyph = null;
        String glyphName = iconGlyphName.getGlyphName();
        int splitIndex;
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
        FontAwesomeIcon badgeIcon = createFontAwesomeIcon(badgeGlyphType, this.iconSize / 2);
        badgeIcon.setGlyphName(badgeGlyphType.getGlyphName());
        badgeIcon.setFill(ColorPaletteService.getColor("white"));
        if (cssClasses != null && !cssClasses.isEmpty()) badgeIcon.getStyleClass().addAll(cssClasses);

        // Position the badge icon (top-left corner) and group it with the main icon
        badgeIcon.setTranslateX(-this.iconSize / 3);
        badgeIcon.setTranslateY(-this.iconSize / 3);
        this.setGraphic(new StackPane(this.icon, badgeIcon));
    }
}
