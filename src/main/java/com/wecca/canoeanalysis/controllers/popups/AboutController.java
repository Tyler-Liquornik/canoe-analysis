package com.wecca.canoeanalysis.controllers.popups;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    // Keeping the public profile URLs in one place prevents the FXML handlers from
    // drifting to an outdated or misspelled address.
    public static final String TYLER_LINKEDIN_URL = "https://www.linkedin.com/in/tyler-liquornik/";
    public static final String PERRY_LINKEDIN_URL = "https://www.linkedin.com/in/perry-lycett-8b9272366/";
    public static final String PROJECT_GITHUB_URL = "https://github.com/Tyler-Liquornik/canoe-analysis";

    @FXML
    private AnchorPane tylerPane;

    @FXML
    private AnchorPane perryPane;

    /** Opens an external page without blocking the JavaFX application thread. */
    public void openLink(String link) {
        try {
            URI uri = URI.create(link);

            // Desktop.browse is the normal cross-platform route and launches the
            // user's configured default browser on both Windows and macOS.
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }

            // Some packaged runtimes do not expose Desktop browsing, so retain a
            // safe platform-specific fallback without waiting for it to close.
            ProcessBuilder processBuilder;
            if (ResourceManagerService.isRunningFromWindows()) {
                processBuilder = new ProcessBuilder(
                        "rundll32.exe", "url.dll,FileProtocolHandler", uri.toString());
            } else if (ResourceManagerService.isRunningFromMac()) {
                processBuilder = new ProcessBuilder("open", uri.toString());
            } else {
                throw new UnsupportedOperationException("Unsupported operating system");
            }
            processBuilder.start();
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void redirectTylerLinkedin() {
        openLink(TYLER_LINKEDIN_URL);
    }

    @FXML
    public void redirectPerryLinkedin() {
        openLink(PERRY_LINKEDIN_URL);
    }

    @FXML
    public void redirectGithub() {
        openLink(PROJECT_GITHUB_URL);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Match the raised-card treatment used throughout the rest of PADDL.
        JFXDepthManager.setDepth(tylerPane, 5);
        JFXDepthManager.setDepth(perryPane, 5);
    }
}
