package com.wecca.canoeanalysis.controllers;

import com.jfoenix.controls.JFXTreeView;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.*;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.models.*;
import com.wecca.canoeanalysis.services.*;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.ControlUtils;
import com.wecca.canoeanalysis.utils.GraphicsUtils;
import com.wecca.canoeanalysis.utils.TestData;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
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
    private JFXTreeView<String> loadsTreeView;

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

    @Getter @Setter
    private MainController mainController;
    @Getter @Setter
    private Canoe canoe; // entity class that models the canoe as a beam
    private Beam beam; // The graphic of the beam

    // Size rules for load graphics (prevents them from rendering too big/small)
    private double[] acceptedGraphicHeightRange;

    /**
     * Toggles settings for empty load list
     * Includes a placeholder list item, enabled/disabled status of buttons, and styling
     */
    public void enableEmptyLoadTreeSettings(boolean enable) {
        LoadTreeManagerService.enableEmptyTreeFiller(enable, "View loads here");
        if (!enable) {
            loadsTreeView.setStyle("-fx-font-weight: normal");
            deleteLoadButton.setDisable(false);
            clearLoadsButton.setDisable(false);
        }
        else {
            // Apply settings
            loadsTreeView.setStyle("-fx-font-weight: bold");
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
        // Layering priority is TriangleStands => above Arrows => above ArrowBoxes => above Curves
        int viewOrder = Integer.MAX_VALUE;
        for (Node node : loadContainerChildren) {
            if (node instanceof Curve)
                node.setViewOrder(viewOrder--);
        }
        for (Node node : loadContainerChildren) {
            if (node instanceof ArrowBox)
                node.setViewOrder(viewOrder--);
        }
        for (Node node : loadContainerChildren) {
            if (node instanceof Arrow)
                node.setViewOrder(viewOrder--);

        }
        for (Node node : loadContainerChildren) {
            if (node instanceof TriangleStand)
                node.setViewOrder(viewOrder--);
        }
    }

    /**
     * Highlights the selected load in the UI
     * Called by the list view of loads when a load is selected
     */
    public void highlightLoad()
    {
        int selectedIndex = LoadTreeManagerService.getSelectedLoadId();
        updateViewOrder();

        // Color the selected load red and color the others black
        for (int i = 0; i < loadContainer.getChildren().size(); i++)
        {

            // Recolor the selected graphic
            Graphic colorableGraphic = (Graphic) loadContainer.getChildren().get(i);
            if (i != selectedIndex)
                colorableGraphic.recolor(false);
            else
            {
                colorableGraphic.recolor(true);

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
        axisLabelR.setLayoutX(581); // TODO: this will not be hard coded anymore once axis labels for new loads are implemented
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
        if (InputParsingService.validateTextAsDouble(canoeLengthTextField.getText())) {
            // Default list view message
            enableEmptyLoadTreeSettings(true);

            // Convert to metric
            double len = InputParsingService.getDistanceConverted(canoeLengthComboBox, canoeLengthTextField);

            // Only allow lengths in the specified range
            if (len >= 0.01) {
                // Update model state to default simple rectangular prism geometry
                canoe.setHull(new Hull(len, 0.4, 0.7));

                // Change the label on the scale
                axisLabelR.setText(String.format("%.2f m", canoe.getHull().getLength()));
                axisLabelR.setLayoutX(565); // TODO: this will not be hard coded anymore once axis labels for new loads are implemented

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

                // TODO: FOR TESTING NOT PERMANENT, NEED TO DELETE
                if (len == 6.0) {
                    System.out.println("Hull has been set to shark bait");
                    canoe.setHull(TestData.generateSharkBaitHull());
                    LoadTreeManagerService.buildLoadTreeView(canoe);
                    System.out.println("Hull mass = " + canoe.getHull().getMass());
                }
            }
            // Populate the alert telling the user the length they've entered is out of the allowed range
            else
                mainController.showSnackbar("Length must be at least 0.01m");
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
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
        if (InputParsingService.allTextFieldsAreDouble(Arrays.asList(pointLocationTextField, pointMagnitudeTextField)))
        {
            double x = InputParsingService.getDistanceConverted(pointLocationComboBox, pointLocationTextField);
            double mag = InputParsingService.getLoadConverted(pointMagnitudeComboBox, pointMagnitudeTextField);
            String direction = pointDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}

            // Validate the load is being added within the length of the canoe
            if (!(0 <= x && x <= canoe.getHull().getLength()))
                mainController.showSnackbar("Load must be contained within the canoe's length");
            // Validate the load is in the accepted magnitude range
            else if (Math.abs(mag) < 0.01)
                mainController.showSnackbar("Load magnitude must be at least 0.01kN");

            else
            {
                // Removes the default list view message if this is the first load
                enableEmptyLoadTreeSettings(false);

                // Add the load to canoe, and the load arrow on the GUI
                PointLoad p = new PointLoad(mag, x, false);
                AddLoadResult addResult = canoe.addLoad(p);
                renderPointLoadGraphic(p, addResult);
                LoadTreeManagerService.buildLoadTreeView(canoe);
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
        if (InputParsingService.allTextFieldsAreDouble(Arrays.asList(distributedMagnitudeTextField, distributedIntervalTextFieldL,
                distributedIntervalTextFieldR)))
        {
            double x = InputParsingService.getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldL);
            double xR = InputParsingService.getDistanceConverted(distributedIntervalComboBox, distributedIntervalTextFieldR);
            double mag = InputParsingService.getLoadConverted(distributedMagnitudeComboBox, distributedMagnitudeTextField);
            String direction = distributedDirectionComboBox.getSelectionModel().getSelectedItem();

            // Apply direction
            if (Objects.equals(direction, "Down")) {mag *= -1;}

            // User entry validations
            if (!(0 <= x && xR <= canoe.getHull().getLength()))
                mainController.showSnackbar("Load must be contained within the canoe's length");
            else if (!(xR > x))
                mainController.showSnackbar("Right interval bound must be greater than the left bound");
            else if (Math.abs(mag) < 0.01)
                mainController.showSnackbar("Load magnitude must be at least 0.01kN");

            else
                {
                    // Removes the default list view message if this is the first load
                    enableEmptyLoadTreeSettings(false);

                    // Add the load to canoe, and update ui state
                    UniformLoadDistribution d = new UniformLoadDistribution(mag, x, xR);
                    mainController.closeSnackBar(mainController.getSnackbar());
                    canoe.addLoad(d);
                    renderLoadGraphics();
                    LoadTreeManagerService.buildLoadTreeView(canoe);
                }
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    // Will eventually have validations on user input for the piecewise, unused parameter for now
    public void addPiecewise(PiecewiseContinuousLoadDistribution piecewise) {
        // Clear previous alert labels
        mainController.closeSnackBar(mainController.getSnackbar());

        // Removes the default list view message if this is the first load
        enableEmptyLoadTreeSettings(false);

        // Add the load to canoe, and update ui state
        renderLoadGraphics();
        LoadTreeManagerService.buildLoadTreeView(canoe);
    }

    /**
     * Add a point load to the canoe object and JavaFX UI.
     * @param pLoad the point load to be added.
     */
    private void renderPointLoadGraphic(PointLoad pLoad, AddLoadResult result)
    {
        // x coordinate in beamContainer for load
        double scaledX = GraphicsUtils.getScaledFromModelToGraphic(pLoad.getX(), beam.getWidth(), canoe.getHull().getLength()); // x position in the beamContainer

        // Render the correct graphic
        if (pLoad.isSupport())
            renderSupportGraphic(scaledX);
        else
            renderArrowGraphic(pLoad, result);
    }

    /**
     * Add a point load to the load container as a graphic of a triangle to represent a pinned support
     * @param beamContainerX the x coordinate of the load within the load container
     */
    private void renderSupportGraphic(double beamContainerX)
    {
        // Create the list of current load graphics
        List<Graphic> loadContainerChildren = new ArrayList<>(loadContainer.getChildren().stream()
                .map(node -> (Graphic) node).toList());

        // Create and add the support graphic
        double tipY = acceptedGraphicHeightRange[1] + (int) beam.getThickness(); // +126
        TriangleStand support = new TriangleStand(beamContainerX, tipY);
        loadContainerChildren.add(support);

        // Clear graphics the load container and add the new list of load graphics including the support, all sorted
        loadContainer.getChildren().clear();
        loadContainer.getChildren().addAll(loadContainerChildren.stream()
                .sorted(Comparator.comparingDouble(Graphic::getX))
                .map(load -> (Node) load).toList());
    }

    /**
     * Add a point load to the load container as a graphic of an arrow to represent the load
     * @param pLoad the point load to add
     * @param result the enum result of adding the load
     */
    private void renderArrowGraphic(PointLoad pLoad, AddLoadResult result)
    {
        // Notify the user regarding point loads combining or cancelling
        mainController.closeSnackBar(mainController.getSnackbar());
        if (result == AddLoadResult.COMBINED)
            mainController.showSnackbar("Point load magnitudes combined");
        else if (result == AddLoadResult.REMOVED)
            mainController.showSnackbar("Point load magnitudes cancelled");

        // Prevent rendering issues with zero-valued loads
        if (pLoad.getMaxSignedValue() == 0)
            return;

        renderLoadGraphics();
    }

    /**
     * Rescales all point loads (arrows) and distributed loads (arrow boxes) based on the load with the highest mag.
     * Point and distributed load magnitudes are compared although their units differ (Force vs. Force / Length)
     */
    public void renderLoadGraphics()
    {
        // Clear load container of graphics
        loadContainer.getChildren().clear();

        // Rescale all graphics relative to the max load
        List<Graphic> rescaledGraphics = new ArrayList<>();
        for (int i = 0; i < canoe.getAllLoads().size(); i++)
        {
            Load load = canoe.getAllLoads().get(i);

            // The ratio of the largest load (always rendered at max size) to this load
            double val = load.getMaxSignedValue();
            double loadMagnitudeRatio = Math.abs(load.getMaxSignedValue() / canoe.getMaxLoadValue());

            // Clip load length if too small (i.e. ratio is too large)
            if (loadMagnitudeRatio < Math.abs(acceptedGraphicHeightRange[0] / acceptedGraphicHeightRange[1]))
                loadMagnitudeRatio = Math.abs(acceptedGraphicHeightRange[0] / acceptedGraphicHeightRange[1]);

            // Render at scaled size (deltaY calculates the downscaling factor)
            double endY = load.getMaxSignedValue() < 0 ? acceptedGraphicHeightRange[1] : acceptedGraphicHeightRange[1] + (int) beam.getThickness();
            double deltaY = (acceptedGraphicHeightRange[1] - acceptedGraphicHeightRange[0]) * loadMagnitudeRatio;
            double startY = load.getMaxSignedValue() < 0 ? acceptedGraphicHeightRange[1] - deltaY : acceptedGraphicHeightRange[1] + (int) beam.getThickness() + deltaY;
            double xScaled = GraphicsUtils.getScaledFromModelToGraphic(load.getX(), beam.getWidth(), canoe.getHull().getLength());

            switch (load)
            {
                case PointLoad ignoredPLoad -> rescaledGraphics.add(new Arrow(xScaled, startY, xScaled, endY));
                case LoadDistribution dist -> {
                    double rxScaled = GraphicsUtils.getScaledFromModelToGraphic(dist.getSection().getRx(), beam.getWidth(), canoe.getHull().getLength());
                    switch (dist) {
                        case UniformLoadDistribution ignoredDLoad -> rescaledGraphics.add(new ArrowBox(xScaled, startY, rxScaled, endY));
                        case PiecewiseContinuousLoadDistribution piecewise -> {
                            if (piecewise.getForce() != 0)
                                rescaledGraphics.add(new Curve(piecewise.getPiecedFunction(), piecewise.getSection(), xScaled, rxScaled, startY, endY));
                        }
                        default -> throw new IllegalStateException("Invalid load type");
                    }
                }
                default -> throw new IllegalArgumentException("Invalid load type");
            }
        }

        rescaledGraphics.sort(Comparator.comparingDouble(Graphic::getX));
        loadContainer.getChildren().addAll((rescaledGraphics.stream().map(element -> (Node) element)).toList());
        updateViewOrder();
    }

    /**
     * Handler for the "solve system" button.
     * Distributes call to appropriate method and re-renders loads.
     */
    public void solveSystem() {
        // Removes the default list view message if this is the first load
        enableEmptyLoadTreeSettings(false);

        // Controls enabling/disabling for UX
        generateGraphsButton.setDisable(false);
        disableLoadingControls(true);
        loadsTreeView.setDisable(false);

        if (standsRadioButton.isSelected()) {
            solveStandSystem();
            solveSystemButton.setOnAction(e -> undoStandsSolve());
        }
        else if (floatingRadioButton.isSelected()) {
            solveFloatingSystem();
            solveSystemButton.setOnAction(e -> undoFloatingSolve());
        }
        else if (submergedRadioButton.isSelected()) {
            solveSubmergedSystem();
            solveSystemButton.setOnAction(e -> undoSubmergedSolve());
        }

        // Update UI to disable loading and enable undo solve functionality
        solveSystemButton.setText("Undo Solve");
        solveSystemButton.setDisable(false);

        // Update state
        LoadTreeManagerService.buildLoadTreeView(canoe);
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
        for (PointLoad supportLoad : supportLoads) {
            AddLoadResult addResult = canoe.addLoad(supportLoad);
            renderPointLoadGraphic(supportLoad, addResult);
        }
    }

    private void undoStandsSolve()
    {
        undoSolveUpdateUI();

        // Simulate the user selecting and deleting the supports which are always the first and last load
        loadsTreeView.getSelectionModel().select(0);
        deleteSelectedLoad();
        loadsTreeView.getSelectionModel().select(loadsTreeView.getRoot().getChildren().size() - 1); //TODO check if needs change (probably)
        deleteSelectedLoad();
    }

    private void solveFloatingSystem()
    {
        PiecewiseContinuousLoadDistribution buoyancy = SolverService.solveFloatingSystem(canoe);
        canoe.getExternalLoadDistributions().add(buoyancy);
        addPiecewise(buoyancy);
    }

    private void undoFloatingSolve()
    {
        // TODO: Implement (after solveFloatingSystem())
        undoSolveUpdateUI();
    }

    private void solveSubmergedSystem()
    {
        // TODO: Implement (consult Design & Analysis)
        mainController.showSnackbar("This has not yet been implemented - Tyler :-)");
    }

    private void undoSubmergedSolve()
    {
        // TODO: Implement (after solveSubmergedSystem())
        undoSolveUpdateUI();
    }

    private void undoSolveUpdateUI()
    {
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
        List<Control> controls = new ArrayList<>(Arrays.asList
        (
            solveSystemButton, pointLoadButton, distributedLoadButton, floatingRadioButton, standsRadioButton,
            submergedRadioButton, distributedMagnitudeComboBox, distributedMagnitudeTextField, pointMagnitudeComboBox,
            pointMagnitudeTextField, pointLocationTextField, pointLocationComboBox, pointDirectionComboBox,
            distributedIntervalTextFieldL, distributedIntervalTextFieldR, distributedIntervalComboBox,
            distributedDirectionComboBox, deleteLoadButton, clearLoadsButton, loadsTreeView, pointDirectionLabel,
            pointMagnitudeLabel, pointLocationLabel, pointTitleLabel, supportTitleLabel, distributedDirectionLabel,
            distributedMagntiudeLabel, distributedIntervalLabel, distributedTitleLabel
        ));

        for (Button button : mainController.getModuleToolBarButtons()) {
            controls.add(button);
            double opacity = b ? 0.4 : 1.0;
            button.setStyle("-fx-opacity: " + opacity);
        }

        for (Control control : controls) {control.setDisable(b);}
    }

    /**
     * Generates an SFD and BMD based on the canoe's load state.
     */
    public void generateDiagram()
    {
        WindowManagerService.openDiagramWindow("Shear Force Diagram", canoe, DiagramService.generateSfdPoints(canoe), "Force [kN]");
        WindowManagerService.openDiagramWindow("Bending Moment Diagram", canoe, DiagramService.generateBmdPoints(canoe), "Moment [kN·m]");
    }

    /**
     * Delete the load selected in the list view
     */
    public void deleteSelectedLoad()
    {
        int selectedIndex = LoadTreeManagerService.getSelectedLoadId();

        // Handle case that no index was selected
        if (selectedIndex == -1)
        {
            mainController.showSnackbar("Cannot perform delete, no load selected");
            return;
        }

        loadContainer.getChildren().remove(selectedIndex);
        canoe.removeLoad(selectedIndex);
        LoadTreeManagerService.buildLoadTreeView(canoe);

        // If the list is empty, toggle empty list settings
        if (loadContainer.getChildren().isEmpty())
            enableEmptyLoadTreeSettings(true);
    }

    /**
     * Clear all loads
     */
    public void clearLoads()
    {
        loadContainer.getChildren().clear();
        canoe.getExternalLoadDistributions().clear();;
        canoe.getExternalLoads().clear();
        canoe.setHull(null);
        LoadTreeManagerService.buildLoadTreeView(canoe);

        // List is empty, toggle empty list settings
        enableEmptyLoadTreeSettings(true);
    }

    /**
     * Upload a YAML file representing the Canoe object model
     * This populates the list view and beam graphic with the new model
     */
    public void uploadCanoe() throws IOException {
        MarshallingService.setBeamController(this);

        // Alert the user they will be overriding the current loads on the canoe by uploading a new one
        if (!canoe.getExternalLoads().isEmpty())
        {
            UploadAlertController.setBeamController(this);
            WindowManagerService.openUtilityWindow("Alert", "view/upload-alert-view.fxml", 350, 230);
        }
        else
            MarshallingService.importCanoeFromYAML(mainController.getPrimaryStage());
    }

    public void setUploadedCanoe(Canoe uploadedCanoe)
    {
        if (uploadedCanoe != null)
        {
            // Update the canoe model
            canoe = uploadedCanoe;

            // Update UI to new canoe
            enableEmptyLoadTreeSettings(uploadedCanoe.getExternalLoads().isEmpty());
            renderLoadGraphics();
            LoadTreeManagerService.buildLoadTreeView(canoe);
            axisLabelR.setText(String.format("%.2f m", canoe.getHull().getLength()));

            // Notify the user of the result
            mainController.showSnackbar("Successfully uploaded Canoe Model");
        }
    }

    /**
     * Download the Canoe object model with the loads currently on the canoe as a YAML file
     * This can be uploaded later with uploadCanoe() or manually modified
     */
    public void downloadCanoe() throws IOException {
        File downloadedFile = MarshallingService.exportCanoeToYAML(canoe, mainController.getPrimaryStage());

        String message = downloadedFile != null ? "Successfully downloaded canoe as \"" + downloadedFile.getName()
                + "\" to " + downloadedFile.getParentFile().getName() : "Download cancelled";

        mainController.showSnackbar(message);
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
        downloadCanoeButton.setOnAction(event -> {try {downloadCanoe();} catch (IOException e) {throw new RuntimeException(e);}});
        FontAwesomeIcon downloadIcon = new FontAwesomeIcon();
        downloadIcon.setFill(ColorPaletteService.getColor("white"));
        downloadIcon.setGlyphName("ARROW_CIRCLE_O_DOWN");
        downloadIcon.setSize("25");
        downloadCanoeButton.setGraphic(downloadIcon);
        beamModuleButtons.add(downloadCanoeButton);

        Button uploadCanoeButton = new Button();
        uploadCanoeButton.getStyleClass().add("transparent-button");
        uploadCanoeButton.setOnAction(event -> {try {uploadCanoe();} catch (IOException e) {throw new RuntimeException(e);}});
        FontAwesomeIcon uploadIcon = new FontAwesomeIcon();
        uploadIcon.setFill(ColorPaletteService.getColor("white"));
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
        // Module init
        setMainController(CanoeAnalysisApplication.getMainController());
        addModuleToolBarButtons();
        canoe = new Canoe();

        // Controls init
        disableLoadingControls(true);
        generateGraphsButton.setDisable(true);

        // Load tree init
        LoadTreeManagerService.setLoadsTreeView(loadsTreeView);
        enableEmptyLoadTreeSettings(true);
        JFXDepthManager.setDepth(loadsTreeView, 4);

        // Radio Buttons init
        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{standsRadioButton, floatingRadioButton, submergedRadioButton};
        ControlUtils.addAllRadioButtonsToToggleGroup(canoeSupportToggleGroup, canoeSupportRButtons, 0);

        // Combo Boxes init
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

        // Text field init
        TextField[] tfs = new TextField[]{pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
                distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField};
        for (TextField tf : tfs) {tf.setText("0.00");}

        // Beam init
        beam = new Beam(0, 84, beamContainer.getPrefWidth(), 25);
        JFXDepthManager.setDepth(beam, 4);
        beamContainer.getChildren().add(beam);

        // Graphics static field init
        double maxGraphicHeight = 84;
        double minimumGraphicHeight = 14;
        acceptedGraphicHeightRange = new double[] {minimumGraphicHeight, maxGraphicHeight};
    }
}