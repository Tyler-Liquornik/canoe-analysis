package com.wecca.canoeanalysis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class CanoeAnalysisApplication extends Application {

    // The entry point of the application
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/canoe-analysis-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 625);
        stage.setTitle("PADDL");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);//removes the top bar, need to custom make this
        stage.show();

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