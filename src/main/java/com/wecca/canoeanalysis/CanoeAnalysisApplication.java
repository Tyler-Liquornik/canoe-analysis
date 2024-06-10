package com.wecca.canoeanalysis;

import com.wecca.canoeanalysis.controllers.MainController;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.Setter;
import org.burningwave.core.assembler.StaticComponentContainer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class CanoeAnalysisApplication extends Application {

    @Getter @Setter
    private static MainController moduleController;

    // The entry point of the application
    @Override
    public void start(Stage stage) throws IOException {
        StaticComponentContainer.Modules.exportAllToAll();

        FXMLLoader mainFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/main-view.fxml"));
        Scene scene = new Scene(mainFxmlLoader.load(), 900, 600);
        stage.setTitle("PADDL");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();

        // Provide a references of stage, scene, controller between
        setModuleController(mainFxmlLoader.getController());
        moduleController.setPrimaryStage(stage);
        moduleController.setPrimaryScene(scene);
        FXMLLoader beamFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/beam-view.fxml"));
        AnchorPane moduleInjectionRoot = beamFxmlLoader.load();
        moduleController.getModuleInjectionRoot().getChildren().setAll(moduleInjectionRoot);

        // Add the CSS file to the scene's stylesheets
        scene.getStylesheets().add(getClass().getResource("css/font.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("css/style.css").toExternalForm());

        // Adding Logo Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png");
        stage.getIcons().add(icon);
    }

    public static void main(String[] args) {
        launch();
    }
}