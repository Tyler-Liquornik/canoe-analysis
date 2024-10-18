package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.function.Consumer;

/**
 * A clickable button whose central feature is an icon
 */
@Getter
@Setter
public class IconButton extends Button {

    private FontAwesomeIcon icon;

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
                       Consumer<MouseEvent> onClickFunction,
                       Consumer<MouseEvent> onReleaseFunction,
                       List<String> cssClasses, double iconSize) {
        FontAwesomeIcon icon = createIcon(iconGlyphName, iconSize);
        this.icon = icon;
        this.setGraphic(icon);
        this.setOnMouseClicked(onClickFunction::accept);
        if (onReleaseFunction != null) {
            this.setOnMouseClicked(null);
            this.setOnMousePressed(onClickFunction::accept);
            this.setOnMouseReleased(onReleaseFunction::accept);
        }
        if (cssClasses != null && !cssClasses.isEmpty())
            this.getStyleClass().addAll(cssClasses);
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
        IconButton button = new IconButton(iconGlyphName, onClickFunction, null,
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
        return new IconButton(iconGlyphType, onPressFunction, onReleaseFunction,
                List.of("transparent-until-hover-button", "transparent-on-hover-button"), iconSize);
    }

    /**
     * Factory method for the panel buttons (plus, switch panel) in hull builder
     */
    public static IconButton getPanelButton(IconGlyphType iconGlyphName, Consumer<MouseEvent> onClickFunction, double iconSize) {
        IconButton button = new IconButton(iconGlyphName, onClickFunction, null,
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
}
