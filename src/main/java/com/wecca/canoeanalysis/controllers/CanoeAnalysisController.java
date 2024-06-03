package com.wecca.canoeanalysis.controllers;

import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.diagrams.*;
import com.wecca.canoeanalysis.graphics.*;
import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.util.*;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.util.Duration;
import lombok.Setter;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Primary controller for longitudinal analysis of a beam
 */
public class CanoeAnalysisController implements Initializable
{
    @FXML
    private Label axisLabelR, lengthLabel, notificationLabel, pointDirectionLabel, pointMagnitudeLabel, pointLocationLabel,
            pointTitleLabel, supportTitleLabel, distributedDirectionLabel, distributedMagntiudeLabel,
            distributedIntervalLabel, distributedTitleLabel;
    @FXML
    private ListView<String> loadListView;

    @FXML
    private Button solveSystemButton, pointLoadButton, distributedLoadButton, setCanoeLengthButton, generateGraphsButton,
            clearLoadsButton, deleteLoadButton, hamburgerButton;
    @FXML
    private TextField pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
            distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField;
    @FXML
    private ComboBox<String> pointDirectionComboBox, pointMagnitudeComboBox, pointLocationComboBox, distributedIntervalComboBox,
            distributedDirectionComboBox, distributedMagnitudeComboBox, canoeLengthComboBox;
    @FXML
    private RadioButton standsRadioButton, floatingRadioButton, submergedRadioButton;
    @FXML
    private AnchorPane loadContainer, beamContainer;
    @FXML
    private JFXDrawer menuDrawer;

    private Canoe canoe; // entity class that models the canoe as a beam
    private Beam beam; // The graphic of the beam

    private final double FEET_TO_METRES = 0.3048; // conversion factor ft to m
    private final double POUNDS_TO_KG = 0.45359237; // conversion factor lb to kg
    private final double GRAVITY = 9.80665; // gravity on earth

    private final double[] acceptedMagRange = new double[] {0.05, 10}; // Acceptable magnitude range (kN)
    private final double[] acceptedLengthRange = new double[] {0.05, 20}; // Acceptable canoe length range (m)
    private final int[] acceptedArrowHeightRange = new int[] {14, 84}; // Acceptable arrow height range (px)
    // Cannot get this from imageView, it hasn't been instantiated until initialize is called
    // Is there a workaround to this that doesn't require adding the imageView manually in code
    // Also this is awkward to add it in with all the fields at the top

    // For draggable window with custom toolbar
    @Setter
    private static Stage primaryStage;
    private static Pane rootPane = primaryStage.get
    private static double xOffset = 0;
    private static double yOffset = 0;

    /**
     * Mouse pressed event handler to record the current mouse position
     * @param event triggers the method
     */
    public void draggableWindowGetLocation(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    /** Mouse dragged event handler to move the window
     * @param event triggers the method
     */
    public void draggableWindowMove(MouseEvent event)
    {
        if (primaryStage != null)
        {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        }
    }

    /**
     * Close the window when the "X" button is pressed
     */
    public void closeWindow() {primaryStage.close();}

    /**
     * Minimize the window the "-" button is pressed
     */
    public void minimizeWindow() {primaryStage.setIconified(true);}

//    public void toggleDrawer()
//    {
//        FadeTransition fadeIn = new FadeTransition(Duration.seconds(3), pane);
//        fadeIn.setFromValue(0);
//        fadeIn.setToValue(1);
//        fadeIn.setCycleCount(1);
//
//        FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), pane);
//        fadeOut.setFromValue(1);
//        fadeOut.setToValue(0);
//        fadeOut.setCycleCount(1);
//
//        fadeIn.play();
//
//        fadeIn.setOnFinished((e) -> fadeOut.play());
//
//        fadeOut.setOnFinished((e) -> {
//            try {
//                AnchorPane parentContent = FXMLLoader.load(getClass().getResource(("/genuinecoder/main/main.fxml")));
//                root.getChildren().setAll(parentContent);
//            } catch (IOException ignored) {}
//        });
//    }

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
     * Checks if a string can be parsed as a double.
     *
     * @param s the string to be checked
     * @return true if the string can be parsed as a double, false otherwise
     */
    public boolean validateTextAsDouble(String s) {
        if (s == null)
            return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
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
     * Toggles settings for empty load list
     * Includes a placeholder list item, enabled/disabled status of buttons, and styling
     */
    public void enableEmptyLoadListSettings(boolean enable) {

        // Load List not empty, toggle normal settings
        if (!enable)
        {
            // Apply settings
            loadListView.setStyle("-fx-font-weight: normal");
            loadListView.getItems().clear();
            deleteLoadButton.setDisable(false);
            clearLoadsButton.setDisable(false);
        }

        // Load list empty, toggle empty settings
        else
        {
            // Apply settings
            loadListView.setStyle("-fx-font-weight: bold");
            loadListView.getItems().clear();
            loadListView.getItems().add("View loads here");
            deleteLoadButton.setDisable(true);
            clearLoadsButton.setDisable(true);
        }
    }

    /**
     * Updates the view order (z-axis rendering) property of all graphics for rendering
     * Supports are above point loads which are above distributed loads as the preferred order
     */
    public void updateViewOrder()
    {
        // Create the list of current load graphics
        List<Node> loadContainerChildren = loadContainer.getChildren().stream().toList();

        // Apply the unique view order for each node based on type
        // Layering priority is SupportTriangles above ArrowBoxes above Arrows
        int viewOrder = Integer.MAX_VALUE;
        for (Node node : loadContainerChildren)
        {
            if (node instanceof ArrowBox)
                node.setViewOrder(viewOrder--);
        }
        for (Node node : loadContainerChildren)
        {
            if (node instanceof Arrow)
                node.setViewOrder(viewOrder--);

        }
        for (Node node : loadContainerChildren)
        {
            if (node instanceof SupportTriangle)
                node.setViewOrder(viewOrder--);
        }
    }

    /**
     * Highlights the selected load in the UI
     * Called by the list view of loads when a load is selected
     */
    public void highlightLoad()
    {
        int selectedIndex = loadListView.getSelectionModel().getSelectedIndex();

        updateViewOrder();

        // Color the selected load red and color the others black
        for (int i = 0; i < loadContainer.getChildren().size(); i++)
        {

            // Recolor the selected graphic
            Colorable colorableGraphic = (Colorable) loadContainer.getChildren().get(i);
            if (i != selectedIndex)
                colorableGraphic.recolor(ColorPalette.ICON.getColor());
            else
            {
                colorableGraphic.recolor(ColorPalette.PRIMARY.getColor());

                // Bring the graphic to the front of the viewing order
                Node node = (Node) colorableGraphic;
                node.setViewOrder(-1);
            }
        }
    }

    /**
     * Resets the length of the canoe, clearing all loads
     */
    public void resetLength()
    {
        if (Objects.equals(solveSystemButton.getText(), "Undo Solve")) // TODO: fix lazy implementation, should manage the state properly
            solveSystemButton.fire();

        clearLoads();
        axisLabelR.setText("X");
        axisLabelR.setLayoutX(607); // this will not be hard coded anymore once axis labels for new loads are implemented
        canoeLengthTextField.setDisable(false);
        canoeLengthComboBox.setDisable(false);
        lengthLabel.setDisable(false);
        deleteLoadButton.setDisable(true);
        clearLoadsButton.setDisable(true);
        disableLoadingControls(true);
        setCanoeLengthButton.setText("Set Length");
        setCanoeLengthButton.setOnAction(e -> setLength());
    }

    /**
     * Called by the "Set Length" button
     * Updates both the model and the UI, showing the length of the canoe
     */
    public void setLength()
    {
        if (validateTextAsDouble(canoeLengthTextField.getText())) {
            // Default list view message
            enableEmptyLoadListSettings(true);

            // Convert to metric
            double len = getDistanceConverted(canoeLengthComboBox, canoeLengthTextField);

            // Only allow lengths in the specified range
            if (len >= acceptedLengthRange[0] && len <= acceptedLengthRange[1]) {
                // Update the canoe model
                canoe.setLen(len);

                // Change the label on the scale
                axisLabelR.setText(String.format("%.2f m", canoe.getLen()));
                axisLabelR.setLayoutX(595); // this will not be hard coded anymore once axis labels for new loads are implemented

                // Clear potential alert and reset access to controls
                notificationLabel.setText("");
                disableLoadingControls(false);
                canoeLengthTextField.setDisable(true);
                canoeLengthComboBox.setDisable(true);
                lengthLabel.setDisable(true);
                deleteLoadButton.setDisable(true);
                clearLoadsButton.setDisable(true);

                // Set length button will now function as a reset length button
                setCanoeLengthButton.setText("Reset Length");
                setCanoeLengthButton.setOnAction(e -> resetLength());
            }
            // Populate the alert telling the user the length they've entered is out of the allowed range
            else
                notificationLabel.setText("Length must be between 0.05m and 20m");
        }
        else
            notificationLabel.setText("One or more entered values are not valid numbers");
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

        return switch (unit) {
            case "N", "N/m" -> d / 1000.0;
            case "kg", "kg/m" -> (d * GRAVITY) / 1000.0;
            case "lb" -> (d * POUNDS_TO_KG * GRAVITY) / 1000.0;
            case "lb/ft" -> (d * POUNDS_TO_KG * GRAVITY) / (1000.0 * FEET_TO_METRES);
            default -> d;
        };
    }

    /**
     * Update the ListView displaying loads to the user to be sorted by x position
     */
    public void updateLoadListView()
    {
        // Get the new list of loads as strings, sorted by x position
        List<Load> loads = new ArrayList<>();
        loads.addAll(canoe.getPLoads());
        loads.addAll(canoe.getDLoads());
        loads.sort(Comparator.comparingDouble(Load::getX));
        List<String> stringLoads = loads.stream()
                .map(Load::toString)
                .toList();


        // Recreate the list view from the updated load list
        loadListView.getItems().clear();
        loadListView.getItems().addAll(stringLoads);
    }

    /**
     * Rescales all point loads (arrows) and distributed loads (arrow boxes) based on the load with the highest mag.
     * Point and distributed load magnitudes are compared although their units differ (Force vs. Force / Length)
     * @param maxMag the maximum magnitude which all other loads are scaled down based off of
     */
    public void rescaleFromMax(double maxMag)
    {
        // Clear load container of all arrows
        loadContainer.getChildren().clear();

        // Find max magnitude pLoad in list of pLoads
        int maxIndex = getMaxIndexFromMagnitude(maxMag);

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
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beam.getThickness(); // 196
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); //126

                Arrow arrow = new Arrow(p.getXScaled(beam.getWidth(), canoe.getLen()), startY, p.getXScaled(beam.getWidth(), canoe.getLen()), endY);
                arrowList.add(arrow);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = p.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(p.getMag()) / maxMag));
                int startY = p.getMag() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beam.getThickness() + deltaY;
                Arrow arrow = new Arrow(p.getXScaled(beam.getWidth(), canoe.getLen()), startY, p.getXScaled(beam.getWidth(), canoe.getLen()), endY);
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
                int startY = d.getW() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beam.getThickness(); // 196
                int endY = d.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); //126

                ArrowBox arrowBox = new ArrowBox(d.getXScaled(beam.getWidth(), canoe.getLen()), startY, d.getRXScaled(beam.getWidth(), canoe.getLen()), endY);
                arrowBoxList.add(arrowBox);
            }

            else
            {
                // Render at scaled size (deltaY calculates the downscaling factor)
                int endY = d.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); //126
                int deltaY = (int) ((acceptedArrowHeightRange[1] - acceptedArrowHeightRange[0]) * (Math.abs(d.getW()) / maxMag));
                int startY = d.getW() < 0 ? acceptedArrowHeightRange[1] - deltaY : acceptedArrowHeightRange[1] + (int) beam.getThickness() + deltaY;

                ArrowBox arrowBox = new ArrowBox(d.getXScaled(beam.getWidth(), canoe.getLen()), startY, d.getRXScaled(beam.getWidth(), canoe.getLen()), endY);
                arrowBoxList.add(arrowBox);
            }
        }

        // Add sorted Arrows and ArrowBoxes to the loadContainer
        List<Positionable> graphics = new ArrayList<>();
        graphics.addAll(arrowList);
        graphics.addAll(arrowBoxList);
        graphics.sort(Comparator.comparingDouble(Positionable::getX));
        arrowBoxList.sort(Comparator.comparingDouble(ArrowBox::getLX));
        loadContainer.getChildren().addAll((graphics.stream().map(element -> (Node) element)).toList());

    }

    /**
     * @param maxMag the magnitude of the maximum load (in kN or kN/m, considered 'equivalent' for graphics rendering)
     * @return the index of that load on the canoes list of loads
     */
    private int getMaxIndexFromMagnitude(double maxMag) {
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
        if (!canoe.getPLoads().isEmpty() && !canoe.getDLoads().isEmpty())
            maxIndex = canoe.getMaxPLoad() > canoe.getMaxDLoad() ? maxPIndex : maxDIndex + canoe.getPLoads().size();
        else if (canoe.getPLoads().isEmpty())
            maxIndex = maxDIndex;
        else
            maxIndex = maxPIndex;
        return maxIndex;
    }

    /**
     * Called by the "Add Point Load" button
     * Deals with parsing and validation user input.
     */
    public void addPointLoad()
    {
        // Clear previous alert label
        notificationLabel.setText("");

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
                notificationLabel.setText("Load must be contained within the canoe's length");
            // Validate the load is in the accepted magnitude range
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                notificationLabel.setText("Load must be between 0.05kN and 10kN");

            else
            {
                // Removes the default list view message if this is the first load
                enableEmptyLoadListSettings(false);

                // Add the load to canoe, and the load arrow on the GUI
                PointLoad p = new PointLoad(mag, x, false);
                addPointLoadGraphic(p);
                updateLoadListView();
            }

        }
        else
            notificationLabel.setText("One or more entered values are not valid numbers");
    }

    /**
     * Called by the "Add Uniformly Distributed Load" button
     * Deals with parsing and validation user input.
     * Main logic handled in addDistributedLoadToCanoe method
     */
    public void addDistributedLoad()
    {// Clear previous alert labels
        notificationLabel.setText("");

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
                notificationLabel.setText("Load must be contained within the canoe's length");
            else if (!(r > l))
                notificationLabel.setText("Right interval bound must be greater than the left bound");
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                notificationLabel.setText("Load must be between 0.05kN/m and 10kN/m");

            else
                {
                    // Removes the default list view message if this is the first load
                    enableEmptyLoadListSettings(false);

                    // Add the load to canoe, and update the ListView
                    UniformDistributedLoad d = new UniformDistributedLoad(l, r, mag);
                    addDistributedLoadGraphic(d);
                    updateLoadListView();
                }
        }
        else
            notificationLabel.setText("One or more entered values are not valid numbers");

    }

    /**
     * Add a distributed load to the canoe object and JavaFX UI.
     * This method was extracted to allow for reuse in system solver methods.
     * @param dLoad the distributed load to be added.
     */
    private void addDistributedLoadGraphic(UniformDistributedLoad dLoad) {
        // Label reset
        notificationLabel.setText("");

        canoe.addDLoad(dLoad);

        // x coordinates of arrows in beamContainer for ArrowBox
        double scaledLX = dLoad.getXScaled(beam.getWidth(), canoe.getLen());
        double scaledRX = dLoad.getRXScaled(beam.getWidth(), canoe.getLen());

        // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
        int endY = dLoad.getW() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); //126

        // Only 1 load, always at max height
        if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2)
        {
            int startY = dLoad.getW() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beam.getThickness(); // 196
            ArrowBox arrowBox = new ArrowBox(scaledLX, startY, scaledRX, endY);
            loadContainer.getChildren().add(arrowBox);
        }

        else
        {
            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
            if (!(canoe.getMaxDLoad() / canoe.getMinDLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
                rescaleFromMax(Math.max(canoe.getMaxPLoad(), canoe.getMaxDLoad()));

            else
            {
                // TODO: implement clipping
                // currently: after getting here and trying to add another load you can't add any more arrows, need to fix that
            }
        }

        // Update the view order for proper z-axis rendering
        updateViewOrder();
    }

    /**
     * Add a point load to the canoe object and JavaFX UI.
     * @param pLoad the point load to be added.
     */
    private void addPointLoadGraphic(PointLoad pLoad)
    {
        // x coordinate in beamContainer for load
        double scaledX = pLoad.getXScaled(beam.getWidth(), canoe.getLen()); // x position in the beamContainer

        AddPointLoadResult addResult = canoe.addPLoad(pLoad);

        // Render the correct graphic
        if (pLoad.isSupport())
            addSupportGraphic(scaledX);
        else
            addArrowGraphic(pLoad, scaledX, addResult);
    }

    /**
     * Add a point load to the load container as a graphic of a triangle to represent a pinned support
     * @param beamContainerX the x coordinate of the load within the load container
     */
    private void addSupportGraphic(double beamContainerX)
    {
        // Create the list of current load graphics
        List<Positionable> loadContainerChildren = new ArrayList<>(loadContainer.getChildren().stream()
                .map(graphic -> (Positionable) graphic).toList());


        // Create and add the support graphic
        double tipY = acceptedArrowHeightRange[1] + (int) beam.getThickness(); // +126
        SupportTriangle support = new SupportTriangle(beamContainerX, tipY);
        loadContainerChildren.add(support);

        // Clear graphics the load container and add the new list of load graphics including the support, all sorted
        loadContainer.getChildren().clear();
        loadContainer.getChildren().addAll(loadContainerChildren.stream()
                .sorted(Comparator.comparingDouble(Positionable::getX))
                .map(load -> (Node) load).toList());

        // Update the view order for proper z-axis rendering
        updateViewOrder();
    }

    /**
     * Add a point load to the load container as a graphic of an arrow to represent the load
     * @param pLoad the point load to add
     * @param beamContainerX the x coordinate of the load within the load container
     * @param result the enum result of adding the load
     */
    private void addArrowGraphic(PointLoad pLoad, double beamContainerX, AddPointLoadResult result)
    {
        // Notify the user regarding point loads combining or cancelling
        notificationLabel.setText("");
        if (result == AddPointLoadResult.COMBINED)
            notificationLabel.setText("Point load magnitudes combined");
        else if (result == AddPointLoadResult.REMOVED)
            notificationLabel.setText("Point load magnitudes cancelled");

        // Prevent rendering issues with zero-valued loads
        if (pLoad.getMag() == 0)
            return;

        // endY always results in the arrow touching the beam (ternary operator accounts for direction)
        int endY = pLoad.getMag() < 0 ? acceptedArrowHeightRange[1] : acceptedArrowHeightRange[1] + (int) beam.getThickness(); // +126

        // If only 1 load it's always at max height
        if (canoe.getPLoads().size() + canoe.getDLoads().size() < 2 && result != AddPointLoadResult.REMOVED)
        {
            int startY = pLoad.getMag() < 0 ? acceptedArrowHeightRange[0] : 2 * acceptedArrowHeightRange[1] + (int) beam.getThickness(); // +196
            Arrow arrow = new Arrow(beamContainerX, startY, beamContainerX, endY);
            loadContainer.getChildren().add(arrow);
        }

        else
        {
            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
            if (!(canoe.getMaxPLoad() / canoe.getMinPLoad() > (double) acceptedArrowHeightRange[1] / (double) acceptedArrowHeightRange[0]))
                rescaleFromMax(Math.max(canoe.getMaxPLoad(), canoe.getMaxDLoad()));

            else
            {
                // TODO: implement clipping
                // currently: after getting here and trying to add another load you can't add any more arrows
            }
        }

        // Update the view order for proper z-axis rendering
        updateViewOrder();
    }

    /**
     * Handler for the "solve system" button.
     * Distributes call to appropriate method and re-renders loads.
     */
    public void solveSystem() {
        // Removes the default list view message if this is the first load
        enableEmptyLoadListSettings(false);

        // Controls enabling/disabling for UX
        generateGraphsButton.setDisable(false);
        disableLoadingControls(true);
        loadListView.setDisable(false);

        if (standsRadioButton.isSelected())
        {
            solveStandSystem();
            solveSystemButton.setText("Undo Solve");
            solveSystemButton.setOnAction(e -> undoStandsSolve());
            solveSystemButton.setDisable(false);
        }
        else if (floatingRadioButton.isSelected())
        {
//            solveFloatingSystem();
//            solveSystemButton.setText("Undo Solve");
//            solveSystemButton.setOnAction(e -> undoFloatingSolve());
//            solveSystemButton.setDisable(false);
        }
        else if (submergedRadioButton.isSelected())
        {
//            solveSubmergedSystem();
//            solveSystemButton.setText("Undo Solve");
//            solveSystemButton.setOnAction(e -> undoSubmergedSolve());
//            solveSystemButton.setDisable(false);
        }

        // Update the lod list view with the supports
        updateLoadListView();
    }

    /**
     * Solve and display the result of the "stand" system load case.
     * This entails two supports at each end of the canoe, symmetrically offset from the beam bounds
     * This interval is currently hardcoded as 0 in SystemSolver, so supports on beam endpoints
     */
    private void solveStandSystem()
    {
        List<PointLoad> supportLoads = SystemSolver.solveStandSystem(canoe);
        for (PointLoad supportLoad : supportLoads) {
            addPointLoadGraphic(supportLoad);}
    }

    private void undoStandsSolve()
    {
        solveSystemButton.setText("Solve System");
        solveSystemButton.setOnAction(e -> solveSystem());
        generateGraphsButton.setDisable(true);
        disableLoadingControls(false);

        // Simulate the user selecting and deleting the supports
        // Supports are always the first and last load
        loadListView.getSelectionModel().select(0);
        deleteLoad();
        loadListView.getSelectionModel().select(loadListView.getItems().size() - 1);
        deleteLoad();
    }

    private void solveFloatingSystem()
    {
        // TODO: Implement (consult Design & Analysis)
    }

    private void undoFloatingSolve()
    {
        // TODO: Implement (after solveFloatingSystem())
    }

    private void solveSubmergedSystem()
    {
        // TODO: Implement (consult Design & Analysis)
    }

    private void undoSubmergedSolve()
    {
        // TODO: Implement (after solveSubmergedSystem())
    }

    /**
     * Bulk package to toggle enabling all the controls related to loading
     * @param b disables controls
     */
    private void disableLoadingControls(boolean b)
    {
        List<Control> controls = Arrays.asList(solveSystemButton, pointLoadButton, distributedLoadButton,
                floatingRadioButton, standsRadioButton, submergedRadioButton, distributedMagnitudeComboBox,
                distributedMagnitudeTextField, pointMagnitudeComboBox, pointMagnitudeTextField, pointLocationTextField,
                pointLocationComboBox, pointDirectionComboBox, distributedIntervalTextFieldL,
                distributedIntervalTextFieldR, distributedIntervalComboBox, distributedDirectionComboBox,
                deleteLoadButton, clearLoadsButton, loadListView, pointDirectionLabel, pointMagnitudeLabel,
                pointLocationLabel, pointTitleLabel, supportTitleLabel, distributedDirectionLabel,
                distributedMagntiudeLabel, distributedIntervalLabel, distributedTitleLabel
        );

        for (Control control : controls) {
            control.setDisable(b);
        }
    }

    /**
     * Generates an SFD and BMD based on the canoe's load state.
     */
    public void generateDiagram()
    {
        // Logging
        System.out.println();
        for (DiagramPoint sfdPoint : Diagram.generateSfdPoints(canoe))
        {
            System.out.println("sfdPoint generated: " + sfdPoint);
        }

        DiagramLogic.setupDiagram(canoe, Diagram.generateSfdPoints(canoe), "Shear Force Diagram", "Force [kN]");

        // Logging
        System.out.println();
        for (DiagramPoint bmdPoint : Diagram.generateBmdPoints(canoe))
        {
            System.out.println("bmdPoint generated: " + bmdPoint);
        }

        DiagramLogic.setupDiagram(canoe, Diagram.generateBmdPoints(canoe), "Bending Moment Diagram", "Moment [kN·m]");
    }

    /**
     * Delete the load selected in the list view
     */
    public void deleteLoad()
    {
        int selectedIndex = loadListView.getSelectionModel().getSelectedIndex();

        // Handle case that no index was selected
        if (selectedIndex == -1)
        {
            notificationLabel.setText("Cannot perform delete, no load selected");
            return;
        }

        loadListView.getItems().remove(selectedIndex);
        loadContainer.getChildren().remove(selectedIndex);
        canoe.removeLoad(selectedIndex);

        // If the list is empty, toggle empty list settings
        if (loadContainer.getChildren().isEmpty())
            enableEmptyLoadListSettings(true);
    }

    /**
     * Clear all loads
     */
    public void clearLoads()
    {
        loadListView.getItems().clear();
        loadContainer.getChildren().clear();
        canoe.clearLoads();

        // List is empty, toggle empty list settings
        enableEmptyLoadListSettings(true);
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
        canoe = Canoe.getInstance();

        // Disable most buttons on startup to prevent inputs in the wrong order
        disableLoadingControls(true);
        generateGraphsButton.setDisable(true);
        enableEmptyLoadListSettings(true);

        // Css styling
        JFXDepthManager.setDepth(loadListView, 4);

        // Setting RadioButton Toggle Group
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{standsRadioButton, floatingRadioButton, submergedRadioButton};
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

        // Create and add the beam
        beam = new Beam(0, 84, beamContainer.getPrefWidth(), 25);
        JFXDepthManager.setDepth(beam, 4);
        beamContainer.getChildren().add(beam);
    }
}