package com.wecca.canoeanalysis.controllers;

import com.wecca.canoeanalysis.services.color.ColorManagerService;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    // TODO: settings state management
    // Note: colors managed through ColorPaletteService
    private boolean isDisplayImperialUnitsEnabled;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setPrimaryColor(ActionEvent actionEvent) throws IOException, URISyntaxException {
        ColorManagerService.putColorPalette("primary", getButtonBackgroundAsColorHexString(((Button) actionEvent.getSource())));
    }

    public static String getButtonBackgroundAsColorHexString(Button button)
    {
        String style = button.getStyle();
        String[] styleProperties = style.split(";");
        for (String property : styleProperties)
        {
            if (property.trim().startsWith("-fx-background-color"))
                return property.split(":")[1].trim();
        }
        throw new RuntimeException("Button -fx-background-color property not found");
    }
}
