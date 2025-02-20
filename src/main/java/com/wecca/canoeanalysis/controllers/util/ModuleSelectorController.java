package com.wecca.canoeanalysis.controllers.util;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.HullBuilderController;
import com.wecca.canoeanalysis.controllers.modules.ModuleController;
import com.wecca.canoeanalysis.services.WindowManagerService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

@Traceable
public class ModuleSelectorController implements Initializable {

    @FXML
    private FontAwesomeIcon hullBuilderIcon, beamIcon, punchingShearIcon, criticalSectionsIcon, failureEnvelopeIcon, poaIcon;

    public static ModuleController selectedModuleController;
    public static Module selectedModule;
    private static FontAwesomeIcon selectedIcon;

    public static Map<Module, FontAwesomeIcon> moduleToSelectorIconMap;

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
    public void clickHullBuilderButton() throws IOException {selectModule(Module.HULL_BUILDER, false);}
    public void clickBeamButton() throws IOException {selectModule(Module.BEAM, false);}
    public void clickPunchingShearButton() throws IOException {selectModule(Module.PUNCHING_SHEAR, false);}
    public void clickCriticalSectionsButton() throws IOException {selectModule(Module.CRITICAL_SECTIONS, false);}
    public void clickFailureEnvelopeButton() throws IOException {selectModule(Module.FAILURE_ENVELOPE, false);}
    public void clickPoaButton() throws IOException {selectModule(Module.PERCENT_OPEN_AREA, false);}

    // Auxiliary button handlers
    public void clickSettingsButton() {
        WindowManagerService.openUtilityWindow("Settings", "view/settings-view.fxml", 550, 325);
    }

    public void clickAboutButton() {
        WindowManagerService.openUtilityWindow("About Me", "view/about-view.fxml", 550, 325);
    }

    /**
     * Selects a module to load in state across the app
     * @param module the module to load
     * @param initialLoad only should be true on the initial module load from the app's driver class/method
     *                    if initialLoad is used elsewhere it is not expected to work and should not be used
     */
    public static void selectModule(Module module, boolean initialLoad) throws IOException {
        if (selectedModule != module || initialLoad) {
            // Remove the key event filter for handling pressing shift used in Hull Builder
            MainController mainController = CanoeAnalysisApplication.getMainController();
            ModuleController currentModuleController = mainController.getCurrentModuleController();
            // TODO: if other modules require key event filters, make a marker interface and group the modules to get rid of this instanceof
            if (currentModuleController instanceof HullBuilderController)
                ((HullBuilderController) currentModuleController).removeKeyEventFilters();

            // Change the icon for the selected module in the drawer
            selectedModule = module;
            selectedIcon.setGlyphName("ANGLE_RIGHT");
            FontAwesomeIcon icon = moduleToSelectorIconMap.get(module);
            selectedIcon = icon;
            icon.setGlyphName("ANGLE_DOUBLE_RIGHT");

            // Load the new FXML content
            FXMLLoader fxmlLoader = new FXMLLoader(CanoeAnalysisApplication.class.getResource("view/" + module.getViewName() + ".fxml"));
            AnchorPane moduleInjectionRoot = fxmlLoader.load();

            // Set the reference to the selected controller
            ModuleController newController = fxmlLoader.getController();
            mainController.setCurrentModuleController(newController);
            selectedModuleController = newController;

            // Set the new root from the respective view FXMl file
            CanoeAnalysisApplication.getMainController().getModuleInjectionRoot().getChildren().setAll(moduleInjectionRoot);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        selectedModule = Module.HULL_BUILDER;
        selectedIcon = hullBuilderIcon;
        moduleToSelectorIconMap = Map.of(
                Module.HULL_BUILDER, hullBuilderIcon,
                Module.BEAM, beamIcon,
                Module.PUNCHING_SHEAR, punchingShearIcon,
                Module.CRITICAL_SECTIONS, criticalSectionsIcon,
                Module.FAILURE_ENVELOPE, failureEnvelopeIcon,
                Module.PERCENT_OPEN_AREA, poaIcon
        );;
    }
}
