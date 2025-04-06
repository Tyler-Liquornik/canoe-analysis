package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.ModuleSelectorController;
import com.wecca.canoeanalysis.controllers.modules.ModuleSelectorController.Module;
import com.wecca.canoeanalysis.services.MarshallingService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.Setter;
import java.net.URL;
import java.util.ResourceBundle;

public class UploadAlertController implements Initializable {

    @Setter
    private static MainController mainController;

    @FXML
    private Label titleLabel, messageLabel;

    /**
     * Close the popup window
     */
    public void closeWindow(ActionEvent e) {
        Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
        WindowManagerService.closeWindow(stage);
    }

    /**
     * Continues with the upload.
     * For the Hull Builder module, imports a Hull and sets it on the HullBuilderController.
     * For the Beam module, imports a Canoe via MarshallingService.
     * If the selected module is neither, an IllegalStateException is thrown.
     */
    public void continueToUpload(ActionEvent e) {
        Module selectedModule = ModuleSelectorController.selectedModule;
        Stage stage = mainController.getPrimaryStage();
        if (selectedModule == Module.HULL_BUILDER) MarshallingService.hullBuilderImportHullFromYAML(stage);
        else if (selectedModule == Module.BEAM) MarshallingService.beamImportCanoeFromYAML(stage);
        else throw new IllegalStateException("Upload not supported for module: " + selectedModule);
        closeWindow(e);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Module selectedModule = ModuleSelectorController.selectedModule;
        if (selectedModule != Module.HULL_BUILDER && selectedModule != Module.BEAM) throw new IllegalStateException("Upload not supported for module: " + selectedModule);
        String modelType = (selectedModule == Module.HULL_BUILDER) ? "Hull" : "Canoe";
        titleLabel.setText(String.format("Upload %s Model?", modelType));
        messageLabel.setText(String.format("This will override the current %s Model. Ensure you download the current %s Model first if you do not wish to lose it permanently.", modelType, modelType));
    }
}
