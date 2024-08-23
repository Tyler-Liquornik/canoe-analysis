package com.wecca.canoeanalysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import com.wecca.canoeanalysis.services.YamlMarshallingService;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.Setter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.URISyntaxException;

public class CanoeAnalysisApplication extends Application {

    @Getter @Setter
    private static MainController mainController;

    @Override
    public void start(Stage stage) throws IOException, URISyntaxException {

        // Stage setup
        FXMLLoader mainFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/main-view.fxml"));
        Scene scene = new Scene(mainFxmlLoader.load(), 900, 600);
        stage.setTitle("PADDL");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();

        // com.wecca.canoeanalysis.Main controller setup, load default module (beam module)
        setMainController(mainFxmlLoader.getController());
        mainController.setPrimaryStage(stage);
        mainController.setPrimaryScene(scene);
        FXMLLoader beamFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/beam-view.fxml"));
        AnchorPane moduleInjectionRoot = beamFxmlLoader.load();
        mainController.getModuleInjectionRoot().getChildren().setAll(moduleInjectionRoot);

        // Add the CSS file to the scene's stylesheets
        scene.getStylesheets().add(ResourceManagerService.getResourceFilePathString("css/style.css", false));
        ColorManagerService.registerForRecoloringFromStylesheet(scene, "css/style.css");

        // Adding Logo Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png");
        stage.getIcons().add(icon);

        // Load the color from the previous session or the default orange "#F96C37" on first load
        ColorManagerService.putColorPalette("primary", YamlMarshallingService.loadSettings().getPrimaryColor());
    }

    public static void main(String[] args) {
        launch();
    }
}