package com.wecca.canoeanalysis.controllers;
import com.jfoenix.effects.JFXDepthManager;
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

    public void openLink(String link){
        try {
            Desktop.getDesktop().browse(new URI(link));
        } catch (IOException | URISyntaxException e) {
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
