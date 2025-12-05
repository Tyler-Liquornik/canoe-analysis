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
 * Logic for opening windows and associated initializations
 * This does not include the main window opened in CanoeAnalysisApplication
 */
public class WindowManagerService {

    private static double stageXOffset = 0;
    private static double stageYOffset = 0;

    /**
     * Set up the canvas/pane for a diagram.
     * @param title  the title of the diagram.
     * @param canoe  to work with
     * @param points the points to render on the diagram.
     * @param yUnits the unit of the y value (i.e. N or kN·m)
     * @param yValName the name representing the y val (i.e. Force or Moment)
     */
    public static void openDiagramWindow(String title, Canoe canoe, List<Point2D> points, String yUnits, String yValName) {
        // Initializing the stage and main pane
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        Pane chartPane = new Pane();
        chartPane.setPrefSize(1125, 750);
        popupStage.setResizable(false);

        // Adding Logo Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png");
        popupStage.getIcons().add(icon);

        // Setting up the diagram specifics
        if(canoe != null) {
            AreaChart<Number, Number> chart = DiagramService.setupChart(canoe, points, yUnits, yValName);
            chartPane.getChildren().add(chart);
        }else{
            LineChart<Number, Number> chart = DiagramService.setupChart(points, yUnits, yValName);
            chartPane.getChildren().add(chart);
        }


        // Setting up the window with a decorator
        JFXDecorator decorator = getDraggableJFXDecorator(popupStage, chartPane);

        popupStage.setOnShown(event -> chartPane.requestFocus());
        if(canoe != null){
            Scene scene = new Scene(decorator, 1125, 775);
            addStyleSheet(scene, "css/chart.css");

            // Setting the scene and showing the stage
            popupStage.setScene(scene);
            popupStage.show();
        }else{
            Scene scene = new Scene(decorator, 1125, 775);
            addStyleSheet(scene, "css/chart.css");

            // Setting the scene and showing the stage
            popupStage.setScene(scene);
            popupStage.show();
        }


    }

    /**
     * @param title the title of the window
     * @param fxmlPath the path to the FXML file in from the resources folder (i.e. view/dummy-view.fxml)
     * @param windowWidth the width of the utility window to open
     * @param windowHeight the height of the utility window to open
     * Note: the window width and height should match the dimensions of the root container (typically an anchor pane) in the FXML
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
     * Load CSS to the JFXDecorator which must be done through the scene and cannot be added inline in FXML
     * @param scene the Scene on which a decorator be styled is present
     */
    private static void addStyleSheet(Scene scene, String path) {
        scene.getStylesheets().add(ResourceManagerService.getResourceFilePathString(path, false));
        ColorManagerService.registerForRecoloringFromStylesheet(scene, path);
    }

    /**
     * Close a stage
     * @param stage to close
     */
    public static void closeWindow(Stage stage) {stage.close();}

    /**
     * Minimize a window to the toolbar
     * @param stage to
     */
    public static void minimizeWindow(Stage stage) {stage.setIconified(true);}

    /**
     * Sets the initial offsets of the stage based on the mouse event's coordinates within the scene.
     * This method is typically used when a drag operation starts, storing the offsets between
     * the mouse position and the top-left corner of the stage.
     * @param event contains the current position of the mouse within the scene.
     */
    public static void setStageOffsets(MouseEvent event) {
        stageXOffset = event.getSceneX();
        stageYOffset = event.getSceneY();
    }

    /**
     * Moves the given stage to a new position based on the current mouse position on the screen.
     * Called during a drag operation to reposition the stage as the user drags it.
     * @param event contains g the current screen coordinates of the mouse.
     * @param stage to be moved
     */
    public static void moveStage(MouseEvent event, Stage stage) {
        if (stage != null) {
            stage.setX(event.getScreenX() - stageXOffset);
            stage.setY(event.getScreenY() - stageYOffset);
        }
    }

    /**
     * JFXDecorator is built to be draggable already, but does not work on macOS
     * This is due to an unfixed bug in JFoenix, so a manual fix is required
     * See: https://github.com/sshahine/JFoenix/issues/590
     * @param popupStage the stage to move on drag
     * @param rootPane the root pane of the stage
     * @return a draggable JFXDecorator
     */
    private static JFXDecorator getDraggableJFXDecorator(Stage popupStage, Pane rootPane) {
        JFXDecorator decorator = new JFXDecorator(popupStage, rootPane, false, false, true);
        // Mouse pressed event handler
        decorator.setOnMousePressed(WindowManagerService::setStageOffsets);

        // Mouse dragged event handler
        decorator.setOnMouseDragged(event -> moveStage(event, popupStage));
        return decorator;
    }

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

        // Base chart
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


        if (tangentPoints != null && tangentPoints.size() >= 2) {

            if (tangentPoints.size() == 2) {
                // single circle
                Point2D pA = tangentPoints.get(0);
                Point2D pB = tangentPoints.get(1);

                // sort so left has smaller x
                Point2D left  = (pA.getX() <= pB.getX()) ? pA : pB;
                Point2D right = (pA.getX() <= pB.getX()) ? pB : pA;

                double s3 = Math.abs(left.getX());
                double s1 = Math.abs(right.getX());

                double s3r = Math.round(s3 * 1000.0) / 1000.0;
                double s1r = Math.round(s1 * 1000.0) / 1000.0;

                XYChart.Series<Number, Number> leftSeries  = new XYChart.Series<>();
                XYChart.Series<Number, Number> rightSeries = new XYChart.Series<>();

                leftSeries.setName(String.format("σ3 = %.3f MPa", -s3r));
                rightSeries.setName(String.format("σ1 = %.3f MPa", s1r));

                leftSeries.getData().add(new XYChart.Data<>(left.getX(), left.getY()));
                rightSeries.getData().add(new XYChart.Data<>(right.getX(), right.getY()));

                chart.getData().add(leftSeries);
                chart.getData().add(rightSeries);

            } else {
                //  two-circle envelope
                int n = tangentPoints.size();
                boolean hasContacts = (n >= 4); // te

                int lineEndIndex = hasContacts ? n - 2 : n;

                Point2D p0 = tangentPoints.get(0);
                Point2D p1 = tangentPoints.get(lineEndIndex - 1);

                double dx = p1.getX() - p0.getX();
                double dy = p1.getY() - p0.getY();
                double m  = dy / dx;
                double b  = p0.getY() - m * p0.getX();

                double mDisp = Math.round(m * 1000.0) / 1000.0;
                double bDisp = Math.round(b * 1000.0) / 1000.0;

                XYChart.Series<Number, Number> tangentSeries = new XYChart.Series<>();
                tangentSeries.setName(String.format("Tangent: y = %.3f x + %.3f", mDisp, bDisp));

                for (int i = 0; i < lineEndIndex; i++) {
                    Point2D pt = tangentPoints.get(i);
                    tangentSeries.getData().add(new XYChart.Data<>(pt.getX(), pt.getY()));
                }
                chart.getData().add(tangentSeries);

                // contact points
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

                // y-intercept marker
                double yIntDisp = Math.round(b * 1000.0) / 1000.0;
                XYChart.Series<Number, Number> yIntSeries = new XYChart.Series<>();
                yIntSeries.setName(String.format("y-intercept (0.000, %.3f)", yIntDisp));
                yIntSeries.getData().add(new XYChart.Data<>(0.0, b));
                chart.getData().add(yIntSeries);
            }
        }

        chartPane.getChildren().add(chart);

        JFXDecorator decorator = getDraggableJFXDecorator(popupStage, chartPane);
        popupStage.setOnShown(event -> chartPane.requestFocus());

        Scene scene = new Scene(decorator, 1125, 775);
        addStyleSheet(scene, "css/chart.css");

        popupStage.setScene(scene);
        popupStage.show();
    }




}
