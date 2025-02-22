package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.fxml.Initializable;
import lombok.Setter;
import java.net.URL;
import java.util.ResourceBundle;

public class FailureEnvelopeController implements Initializable, ModuleController {

    @Setter
    private static MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        mainController.resetToolBarButtons();
    }
}
