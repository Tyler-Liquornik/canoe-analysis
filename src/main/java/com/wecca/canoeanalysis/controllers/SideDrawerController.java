package com.wecca.canoeanalysis.controllers;

import com.jfoenix.controls.JFXDecorator;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SideDrawerController implements Initializable {

    @FXML
    private FontAwesomeIcon beamIcon, punchingShearIcon, criticalSectionsIcon, failureEnvelopeIcon, poaIcon;

    private Module selectedModule;
    public FontAwesomeIcon selectedIcon;

    @Getter
    public enum Module {

        BEAM("beam-view"),
        PUNCHING_SHEAR("punching-shear-view"),
        CRITICAL_SECTIONS("critical-sections-view"),
        FAILURE_ENVELOPE("failure-envelope-view"),
        PERCENT_OPEN_AREA("percent-open-area-view");

        private final String viewName;

        Module(String viewName) {
            this.viewName = viewName;
        }
    }

    // Module selection handlers
    public void clickBeamButton() throws IOException {selectModule(Module.BEAM, beamIcon);}
    public void clickPunchingShearButton() throws IOException {selectModule(Module.PUNCHING_SHEAR, punchingShearIcon);}
    public void clickCriticalSectionsButton() throws IOException {selectModule(Module.CRITICAL_SECTIONS, criticalSectionsIcon);}
    public void clickFailureEnvelopeButton() throws IOException {selectModule(Module.FAILURE_ENVELOPE, failureEnvelopeIcon);}
    public void clickPoaButton() throws IOException {selectModule(Module.PERCENT_OPEN_AREA, poaIcon);}

    // Auxiliary button handlers
    public void clickSettingsButton() {
    }

    public void clickAboutButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/about-view.fxml"));
        AnchorPane rootPane = fxmlLoader.load();

        Stage stage = new Stage();
        JFXDecorator decorator = new JFXDecorator(stage, new VBox(rootPane), false, false, true);

        Scene scene = new Scene(decorator, 550, 325);
        stage.setTitle("About Me");
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnShown(event -> rootPane.requestFocus());
        stage.setResizable(false);
        stage.getIcons().add(new Image("file:src/main/resources/com/wecca/canoeanalysis/images/canoe.png"));
        stage.show();
    }

    public void selectModule(Module module, FontAwesomeIcon icon) throws IOException {
        if (selectedModule != module)
        {
            // Change the icon for the selected module in the drawer
            selectedModule = module;
            selectedIcon.setGlyphName("ANGLE_RIGHT");
            selectedIcon = icon;
            icon.setGlyphName("ANGLE_DOUBLE_RIGHT");

            // Load the new FXML content
            FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/" + module.getViewName() + ".fxml"));
            AnchorPane moduleInjectionRoot = fxmlLoader.load();

            // Set the new root from the respective view FXMl file
            CanoeAnalysisApplication.getMainController().getModuleInjectionRoot().getChildren().setAll(moduleInjectionRoot);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.selectedModule = Module.BEAM;
        this.selectedIcon = beamIcon;
    }
}
