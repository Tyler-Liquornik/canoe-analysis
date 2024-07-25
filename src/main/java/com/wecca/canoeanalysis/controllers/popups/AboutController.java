package com.wecca.canoeanalysis.controllers.popups;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    public AnchorPane imagePane;

    public void openLink(String link) {
        try {
            ProcessBuilder processBuilder;
            if (ResourceManagerService.isRunningFromWindows())
                processBuilder = new ProcessBuilder("cmd", "/c", "start", link);
            else if (ResourceManagerService.isRunningFromMac())
                processBuilder = new ProcessBuilder("open", link);
            else
                throw new UnsupportedOperationException("Unsupported operating system");
            processBuilder.start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void redirectLinkedin()
    {openLink("https://www.linkedin.com/in/tyler-liquornik/");}

    public void redirectGithub() {openLink("https://github.com/Tyler-Liquornik/canoe-analysis");}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        JFXDepthManager.setDepth(imagePane, 5);
    }
}
