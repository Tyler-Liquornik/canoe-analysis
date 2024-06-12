package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXDecorator;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.diagrams.DiagramPoint;
import com.wecca.canoeanalysis.components.diagrams.FixedTicksNumberAxis;
import com.wecca.canoeanalysis.models.Canoe;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;

/**
 * Logic for opening windows and associated initializations
 * This does not include the main window opened in CanoeAnalysisApplication
 */
public class WindowOpenerService {
    /**
     * Set up the canvas/pane for a diagram.
     * @param title  the title of the diagram.
     * @param canoe  to work with
     * @param points the points to render on the diagram.
     * @param yUnits the units of the y-axis on the diagram.
     */
    public static void openDiagramWindow(String title, Canoe canoe, List<DiagramPoint> points, String yUnits)
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
        JFXDecorator decorator = new JFXDecorator(popupStage, chartPane, false, false, true);
        popupStage.setOnShown(event -> chartPane.requestFocus());
        Scene scene = new Scene(decorator, 1125, 775);
        styleJFXDecorator(scene, "css/chart.css");

        // Setting the scene and showing the stage
        popupStage.setScene(scene);
        popupStage.show();
    }

    /**
     * @param title the title of the window
     */
    public static void openAboutWindow(String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/about-view.fxml"));
        AnchorPane rootPane = fxmlLoader.load();

        Stage stage = new Stage();
        JFXDecorator decorator = new JFXDecorator(stage, new VBox(rootPane), false, false, true);

        Scene scene = new Scene(decorator, 550, 325);
        styleJFXDecorator(scene, "css/style.css");
        stage.setTitle(title);
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnShown(event -> rootPane.requestFocus());
        stage.setResizable(false);
        stage.getIcons().add(new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png"));
        stage.show();
    }

    /**
     * Load CSS to the JFXDecorator which must be done through the scene and cannot be added inline in FXML
     * @param scene the Scene on which a decorator be styled is present
     */
    private static void styleJFXDecorator(Scene scene, String cssPath) {
        scene.getStylesheets().add(CanoeAnalysisApplication.class.getResource(cssPath).toExternalForm());
        ColorManagerService.addEntryToStylesheetMapping(scene, cssPath);
    }
}
