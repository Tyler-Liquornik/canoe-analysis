package com.wecca.canoeanalysis.controllers;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import javafx.fxml.Initializable;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

public class PercentOpenAreaController implements Initializable {

    @Setter
    private static MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        mainController.resetToolBarButtons();
    }
}
