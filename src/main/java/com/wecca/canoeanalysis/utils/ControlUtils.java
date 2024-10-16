package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.util.function.Consumer;

public class ControlUtils {
    /**
     * Put a group of radio buttons into a toggle group (only allow one to be selected at a time)
     * Have one of the buttons be selected by default
     * @param group for the buttons to be added to
     * @param buttons the radio buttons to add to the group
     * @param selectedIndex the index in buttons to be selected on initialization
     */
    public static void addAllRadioButtonsToToggleGroup(ToggleGroup group, RadioButton[] buttons, int selectedIndex) {
        for (RadioButton b : buttons) {
            b.setToggleGroup(group);
        }
        buttons[selectedIndex].setSelected(true);
    }

    /**
     * Populate a combo box and set a default item to show
     * @param comboBox the combo box to populate
     * @param options the list of options to populate the combo box with
     * @param selectedIndex the index in the list of options to display on initialization
     */
    public static void initComboBoxesWithDefaultSelected(ComboBox<String> comboBox, String[] options, int selectedIndex)
    {
        comboBox.setItems(FXCollections.observableArrayList(options));
        comboBox.getSelectionModel().select(selectedIndex);
    }

    /**
     * Creates a Button with a Font Awesome icon and registers an event handler for a specified event type.
     * The button's appearance can be customized to become transparent on hover, and an icon of a given size
     * is displayed on it.
     *
     * @param <T> The type of the event to handle
     * @param iconGlyphName represents the icon to be displayed on the button.
     * @param eventType The type of the event to listen for
     * @param onEvent The action to perform when the specified event is triggered.
     * @param iconSize The size of the icon in pixels.
     * @param transparentOnHover Whether the button should become transparent when hovered.
     * @return A button with the specified icon and event handler.
     */
    public static <T extends Event> Button getIconButton(IconGlyphType iconGlyphName, EventType<T> eventType, Consumer<? super T> onEvent, double iconSize, boolean transparentOnHover) {
        Button button = new Button();
        button.getStyleClass().add("transparent-button");
        if (transparentOnHover)
            button.getStyleClass().add("transparent-button-no-hover");
        button.addEventHandler(eventType, onEvent::accept);
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setFill(ColorPaletteService.getColor("white"));
        icon.setGlyphName(iconGlyphName.getGlyphName());
        icon.setSize(String.valueOf(iconSize));
        button.setGraphic(icon);
        return button;
    }
}