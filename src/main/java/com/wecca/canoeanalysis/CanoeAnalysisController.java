package com.wecca.canoeanalysis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

public class CanoeAnalysisController implements Initializable {
    @FXML
    private ListView<String> loadList;
    @FXML
    private Button solveSystemButton, pointLoadButton, uniformLoadButton, setCanoeLengthButton, generateGraphsButton;
    @FXML
    private TextField pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
            distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField;
    @FXML
    private ComboBox<String> pointDirectionComboBox, pointMagnitudeComboBox, pointLocationComboBox, distributedIntervalComboBox,
            distributedDirectionComboBox, distributedMagnitudeComboBox, canoeLengthComboBox;
    @FXML
    private RadioButton standsRadioButton, floatingRadioButton;
    @FXML
    private ImageView beamImageView;
    @FXML
    private AnchorPane lowerLeftAnchorPane, lowerRightAnchorPane;

    private Canoe canoe;

    private final double E = 0.3048; // conversion factor ft to m
    private final double F = 0.45359237; // conversion factor lb to kg
    private final double G = 9.80665; // gravity on earth

    private boolean canoeLengthSet = false; // update to prevent resetting canoe length


    // Set all buttons to a ToggleGroup, with a default selected button
    public void setAllToggleGroup(ToggleGroup t, RadioButton[] r, int i) {
        for (RadioButton b : r) {
            b.setToggleGroup(t);
        }

        r[i].setSelected(true);
    }

    // ComboBox set items with a default selected item
    public <T> void setAllWithDefault(ComboBox<T> c, T[] a, int i) {
        c.setItems(FXCollections.observableArrayList(a));
        c.getSelectionModel().select(i);
    }

    // Checks if a string can be converted into a double
    public boolean validateTextAsDouble(String s)
    {
        try
        {
            double d = Double.parseDouble(s);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void setCanoeLength()
    {
        // Can only set the canoe length once (avoids forces off of the canoe)
        if (!canoeLengthSet)
        {
            canoe.setLen(getDistanceConverted(canoeLengthComboBox, canoeLengthTextField));
            canoeLengthSet = true;
        }
    }


    // Convert entered location value to m (assumes already validated as double)
    public double getDistanceConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        if (unit == "m") {return d;}
        else {return d * E;}
    }

    // Convert entered value to kN or kN/m (assumes already validated as double)
    public double getLoadConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        if (unit == "kN" || unit == "kN/m") {return d;}
        else if (unit == "N" || unit == "N/m") {return d / 1000.0;}
        else if (unit == "kg" || unit == "kg/m") {return (d * G) / 1000.0;}
        else if (unit == "lb") {return (d * F * G) / 1000.0;}
        else {return (d * F * G) / (1000.0 * E);} // lb/ft option
    }

    public void updateLoadList()
    {
        // Clear current ListView
        loadList.getItems().clear();

        // Update the ListView from the loads on the canoe
        for (PointLoad p : canoe.getPLoads())
        {
            loadList.getItems().add(p.toString());
        }

        // Update the ListView from the loads on the canoe
        for (UniformDistributedLoad d : canoe.getDLoads())
        {
            loadList.getItems().add(d.toString());
        }
    }

    public void addPointLoad()
    {
        // Validate the entered numbers are doubles
        if (validateTextAsDouble(pointLocationTextField.getText())
        && validateTextAsDouble(pointMagnitudeTextField.getText()))
        {
            double x = getDistanceConverted(pointLocationComboBox, pointLocationTextField);
            double mag = getLoadConverted(pointMagnitudeComboBox, pointMagnitudeTextField);
            String direction = pointDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (direction == "Down") {mag *= -1;}


            // Validate the load is being added within the length of the canoe
            if (0 <= x && x <= canoe.getLen())
            {
                // Add the load to canoe
                canoe.addPLoad(new PointLoad(mag, x));
            }
        }

        updateLoadList();
    }

    public void addDistributedLoad()
    {
        // Validate the entered numbers are doubles
        if (validateTextAsDouble(distributedMagnitudeTextField.getText())
                && validateTextAsDouble(distributedIntervalTextFieldL.getText())
                    && validateTextAsDouble(distributedIntervalTextFieldR.getText()))
        {
            double l = getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldL);
            double r = getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldR);
            double mag = getLoadConverted(distributedMagnitudeComboBox, distributedMagnitudeTextField);
            String direction = distributedDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (direction == "Down") {mag *= -1;}

            // Validate the load is being added within the length of the canoe
            // Validate the right bound of the interval is greater than the left
            if (0 <= l && l <= canoe.getLen() && r <= canoe.getLen() && r > l)
            {
                // Add the load to canoe
                canoe.addDLoad(new UniformDistributedLoad(l, r, mag));
            }
        }

        updateLoadList();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Instantiate the canoe
        ArrayList<PointLoad> p = new ArrayList<>();
        ArrayList<UniformDistributedLoad> d = new ArrayList<>();
        canoe = new Canoe(0, p, d);

        // Set Black Borders
        loadList.setStyle("-fx-border-color: black");
        lowerRightAnchorPane.setStyle("-fx-border-color: black");

        // Loading Images
        Image beamImage = new Image("file:src/main/resources/com/wecca/canoeanalysis/beam.png");
        beamImageView.setImage(beamImage);

        // Setting RadioButton Toggle Group
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{floatingRadioButton, standsRadioButton};
        setAllToggleGroup(canoeSupportToggleGroup, canoeSupportRButtons, 0);

        // Populate ComboBoxes
        String[] directions = new String[]{"Up", "Down"};
        String[] loadUnits = new String[]{"kN", "N", "kg", "lb"};
        String[] distanceUnits = new String[]{"m", "ft"};
        String[] distributedLoadUnits = new String[]{"kN/m", "N/m", "kg/m", "lb/ft"};

        setAllWithDefault(pointDirectionComboBox, directions, 0);
        setAllWithDefault(pointMagnitudeComboBox, loadUnits, 0);
        setAllWithDefault(pointLocationComboBox, distanceUnits, 0);
        setAllWithDefault(distributedIntervalComboBox, distanceUnits, 0);
        setAllWithDefault(distributedDirectionComboBox, directions, 0);
        setAllWithDefault(distributedMagnitudeComboBox, distributedLoadUnits, 0);
        setAllWithDefault(canoeLengthComboBox, distanceUnits, 0);

        // Populate TextFields with default values
        TextField[] tfs = new TextField[]{pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
                distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField};
        for (TextField tf : tfs) {tf.setText("0.00");}
    }
}