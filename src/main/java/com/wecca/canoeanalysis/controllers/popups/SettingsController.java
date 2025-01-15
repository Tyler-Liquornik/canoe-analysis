package com.wecca.canoeanalysis.controllers.popups;

import com.jfoenix.controls.JFXToggleButton;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.data.Settings;
import com.wecca.canoeanalysis.services.YamlMarshallingService;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import lombok.Setter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    private JFXToggleButton imperialUnitsToggleButton;
    @Setter
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
    }


    public void setSettings(ActionEvent actionEvent) throws IOException, URISyntaxException {
        String selectedButtonColorHex = getButtonBackgroundAsColorHexString(((Button) actionEvent.getSource()));
        boolean isDisplayImperialUnitsEnabled = imperialUnitsToggleButton.isSelected();
        ColorManagerService.putColorPalette("primary", selectedButtonColorHex);
        YamlMarshallingService.saveSettings(new Settings(selectedButtonColorHex, isDisplayImperialUnitsEnabled));
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
