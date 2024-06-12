package com.wecca.canoeanalysis.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.ColorPalette;
import com.wecca.canoeanalysis.components.graphics.*;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.services.*;
import com.wecca.canoeanalysis.utils.ControlUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.paint.Paint;
import lombok.Setter;
import java.net.URL;
import java.util.*;

/**
 * Primary controller for longitudinal analysis of a beam
 */
public class BeamController implements Initializable
{
    @FXML
    private Label axisLabelR, lengthLabel, pointDirectionLabel, pointMagnitudeLabel, pointLocationLabel,
            pointTitleLabel, supportTitleLabel, distributedDirectionLabel, distributedMagntiudeLabel,
            distributedIntervalLabel, distributedTitleLabel;
    @FXML
    private ListView<String> loadListView;

    @FXML
    private Button solveSystemButton, pointLoadButton, distributedLoadButton, setCanoeLengthButton, generateGraphsButton,
            clearLoadsButton, deleteLoadButton;
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

    @Setter
    private static MainController mainController;

    private Canoe canoe; // entity class that models the canoe as a beam
    private Beam beam; // The graphic of the beam

    // Size rules for load graphics (prevents them from rendering too big/small)
    private double[] acceptedGraphicHeightRange;


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
                colorableGraphic.recolor(ColorPalette.getInstance().getWhite());
            else
            {
                colorableGraphic.recolor(ColorPalette.getInstance().getPrimary());

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
        axisLabelR.setLayoutX(607); // TODO: this will not be hard coded anymore once axis labels for new loads are implemented
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
        if (ParsingService.validateTextAsDouble(canoeLengthTextField.getText())) {
            // Default list view message
            enableEmptyLoadListSettings(true);

            // Convert to metric
            double len = ParsingService.getDistanceConverted(canoeLengthComboBox, canoeLengthTextField);

            // Only allow lengths in the specified range
            if (len >= 0.01) {
                // Update model state
                canoe.setLen(len);

                // Change the label on the scale
                axisLabelR.setText(String.format("%.2f m", canoe.getLen()));
                axisLabelR.setLayoutX(595); // TODO: this will not be hard coded anymore once axis labels for new loads are implemented

                // Clear potential alert and reset access to controls
                mainController.closeSnackBar(mainController.getSnackbar());
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
                mainController.showSnackbar("Length must be at least 0.01m");
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
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

        // The index in the canoes Load list of the maximum magnitude load
        int maxIndex = canoe.getMaxLoadIndex();

        // Rescale all graphics relative to the max load
        List<Graphic> rescaledGraphics = new ArrayList<>();
        for (int i = 0; i < canoe.getLoads().size(); i++)
        {
            Load l = canoe.getLoads().get(i);

            // The ratio of the largest load (always rendered at max size) to this load
            double loadMagnitudeRatio = Math.abs(l.getMag() / canoe.getLoads().get(maxIndex).getMag());

            // Clip load length if too small (i.e. ratio is too large)
            if (loadMagnitudeRatio < Math.abs(acceptedGraphicHeightRange[0] / acceptedGraphicHeightRange[1]))
                loadMagnitudeRatio = Math.abs(acceptedGraphicHeightRange[0] / acceptedGraphicHeightRange[1]);

            // Render at scaled size (deltaY calculates the downscaling factor)
            double endY = l.getMag() < 0 ? acceptedGraphicHeightRange[1] : acceptedGraphicHeightRange[1] + (int) beam.getThickness();
            double deltaY = (acceptedGraphicHeightRange[1] - acceptedGraphicHeightRange[0]) * loadMagnitudeRatio;
            double startY = l.getMag() < 0 ? acceptedGraphicHeightRange[1] - deltaY : acceptedGraphicHeightRange[1] + (int) beam.getThickness() + deltaY;

            if (l instanceof PointLoad)
                rescaledGraphics.add(new Arrow(l.getXScaled(beam.getWidth(), canoe.getLen()), startY, l.getXScaled(beam.getWidth(), canoe.getLen()), endY));
            else if (l instanceof UniformDistributedLoad)
                rescaledGraphics.add(new ArrowBox(l.getXScaled(beam.getWidth(), canoe.getLen()), startY, ((UniformDistributedLoad) l).getRXScaled(beam.getWidth(), canoe.getLen()), endY));
        }

        rescaledGraphics.sort(Comparator.comparingDouble(Graphic::getX));
        loadContainer.getChildren().addAll((rescaledGraphics.stream().map(element -> (Node) element)).toList());
        updateViewOrder();
    }

    /**
     * Called by the "Add Point Load" button
     * Deals with parsing and validation user input.
     */
    public void addPointLoad()
    {
        // Clear previous alert label
        mainController.closeSnackBar(mainController.getSnackbar());

        // Validate the entered numbers are doubles
        if (ParsingService.allTextFieldsAreDouble(Arrays.asList(pointLocationTextField, pointMagnitudeTextField)))
        {
            double x = ParsingService.getDistanceConverted(pointLocationComboBox, pointLocationTextField);
            double mag = ParsingService.getLoadConverted(pointMagnitudeComboBox, pointMagnitudeTextField);
            String direction = pointDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}

            // Validate the load is being added within the length of the canoe
            if (!(0 <= x && x <= canoe.getLen()))
                mainController.showSnackbar("Load must be contained within the canoe's length");
            // Validate the load is in the accepted magnitude range
            else if (Math.abs(mag) < 0.01)
                mainController.showSnackbar("Load magnitude at least 0.01kN");

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
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Called by the "Add Uniformly Distributed Load" button
     * Deals with parsing and validation user input.
     * Main logic handled in addDistributedLoadToCanoe method
     */
    public void addDistributedLoad()
    {
        // Clear previous alert labels
        mainController.closeSnackBar(mainController.getSnackbar());

        // Validate the entered numbers are doubles
        if (ParsingService.allTextFieldsAreDouble(Arrays.asList(distributedMagnitudeTextField, distributedIntervalTextFieldL,
                distributedIntervalTextFieldR)))
        {
            double x = ParsingService.getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldL);
            double xR = ParsingService.getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldR);
            double mag = ParsingService.getLoadConverted(distributedMagnitudeComboBox, distributedMagnitudeTextField);
            String direction = distributedDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}

            // User entry validations
            if (!(0 <= x && xR <= canoe.getLen()))
                mainController.showSnackbar("Load must be contained within the canoe's length");
            else if (!(xR > x))
                mainController.showSnackbar("Right interval bound must be greater than the left bound");
            else if (Math.abs(mag) < 0.01)
                mainController.showSnackbar("Load magnitude at least 0.01kN");

            else
                {
                    // Removes the default list view message if this is the first load
                    enableEmptyLoadListSettings(false);

                    // Add the load to canoe, and update ui state
                    UniformDistributedLoad d = new UniformDistributedLoad(x, xR, mag);
                    addArrowBoxGraphic(d);
                    updateLoadListView();
                }
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Add a distributed load to the canoe object and JavaFX UI.
     * This method was extracted to allow for reuse in system solver methods.
     * @param dLoad the distributed load to be added.
     */
    private void addArrowBoxGraphic(UniformDistributedLoad dLoad) {
        // Label reset
        mainController.closeSnackBar(mainController.getSnackbar());

        // Add the load to the model
        canoe.addLoad(dLoad);

        // Refresh load graphics
        refreshLoadGraphics();
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

        // Update ui state
    }

    /**
     * Add a point load to the load container as a graphic of an arrow to represent the load
     * @param pLoad the point load to add
     * @param result the enum result of adding the load
     */
    private void addArrowGraphic(PointLoad pLoad, AddLoadResult result)
    {
        // Notify the user regarding point loads combining or cancelling
        mainController.closeSnackBar(mainController.getSnackbar());
        if (result == AddLoadResult.COMBINED)
            mainController.showSnackbar("Point load magnitudes combined");
        else if (result == AddLoadResult.REMOVED)
            mainController.showSnackbar("Point load magnitudes cancelled");

        // Prevent rendering issues with zero-valued loads
        if (pLoad.getMag() == 0)
            return;

        refreshLoadGraphics();
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

        // Update state
        updateLoadListView();
        updateViewOrder();
    }

    /**
     * Solve and display the result of the "stand" system load case.
     * This entails two supports at each end of the canoe, symmetrically offset from the beam bounds
     * This interval is currently hardcoded as 0 in SystemSolver, so supports on beam endpoints
     */
    private void solveStandSystem()
    {
        List<PointLoad> supportLoads = SolverService.solveStandSystem(canoe);
        for (PointLoad supportLoad : supportLoads) {addPointLoadGraphic(supportLoad);}
    }

    private void undoStandsSolve()
    {
        solveSystemButton.setText("Solve System");
        solveSystemButton.setOnAction(e -> solveSystem());
        generateGraphsButton.setDisable(true);
        disableLoadingControls(false);

        // Simulate the user selecting and deleting the supports which are always the first and last load
        loadListView.getSelectionModel().select(0);
        deleteLoad();
        loadListView.getSelectionModel().select(loadListView.getItems().size() - 1);
        deleteLoad();
    }

    private void solveFloatingSystem()
    {
        // TODO: Implement (consult Design & Analysis)
        mainController.showSnackbar("This has not yet been implemented - Tyler :-)");
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
        mainController.showSnackbar("This has not yet been implemented - Tyler :-)");
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
        WindowOpenerService.openDiagramWindow("Shear Force Diagram", canoe, DiagramService.generateSfdPoints(canoe), "Force [kN]");
        WindowOpenerService.openDiagramWindow("Bending Moment Diagram", canoe, DiagramService.generateBmdPoints(canoe), "Moment [kN·m]");
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
            mainController.showSnackbar("Cannot perform delete, no load selected");
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
     * Upload a JSON file representing the Canoe object model
     * This populates the list view and beam graphic with the new model
     */
    public void uploadCanoe()
    {
        // TODO
    }

    /**
     * Download the Canoe object model with the loads currently on the canoe as a JSON file
     * This can be uploaded later with uploadCanoe() or manually modified
     */
    public void downloadCanoe() throws JsonProcessingException {
        // TODO
        String JSONCanoe = (new ObjectMapper()).writeValueAsString(canoe);
        System.out.println(JSONCanoe);
    }

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     * Currently, this provides buttons to download and upload the Canoe object as JSON
     */
    public void addModuleToolBarButtons()
    {
        List<Button> beamModuleButtons = new ArrayList<>();

        Button downloadCanoeButton = new Button();
        downloadCanoeButton.getStyleClass().add("transparent-button");
        downloadCanoeButton.setOnAction(event -> {
            try {
                downloadCanoe();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        FontAwesomeIcon downloadIcon = new FontAwesomeIcon();
        downloadIcon.setFill(Paint.valueOf("WHITE"));
        downloadIcon.setGlyphName("ARROW_CIRCLE_O_DOWN");
        downloadIcon.setSize("25");
        downloadCanoeButton.setGraphic(downloadIcon);
        beamModuleButtons.add(downloadCanoeButton);

        Button uploadCanoeButton = new Button();
        uploadCanoeButton.getStyleClass().add("transparent-button");
        uploadCanoeButton.setOnAction(event -> uploadCanoe());
        FontAwesomeIcon uploadIcon = new FontAwesomeIcon();
        uploadIcon.setFill(Paint.valueOf("WHITE"));
        uploadIcon.setGlyphName("ARROW_CIRCLE_O_UP");
        uploadIcon.setSize("25");
        uploadCanoeButton.setGraphic(uploadIcon);
        beamModuleButtons.add(uploadCanoeButton);

        mainController.resetToolBarButtons();
        mainController.addToolBarButtons(beamModuleButtons);
    }

    /**
     * Operations called on initialization of the view
     * @param url unused, part of javafx framework
     * @param resourceBundle unused, part of javafx framework
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        // Add module tool bar buttons
        addModuleToolBarButtons();

        // Instantiate the canoe
        canoe = Canoe.getInstance();

        // Reset module state if switching PADDL modules
        canoe.setLen(0);
        canoe.getPLoads().clear();
        canoe.getDLoads().clear();
        canoe.getLoads().clear();
        loadListView.getItems().clear();
        loadContainer.getChildren().clear();

        // Disable most buttons on startup to prevent inputs in the wrong order
        disableLoadingControls(true);
        generateGraphsButton.setDisable(true);
        enableEmptyLoadListSettings(true);

        // Css styling
        JFXDepthManager.setDepth(loadListView, 4);

        // Setting RadioButton Toggle Group
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{standsRadioButton, floatingRadioButton, submergedRadioButton};
        ControlUtils.addAllRadioButtonsToToggleGroup(canoeSupportToggleGroup, canoeSupportRButtons, 0);

        // Populate ComboBoxes
        String[] directions = new String[]{"Down", "Up"};
        String[] loadUnits = new String[]{"kN", "N", "kg", "lb"};
        String[] distanceUnits = new String[]{"m", "ft"};
        String[] distributedLoadUnits = new String[]{"kN/m", "N/m", "kg/m", "lb/ft"};

        ControlUtils.initComboBoxesWithDefaultSelected(pointDirectionComboBox, directions, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(pointMagnitudeComboBox, loadUnits, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(pointLocationComboBox, distanceUnits, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(distributedIntervalComboBox, distanceUnits, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(distributedDirectionComboBox, directions, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(distributedMagnitudeComboBox, distributedLoadUnits, 0);
        ControlUtils.initComboBoxesWithDefaultSelected(canoeLengthComboBox, distanceUnits, 0);

        // Populate the TextFields with default values
        TextField[] tfs = new TextField[]{pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
                distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField};
        for (TextField tf : tfs) {tf.setText("0.00");}

        // Beam initialization
        beam = new Beam(0, 84, beamContainer.getPrefWidth(), 25);
        JFXDepthManager.setDepth(beam, 4);
        beamContainer.getChildren().add(beam);

        // Initialize maximum allowed graphics size from beam graphic and container dimensions
        double maxGraphicHeight = 84;
        double minimumGraphicHeight = 14;
        acceptedGraphicHeightRange = new double[] {minimumGraphicHeight, maxGraphicHeight};
    }
}