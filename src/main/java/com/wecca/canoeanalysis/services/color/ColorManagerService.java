package com.wecca.canoeanalysis.services.color;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.Graphic;
import com.wecca.canoeanalysis.utils.ColorUtils;
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
import java.util.stream.Stream;

/**
 * This service will dynamically update styles.css and ColorPalette colors and propagate all color changes.
 * This is all based off of transformations defined in ColorTransformationService where instructions on usage are
 * Functionality of this service is enabled by the reflection API
 */
public class ColorManagerService {

    // Scenes / Parents to have CSS colors reloaded on color change must be added here
    private static final HashMap<Object, String> stylesheetMap = new HashMap<>();

    // Graphics to have ColorPalette colors reloaded on color change must be added here
    private static final HashSet<Graphic> graphics = new HashSet<>();

    /**
     * Adds an entry to the stylesheet mapping.
     *
     * @param object the object (Parent or Scene) to which the stylesheet will be applied
     * @param stylesheetPath the path to the stylesheet
     */
    public static void registerForRecoloringFromStylesheet(Object object, String stylesheetPath) {
        if (object instanceof Scene || object instanceof Parent)
            stylesheetMap.put(object, stylesheetPath);
        else
            throw new RuntimeException("addEntryToStyleSheetMapping method only takes objects of type 'Scene' or 'Parent'");
    }

    public static void registerForRecoloringFromColorPalette(Graphic graphic) {
        graphics.add(graphic);
    }


    /**
     * Reloads the stylesheets for all registered objects.
     * Anytime a Parent or Scene object loads CSS it needs to add to the styleSheetMap to be used here.
     */
    private static void reloadStyleSheetsColors() {
        for (Map.Entry<Object, String> entry : stylesheetMap.entrySet()) {
            Object object = entry.getKey();
            String stylesheetPath = entry.getValue();
            if (object instanceof Parent parent) {
                parent.getStylesheets().clear();
                parent.getStylesheets().add(CanoeAnalysisApplication.class.getResource(stylesheetPath).toExternalForm());
            } else if (object instanceof Scene scene) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(CanoeAnalysisApplication.class.getResource(stylesheetPath).toExternalForm());
            }
        }
    }

    private static void reloadGraphicsColors() {
        for (Graphic graphic : graphics)
        {
            if (graphic.isColored())
                graphic.recolor(true);
        }
    }


    /**
     * Derives colors based on a specified base color and updates the CSS file and ColorPalette map.
     *
     * @param baseName the base color name (e.g., "primary")
     * @param baseColorHex the base color hex value (e.g., "#FF0000")
     */
    public static void putColorPalette(String baseName, String baseColorHex) throws IOException, URISyntaxException {
        // Build a map of CSS color variables to values
        Color baseColor = Color.web(baseColorHex);
        HashMap<String, String> colorsMap = deriveColorVariants(baseName, baseColor);
        colorsMap.put(String.format("-fx-%s", baseName), baseColorHex);

        // Read the CSS file
        Path path = Paths.get(CanoeAnalysisApplication.class.getResource("css/style.css").toURI());
        List<String> lines = Files.readAllLines(path);

        // Create a map to keep track of whether a property was replaced
        Map<String, Boolean> propertyReplaced = new HashMap<>();
        colorsMap.forEach((key, value) -> propertyReplaced.put(key, false));

        // Replace properties if they exist
        List<String> updatedLines = lines.stream()
                .map(line -> {
                    for (Map.Entry<String, String> entry : colorsMap.entrySet()) {
                        String cssVariable = entry.getKey();
                        String colorValue = entry.getValue();
                        if (line.contains(cssVariable + ":")) {
                            propertyReplaced.put(cssVariable, true);
                            return line.replaceAll(cssVariable + ":.*?;", cssVariable + ": " + colorValue + ";");
                        }
                    }
                    return line;
                })
                .toList();

        // Insert any properties that were not replaced from the base marker
        List<String> finalLines = updatedLines.stream()
                .flatMap(line -> {
                    if (line.contains(String.format("-fx-%s:", baseName))) {
                        List<String> missingProperties = colorsMap.entrySet().stream()
                                .filter(entry -> !propertyReplaced.get(entry.getKey()))
                                .map(entry -> String.format("    %s: %s;", entry.getKey(), entry.getValue()))
                                .toList();
                        return Stream.concat(Stream.of(line), missingProperties.stream());
                    }
                    return Stream.of(line);
                })
                .collect(Collectors.toList());

        // Write the results to the stylesheet and reload them
        Files.write(path, finalLines, StandardOpenOption.TRUNCATE_EXISTING);
        reloadStyleSheetsColors();

        // Refresh ColorPaletteService and reload graphics
        colorsMap.forEach((cssVariable, colorValue) -> {
            String constantName = cssVariable.substring(4);
            ColorPaletteService.putColor(constantName, colorValue);
        });
        reloadGraphicsColors();
    }

    /**
     * Derives color variants for a given base color using transformation methods from ColorTransformationService.
     *
     * @param baseName  the base color name (e.g., "primary")
     * @param baseColor the base color object
     * @return a map of CSS variable names to hex color values
     */
    private static HashMap<String, String> deriveColorVariants(String baseName, Color baseColor) {
        HashMap<String, String> colorVariants = new HashMap<>();

        try {
            Method[] methods = ColorTransformationService.class.getDeclaredMethods();

            for (Method method : methods) {
                if (method.getName().startsWith(baseName + "_")) {
                    String transformationName = method.getName().substring(baseName.length() + 1); // Remove "base_" prefix
                    String cssVariableName = String.format("-fx-%s-%s", baseName, transformationName);
                    Color transformedColor = (Color) method.invoke(null, baseColor);
                    String colorHex = ColorUtils.colorToHexString(transformedColor);
                    colorVariants.put(cssVariableName, colorHex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return colorVariants;
    }
}
