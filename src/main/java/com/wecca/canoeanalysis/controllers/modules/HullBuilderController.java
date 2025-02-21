package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.CurvedGraphic;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.components.graphics.hull.CubicBezierSplineHullGraphic;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Range;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.HullGeometryService;
import com.wecca.canoeanalysis.services.MarshallingService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.GraphicsUtils;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * To whoever is coding here:
 *
 * You need to be extremely careful that everything here is very performant!
 * As the user turns the knobs, they fire off a high frequency of events (picture like a machine gun but instead of bullets it shoots events XD)
 * These events must be handled with low latency
 * Because you might have to calculate several integrals or something PER EVENT!!!!
 * Be smart with good DS&A knowledge, do not write slow code
 * Otherwise animations and UI controls become choppy and the UX is ruined
 */
@Traceable
public class HullBuilderController implements Initializable, ModuleController {

    // FXML UI Components
    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @FXML
    private Label intervalLabel, heightLabel, volumeLabel, massLabel, propertiesPanelTitleLabel,
            poiTitleLabel, poiDataLabel, poiModeLabel;

    // Refs
    @Getter @Setter
    private Hull hull;
    @Setter
    private static MainController mainController;
    private List<ChangeListener<Number>> knobListeners;
    private EventHandler<KeyEvent> keyPressedHandler;
    private EventHandler<KeyEvent> keyReleasedHandler;

    // Added UI & Graphics
    @Getter @Setter
    private AnchorPane hullGraphicPane;
    private CubicBezierSplineHullGraphic hullGraphic;
    private List<Knob> knobs;
    private Line mouseXTrackerLine;
    private Circle intersectionPoint;
    private Group intersectionXMark;
    private Line dragIndicatorLine;
    private AnchorPane intersectionPointPane;

    // State
    @Getter @Setter
    private boolean previousPressedBefore;
    @Getter @Setter
    private boolean nextPressedBefore;
    @Getter @Setter
    private HullSection selectedHullSection;
    @Getter @Setter
    private int selectedHullSectionIndex;
    @Getter @Setter
    private boolean sectionPropertiesSelected;
    @Getter @Setter
    private int graphicsViewingState;
    @Getter @Setter
    private boolean sectionEditorEnabled;
    @Getter @Setter
    private List<Range> overlaySections;
    @Getter @Setter
    private Point2D initialKnotDragMousePos;
    @Getter @Setter
    private Point2D initialKnotDragKnotPos;
    @Getter @Setter
    private Point2D newKnotDragKnotPos;
    @Getter @Setter
    private Hull knotDraggingPreviewHull;
    @Getter @Setter
    private long dragStartTime;
    @Getter @Setter
    private boolean isDraggingKnot;
    @Getter @Setter
    private boolean isDraggingKnotPreview;
    @Getter @Setter
    private double knotEditingCurrentMouseX;
    @Getter @Setter
    private double knotEditingCurrentMouseY;
    @Getter @Setter
    private boolean mousePressWasInAddingKnotPointZone;
    @Getter @Setter
    private boolean shiftKeyPressHadMouseInDeletingKnotPointZone;
    @Getter @Setter
    private boolean knotEditingMouseButtonDown;

    private final double HULL_VIEW_PANEL_HEIGHT = 45;
    // Note to developer: This does not have deep immutability. Do not mutate! Meant as a constant reference to SharkBait!
    private final Hull SHARK_BAIT_HULL = SharkBaitHullLibrary.generateSharkBaitHullScaledFromBezier(6);

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        // TODO: add functions to buttons
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.PENCIL, this::toggleSectionsEditorMode);
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    public void dummy() {
        mainController.showSnackbar("WIP");
    }

    public void dummyOnClick(MouseEvent event) {
        mainController.showSnackbar("WIP");
    }

    /**
     * Handler for the pencil button which enables the ability to add or delete knots.
     * The logic here is responsible for immediate UI/state changes
     * @param event the click event
     */
    private void toggleSectionsEditorMode(MouseEvent event) {
        sectionEditorEnabled = !sectionEditorEnabled;
        nextPressedBefore = false;
        previousPressedBefore = false;
        selectedHullSectionIndex = -1;

        updateSectionsEditorHullCurveOverlay();
        updateAddingKnotTitleLabel(isMouseInAddingKnotPointZone(mouseXTrackerLine.getStartX()));

        List<Button> toolBarButtons = mainController.getModuleToolBarButtons();
        Button toggledIconButton = IconButton.getToolbarButton(sectionEditorEnabled ? IconGlyphType.X__PENCIL : IconGlyphType.PENCIL, this::toggleSectionsEditorMode);
        toolBarButtons.set(2, toggledIconButton);
        mainController.addOrSetToolBarButtons(toolBarButtons);
        hullViewAnchorPane.setCursor(sectionEditorEnabled ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        hullViewAnchorPane.setOnMouseEntered(sectionEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.CROSSHAIR) : null);
        hullViewAnchorPane.setOnMouseExited(sectionEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.DEFAULT) : null);
        hullViewAnchorPane.setOnMouseMoved(sectionEditorEnabled ? this::handleKnotEditingMouseMoved : null);
        if (!sectionEditorEnabled) {
            if (intersectionPoint != null) {
                intersectionPoint.setOpacity(0);
                intersectionPoint = null;
            }
            hullViewAnchorPane.setOnMousePressed(null);
            intersectionXMark.setOpacity(0);
            poiTitleLabel.setText("");
            poiDataLabel.setText("");
        }
        else {
            if (sectionPropertiesSelected) switchButton(null); // Always show canoe properties first when in sections editor mode
            updateHullIntersectionPointDisplay(new Point2D(hull.getLength() / 2, hull.getMaxHeight()), null);
            hullViewAnchorPane.setOnMousePressed(this::handleKnotEditMousePressed);
        }

        if (selectedHullSection != null) hullGraphic.recolor(!sectionEditorEnabled);
        hullGraphic.setColoredSectionIndex(-1);

        List<Button> topLeftButtons = hullViewAnchorPane.getChildren().stream().filter(node -> (node instanceof Button)).map(node -> (Button) node).toList();
        List<Button> sectionSelectorButtons = Arrays.asList(topLeftButtons.get(0), topLeftButtons.get(1));
        sectionSelectorButtons.forEach(button -> button.setDisable(sectionEditorEnabled));
        sectionSelectorButtons.forEach(button -> button.setOpacity(1));

        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().removeListener(knobListeners.get(i));}
        if (selectedHullSection != null) {
            if (sectionEditorEnabled) {
                selectedHullSection = null;
                unboundKnobs();
                knobs.forEach(knobs -> knobs.setKnobValue(0));
                if (sectionPropertiesSelected) setBlankSectionProperties();
            }
            knobs.forEach(knob -> knob.setLocked(sectionEditorEnabled));
            if (!sectionEditorEnabled) {
                for (int i = 0; i < knobs.size(); i++) {
                    knobs.get(i).valueProperty().addListener(knobListeners.get(i));
                }
                if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                if (intersectionXMark != null) intersectionXMark.setOpacity(0);
            }
        }

        // Update mouse X tracker line visibility and style
        mouseXTrackerLine.setVisible(sectionEditorEnabled);
        if (sectionEditorEnabled) {
            mouseXTrackerLine.setStroke(ColorPaletteService.getColor("white"));
            mouseXTrackerLine.setOpacity(0.7);
            mouseXTrackerLine.getStrokeDashArray().setAll(5.0, 9.0);
            double halfWidth = hullViewAnchorPane.getWidth() / 2;
            mouseXTrackerLine.setStartX(halfWidth);
            mouseXTrackerLine.setEndX(halfWidth);
            updateMouseLinePoint(new Point2D(hullGraphicPane.getWidth() / 2, hullGraphicPane.getHeight()), isMouseInAddingKnotPointZone(halfWidth));
            mouseXTrackerLine.setStartY(1);
            mouseXTrackerLine.setEndY(hullViewAnchorPane.getHeight() - 1);
            hullViewAnchorPane.setOnMousePressed(this::handleKnotEditMousePressed);
            hullViewAnchorPane.setOnMouseDragged(this::handleKnotDragMouseDragged);
            hullViewAnchorPane.setOnMouseReleased(this::handleKnotDragMouseReleased);
        }
        else {
            hullViewAnchorPane.setOnMousePressed(null);
            hullViewAnchorPane.setOnMouseDragged(null);
            hullViewAnchorPane.setOnMouseReleased(null);
        }

        // Notify the user
        mainController.showSnackbar(sectionEditorEnabled
                ? "Sections Editor Enabled"
                : "Sections Editor Disabled");
    }

    /**
     * Updates the hull curve overlay to show regions between control points in each section.
     * The overlays are added to a dedicated `overlayPane` above the `hullGraphicPane`.
     * a list of x-intervals where the overlays were added (in function space, NOT graphic space) is added to state
     */
    private void updateSectionsEditorHullCurveOverlay() {
        List<Range> overlaySections = new ArrayList<>();
        boolean enableOverlays = !(hullGraphic.getChildren().getLast() instanceof CurvedGraphic);
        // Remove all CurvedGraphic overlays by iterating backwards through hullGraphic's children
        if (!enableOverlays) {
            for (int i = hullGraphic.getChildren().size() - 1; i >= 0; i--) {
                if (hullGraphic.getChildren().get(i) instanceof CurvedGraphic) hullGraphic.getChildren().remove(i);
                else break;
            }
        }
        else {
            // Each section is a candidate for an overlay if there's room
            List<CurvedGraphic> overlayCurves = new ArrayList<>();
            for (HullSection section : hull.getHullSections()) {
                if (!(section.getSideProfileCurve() instanceof CubicBezierFunction bezier))
                    throw new IllegalArgumentException("Can only work with Bezier Hulls");

                // Determine the x-range between the control points, which acts as the free section to place a knot point
                Range overlaySection;
                double lControlX = bezier.getControlX1();
                double rControlX = bezier.getControlX2();
                if (Math.abs(rControlX - lControlX) <= 5e-3 || rControlX - lControlX <= 5e-3)
                    overlaySection = new Range(lControlX, rControlX);
                else {
                    overlaySection = new Section(lControlX, rControlX);
                    double maxY = bezier.getMaxValue(new Section(lControlX, rControlX));
                    double minY = bezier.getMinValue(new Section(lControlX, rControlX));
                    Rectangle validAddKnotRectangle = new Rectangle(
                            (lControlX / hull.getLength()) * hullGraphicPane.getWidth(),
                            (minY / -hull.getMaxHeight()) * (HULL_VIEW_PANEL_HEIGHT * (hull.getMaxHeight() / SHARK_BAIT_HULL.getMaxHeight())),
                            ((rControlX - lControlX) / hull.getLength()) * hullGraphicPane.getWidth(),
                            ((Math.abs(maxY - minY)) / -hull.getMaxHeight()) * (HULL_VIEW_PANEL_HEIGHT * (hull.getMaxHeight() / SHARK_BAIT_HULL.getMaxHeight()))
                    );
                    CurvedGraphic overlayCurve = new CurvedGraphic(bezier, new Section(lControlX, rControlX), validAddKnotRectangle, false);
                    overlayCurve.getLinePath().setStrokeWidth(2.0);
                    overlayCurve.recolor(true);
                    overlayCurves.add(overlayCurve);
                }
                overlaySections.add(overlaySection);
            }
            overlayCurves.forEach(overlay -> overlay.setViewOrder(0));
            IntStream.range(0, 2).forEach(i -> hullGraphic.getChildren().get(i).setViewOrder(1));
            IntStream.range(2, hullGraphic.getChildren().size()).forEach(i -> hullGraphic.getChildren().get(i).setViewOrder(-1));
            overlayCurves.forEach(hullGraphic.getChildren()::add);
        }
        this.overlaySections = overlaySections;
    }

    /**
     * Displays the intersection point on the hull view pane.
     * @param position The position of the intersection point in the hull view pane's local coordinate space.
     */
    private void updateMouseLinePoint(Point2D position, boolean pointElseXMark) {
        if (!intersectionPointPane.getChildren().contains(intersectionPoint)) {
            intersectionPoint = new Circle(5);
            intersectionPoint.setFill(ColorPaletteService.getColor("white"));
            intersectionPointPane.getChildren().add(intersectionPoint);
        }
        if (pointElseXMark) {
            intersectionXMark.setOpacity(0);
            intersectionPoint.setCenterX(position.getX());
            intersectionPoint.setCenterY(position.getY());
            intersectionPoint.setOpacity(1.0);
        }
        else {
            intersectionPoint.setOpacity(0);
            intersectionXMark.setLayoutX(position.getX());
            intersectionXMark.setLayoutY(position.getY());
            intersectionXMark.setOpacity(1.0);
        }
    }


    /**
     * Display if the user is hovering their mouse in a section to add or delete a knot point
     * @param adding whether or knot the user is in a region where
     */
    private void updateAddingKnotTitleLabel(boolean adding) {
        if (!sectionEditorEnabled) return; // Cannot be adding/deleting a knot if not in section editor
        String addingOrDeleting = adding ? "Adding" : "Deleting";
        poiTitleLabel.setLayoutX(adding ? 751 : 748);
        poiTitleLabel.setText(String.format("%s Knot Point", addingOrDeleting));
    }

    /**
     * Check if we are in a position to add, or delete a knot point in section editor mode
     * @param mouseX the x position of the mouse
     */
    private boolean isMouseInAddingKnotPointZone(double mouseX) {
        if (!sectionEditorEnabled) return false;
        if (mouseX >= hullGraphicPane.getLayoutX() && mouseX <= hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth()) {
            double poiX = mouseX - hullGraphicPane.getLayoutX();
            double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
            return overlaySections.stream()
                    .anyMatch(section -> functionSpaceX >= section.getX() && functionSpaceX <= section.getRx());
        }
        else return false;
    }

    /**
     * Set the side view hull to build
     * @param hull to set from
     */
    public void setSideViewHullGraphic(Hull hull) {
        // Set and layout parent pane
        double sideViewPanelWidth = 700;
        double sideViewPanelHeight = HULL_VIEW_PANEL_HEIGHT * (hull.getMaxHeight() / SHARK_BAIT_HULL.getMaxHeight());
        double paneX = hullViewAnchorPane.prefWidth(-1) / 2 - sideViewPanelWidth / 2;
        double paneY = hullViewAnchorPane.prefHeight(-1) / 2 - HULL_VIEW_PANEL_HEIGHT / 2;
        hullGraphicPane.setPrefSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setMaxSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setMinSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setLayoutX(paneX);
        hullGraphicPane.setLayoutY(paneY);
        if (intersectionPointPane != null) {
            intersectionPointPane.setPrefHeight(sideViewPanelHeight);
            intersectionPointPane.setMaxHeight(sideViewPanelHeight);
            intersectionPointPane.setMinHeight(sideViewPanelHeight);
        }

        // Setup graphic
        Rectangle rect = new Rectangle(0, 0, sideViewPanelWidth, sideViewPanelHeight);
        List<CubicBezierFunction> beziers = hull.getHullSections().stream().map(section -> {
            BoundedUnivariateFunction sideProfileCurveFunction = section.getSideProfileCurve();
            if (sideProfileCurveFunction instanceof CubicBezierFunction bezier)
                return bezier;
            else
                throw new RuntimeException("Not a bezier hull");
        }).toList();
        hullGraphic = new CubicBezierSplineHullGraphic(beziers, rect, true);

        // Add graphic to pane
        hullGraphicPane.getChildren().clear();
        hullGraphicPane.getChildren().add(hullGraphic);
        hullViewAnchorPane.getChildren().set(1, hullGraphicPane);
        applyGraphicsViewingState();

        // Keep the selected hull section colored
        if (selectedHullSection != null && selectedHullSectionIndex != -1)
            hullGraphic.colorBezierPointGroup(selectedHullSectionIndex, true);
    }

    /**
     * Set the side view hull to build
     *
     * @param hull to set from
     * TODO
     */
    public void setTopViewHullGraphic(Hull hull) {
    }

    /**
     * Set the front view hull to build
     *
     * @param hull to set from
     * TODO
     */
    public void setFrontViewHullGraphic(Hull hull) {
    }

    /**
     * Highlight the previous section to the left to view and edit
     */
    public void selectPreviousHullSection(MouseEvent e) {
        unlockKnobsOnFirstSectionSelect();

        // Use modulo to wrap index
        if (selectedHullSectionIndex == -1) selectedHullSectionIndex++;
        selectedHullSectionIndex = (selectedHullSectionIndex - 1 + hull.getHullSections().size()) % hull.getHullSections().size();
        selectedHullSection = hull.getHullSections().get(selectedHullSectionIndex);

        hullGraphic.colorPreviousBezierPointGroup();

        if (sectionPropertiesSelected) {
            setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),
                    selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        }

        previousPressedBefore = true;
        unboundKnobs();
        setKnobValues();
        setKnobBounds();
    }

    /**
     * Highlight the next section to the right to view and edit
     */
    public void selectNextHullSection(MouseEvent e) {
        unlockKnobsOnFirstSectionSelect();

        // Use modulo to wrap index
        selectedHullSectionIndex = (selectedHullSectionIndex + 1) % hull.getHullSections().size();
        selectedHullSection = hull.getHullSections().get(selectedHullSectionIndex);

        hullGraphic.colorNextBezierPointGroup();

        if (sectionPropertiesSelected) {
            setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),
                    selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        }

        nextPressedBefore = true;
        unboundKnobs();
        setKnobValues();
        setKnobBounds();
    }

    /**
     * Unlock all knobs when selecting a section for the first time
     */
    private void unlockKnobsOnFirstSectionSelect() {
        if (!nextPressedBefore && !previousPressedBefore) {
            for (Knob knob : knobs) {
                knob.setLocked(false);
            }
        }
    }

    /**
     * Display section properties with corresponding attributes
     */
    public void setSectionProperties(double height, double volume, double mass, double x, double rx) {
        String heightInfo = String.format("%.4f m", height);
        this.heightLabel.setText(heightInfo);
        String interval = String.format("(%.4f m, %.4f m)", x, rx);
        this.intervalLabel.setText(interval);
        String volumeFormated = String.format("%.4f m^3", volume);
        this.volumeLabel.setText(volumeFormated);
        String massFormated = String.format("%.4f kg", mass);
        this.massLabel.setText(massFormated);
    }

    /**
     * Calculates and sets the knob values in polar coordinates, for when a user goes to the next/prev section
      */
    private void setKnobValues() {
        // Batch update values, add back listeners
        List<Double> knobValues = HullGeometryService.getPolarParameterValues();
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().removeListener(knobListeners.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).setKnobValue(knobValues.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().addListener(knobListeners.get(i));}
    }

    /**
     * In an r-θ pair of knobs, the bounds of one variable are relative to the other.
     * Thus, if we update r, we must adjust the θ bounds, and vice versa.
     */
    @Traceable
    private void setSiblingKnobBounds(int knobIndex, double siblingTheta) {
        int siblingIndex = switch (knobIndex) {
            case 0 -> 1; // rL -> θL
            case 1 -> 0; // θL -> rL
            case 2 -> 3; // rR -> θR
            case 3 -> 2; // θR -> rR
            default -> throw new IllegalArgumentException("Invalid knob index: " + knobIndex);
        };
        Knob siblingKnob = knobs.get(siblingIndex);

        // Get current knob values
        double r = knobs.get((knobIndex < 2) ? 0 : 2).getValue();
        double theta = knobs.get((knobIndex < 2) ? 1 : 3).getValue();

        // Delegate logic to HullGeometryService
        if (knobIndex % 2 == 0) { // Updating r, adjust θ bounds
            double[] thetaBounds = HullGeometryService.calculateSiblingThetaBounds(knobIndex, siblingTheta, r);
            siblingKnob.setKnobMin(thetaBounds[0]);
            siblingKnob.setKnobMax(thetaBounds[1]);
        } else { // Updating θ, adjust r bounds
            double rMax = HullGeometryService.calculateSiblingRMax(knobIndex, theta);
            siblingKnob.setKnobMax(rMax);
        }
    }

    /**
     * Sets the bounds of the knobs so that the control points stay in the rectangle bounded by:
     * x = 0, x = L, y = 0, y = -h.
     * The height h comes from the front view and corresponds to the lowest y-value of the knot point.
     * MUST BE DONE AFTER SETTING KNOB VALUES WHEN SWITCHING SECTIONS
     */
    private void setKnobBounds() {
        double[] bounds = HullGeometryService.calculateParameterBounds();
        knobs.get(0).setKnobMin(bounds[0]); // rL min
        knobs.get(0).setKnobMax(bounds[1]); // rL max
        knobs.get(1).setKnobMin(bounds[2]); // thetaL min
        knobs.get(1).setKnobMax(bounds[3]); // thetaL max
        knobs.get(2).setKnobMin(bounds[4]); // rR min
        knobs.get(2).setKnobMax(bounds[5]); // rR max
        knobs.get(3).setKnobMin(bounds[6]); // thetaR min
        knobs.get(3).setKnobMax(bounds[7]); // thetaR max
    }

    /**
     * Display N/A for all section properties
     */
    public void setBlankSectionProperties() {
        String na = "N/A";
        this.heightLabel.setText(na);
        this.intervalLabel.setText(na);
        this.volumeLabel.setText(na);
        this.massLabel.setText(na);
    }

    /**
     * Set bounds for r and theta back to minimum and maximum possible in the whole rectangle bounding the model
     * Used before setting the knob values to prevent setting the knob values out of bounds
     */
    public void unboundKnobs() {
        double minR = HullGeometryService.OPEN_INTERVAL_TOLERANCE;
        double maxPossibleR = hull.getLength();
        double minPossibleTheta = HullGeometryService.OPEN_INTERVAL_TOLERANCE;
        double maxPossibleTheta = 360 - HullGeometryService.OPEN_INTERVAL_TOLERANCE;

        for (int i = 0; i < knobs.size(); i++) {
            knobs.get(i).valueProperty().removeListener(knobListeners.get(i));
        }

        knobs.get(0).setKnobMin(minR);
        knobs.get(0).setKnobMax(maxPossibleR);
        knobs.get(1).setKnobMin(minPossibleTheta);
        knobs.get(1).setKnobMax(maxPossibleTheta);
        knobs.get(2).setKnobMin(minR);
        knobs.get(2).setKnobMax(maxPossibleR);
        knobs.get(3).setKnobMin(minPossibleTheta);
        knobs.get(3).setKnobMax(maxPossibleTheta);

        for (int i = 0; i < knobs.size(); i++) {
            knobs.get(i).valueProperty().addListener(knobListeners.get(i));
        }
    }


    /**
     * Switches back and forth from canoe properties to section properties
     */
    public void switchButton(MouseEvent e) {
        if (sectionPropertiesSelected) {
            sectionPropertiesSelected = false;
            propertiesPanelTitleLabel.setText("Canoe Properties");
            setSectionProperties(hull.getMaxHeight(), hull.getTotalVolume(),hull.getMass(), 0, hull.getLength());
        }
        else {
            sectionPropertiesSelected = true;
            propertiesPanelTitleLabel.setText("Section Properties");
            try {
                setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
            } catch (Exception ex) {
                setBlankSectionProperties();
            }
        }
    }

    /**
     * Updates the knot and control points of the selected hull section and its graphic
     * Updates the section properties panel
     * Updates associated knob bounds in r-theta pair
     * In order to maintain C1 smoothness, the angle must match for both sides of all bezier handles.
     * Since handles lie of the edges of sections, adjusting a handle requires moving the control point in the adjacent section
     * Thus we differentiate between the data of the selected and adjacent hull sections
     *
     * @param knobIndex the index of the knob that was changed (knobs indexed L to R in increasing order from 0)
     * @param oldROrThetaVal the old value for the knob (before user interaction)
     * @param newROrThetaVal the new value for the knob (after user interaction)
     */
    @Traceable
    private void updateSystemFromKnob(int knobIndex, double oldROrThetaVal, double newROrThetaVal) {
        if (Math.abs(oldROrThetaVal - newROrThetaVal) < 1e-6) return;
        if (selectedHullSection == null) return;

        // Update the relevant knob value
        knobs.get(knobIndex).setKnobValue(newROrThetaVal);

        // Update the model
        double[] knobValues = knobs.stream().mapToDouble(Knob::getValue).toArray();
        hull = HullGeometryService.updateHullParameter(knobIndex, newROrThetaVal, knobValues);

        // Update UI with new hull
        hullGraphicPane.getChildren().clear();
        setSideViewHullGraphic(hull);

        // If section properties are selected, update the selected section using its index.
        if (sectionPropertiesSelected && selectedHullSectionIndex >= 0 && selectedHullSectionIndex < hull.getHullSections().size())
            selectedHullSection = hull.getHullSections().get(selectedHullSectionIndex);

        recalculateAndDisplayHullProperties();

        // Update sibling knob bounds
        double siblingTheta = knobs.get((knobIndex % 2 == 0) ? knobIndex + 1 : knobIndex - 1).getValue();
        setSiblingKnobBounds(knobIndex, siblingTheta);
    }

    /**
     * Update the UI based on the current property selection
     */
    private void recalculateAndDisplayHullProperties() {
        if (sectionPropertiesSelected && selectedHullSection != null) {
            setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),
                    selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        } else if (selectedHullSection != null) {
            setSectionProperties(hull.getMaxHeight(), hull.getTotalVolume(),
                    hull.getMass(), 0, hull.getLength());
        }
    }

    /**
     * Add 4 knobs with N/A displaying, for when no hull section is selected
     */
    private void layoutKnobs() {
        double layoutY = 55;
        double knobSize = 40;
        List<String> knobLabels = List.of("rL", "θL", "rR", "θR");
        List<Double> minValues = List.of(0.0, 180.0, 0.0, 0.0);
        List<Double> maxValues = List.of(1.5, 360.0, 1.5, 180.0);
        List<Double> layoutXs = List.of(30.0, 140.0, 290.0, 400.0);

        knobs = new ArrayList<>();
        knobListeners = new ArrayList<>();

        for (int i = 0; i < knobLabels.size(); i++) {
            Knob knob = new Knob(knobLabels.get(i), 0, minValues.get(i), maxValues.get(i), layoutXs.get(i), layoutY, knobSize, 1.8);
            knob.setLocked(true);
            knobs.add(knob);
            int finalI = i;
            knobListeners.add((observable, oldValue, newValue) -> updateSystemFromKnob(finalI, oldValue.doubleValue(), newValue.doubleValue()));
            knob.valueProperty().addListener(knobListeners.get(i));
        }

        curveParameterizationAnchorPane.getChildren().addAll(knobs);
    }

    /**
     * Toggles the viewing state of all graphics except the hull itself.
     * The toggle cycles through the following states:
     * - Everything visible
     * - Some things visible
     * - Nothing visible
     * @param e the MouseEvent triggered by the button click
     */
    private void toggleGraphicsViewingStateButton(MouseEvent e) {
        // 3 states
        graphicsViewingState = (graphicsViewingState + 1) % 3;

        // Update the button's icon based on the current state
        IconGlyphType newIcon;
        switch (graphicsViewingState) {
            case 1 -> newIcon = IconGlyphType.HALF_FILLED_CIRCLE;
            case 2 -> newIcon = IconGlyphType.RING;
            default -> newIcon = IconGlyphType.CIRCLE;
        }
        IconButton graphicsViewingStateButton = (IconButton) e.getSource();
        graphicsViewingStateButton.setIcon(newIcon);
        applyGraphicsViewingState();
    }

    /**
     * Apply the current graphics transparency state to slopeGraphics
     */
    private void applyGraphicsViewingState() {
        hullGraphic.getSlopeGraphics().forEach(slopeGraphic -> {
            switch (graphicsViewingState) {
                case 0 -> slopeGraphic.setOpacity(1.0);
                case 1 -> slopeGraphic.getAllGraphicsButCenterPoint()
                        .forEach(node -> node.setOpacity(0.0));
                case 2 -> {
                    slopeGraphic.getAllGraphicsButCenterPoint()
                            .forEach(node -> node.setOpacity(1.0));
                    slopeGraphic.setOpacity(0.0);
                }
            }
        });
    }


    /**
     * Add and position blue buttons to corners of panels
     */
    private void layoutPanelButtons() {
        IconButton nextLeftSectionButton = IconButton.getPanelButton(IconGlyphType.LEFT, this::selectPreviousHullSection, 12);
        IconButton nextRightSectionButton = IconButton.getPanelButton(IconGlyphType.RIGHT, this::selectNextHullSection, 12);
        IconButton graphicsTransparencyButton = IconButton.getPanelButton(IconGlyphType.CIRCLE, this::toggleGraphicsViewingStateButton, 12);
        IconButton curveParameterizationPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);
        IconButton canoePropertiesSwitchButton = IconButton.getPanelButton(IconGlyphType.SWITCH, this::switchButton, 14);
        IconButton canoePropertiesPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);

        nextRightSectionButton.getIcon().setTranslateX(1.5);
        nextRightSectionButton.getIcon().setTranslateY(0.5);
        nextLeftSectionButton.getIcon().setTranslateX(-0.5);
        nextLeftSectionButton.getIcon().setTranslateY(0.5);
        curveParameterizationPlusButton.getIcon().setTranslateY(1);
        canoePropertiesPlusButton.getIcon().setTranslateY(1);

        hullViewAnchorPane.getChildren().addAll(nextLeftSectionButton, nextRightSectionButton, graphicsTransparencyButton);
        curveParameterizationAnchorPane.getChildren().add(curveParameterizationPlusButton);
        propertiesAnchorPane.getChildren().addAll(canoePropertiesSwitchButton, canoePropertiesPlusButton);

        double marginToPanel = 15;
        double marginBetween = 10;
        double buttonWidth = nextLeftSectionButton.prefWidth(-1);

        AnchorPane.setTopAnchor(nextLeftSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextLeftSectionButton, marginToPanel + 2 * (marginBetween + buttonWidth));
        AnchorPane.setTopAnchor(nextRightSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextRightSectionButton, marginToPanel);
        AnchorPane.setTopAnchor(graphicsTransparencyButton, marginToPanel);
        AnchorPane.setRightAnchor(graphicsTransparencyButton, marginToPanel + marginBetween + buttonWidth);
        AnchorPane.setTopAnchor(curveParameterizationPlusButton, marginToPanel);
        AnchorPane.setRightAnchor(curveParameterizationPlusButton, marginToPanel);
        AnchorPane.setTopAnchor(canoePropertiesSwitchButton, marginToPanel);
        AnchorPane.setRightAnchor(canoePropertiesSwitchButton, marginToPanel);
        AnchorPane.setTopAnchor(canoePropertiesPlusButton, marginToPanel + marginBetween + buttonWidth);
        AnchorPane.setRightAnchor(canoePropertiesPlusButton, marginToPanel);
    }

    /**
     * Handles the mouse exiting the hull view pane
     */
    private void handleHullViewPaneMouseExited(MouseEvent event) {
        // Updates if the user's mouse exited the pane while using the drag knot feature
        if (isDraggingKnot || isDraggingKnotPreview) {
            // State update out of dragging mode
            isDraggingKnot = false;
            isDraggingKnotPreview = false;
            shiftKeyPressHadMouseInDeletingKnotPointZone = false;
            mousePressWasInAddingKnotPointZone = false;
            initialKnotDragMousePos = null;
            initialKnotDragKnotPos = null;

            // Mouse tracker line renders back in
            double mouseX = event.getX();
            mouseXTrackerLine.setOpacity(0.7);
            mouseXTrackerLine.setStartX(mouseX);
            mouseXTrackerLine.setEndX(mouseX);
            mouseXTrackerLine.setStartY(0);
            mouseXTrackerLine.setEndY(hullViewAnchorPane.getHeight() - 1);
            updateHullIntersectionPoint(mouseX);

            // Other UI updates
            poiModeLabel.setText("");
            updateAddingKnotTitleLabel(isMouseInAddingKnotPointZone(mouseX));
            if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
            hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);

            // User exited during a drag (closed hand), not a drag preview (open hand)
            // Cancel the drag transaction & restore state from before dragging
            if (!isDraggingKnotPreview) {
                setSideViewHullGraphic(hull);
                knotDraggingPreviewHull = MarshallingService.deepCopy(hull);
            }
        }

        // State updates regardless of if the user was using the drag knot feature
        knotEditingCurrentMouseX = -1;
        knotEditingCurrentMouseY = -1;

        // No longer dragging, thus no longer require dynamic handlers
        hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
        hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
    }

    /**
     * Handles mouse movement within the hull view pane while using the knot editor feature
     */
    private void handleKnotEditingMouseMoved(MouseEvent event) {
        // Only perform computations if the sections editor is enabled
        if (!sectionEditorEnabled) {
            if (intersectionPoint != null) intersectionPoint.setOpacity(0);
            if (intersectionXMark != null) intersectionXMark.setOpacity(0);
            if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
            return;
        }

        knotEditingCurrentMouseX = event.getX();
        knotEditingCurrentMouseY = event.getY();
        double mouseX = event.getX();
        double paneHeight = hullViewAnchorPane.getHeight();

        if (event.isShiftDown() && shiftKeyPressHadMouseInDeletingKnotPointZone) {
            if (!isMouseInAddingKnotPointZone(mouseX)) {
                // In dragging zone: hide the vertical tracker and POI markers, and update labels to indicate dragging.
                mouseXTrackerLine.setOpacity(0);
                if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                if (intersectionXMark != null) intersectionXMark.setOpacity(0);
                poiTitleLabel.setText("Dragging Knot Point");
            } else {
                // If the mouse moves into the adding zone while Shift is not held:
                if (!isDraggingKnot && !isDraggingKnotPreview) {
                    initialKnotDragMousePos = null;
                    initialKnotDragKnotPos = null;

                    // Restore the vertical tracker and POI markers
                    mouseXTrackerLine.setOpacity(0.7);
                    mouseXTrackerLine.setStartX(mouseX);
                    mouseXTrackerLine.setEndX(mouseX);
                    mouseXTrackerLine.setStartY(0);
                    mouseXTrackerLine.setEndY(paneHeight - 1);
                    updateHullIntersectionPoint(mouseX);
                    // updateHullIntersectionPointDisplay(, null);
                    poiModeLabel.setText("");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);

                    hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                    hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
                }
            }
            if (!mousePressWasInAddingKnotPointZone
                    || shiftKeyPressHadMouseInDeletingKnotPointZone
                    || !isMouseInAddingKnotPointZone(mouseX)) {
                if (initialKnotDragKnotPos != null) {
                    // Create the drag indicator line
                    double knotScreenX = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getX(),
                            hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
                    double knotScreenY = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getY(),
                            hullGraphicPane.getPrefHeight(), -Math.abs(hull.getMaxHeight())) + hullGraphicPane.getLayoutY();
                    dragIndicatorLine.setStartX(event.getX());
                    dragIndicatorLine.setStartY(event.getY() - 1);
                    dragIndicatorLine.setEndX(knotScreenX);
                    dragIndicatorLine.setEndY(knotScreenY);
                    dragIndicatorLine.setVisible(true);
                }
            }
        }
        // When Shift is not held, update the UI for adding/deleting mode only
        else {
            mouseXTrackerLine.setOpacity(0.7);
            mouseXTrackerLine.setStartX(mouseX);
            mouseXTrackerLine.setEndX(mouseX);
            mouseXTrackerLine.setStartY(0);
            mouseXTrackerLine.setEndY(paneHeight - 1);
            updateHullIntersectionPoint(mouseX);

            if (!isMouseInAddingKnotPointZone(mouseX)) {
                poiTitleLabel.setText("Deleting Knot Point");
                if (shiftKeyPressHadMouseInDeletingKnotPointZone)
                    poiModeLabel.setText("Hold Shift to Drag Knot Point");
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
            } else {
                poiTitleLabel.setText("Adding Knot Point");
                poiModeLabel.setText("");
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
            }
        }
    }


    /**
     * Render or un-render the point of intersection between the line x = mouseX and the hull curve graphic
     * @param mouseX the x position of the mouse at which the vertical line is at
     */
    private void updateHullIntersectionPoint(double mouseX) {
        double functionSpaceX;
        double functionSpaceY;
        Point2D pointToDisplay;
        if (mouseX >= hullGraphicPane.getLayoutX() && mouseX <= hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth()) {
            double poiX = mouseX - hullGraphicPane.getLayoutX();
            functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
            HullSection section = hull.getHullSections().stream()
                    .filter(s -> s.getX() <= functionSpaceX && s.getRx() >= functionSpaceX)
                    .findFirst().orElseThrow(() -> new RuntimeException("Cannot place intersection point, out of bounds"));
            CubicBezierFunction bezier = (CubicBezierFunction) section.getSideProfileCurve();
            functionSpaceY = bezier.value(functionSpaceX);
            double poiY = (functionSpaceY / hull.getLength()) * hullGraphicPane.getWidth();
            Point2D poi = new Point2D(poiX, -poiY);
            updateMouseLinePoint(poi, isMouseInAddingKnotPointZone(mouseX));
            pointToDisplay = new Point2D(functionSpaceX, functionSpaceY);
        }
        else {
            pointToDisplay = null;
            if (intersectionPoint != null) {
                intersectionPoint.setOpacity(0);
                intersectionPoint = null;
            }
            if (intersectionXMark != null) intersectionXMark.setOpacity(0);
        }
        updateHullIntersectionPointDisplay(pointToDisplay, null);
    }

    /**
     * Updates the knot point display label.
     * Converts the given function-space point to a formatted string and updates poiDataLabel.
     * If a new point is provided, the label shows the transformation as: (x: old, y: old) ⇒ (x: new, y: new)
     * If the functionSpacePoint is null (or section editing is disabled), it shows "(x: N/A, y: N/A)".
     * @param functionSpacePoint current function-space point (or null)
     * @param optionalFunctionSpaceUpdatedPoint new point after the operation (or null)
     */
    private void updateHullIntersectionPointDisplay(Point2D functionSpacePoint, Point2D optionalFunctionSpaceUpdatedPoint) {
        String pointData;
        String nullPointString = "(x: N/A, y: N/A)";

        if (functionSpacePoint != null && sectionEditorEnabled) {
            // Convert the function-space point to a screen coordinate to decide mode.
            double screenX = (functionSpacePoint.getX() / hull.getLength()) * hullGraphicPane.getWidth() + hullGraphicPane.getLayoutX();
            if (!isMouseInAddingKnotPointZone(screenX)) {
                // Deletion mode: show the existing editable knot point, if available.
                Point2D deletableKnotPoint = HullGeometryService.getEditableKnotPoint(functionSpacePoint.getX());
                pointData = (deletableKnotPoint == null)
                        ? nullPointString
                        : String.format("(x: %.3f, y: %.3f)", deletableKnotPoint.getX(), deletableKnotPoint.getY());
            } else {
                // Addition mode: show the current function-space point.
                String currentXStr = String.format("%.3f", CalculusUtils.roundXDecimalDigits(functionSpacePoint.getX(), 3));
                String currentYStr = String.format("%.3f", CalculusUtils.roundXDecimalDigits(functionSpacePoint.getY(), 3));
                pointData = String.format("(x: %s, y: %s)", currentXStr, currentYStr);
            }
            // Append new point info if provided.
            if (optionalFunctionSpaceUpdatedPoint != null) {
                String newXStr = String.format("%.3f", CalculusUtils.roundXDecimalDigits(optionalFunctionSpaceUpdatedPoint.getX(), 3));
                String newYStr = String.format("%.3f", CalculusUtils.roundXDecimalDigits(optionalFunctionSpaceUpdatedPoint.getY(), 3));
                pointData += " ⇒ " + String.format("(x: %s, y: %s)", newXStr, newYStr);
            }
        } else pointData = nullPointString;

        poiDataLabel.setText(pointData);
        double centerX = (poiTitleLabel.getWidth() / 2) + poiTitleLabel.getLayoutX();
        double offset = (optionalFunctionSpaceUpdatedPoint == null) ? 0 : 55;
        poiDataLabel.setLayoutX(centerX - (poiDataLabel.getWidth() / 2) - offset);
    }

    /**
     * Copies the fields of the source pane for position and dimension into a new pane
     * @param sourcePane the pane to clone the fields from
     * @return the new pane which overlays the original if in the same parent (assuming no styles interfere)
     */
    private AnchorPane getOverlayPane(AnchorPane sourcePane) {
        AnchorPane overlayPane = new AnchorPane();
        overlayPane.setLayoutX(sourcePane.getLayoutX());
        overlayPane.setLayoutY(sourcePane.getLayoutY());
        overlayPane.setPrefWidth(sourcePane.getPrefWidth());
        overlayPane.setPrefHeight(sourcePane.getPrefHeight());
        overlayPane.setMinWidth(sourcePane.getMinWidth());
        overlayPane.setMinHeight(sourcePane.getMinHeight());
        overlayPane.setMaxWidth(sourcePane.getMaxWidth());
        overlayPane.setMaxHeight(sourcePane.getMaxHeight());
        return overlayPane;
    }

    /**
     * Funnels all knot editing clicks (adding, deleting, or dragging (editing))
     * Sets up and calls flow for each case, and catches invalid flows which display errors
     * @param event containing the mouses position for the click
     */
    private void handleKnotEditMousePressed(MouseEvent event) {
        // Out-of-bounds check
        double mouseX = event.getX();
        if (mouseX < hullGraphicPane.getLayoutX() || mouseX > hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth())
            return;

        // State updates
        isDraggingKnot = sectionEditorEnabled && event.isShiftDown() && isDraggingKnotPreview;
        knotEditingMouseButtonDown = true;
        isDraggingKnotPreview = false;
        mousePressWasInAddingKnotPointZone = isMouseInAddingKnotPointZone(mouseX);

        // Get the knot point we are editing
        double poiX = mouseX - hullGraphicPane.getLayoutX();
        double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
        Point2D editableKnot = HullGeometryService.getEditableKnotPoint(functionSpaceX);

        // Dragging Behaviour if needed
        if (isDraggingKnot) {
            // Immediate State and UI updates
            dragStartTime = System.currentTimeMillis();
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());
            initialKnotDragKnotPos = editableKnot;
            hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());
            poiModeLabel.setText("Release Shift to Cancel Dragging");

            // Mouse dragged/released handlers attached and will take over from here
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            return;
        }

        // Adding & Deleting Knot point behaviour if not dragging
        HullSection section = hull.getHullSections().stream()
                .filter(s -> s.getX() <= functionSpaceX && s.getRx() >= functionSpaceX)
                .findFirst().orElseThrow(() -> new RuntimeException("Cannot place intersection point, out of bounds"));
        CubicBezierFunction bezier = (CubicBezierFunction) section.getSideProfileCurve();
        double functionSpaceY = bezier.value(functionSpaceX);

        // If no candidate deletable knot to deletes exists, that means we should add a knot point
        Hull updatedHull = null;
        boolean isAddOperation = false;
        if (editableKnot == null) {
            if (isMouseInAddingKnotPointZone(mouseX)) {
                updatedHull = HullGeometryService.addKnotPoint(new Point2D(functionSpaceX, functionSpaceY));
                isAddOperation = true;
            }
        }
        // Otherwise, if a candidate deletable knot exists and there are enough sections, delete it.
        else if (hull.getHullSections().size() > 2) updatedHull = HullGeometryService.deleteKnotPoint(editableKnot);

        // Hull State/graphics updates if the knot edit changed the hull
        if (updatedHull != null) {
            selectedHullSection = null;
            selectedHullSectionIndex = -1;
            nextPressedBefore = false;
            previousPressedBefore = false;
            recalculateAndDisplayHullProperties();
            setSideViewHullGraphic(updatedHull);
            hull = updatedHull;
            updateSectionsEditorHullCurveOverlay();
        }

        // Display the appropriate snackbar message to the user
        if (updatedHull == null) {
            if (editableKnot == null) mainController.showSnackbar("Cannot delete knot point");
            else mainController.showSnackbar(String.format("Cannot delete knot point: (x = %.3f, y = %.3f), too few sections", editableKnot.getX(), editableKnot.getY()));
        }
        else {
            if (isAddOperation) mainController.showSnackbar(String.format("Knot point added: (x = %.3f, y = %.3f)", functionSpaceX, functionSpaceY));
            else mainController.showSnackbar(String.format("Knot point deleted successfully: (x = %.3f, y = %.3f)", editableKnot.getX(), editableKnot.getY()));
        }
    }

    /**
     * State and UI update for dragging the mouse when using the drag knot feature
     * Uses geometry service to update graphics as the knot is dragged
     */
    private void handleKnotDragMouseDragged(MouseEvent event) {
        // isDraggingKnot should be updated before this handler is attached for use
        if (isDraggingKnot) {
            // Update State
            knotEditingCurrentMouseX = event.getX();
            knotEditingCurrentMouseY = event.getY();

            // Update the drag indicator line
            double knotScreenX = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getX(), hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
            double knotScreenY = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getY(), hullGraphicPane.getPrefHeight(), -Math.abs(hull.getMaxHeight())) + hullGraphicPane.getLayoutY();
            double offsetX = knotScreenX - initialKnotDragMousePos.getX();
            double offsetY = knotScreenY - initialKnotDragMousePos.getY();
            double newEndX = event.getX() + offsetX;
            double newEndY = event.getY() + offsetY;
            dragIndicatorLine.setStartX(event.getX());
            dragIndicatorLine.setStartY(event.getY() - 1);
            dragIndicatorLine.setEndX(newEndX);
            dragIndicatorLine.setEndY(newEndY);
            dragIndicatorLine.setVisible(true);

            // Get the new knot position after dragging
            double graphicWidth = hullGraphicPane.getPrefWidth();
            double graphicHeight = hullGraphicPane.getPrefHeight();
            double modelWidth = hull.getLength();
            double modelHeight = Math.abs(hull.getMaxHeight());
            double newKnotModelX = ((newEndX - hullGraphicPane.getLayoutX()) / graphicWidth) * modelWidth;
            double newKnotModelY = ((newEndY - hullGraphicPane.getLayoutY()) / graphicHeight) * (-modelHeight);
            newKnotDragKnotPos = new Point2D(newKnotModelX, newKnotModelY);

            // Update the preview hull by calling the service.
            // This preview hull is kept separate from the original hull until the transaction commits.
            knotDraggingPreviewHull = HullGeometryService.dragKnotPoint(initialKnotDragKnotPos, newKnotDragKnotPos);
            setSideViewHullGraphic(knotDraggingPreviewHull);
            updateHullIntersectionPointDisplay(initialKnotDragKnotPos, newKnotDragKnotPos);
            hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);
        }
        else if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
    }

    /**
     * State and UI update for releasing the mouse when using the drag knot feature
     * Locks in a new geometry with an edited knot, graphics update to reflect that
     */
    private void handleKnotDragMouseReleased(MouseEvent event) {
        // User-action independent state update
        knotEditingMouseButtonDown = false;

        if (isDraggingKnot) {
            // Dragging State & UI Updates
            isDraggingKnot = false;
            initialKnotDragMousePos = null;
            poiModeLabel.setText("Click and Hold to Drag");
            hullViewAnchorPane.setCursor(Cursor.OPEN_HAND);
            dragIndicatorLine.setVisible(false);

            // Prevent short drags which are likely a misclick from the user, reverting the drag transaction
            if (System.currentTimeMillis() - dragStartTime <= 200) updateHullIntersectionPointDisplay(initialKnotDragKnotPos, null);
            else {
                // Commit the transaction by updating hull
                hull = knotDraggingPreviewHull;
                updateHullIntersectionPointDisplay(newKnotDragKnotPos, null);
            }
            setSideViewHullGraphic(hull);

            // More State updates
            knotDraggingPreviewHull = null;
            initialKnotDragKnotPos = null;
            newKnotDragKnotPos = null;

            // Remove these dynamic handlers for the drag knot feature
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
        }
    }

    /**
     * @return if the mouse is hovering over the hull view anchor pane
     */
    private boolean isMouseOverHullViewAnchorPane() {
        if (knotEditingCurrentMouseX == -1 || knotEditingCurrentMouseY == -1) return false;
        double layoutX = hullViewAnchorPane.getLayoutX();
        double layoutY = hullViewAnchorPane.getLayoutY();
        double width = hullViewAnchorPane.getWidth();
        double height = hullViewAnchorPane.getHeight();
        return knotEditingCurrentMouseX >= layoutX && knotEditingCurrentMouseX <= (layoutX + width)
                && knotEditingCurrentMouseY >= layoutY && knotEditingCurrentMouseY <= (layoutY + height);
    }

    /**
     * Detects the release of any key on the keyboard
     * @param event contains information about the event where some key was released
     */
    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT) {
            // If a knot is currently being dragged, releasing shift cancels the dragging
            if (isDraggingKnot) {
                isDraggingKnot = false;
                isDraggingKnotPreview = false;
                initialKnotDragMousePos = null;
                knotDraggingPreviewHull = null;

                // Restore the vertical tracker and POI markers
                double paneHeight = hullViewAnchorPane.getHeight();
                mouseXTrackerLine.setOpacity(0.7);
                mouseXTrackerLine.setStartX(knotEditingCurrentMouseX);
                mouseXTrackerLine.setEndX(knotEditingCurrentMouseX);
                mouseXTrackerLine.setStartY(0);
                mouseXTrackerLine.setEndY(paneHeight - 1);
                updateHullIntersectionPoint(knotEditingCurrentMouseX);
                updateHullIntersectionPointDisplay(initialKnotDragKnotPos, null);

                initialKnotDragKnotPos = null;
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                poiModeLabel.setText("Click and Hold to Drag");
                setSideViewHullGraphic(hull);

                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            }

            // Then update label and cursor based on the current mouse position.
            if (isMouseOverHullViewAnchorPane()) {
                if (isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)) {
                    poiTitleLabel.setText("Adding Knot Point");
                    poiModeLabel.setText("");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                } else {
                    poiTitleLabel.setText("Deleting Knot Point");
                    poiModeLabel.setText("Hold Shift to Drag Knot Point");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                }
                dragIndicatorLine.setVisible(false);
                mouseXTrackerLine.setOpacity(0.7);
                mouseXTrackerLine.setStartX(knotEditingCurrentMouseX);
                mouseXTrackerLine.setEndX(knotEditingCurrentMouseX);
                mouseXTrackerLine.setStartY(0);
                mouseXTrackerLine.setEndY(hullViewAnchorPane.getHeight() - 1);
                updateHullIntersectionPoint(knotEditingCurrentMouseX);
            }
        }
    }

    /**
     * Detects the pressing of any key on the keyboard
     * @param event contains information about the event where some key was pressed
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT && isMouseOverHullViewAnchorPane()) {
            if (!knotEditingMouseButtonDown) {
                // Calculate the candidate knot point (in function space) based on the current mouse X.
                double poiX = knotEditingCurrentMouseX - hullGraphicPane.getLayoutX();
                double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
                Point2D editableKnot = HullGeometryService.getEditableKnotPoint(functionSpaceX);

                if (isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)) {
                    poiModeLabel.setText("");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                }
                else {
                    isDraggingKnotPreview = true;

                    // Hide vertical tracker and POI indicators while dragging.
                    mouseXTrackerLine.setOpacity(0);
                    if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                    if (intersectionXMark != null) intersectionXMark.setOpacity(0);
                    poiTitleLabel.setText("Dragging Knot Point");
                    poiModeLabel.setText("Click and Hold to Drag");
                    hullViewAnchorPane.setCursor(Cursor.OPEN_HAND);

                    // Create the drag indicator line
                    if (editableKnot != null) {
                        double knotScreenX = GraphicsUtils.getScaledFromModelToGraphic(editableKnot.getX(), hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
                        double knotScreenY = GraphicsUtils.getScaledFromModelToGraphic(editableKnot.getY(), hullGraphicPane.getPrefHeight(), -Math.abs(hull.getMaxHeight())) + hullGraphicPane.getLayoutY();
                        dragIndicatorLine.setStartX(knotEditingCurrentMouseX);
                        dragIndicatorLine.setStartY(knotEditingCurrentMouseY - 1);
                        dragIndicatorLine.setEndX(knotScreenX);
                        dragIndicatorLine.setEndY(knotScreenY);
                        dragIndicatorLine.setVisible(true);
                    }
                }
                initialKnotDragKnotPos = HullGeometryService.getEditableKnotPoint(functionSpaceX);
            }
            shiftKeyPressHadMouseInDeletingKnotPointZone =
                    !isMouseInAddingKnotPointZone(knotEditingCurrentMouseX) && isMouseOverHullViewAnchorPane();
        }
    }

    /**
     * Remove any module specific key event filters from the scene, to be called this method when the module is unloaded from SideDrawerController.
     */
    public void removeKeyEventFilters() {
        if (hullViewAnchorPane.getScene() != null) {
            hullViewAnchorPane.getScene().removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
            hullViewAnchorPane.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set controller instances
        setMainController(CanoeAnalysisApplication.getMainController());
        HullGeometryService.setHullBuilderController(this);

        // Layout
        initModuleToolBarButtons();
        layoutKnobs();
        layoutPanelButtons();

        // Set default hull
        hullGraphicPane = new AnchorPane();
        hull = SHARK_BAIT_HULL;
        setSideViewHullGraphic(hull);
        setBlankSectionProperties();

        // Sections Editor
        intersectionPointPane = new AnchorPane();
        intersectionPointPane.setPickOnBounds(false);
        intersectionPointPane = getOverlayPane(hullGraphicPane);
        hullViewAnchorPane.getChildren().add(intersectionPointPane);
        keyReleasedHandler = this::handleKeyReleased;
        keyPressedHandler = this::handleKeyPressed;

        Line xLine1 = new Line(-5, -5, 5, 5);
        Line xLine2 = new Line(-5, 5, 5, -5);
        xLine1.setStroke(ColorPaletteService.getColor("white"));
        xLine2.setStroke(ColorPaletteService.getColor("white"));
        xLine1.setStrokeWidth(2);
        xLine2.setStrokeWidth(2);
        intersectionXMark = new Group(xLine1, xLine2);
        intersectionXMark.setOpacity(0);
        intersectionPointPane.getChildren().add(intersectionXMark);

        mouseXTrackerLine = new Line();
        mouseXTrackerLine.setVisible(false);
        hullViewAnchorPane.getChildren().addLast(mouseXTrackerLine);

        // This handler is not dynamic like some others, it stays attached to the pane
        hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleHullViewPaneMouseExited);

        dragIndicatorLine = new Line();
        dragIndicatorLine.setStroke(ColorPaletteService.getColor("white"));
        dragIndicatorLine.getStrokeDashArray().setAll(4.0, 4.0);
        hullViewAnchorPane.getChildren().add(dragIndicatorLine);
        dragIndicatorLine.setVisible(false);

        poiTitleLabel.setText("");
        poiDataLabel.setText("");
        poiModeLabel.setText("");

        // Initialize state
        previousPressedBefore = false;
        nextPressedBefore = false;
        selectedHullSection = null;
        selectedHullSectionIndex = -1;
        sectionPropertiesSelected = true;
        graphicsViewingState = 0;
        sectionEditorEnabled = false;

        // Dragging Feature State [so much state for one feature D:]
        isDraggingKnot = false;
        isDraggingKnotPreview = false;
        initialKnotDragKnotPos = null;
        initialKnotDragMousePos = null;
        knotEditingCurrentMouseX = -1;
        knotEditingCurrentMouseY = -1;
        mousePressWasInAddingKnotPointZone = false;
        knotEditingMouseButtonDown = false;
        shiftKeyPressHadMouseInDeletingKnotPointZone = false;
        knotDraggingPreviewHull = MarshallingService.deepCopy(hull);
        newKnotDragKnotPos = null;
        dragStartTime = -1;

        // Attach a keystroke event detection filter once the scene is available.
        hullViewAnchorPane.setFocusTraversable(true);
        hullViewAnchorPane.requestFocus();
        hullViewAnchorPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            }
        });
    }
}
