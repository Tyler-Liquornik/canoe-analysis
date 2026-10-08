package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.fxml.Initializable;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Placeholder controller for the future STL-to-PADDL import workflow.
 *
 * <p>The module intentionally contains no file parsing or model-generation
 * behavior yet. Its only responsibility is to participate in normal module
 * navigation and clear toolbar actions left by the previously open module.</p>
 */
public class StlLoaderController implements Initializable, ModuleController {

    @Setter
    private static MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();
    }
}
