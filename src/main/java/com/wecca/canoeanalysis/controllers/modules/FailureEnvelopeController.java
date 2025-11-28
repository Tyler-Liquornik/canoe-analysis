package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
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
            canoeThickness, maxShear, maxCompression, maxTension, maxShearStress, maxTensile;

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
    public void calculateValues() {
        if (maxMoment.getText().isEmpty() || compressionField.getText().isEmpty() ||
                tensionField.getText().isEmpty() || momentOfInertia.getText().isEmpty() ||
                qMax.getText().isEmpty() || maxShear.getText().isEmpty() ||
                canoeThickness.getText().isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MISSING INPUT");
            alert.setContentText("Text field missing input please fill in with double");
            alert.showAndWait();
            return;
        }
        double Mmax = Double.parseDouble(maxMoment.getText());
        double Yc = Double.parseDouble(compressionField.getText());
        double Yt = Double.parseDouble(tensionField.getText());
        double I = Double.parseDouble(momentOfInertia.getText());
        double Vmax = Double.parseDouble(maxShear.getText());
        double Qmax = Double.parseDouble(qMax.getText());
        double t = Double.parseDouble(canoeThickness.getText());

        double sigmaCMax = (Mmax * Yc) / I;
        double sigmaTMax = (Mmax * Yt) / I;
        double tauMax = (Vmax * Qmax) / (I * t);

        maxCompression.setText(String.valueOf(sigmaCMax * (10e-7)));
        maxTension.setText(String.valueOf(sigmaTMax * (10e-7)));
        maxShearStress.setText(String.valueOf(tauMax * (10e-7)));

    }


    /**
     * This method triggers when generate Diagrams button is clicked.
     * It will use the values to generate the two required diagrams and display
     * them in their respected sections.
     */
    public void generateDiagrams() {
        if (maxTension.getText().isEmpty() || compressionField.getText().isEmpty() ||
                tensionField.getText().isEmpty() || maxCompression.getText().isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MISSING INPUT");
            alert.setContentText("Text field missing input please fill in with double");
            alert.showAndWait();
            return;
        }
        generateShearStressVSCompressiveAndTensileStrength();
        generateShearStressVSNormalStress();
    }

    /**
     * This method generates and displays the shear stress vs compressive and tensile strength graph
     * <p>
     * Mohrs circle equation cheat sheet
     * <p>
     * Circle 1
     * x shift comes from half compressive strength (Yc)
     * radius is also have compressive strength (Yc)
     * <p>
     * Circle 2
     * x shift is half tensile strength (tmax)
     * radius is half tensile strength (tmax)
     * <p>
     * Circle 3
     * x shift is difference between sigma1 and sigma3 divided by 2
     * radius is sigma1 and sigma3 added together the divided by 2
     */
    public void generateShearStressVSCompressiveAndTensileStrength() {
        /*
         *  Here is example code on how to use diagram service to plot a circle and attach it to the view
         */
        List<Point2D> circlePoints = new ArrayList<>();

        double sigma1 = (Double.parseDouble(maxTension.getText()));
        double sigma3 = (Double.parseDouble(maxCompression.getText()));
        ;
        int numPoints = 200; // number of sample points for smoothness

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = ((sigma1 + sigma3) / 2) * Math.cos(angle) - Math.abs((sigma1 - sigma3) / 2);
            double y = ((sigma1 + sigma3) / 2) * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // Create chart
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "MPa", "Normal Stress"
        );
        chart.getXAxis().setLabel("Normal Stress (MPa)");
        chart.getYAxis().setLabel("Shear Stress (MPa)");

        // Anchor chart to fill pane
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);


        WindowManagerService.openDiagramWindow("Shear Stress vs Normal Stress",
                null,
                circlePoints,
                "(MPa)",
                "ShearStress");
        // Apply stylesheet
        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        // Add chart to container
        chart1AnchorPane.getChildren().add(chart);
    }

    /**
     * This method generates and displays the shear stress vs normal stress graph
     * with two Mohr circles (tension and compression) and the common upper tangent
     * from the left circle to the right circle.
     */
    public void generateShearStressVSNormalStress() {

        // List of points for both circles (used by DiagramService + window)
        List<Point2D> circlePoints = new ArrayList<>();
        List<Point2D> tangentPoints = new ArrayList<>();  // for separate window

        // Inputs
        double Yc      = Double.parseDouble(compressionField.getText());   // compressive strength
        double maxTens = Double.parseDouble(maxTensile.getText());         // tensile strength
        double maxComp = Double.parseDouble(maxCompression.getText());     // still available if needed

        int numPoints = 200; // number of sample points per circle for smoothness

        // ----- Compression circle (you currently draw it on the LEFT) -----
        for (int i = 0; i < numPoints; i++) {
            double angle  = 2.0 * Math.PI * i / numPoints;
            double radius = Math.abs(Yc) / 2.0;
            double center = -Math.abs(Yc) / 2.0;      // left
            double x = radius * Math.cos(angle) + center;
            double y = radius * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // ----- Tension circle (you currently draw it on the RIGHT) -----
        for (int i = 0; i < numPoints; i++) {
            double angle  = 2.0 * Math.PI * i / numPoints;
            double radius = Math.abs(maxTens) / 2.0;
            double center =  Math.abs(maxTens) / 2.0; // right
            double x = radius * Math.cos(angle) + center;
            double y = radius * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // ----- Create base chart from circle points -----
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "MPa", "Normal Stress"
        );
        chart.getXAxis().setLabel("Normal Stress (MPa)");
        chart.getYAxis().setLabel("Shear Stress (MPa)");

        // ----- Compute and add upper external tangent between the two circles -----
        // First, match the *actual* geometry you used when drawing the circles.
        double cComp = -Math.abs(Yc)      / 2.0;   // compression circle center (as drawn)
        double rComp =  Math.abs(Yc)      / 2.0;
        double cTens =  Math.abs(maxTens) / 2.0;   // tension circle center (as drawn)
        double rTens =  Math.abs(maxTens) / 2.0;

        // Work out which circle is actually left/right on the x-axis
        double cLeft, rLeft, cRight, rRight;
        if (cComp < cTens) {
            cLeft = cComp;  rLeft = rComp;
            cRight = cTens; rRight = rTens;
        } else {
            cLeft = cTens;  rLeft = rTens;
            cRight = cComp; rRight = rComp;
        }

        double d      = cRight - cLeft;           // center-to-center distance (> 0)
        double deltaR = rRight - rLeft;           // radius difference
        double denom  = d * d - deltaR * deltaR;  // must be > 0 for a real external tangent

        if (denom > 0.0) {
            // Slope of one external tangent (upper one when used with b below)
            double m = deltaR / Math.sqrt(denom);

            // sqrt(1 + m^2) used in distance formula from center to line
            double s = Math.sqrt(1.0 + m * m);

            // For the upper tangent:
            // (m * cLeft + b) / sqrt(1 + m^2) = rLeft  -->  b = rLeft * s - m * cLeft
            double b = rLeft * s - m * cLeft;

            // ---- print slope and intercept to console ----
            System.out.println("Mohr envelope tangent: slope m = " + m + ", intercept b = " + b);

            XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
            tangentSeries.setName("Tangent");

            // Choose an x-range that comfortably spans both circles
            double xMin = cLeft  - 1.1 * rLeft;
            double xMax = cRight + 1.1 * rRight;
            int numLinePoints = 80;

            for (int i = 0; i <= numLinePoints; i++) {
                double x = xMin + (xMax - xMin) * i / (double) numLinePoints;
                double y = m * x + b;

                tangentSeries.getData().add(new XYChart.Data<>(x, y));
                tangentPoints.add(new Point2D(x, y));   // store for separate window
            }

            // Add the tangent series to the chart
            chart.getData().add(tangentSeries);
        }

        // ----- Layout and window setup -----
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        // Open separate diagram window with circles + tangent
        WindowManagerService.openDiagramWindowWithTangent(
                "Mohr’s Circle Diagram",
                null,
                circlePoints,
                tangentPoints,
                "MPa",
                "Shear Stress vs Normal Stress"
        );

        // Apply stylesheet
        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        // Add chart to the UI container
        chart2AnchorPane.getChildren().add(chart);
    }



}



