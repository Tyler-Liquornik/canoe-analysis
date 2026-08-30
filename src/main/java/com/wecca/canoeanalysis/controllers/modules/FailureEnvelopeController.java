package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXButton;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

public class FailureEnvelopeController implements Initializable, ModuleController {

    @Setter
    private static MainController mainController;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();


    }


}
