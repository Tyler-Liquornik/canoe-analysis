package com.wecca.canoeanalysis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class CanoeAnalysisController implements Initializable
{
    @FXML
    private TextField pointLoadMagnitudeTextField, pointLoadLocationTextField, uniformLoadMagnitudeTextField,
            uniformLoadIntervalTextFieldL, uniformLoadIntervalTextFieldR, canoeLengthTextField;
    @FXML
    private ComboBox<String> pointDirectionComboBox, pointMagnitudeComboBox, pointLocationComboBox, distributedIntervalComboBox,
            distributedDirectionComboBox, distributedMagnitudeComboBox, canoeLengthComboBox;
    @FXML
    private RadioButton standsRadioButton, floatingRadioButton;
    @FXML
    private ImageView beamImageView;
    @FXML
    private AnchorPane lowerLeftAnchorPane, lowerRightAnchorPane;


    // Set all buttons to a ToggleGroup, with a default selected button
    public void setAllToggleGroup(ToggleGroup t, RadioButton[] r, int i)
    {
        for (RadioButton b : r)
        {
            b.setToggleGroup(t);
        }
        
        r[i].setSelected(true);
    }

    // ComboBox set items with a default selected item
    public <T> void setAllWithDefault (ComboBox<T> c, T[] a, int i)
    {
        c.setItems(FXCollections.observableArrayList(a));
        c.getSelectionModel().select(i);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Bottom Pane Borders
        lowerRightAnchorPane.setStyle("-fx-border-color: black");
        lowerLeftAnchorPane.setStyle("-fx-border-color: black");

        // Loading Images
        Image beamImage = new Image("file:src/main/resources/com/wecca/canoeanalysis/beam.png");
        beamImageView.setImage(beamImage);

        // Setting RadioButton Toggle Group
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[] {floatingRadioButton, standsRadioButton};
        setAllToggleGroup(canoeSupportToggleGroup, canoeSupportRButtons, 0);

        // Populate ComboBoxes
        String[] directions = new String[] {"Up", "Down"};
        String[] loadUnits = new String[] {"kN", "N", "kg", "lb"};
        String[] distanceUnits = new String[] {"m", "ft"};
        String[] distributedLoadUnits = new String[] {"kN/m", "N/m", "kg/m", "lb/ft"};

        setAllWithDefault(pointDirectionComboBox, directions, 0);
        setAllWithDefault(pointMagnitudeComboBox, loadUnits, 0);
        setAllWithDefault(pointLocationComboBox, distanceUnits, 0);
        setAllWithDefault(distributedIntervalComboBox, distanceUnits, 0);
        setAllWithDefault(distributedDirectionComboBox, directions, 0);
        setAllWithDefault(distributedMagnitudeComboBox, distributedLoadUnits, 0);
        setAllWithDefault(canoeLengthComboBox, distanceUnits, 0);

        // Populate TextFields with default values
        String zero = String.format("%.2f", 0.0);

        TextField[] tfs = new TextField[] {pointLoadMagnitudeTextField, pointLoadLocationTextField, uniformLoadMagnitudeTextField,
                uniformLoadIntervalTextFieldL, uniformLoadIntervalTextFieldR, canoeLengthTextField};

        for (TextField t : tfs)
        {
            t.setText(zero);
        }
    }
}