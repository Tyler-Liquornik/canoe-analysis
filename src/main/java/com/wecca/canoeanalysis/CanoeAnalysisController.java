package com.wecca.canoeanalysis;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class CanoeAnalysisController implements Initializable
{
    @FXML
    private ImageView beamImageView;
    @FXML
    private AnchorPane lowerLeftAnchorPane;
    @FXML
    private AnchorPane lowerRightAnchorPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        lowerRightAnchorPane.setStyle("-fx-border-color: black");
        lowerLeftAnchorPane.setStyle("-fx-border-color: black");

        Image beamImage = new Image("file:src/main/resources/com/wecca/canoeanalysis/beam.png");
        beamImageView.setImage(beamImage);
    }
}