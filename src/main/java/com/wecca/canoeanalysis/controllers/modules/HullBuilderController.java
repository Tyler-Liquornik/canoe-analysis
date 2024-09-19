package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import lombok.Setter;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Traceable
public class HullBuilderController implements Initializable {

    @Setter
    private static MainController mainController;

    @FXML
    private AnchorPane canoeViewAnchorPane, curveKnobsAnchorPane, sectionPropertiesAnchorPane;

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     * Currently, this provides buttons to download and upload the Canoe object as JSON
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<ActionEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        // TODO: add functions to buttons
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.SCISSORS, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.CHAIN, e -> dummy());
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    public void dummy() {}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());
        initModuleToolBarButtons();

        // Create the knob and configure it
        Knob knob = new Knob("x", 0, 0, 10, 50, 50);

        // Add the knob to the container
        curveKnobsAnchorPane.getChildren().add(knob);
    }
}
