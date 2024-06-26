package com.wecca.canoeanalysis.controllers;

import com.wecca.canoeanalysis.models.Canoe;
import com.wecca.canoeanalysis.services.MarshallingService;
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
    public static BeamController beamController;
    @Setter
    public static Canoe uploadedCanoe;

    public void closeWindow(ActionEvent e) {
        WindowManagerService.closeWindow(((Stage) ((Button) e.getSource()).getScene().getWindow()));
    }

    public void continueToCanoeUpload(ActionEvent e) {
        MarshallingService.importCanoeFromYAML(beamController.getMainController().getPrimaryStage());
        closeWindow(e);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}
}