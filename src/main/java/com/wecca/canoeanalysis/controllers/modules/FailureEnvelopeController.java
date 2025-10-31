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

        maxCompression.setText(String.valueOf(sigmaCMax));
        maxTension.setText(String.valueOf(sigmaTMax));
        maxShearStress.setText(String.valueOf(tauMax));

    }


    /**
     * This method triggers when generate Diagrams button is clicked.
     * It will use the values to generate the two required diagrams and display
     * them in their respected sections.
     */
    public void generateDiagrams(){
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
     *
     * Mohrs circle equation cheat sheet
     *
     * Circle 1
     * x shift comes from half compressive strength (Yc)
     * radius is also have compressive strength (Yc)
     *
     * Circle 2
     * x shift is half tensile strength (tmax)
     * radius is half tensile strength (tmax)
     *
     * Circle 3
     * x shift is difference between sigma1 and sigma3 divided by 2
     * radius is sigma1 and sigma3 added together the divided by 2
     */
    public void generateShearStressVSCompressiveAndTensileStrength(){
        /*
         *  Here is example code on how to use diagram service to plot a circle and attach it to the view
         */
        List<Point2D> circlePoints = new ArrayList<>();

        double sigma1 = Double.parseDouble(maxTension.getText());
        double sigma3 = Double.parseDouble(maxCompression.getText());;
        int numPoints = 200; // number of sample points for smoothness

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = ((sigma1+sigma3)/2) * Math.cos(angle) - Math.abs((sigma1-sigma3)/2);
            double y = ((sigma1+sigma3)/2) * Math.sin(angle);
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
        List<Point2D> circlePoints = new ArrayList<>();
        double Yc = Double.parseDouble(compressionField.getText());
        double maxTens = Double.parseDouble(maxTension.getText());
        double maxComp = Double.parseDouble(maxCompression.getText());;
        int numPoints = 200; // number of sample points for smoothness

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = ((Yc)/2) * Math.cos(angle) - Math.abs((Yc)/2);
            double y = ((Yc)/2) * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }


        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = ((maxTens)/2) * Math.cos(angle) + Math.abs((maxTens)/2);
            double y = ((maxTens)/2) * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // Create chart
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "MPa", "Normal Stress"
        );
        chart.getXAxis().setLabel("Normal Stress (MPa)");
        chart.getYAxis().setLabel("Shear Stress (MPa)");

        double x1 = -Math.abs(Yc) / 2.0;
        double r1 = Math.abs(Yc) / 2.0;
        double x2 = Math.abs(maxTens) / 2.0;
        double r2 = Math.abs(maxTens) / 2.0;
        double d = x2 - x1;

        if (d > Math.abs(r2 - r1)) {
            // slope of external tangent
            double m = (r2 - r1) / Math.sqrt(d * d - (r2 - r1) * (r2 - r1));
            // pick the upper tangent (positive above x-axis)
            double s = Math.sqrt(m * m + 1);
            double b = r1 * s - m * x1;

            // --- draw tangent line ---
            XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
            tangentSeries.setName(String.format("y = %.3fx + %.3f", m, b));

            double xMin = x1 - 1;
            double xMax = x2 + 1;
            tangentSeries.getData().add(new XYChart.Data<>(xMin, m * xMin + b));
            tangentSeries.getData().add(new XYChart.Data<>(xMax, m * xMax + b));
            chart.getData().add(tangentSeries);

            System.out.printf("Tangent slope = %.3f, y-intercept = %.3f%n", m, b);
        }

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
        chart2AnchorPane.getChildren().add(chart);
    }

}
