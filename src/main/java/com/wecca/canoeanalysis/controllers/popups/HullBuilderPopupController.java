package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.BeamController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.services.LoadTreeManagerService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * For now, this will just be a smaller window that will notify the user about the beta status of this submodule
 * It will just set the hull geometry to a scaled version of 2024's SharkBait based on the provided hull length
 * Later, this will be implemented to allow the user to specify curves for hull geometry and material properties
 */
public class HullBuilderPopupController implements Initializable {

    @Setter
    private static MainController mainController;
    @Setter
    private static BeamController beamController;
    @Setter
    private static Canoe canoe;

    public void closeWindow(ActionEvent e) {
        WindowManagerService.closeWindow(((Stage) ((Button) e.getSource()).getScene().getWindow()));
    }

    public void setHullScaledSharkBait(ActionEvent e) {
        // Hull hull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromParabolasC1Smooth(canoe.getHull().getLength());
        // Hull hull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromParabolasC0Smooth(canoe.getHull().getLength());
        Hull hull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromBezier(canoe.getHull().getLength());
        canoe.setHull(hull);
        beamController.setCanoe(canoe);
        mainController.showSnackbar("Successfully set hull to Shark Bait");
        beamController.checkAndSetEmptyLoadTreeSettings();
        LoadTreeManagerService.buildLoadTreeView(canoe);
        mainController.disableModuleToolBarButton(true, 2);
        closeWindow(e);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        canoe = beamController.getCanoe();
    }
}
