package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXDecorator;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Centralized logic for opening and managing auxiliary windows
 * (diagram popups, utility windows, etc.) in the application.
 * <p>
 * This class does <b>not</b> handle the main application window
 * created in {@link CanoeAnalysisApplication}.
 */
public class WindowManagerService {

    /**
     * Horizontal offset between the mouse click position and the stage's X position
     * when a drag operation starts.
     */
    private static double stageXOffset = 0;

    /**
     * Vertical offset between the mouse click position and the stage's Y position
     * when a drag operation starts.
     */
    private static double stageYOffset = 0;

    /**
     * Opens a diagram window and renders a chart based on a list of points.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>Creates a new non-resizable popup {@link Stage}.</li>
     *     <li>If {@code canoe} is non-null, uses an {@link AreaChart} configured
     *         via {@link DiagramService#setupChart(Canoe, List, String, String)}.</li>
     *     <li>If {@code canoe} is null, uses a {@link LineChart} via
     *         {@link DiagramService#setupChart(List, String, String)}.</li>
     *     <li>Wraps the chart in a {@link JFXDecorator} that is draggable
     *         (with macOS fix).</li>
     *     <li>Applies {@code css/chart.css} for styling.</li>
     * </ul>
     *
     * @param title    the title of the diagram window.
     * @param canoe    the {@link Canoe} model used to configure the chart,
     *                 or {@code null} to render a simple chart from points.
     * @param points   the points to render on the diagram (x = horizontal axis, y = vertical axis).
     * @param yUnits   the unit of the y-axis value (e.g. {@code "N"}, {@code "kN·m"}, {@code "MPa"}).
     * @param yValName the physical quantity for the y-axis label (e.g. {@code "Force"}, {@code "Moment"}).
     */
    public static void openDiagramWindow(String title, Canoe canoe, List<Point2D> points, String yUnits, String yValName) {
        // Initialize the stage and main pane
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        Pane chartPane = new Pane();
        chartPane.setPrefSize(1125, 750);
        popupStage.setResizable(false);

        // Add application icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png");
        popupStage.getIcons().add(icon);

        // Set up the chart: AreaChart when canoe is present, otherwise LineChart
        if (canoe != null) {
            AreaChart<Number, Number> chart = DiagramService.setupChart(canoe, points, yUnits, yValName);
            chartPane.getChildren().add(chart);
        } else {
            LineChart<Number, Number> chart = DiagramService.setupChart(points, yUnits, yValName);
            chartPane.getChildren().add(chart);
        }

        // Wrap in a draggable JFXDecorator
        JFXDecorator decorator = getDraggableJFXDecorator(popupStage, chartPane);

        popupStage.setOnShown(event -> chartPane.requestFocus());

        // Create scene, apply chart stylesheet, and show
        Scene scene = new Scene(decorator, 1125, 775);
        addStyleSheet(scene, "css/chart.css");

        popupStage.setScene(scene);
        popupStage.show();
    }

    /**
     * Opens a general-purpose utility window from an FXML file.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>Loads the specified FXML into an {@link AnchorPane}.</li>
     *     <li>Wraps it in a {@link VBox} and {@link JFXDecorator} (draggable).</li>
     *     <li>Applies {@code css/style.css} to the scene.</li>
     *     <li>Sizes the window according to {@code windowWidth} and {@code windowHeight}.</li>
     * </ul>
     *
     * @param title        the title of the utility window.
     * @param fxmlPath     the relative path to the FXML resource from the resources root
     *                     (e.g. {@code "view/dummy-view.fxml"}).
     * @param windowWidth  the width of the utility window (should match the FXML root container width).
     * @param windowHeight the height of the utility window (should match the FXML root container height).
     */
    public static void openUtilityWindow(String title, String fxmlPath, int windowWidth, int windowHeight) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource(fxmlPath));
            AnchorPane rootPane = fxmlLoader.load();

            Stage stage = new Stage();
            JFXDecorator decorator = getDraggableJFXDecorator(stage, new VBox(rootPane));

            Scene scene = new Scene(decorator, windowWidth, windowHeight);
            addStyleSheet(scene, "css/style.css");
            stage.setTitle(title);
            stage.setOnShown(event -> rootPane.requestFocus());
            stage.setResizable(false);
            stage.getIcons().add(new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png"));
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a CSS stylesheet into the given {@link Scene} and registers the scene
     * for dynamic recoloring using {@link ColorManagerService}.
     *
     * @param scene the {@link Scene} whose stylesheets should be updated.
     * @param path  the path to the CSS file (relative to the resources root),
     *              e.g. {@code "css/style.css"} or {@code "css/chart.css"}.
     */
    private static void addStyleSheet(Scene scene, String path) {
        scene.getStylesheets().add(ResourceManagerService.getResourceFilePathString(path, false));
        ColorManagerService.registerForRecoloringFromStylesheet(scene, path);
    }

    /**
     * Closes the given stage.
     *
     * @param stage the {@link Stage} to close (must not be {@code null}).
     */
    public static void closeWindow(Stage stage) {
        stage.close();
    }

    /**
     * Minimizes (iconifies) the given stage to the OS window bar.
     *
     * @param stage the {@link Stage} to minimize (must not be {@code null}).
     */
    public static void minimizeWindow(Stage stage) {
        stage.setIconified(true);
    }

    /**
     * Stores the initial offsets between the mouse position and the top-left corner
     * of the stage when a drag operation begins.
     * <p>
     * Typically called from a mouse-pressed handler on the window’s decorator.
     *
     * @param event the {@link MouseEvent} containing the current position of the mouse
     *              within the {@link Scene}.
     */
    public static void setStageOffsets(MouseEvent event) {
        stageXOffset = event.getSceneX();
        stageYOffset = event.getSceneY();
    }

    /**
     * Moves the given stage to a new position based on the current mouse position
     * on the screen and the stored offsets.
     * <p>
     * Typically called from a mouse-dragged handler to make the window draggable.
     *
     * @param event the {@link MouseEvent} containing the current mouse position
     *              in screen coordinates.
     * @param stage the {@link Stage} to move; if {@code null}, no action is taken.
     */
    public static void moveStage(MouseEvent event, Stage stage) {
        if (stage != null) {
            stage.setX(event.getScreenX() - stageXOffset);
            stage.setY(event.getScreenY() - stageYOffset);
        }
    }

    /**
     * Creates a {@link JFXDecorator} with manual drag handling applied.
     * <p>
     * Rationale:
     * <ul>
     *     <li>JFXDecorator supports dragging by default, but this does not work properly on macOS
     *         due to an unfixed bug in JFoenix (see GitHub issue #590).</li>
     *     <li>This method applies custom mouse-pressed and mouse-dragged handlers to enable
     *         smooth dragging across platforms.</li>
     * </ul>
     *
     * @param popupStage the {@link Stage} that should move when the decorator is dragged.
     * @param rootPane   the root {@link Pane} to decorate (chart pane, VBox, etc.).
     * @return a {@link JFXDecorator} configured to drag the provided stage when clicked and dragged.
     */
    private static JFXDecorator getDraggableJFXDecorator(Stage popupStage, Pane rootPane) {
        JFXDecorator decorator = new JFXDecorator(popupStage, rootPane, false, false, true);

        // Mouse pressed: store offsets
        decorator.setOnMousePressed(WindowManagerService::setStageOffsets);

        // Mouse dragged: move the stage
        decorator.setOnMouseDragged(event -> moveStage(event, popupStage));
        return decorator;
    }

    /**
     * Opens a diagram window for Mohr’s circle / failure envelope visualizations.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>Creates a new non-resizable popup {@link Stage} with a chart.</li>
     *     <li>Renders the "base" circle data using:
     *         <ul>
     *             <li>{@link AreaChart} if {@code canoe} is non-null.</li>
     *             <li>{@link LineChart} if {@code canoe} is null.</li>
     *         </ul>
     *     </li>
     *     <li>If {@code tangentPoints} is provided:
     *         <ul>
     *             <li>If it contains exactly 2 points:
     *                 <ul>
     *                     <li>Interprets them as left and right x-intercepts (σ₃, σ₁) on a single circle.</li>
     *                     <li>Creates two series with labels in the legend.</li>
     *                 </ul>
     *             </li>
     *             <li>If it contains more than 2 points:
     *                 <ul>
     *                     <li>Interprets the first {@code n-2} points as samples along a tangent line.</li>
     *                     <li>Interprets the last 2 points as contact points between the tangent and the circles.</li>
     *                     <li>Computes and displays the tangent equation, contact points,
     *                         and the y-intercept (0, b) as separate series for the legend.</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>Applies {@code css/chart.css} for styling and enables dragging via {@link JFXDecorator}.</li>
     * </ul>
     *
     * @param title         the title of the diagram window.
     * @param canoe         the {@link Canoe} to use for area-chart-based diagram construction,
     *                      or {@code null} for a simple line chart.
     * @param circlePoints  the points representing the Mohr circle(s) to plot.
     * @param tangentPoints optional list of tangent-related points:
     *                      <ul>
     *                          <li>{@code null} or size &lt; 2 → no tangent is shown.</li>
     *                          <li>size == 2 → interpreted as left/right x-intercepts.</li>
     *                          <li>size &gt;= 4 → first {@code n-2} are line samples,
     *                              last two are contact points.</li>
     *                      </ul>
     * @param units         the units for the vertical axis (e.g. {@code "MPa"}).
     * @param yAxisLabel    the label for the vertical axis (e.g. {@code "Shear Stress vs Normal Stress"}).
     */
    public static void openDiagramWindow2(
            String title,
            Canoe canoe,
            List<Point2D> circlePoints,
            List<Point2D> tangentPoints,
            String units,
            String yAxisLabel
    ) {
        // Initialize stage and pane
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        Pane chartPane = new Pane();
        chartPane.setPrefSize(1125, 750);
        popupStage.setResizable(false);

        // Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png");
        popupStage.getIcons().add(icon);

        // Base chart (circle data)
        XYChart<Number, Number> chart;
        if (canoe != null) {
            AreaChart<Number, Number> areaChart =
                    DiagramService.setupChart(canoe, circlePoints, units, yAxisLabel);
            chart = areaChart;
        } else {
            LineChart<Number, Number> lineChart =
                    DiagramService.setupChart(circlePoints, units, yAxisLabel);
            chart = lineChart;
        }
        chart.setLegendVisible(true);

        // Optional tangent / intercept plotting
        if (tangentPoints != null && tangentPoints.size() >= 2) {

            if (tangentPoints.size() == 2) {
                // ===== Single-circle case: two points are x-intercepts (σ3, σ1) =====
                Point2D pA = tangentPoints.get(0);
                Point2D pB = tangentPoints.get(1);

                // Sort so "left" has the smaller x value
                Point2D left = (pA.getX() <= pB.getX()) ? pA : pB;
                Point2D right = (pA.getX() <= pB.getX()) ? pB : pA;

                double s3 = Math.abs(left.getX());
                double s1 = Math.abs(right.getX());

                double s3r = Math.round(s3 * 1000.0) / 1000.0;
                double s1r = Math.round(s1 * 1000.0) / 1000.0;

                XYChart.Series<Number, Number> leftSeries = new XYChart.Series<>();
                XYChart.Series<Number, Number> rightSeries = new XYChart.Series<>();

                leftSeries.setName(String.format("σ3 = %.3f MPa", -s3r));
                rightSeries.setName(String.format("σ1 = %.3f MPa", s1r));

                leftSeries.getData().add(new XYChart.Data<>(left.getX(), left.getY()));
                rightSeries.getData().add(new XYChart.Data<>(right.getX(), right.getY()));

                chart.getData().add(leftSeries);
                chart.getData().add(rightSeries);

            } else {
                // ===== Two-circle envelope case: tangent line + contact points + y-intercept =====
                int n = tangentPoints.size();
                boolean hasContacts = (n >= 4); // last two points are contact points

                // Index of the last line-sample point
                int lineEndIndex = hasContacts ? n - 2 : n;

                Point2D p0 = tangentPoints.get(0);
                Point2D p1 = tangentPoints.get(lineEndIndex - 1);

                double dx = p1.getX() - p0.getX();
                double dy = p1.getY() - p0.getY();
                double m = dy / dx;
                double b = p0.getY() - m * p0.getX();

                double mDisp = Math.round(m * 1000.0) / 1000.0;
                double bDisp = Math.round(b * 1000.0) / 1000.0;

                XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
                tangentSeries.setName(String.format("Tangent: y = %.3f x + %.3f", mDisp, bDisp));

                // Add line points
                for (int i = 0; i < lineEndIndex; i++) {
                    Point2D pt = tangentPoints.get(i);
                    tangentSeries.getData().add(new XYChart.Data<>(pt.getX(), pt.getY()));
                }
                chart.getData().add(tangentSeries);

                // Contact points (last two points in list)
                if (hasContacts) {
                    Point2D cp1 = tangentPoints.get(n - 2);
                    Point2D cp2 = tangentPoints.get(n - 1);

                    double cp1x = Math.round(cp1.getX() * 1000.0) / 1000.0;
                    double cp1y = Math.round(cp1.getY() * 1000.0) / 1000.0;
                    double cp2x = Math.round(cp2.getX() * 1000.0) / 1000.0;
                    double cp2y = Math.round(cp2.getY() * 1000.0) / 1000.0;

                    XYChart.Series<Number, Number> cp1Series = new XYChart.Series<>();
                    XYChart.Series<Number, Number> cp2Series = new XYChart.Series<>();

                    cp1Series.setName(String.format("Contact 1 (%.3f, %.3f)", cp1x, cp1y));
                    cp2Series.setName(String.format("Contact 2 (%.3f, %.3f)", cp2x, cp2y));

                    cp1Series.getData().add(new XYChart.Data<>(cp1.getX(), cp1.getY()));
                    cp2Series.getData().add(new XYChart.Data<>(cp2.getX(), cp2.getY()));

                    chart.getData().add(cp1Series);
                    chart.getData().add(cp2Series);
                }

                // y-intercept marker at (0, b)
                double yIntDisp = Math.round(b * 1000.0) / 1000.0;
                XYChart.Series<Number, Number> yIntSeries = new XYChart.Series<>();
                yIntSeries.setName(String.format("y-intercept (0.000, %.3f)", yIntDisp));
                yIntSeries.getData().add(new XYChart.Data<>(0.0, b));
                chart.getData().add(yIntSeries);
            }
        }

        chartPane.getChildren().add(chart);

        // Wrap in draggable decorator
        JFXDecorator decorator = getDraggableJFXDecorator(popupStage, chartPane);
        popupStage.setOnShown(event -> chartPane.requestFocus());

        Scene scene = new Scene(decorator, 1125, 775);
        addStyleSheet(scene, "css/chart.css");

        popupStage.setScene(scene);
        popupStage.show();
    }
}
