package com.wecca.canoeanalysis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    private RadioButton standsRadioButton, floatingRadioButton, submergedRadioButton;
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

    // Highlight the load selected on the ListView red
    public void highlightLoad()
    {
        // Add 1 as the first child of beamContainer is the imageview for beam.png
        int selectedIndex = loadList.getSelectionModel().getSelectedIndex() + 1;

        for (int i = 1; i < beamContainer.getChildren().size(); i++)
        {
            // Paint all but the selected load black
            if (i !=selectedIndex)
            {
                // Deal with pLoads and dLoads separately
                if (i < canoe.getPLoads().size() + 1)
                    ((Arrow) beamContainer.getChildren().get(i)).setFill(Color.BLACK);
                else
                {
                    ((ArrowBox) beamContainer.getChildren().get(i)).getLArrow().setFill(Color.BLACK);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getRArrow().setFill(Color.BLACK);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getBorderLine().setStroke(Color.BLACK);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getBox().setFill(Color.LIGHTGREY);
                }
            }

            // Paint the selected load red
            else
            {
                if (i < canoe.getPLoads().size() + 1)
                    ((Arrow) beamContainer.getChildren().get(selectedIndex)).setFill(Color.RED);
                else
                {
                    ((ArrowBox) beamContainer.getChildren().get(i)).getLArrow().setFill(Color.RED);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getRArrow().setFill(Color.RED);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getBorderLine().setStroke(Color.RED);
                    ((ArrowBox) beamContainer.getChildren().get(i)).getBox().setFill(Color.LIGHTPINK);
                }
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

    // Rescale Arrows and ArrowBoxes, max magnitude gets the max height, all others scale down to size
    public void rescaleFromMax(double maxMag)
    {
        // Clear beam container of all arrows (index 1 is the imageview, gets skipped)
        beamContainer.getChildren().subList(1, beamContainer.getChildren().size()).clear();

        // Find max magnitude load in list of loads
        int maxPIndex = 0; boolean chosenPMax = false; // If there's a tie, don't choose another max
        for (int i = 0; i < canoe.getPLoads().size(); i++)
        {
            PointLoad p = canoe.getPLoads().get(i);

            // Found a new max, get its index to reference later (avoids issues with 2 equal maxes)
            if (Math.abs(p.getMag()) == maxMag && !chosenPMax)
            {
                maxPIndex = i;
                chosenPMax = true;
            }
        }

        int maxDIndex = 0; boolean chosenDMax = false;
        for (int i = 0; i < canoe.getDLoads().size(); i++)
        {
            UniformDistributedLoad d = canoe.getDLoads().get(i);

            // Found a new max, get its index to reference later (avoids issues with 2 equal maxes)
            if (Math.abs(d.getW()) == maxMag && !chosenDMax)
            {
                // Adjustment factor because pLoads coming before dLoads in the ListView
                maxDIndex = i;
                chosenDMax = true;
            }
        }

        // Max load between pLoads ands dLoads, dLoads index adjustment factor as dLoads come after pLoads as ListView items
        int maxIndex = 0;
        if (canoe.getPLoads().size() > 0 && canoe.getDLoads().size() > 0)
            maxIndex = canoe.getMaxPLoad() > canoe.getMaxDLoad() ? maxPIndex : maxDIndex + canoe.getPLoads().size();
        else if (canoe.getPLoads().size() == 0)
            maxIndex = maxDIndex + canoe.getPLoads().size();
        else if (canoe.getDLoads().size() == 0)
            maxIndex = maxPIndex;

        // List of Arrows and ArrowBoxes
        ArrayList<Arrow> arrowList = new ArrayList<>();
        ArrayList<ArrowBox> arrowBoxList = new ArrayList<>();

        // Render all arrows not marked as the max scaled to size
        for (int i = 0; i < canoe.getPLoads().size(); i++)
        {
            PointLoad p = canoe.getPLoads().get(i);

            // Deal with the max separately
            if (i == maxIndex)
            {
                // Render the max at max size
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

                Arrow arrow = new Arrow(p.getXScaled(beamContainer.getWidth(), canoe.getLen()), startY, p.getXScaled(beamContainer.getWidth(), canoe.getLen()), endY);
                arrowList.add(arrow);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(p.getMag()) / maxMag));
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight() + deltaY;
                Arrow arrow = new Arrow(p.getXScaled(beamContainer.getWidth(), canoe.getLen()), startY, p.getXScaled(beamContainer.getWidth(), canoe.getLen()), endY);
                arrowList.add(arrow);
            }
        }

        // Render all arrowBoxes not marked as the max scaled to size, dLoads index adjustment factor as dLoads come after pLoads as ListView items
        for (int i = canoe.getPLoads().size(); i < canoe.getPLoads().size() + canoe.getDLoads().size(); i++)
        {
            UniformDistributedLoad d = canoe.getDLoads().get(i - canoe.getPLoads().size());

            // Deal with the max separately
            if (i == maxIndex)
            {
                // Render the max at max size
                int startY = d.getW() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
                int endY = d.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

                ArrowBox arrowBox = new ArrowBox(d.getLXScaled(beamContainer.getWidth(), canoe.getLen()), startY, d.getRXScaled(beamContainer.getWidth(), canoe.getLen()), endY);
                arrowBoxList.add(arrowBox);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = d.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(d.getW()) / maxMag));
                int startY = d.getW() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight() + deltaY;

                ArrowBox arrowBox = new ArrowBox(d.getLXScaled(beamContainer.getWidth(), canoe.getLen()), startY, d.getRXScaled(beamContainer.getWidth(), canoe.getLen()), endY);
                arrowBoxList.add(arrowBox);
            }
        }

        // Add sorted Arrows and ArrowBoxes to the beamContainer
        arrowList.sort(new ArrowComparator());
        arrowBoxList.sort(new ArrowBoxComparator());
        beamContainer.getChildren().addAll(arrowList);
        beamContainer.getChildren().addAll(arrowBoxList);
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

                // Only 1 load, always at max height
                if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2)
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
                        rescaleFromMax(canoe.getMaxPLoad());
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
                        UniformDistributedLoad d = new UniformDistributedLoad(l, r, mag);
                        canoe.addDLoad(new UniformDistributedLoad(l, r, mag));

                        // x coordinates of arrows in beamContainer for ArrowBox
                        double scaledLX = d.getLXScaled(beamContainer.getWidth(), canoe.getLen());
                        double scaledRX = d.getRXScaled(beamContainer.getWidth(), canoe.getLen());

                        // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
                        int endY = mag < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

                        // Only 1 load, always at max height
                        if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2)
                        {
                            int startY = mag < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
                            ArrowBox arrowBox = new ArrowBox(scaledLX, startY, scaledRX, endY);
                            beamContainer.getChildren().add(arrowBox);
                        }

                        else
                        {
                            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
                            if (!(canoe.getMaxDLoad() / canoe.getMinDLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
                            {
                                rescaleFromMax(canoe.getMaxDLoad());
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Instantiate the canoe
        ArrayList<PointLoad> pLoads = new ArrayList<>();
        ArrayList<UniformDistributedLoad> dLoads = new ArrayList<>();
        canoe = new Canoe(0, pLoads, dLoads);

        // Set Black Borders
        loadList.setStyle("-fx-border-color: black");
        lowerRightAnchorPane.setStyle("-fx-border-color: black");

        // Loading Images
        Image beamImage = new Image("file:src/main/resources/com/wecca/canoeanalysis/beam.png");
        beamImageView.setImage(beamImage);

        // Setting RadioButton Toggle Group
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{floatingRadioButton, standsRadioButton, submergedRadioButton};
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