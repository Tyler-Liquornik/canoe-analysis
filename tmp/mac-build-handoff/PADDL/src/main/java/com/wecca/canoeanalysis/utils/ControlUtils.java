package com.wecca.canoeanalysis.utils;

import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

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
    public static void initComboBoxesWithDefaultSelected(ComboBox<String> comboBox, String[] options, int selectedIndex) {
        comboBox.setItems(FXCollections.observableArrayList(options));
        comboBox.getSelectionModel().select(selectedIndex);
    }

    /**
     * Highlights or removes the invalid input style on the textField based on the value range.
     * Uses an exclusive lower bound if exclusiveMin is true.
     * @param comboBox the unit selection for conversion
     * @param textField the text field with user input
     * @param min the minimum valid value
     * @param max the maximum valid value
     * @param isDistanceElseLoad if true, use distance conversion; if false, use load conversion.
     * @param exclusiveMin if true, the value must be strictly greater than min
     */
    public static void validateNumericInput(ComboBox<String> comboBox, JFXTextField textField, double min, double max, boolean isDistanceElseLoad, boolean exclusiveMin) {
        if (InputParsingUtils.validateTextAsDouble(textField.getText())) {
            double value = isDistanceElseLoad ? InputParsingUtils.getDistanceConverted(comboBox, textField)
                    : InputParsingUtils.getLoadConverted(comboBox, textField);
            boolean valid = exclusiveMin ? (value > min && value <= max) : (value >= min && value <= max);
            if (!valid && !textField.getStyleClass().contains("invalid-input")) {
                textField.getStyleClass().add("invalid-input");
                slideFocusAnimationForInvalidInput(textField);
            } else if (valid && textField.getStyleClass().contains("invalid-input")) {
                textField.getStyleClass().removeAll("invalid-input");
                slideFocusAnimationForInvalidInput(textField);
            }
        } else if (!textField.getStyleClass().contains("invalid-input")) {
            textField.getStyleClass().add("invalid-input");
            slideFocusAnimationForInvalidInput(textField);
        }
    }

    /**
     * Validates the numeric interval fields (leftField and rightField) together.
     * If both fields contain valid numbers, it converts them.
     * If the right value is less than or equal to the left value, both fields are marked invalid.
     * Otherwise, the method validates the right field using leftValue as the exclusive lower bound
     * (and the left field using rightValue as the exclusive upper bound) and removes any error styling.
     * @param comboBox the unit selection for conversion
     * @param leftField the text field containing the left bound value
     * @param rightField the text field containing the right bound value
     * @param globalMin the global minimum allowed value for the left bound
     * @param globalMax the global maximum allowed value for the right bound
     * @param isDistanceElseLoad if true, the input is treated as a distance; if false, as a load.
     */
    public static void validateSectionFields(ComboBox<String> comboBox, JFXTextField leftField, JFXTextField rightField, double globalMin, double globalMax, boolean isDistanceElseLoad) {
        boolean leftValid = InputParsingUtils.validateTextAsDouble(leftField.getText());
        boolean rightValid = InputParsingUtils.validateTextAsDouble(rightField.getText());
        if (leftValid && rightValid) {
            double leftValue = InputParsingUtils.getDistanceConverted(comboBox, leftField);
            double rightValue = InputParsingUtils.getDistanceConverted(comboBox, rightField);
            if (rightValue <= leftValue) {
                if (!leftField.getStyleClass().contains("invalid-input")) leftField.getStyleClass().add("invalid-input");
                if (!rightField.getStyleClass().contains("invalid-input")) rightField.getStyleClass().add("invalid-input");
            } else {
                leftField.getStyleClass().removeAll("invalid-input");
                rightField.getStyleClass().removeAll("invalid-input");
                // Validate rightField with leftValue as bounds
                validateNumericInput(comboBox, rightField, leftValue, globalMax, isDistanceElseLoad, true);
                validateNumericInput(comboBox, leftField, globalMin, rightValue, isDistanceElseLoad, false);
            }
        // If one field is not valid on its own, flag the right field as invalid.
        } else rightField.getStyleClass().add("invalid-input");
    }

    /**
     * Triggers a slide animation on the textField when input is invalid.
     * Accomplished by temporarily moving the focus away and then back to trigger the animation.
     * @param textField the text field to animate
     */
    private static void slideFocusAnimationForInvalidInput(JFXTextField textField) {
        // Store the current focused node
        Node focusedNode = textField.getScene().getFocusOwner();
        if (focusedNode != textField) return;

        // Trigger the slide animation
        textField.getParent().requestFocus(); // Temporarily move focus away
        Platform.runLater(() -> {
            // Restore focus to trigger the animation
            textField.requestFocus();
            // Set the caret position to the end of the text
            textField.positionCaret(textField.getText().length());
        });
    }

    /**
     * Uses a regex scan text field text to allow digits before an optional decimal point and up to numDecimals digits after.
     * @param textField the text field to restrict
     * @param numDecimals the number of decimal places to restrict up to
     */
    public static void restrictDecimalTypingPrecision(JFXTextField textField, int numDecimals) {
        String regex = String.format("^\\d*\\.?\\d{0,%d}$", numDecimals);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(regex)) textField.setText(oldValue);
        });
    }
}