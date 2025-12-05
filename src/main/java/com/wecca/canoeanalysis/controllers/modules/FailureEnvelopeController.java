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
    /**
     * This method generates and displays the shear stress vs compressive and tensile strength graph
     * as a single Mohr circle and labels the x-intercepts (σ3 on the left, σ1 on the right),
     * using the same pattern as the tangent diagram helper.
     */
    public void generateShearStressVSCompressiveAndTensileStrength() {

        List<Point2D> circlePoints   = new ArrayList<>();
        List<Point2D> interceptPoints = new ArrayList<>();

        double sigma1 = Double.parseDouble(maxTension.getText());      // principal tension
        double sigma3 = Double.parseDouble(maxCompression.getText());  // principal compression
        int numPoints = 200; // number of sample points for smoothness


        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = ((sigma1 + sigma3) / 2.0) * Math.cos(angle)
                    - Math.abs((sigma1 - sigma3) / 2.0);
            double y = ((sigma1 + sigma3) / 2.0) * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }


        // left intercept at -σ3, right intercept at +σ1
        double leftX  = -sigma3;
        double rightX =  sigma1;

        interceptPoints.add(new Point2D(leftX, 0.0));
        interceptPoints.add(new Point2D(rightX, 0.0));

        // Rounded for legend text
        double s3r = Math.round(Math.abs(sigma3) * 1000.0) / 1000.0;
        double s1r = Math.round(Math.abs(sigma1) * 1000.0) / 1000.0;

        // ---- Create chart ----
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "MPa", "Normal Stress"
        );
        chart.getXAxis().setLabel("Normal Stress (MPa)");
        chart.getYAxis().setLabel("Shear Stress (MPa)");
        chart.setLegendVisible(true);


        XYChart.Series<Number, Number> leftSeries  = new XYChart.Series<>();
        XYChart.Series<Number, Number> rightSeries = new XYChart.Series<>();

        leftSeries.setName(String.format("σ3 = %.3f MPa", s3r));
        rightSeries.setName(String.format("σ1 = %.3f MPa", s1r));

        leftSeries.getData().add(new XYChart.Data<>(leftX, 0.0));
        rightSeries.getData().add(new XYChart.Data<>(rightX, 0.0));

        chart.getData().add(leftSeries);
        chart.getData().add(rightSeries);

        // ---- Layout ----
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        chart1AnchorPane.getChildren().add(chart);


        WindowManagerService.openDiagramWindow2(
                "Shear Stress vs Normal Stress",
                null,
                circlePoints,
                interceptPoints,   // 2-point list
                "MPa",
                "ShearStress"
        );
    }


    /**
     * Generates the Mohr diagram with:
     *  - tension circle (left) and compression circle (right)
     *  - common upper tangent between them
     *  - legend entries for:
     *        * the tangent line   (y = m x + b)
     *        * the two contact points
     *        * the y-intercept
     */
    public void generateShearStressVSNormalStress() {

        // Points for both circles (for DiagramService + extra window)
        List<Point2D> circlePoints  = new ArrayList<>();
        List<Point2D> tangentPoints = new ArrayList<>();


        double Yc      = Double.parseDouble(compressionField.getText());   // compressive strength
        double maxTens = Double.parseDouble(maxTensile.getText());         // tensile strength
        double maxComp = Double.parseDouble(maxCompression.getText());     // still available if needed

        int numPoints = 200;




        double cComp =  Math.abs(Yc) / 2.0;
        double rComp =  Math.abs(Yc) / 2.0;

        for (int i = 0; i < numPoints; i++) {
            double angle  = 2.0 * Math.PI * i / numPoints;
            double x = rComp * Math.cos(angle) + cComp;
            double y = rComp * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }


        double cTens = -Math.abs(maxTens) / 2.0;
        double rTens =  Math.abs(maxTens) / 2.0;

        for (int i = 0; i < numPoints; i++) {
            double angle  = 2.0 * Math.PI * i / numPoints;
            double x = rTens * Math.cos(angle) + cTens;
            double y = rTens * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // ===== 2. Base chart =====
        LineChart<Number, Number> chart = DiagramService.setupChart(
                circlePoints, "MPa", "Normal Stress"
        );
        chart.getXAxis().setLabel("Normal Stress (MPa)");
        chart.getYAxis().setLabel("Shear Stress (MPa)");
        chart.setLegendVisible(true);


        // Decide which circle is left/right on the x-axis
        double cLeft,  rLeft;
        double cRight, rRight;
        boolean leftIsTension;

        if (cTens < cComp) {
            cLeft        = cTens;  rLeft  = rTens;
            cRight       = cComp;  rRight = rComp;
            leftIsTension = true;
        } else {
            cLeft        = cComp;  rLeft  = rComp;
            cRight       = cTens;  rRight = rTens;
            leftIsTension = false;
        }

        double d      = cRight - cLeft;               // center distance
        double deltaR = rRight - rLeft;               // radius difference
        double denom  = d * d - deltaR * deltaR;

        if (denom > 0.0) {
            // Slope of tangent
            double m = deltaR / Math.sqrt(denom);


            double s = Math.sqrt(1.0 + m * m);


            double b = rLeft * s - m * cLeft;


            double mDisp = Math.round(m * 1000.0) / 1000.0;
            double bDisp = Math.round(b * 1000.0) / 1000.0;

            System.out.println("Mohr envelope tangent: y = " + m + " x + " + b);


            XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
            tangentSeries.setName(String.format("Tangent: y = %.3f x + %.3f", mDisp, bDisp));

            double xMin = cLeft  - 1.1 * rLeft;
            double xMax = cRight + 1.1 * rRight;
            int    numLinePoints = 80;

            for (int i = 0; i <= numLinePoints; i++) {
                double x = xMin + (xMax - xMin) * i / (double) numLinePoints;
                double y = m * x + b;
                tangentSeries.getData().add(new XYChart.Data<>(x, y));
                tangentPoints.add(new Point2D(x, y));   // line points
            }


            // General line: A x + B y + C = 0 => from y = m x + b -> m x - y + b = 0
            double A = m;
            double B = -1.0;
            double C = b;
            double denAB = A * A + B * B;   // = m^2 + 1


            java.util.function.Function<Double, Point2D> contactPoint =
                    (Double cx) -> {
                        double val   = A * cx + C;             // A*cx + B*0 + C
                        double xStar = cx - A * val / denAB;
                        double yStar =      - B * val / denAB; // = val/(m^2+1)
                        return new Point2D(xStar, yStar);
                    };

            Point2D leftContact  = contactPoint.apply(cLeft);
            Point2D rightContact = contactPoint.apply(cRight);


            tangentPoints.add(leftContact);
            tangentPoints.add(rightContact);

            double lx = Math.round(leftContact.getX()  * 1000.0) / 1000.0;
            double ly = Math.round(leftContact.getY()  * 1000.0) / 1000.0;
            double rx = Math.round(rightContact.getX() * 1000.0) / 1000.0;
            double ry = Math.round(rightContact.getY() * 1000.0) / 1000.0;
            double yIntDisp = Math.round(b * 1000.0) / 1000.0;

            // Series for contact points + y-intercept (so legend shows coordinates)
            XYChart.Series<Number, Number> leftContactSeries  = new XYChart.Series<>();
            XYChart.Series<Number, Number> rightContactSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> yInterceptSeries   = new XYChart.Series<>();

            if (leftIsTension) {
                leftContactSeries.setName(String.format("Tension contact (%.3f, %.3f)", lx, ly));
                rightContactSeries.setName(String.format("Compression contact (%.3f, %.3f)", rx, ry));
            } else {
                leftContactSeries.setName(String.format("Compression contact (%.3f, %.3f)", lx, ly));
                rightContactSeries.setName(String.format("Tension contact (%.3f, %.3f)", rx, ry));
            }
            yInterceptSeries.setName(String.format("y-intercept (0.000, %.3f)", yIntDisp));

            leftContactSeries.getData().add(
                    new XYChart.Data<>(leftContact.getX(), leftContact.getY()));
            rightContactSeries.getData().add(
                    new XYChart.Data<>(rightContact.getX(), rightContact.getY()));
            yInterceptSeries.getData().add(
                    new XYChart.Data<>(0.0, b));

            // Add to chart
            chart.getData().add(tangentSeries);
            chart.getData().add(leftContactSeries);
            chart.getData().add(rightContactSeries);
            chart.getData().add(yInterceptSeries);
        }


        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        // Open secondary window that will also plot tangent & label it
        WindowManagerService.openDiagramWindow2(
                "Mohr’s Circle Diagram",
                null,
                circlePoints,
                tangentPoints,
                "MPa",
                "Shear Stress vs Normal Stress"
        );

        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        chart2AnchorPane.getChildren().add(chart);
    }





}



