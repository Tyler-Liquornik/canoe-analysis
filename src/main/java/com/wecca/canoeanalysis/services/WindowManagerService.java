package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXDecorator;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.TraceIgnore;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
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
     * @param yUnits the units of the y-axis on the diagram.
     */
    public static void openDiagramWindow(String title, Canoe canoe, List<Point2D> points, String yUnits)
    {
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
        AreaChart<Number, Number> chart = DiagramService.setupChart(canoe, points, yUnits);
        chartPane.getChildren().add(chart);

        // Setting up the window with a decorator
        JFXDecorator decorator = getDraggableJFXDecorator(popupStage, chartPane);

        popupStage.setOnShown(event -> chartPane.requestFocus());
        Scene scene = new Scene(decorator, 1125, 775);
        addStyleSheet(scene, "css/chart.css");

        // Setting the scene and showing the stage
        popupStage.setScene(scene);
        popupStage.show();
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

    public static void setStageOffsets(MouseEvent event) {
        stageXOffset = event.getSceneX();
        stageYOffset = event.getSceneY();
    }

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
}
