package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import lombok.Setter;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class FailureEnvelopeController implements Initializable, ModuleController {

    @FXML
    private JFXTextField compressionField, tensionField, qMax, momentOfInertia, maxMoment,
            canoeThickness, maxShear, maxCompression, maxTension, maxShearStress;

    @FXML
    private AnchorPane chart2AnchorPane;

    @FXML
    private AnchorPane chart1AnchorPane;

    @Setter
    private static MainController mainController;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();
    }

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     * Currently, this provides only a button to open glossary, no upload or download yet
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        //iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> downloadCanoe());
        //iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> uploadCanoe());
        iconGlyphToFunctionMap.put(IconGlyphType.BOOK, e -> openGlossary());

        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }
    /**
     * Open the glossary window
     */
    public void openGlossary() {
        /*
         * Here is an example of how we did it in punching shear
         */
        //WindowManagerService.openUtilityWindow("Glossary", "/com/wecca/canoeanalysis/view/shear-equations-view.fxml", 800, 550);
    }

    /**
     * This method triggers when calculate value button clicked. It uses Input values to
     * calculate max values, and fill their corresponding textboxs
     */
    public void calculateValues(){

    }
    /**
     * This method triggers when generate Diagrams button is clicked.
     * It will use the values to generate the two required diagrams and display
     * them in their respected sections.
     */
    public void generateDiagrams(){
        generateShearStressVSCompressiveAndTensileStrength();
        generateShearStressVSNormalStress();
    }

    /**
     * This method generates and displays the shear stress vs compressive and tensile strength graph
     *
     */
    public void generateShearStressVSCompressiveAndTensileStrength(){
        /*
         *  Here is example code on how to use diagram service to plot a cicle and attach it to the view
         */
        List<Point2D> circlePoints = new ArrayList<>();
        double radius = 50; // change radius as needed
        int numPoints = 200; // number of sample points for smoothness

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }


        // Create chart
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "m", "Y"
        );
        // Anchor chart to fill pane
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        // Apply stylesheet
        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        // Add chart to container
        chart1AnchorPane.getChildren().add(chart);
    }

    /**
     * This method generates and displays the shear stress vs normal stress graph
     */
    public void generateShearStressVSNormalStress(){

    }
}
