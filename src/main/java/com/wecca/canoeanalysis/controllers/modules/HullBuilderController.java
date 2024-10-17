package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
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
    private AnchorPane canoeViewAnchorPane, curveParameterizationAnchorPane, canoePropertiesAnchorPane;

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        // TODO: add functions to buttons
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.SCISSORS, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.CHAIN, e -> dummy());
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    public void dummy() {}

    public void dummyOnClick(MouseEvent event) {}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());
        initModuleToolBarButtons();

        // Add knobs
        Knob leftKnob = new Knob("a", 0, 0, 100, 45, 55);
        Knob middleKnob = new Knob("h", 0, 0, 100, 215, 55);
        Knob rightKnob = new Knob("k", 0, 0, 100, 385, 55);
        curveParameterizationAnchorPane.getChildren().addAll(leftKnob, middleKnob, rightKnob);

        // Add and position panel buttons
        double marginToPanel = 15;
        double marginBetween = 10;

        IconButton nextLeftSectionButton = IconButton.getPanelButton(IconGlyphType.LEFT, this::dummyOnClick, 12);
        IconButton nextRightSectionButton = IconButton.getPanelButton(IconGlyphType.RIGHT, this::dummyOnClick, 12);
        IconButton curveParameterizationPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);
        IconButton canoePropertiesSwitchButton = IconButton.getPanelButton(IconGlyphType.SWITCH, this::dummyOnClick, 14);
        IconButton canoePropertiesPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);

        nextRightSectionButton.getIcon().setTranslateX(1.5);
        nextLeftSectionButton.getIcon().setTranslateX(-0.5);
        curveParameterizationPlusButton.getIcon().setTranslateY(1);
        canoePropertiesPlusButton.getIcon().setTranslateY(1);

        canoeViewAnchorPane.getChildren().addAll(nextLeftSectionButton, nextRightSectionButton);
        curveParameterizationAnchorPane.getChildren().add(curveParameterizationPlusButton);
        canoePropertiesAnchorPane.getChildren().addAll(canoePropertiesSwitchButton, canoePropertiesPlusButton);

        AnchorPane.setTopAnchor(nextLeftSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextLeftSectionButton, marginToPanel + marginBetween + nextLeftSectionButton.prefWidth(-1));
        AnchorPane.setTopAnchor(nextRightSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextRightSectionButton, marginToPanel);
        AnchorPane.setTopAnchor(curveParameterizationPlusButton, marginToPanel);
        AnchorPane.setRightAnchor(curveParameterizationPlusButton, marginToPanel);
        AnchorPane.setTopAnchor(canoePropertiesSwitchButton, marginToPanel);
        AnchorPane.setRightAnchor(canoePropertiesSwitchButton, marginToPanel);
        AnchorPane.setTopAnchor(canoePropertiesPlusButton, marginToPanel + marginBetween + nextLeftSectionButton.prefHeight(-1));
        AnchorPane.setRightAnchor(canoePropertiesPlusButton, marginToPanel);
    }
}
