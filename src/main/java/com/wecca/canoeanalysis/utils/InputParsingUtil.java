package com.wecca.canoeanalysis.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class InputParsingUtil {

    /**
     * Convert the distance in the text field to m from the unit selected in the combo box
     * Assumes the value in the text field already validated as double
     * @param c the combo box with the selected unit to convert from
     * @param t the text field with the value to convert
     * @return the value converted to m
     */
    public static double getDistanceConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        if (Objects.equals(unit, "m")) {return d;}
        else {return d * PhysicalConstants.FEET_TO_METRES.getValue();}
    }

    /**
     * Convert the load in the text field to kN or kN/m from the unit selected in the combo box
     * Assumes the value in the text field already validated as double
     * @param c the combo box with the selected unit to convert from
     * @param t the text field with the value to convert
     * @return the value converted to kN (point load) or kN/m (distributed load)
     */
    public static double getLoadConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        return switch (unit) {
            case "N", "N/m" -> d / 1000.0;
            case "kg", "kg/m" -> (d * PhysicalConstants.GRAVITY.getValue()) / 1000.0;
            case "lb" -> (d * PhysicalConstants.POUNDS_TO_KG.getValue() * PhysicalConstants.GRAVITY.getValue()) / 1000.0;
            case "lb/ft" -> (d * PhysicalConstants.POUNDS_TO_KG.getValue() * PhysicalConstants.GRAVITY.getValue()) / (1000.0 * PhysicalConstants.FEET_TO_METRES.getValue());
            default -> d;
        };
    }

    /**
     * Checks if a string can be parsed as a double.
     *
     * @param s the string to be checked
     * @return true if the string can be parsed as a double, false otherwise
     */
    public static boolean validateTextAsDouble(String s) {
        if (s == null)
            return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validateTextAsPercent(String s) {
        boolean isDouble = validateTextAsDouble(s);
        if (!isDouble)
            return false;
        else {
            double d = Double.parseDouble(s);
            return (d > 0 && d < 100);
        }
    }

    /**
     * Check if a list of text fields all contain double values.
     * @param fields list of text fields to check.
     * @return whether each text field contains a double value.
     */
    public static boolean allTextFieldsAreDouble(List<TextField> fields) {
        for (TextField field : fields) {
            if (!validateTextAsDouble(field.getText())) return false;
        }
        return true;
    }


}
