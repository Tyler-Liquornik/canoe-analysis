package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTreeView;
import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.controls.LoadTreeItem;
import com.wecca.canoeanalysis.components.graphics.*;
import com.wecca.canoeanalysis.controllers.popups.*;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.HeavisideStep;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.services.*;
import com.wecca.canoeanalysis.utils.*;
import javafx.event.ActionEvent;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.*;
import javafx.scene.shape.Rectangle;
import lombok.*;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Primary controller for longitudinal analysis of a beam
 */
public class BeamController implements Initializable {
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
    @FXML @Getter
    private AnchorPane loadContainer, beamContainer;

    @Getter @Setter
    private MainController mainController;
    @Getter
    private Canoe canoe;
    @Getter @Setter
    private CurvedProfile canoeGraphic;

    /**
     * Toggles settings for empty tree view if it's considered empty (placeholder doesn't count)
     */
    public void checkAndSetEmptyLoadTreeSettings() {
        boolean isTreeViewEmpty = LoadTreeManagerService.isTreeViewEmpty();
        LoadTreeItem root = LoadTreeManagerService.getRoot();
        if (isTreeViewEmpty) {
            root.getChildren().clear();
            root.getChildrenLoadItems().clear();
            root.addChild(new LoadTreeItem(-1, -1, "View Loads Here"));
        }
        String fontWeight = isTreeViewEmpty ? "bold" : "normal";
        loadsTreeView.setStyle("-fx-font-weight: " + fontWeight);
        loadsTreeView.setRoot(root);
        loadsTreeView.setShowRoot(false);
        deleteLoadButton.setDisable(isTreeViewEmpty);
        clearLoadsButton.setDisable(isTreeViewEmpty);
    }

    /**
     * Updates the view order (z-axis rendering) property of all graphics for rendering
     * Supports are above point loads which are above distributed loads as the preferred order
     */
    public void updateViewOrder() {
        // Create the list of current load graphics
        List<Node> loadContainerChildren = loadContainer.getChildren().stream().toList();

        // Apply the unique view order for each node based on type
        // Layering priority is TriangleStands => above Arrows => above ArrowBoundCurves => above Curves
        int viewOrder = Integer.MAX_VALUE;
        for (Node node : loadContainerChildren) {
            if (node instanceof Curve)
                node.setViewOrder(viewOrder--);
        }
        for (Node node : loadContainerChildren) {
            if (node instanceof ArrowBoundCurve)
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
    public void highlightLoad() {
        int selectedIndex = LoadTreeManagerService.getSelectedLoadId();
        updateViewOrder();

        // Color the selected load red and color the others black
        for (int i = 0; i < loadContainer.getChildren().size(); i++) {
            // Recolor the selected graphic
            Graphic graphic = (Graphic) loadContainer.getChildren().get(i);
            if (i != selectedIndex)
                graphic.recolor(false);
            else {
                graphic.recolor(true);

                // Bring the graphic to the front of the viewing order
                Node node = (Node) graphic;
                node.setViewOrder(-1);
            }
        }
    }

    /**
     * Resets the length of the canoe, clearing all loads
     */
    public void resetCanoe() {
        if (Objects.equals(solveSystemButton.getText(), "Undo Solve")) // TODO: fix lazy implementation, should manage the state properly
            solveSystemButton.fire();

        clearCanoeModel();
        axisLabelR.setText("X");
        axisLabelR.setLayoutX(581); // TODO: this will not be hard coded anymore once axis labels for new loads are implemented
        canoeLengthTextField.setDisable(false);
        canoeLengthComboBox.setDisable(false);
        lengthLabel.setDisable(false);
        deleteLoadButton.setDisable(true);
        clearLoadsButton.setDisable(true);
        disableLoadingControls(true);
        mainController.disableModuleToolBarButton(true, 0);
        setCanoeLengthButton.setText("Set Length");
        setCanoeLengthButton.setOnAction(e -> setLength());
    }

    /**
     * Updates both the model and the UI, showing the length of the canoe
     */
    public void setLength() {
        if (InputParsingService.validateTextAsDouble(canoeLengthTextField.getText())) {

            // Convert to metric
            double length = InputParsingService.getDistanceConverted(canoeLengthComboBox, canoeLengthTextField);

            // Only allow lengths in the specified range
            if (length >= 0.01) {
                // Update model state to default simple rectangular prism geometry
                canoe.setHull(SharkBaitHullLibrary.generateDefaultHull(length));

                // Change the label on the scale
                axisLabelR.setText(String.format("%.2f m", canoe.getHull().getLength()));
                axisLabelR.setLayoutX(565); // TODO: this will not be hard coded anymore once axis labels are implemented

                // Clear potential alert and reset access to controls
                mainController.closeSnackBar(mainController.getSnackbar());
                disableLoadingControls(false);
                canoeLengthTextField.setDisable(true);
                canoeLengthComboBox.setDisable(true);
                lengthLabel.setDisable(true);
                deleteLoadButton.setDisable(true);
                clearLoadsButton.setDisable(true);

                // Set length button will now function as a reset length button
                setCanoeLengthButton.setText("Reset Canoe");
                setCanoeLengthButton.setOnAction(e -> resetCanoe());

                checkAndSetEmptyLoadTreeSettings();
            }
            // Populate the alert telling the user the length they've entered is out of the allowed range
            else
                mainController.showSnackbar("Length must be at least 0.01m");
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Validate and add a distributed point based on filled fields
     */
    public void addPointLoad() {
        // Clear previous alert label
        mainController.closeSnackBar(mainController.getSnackbar());

        // Validate the entered numbers are doubles
        if (InputParsingService.allTextFieldsAreDouble(Arrays.asList(pointLocationTextField, pointMagnitudeTextField))) {
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

            else {
                // Add the load to canoe, and the load arrow on the GUI
                PointLoad p = new PointLoad(mag, x, false);
                AddLoadResult addResult = canoe.addLoad(p);
                displayAddResults(p, addResult);
                renderPointLoadGraphic(p);
                LoadTreeManagerService.buildLoadTreeView(canoe);
                checkAndSetEmptyLoadTreeSettings();
            }

        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Validate and add a distributed load based on filled fields
     */
    public void addDistributedLoad() {
        // Clear previous alert labels
        mainController.closeSnackBar(mainController.getSnackbar());

        // Validate the entered numbers are doubles
        if (InputParsingService.allTextFieldsAreDouble(Arrays.asList(distributedMagnitudeTextField, distributedIntervalTextFieldL,
                distributedIntervalTextFieldR))) {
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

            else {
                // Add the load to canoe, and update UI state
                UniformLoadDistribution d = new UniformLoadDistribution(mag, x, xR);
                AddLoadResult addResult = canoe.addLoad(d);
                displayAddResults(d, addResult);
                renderGraphics();
                LoadTreeManagerService.buildLoadTreeView(canoe);
                checkAndSetEmptyLoadTreeSettings();
            }
        }
        else
            mainController.showSnackbar("One or more entered values are not valid numbers");
    }

    /**
     * Add and validate a piecewise load distribution
     * Note: the user cannot manually add a custom load distribution (only hull and solved buoyancy)
     * Thus there are no user input validations at the moment
     * @param piecewise the distribution to validate and add
     */
    public void addPiecewiseLoadDistribution(PiecewiseContinuousLoadDistribution piecewise) {
        canoe.addLoad(piecewise);

        // Clear previous alert labels
        mainController.closeSnackBar(mainController.getSnackbar());

        // Add the load to canoe, and update ui state
        renderGraphics();
        LoadTreeManagerService.buildLoadTreeView(canoe);
        checkAndSetEmptyLoadTreeSettings();
    }

    /**
     * Add a point load to the canoe object and JavaFX UI.
     * @param pLoad the point load to be added.
     */
    private void renderPointLoadGraphic(PointLoad pLoad) {
        // x coordinate in beamContainer for load
        double scaledX = GraphicsUtils.getScaledFromModelToGraphic(pLoad.getX(), canoeGraphic.getEncasingRectangle().getWidth(), canoe.getHull().getLength()); // x position in the beamContainer

        // Render the correct graphic
        if (pLoad.isSupport())
            renderSupportGraphic(scaledX);
        else
            renderGraphics();;
    }

    /**
     * Add a point load to the load container as a graphic of a triangle to represent a pinned support
     * @param beamContainerX the x coordinate of the load within the load container
     */
    private void renderSupportGraphic(double beamContainerX) {
        // Create the list of current load graphics
        List<Graphic> loadContainerChildren = new ArrayList<>(loadContainer.getChildren().stream()
                .map(node -> (Graphic) node).toList());

        // Create and add the support graphic
        double tipY = GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + (int) canoeGraphic.getHeight(beamContainerX); // +126
        TriangleStand support = new TriangleStand(beamContainerX, tipY);
        loadContainerChildren.add(support);

        // Clear graphics the load container and add the new list of load graphics including the support, all sorted
        loadContainer.getChildren().clear();
        GraphicsUtils.sortGraphics(loadContainerChildren);
        loadContainer.getChildren().addAll(loadContainerChildren.stream()
                .map(load -> (Node) load).toList());
    }

    /**
     * Rescales all point loads based on the load with the highest max abs value
     * Point and distributed load magnitudes are compared although their units differ (Force vs. Force / Length)
     */
    public void renderGraphics() {
        loadContainer.getChildren().clear();
        double canoeGraphicLength = canoeGraphic.getEncasingRectangle().getWidth();

        // Rescale all graphics relative to the max load
        List<Graphic> rescaledGraphics = new ArrayList<>();
        for (Load load : canoe.getAllLoads()) {

            // The ratio of the largest load to this load (always 1 if the canoe has no other loads)
            double loadMagnitudeRatio = LoadUtils.getLoadMagnitudeRatio(canoe, load);
            double loadMax = load.getMaxSignedValue();
            double hullAbsMax = canoe.getHull().getPiecedSideProfileCurve().getMaxValue(canoe.getHull().getSection());

            // Render at scaled size (deltaY calculates the downscaling factor)
            double x = GraphicsUtils.getScaledFromModelToGraphic(load.getX(), canoeGraphicLength, canoe.getHull().getLength());
            double endY = loadMax < 0 ? GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] : GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + canoeGraphic.getHeight(load.getX());
            double deltaY = (GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] - GraphicsUtils.acceptedBeamLoadGraphicHeightRange[0]) * loadMagnitudeRatio;
            double startY = loadMax < 0 ? GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] - deltaY : GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + canoeGraphic.getHeight(load.getX()) + deltaY;

            // Render the correct graphic based on the subtype of the load
            switch (load) {
                case PointLoad ignoredPLoad -> rescaledGraphics.add(new Arrow(x, x, startY, endY));
                case LoadDistribution dist -> {
                    BoundedUnivariateFunction hullCurve = X -> canoe.getHull().getPiecedSideProfileCurve().value(X) - hullAbsMax;
                    double rx = GraphicsUtils.getScaledFromModelToGraphic(dist.getSection().getRx(), canoeGraphicLength, canoe.getHull().getLength());
                    double deltaX = rx - x;
                    double endRy = loadMax < 0 ? GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] : GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + canoeGraphic.getHeight(dist.getSection().getRx());
                    double startRy = loadMax < 0 ? GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] - deltaY : GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + canoeGraphic.getHeight(dist.getSection().getRx()) + deltaY;
                    double maxX = hullCurve.getMaxSignedValuePoint(dist.getSection()).getX();
                    double maxY = GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1] + canoeGraphic.getHeight(maxX) + deltaY;
                    double rectHeight = loadMax < 0 ? deltaY : maxY - Math.min(endY, endRy);
                    double rectY = loadMax < 0 ? startY : maxY;
                    double flip = loadMax < 0 ? 1 : -1;
                    Rectangle rect = new Rectangle(x, rectY, deltaX, rectHeight * flip);
                    switch (dist) {
                        case UniformLoadDistribution ignoredDLoad -> {
                            Arrow lArrow = new Arrow(x, x, startY, endY);
                            Arrow rArrow = new Arrow(rx, rx, startRy, endRy);
                            HeavisideStep step = new HeavisideStep(loadMax, dist.getX());


                            BoundedUnivariateFunction f = loadMax < 0 ? step : X -> step.value(X) - GraphicsUtils.getScaledFromModelToGraphic(hullCurve.value(X), step.getA() / loadMagnitudeRatio, hullAbsMax) / 2; // figure out not hardcoded, 2.7 for max scaled atm
                            rescaledGraphics.add(new ArrowBoundCurve(f, dist.getSection(), rect, lArrow, rArrow));
                        }
                        case PiecewiseContinuousLoadDistribution piecewise -> {
                            if (piecewise.getForce() != 0) {
                                BoundedUnivariateFunction f = X -> loadMax < 0 ? piecewise.getPiecedFunction().value(X) : piecewise.getPiecedFunction().value(X) - GraphicsUtils.getScaledFromModelToGraphic(hullCurve.value(X), piecewise.getX() / loadMagnitudeRatio, hullAbsMax) / 2;
                                rescaledGraphics.add(new Curve(f, piecewise.getSection(), rect));
                            }
                        }
                        default -> throw new IllegalStateException("Invalid load type");
                    }
                }
                default -> throw new IllegalArgumentException("Invalid load type");
            }
        }

        GraphicsUtils.sortGraphics(rescaledGraphics);
        loadContainer.getChildren().addAll((rescaledGraphics.stream().map(element -> (Node) element)).toList());
        updateViewOrder();
    }

    public void renderCanoeGraphic() {
        beamContainer.getChildren().clear();
        beamContainer.getChildren().add((Node) canoeGraphic);
    }

    /**
     * Display the less common results of adding the load (COMBINED | CANCELLED cases)
     * @param load the added load
     * @param result the result of adding the load
     */
    private void displayAddResults(Load load, AddLoadResult result) {
        mainController.closeSnackBar(mainController.getSnackbar());
        if (result == AddLoadResult.COMBINED)
            mainController.showSnackbar(load.getType().getDescription() + " magnitudes combined");
        else if (result == AddLoadResult.REMOVED)
            mainController.showSnackbar(load.getType().getDescription() + " magnitudes cancelled");
    }

    /**
     * Handler for the "solve system" button.
     * Distributes call to appropriate method and re-renders loads.
     */
    public void solveSystem() {
        // System solve with upward net force makes no sense
        if (canoe.getNetForce() > 0) {
            mainController.showSnackbar("Cannot solve the system with upward net force. Remove some loads and try again");
            return;
        }


        if (submergedRadioButton.isSelected()) {
            solveSubmergedSystem();
            return; // TODO: after implemented solveSubmergedSystem()
        }
        if (standsRadioButton.isSelected()) {
            solveStandSystem();
            solveSystemButton.setOnAction(e -> undoStandsSolve());
        }
        else if (floatingRadioButton.isSelected()) {
            if (canoe.getHull().getWeight() == 0) {
                mainController.showSnackbar("Cannot solve a buoyancy without a hull. Please build a hull first");
                mainController.flashModuleToolBarButton(0, 8000); // as a hint for the user
                return;
            }
            double maximumPossibleBuoyancyForce = BeamSolverService.getTotalBuoyancy(canoe, 0);
            if (-canoe.getNetForce() > maximumPossibleBuoyancyForce) {
                mainController.showSnackbar("Cannot solve buoyancy as there is too much load. The canoe will sink!");
                return;
            }
            if (!canoe.isSymmetricallyLoaded()) {
                mainController.showSnackbar("Cannot solve buoyancy for an asymmetrically loaded canoe. The canoe will tip over!");
                return;
            }
            solveFloatingSystem();
            solveSystemButton.setOnAction(e -> undoFloatingSolve());
        }

        // Update UI state
        generateGraphsButton.setDisable(false);
        disableLoadingControls(true);
        loadsTreeView.setDisable(false);
        solveSystemButton.setText("Undo Solve");
        solveSystemButton.setDisable(false);
        LoadTreeManagerService.buildLoadTreeView(canoe);
        checkAndSetEmptyLoadTreeSettings();
        deleteLoadButton.setDisable(true);
        clearLoadsButton.setDisable(true);
        mainController.disableModuleToolBarButton(true, 0);
        updateViewOrder();
    }

    /**
     * Solve and display the result of the "stand" system load case.
     * This entails two supports at each end of the canoe, symmetrically offset from the beam bounds
     */
    private void solveStandSystem() {
        List<PointLoad> supportLoads = BeamSolverService.solveStandSystem(canoe);
        for (PointLoad supportLoad : supportLoads) {
            canoe.addLoad(supportLoad);
            renderPointLoadGraphic(supportLoad);
        }
        checkAndSetEmptyLoadTreeSettings();
    }

    /**
     * Undo the effects of the stands solve
     */
    private void undoStandsSolve() {
        undoSolveUpdateUI();
        clearLoadsOfType(LoadType.POINT_LOAD_SUPPORT);
    }

    /**
     * Solve and display the result of the "floating" system load case.
     * This entails a buoyancy distribution that keeps the canoe afloat
     */
    private void solveFloatingSystem() {
        PiecewiseContinuousLoadDistribution buoyancy = BeamSolverService.solveFloatingSystem(canoe);
        if (!(buoyancy.getForce() == 0))
            addPiecewiseLoadDistribution(buoyancy);
    }

    /**
     * Undo the effects of the floating solve
     */
    private void undoFloatingSolve() {
        clearLoadsOfType(LoadType.BUOYANCY);
        renderGraphics(); // removing buoyancy likely changes scaling, need to recalculate scaling
        undoSolveUpdateUI();
    }

    /**
     * Solve and display the result of the "submerged" system load case.
     * This entails a canoe fully underwater
     */
    private void solveSubmergedSystem() {
        // TODO: Implement once engineering analysis methodologies developed
        mainController.showSnackbar("The team is still working on their methods for this! Try again later.");
    }

    /**
     * Undo the effects of the submerged solve
     */
    private void undoSubmergedSolve() {
        // TODO: Implement (after solveSubmergedSystem())
        undoSolveUpdateUI();
    }

    /**
     * Change the solve system button back into to a button to solve system instead of undo solve
     */
    private void undoSolveUpdateUI() {
        solveSystemButton.setText("Solve System");
        solveSystemButton.setOnAction(e -> solveSystem());
        generateGraphsButton.setDisable(true);
        LoadTreeManagerService.buildLoadTreeView(canoe);
        disableLoadingControls(false);
        boolean isHullPresent = canoe.getHull().getWeight() != 0;
        mainController.disableModuleToolBarButton(isHullPresent, 0);
        checkAndSetEmptyLoadTreeSettings();
    }

    /**
     * Bulk package to toggle enabling all the controls related to loading
     * @param b disables controls
     */
    private void disableLoadingControls(boolean b) {
        // Disable all the controls
        List<Control> controls = new ArrayList<>(Arrays.asList(
            solveSystemButton, pointLoadButton, distributedLoadButton, floatingRadioButton, standsRadioButton,
            submergedRadioButton, distributedMagnitudeComboBox, distributedMagnitudeTextField, pointMagnitudeComboBox,
            pointMagnitudeTextField, pointLocationTextField, pointLocationComboBox, pointDirectionComboBox,
            distributedIntervalTextFieldL, distributedIntervalTextFieldR, distributedIntervalComboBox,
            distributedDirectionComboBox, deleteLoadButton, clearLoadsButton, loadsTreeView, pointDirectionLabel,
            pointMagnitudeLabel, pointLocationLabel, pointTitleLabel, supportTitleLabel, distributedDirectionLabel,
            distributedMagntiudeLabel, distributedIntervalLabel, distributedTitleLabel
        ));

        for (Control control : controls) {
            control.setDisable(b);
        }
        mainController.disableAllModuleToolbarButton(b);

        // Only enable the hull builder if a custom hull hasn't yet been set
        if (canoe.getHull() != null) {
            boolean disableHullBuilder = !canoe.getHull().equals(
                    SharkBaitHullLibrary.generateDefaultHull(canoe.getHull().getLength()));
            mainController.disableModuleToolBarButton(disableHullBuilder, 0);
        }
    }

    /**
     * Generates an SFD and BMD based on the canoe's load state.
     */
    public void generateDiagram() {
        WindowManagerService.openDiagramWindow("Shear Force Diagram", canoe, DiagramService.generateSfdPoints(canoe), "Force [kN]");
        WindowManagerService.openDiagramWindow("Bending Moment Diagram", canoe, DiagramService.generateBmdPoints(canoe), "Moment [kN·m]");
    }

    /**
     * Delete the load selected in the LoadTreeView
     */
    public void deleteSelectedLoad() {
        LoadTreeItem selectedItem = LoadTreeManagerService.getSelectedLoadTreeItem();
        int selectedIndex = LoadTreeManagerService.getSelectedLoadId();

        // Handle case that no index was selected
        if (selectedIndex == -1) {
            mainController.showSnackbar("Cannot perform delete, no load selected");
            return;
        }

        if (selectedItem != null ) {
            // Select up the hierarchy of nested LoadTreeItems to the highest ancestor of the tree below the root
            if (selectedItem.isField())
                selectedItem = selectedItem.getParentLoadItem();
            if (selectedItem.isNested())
                selectedItem = selectedItem.getParentLoadItem();
            // Deleting the hull is a special case
            if (selectedItem.getLoad() != null && selectedItem.getLoad().getType() != null &&
                    selectedItem.getLoad().getType() == LoadType.HULL) {
                canoe.setHull(SharkBaitHullLibrary.generateDefaultHull(canoe.getHull().getLength()));
                resetCanoeGraphic();
                mainController.disableModuleToolBarButton(false, 0);
            }
            else {
                // If the hull is present in the treeView it will disrupt the order sync between canoe.loads and loadContainer.children, we need to adjust for this
                int hullWeightLoadId = LoadTreeManagerService.getHullLoadTreeItemLoadId();
                if (selectedItem.getLoadId() > hullWeightLoadId && hullWeightLoadId != -1) {
                    canoe.getLoads().remove(--selectedIndex);
                    selectedIndex++;
                }
                else
                    canoe.getLoads().remove(selectedIndex);
            }
        }
        loadContainer.getChildren().remove(selectedIndex);
        LoadTreeManagerService.buildLoadTreeView(canoe);
        renderGraphics();

        if (loadContainer.getChildren().isEmpty())
            checkAndSetEmptyLoadTreeSettings();
    }

    /**
     * Clear all loads of a specific load type.
     * This is achieved by simulating the user selecting and deleting the load.
     * @param loadType the type to delete loads of
     */
    private void clearLoadsOfType(LoadType loadType) {
        List<LoadTreeItem> treeItems = LoadTreeManagerService.getRoot().getChildren().stream()
                .map(item -> (LoadTreeItem) item)
                .toList();

        for (int i = 0; i < treeItems.size(); i++) {
            LoadTreeItem item = treeItems.get(i);
            if (item.getLoad() != null && item.getLoad().getType() == loadType) {
                loadsTreeView.getSelectionModel().select(item);
                deleteSelectedLoad();
                // Update treeItems list after deletion and restart the loop
                treeItems = LoadTreeManagerService.getRoot().getChildren().stream()
                        .map(treeItem -> (LoadTreeItem) treeItem)
                        .toList();
                i = -1;
            }
        }
    }


    public void clearCanoeModel() {
        loadContainer.getChildren().clear();
        canoe.getLoads().clear();
        Hull hull = SharkBaitHullLibrary.generateDefaultHull(canoe.getHull().getLength());
        canoe.setHull(hull);
        LoadTreeManagerService.buildLoadTreeView(canoe);
        checkAndSetEmptyLoadTreeSettings();
        resetCanoeGraphic();
    }

    public void resetCanoeGraphic() {
        Rectangle rect = new Rectangle(0, 84, beamContainer.getPrefWidth(), 25);
        setCanoeGraphic(new Beam(rect));
        beamContainer.getChildren().clear();
        beamContainer.getChildren().add((Node) canoeGraphic);
        renderCanoeGraphic();
    }

    /**
     * Use the hull curvature of the canoe to set the canoe graphic
     * @param canoe with teh hull curvature to create the graphic for
     */
    public void setCanoeGraphicFromCanoe(Canoe canoe) {
        Hull hull = canoe.getHull();
        Rectangle rect = canoeGraphic.getEncasingRectangle();
        rect.setHeight(35);
        setCanoeGraphic(new ClosedCurve(
                hull.getPiecedSideProfileCurve(), hull.getSection(), rect));
        beamContainer.getChildren().clear();
        beamContainer.getChildren().add((Node) canoeGraphic);
        renderCanoeGraphic();
    }

    /**
     * Upload a YAML file representing the Canoe object model
     * This populates the list view and beam graphic with the new model
     */
    public void uploadCanoe() {
        YamlMarshallingService.setBeamController(this);

        // Alert the user they will be overriding the current loads on the canoe by uploading a new one
        if (!canoe.getAllLoads().isEmpty()) {
            UploadAlertController.setBeamController(this);
            WindowManagerService.openUtilityWindow("Alert", "view/upload-alert-view.fxml", 350, 230);
        }
        else
            YamlMarshallingService.importCanoeFromYAML(mainController.getPrimaryStage());
    }

    /**
     * Set the canoe and sync with graphics and tree view models
     * @param canoe the canoe to set
     */
    public void setCanoe(Canoe canoe) {
        if (canoe != null) {
            // Update the canoe model
            this.canoe = canoe;

            // Update UI to new canoe
            renderGraphics();
            LoadTreeManagerService.buildLoadTreeView(this.canoe);
            axisLabelR.setText(String.format("%.2f m", this.canoe.getHull().getLength()));
            checkAndSetEmptyLoadTreeSettings();
        }
    }

    /**
     * Download the Canoe object model with the loads currently on the canoe as a YAML file
     * This can be uploaded later with uploadCanoe() or manually modified
     */
    public void downloadCanoe() {
        File downloadedFile = YamlMarshallingService.exportCanoeToYAML(canoe, mainController.getPrimaryStage());

        String message = downloadedFile != null ? "Successfully downloaded canoe as \"" + downloadedFile.getName()
                + "\" to " + downloadedFile.getParentFile().getName() : "Download cancelled";

        mainController.showSnackbar(message);
    }

    /**
     * Open the hull builder submodule (just a utility window for now until fully developed)
     */
    public void openHullBuilder() {
        HullBuilderController.setMainController(mainController);
        HullBuilderController.setBeamController(this);
        WindowManagerService.openUtilityWindow("Hull Builder Beta", "view/hull-builder-view.fxml", 350, 230);
    }

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     * Currently, this provides buttons to download and upload the Canoe object as JSON
     */
    public void addModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<ActionEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        iconGlyphToFunctionMap.put(IconGlyphType.WRENCH, e -> openHullBuilder());
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> downloadCanoe());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> uploadCanoe());
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    /**
     * Operations called on initialization of the view
     * @param url unused, part of javafx framework
     * @param resourceBundle unused, part of javafx framework
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Module init
        setMainController(CanoeAnalysisApplication.getMainController());
        addModuleToolBarButtons();
        canoe = new Canoe();

        // Load tree init
        LoadTreeManagerService.setBeamController(this);
        LoadTreeManagerService.setLoadsTreeView(loadsTreeView);
        LoadTreeManagerService.getRoot().getChildren().clear();
        checkAndSetEmptyLoadTreeSettings();
        JFXDepthManager.setDepth(loadsTreeView, 4);

        // Graphics init
        resetCanoeGraphic();

        double maxGraphicHeight = 84;
        double minimumGraphicHeight = 14;
        GraphicsUtils.acceptedBeamLoadGraphicHeightRange = new double[] {minimumGraphicHeight, maxGraphicHeight};

        // Controls init
        disableLoadingControls(true);
        generateGraphsButton.setDisable(true);

        ToggleGroup canoeSupportToggleGroup = new ToggleGroup();
        RadioButton[] canoeSupportRButtons = new RadioButton[]{standsRadioButton, floatingRadioButton, submergedRadioButton};
        ControlUtils.addAllRadioButtonsToToggleGroup(canoeSupportToggleGroup, canoeSupportRButtons, 0);

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

        TextField[] tfs = new TextField[]{pointMagnitudeTextField, pointLocationTextField, distributedMagnitudeTextField,
                distributedIntervalTextFieldL, distributedIntervalTextFieldR, canoeLengthTextField};
        for (TextField tf : tfs) {tf.setText("0.00");}
    }
}