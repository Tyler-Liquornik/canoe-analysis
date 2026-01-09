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
        initModuleToolBarButtons();
    }

    /**
     * Initializes toolbar buttons for the module.
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        // iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> downloadCanoe());
        // iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD,   e -> uploadCanoe());
        iconGlyphToFunctionMap.put(IconGlyphType.BOOK, e -> openGlossary());

        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    /**
     * Opens a glossary window with relevant equations/definitions.
     */
    public void openGlossary() {
         WindowManagerService.openUtilityWindow(
                  "Glossary",
                  "/com/wecca/canoeanalysis/view/failure-envelope-glossary.fxml",
                  747,
                  403
          );
    }

    /**
     * Calculates maximum tensile, compressive, and shear stresses based on user input.
     *  Mmax – maximum bending moment
     *  Yc – compression force
     *  Yt – tension force
     *  I  – moment of inertia
     *  Vmax – maximum shear force
     *  Qmax – first moment of area at location of interest
     *  t – thickness at shear location
     */
    public void calculateValues() {
        if (maxMoment.getText().isEmpty() || compressionField.getText().isEmpty() ||
                tensionField.getText().isEmpty() || momentOfInertia.getText().isEmpty() ||
                qMax.getText().isEmpty() || maxShear.getText().isEmpty() ||
                canoeThickness.getText().isEmpty()) {

            mainController.showSnackbar("Please fill all the fields with valid numeric values.");
            return;
        }

        // Parse input values
        double Mmax = Double.parseDouble(maxMoment.getText());
        double Yc = Double.parseDouble(compressionField.getText());
        double Yt = Double.parseDouble(tensionField.getText());
        double I = Double.parseDouble(momentOfInertia.getText());
        double Vmax = Double.parseDouble(maxShear.getText());
        double Qmax = Double.parseDouble(qMax.getText());
        double t = Double.parseDouble(canoeThickness.getText());

        // Formulas
        double sigmaCMax = (Mmax * Yc) / I;
        double sigmaTMax = (Mmax * Yt) / I;
        double tauMax = (Vmax * Qmax) / (I * t);

        // Convert to MPa
        maxCompression.setText(String.valueOf(sigmaCMax * (10e-7)));
        maxTension.setText(String.valueOf(sigmaTMax * (10e-7)));
        maxShearStress.setText(String.valueOf(tauMax * (10e-7)));
    }

    /**
     * Generates and displays the two Mohr’s circle diagrams:
     *
     * Single-circle diagram with σ₁ and σ₃ labeled on the x-axis.
     * Two-circle envelope with a common tangent and labeled contact points.
     */
    public void generateDiagrams() {
        if (maxTension.getText().isEmpty() || compressionField.getText().isEmpty() ||
                tensionField.getText().isEmpty() || maxCompression.getText().isEmpty()) {

            mainController.showSnackbar("Please fill all the fields with valid numeric values.");
            return;
        }
        generateShearStressVSCompressiveAndTensileStrength();
        generateShearStressVSNormalStress();
    }

    /**
     * Generates and displays the Mohr’s circle for shear stress vs. normal stress
     * using the principal stresses σ₁ and σ₃.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>Builds a single Mohr circle from σ₁ (tension) and σ₃ (compression).</li>
     *     <li>Plots the circle in {@code chart1AnchorPane}.</li>
     *     <li>Marks and labels the x-intercepts:
     *         <ul>
     *             <li>Left intercept: σ₃ (compression)</li>
     *             <li>Right intercept: σ₁ (tension)</li>
     *         </ul>
     *     </li>
     *     <li>Opens a secondary window via {@link WindowManagerService#openDiagramWindow2}
     *         and passes the circle and intercept points.</li>
     * </ul>
     * Required inputs:
     * <ul>
     *     <li>{@code maxTension} (σ₁)</li>
     *     <li>{@code maxCompression} (σ₃)</li>
     * </ul>
     */
    public void generateShearStressVSCompressiveAndTensileStrength() {

        List<Point2D> circlePoints = new ArrayList<>();
        List<Point2D> interceptPoints = new ArrayList<>();

        // σ1: principal tensile stress; σ3: principal compressive stress
        double sigma1 = Double.parseDouble(maxTension.getText());
        double sigma3 = Double.parseDouble(maxCompression.getText());
        int numPoints = 200; // number of sample points for smoothness

        /*
         * Standard Mohr’s circle construction:
         * Center = (σ1 + σ3) / 2
         * Radius = (σ1 - σ3) / 2
         *
         * Here we parametrize the circle with angle θ and convert to (σ, τ).
         */
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double center = (sigma1 + sigma3) / 2.0;
            double radius = (sigma1 - sigma3) / 2.0;

            double x = center + radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // Left and right x-intercepts on Mohr’s circle
        // (σ3 is compressive, so appears on the left; σ1 on the right)
        double leftX = sigma3;
        double rightX = sigma1;

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

        // Series for the two x-intercept points
        XYChart.Series<Number, Number> leftSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> rightSeries = new XYChart.Series<>();

        leftSeries.setName(String.format("σ₃ = %.3f MPa", s3r));
        rightSeries.setName(String.format("σ₁ = %.3f MPa", s1r));

        leftSeries.getData().add(new XYChart.Data<>(leftX, 0.0));
        rightSeries.getData().add(new XYChart.Data<>(rightX, 0.0));

        chart.getData().add(leftSeries);
        chart.getData().add(rightSeries);

        // Fill the anchor pane
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        chart.getStylesheets().add(
                ResourceManagerService.getResourceFilePathString("css/chart.css", false)
        );

        chart1AnchorPane.getChildren().add(chart);

        // Secondary window with the same data
        WindowManagerService.openDiagramWindow2(
                "Shear Stress vs Normal Stress",
                null,
                circlePoints,
                interceptPoints,
                "MPa",
                "ShearStress"
        );
    }

    /**
     * Generates the Mohr diagram with two circles (tension and compression) and a common tangent
     * that defines the failure envelope.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>Builds:
     *         <ul>
     *             <li>A compression circle with center {@code +Yc/2} and radius {@code Yc/2}.</li>
     *             <li>A tension circle with center {@code -σt/2} and radius {@code σt/2}.</li>
     *         </ul>
     *     </li>
     *     <li>Determines which circle lies to the left/right on the σ-axis.</li>
     *     <li>Computes the common upper tangent line between the two circles:
     *         <ul>
     *             <li>Finds slope {@code m} and intercept {@code b} of the line.</li>
     *             <li>Computes contact points on each circle.</li>
     *             <li>Finds the y-intercept (0, b).</li>
     *         </ul>
     *     </li>
     *     <li>Plots:
     *         <ul>
     *             <li>Both circles (via {@link DiagramService#setupChart}).</li>
     *             <li>The tangent line.</li>
     *             <li>The two contact points (labeled as tension/compression).</li>
     *             <li>The y-intercept.</li>
     *         </ul>
     *     </li>
     *     <li>Renders the chart in {@code chart2AnchorPane} and opens a secondary diagram window
     *         with {@link WindowManagerService#openDiagramWindow2}.</li>
     * </ul>
     * Required inputs:
     * <ul>
     *     <li>{@code compressionField} – compressive strength (Yc)</li>
     *     <li>{@code maxTensile}      – tensile strength limit</li>
     *     <li>{@code maxCompression}  – max compressive stress (currently not directly used)</li>
     * </ul>
     */
    public void generateShearStressVSNormalStress() {

        // Points for both circles (for DiagramService + extra window)
        List<Point2D> circlePoints = new ArrayList<>();
        List<Point2D> tangentPoints = new ArrayList<>();

        // Input strengths
        double Yc = Double.parseDouble(compressionField.getText());   // compressive strength
        double maxTens = Double.parseDouble(maxTensile.getText());    // tensile strength
        double maxComp = Double.parseDouble(maxCompression.getText()); // still available if needed

        int numPoints = 200;

        // ===== 1. Build compression circle =====
        // Center and radius for compression circle (right side of σ-axis)
        double cComp = Math.abs(Yc) / 2.0;
        double rComp = Math.abs(Yc) / 2.0;

        for (int i = 0; i < numPoints; i++) {
            double angle = 2.0 * Math.PI * i / numPoints;
            double x = rComp * Math.cos(angle) + cComp;
            double y = rComp * Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // ===== 1b. Build tension circle =====
        // Center and radius for tension circle (left side of σ-axis)
        double cTens = -Math.abs(maxTens) / 2.0;
        double rTens = Math.abs(maxTens) / 2.0;

        for (int i = 0; i < numPoints; i++) {
            double angle = 2.0 * Math.PI * i / numPoints;
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
        double cLeft, rLeft;
        double cRight, rRight;
        boolean leftIsTension;

        if (cTens < cComp) {
            cLeft = cTens;
            rLeft = rTens;
            cRight = cComp;
            rRight = rComp;
            leftIsTension = true;
        } else {
            cLeft = cComp;
            rLeft = rComp;
            cRight = cTens;
            rRight = rTens;
            leftIsTension = false;
        }

        double d = cRight - cLeft;      // distance between circle centers
        double deltaR = rRight - rLeft; // difference in radii
        double denom = d * d - deltaR * deltaR;

        /*
         * Geometry for common external tangent between two circles:
         * If d is the center distance and rRight, rLeft are radii, then
         * the slope of the common external tangent is:
         *
         *     m = (rRight - rLeft) / sqrt(d^2 - (rRight - rLeft)^2)
         *
         * Once m is known, the tangent line is y = m x + b, and we can
         * solve for b by enforcing distance from line to circle center
         * equals the circle radius.
         */
        if (denom > 0.0) {
            // Slope of tangent
            double m = deltaR / Math.sqrt(denom);

            // Distance from line y = m x + b to circle center must equal radius
            // |A*cx + B*cy + C| / sqrt(A^2 + B^2) = r, where A=m, B=-1, C=b
            double s = Math.sqrt(1.0 + m * m);
            double b = rLeft * s - m * cLeft;

            double mDisp = Math.round(m * 1000.0) / 1000.0;
            double bDisp = Math.round(b * 1000.0) / 1000.0;

            System.out.println("Mohr envelope tangent: y = " + m + " x + " + b);

            // Tangent line series
            XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
            tangentSeries.setName(String.format("Tangent: y = %.3f x + %.3f", mDisp, bDisp));

            double xMin = cLeft - 1.1 * rLeft;
            double xMax = cRight + 1.1 * rRight;
            int numLinePoints = 80;

            for (int i = 0; i <= numLinePoints; i++) {
                double x = xMin + (xMax - xMin) * i / (double) numLinePoints;
                double y = m * x + b;
                tangentSeries.getData().add(new XYChart.Data<>(x, y));
                tangentPoints.add(new Point2D(x, y));   // line points
            }

            // General line in form A x + B y + C = 0
            // From y = m x + b  =>  m x - y + b = 0
            double A = m;
            double B = -1.0;
            double C = b;
            double denAB = A * A + B * B;   // = m^2 + 1

            // Function to compute contact point of the line with a circle whose center is at (cx, 0)
            java.util.function.Function<Double, Point2D> contactPoint =
                    (Double cx) -> {
                        // Distance from center to line projection
                        double val = A * cx + C;             // A*cx + B*0 + C
                        double xStar = cx - A * val / denAB; // projected x on line
                        double yStar = -B * val / denAB;     // projected y on line
                        return new Point2D(xStar, yStar);
                    };

            // Contact points on left and right circles
            Point2D leftContact = contactPoint.apply(cLeft);
            Point2D rightContact = contactPoint.apply(cRight);

            tangentPoints.add(leftContact);
            tangentPoints.add(rightContact);

            double lx = Math.round(leftContact.getX() * 1000.0) / 1000.0;
            double ly = Math.round(leftContact.getY() * 1000.0) / 1000.0;
            double rx = Math.round(rightContact.getX() * 1000.0) / 1000.0;
            double ry = Math.round(rightContact.getY() * 1000.0) / 1000.0;
            double yIntDisp = Math.round(b * 1000.0) / 1000.0;

            // Series for contact points + y-intercept (so legend shows coordinates)
            XYChart.Series<Number, Number> leftContactSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> rightContactSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> yInterceptSeries = new XYChart.Series<>();

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

        // Anchor the chart to fill its pane
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
