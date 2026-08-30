package com.wecca.canoeanalysis;

import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.ModuleSelectorController;
import com.wecca.canoeanalysis.models.data.Settings;
import com.wecca.canoeanalysis.services.LoggerService;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import com.wecca.canoeanalysis.services.MarshallingService;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

public class CanoeAnalysisApplication extends Application {

    @Getter @Setter
    private static MainController mainController;

    /**
     * Called implicitly by launch() in main()
     */
    @Override
    public void start(Stage stage) throws IOException, URISyntaxException {

        // Logging setup
        LoggerService.redirectSystemStreamsToLogger();
        LoggerService.createDebugFileParentFolder();

        // Stage setup
        FXMLLoader mainFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/main-view.fxml"));
        Scene scene = new Scene(mainFxmlLoader.load(), 900, 600);
        stage.setTitle("PADDL");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();

        // com.wecca.canoeanalysis.Main controller setup, load default module (hull builder module)
        setMainController(mainFxmlLoader.getController());
        mainController.setPrimaryStage(stage);
        mainController.setPrimaryScene(scene);
        FXMLLoader beamFxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/" + ModuleSelectorController.selectedModule.getViewName() + ".fxml"));
        AnchorPane moduleInjectionRoot = beamFxmlLoader.load();
        mainController.getModuleInjectionRoot().getChildren().setAll(moduleInjectionRoot);
        ModuleSelectorController.selectModule(ModuleSelectorController.Module.HULL_BUILDER, true);

        // Add the CSS file to the scene's stylesheets
        scene.getStylesheets().add(ResourceManagerService.getResourceFilePathString("css/style.css", false));
        ColorManagerService.registerForRecoloringFromStylesheet(scene, "css/style.css");

        // Resolve the icon from the classpath so it works from both the IDE and
        // the JAR embedded inside native Windows/macOS packages.
        Image icon = new Image(Objects.requireNonNull(
                CanoeAnalysisApplication.class.getResource("images/canoe.png")).toExternalForm());
        stage.getIcons().add(icon);

        // Load the color from the previous session or the default orange "#F96C37" on first load
        ColorManagerService.putColorPalette("primary", MarshallingService
                        .loadYamlData(Settings.class, new Settings("#F96C37"), MarshallingService.SETTINGS_FILE_PATH)
                        .getPrimaryColor());
    }

    public static void main(String[] args) {
        launch();
    }
}
