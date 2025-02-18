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
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
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

    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @FXML
    private Label intervalLabel, heightLabel, volumeLabel, massLabel, propertiesPanelTitleLabel,
            poiTitleLabel, poiDataLabel, poiModeLabel;

    @Setter
    private static MainController mainController;

    // Refs
    @Getter @Setter
    private Hull hull;
    private final Hull sharkBaitHull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromBezier(6);
    private List<ChangeListener<Number>> knobListeners;
    private EventHandler<KeyEvent> shiftKeyPressedHandler;
    private EventHandler<KeyEvent> shiftKeyReleasedHandler;

    // UI & Graphics
    @Getter @Setter
    private AnchorPane hullGraphicPane;
    private CubicBezierSplineHullGraphic hullGraphic;
    private List<Knob> knobs;
    private Line mouseXTrackerLine;
    private Circle intersectionPoint;
    private Group intersectionXMark;
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
    private boolean isDraggingKnot;
    @Getter @Setter
    private double currentMouseX;
    @Getter @Setter
    private double currentMouseY;

    // Constants
    private final double sharkBaitSideViewPanelHeight = 45;

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
        hullViewAnchorPane.setOnMouseMoved(sectionEditorEnabled ? this::handleMouseMovedHullViewPane : null);
        if (!sectionEditorEnabled) {
            if (intersectionPoint != null) {
                intersectionPoint.setOpacity(0);
                intersectionPoint = null;
            }
            hullViewAnchorPane.setOnMouseClicked(null);
            intersectionXMark.setOpacity(0);
            poiTitleLabel.setText("");
            poiDataLabel.setText("");
        }
        else {
            if (sectionPropertiesSelected) switchButton(null); // Always show canoe properties first when in sections editor mode
            updateHullIntersectionPointDisplay(hull.getLength() / 2, hull.getMaxHeight());
            hullViewAnchorPane.setOnMouseClicked(this::handleKnotEditingClick);
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
            hullViewAnchorPane.setOnMouseClicked(this::handleKnotEditingClick);
            hullViewAnchorPane.setOnMouseDragged(this::handleKnotDragMouseDragged);
            hullViewAnchorPane.setOnMouseReleased(this::handleKnotDragMouseReleased);
        }
        else {
            hullViewAnchorPane.setOnMouseClicked(null);
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
                            (minY / -hull.getMaxHeight()) * (sharkBaitSideViewPanelHeight * (hull.getMaxHeight() / sharkBaitHull.getMaxHeight())),
                            ((rControlX - lControlX) / hull.getLength()) * hullGraphicPane.getWidth(),
                            ((Math.abs(maxY - minY)) / -hull.getMaxHeight()) * (sharkBaitSideViewPanelHeight * (hull.getMaxHeight() / sharkBaitHull.getMaxHeight()))
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
        double sideViewPanelHeight = sharkBaitSideViewPanelHeight * (hull.getMaxHeight() / sharkBaitHull.getMaxHeight());
        double paneX = hullViewAnchorPane.prefWidth(-1) / 2 - sideViewPanelWidth / 2;
        double paneY = hullViewAnchorPane.prefHeight(-1) / 2 - sharkBaitSideViewPanelHeight / 2;
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
    @Traceable
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
     * Handles mouse movement within the hull view pane.
     * Updates the vertical tracker line and checks for hull intersection to display the POI.
     */
    private void handleMouseMovedHullViewPane(MouseEvent event) {
        currentMouseX = event.getX();
        currentMouseY = event.getY();

        // Only perform computations if the sections editor is enabled
        if (!sectionEditorEnabled) {
            if (intersectionPoint != null) intersectionPoint.setOpacity(0);
            if (intersectionXMark != null) intersectionXMark.setOpacity(0);
            return;
        }

        double mouseX = event.getX();
        double paneHeight = hullViewAnchorPane.getHeight();

        if (event.isShiftDown()) {
            if (!isMouseInAddingKnotPointZone(mouseX)) {
                // In dragging zone: hide the vertical tracker and POI markers,
                // and update labels to indicate dragging.
                mouseXTrackerLine.setOpacity(0);
                if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                if (intersectionXMark != null) intersectionXMark.setOpacity(0);
                poiTitleLabel.setText("Dragging Knot Point");
                poiModeLabel.setText("Release Shift to Stop Dragging");
                hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);
            } else {
                // If the mouse moves into the adding zone while Shift is held:
                // Cancel any active dragging.
                if (isDraggingKnot) {
                    isDraggingKnot = false;
                    initialKnotDragMousePos = null;
                    initialKnotDragKnotPos = null;
                    hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                    hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
                }
                // Restore the vertical tracker and POI markers as in non-drag mode.
                mouseXTrackerLine.setOpacity(0.7);
                mouseXTrackerLine.setStartX(mouseX);
                mouseXTrackerLine.setEndX(mouseX);
                mouseXTrackerLine.setStartY(0);
                mouseXTrackerLine.setEndY(paneHeight - 1);
                updateHullIntersectionPoint(mouseX);
                poiTitleLabel.setText("Adding Knot Point");
                poiModeLabel.setText("");
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
            }
        } else {
            // When Shift is not held, update normally.
            mouseXTrackerLine.setOpacity(0.7);
            mouseXTrackerLine.setStartX(mouseX);
            mouseXTrackerLine.setEndX(mouseX);
            mouseXTrackerLine.setStartY(0);
            mouseXTrackerLine.setEndY(paneHeight - 1);
            updateHullIntersectionPoint(mouseX);

            if (!isMouseInAddingKnotPointZone(mouseX)) {
                poiTitleLabel.setText("Deleting Knot Point");
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
        Double functionSpaceX;
        Double functionSpaceY;
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
        }
        else {
            functionSpaceX = null;
            functionSpaceY = null;
            if (intersectionPoint != null) {
                intersectionPoint.setOpacity(0);
                intersectionPoint = null;
            }
            if (intersectionXMark != null) intersectionXMark.setOpacity(0);
        }
        updateHullIntersectionPointDisplay(functionSpaceX, functionSpaceY);
    }

    /**
     * Update the display for adding and deleting knot points to show where the point will be added or deleted
     * @param functionSpaceX the x coordinate of the point, in function space (not graphics space)
     * @param functionSpaceY the y coordinate of the point, in function space (not graphics space)
     */
    private void updateHullIntersectionPointDisplay(Double functionSpaceX, Double functionSpaceY) {
        String pointData;

        String nullPointString = "(x: N/A, y: N/A)";
        if (functionSpaceX != null && sectionEditorEnabled) {
            // Convert functionSpaceX back to mouseX
            double poiX = (functionSpaceX / hull.getLength()) * hullGraphicPane.getWidth();
            double mouseX = poiX + hullGraphicPane.getLayoutX();

            // Determine if in adding or deleting mode and display the appropriate point to add or delete
            if (!isMouseInAddingKnotPointZone(mouseX)) {
                Point2D deletableKnotPoint = HullGeometryService.getEditableKnotPoint(functionSpaceX);
                if (deletableKnotPoint == null) pointData = nullPointString;
                else pointData = String.format("(x: %.3f, y: %.3f)", deletableKnotPoint.getX(), deletableKnotPoint.getY());
            } else {
                String functionSpaceXString = String.format("%.3f", CalculusUtils.roundXDecimalDigits(functionSpaceX, 3));
                String functionSpaceYString = functionSpaceY == null ? "N/A"
                        : String.format("%.3f", CalculusUtils.roundXDecimalDigits(functionSpaceY, 3));
                pointData = String.format("(x: %s, y: %s)", functionSpaceXString, functionSpaceYString);
            }
        }
        else pointData = nullPointString;

        // Adjust position of the data label to keep it centered
        poiDataLabel.setText(pointData);
        double centerX = (poiTitleLabel.getWidth() / 2) + poiTitleLabel.getLayoutX();
        double poiDataLabelX = centerX - (poiDataLabel.getWidth() / 2);
        poiDataLabel.setLayoutX(poiDataLabelX);
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
    private void handleKnotEditingClick(MouseEvent event) {
        double mouseX = event.getX();
        // Out-of-bounds check.
        if (mouseX < hullGraphicPane.getLayoutX() ||
                mouseX > hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth())
            return;

        double poiX = mouseX - hullGraphicPane.getLayoutX();
        final double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();

        // Get candidate knot for editing (using your editable method)
        Point2D editableKnot = HullGeometryService.getEditableKnotPoint(functionSpaceX);

        // If Shift is held and not in the adding zone, initiate dragging.
        if (event.isShiftDown() && !isMouseInAddingKnotPointZone(mouseX) && editableKnot != null) {
            isDraggingKnot = true;
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());
            initialKnotDragKnotPos = editableKnot;
            // Compute global minimum knot:
            Point2D globalMinKnot = hull.getHullSections().stream()
                    .flatMap(hs -> ((CubicBezierFunction) hs.getSideProfileCurve()).getKnotPoints().stream())
                    .min(Comparator.comparingDouble(Point2D::getY))
                    .orElseThrow(() -> new RuntimeException("No minimum knot found"));
            if (Math.abs(editableKnot.getY() - globalMinKnot.getY()) < 1e-6)
                poiModeLabel.setText(String.format("Height Change: (%.4f => %.4f)", editableKnot.getY(), editableKnot.getY()));
            else
                poiModeLabel.setText("Release Shift to Delete");

            // Dynamically attach drag and release handlers
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);

            return; // Abort further add/delete processing.
        }

        // Otherwise, proceed with normal add/delete behavior.
        HullSection section = hull.getHullSections().stream()
                .filter(s -> s.getX() <= functionSpaceX && s.getRx() >= functionSpaceX)
                .findFirst().orElseThrow(() -> new RuntimeException("Cannot place intersection point, out of bounds"));
        CubicBezierFunction bezier = (CubicBezierFunction) section.getSideProfileCurve();
        double functionSpaceY = bezier.value(functionSpaceX);

        Hull updatedHull = null;
        boolean isAddOperation = false;
        // If no candidate knot exists, try adding if in the adding zone.
        if (editableKnot == null) {
            if (isMouseInAddingKnotPointZone(mouseX)) {
                updatedHull = HullGeometryService.addKnotPoint(new Point2D(functionSpaceX, functionSpaceY));
                isAddOperation = true;
            }
        }
        // Otherwise, if a candidate exists and there are enough sections, delete it.
        else if (hull.getHullSections().size() > 2) updatedHull = HullGeometryService.deleteKnotPoint(editableKnot);

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

        if (updatedHull == null) {
            if (editableKnot == null)
                mainController.showSnackbar("Cannot delete knot point");
            else
                mainController.showSnackbar(String.format("Cannot delete knot point: (x = %.3f, y = %.3f), too few sections", editableKnot.getX(), editableKnot.getY()));
        }
        else {
            if (isAddOperation)
                mainController.showSnackbar(String.format("Knot point added: (x = %.3f, y = %.3f)", functionSpaceX, functionSpaceY));
            else
                mainController.showSnackbar(String.format("Knot point deleted successfully: (x = %.3f, y = %.3f)", editableKnot.getX(), editableKnot.getY()));
        }
    }

    /**
     * State and UI update for dragging the mouse when using the drag knot feature
     */
    private void handleKnotDragMouseDragged(MouseEvent event) {
        if (!isDraggingKnot) return;

        double currentX = event.getX();
        double currentY = event.getY();
        double deltaX = currentX - initialKnotDragMousePos.getX();
        double deltaY = currentY - initialKnotDragMousePos.getY();
        Point2D newKnotPos = new Point2D(
                initialKnotDragKnotPos.getX() + deltaX,
                initialKnotDragKnotPos.getY() + deltaY);

        // Determine if the dragged knot is the global minimum.
        Point2D globalMinKnot = hull.getHullSections().stream()
                .flatMap(hs -> ((CubicBezierFunction) hs.getSideProfileCurve()).getKnotPoints().stream())
                .min(Comparator.comparingDouble(Point2D::getY))
                .orElseThrow(() -> new RuntimeException("No minimum knot found"));
        boolean isMin = Math.abs(initialKnotDragKnotPos.getY() - globalMinKnot.getY()) < 1e-6;

        if (!isMin) {
            // Clamp newKnotPos within hull bounds (with a tolerance)
            double tol = 0.01;
            double minX = 0, maxX = hull.getLength();
            double minY = -hull.getMaxHeight(), maxY = 0;
            double clampedX = Math.max(minX + tol, Math.min(newKnotPos.getX(), maxX - tol));
            double clampedY = Math.max(minY + tol, Math.min(newKnotPos.getY(), maxY - tol));
            newKnotPos = new Point2D(clampedX, clampedY);
        } else {
            // For the min knot, update the label with the height change.
            poiModeLabel.setText(String.format("Height Change: (%.4f => %.4f)",
                    initialKnotDragKnotPos.getY(), newKnotPos.getY()));
        }

        // Update hull geometry via your service method.
        hull = HullGeometryService.dragKnotPoint(initialKnotDragKnotPos, newKnotPos);
        setSideViewHullGraphic(hull); // re-render the hull
    }

    /**
     * State and UI update for releasing the mouse when using the drag knot feature
     */
    private void handleKnotDragMouseReleased(MouseEvent event) {
        if (isDraggingKnot) {
            isDraggingKnot = false;
            initialKnotDragMousePos = null;
            initialKnotDragKnotPos = null;
            poiModeLabel.setText("");
            // Remove these dynamic handlers
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
        }
    }

    /**
     * @return if the mouse is hovering over the hull view anchor pane
     */
    private boolean isMouseOverHullViewAnchorPane() {
        double layoutX = hullViewAnchorPane.getLayoutX();
        double layoutY = hullViewAnchorPane.getLayoutY();
        double width = hullViewAnchorPane.getWidth();
        double height = hullViewAnchorPane.getHeight();
        return currentMouseX >= layoutX && currentMouseX <= (layoutX + width)
                && currentMouseY >= layoutY && currentMouseY <= (layoutY + height);
    }

    /**
     * Detects the release of any key on the keyboard
     * @param event contains information about the event where some key was released
     */
    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT) {
            // If a knot is currently being dragged, stop dragging immediately.
            if (isDraggingKnot) {
                isDraggingKnot = false;
                initialKnotDragMousePos = null;
                initialKnotDragKnotPos = null;
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            }
            // Then update label and cursor based on the current mouse position.
            if (isMouseOverHullViewAnchorPane()) {
                if (isMouseInAddingKnotPointZone(currentMouseX)) {
                    poiTitleLabel.setText("Adding Knot Point");
                    poiModeLabel.setText("");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                } else {
                    poiTitleLabel.setText("Deleting Knot Point");
                    poiModeLabel.setText("Hold Shift to Drag Knot Point");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                }
                mouseXTrackerLine.setOpacity(0.7);
                mouseXTrackerLine.setStartX(currentMouseX);
                mouseXTrackerLine.setEndX(currentMouseX);
                mouseXTrackerLine.setStartY(0);
                mouseXTrackerLine.setEndY(hullViewAnchorPane.getHeight() - 1);
                updateHullIntersectionPoint(currentMouseX);
            }
        }
    }

    /**
     * Detects the pressing of any key on the keyboard
     * @param event contains information about the event where some key was pressed
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT) {
            if (isMouseOverHullViewAnchorPane()) {
                if (isMouseInAddingKnotPointZone(currentMouseX)) {
                    poiModeLabel.setText("");
                    hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                } else {
                    // Hide vertical tracker and POI indicators while dragging.
                    mouseXTrackerLine.setOpacity(0);
                    if (intersectionPoint != null) {
                        intersectionPoint.setOpacity(0);
                    }
                    if (intersectionXMark != null) {
                        intersectionXMark.setOpacity(0);
                    }
                    poiTitleLabel.setText("Dragging Knot Point");
                    poiModeLabel.setText("Release Shift to Stop Dragging");
                    hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);
                }
            }
        }
    }

    /**
     * Remove any module specific key event filters from the scene, to be called this method when the module is unloaded from SideDrawerController.
     */
    public void removeKeyEventFilters() {
        if (hullViewAnchorPane.getScene() != null) {
            hullViewAnchorPane.getScene().removeEventFilter(KeyEvent.KEY_RELEASED, shiftKeyReleasedHandler);
            hullViewAnchorPane.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, shiftKeyPressedHandler);
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
        hull = sharkBaitHull;
        setSideViewHullGraphic(hull);
        setBlankSectionProperties();

        // Sections Editor
        intersectionPointPane = new AnchorPane();
        intersectionPointPane.setPickOnBounds(false);
        intersectionPointPane = getOverlayPane(hullGraphicPane);
        hullViewAnchorPane.getChildren().add(intersectionPointPane);
        shiftKeyReleasedHandler = this::handleKeyReleased;
        shiftKeyPressedHandler = this::handleKeyPressed;

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
        isDraggingKnot = false;
        initialKnotDragKnotPos = null;
        initialKnotDragMousePos = null;
        currentMouseX = 0;
        currentMouseY = 0;

        // Attach a keystroke event detection filter once the scene is available.
        hullViewAnchorPane.setFocusTraversable(true);
        hullViewAnchorPane.requestFocus();
        hullViewAnchorPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_RELEASED, shiftKeyReleasedHandler);
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, shiftKeyPressedHandler);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, shiftKeyReleasedHandler);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, shiftKeyPressedHandler);
            }
        });
    }
}
