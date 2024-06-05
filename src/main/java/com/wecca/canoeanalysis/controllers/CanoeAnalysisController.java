package com.wecca.canoeanalysis.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.effects.JFXDepthManager;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.diagrams.*;
import com.wecca.canoeanalysis.graphics.*;
import com.wecca.canoeanalysis.graphics.CustomJFXSnackBarLayout;
import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.util.*;
import javafx.animation.*;
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
    private Label axisLabelR, lengthLabel, pointDirectionLabel, pointMagnitudeLabel, pointLocationLabel,
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
    private AnchorPane root, loadContainer, beamContainer;
    @FXML
    private AnchorPane menuDrawer;
    @FXML
    private JFXHamburger hamburger;
    @FXML
    private JFXSnackbar snackbar;

    private Canoe canoe; // entity class that models the canoe as a beam
    private Beam beam; // The graphic of the beam

    private final double FEET_TO_METRES = 0.3048; // conversion factor ft to m
    private final double POUNDS_TO_KG = 0.45359237; // conversion factor lb to kg
    private final double GRAVITY = 9.80665; // gravity on earth

    private final double[] acceptedMagRange = new double[] {0.05, 10}; // Acceptable magnitude range (kN)
    private final double[] acceptedLengthRange = new double[] {0.05, 20}; // Acceptable canoe length range (m)
    private final int[] acceptedGraphicHeightRange = new int[] {14, 84}; // Acceptable arrow height range (px)
    // Cannot get this from imageView, it hasn't been instantiated until initialize is called
    // Is there a workaround to this that doesn't require adding the imageView manually in code
    // Also this is awkward to add it in with all the fields at the top

    // For draggable window with custom toolbar
    @Setter
    private static Stage primaryStage;
    private static double xOffset = 0;
    private static double yOffset = 0;

    // Drawer state management
    private boolean isDrawerOpen = false;
    private AnimationTimer drawerTimer;
    private double drawerTargetX; // Target X position for the drawer

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

    public void toggleDrawer() {
        if (isDrawerOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    private void openDrawer() {
        drawerTargetX = 0;
        menuDrawer.setVisible(true);
        startDrawerTimer();
    }

    private void closeDrawer() {
        drawerTargetX = -menuDrawer.getPrefWidth();
        startDrawerTimer();
    }

    private void startDrawerTimer()
    {
        hamburgerButton.setDisable(true);
        hamburger.setDisable(false);

        if (drawerTimer != null)
            drawerTimer.stop();

        drawerTimer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                double currentX = menuDrawer.getTranslateX();
                if (currentX != drawerTargetX)
                {
                    double newX = currentX + (drawerTargetX - currentX) * 0.075;
                    menuDrawer.setTranslateX(newX);
                    if (Math.abs(newX - drawerTargetX) < 1)
                    {
                        menuDrawer.setTranslateX(drawerTargetX);
                        stop();
                        isDrawerOpen = drawerTargetX == 0;
                        hamburgerButton.setDisable(false);
                    }
                }
                else
                {
                    stop();
                    hamburgerButton.setDisable(false);
                }
            }
        };
        drawerTimer.start();
    }

    public void showSnackbar(String message) {
        closeSnackBar(snackbar);
        initializeSnackbar(); // Reinitialization is required to fix visual bugs
        CustomJFXSnackBarLayout snackbarLayout = new CustomJFXSnackBarLayout(message, "DISMISS", event -> closeSnackBar(snackbar));
        snackbarLayout.setPrefHeight(50);
        Button dismissButton = snackbarLayout.getAction();
        dismissButton.getStyleClass().add("dismiss-button");
        JFXSnackbar.SnackbarEvent snackbarEvent = new JFXSnackbar.SnackbarEvent(snackbarLayout, Duration.seconds(3));
        snackbar.fireEvent(snackbarEvent);
    }

    public void initializeSnackbar()
    {
        snackbar = new JFXSnackbar(root);
        JFXDepthManager.setDepth(snackbar, 5);
        snackbar.setPrefWidth(200);
        snackbar.setViewOrder(Integer.MIN_VALUE);
        snackbar.getStylesheets().add(CanoeAnalysisApplication.class.getResource("css/style.css").toExternalForm());
    }

    // .close() has an unfixed bug in the JFoenix library itself
    // The bug at https://github.com/sshahine/JFoenix/issues/1101 has not been adequately fixed
    // Custom fix source: https://github.com/sshahine/JFoenix/issues/983 from GitHub user sawaYch
    public void closeSnackBar(JFXSnackbar sb) {
        Timeline closeAnimation = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        e -> sb.toFront(),
                        new KeyValue(sb.opacityProperty(), 1, Interpolator.EASE_IN),
                        new KeyValue(sb.translateYProperty(), 0, Interpolator.EASE_OUT)
                ),
                new KeyFrame(
                        Duration.millis(290),
                        new KeyValue(sb.visibleProperty(), true, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(300),
                        e -> sb.toBack(),
                        new KeyValue(sb.visibleProperty(), false, Interpolator.EASE_BOTH),
                        new KeyValue(sb.translateYProperty(),
                                sb.getLayoutBounds().getHeight(),
                                Interpolator.EASE_IN),
                        new KeyValue(sb.opacityProperty(), 0, Interpolator.EASE_OUT)
                )
        );
        closeAnimation.setCycleCount(1);
        closeAnimation.play();
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
        // Layering priority is SupportTriangles => above Arrows => above ArrowBoxes
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
            Graphic colorableGraphic = (Graphic) loadContainer.getChildren().get(i);
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
                closeSnackBar(snackbar);
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
                showSnackbar("Length must be between 0.05m and 20m");
        }
        else
            showSnackbar("One or more entered values are not valid numbers");
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
        List<Load> loads = new ArrayList<>(canoe.getLoads());
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
     */
    public void refreshLoadGraphics()
    {
        // Clear load container of graphics
        loadContainer.getChildren().clear();

        // List of Arrows and ArrowBoxes
        List<Graphic> graphics = new ArrayList<>();

        // The index in the canoes Load list of the maximum magnitude load
        int maxIndex = canoe.getMaxLoadIndex();

        // Rescale all graphics from max
        for (int i = 0; i < canoe.getLoads().size(); i++)
        {
            Load l = canoe.getLoads().get(i);

            // Render at scaled size (deltaY calculates the downscaling factor)
            int endY = l.getMag() < 0 ? acceptedGraphicHeightRange[1] : acceptedGraphicHeightRange[1] + (int) beam.getThickness(); // Beam thickness = 126
            int deltaY = (int) ((acceptedGraphicHeightRange[1] - acceptedGraphicHeightRange[0]) * Math.abs(l.getMag() / canoe.getLoads().get(maxIndex).getMag()));
            int startY = l.getMag() < 0 ? acceptedGraphicHeightRange[1] - deltaY : acceptedGraphicHeightRange[1] + (int) beam.getThickness() + deltaY;

            if (l instanceof PointLoad)
                graphics.add(new Arrow(l.getXScaled(beam.getWidth(), canoe.getLen()), startY, l.getXScaled(beam.getWidth(), canoe.getLen()), endY));
            else if (l instanceof UniformDistributedLoad)
                graphics.add(new ArrowBox(l.getXScaled(beam.getWidth(), canoe.getLen()), startY, ((UniformDistributedLoad) l).getRXScaled(beam.getWidth(), canoe.getLen()), endY));
        }

        graphics.sort(Comparator.comparingDouble(Graphic::getX));
        loadContainer.getChildren().addAll((graphics.stream().map(element -> (Node) element)).toList());
    }

    /**
     * Called by the "Add Point Load" button
     * Deals with parsing and validation user input.
     */
    public void addPointLoad()
    {
        // Clear previous alert label
        closeSnackBar(snackbar);

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
                showSnackbar("Load must be contained within the canoe's length");
            // Validate the load is in the accepted magnitude range
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                showSnackbar("Load must be between 0.05kN and 10kN");

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
            showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Called by the "Add Uniformly Distributed Load" button
     * Deals with parsing and validation user input.
     * Main logic handled in addDistributedLoadToCanoe method
     */
    public void addDistributedLoad()
    {// Clear previous alert labels
        closeSnackBar(snackbar);

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
                showSnackbar("Load must be contained within the canoe's length");
            else if (!(r > l))
                showSnackbar("Right interval bound must be greater than the left bound");
            else if (!(acceptedMagRange[0] <= Math.abs(mag) && Math.abs(mag) <= acceptedMagRange[1]))
                showSnackbar("Load must be between 0.05kN/m and 10kN/m");

            else
                {
                    // Removes the default list view message if this is the first load
                    enableEmptyLoadListSettings(false);

                    // Add the load to canoe, and update the ListView
                    UniformDistributedLoad d = new UniformDistributedLoad(l, r, mag);
                    addArrowBoxGraphic(d);
                    updateLoadListView();
                }
        }
        else
            showSnackbar("One or more entered values are not valid numbers");

    }

    /**
     * Add a distributed load to the canoe object and JavaFX UI.
     * This method was extracted to allow for reuse in system solver methods.
     * @param dLoad the distributed load to be added.
     */
    private void addArrowBoxGraphic(UniformDistributedLoad dLoad) {
        // Label reset
        closeSnackBar(snackbar);

        canoe.addLoad(dLoad);

        // x coordinates of arrows in beamContainer for ArrowBox
        double scaledX = dLoad.getXScaled(beam.getWidth(), canoe.getLen());
        double scaledRX = dLoad.getRXScaled(beam.getWidth(), canoe.getLen());

        // endY is always results in the arrow touching the beam (ternary operator accounts for direction)
        int endY = dLoad.getMag() < 0 ? acceptedGraphicHeightRange[1] : acceptedGraphicHeightRange[1] + (int) beam.getThickness(); //126

        // Only 1 load, always at max height
        if (canoe.getLoads().size() <= 1)
        {
            int startY = dLoad.getMag() < 0 ? acceptedGraphicHeightRange[0] : 2 * acceptedGraphicHeightRange[1] + (int) beam.getThickness(); // 196
            ArrowBox arrowBox = new ArrowBox(scaledX, startY, scaledRX, endY);
            loadContainer.getChildren().add(arrowBox);
        }

        else
        {
            // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
            if (!(canoe.getLoads().get(canoe.getMaxLoadIndex()).getMag() / canoe.getLoads().get(canoe.getMinLoadIndex()).getMag() > (double) acceptedGraphicHeightRange[1] / (double) acceptedGraphicHeightRange[0]))
                refreshLoadGraphics();

            else
            {
                // TODO: implement clipping
                // currently: after getting here and trying to add another load you can't add any more arrows, need to fix that
                showSnackbar("This is an edge case with no handler yet - Tyler :-)");
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

        AddLoadResult addResult = canoe.addLoad(pLoad);

        // Render the correct graphic
        if (pLoad.isSupport())
            addSupportGraphic(scaledX);
        else
            addArrowGraphic(pLoad, addResult);
    }

    /**
     * Add a point load to the load container as a graphic of a triangle to represent a pinned support
     * @param beamContainerX the x coordinate of the load within the load container
     */
    private void addSupportGraphic(double beamContainerX)
    {
        // Create the list of current load graphics
        List<Graphic> loadContainerChildren = new ArrayList<>(loadContainer.getChildren().stream()
                .map(node -> (Graphic) node).toList());


        // Create and add the support graphic
        double tipY = acceptedGraphicHeightRange[1] + (int) beam.getThickness(); // +126
        SupportTriangle support = new SupportTriangle(beamContainerX, tipY);
        loadContainerChildren.add(support);

        // Clear graphics the load container and add the new list of load graphics including the support, all sorted
        loadContainer.getChildren().clear();
        loadContainer.getChildren().addAll(loadContainerChildren.stream()
                .sorted(Comparator.comparingDouble(Graphic::getX))
                .map(load -> (Node) load).toList());

        // Update the view order for proper z-axis rendering
        updateViewOrder();
    }

    /**
     * Add a point load to the load container as a graphic of an arrow to represent the load
     * @param pLoad the point load to add
     * @param result the enum result of adding the load
     */
    private void addArrowGraphic(PointLoad pLoad, AddLoadResult result)
    {
        // Notify the user regarding point loads combining or cancelling
        closeSnackBar(snackbar);
        if (result == AddLoadResult.COMBINED)
            showSnackbar("Point load magnitudes combined");
        else if (result == AddLoadResult.REMOVED)
            showSnackbar("Point load magnitudes cancelled");

        // Prevent rendering issues with zero-valued loads
        if (pLoad.getMag() == 0)
            return;

        // If only 1 load it's always at max height, so we don't need to worry checking relative scaling
        if (canoe.getLoads().size() <= 1 && result != AddLoadResult.REMOVED)
            refreshLoadGraphics();

        else
        {
            // Load list can be empty if loads cancelled out
            if (!canoe.getLoads().isEmpty())
            {
                // Stay within the limits of the accepted height range (based on pixel spacing in the GUI)
                if (!(canoe.getLoads().get(canoe.getMaxLoadIndex()).getMag() / canoe.getLoads().get(canoe.getMinLoadIndex()).getMag() > (double) acceptedGraphicHeightRange[1] / (double) acceptedGraphicHeightRange[0]))
                    refreshLoadGraphics();

                else {
                    // TODO: implement clipping
                    // currently: after getting here and trying to add another load you can't add any more arrows
                    showSnackbar("This is an edge case with no handler yet - Tyler :-)");
                }
            }
            else
                refreshLoadGraphics();
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
            solveSystemButton.setOnAction(e -> undoStandsSolve());
        }
        else if (floatingRadioButton.isSelected())
        {
            solveFloatingSystem();
            solveSystemButton.setOnAction(e -> undoFloatingSolve());
        }
        else if (submergedRadioButton.isSelected())
        {
            solveSubmergedSystem();
            solveSystemButton.setOnAction(e -> undoSubmergedSolve());
        }

        // Update UI to disable loading and enable undo solve functionality
        solveSystemButton.setText("Undo Solve");
        solveSystemButton.setDisable(false);

        // Update the load list view supports from solve
        updateLoadListView();
    }

    /**
     * Solve and display the result of the "stand" system load case.
     * This entails two supports at each end of the canoe, symmetrically offset from the beam bounds
     * This interval is currently hardcoded as 0 in SystemSolver, so supports on beam endpoints
     */
    private void solveStandSystem()
    {
        List<PointLoad> supportLoads = SolverUtils.solveStandSystem(canoe);
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
        showSnackbar("This has not yet been implemented - Tyler :-)");
    }

    private void undoFloatingSolve()
    {
        // TODO: Implement (after solveFloatingSystem())
        solveSystemButton.setText("Solve System");
        solveSystemButton.setOnAction(e -> solveSystem());
        generateGraphsButton.setDisable(true);
        disableLoadingControls(false);
    }

    private void solveSubmergedSystem()
    {
        // TODO: Implement (consult Design & Analysis)
        showSnackbar("This has not yet been implemented - Tyler :-)");
    }

    private void undoSubmergedSolve()
    {
        // TODO: Implement (after solveSubmergedSystem())
        solveSystemButton.setText("Solve System");
        solveSystemButton.setOnAction(e -> solveSystem());
        generateGraphsButton.setDisable(true);
        disableLoadingControls(false);
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
            showSnackbar("Cannot perform delete, no load selected");
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

    private void initializeHamburger() {
        HamburgerBackArrowBasicTransition transition = new HamburgerBackArrowBasicTransition(hamburger);
        transition.setRate(-1);
        hamburgerButton.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
            transition.setRate(transition.getRate() * -1);
            transition.play();
            toggleDrawer();
        });
    }

    private void initializeDrawer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wecca/canoeanalysis/view/side-drawer-view.fxml"));
            AnchorPane drawerContent = loader.load();
            menuDrawer.getChildren().setAll(drawerContent);
            menuDrawer.setTranslateX(-menuDrawer.getPrefWidth());
        } catch (IOException ignored) {}
    }

    public void uploadCanoe()
    {

    }

    public void downloadCanoe() throws JsonProcessingException {
        String JSONCanoe = (new ObjectMapper()).writeValueAsString(canoe);
        System.out.println(JSONCanoe);
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
        JFXDepthManager.setDepth(menuDrawer, 5);

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

        // JFX Initialization refactored separately
        initializeHamburger();
        initializeDrawer();
        initializeSnackbar();
    }
}