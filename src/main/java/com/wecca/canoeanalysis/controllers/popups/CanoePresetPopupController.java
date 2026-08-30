package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.services.WindowManagerService;
import com.wecca.canoeanalysis.utils.CanoePreset;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared two-canoe chooser used by every implemented module that supports
 * yearly presets. The module supplies the callback because a preset means
 * hull geometry in Beam/Hull Builder and report inputs in analysis modules.
 */
public class CanoePresetPopupController {

    private static Consumer<CanoePreset> presetHandler;

    /**
     * Configures and opens the chooser for the calling module.
     *
     * @param mainController current main-window controller
     * @param handler module-specific preset loader
     */
    public static void open(MainController mainController, Consumer<CanoePreset> handler) {
        Objects.requireNonNull(mainController, "Main controller is required");
        presetHandler = Objects.requireNonNull(handler, "Preset handler is required");
        WindowManagerService.openUtilityWindow(
                "Canoe Presets",
                "view/canoe-preset-popup-view.fxml",
                350,
                170);
    }

    /** Selects the 2025 GirRaft data set. */
    public void setGirRaftPreset(ActionEvent event) {
        applyPresetAndClose(CanoePreset.GIRRAFT_2025, event);
    }

    /** Selects the 2026 Raft Punk data set. */
    public void setRaftPunkPreset(ActionEvent event) {
        applyPresetAndClose(CanoePreset.RAFT_PUNK_2026, event);
    }

    /** Closes the chooser without changing module state. */
    public void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        WindowManagerService.closeWindow(stage);
    }

    /** Sends the selection to the current module before closing the popup. */
    private void applyPresetAndClose(CanoePreset preset, ActionEvent event) {
        if (presetHandler == null) {
            throw new IllegalStateException("Canoe preset popup was opened without a module handler");
        }
        presetHandler.accept(preset);
        closeWindow(event);
    }
}
