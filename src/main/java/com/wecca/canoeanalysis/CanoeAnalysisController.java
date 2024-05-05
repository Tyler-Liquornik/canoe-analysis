package com.wecca.canoeanalysis;

import com.wecca.canoeanalysis.graphics.Arrow;
import com.wecca.canoeanalysis.graphics.ArrowBox;
import com.wecca.canoeanalysis.graphics.ArrowBoxComparator;
import com.wecca.canoeanalysis.graphics.ArrowComparator;
import com.wecca.canoeanalysis.diagrams.Diagram;
import com.wecca.canoeanalysis.diagrams.DiagramPoint;
import com.wecca.canoeanalysis.models.Canoe;
import com.wecca.canoeanalysis.models.PointLoad;
import com.wecca.canoeanalysis.models.UniformDistributedLoad;
import com.wecca.canoeanalysis.utility.AddPointLoadResult;
import com.wecca.canoeanalysis.utility.SystemSolver;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.net.URL;
import java.util.*;


/**
 * Primary controller for longitudinal analysis of a beam
 */
public class CanoeAnalysisController implements Initializable
{
    @FXML
    private Label lengthLabelL, lengthLabelRTemp, lengthLabelR, lengthAlertLabel, boundsAlertLabel, magnitudeAlertLabel,
            intervalAlertLabel, distributedMagnitudeAlertLabel, combinedAlertLabel, failedDoubleParseAlertLabel;
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

    private Canoe canoe; // entity class that models the canoe as a beam

    private final double FEET_TO_METRES = 0.3048; // conversion factor ft to m
    private final double POUNDS_TO_KG = 0.45359237; // conversion factor lb to kg
    private final double GRAVITY = 9.80665; // gravity on earth

    private final double[] acceptedMagRange = new double[] {0.05, 10}; // Acceptable magnitude range (kN)

    private final double[] acceptedLengthRange = new double[] {0.05, 20}; // Acceptable canoe length range (m)
    private final int[] acceptedArrowHeightRange = new int[] {14, 84}; // Acceptable arrow height range (px)
    // Cannot get this from imageView, it hasn't been instantiated until initialize is called
    // Is there a workaround to this that doesn't require adding the imageView manually in code
    // Also this is awkward to add it in with all the fields at the top

    private final double adjFactor = 5; // beamImageView not at x = 5 in beamContainer

    /**
     * Round a double to x digits.
     * @param num the number to round.
     * @param numDigits the number of digits to round to.
     * @return the rounded double.
     */
    public static double roundXDigits(double num, int numDigits) {
        double factor = Math.pow(10, numDigits);
        return Math.round(num * factor) / factor;
    }


    /**
     * Put a group of radio buttons into a toggle group (only allow one to be selected at a time)
     * Have one of the buttons be selected by default
     * @param group for the buttons to be added to
     * @param buttons the radio buttons to add to the group
     * @param selectedIndex the index in buttons to be selected on initialization
     */
    public void setAllToggleGroup(ToggleGroup group, RadioButton[] buttons, int selectedIndex)
    {
        for (RadioButton b : buttons) {
            b.setToggleGroup(group);
        }

        buttons[selectedIndex].setSelected(true);
    }

    /**
     * Populate a combo box and set a default item to show
     * @param comboBox the combo box to populate
     * @param options the list of options to populate the combo box with
     * @param selectedIndex the index in the list of options to display on initialization
     */
    public void setAllWithDefault(ComboBox<String> comboBox, String[] options, int selectedIndex)
    {
        comboBox.setItems(FXCollections.observableArrayList(options));
        comboBox.getSelectionModel().select(selectedIndex);
    }

    /**
     * Checks if a string can be parsed as a double
     * @param s the string to be checked
     * @return whether the string is a stringified double
     */
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

    /**
     * Highlights the selected load red in the UI
     * Called by the list view of loads when a load is selected
     */
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

    /**
     * Called by the "Set Length" button
     * Updates both the model and the UI, showing the length of the canoe
     */
    public void setCanoeLength()
    {
        // Convert to metric
        double len = getDistanceConverted(canoeLengthComboBox, canoeLengthTextField);

        // Only allow lengths in the specified range
        if (len >= acceptedLengthRange[0] && len <= acceptedLengthRange[1])
        {
            // Update the canoe model
            canoe.setLen(len);

            // Change the label on the scale
            lengthLabelRTemp.setText("");
            lengthLabelR.setText(String.format("%.2f m", canoe.getLen()));

            // Clear potential alert and reset access to controls
            lengthAlertLabel.setText("");
            disableInitialize(false);
            canoeLengthTextField.setDisable(true);
            canoeLengthComboBox.setDisable(true);
            setCanoeLengthButton.setDisable(true);
        }
        // Populate the alert telling the user the length they've entered is out of the allowed range
        else
        {
            lengthAlertLabel.setText("Length must be between 0.05m and 20m");
        }
    }


    /**
     * Convert the distance in the text field to m from the unit selected in the combo box
     * Assumes the value in the text field already validated as double
     * @param c the combo box with the selected unit to convert from
     * @param t the text field with the value to convert
     * @return the value converted to m
     */
    public double getDistanceConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        if (Objects.equals(unit, "m")) {return d;}
        else {return d * FEET_TO_METRES;}
    }

    /**
     * Convert the load in the text field to kN or kN/m from the unit selected in the combo box
     * Assumes the value in the text field already validated as double
     * @param c the combo box with the selected unit to convert from
     * @param t the text field with the value to convert
     * @return the value converted to kN (point load) or kN/m (distributed load)
     */
    public double getLoadConverted(ComboBox<String> c, TextField t)
    {
        String unit = c.getSelectionModel().getSelectedItem();
        double d = Double.parseDouble(t.getText());

        if (Objects.equals(unit, "kN") || Objects.equals(unit, "kN/m")) {return d;}
        else if (Objects.equals(unit, "N") || Objects.equals(unit, "N/m")) {return d / 1000.0;}
        else if (Objects.equals(unit, "kg") || Objects.equals(unit, "kg/m")) {return (d * GRAVITY) / 1000.0;}
        else if (Objects.equals(unit, "lb")) {return (d * POUNDS_TO_KG * GRAVITY) / 1000.0;}
        else {return (d * POUNDS_TO_KG * GRAVITY) / (1000.0 * FEET_TO_METRES);} // lb/ft option
    }

    /**
     * Update the ListView displaying loads to the user to match the canoe model
     */
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

    /**
     * Rescales all point loads (arrows) and distributed loads (arrow boxes) based on the load with the highest mag.
     * Point and distributed load magnitudes are compared although their units differ (Force vs. Force / Length)
     * @param maxMag the maximum magnitude which all other loads are scaled down based off of
     */
    public void rescaleFromMax(double maxMag)
    {
        // Clear beam container of all arrows (index 0 is the imageview, gets skipped)
        beamContainer.getChildren().subList(1, beamContainer.getChildren().size()).clear();

        // Find max magnitude pLoad in list of pLoads
        int maxPIndex = 0;
        for (int i = 0; i < canoe.getPLoads().size(); i++)
        {
            PointLoad p = canoe.getPLoads().get(i);

            // Found a new max, get its index to reference later (avoids issues with 2 equal maxes)
            if (Math.abs(p.getMag()) == maxMag)
            {
                maxPIndex = i;
                break;
            }
        }

        // Find max magnitude dLoad in list of dLoads
        int maxDIndex = 0;
        for (int i = 0; i < canoe.getDLoads().size(); i++)
        {
            UniformDistributedLoad d = canoe.getDLoads().get(i);

            // Found a new max, get its index to reference later (avoids issues with 2 equal maxes)
            if (Math.abs(d.getW()) == maxMag)
            {
                // Adjustment factor because pLoads coming before dLoads in the ListView
                maxDIndex = i;
                break;
            }
        }

        // Max load between pLoads ands dLoads, dLoads index adjustment factor as dLoads come after pLoads as ListView items
        int maxIndex;
        if (canoe.getPLoads().size() > 0 && canoe.getDLoads().size() > 0)
            maxIndex = canoe.getMaxPLoad() > canoe.getMaxDLoad() ? maxPIndex : maxDIndex + canoe.getPLoads().size();
        else if (canoe.getPLoads().size() == 0)
            maxIndex = maxDIndex;
        else
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

                Arrow arrow = new Arrow(p.getXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, startY, p.getXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, endY);
                arrowList.add(arrow);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(p.getMag()) / maxMag));
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight() + deltaY;
                Arrow arrow = new Arrow(p.getXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, startY, p.getXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, endY);
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

                ArrowBox arrowBox = new ArrowBox(d.getLXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, startY, d.getRXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, endY);
                arrowBoxList.add(arrowBox);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = d.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(d.getW()) / maxMag));
                int startY = d.getW() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight() + deltaY;

                ArrowBox arrowBox = new ArrowBox(d.getLXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, startY, d.getRXScaled(beamImageView.getFitWidth(), canoe.getLen()) + adjFactor, endY);
                arrowBoxList.add(arrowBox);
            }
        }

        // Add sorted Arrows and ArrowBoxes to the beamContainer
        arrowList.sort(new ArrowComparator());
        arrowBoxList.sort(new ArrowBoxComparator());
        beamContainer.getChildren().addAll(arrowList);
        beamContainer.getChildren().addAll(arrowBoxList);
    }

    /**
     * Called by the "Add Point Load" button
     * Deals with parsing and validation user input.
     * Main logic handled in addPointLoadToCanoe method
     */
    public void addPointLoad()
    {
        // Clear previous alert labels
        boundsAlertLabel.setText("");
        magnitudeAlertLabel.setText("");
        failedDoubleParseAlertLabel.setText("");

        // Validate the entered numbers are doubles
        if (allTextFieldsAreDouble(Arrays.asList(pointLocationTextField, pointMagnitudeTextField)))
        {
            double x = getDistanceConverted(pointLocationComboBox, pointLocationTextField);
            double mag = getLoadConverted(pointMagnitudeComboBox, pointMagnitudeTextField);
            String direction = pointDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}


            // Validate the load is being added within the length of the canoe
            if (!(0 <= x && x <= canoe.getLen()))
                boundsAlertLabel.setText("Load must be contained within the canoe's length");
            // Validate the load is in the accepted magnitude range
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                magnitudeAlertLabel.setText("Load must be between 0.05kN and 10kN");

            else
            {
                // Add the load to canoe, and the load arrow on the GUI
                PointLoad p = new PointLoad(mag, x);
                addPointLoadToCanoe(p);
                updateLoadList();
            }

        }
        else
            failedDoubleParseAlertLabel.setText("One or more entered values are not numbers");
    }

    /**
     * Called by the "Add Uniformly Distributed Load" button
     * Deals with parsing and validation user input.
     * Main logic handled in addDistributedLoadToCanoe method
     */
    public void addDistributedLoad()
    {
        // Clear previous alert labels
        boundsAlertLabel.setText("");
        intervalAlertLabel.setText("");
        distributedMagnitudeAlertLabel.setText("");
        failedDoubleParseAlertLabel.setText("");

        // Validate the entered numbers are doubles
        if (allTextFieldsAreDouble(Arrays.asList(distributedMagnitudeTextField, distributedIntervalTextFieldL,
                distributedIntervalTextFieldR)))
        {
            double l = getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldL);
            double r = getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldR);
            double mag = getLoadConverted(distributedMagnitudeComboBox, distributedMagnitudeTextField);
            String direction = distributedDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}

            // User entry validations
            if (!(0 <= l && r <= canoe.getLen()))
                boundsAlertLabel.setText("Load must be contained within the canoe's length");
            else if (!(r > l))
                intervalAlertLabel.setText("Right interval bound must be greater than the left bound");
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                distributedMagnitudeAlertLabel.setText("Load must be between 0.05kN/m and 10kN/m");

            else
                {
                    // Add the load to canoe, and update the ListView
                    UniformDistributedLoad d = new UniformDistributedLoad(l, r, mag);
                    addDistributedLoadToCanoe(d);
                    updateLoadList();
                }
        }
        else
            failedDoubleParseAlertLabel.setText("One or more entered values are not numbers");

    }

    /**
     * Add a distributed load to the canoe object and JavaFX UI.
     * This method was extracted to allow for reuse in system solver methods.
     * @param dLoad the distributed load to be added.
     */
    private void addDistributedLoadToCanoe(UniformDistributedLoad dLoad) {
        // Label reset
        combinedAlertLabel.setText("");

        canoe.addDLoad(dLoad);

        // x coordinates of arrows in beamContainer for ArrowBox
        double scaledLX = dLoad.getLXScaled(beamImageView.getFitWidth(), canoe.getLen());
        double scaledRX = dLoad.getRXScaled(beamImageView.getFitWidth(), canoe.getLen());

        // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
        int endY = dLoad.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

        // Only 1 load, always at max height
        if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2)
        {
            int startY = dLoad.getW() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
            ArrowBox arrowBox = new ArrowBox(scaledLX + adjFactor, startY, scaledRX + adjFactor, endY);
            beamContainer.getChildren().add(arrowBox);
        }

        else
        {
            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
            if (!(canoe.getMaxDLoad() / canoe.getMinDLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
            {
                rescaleFromMax(Math.max(canoe.getMaxPLoad(), canoe.getMaxDLoad()));
            }

            else
            {
                // TODO
                // currently: after getting here and trying to add another load you can't add any more arrows
            }
        }
    }

    /**
     * Add a point load to the canoe object and JavaFX UI.
     * This method was extracted to allow for reuse in system solver methods.
     * @param pLoad the point load to be added.
     */
    private void addPointLoadToCanoe(PointLoad pLoad) {
        AddPointLoadResult addResult = canoe.addPLoad(pLoad);

        // x coordinate in beamContainer for load arrow
        double scaledX = pLoad.getXScaled(beamImageView.getFitWidth(), canoe.getLen()); // x position in the beamContainer

        // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
        int endY = pLoad.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); //126

        // Notify the user regarding point loads combining or cancelling
        combinedAlertLabel.setText("");
        if (addResult == AddPointLoadResult.COMBINED)
            combinedAlertLabel.setText("Point load magnitudes combined");
        else if (addResult == AddPointLoadResult.REMOVED)
            combinedAlertLabel.setText("Point load magnitudes cancelled");

        // Only 1 load, always at max height
        if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2 && addResult != AddPointLoadResult.REMOVED)
        {
            int startY = pLoad.getMag() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beamImageView.getFitHeight(); // 196
            Arrow arrow = new Arrow(scaledX + adjFactor, startY, scaledX + adjFactor, endY);
            beamContainer.getChildren().add(arrow);
        }

        else
        {
            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
            if (!(canoe.getMaxPLoad() / canoe.getMinPLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
            {
                rescaleFromMax(Math.max(canoe.getMaxPLoad(), canoe.getMaxDLoad()));
            }

            else
            {
                // TODO: throw an error
                // currently: after getting here and trying to add another load you can't add any more arrows
            }
        }
    }

    /**
     * Check if a list of text fields all contain double values.
     * @param fields list of text fields to check.
     * @return whether each text field contains a double value.
     */
    private boolean allTextFieldsAreDouble(List<TextField> fields) {
        for (TextField field : fields) {
            if (!validateTextAsDouble(field.getText())) return false;
        }
        return true;
    }

    /**
     * Handler for the "solve system" button.
     * Distributes call to appropriate method and re-renders loads.
     */
    public void solveSystem() {

        generateGraphsButton.setDisable(false);

        if (standsRadioButton.isSelected()) {
            solveStandSystem();
        } else if (floatingRadioButton.isSelected()) {
            // Solve floating case
        } else if (submergedRadioButton.isSelected()) {
            // Solve submerged case
        } else {
            // throw error
        }

        updateLoadList(); // TODO: change to support icons
    }

    /**
     * Set up the canvas/pane for a diagram.
     * @param points the points to render on the diagram.
     * @param title the title of the diagram.
     * @param yUnits the units of the y-axis on the diagram.
     * @return a list of series where each series is a section of the overall piecewise function
     */
    private List<XYChart.Series> setupDiagram(List<DiagramPoint> points, String title, String yUnits)
    {
        // Initializing the stage and main pane
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        Pane chartPane = new Pane();
        chartPane.setPrefSize(1125, 750);

        // Adding Logo Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/canoe.png");
        popupStage.getIcons().add(icon);

        // Setting the axes of the chart
        NumberAxis yAxis = new NumberAxis();
        NumberAxis xAxis = new NumberAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLabel("Distance [m]");
        yAxis.setLabel(yUnits);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(canoe.getLen());

        // Creating and styling the line chart
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setPrefSize(1125, 750);
        chart.setLegendVisible(false);
        chart.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());

        // Adding the sections of the pseudo piecewise function separately
        boolean set = false; // only need to set the name of the series once since its really one piecewise function
        TreeSet<Double> criticalPoints = canoe.getSectionEndPoints();
        List<List<DiagramPoint>> intervals = partitionPoints(points, criticalPoints);
        List<XYChart.Series> intervalsAsSeries = new ArrayList<>();
        for (List<DiagramPoint> interval : intervals)
        {
            XYChart.Series series = new XYChart.Series();
            for (DiagramPoint point : interval)
            {
                series.getData().add(new XYChart.Data<>(point.getX(), point.getY()));

                System.out.println("Point added to chart " + title + ": " + point);
            }

            if (!set)
            {
                series.setName(yUnits);
                set = true;
            }
            series.setName(yUnits);
            chart.getData().add(series);
            intervalsAsSeries.add(series);
        }

        // Creating the scene and adding the chart to it
        chartPane.getChildren().add(chart);
        Scene scene = new Scene(chartPane, 1125, 750);
        popupStage.setScene(scene);
        popupStage.show();

        return intervalsAsSeries;
    }

    /**
     * Generates an SFD and BMD based on the canoe's load state.
     */
    public void generateDiagram()
    {
        // Testing
        for (DiagramPoint sfdPoint : Diagram.generateSfdPoints(canoe))
        {
            System.out.println("sfdPoint generated: " + sfdPoint);
        }

        setupDiagram(Diagram.generateSfdPoints(canoe), "Shear Force Diagram", "Force [kN]");

        // Testing
        for (DiagramPoint bmdPoint : Diagram.generateBmdPoints(canoe))
        {
            System.out.println("bmdPoint generated: " + bmdPoint);
        }

        setupDiagram(Diagram.generateBmdPoints(canoe), "Bending Moment Diagram", "Moment [kN·m]");
    }

    /**
     * Consider the list of points is a "pseudo piecewise function" (pseudo as discrete points are defined rather than a continuous function)
     * This method breaks it into a set of "pseudo functions"
     * @param points act together as a piecewise function
     * @param partitions the locations where the form of the piecewise changes
     * @return a set containing each section of the piecewise pseudo functions with unique form
     */
    private List<List<DiagramPoint>> partitionPoints(List<DiagramPoint> points, TreeSet<Double> partitions)
    {
        // For testing
        System.out.println("\nWorking with partitions:");
        for (double partitionPoint : partitions)
        {
            System.out.println("x = " + partitionPoint);
        }

        // Initializing lists
        List<List<DiagramPoint>> partitionedIntervals = new ArrayList<>();
        List<Double> partitionsList = new ArrayList<>(partitions);

        // If the first point is doubled up due to a jump discontinuity then throw away the first point (0, 0)
        // By "doubled up" I mean two points at the same x coordinate
        if (points.get(0).getX() == 0 && points.get(1).getX() == 0)
            points.remove(0);

        // Same idea for last two points if they double up
        if (points.get(points.size() - 1).getX() == canoe.getLen() && points.get(points.size() - 2).getX() == canoe.getLen())
            points.remove(points.size() - 1);

        // Remove zero from the partition list (always first as the TreeSet was sorted ascending)
        if (partitionsList.get(0) == 0)
            partitionsList.remove(0);

        // Keep track of intervals and points that partition them
        int partitionIndex = 0;
        List<DiagramPoint> interval = new ArrayList<>();

        // Put all the points into intervals
        for (int i = 0; i < points.size(); i++)
        {
            // Get the current point
            DiagramPoint point = points.get(i);

            // Keep adding points to the interval until the partition index reached
            // Empty interval means this is the first point to be included
            if (point.getX() != partitionsList.get(partitionIndex) || interval.isEmpty())
                interval.add(point);

            // Add the interval to the list of partitioned intervals and prepare for the next interval
            else
            {
                interval.add(point); // this is the partition point, which acts as the right endpoint of the interval

                System.out.println("Added partition point x = " + point.getX());

                // If not at the right boundary of the beam
                if (i != points.size() - 1)
                {
                    // If no jump discontinuity, create a duplicate point to act as the left endpoint of the next interval
                    if (point.getX() != points.get(i + 1).getX())
                        i--;
                }
                partitionIndex++;
                partitionedIntervals.add(new ArrayList<>(interval)); // add a copy of the interval to the intervals list
                interval.clear();
            }

        }

        return partitionedIntervals;
    }

    /**
     * Finds the coefficients of the quadratic curve ax^2 + bx + c from a list of 3 of its points
     * @param points the three points on the parabola, preferred to be the interval endpoints and critical points to reduce error due to the numerical nature of point generation
     * @return the coefficients [a, b, c]
     */
    public double[] getQuadraticCoefficients(DiagramPoint[] points)
    {
        double x1 = points[0].getX();
        double y1 = points[0].getY();
        double x2 = points[1].getX();
        double y2 = points[1].getY();
        double x3 = points[2].getX();
        double y3 = points[2].getY();

        double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
        double a    = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
        double b    = (x3*x3 * (y1 - y2) + x2*x2 * (y3 - y1) + x1*x1 * (y2 - y3)) / denom;
        double c     = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

        return new double[]{a, b, c};
    }

    /**
     * Finds the coefficients of the line mx + b from a list of 2 of its points
     * @param points the three points on the parabola, preferred to be the interval endpoints to reduce error due to the numerical nature of point generation
     * @return the coefficients [a, b]
     */
    public double[] getLinearCoefficients(DiagramPoint[] points)
    {
        double x1 = points[0].getX();
        double y1 = points[0].getY();
        double x2 = points[1].getX();
        double y2 = points[1].getY();

        double m = (y2 - y1) / (x2 - x1);
        double b = y1 - m * x1;

        return new double[] {m, b};
    }

    /**
     * Get the critical (max or min) value of a parabola on interval [l, r]
     * @param coefficients in the form [a, b, c] for parabola ax^2 + bx + c
     * @param l the left bound of the interval to check
     * @param r the right bound of the interval to check
     * @return the critical point on the interval as <X, Y>
     */
    public HashMap<Double, Double> getQuadraticCritical(double[] coefficients, double l, double r)
    {
        HashMap<Double, Double> critical = new HashMap<>();

        double a = coefficients[0];
        double b = coefficients[1];
        double c = coefficients[2];

        double xCritical = -b / (2 * a);
        double yCritical = c - (b * b) / (4 * a);

        critical.put(xCritical, yCritical);
        return critical;
    }

    /**
     * Get the max value of a line on interval [l, r]
     * @param coefficients in the form [m, c] for line mx + c
     * @param l the left bound of the interval to check
     * @param r the right bound of the interval to check
     * @return the maximum value on the interval as <X, Y>
     */
    public HashMap<Double, Double> getLinearMax(double[] coefficients, double l, double r)
    {
        HashMap<Double, Double> critical = new HashMap<>();

       double m = coefficients[0];
       double c = coefficients[1];
       double y1 = m * l + c;
       double y2 = m * r + c;

       if (y1 >= y2)
           critical.put(l, y1);
       else
           critical.put(r, y2);

       return critical;
    }

    /**
     * Solve and display the result of the "stand" system load case.
     * This entails two supports at each end of the canoe, symmetrically offset from the beam bounds
     * This interval is currently hardcoded as 0 in SystemSolver, so supports on beam endpoints
     */
    private void solveStandSystem()
    {
        List<PointLoad> newLoads = SystemSolver.solveStandSystem(canoe);
        for (PointLoad load : newLoads) {addPointLoadToCanoe(load);}
    }

    /**
     * Bulk package to toggle enabling all the controls related to loading
     * @param b disables controls
     */
    private void disableInitialize(boolean b)
    {
        solveSystemButton.setDisable(b);
        pointLoadButton.setDisable(b);
        uniformLoadButton.setDisable(b);
        floatingRadioButton.setDisable(b);
        standsRadioButton.setDisable(b);
        submergedRadioButton.setDisable(b);
        distributedMagnitudeComboBox.setDisable(b);
        distributedMagnitudeTextField.setDisable(b);
        pointMagnitudeComboBox.setDisable(b);
        pointMagnitudeTextField.setDisable(b);
        pointLocationTextField.setDisable(b);
        pointLocationComboBox.setDisable(b);
        pointDirectionComboBox.setDisable(b);
        distributedIntervalTextFieldL.setDisable(b);
        distributedIntervalTextFieldR.setDisable(b);
        distributedIntervalComboBox.setDisable(b);
        distributedDirectionComboBox.setDisable(b);
    }

    /**
     * Operations called on initialization of the view
     * @param url unused, part of javafx framework
     * @param resourceBundle unused, part of javafx framework
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Instantiate the canoe
        ArrayList<PointLoad> pLoads = new ArrayList<>();
        ArrayList<UniformDistributedLoad> dLoads = new ArrayList<>();
        canoe = Canoe.getInstance();

        // Disable most buttons on startup to prevent inputs in the wrong order
        disableInitialize(true);
        generateGraphsButton.setDisable(true);

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

        // Populate the TextFields with default values
        TextField[] tfs = new TextField[]{pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
                distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField};
        for (TextField tf : tfs) {tf.setText("0.00");}
    }
}