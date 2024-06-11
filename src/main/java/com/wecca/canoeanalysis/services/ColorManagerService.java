package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.ColorPalette;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This service will dynamically update styles.css and ColorPalette colors and propagate all color changes
 * It will also define color deriving methods because JavaFX CSS has bad / barely any options
 * This will allow a flexible approach to allow CSS changes to persist
 */
public class ColorManagerService
{
    // Css loading information for CssManagerService
    private static final HashMap<Object, String> stylesheetMap = new HashMap<>();

    /**
     * Adds an entry to the stylesheet mapping.
     *
     * @param object the object (Parent or Scene) to which the stylesheet will be applied
     * @param stylesheetPath the path to the stylesheet
     */
    public static void addEntryToStylesheetMapping(Object object, String stylesheetPath) {
        if (object instanceof Scene || object instanceof Parent)
            stylesheetMap.put(object, stylesheetPath);
        else
            throw new RuntimeException("addEntryToStyleSheetMapping method only takes objects of type 'Scene' or 'Parent'");
    }

    /**
     * Updates the primary color and associated derived values in the CSS file.
     * Derived values in the palette are '-fx-primary-light' and '-fx-primary-desaturated' at the moment
     * @param newPrimaryColor the new primary color to be set in the CSS file
     * @throws IOException if an I/O error occurs
     */
    public static void setPrimaryColor(String newPrimaryColor) throws IOException, URISyntaxException
    {
        // Build a map of css color variables to values
        Color primaryColor = Color.web(newPrimaryColor);
        HashMap<String, String> colorsMap = derivePrimaryColorVariants(primaryColor);
        colorsMap.put("-fx-primary", newPrimaryColor);

        // Dynamically edit each variable in the map to the specified value
        Path path = Paths.get(CanoeAnalysisApplication.class.getResource("css/style.css").toURI());
        List<String> lines = Files.readAllLines(path);
        List<String> updatedLines = lines.stream()
                .map(line -> {
                    for (Map.Entry<String, String> entry : colorsMap.entrySet()) {
                        String cssVariable = entry.getKey();
                        String colorValue = entry.getValue();
                        if (line.contains(cssVariable + ":")) {
                            return line.replaceAll(cssVariable + ":.*?;", cssVariable + ": " + colorValue + ";");
                        }
                    }
                    return line;
                })
                .collect(Collectors.toList());

        // Write the results to the stylesheet
        Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);

        // Reload the stylesheets for all components added to the styleSheetMap
        reloadStyleSheets();

        // Set colors in ColorPalette
        reloadColorPalette(colorsMap);
    }

    /**
     * Reloads the stylesheets for all registered objects.
     * Anytime a Parent or Scene object loads css it needs to add to the styleSheetMap to be used here
     */
    private static void reloadStyleSheets()
    {
        for (Map.Entry<Object, String> entry : stylesheetMap.entrySet()) {
            Object object = entry.getKey();
            String stylesheetPath = entry.getValue();
            if (object instanceof Parent parent) {
                parent.getStylesheets().clear();
                parent.getStylesheets().add(CanoeAnalysisApplication.class.getResource(stylesheetPath).toExternalForm());
            }
            else if (object instanceof Scene scene) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(CanoeAnalysisApplication.class.getResource(stylesheetPath).toExternalForm());
            }
        }
    }

    /**
     * Calls setters to set each color value in ColorPalette
     * @param colorPaletteAsMap contains <JavaFX CSS variable name : color hex string value></JavaFX>
     */
    private static void reloadColorPalette(HashMap<String, String> colorPaletteAsMap)
    {
        for (Map.Entry<String, String> entry : colorPaletteAsMap.entrySet()) {
            callSetter(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets
     * @param cssVariable The JavaFX CSS color variable name
     * @param colorValue The color variable value as a string hex (ex. #FFFFFF for white)
     */
    private static void callSetter(String cssVariable, String colorValue) {
        // Convert CSS variable to setter method name
        String methodName = "set" + convertCssToMethodName(cssVariable);

        try {
            ColorPalette instance = ColorPalette.getInstance();
            Method setter = instance.getClass().getMethod(methodName, Color.class);
            Color color = Color.web(colorValue);
            setter.invoke(instance, color);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets setter method names in ColorPalette from the JavaFX CSS color variable name
     * @param cssVariable the JavaFX CSS color variable
     * @return the name of the setter to be invoked from ColorPalette
     */
    private static String convertCssToMethodName(String cssVariable) {
        // Remove the '-fx-' prefix
        String baseName = cssVariable.substring(4);

        // Convert to PascalCase
        String[] parts = baseName.split("-");
        StringBuilder methodName = new StringBuilder();
        for (String part : parts) {
            methodName.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }

        return methodName.toString();
    }

    /**
     * IMPORTANT: included derived values from primary are hardcoded here and need to be added as needed
     * @param primaryColor passed in as a reference to derive variants from
     * @return a hash map of <JavaFX CSS variable name : Hex color></JavaFX>
     */
    private static HashMap<String, String> derivePrimaryColorVariants(Color primaryColor) {
        HashMap<String, String> colorVariants = new HashMap<>();

        // Calculate the light variant by increasing the brightness
        Color primaryLight = ColorTransformer.transform(primaryColor, ColorTransformationService::lighten);
        String primaryLightHex = colorToHexString(primaryLight);
        colorVariants.put("-fx-primary-light", primaryLightHex);

        // Calculate the desaturated variant by decreasing the saturation
        Color primaryDesaturated = ColorTransformer.transform(primaryColor, ColorTransformationService::darkenAndDesaturate);
        String primaryDesaturatedHex = colorToHexString(primaryDesaturated);
        colorVariants.put("-fx-primary-desaturated", primaryDesaturatedHex);

        return colorVariants;
    }

    /**
     * Convert a color object with RGB values to a hex representation as a string
     * @param color the color object to convert
     * @return the color in hex format as a string
     */
    private static String colorToHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
