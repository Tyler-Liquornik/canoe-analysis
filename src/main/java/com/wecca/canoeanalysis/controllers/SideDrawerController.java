package com.wecca.canoeanalysis.controllers;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.Getter;

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
    public void clickBeamButton() {selectModule(Module.BEAM, beamIcon);}
    public void clickPunchingShearButton() {selectModule(Module.PUNCHING_SHEAR, punchingShearIcon);}
    public void clickCriticalSectionsButton() {selectModule(Module.CRITICAL_SECTIONS, criticalSectionsIcon);}
    public void clickFailureEnvelopeButton() {selectModule(Module.FAILURE_ENVELOPE, failureEnvelopeIcon);}
    public void clickPoaButton() {selectModule(Module.PERCENT_OPEN_AREA, poaIcon);}

    // Auxiliary button handlers
    public void clickSettingsButton() {
    }

    public void clickAboutButton() {
    }

    public void selectModule(Module module, FontAwesomeIcon icon)
    {
        if (selectedModule != module)
        {
            selectedModule = module;
            selectedIcon.setGlyphName("ANGLE_RIGHT");
            selectedIcon = icon;
            icon.setGlyphName("ANGLE_DOUBLE_RIGHT");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.selectedModule = Module.BEAM;
        this.selectedIcon = beamIcon;
    }
}
