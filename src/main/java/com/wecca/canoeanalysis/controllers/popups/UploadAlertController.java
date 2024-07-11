package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.controllers.modules.BeamController;
import com.wecca.canoeanalysis.services.YamlMarshallingService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;

import javafx.event.ActionEvent;
import java.net.URL;
import java.util.ResourceBundle;

public class UploadAlertController implements Initializable {

    @Setter
    private static BeamController beamController;

    public void closeWindow(ActionEvent e) {
        WindowManagerService.closeWindow(((Stage) ((Button) e.getSource()).getScene().getWindow()));
    }

    public void continueToCanoeUpload(ActionEvent e) {
        YamlMarshallingService.importCanoeFromYAML(beamController.getMainController().getPrimaryStage());
        closeWindow(e);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}
}