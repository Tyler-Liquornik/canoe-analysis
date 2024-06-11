package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.ColorPalette;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This service will dynamically update PADDL CSS file colors
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
     * Updates the primary color in the CSS file.
     *
     * @param newPrimaryColor the new primary color to be set in the CSS file
     * @throws IOException if an I/O error occurs
     */
    public static void setPrimaryColor(String newPrimaryColor) throws IOException, URISyntaxException {
        ColorPalette.getInstance().setPrimary(Color.web(newPrimaryColor));

        Path path = Paths.get(CanoeAnalysisApplication.class.getResource("css/style.css").toURI());
        List<String> lines = Files.readAllLines(path);
        List<String> updatedLines = lines.stream()
                .map(line -> {
                    if (line.contains("-fx-primary:")) {
                        return line.replaceAll("-fx-primary:.*?;", "-fx-primary: " + newPrimaryColor + ";");
                    }
                    return line;
                })
                .collect(Collectors.toList());

        Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
        reloadStyleSheets();
    }

    /**
     * Reloads the stylesheets for all registered objects.
     * Anytime a Parent or Scene object loads css it needs to add to the styleSheetMap to be used here
     */
    public static void reloadStyleSheets() {
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
}
