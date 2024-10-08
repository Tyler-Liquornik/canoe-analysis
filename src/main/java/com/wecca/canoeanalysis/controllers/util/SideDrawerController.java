package com.wecca.canoeanalysis.controllers.util;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.services.WindowManagerService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Traceable
public class SideDrawerController implements Initializable {

    @FXML
    private FontAwesomeIcon hullBuilderIcon, beamIcon, punchingShearIcon, criticalSectionsIcon, failureEnvelopeIcon, poaIcon;

    public static Module selectedModule;
    private static FontAwesomeIcon selectedIcon;

    @Getter
    public enum Module {

        HULL_BUILDER("hull-builder-view"),
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
    public void clickHullBuilderButton() throws IOException {selectModule(Module.HULL_BUILDER, hullBuilderIcon);}
    public void clickBeamButton() throws IOException {selectModule(Module.BEAM, beamIcon);}
    public void clickPunchingShearButton() throws IOException {selectModule(Module.PUNCHING_SHEAR, punchingShearIcon);}
    public void clickCriticalSectionsButton() throws IOException {selectModule(Module.CRITICAL_SECTIONS, criticalSectionsIcon);}
    public void clickFailureEnvelopeButton() throws IOException {selectModule(Module.FAILURE_ENVELOPE, failureEnvelopeIcon);}
    public void clickPoaButton() throws IOException {selectModule(Module.PERCENT_OPEN_AREA, poaIcon);}

    // Auxiliary button handlers
    public void clickSettingsButton() {
        WindowManagerService.openUtilityWindow("Settings", "view/settings-view.fxml", 550, 325);
    }

    public void clickAboutButton() {
        WindowManagerService.openUtilityWindow("About Me", "view/about-view.fxml", 550, 325);
    }

    public void selectModule(Module module, FontAwesomeIcon icon) throws IOException {
        if (selectedModule != module) {
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
        selectedModule = Module.HULL_BUILDER;
        selectedIcon = hullBuilderIcon;
    }
}
