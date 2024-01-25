package com.wecca.canoeanalysis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

public class CanoeAnalysisController implements Initializable
{
    @FXML
    private Label lengthLabelL, lengthLabelRTemp, lengthLabelR;
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
    private AnchorPane lowerRightAnchorPane, beamContainer;

    private Canoe canoe;

    private final double E = 0.3048; // conversion factor ft to m
    private final double F = 0.45359237; // conversion factor lb to kg
    private final double G = 9.80665; // gravity on earth

    private final double[] acceptedMagRange = new double[] {0.05, 10}; // Acceptable magnitude range (kN)
    private final int[] acceptedArrowHeightRange = new int[] {14, 84}; // Acceptable arrow height range (px)
    // Cannot get this from imageView, it hasn't been instantiated until initialize is called
    // Is there a workaround to this that doesn't require adding the imageView manually in code
    // Also this is awkward to add it in with all the fields at the top

    private boolean canoeLengthSet = false; // update to prevent resetting canoe length


    // Set all buttons to a ToggleGroup, with a default selected button
    public void setAllToggleGroup(ToggleGroup t, RadioButton[] r, int i)
    {
        for (RadioButton b : r) {
            b.setToggleGroup(t);
        }

        r[i].setSelected(true);
    }

    // ComboBox default item showing (used on initialization)
    public void setAllWithDefault(ComboBox<String> c, String[] a, int i)
    {
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

    // With only point loads, the order of the ListView items matches insertion order
    // This is not the case with uniformly distributed loads right now
    // This will also complicate this method as more arrows are introduced

    // Highlight the arrow selected on the ListView red
    public void highlightArrow()
    {
        // Add 1 as the first child of beamContainer is the imageview for beam.png
        int selectedIndex = loadList.getSelectionModel().getSelectedIndex() + 1;
        Arrow selected = (Arrow) beamContainer.getChildren().get(selectedIndex);

        // Make the selected arrow red
        selected.setFill(Color.RED);

        for (int i = 1; i < beamContainer.getChildren().size(); i++)
        {
            // Don't repaint the selected arrow black
            if (i != selectedIndex)
            {
                ((Arrow) beamContainer.getChildren().get(i)).setFill(Color.BLACK);
            }
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

        // Change the label on the scale
        lengthLabelRTemp.setText("");
        lengthLabelR.setText(String.format("%.2f m", canoe.getLen()));

        // Disable the length text field (can only set length once)
        canoeLengthTextField.setText("");
        canoeLengthTextField.setDisable(true);
    }


    // Convert entered location value to m (assumes already validated as double)
    // Todo: validate in the method, return some flag if invalid -> need to refactor some of the dependent code when done
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

    // Rescale arrows, max magnitude gets the max height, all others scale down to size
    public void rescaleArrowsFromMax(double maxMag)
    {
        // Clear beam container of all arrows (index 1 is the imageview, gets skipped)
        beamContainer.getChildren().subList(1, beamContainer.getChildren().size()).clear();

        // Find max magnitude load in list of loads
        int maxIndex = 0; boolean chosenMax = false; // If there's a tie, don't choose another max
        for (int i = 0; i < canoe.getPLoads().size(); i++)
        {
            PointLoad p = canoe.getPLoads().get(i);

            // Found a new max, get its index to reference later (avoids issues with 2 equal maxes)
            if (Math.abs(p.getMag()) == maxMag && !chosenMax)
            {
                maxIndex = i;
                chosenMax = true;

                // Render the max at max size
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

                Arrow arrow = new Arrow(p.getXScaled(beamContainer.getWidth(), canoe.getLen()), startY, p.getXScaled(beamContainer.getWidth(), canoe.getLen()), endY);
                beamContainer.getChildren().add(arrow);
            }
        }

        // Render all forces not marked as the max scaled to size
        for (int i = 0; i < canoe.getPLoads().size(); i++)
        {
            PointLoad p = canoe.getPLoads().get(i);

            // Skip the max, already been dealt with
            if (i != maxIndex)
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(p.getMag()) / maxMag));
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight() + deltaY;
                beamContainer.getChildren().add(new Arrow(p.getXScaled(beamContainer.getWidth(), canoe.getLen()), startY, p.getXScaled(beamContainer.getWidth(), canoe.getLen()), endY));
            }
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


            // Validate the load is being added within the length of the canoe, and is of acceptable magnitude range
            if (0 <= x && x <= canoe.getLen() && acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1])
            {
                // Add the load to canoe, and the load arrow on the GUI
                PointLoad p = new PointLoad(mag, x);
                canoe.addPLoad(p);

                // x coordinate in beamContainer for load arrow
                double scaledX = p.getXScaled(beamContainer.getWidth(), canoe.getLen()); // x position in the beamContainer

                // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
                int endY = mag < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

                // First load, arrow max height
                if (canoe.getPLoads().size() < 2)
                {
                    int startY = mag < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
                    Arrow arrow = new Arrow(scaledX, startY, scaledX, endY);
                    beamContainer.getChildren().add(arrow);
                }

                else
                {
                    // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
                    if (!(canoe.getMaxPLoad() / canoe.getMinPLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
                    {
                        rescaleArrowsFromMax(canoe.getMaxPLoad());
                    }

                    else
                    {
                        // currently: after getting here and trying to add another load you can't add any more arrows
                    }
                }


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

                // Validate the load is being added within the length of the canoe, and the magnitude is in the acceptable range
                // Validate the right bound of the interval is greater than the left
                if (0 <= l && l <= canoe.getLen() && r <= canoe.getLen() && r > l
                        && acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1])
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
        String[] directions = new String[]{"Down", "Up"};
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