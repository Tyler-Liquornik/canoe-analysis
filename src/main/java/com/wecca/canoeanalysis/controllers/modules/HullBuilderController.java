package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Debounce;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.CurvedGraphic;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.components.graphics.hull.CubicBezierSplineHullGraphic;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Zone;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.HullGeometryService;
import com.wecca.canoeanalysis.services.MarshallingService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.GraphicsUtils;
import com.wecca.canoeanalysis.utils.HullLibrary;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * To whoever is coding here:
 * You need to be extremely careful that everything here is very performant!
 * Be smart with good DS&A knowledge, and DO NOT WRITE SLOW CODE
 * ----------------------------------------------------------------------------
 * UI Controls for modifying the hull's geometry, primarily relying on HullGeometryService on the backend.
 */
@Traceable
public class HullBuilderController implements Initializable, ModuleController {

    // FXML UI Components
    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @FXML
    private Label intervalLabel, heightLabel, volumeLabel, massLabel, propertiesPanelTitleLabel,
            poiModeLabel, poiDataLabel, poiInfoLabel;

    // Refs
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
    @Getter
    private Hull hull;
    @Getter @Setter
    private CubicBezierFunction selectedBezierSegment;
    @Getter @Setter
    private int selectedBezierSegmentIndex;
    @Getter @Setter
    private List<Zone> overlayZones;
    @Getter @Setter
    private Point2D initialKnotDragKnotPos;
    private Point2D newKnotDragKnotPos;
    private Point2D initialKnotDragMousePos;
    private Hull knotDraggingPreviewHull;
    private boolean previousPressedBefore;
    private boolean nextPressedBefore;
    private boolean sectionPropertiesSelected;
    private boolean knotEditorEnabled;
    private boolean isDraggingKnot;
    private boolean isDraggingKnotPreview;
    private boolean isShiftKeyPressed;
    private boolean shiftKeyPressHadMouseInDraggableKnotPointZone;
    private boolean knotEditingMouseButtonDown;
    private int graphicsViewingState;
    private long dragStartTime;
    private double knotEditingCurrentMouseX;
    private double knotEditingCurrentMouseY;

    // Constants
    private final double TOP_ALLOWED_HULL_HEIGHT = 0.2;
    private final double BOTTOM_ALLOWED_MAX_HULL_HEIGHT = 0.5;

    // Note to developer: These hulls do not have deep immutability. Do not mutate!
    private final Hull SHARKBAIT_HULL = HullLibrary.generateSharkBaitHullScaled(HullLibrary.SHARK_BAIT_LENGTH);
    private final Hull ON_LOAD_HULL = HullLibrary.generateGirRaftHullScaled(HullLibrary.GIRRAFT_LENGTH);
    private final double ON_LOAD_SIDE_VIEW_PANE_HEIGHT = Math.ceil(45 * (ON_LOAD_HULL.getMaxHeight() / SHARKBAIT_HULL.getMaxHeight()));
    private final double ON_LOAD_DATA_LABEL_POS = 749;

    /**
     * Custom logic to set the hull
     * @param hull the hull to set
     */
    public void setHull(@NonNull Hull hull) {
        this.hull = hull;
        renderHullGraphic(hull);
        setHullProperties(hull);
        toggleOrUpdateKnotEditingHullCurveOverlay(false);
    }

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> downloadHull());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> uploadHull());
        iconGlyphToFunctionMap.put(IconGlyphType.PENCIL, this::toggleKnotEditorMode);
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    public void dummyOnClick(MouseEvent event) {
        mainController.showSnackbar("WIP");
    }

    /**
     * Handler for the pencil button which enables the ability to add or delete knots.
     * The logic here is responsible for immediate UI/state changes
     * @param event the click event
     */
    private void toggleKnotEditorMode(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        knotEditorEnabled = !knotEditorEnabled;
        nextPressedBefore = false;
        previousPressedBefore = false;
        selectedBezierSegmentIndex = -1;

        toggleOrUpdateKnotEditingHullCurveOverlay(true);
        updateKnotEditingTitleLabel(isMouseInAddingKnotPointZone(mouseXTrackerLine.getStartX()));

        List<Button> toolBarButtons = mainController.getModuleToolBarButtons();
        Button toggledIconButton = IconButton.getToolbarButton(knotEditorEnabled ? IconGlyphType.X__PENCIL : IconGlyphType.PENCIL, this::toggleKnotEditorMode);
        toolBarButtons.set(2, toggledIconButton);
        mainController.addOrSetToolBarButtons(toolBarButtons);
        hullViewAnchorPane.setCursor(knotEditorEnabled ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        hullViewAnchorPane.setOnMouseEntered(knotEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.CROSSHAIR) : null);
        hullViewAnchorPane.setOnMouseExited(knotEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.DEFAULT) : null);
        hullViewAnchorPane.setOnMouseMoved(knotEditorEnabled ? this::handleKnotEditingMouseMoved : null);
        if (!knotEditorEnabled) {
            if (intersectionPoint != null) {
                intersectionPoint.setOpacity(0);
                intersectionPoint = null;
            }
            hullViewAnchorPane.setOnMousePressed(null);
            intersectionXMark.setOpacity(0);
            poiInfoLabel.setText("");
            poiModeLabel.setText("");
            poiDataLabel.setText("");
        }
        else {
            if (sectionPropertiesSelected) switchButton(null); // Always show canoe properties first when in knot editor mode
            updateHullIntersectionPointDisplay(new Point2D(hull.getLength() / 2, hull.getMaxHeight()), null);
            hullViewAnchorPane.setOnMousePressed(this::handleKnotEditMousePressed);
        }

        if (selectedBezierSegment != null) hullGraphic.recolor(!knotEditorEnabled);
        hullGraphic.setColoredSectionIndex(-1);

        List<Button> topLeftButtons = hullViewAnchorPane.getChildren().stream().filter(node -> (node instanceof Button)).map(node -> (Button) node).toList();
        List<Button> sectionSelectorButtons = Arrays.asList(topLeftButtons.get(0), topLeftButtons.get(1));
        sectionSelectorButtons.forEach(button -> button.setDisable(knotEditorEnabled));
        sectionSelectorButtons.forEach(button -> button.setOpacity(1));

        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().removeListener(knobListeners.get(i));}
        if (selectedBezierSegment != null) {
            if (knotEditorEnabled) {
                selectedBezierSegment = null;
                unboundKnobs();
                knobs.forEach(knobs -> knobs.setKnobValue(0));
                if (sectionPropertiesSelected) setBlankSectionProperties();
            }
            knobs.forEach(knob -> knob.setLocked(knotEditorEnabled));
            if (!knotEditorEnabled) {
                for (int i = 0; i < knobs.size(); i++) {
                    knobs.get(i).valueProperty().addListener(knobListeners.get(i));
                }
                if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                if (intersectionXMark != null) intersectionXMark.setOpacity(0);
            }
        }

        // Update mouse X tracker line visibility and style
        mouseXTrackerLine.setVisible(knotEditorEnabled);
        if (knotEditorEnabled) {
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
            selectNextHullSection(null);
            if (!sectionPropertiesSelected) switchButton(null);
        }

        // Notify the user
        mainController.showSnackbar(knotEditorEnabled
                ? "Knot Editor Enabled"
                : "Knot Editor Disabled");
    }

    /**
     * Toggles or Updates (based on knotEditorEnabled) the hull curve overlay to show regions between control points in each section.
     * The overlays are added to a dedicated `overlayPane` above the `hullGraphicPane`.
     * a list of x-intervals where the overlays were added (in function space, NOT graphic space) is added to state
     */
    private void toggleOrUpdateKnotEditingHullCurveOverlay(boolean toggleElseUpdate) {
        // Toggle or update overlay visibility
        boolean enableOverlays = !(hullGraphic.getChildren().getLast() instanceof CurvedGraphic);
        if (!toggleElseUpdate) enableOverlays = enableOverlays && knotEditorEnabled;
        List<Zone> overlayZones = new ArrayList<>();

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
            for (CubicBezierFunction bezier : hull.getSideViewSegments()) {
                // Determine the x-range between the control points, which acts as the free section to place a knot point
                Zone overlayZone;
                double lControlX = bezier.getControlX1();
                double rControlX = bezier.getControlX2();
                if (Math.abs(rControlX - lControlX) <= 5e-3 || rControlX - lControlX <= 5e-3)
                    overlayZone = new Zone(lControlX, rControlX);
                else {
                    overlayZone = new Section(lControlX, rControlX);
                    double maxY = bezier.getMaxValue(new Section(lControlX, rControlX));
                    double minY = bezier.getMinValue(new Section(lControlX, rControlX));
                    Rectangle validAddKnotRectangle = new Rectangle(
                            (lControlX / hull.getLength()) * hullGraphicPane.getWidth(),
                            (minY / -hull.getMaxHeight()) * (ON_LOAD_SIDE_VIEW_PANE_HEIGHT * (hull.getMaxHeight() / ON_LOAD_HULL.getMaxHeight())),
                            ((rControlX - lControlX) / hull.getLength()) * hullGraphicPane.getWidth(),
                            ((Math.abs(maxY - minY)) / -hull.getMaxHeight()) * (ON_LOAD_SIDE_VIEW_PANE_HEIGHT * (hull.getMaxHeight() / ON_LOAD_HULL.getMaxHeight()))
                    );
                    CurvedGraphic overlayCurve = new CurvedGraphic(bezier, new Section(lControlX, rControlX), validAddKnotRectangle, false);
                    overlayCurve.getLinePath().setStrokeWidth(2.0);
                    overlayCurve.recolor(true);
                    overlayCurves.add(overlayCurve);
                }
                overlayZones.add(overlayZone);
            }
            overlayCurves.forEach(overlay -> overlay.setViewOrder(0));
            IntStream.range(0, 2).forEach(i -> hullGraphic.getChildren().get(i).setViewOrder(1));
            IntStream.range(2, hullGraphic.getChildren().size()).forEach(i -> hullGraphic.getChildren().get(i).setViewOrder(-1));
            overlayCurves.forEach(hullGraphic.getChildren()::add);
        }
        this.overlayZones = overlayZones;
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
    private void updateKnotEditingTitleLabel(boolean adding) {
        if (!knotEditorEnabled) return; // Cannot be adding/deleting a knot if not in knot editor
        String addingOrDeleting = adding ? "Adding" : "Deleting";
        double dataLabelPos = ON_LOAD_DATA_LABEL_POS;
        if (adding) dataLabelPos += 2;
        poiModeLabel.setLayoutX(dataLabelPos);
        poiModeLabel.setText(String.format("%s Knot Point", addingOrDeleting));
    }

    /**
     * Check if the mouse is in an x position to add a knot point while in knot editor mode
     * Visually, these (should be assuming all is working properly) are the colored section gaps in knot editor mode between non-overlapping bezier handles
     * @param mouseX the x position of the mouse
     */
    private boolean isMouseInAddingKnotPointZone(double mouseX) {
        if (!knotEditorEnabled) return false;
        if (mouseX >= hullGraphicPane.getLayoutX() && mouseX <= hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth()) {
            double poiX = mouseX - hullGraphicPane.getLayoutX();
            double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
            return overlayZones.stream()
                    .anyMatch(section -> functionSpaceX >= section.getX() && functionSpaceX <= section.getRx());
        }
        else return false;
    }

    /**
     * Check if the mouse is in an x position hovering over the edge knot point while in knot editor mode
     * @param mouseX the x position of the mouse
     */
    private boolean isMouseInEdgeKnotPointZone(double mouseX) {
        if (!knotEditorEnabled) return false;
        if (mouseX >= hullGraphicPane.getLayoutX() && mouseX <= hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth()) {
            // Pre-computation for next step
            double poiX = mouseX - hullGraphicPane.getLayoutX();
            double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
            double functionSpaceY = (CalculusUtils.getSegmentForX(hull.getSideViewSegments(), functionSpaceX)).value(functionSpaceX);
            Point2D functionSpacePoint = new Point2D(functionSpaceX, functionSpaceY);

            // If we rule out all other cases, we must be in an edge zone (an edge HullSection in terms of the old model)
            boolean isMouseInHullZone = isMouseInHullZone(knotEditingCurrentMouseX);
            boolean isMouseInDeletingZone = (HullGeometryService.getEditableKnotPoint(functionSpacePoint.getX()) != null);
            boolean isMouseInAddingZone = isMouseInAddingKnotPointZone(mouseX);
            return (isMouseInHullZone && !isMouseInDeletingZone && !isMouseInAddingZone);
        }
        else return false;
    }

    /**
     * Check if the mouse are in an x position hovering over the hull graphic
     * @param mouseX the x position of the mouse
     */
    private boolean isMouseInHullZone(double mouseX) {
        double poiX = mouseX - hullGraphicPane.getLayoutX();
        double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
        return functionSpaceX >= 0 && functionSpaceX <= hull.getLength();
    }

    /**
     * @param mousePos the position of the mouse (in graphics space, not function space)
     * @return if the mouse is hovering over the hull view anchor pane
     */
    private boolean isMouseOverHullViewAnchorPane(Point2D mousePos) {
        double mouseX = mousePos.getX();
        double mouseY = mousePos.getY();
        if (mouseX == -1 || mouseY == -1) return false;
        double layoutX = hullViewAnchorPane.getLayoutX();
        double layoutY = hullViewAnchorPane.getLayoutY();
        double width = hullViewAnchorPane.getWidth();
        double height = hullViewAnchorPane.getHeight();
        return mouseX >= layoutX && mouseX <= (layoutX + width) && mouseY >= layoutY && mouseY <= (layoutY + height);
    }

    /**
     * Render the graphic for the hull
     * @param hull to set from
     */
    public void renderHullGraphic(Hull hull) {
        // Set and layout parent pane
        double sideViewPanelWidth = 700;
        double sideViewPanelHeight = ON_LOAD_SIDE_VIEW_PANE_HEIGHT * (hull.getMaxHeight() / ON_LOAD_HULL.getMaxHeight());
        double paneX = hullViewAnchorPane.prefWidth(-1) / 2 - sideViewPanelWidth / 2;
        double paneY = hullViewAnchorPane.prefHeight(-1) / 2 - ON_LOAD_SIDE_VIEW_PANE_HEIGHT / 2;
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
        List<CubicBezierFunction> beziers = hull.getSideViewSegments();
        hullGraphic = new CubicBezierSplineHullGraphic(beziers, rect, true);

        // Add graphic to pane
        hullGraphicPane.getChildren().clear();
        hullGraphicPane.getChildren().add(hullGraphic);
        hullViewAnchorPane.getChildren().set(1, hullGraphicPane);
        applyGraphicsViewingState();

        // Keep the selected hull section colored
        if (selectedBezierSegment != null && selectedBezierSegmentIndex != -1)
            hullGraphic.colorBezierPointGroup(selectedBezierSegmentIndex, true);
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
     * Selects the next or previous Bezier segment.
     * @param isNext if true, selects the next segment; if false, selects the previous segment.
     */
    private void selectSegment(boolean isNext) {
        int delta = isNext ? 1 : -1;
        unlockKnobsOnFirstSectionSelect();
        if (!isNext) {
            if (selectedBezierSegmentIndex == -1) {
                selectedBezierSegmentIndex++;
            }
            selectedBezierSegmentIndex = (selectedBezierSegmentIndex + delta + hull.getSideViewSegments().size()) % hull.getSideViewSegments().size();
            hullGraphic.colorPreviousBezierPointGroup();
            previousPressedBefore = true;
        } else {
            selectedBezierSegmentIndex = (selectedBezierSegmentIndex + delta) % hull.getSideViewSegments().size();
            hullGraphic.colorNextBezierPointGroup();
            nextPressedBefore = true;
        }

        selectedBezierSegment = hull.getSideViewSegments().get(selectedBezierSegmentIndex);
        Section selectedSection = new Section(selectedBezierSegment.getX1(), selectedBezierSegment.getX2());
        if (sectionPropertiesSelected) setHullSectionProperties(hull, selectedSection);

        unboundKnobs();
        setKnobValues();
        setKnobBounds();
    }

    /**
     * Highlight the previous section to the left to view and edit.
     */
    public void selectPreviousHullSection(MouseEvent e) {
        selectSegment(false);
    }

    /**
     * Highlight the next section to the right to view and edit.
     */
    public void selectNextHullSection(MouseEvent e) {
        selectSegment(true);
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
     * Display properties in the bottom right panel with corresponding attributes
     */
    public void setHullPropertiesPaneValues(double height, double volume, double mass, double x, double rx) {
        String heightInfo = String.format("%.4f m", Math.abs(height));
        this.heightLabel.setText(heightInfo);
        String interval = String.format("(%.4f m, %.4f m)", x, rx);
        this.intervalLabel.setText(interval);
        String volumeFormated = String.format("%.4f m^3", volume);
        this.volumeLabel.setText(volumeFormated);
        String massFormated = String.format("%.4f kg", mass);
        this.massLabel.setText(massFormated);
    }

    /**
     * Shortcut to update the properties panel with the overall Canoe properties.
     * Uses the Canoe’s hull overall values.
     * @param hull the hull object from which to set properties in the bottom right pane
     */
    @Debounce(ms = 12)
    public void setHullProperties(Hull hull) {
        setHullPropertiesPaneValues(
                hull.getMaxHeight(),
                hull.getTotalVolume(),
                hull.getMass(),
                0,
                hull.getLength());
    }

    /**
     * Shortcut to update the properties panel with a selected hull section’s values.
     * Uses the provided Section to compute section-specific properties.
     * @param hull the hull object from which to set properties in the bottom right pane
     * @param section the Section representing the selected hull segment
     */
    @Debounce(ms = 12)
    public void setHullSectionProperties(Hull hull, Section section) {
        setHullPropertiesPaneValues(
                hull.getSectionSideViewCurveHeight(section),
                hull.getSectionVolume(section),
                hull.getSectionMass(section),
                section.getX(),
                section.getRx()
        );
    }

    /**
     * Calculates and sets the knob values in polar coordinates, for when a user goes to the next/prev section
      */
    private void setKnobValues() {
        // Batch update values, add back listeners
        List<Double> knobValues = HullGeometryService.getPolarParameterValues(selectedBezierSegment);
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
            setHullProperties(hull);
        }
        else {
            sectionPropertiesSelected = true;
            propertiesPanelTitleLabel.setText("Section Properties");

            // Set blank properties if no hull section is selected
            if (selectedBezierSegmentIndex == -1) {
                setBlankSectionProperties();
                return;
            }

            // Set the properties if a hull section is selected
            CubicBezierFunction bezier = hull.getSideViewSegments().get(selectedBezierSegmentIndex);
            Section section = new Section(bezier.getX1(), bezier.getX2());
            setHullSectionProperties(hull, section);
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
        if (selectedBezierSegment == null) return;

        // Update the relevant knob value
        knobs.get(knobIndex).setKnobValue(newROrThetaVal);

        // Update the model
        double[] knobValues = knobs.stream().mapToDouble(Knob::getValue).toArray();
        hull = HullGeometryService.updatePolarHullParameter(knobIndex, newROrThetaVal, knobValues);

        // Update UI with new hull
        hullGraphicPane.getChildren().clear();
        renderHullGraphic(hull);

        // If section properties are selected, update the selected section using its index.
        if (sectionPropertiesSelected && selectedBezierSegmentIndex >= 0 && selectedBezierSegmentIndex < hull.getSideViewSegments().size())
            selectedBezierSegment = hull.getSideViewSegments().get(selectedBezierSegmentIndex);

        recalculateAndDisplayHullProperties();

        // Update sibling knob bounds
        double siblingTheta = knobs.get((knobIndex % 2 == 0) ? knobIndex + 1 : knobIndex - 1).getValue();
        setSiblingKnobBounds(knobIndex, siblingTheta);
    }

    /**
     * Update the UI based on the current property selection.
     * If section properties are selected and a Bezier segment is active,
     * display its properties; otherwise display overall hull properties.
     */
    private void recalculateAndDisplayHullProperties() {
        if (sectionPropertiesSelected && selectedBezierSegment != null) {
            Section selectedSection = new Section(selectedBezierSegment.getX1(), selectedBezierSegment.getX2());
            setHullSectionProperties(hull, selectedSection);
        } else setHullProperties(hull);
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
            shiftKeyPressHadMouseInDraggableKnotPointZone = false;
            initialKnotDragMousePos = null;
            initialKnotDragKnotPos = null;

            // Other UI updates
            updateMouseXTrackerLine(event.getX());
            poiInfoLabel.setText("");
            updateKnotEditingTitleLabel(isMouseInAddingKnotPointZone(event.getX()));
            if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
            hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);

            // User exited during a drag (closed hand), not a drag preview (open hand)
            // Cancel the drag transaction & restore state from before dragging
            if (!isDraggingKnotPreview) {
                renderHullGraphic(hull);
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
        poiModeLabel.setOpacity(1);

        // Only perform computations if the sections editor is enabled
        if (!knotEditorEnabled) {
            if (intersectionPoint != null) intersectionPoint.setOpacity(0);
            if (intersectionXMark != null) intersectionXMark.setOpacity(0);
            if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
            return;
        }

        knotEditingCurrentMouseX = event.getX();
        knotEditingCurrentMouseY = event.getY();
        if (event.isShiftDown() && shiftKeyPressHadMouseInDraggableKnotPointZone) {
            if (!isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)) {
                // In dragging zone: hide the vertical tracker and POI markers, and update labels to indicate dragging.
                mouseXTrackerLine.setOpacity(0);
                if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                if (intersectionXMark != null) intersectionXMark.setOpacity(0);
                poiModeLabel.setText("Dragging Knot Point");
            }
            // If the mouse moves into the adding zone while Shift is not held:
            else if (!isDraggingKnot && !isDraggingKnotPreview) {
                initialKnotDragMousePos = null;
                initialKnotDragKnotPos = null;
                updateMouseXTrackerLine(knotEditingCurrentMouseX);
                poiInfoLabel.setText("");
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            }
            // Create the drag indicator line
            if (initialKnotDragKnotPos != null) {
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

        // When Shift is not held, update the UI for adding/deleting mode only
        else {
            updateMouseXTrackerLine(knotEditingCurrentMouseX);
            if (!isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)) {
                poiModeLabel.setText("Deleting Knot Point");
                double poiX = knotEditingCurrentMouseX - hullGraphicPane.getLayoutX();
                double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
                Point2D editableKnotPoint = HullGeometryService.getEditableKnotPoint(functionSpaceX);

                if (editableKnotPoint != null) {
                    if (Math.abs(Math.abs(editableKnotPoint.getY()) - Math.abs(hull.getMaxHeight())) < 1e-6)
                        poiModeLabel.setText("Minimum Knot Point");
                    if (!isShiftKeyPressed || shiftKeyPressHadMouseInDraggableKnotPointZone)
                        poiInfoLabel.setText("Hold Shift to Drag Knot Point");
                }
                else if (!isMouseInHullZone(knotEditingCurrentMouseX)) {
                    poiModeLabel.setOpacity(0);
                    poiInfoLabel.setText("");
                }
                else { // Edge section knot points cannot be dragged (i.e. they are locked in place)
                    poiModeLabel.setText("Locked Knot Point");
                    poiInfoLabel.setText("");
                }
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
            }
            else {
                poiModeLabel.setText("Adding Knot Point");
                poiInfoLabel.setText("");
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
            }
        }
    }

    /**
     * Render the mouse x tracker line along with the hull intersection point
     * @param mouseX the x position of the mouse at which the vertical line is at
     */
    private void updateMouseXTrackerLine(double mouseX) {
        double paneHeight = hullViewAnchorPane.getHeight();
        mouseXTrackerLine.setOpacity(0.7);
        mouseXTrackerLine.setStartX(mouseX);
        mouseXTrackerLine.setEndX(mouseX);
        mouseXTrackerLine.setStartY(0);
        mouseXTrackerLine.setEndY(paneHeight - 1);
        updateHullIntersectionPoint(mouseX);
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
            CubicBezierFunction bezier = hull.getSideViewSegments().stream()
                    .filter(s -> s.getX1() <= functionSpaceX && s.getX2() >= functionSpaceX)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cannot place intersection point, out of bounds"));
            functionSpaceY = bezier.value(functionSpaceX);
            double poiY = (functionSpaceY / hull.getMaxHeight()) * hullGraphicPane.getPrefHeight();
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
     * Converts the given function-space point to a formatted string (x: xVal, y: yVal) and updates poiDataLabel.
     * If a new point is provided, the label shows the transformation as: (x: oldVal, y: oldVal) ⇒ (x: newVal, y: newVal)
     * If the functionSpacePoint is null (or knot editing mode is disabled), it shows "(x: N/A, y: N/A)".
     * @param functionSpacePoint current function-space point (or null)
     * @param optionalFunctionSpaceUpdatedPoint new point after the operation (or null)
     */
    private void updateHullIntersectionPointDisplay(Point2D functionSpacePoint, Point2D optionalFunctionSpaceUpdatedPoint) {
        String pointData;
        String nullPointString = "(x: N/A, y: N/A)";

        // Display the correct state of stringified points
        if (functionSpacePoint != null && knotEditorEnabled) {
            double screenX = (functionSpacePoint.getX() / hull.getLength()) * hullGraphicPane.getWidth() + hullGraphicPane.getLayoutX();
            if (!isMouseInAddingKnotPointZone(screenX)) {
                Point2D displayKnotPoint = HullGeometryService.getEditableKnotPoint(functionSpacePoint.getX());

                // Edge knot points are not editable so displayKnotPoint is null, but we want to display the point anyway
                if (displayKnotPoint == null && isMouseInHullZone(knotEditingCurrentMouseX)) {
                    boolean isMouseOnLeftHalf = knotEditingCurrentMouseX < hullViewAnchorPane.getPrefWidth() / 2;
                    double functionSpaceX = isMouseOnLeftHalf ? 0 : hull.getLength();
                    double functionSpaceY = 0;
                    displayKnotPoint = new Point2D(functionSpaceX, functionSpaceY);
                }
                pointData = (displayKnotPoint == null)
                        ? nullPointString
                        : String.format("(x: %.3f, y: %.3f)", displayKnotPoint.getX(), displayKnotPoint.getY());
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
        double centerX = (poiModeLabel.getWidth() / 2) + poiModeLabel.getLayoutX();
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
        if (event.getButton() != MouseButton.PRIMARY) return;

        // Out-of-bounds check
        double mouseX = event.getX();
        if (mouseX < hullGraphicPane.getLayoutX() || mouseX > hullGraphicPane.getLayoutX() + hullGraphicPane.getWidth())
            return;

        // State updates
        isDraggingKnot = knotEditorEnabled && event.isShiftDown() && isDraggingKnotPreview;
        knotEditingMouseButtonDown = true;
        isDraggingKnotPreview = false;

        // Get the knot point we are editing
        double poiX = mouseX - hullGraphicPane.getLayoutX();
        double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
        Point2D editableKnot = HullGeometryService.getEditableKnotPoint(functionSpaceX);

        // Dragging Behaviour
        if (isDraggingKnot) {
            // Immediate State and UI updates
            dragStartTime = System.currentTimeMillis();
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());
            if (initialKnotDragKnotPos == null) initialKnotDragKnotPos = editableKnot;
            hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());
            poiInfoLabel.setText("Release Shift to Cancel Dragging");

            // Mouse dragged/released handlers attached and will take over from here
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            return;
        }

        // Must be Adding or Deleting Knot point behaviour if not dragging
        CubicBezierFunction bezier = hull.getSideViewSegments().stream()
                .filter(s -> s.getX1() <= functionSpaceX && s.getX2() >= functionSpaceX)
                .findFirst().orElseThrow(() -> new RuntimeException("Cannot place intersection point, out of bounds"));
        double functionSpaceY = bezier.value(functionSpaceX);

        // If no candidate deletable knot to delete exists, that means we should add a knot point
        Hull updatedHull = null;
        boolean isAddOperation = false;
        if (editableKnot == null && isMouseInAddingKnotPointZone(mouseX)) {
            updatedHull = HullGeometryService.addKnotPoint(new Point2D(functionSpaceX, functionSpaceY));
            isAddOperation = true;
        }
        // Otherwise, if a candidate deletable knot exists and there are enough sections, delete it.
        else if (hull.getSideViewSegments().size() > 2) updatedHull = HullGeometryService.deleteKnotPoint(editableKnot);

        // Hull State/graphics updates if the knot edit changed the hull
        if (updatedHull != null) {
            selectedBezierSegment = null;
            selectedBezierSegmentIndex = -1;
            nextPressedBefore = false;
            previousPressedBefore = false;
            recalculateAndDisplayHullProperties();
            renderHullGraphic(updatedHull);
            hull = updatedHull;
            toggleOrUpdateKnotEditingHullCurveOverlay(false);
        }

        // Display the appropriate snackbar message to the user
        if (updatedHull == null) {
            if (editableKnot == null) mainController.showSnackbar("Cannot delete edge knot point");
            else if (Math.abs(editableKnot.getY()) - Math.abs(hull.getMaxHeight()) < 1e-6) mainController.showSnackbar("Cannot delete the minimum knot point");
            else mainController.showSnackbar("Cannot delete knot point, too few sections");
        }
        else {
            if (isAddOperation) mainController.showSnackbar(String.format("Knot point added: (x = %.3f, y = %.3f)", functionSpaceX, functionSpaceY));
            else mainController.showSnackbar(String.format("Knot point deleted : (x = %.3f, y = %.3f)", editableKnot.getX(), editableKnot.getY()));
        }
    }

    /**
     * State and UI updates for dragging the mouse when using the drag knot feature
     * Uses HullGeometryService to query for and receive updated graphics as the knot is dragged
     */
    private void handleKnotDragMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        // isDraggingKnot should be updated before this handler is attached for use
        if (isDraggingKnot) {

            // Update State
            knotEditingCurrentMouseX = event.getX();
            knotEditingCurrentMouseY = event.getY();
            hullViewAnchorPane.setCursor(Cursor.CLOSED_HAND);

            // Get the min knot if it is the one being dragged
            Point2D minKnot = CalculusUtils.getSplineKnots(hull.getSideViewSegments()).stream()
                    .min(Comparator.comparingDouble(Point2D::getY))
                    .orElseThrow(() -> new IllegalStateException("No points found"));
            boolean isDraggingMinKnot = (minKnot.distance(initialKnotDragKnotPos) < 1e-6);

            // Get the new knot position after dragging
            double eps = 1e-3;
            double top = -eps;
            double globalMaxY = isDraggingMinKnot ? -TOP_ALLOWED_HULL_HEIGHT -eps: top;
            double globalMinY = isDraggingMinKnot ? -BOTTOM_ALLOWED_MAX_HULL_HEIGHT + eps : -hull.getMaxHeight() + eps;
            double minusAbsHeight = -Math.abs(hull.getMaxHeight());
            double initialKnotScreenX = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getX(), hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
            double initialKnotScreenY = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getY(), ON_LOAD_SIDE_VIEW_PANE_HEIGHT, minusAbsHeight) + hullGraphicPane.getLayoutY();
            double knotToMouseDistX = initialKnotScreenX - initialKnotDragMousePos.getX();
            double knotToMouseDistY = initialKnotScreenY - initialKnotDragMousePos.getY();
            double newKnotScreenX = knotEditingCurrentMouseX + knotToMouseDistX;
            double newKnotScreenY = knotEditingCurrentMouseY + knotToMouseDistY;
            double modelWidth = hull.getLength();
            double newKnotModelX = ((newKnotScreenX - hullGraphicPane.getLayoutX()) / hullGraphicPane.getPrefWidth()) * modelWidth;
            double newKnotModelY = ((newKnotScreenY - hullGraphicPane.getLayoutY()) / ON_LOAD_SIDE_VIEW_PANE_HEIGHT) * minusAbsHeight;
            newKnotDragKnotPos = new Point2D(newKnotModelX, newKnotModelY);

            // Clamp new knot position in its box
            double clampedNewKnotX;
            double clampedNewKnotY;
            boolean isDraggingLeft = newKnotDragKnotPos.getX() < initialKnotDragKnotPos.getX();
            double plusMinusEps = !isDraggingLeft ? eps : -eps;
            double adjacentSectionPointX = initialKnotDragKnotPos.getX() + plusMinusEps;
            CubicBezierFunction adjacentBezier = CalculusUtils.getSegmentForX(hull.getSideViewSegments(), adjacentSectionPointX);
            double outerXBoundary = isDraggingLeft ? adjacentBezier.getControlX1() : adjacentBezier.getControlX2(); // boundary against which the user might be dragging
            outerXBoundary -= 2 * plusMinusEps;
            if (isDraggingLeft) clampedNewKnotX = Math.max(newKnotDragKnotPos.getX(), outerXBoundary);
            else clampedNewKnotX = Math.min(newKnotDragKnotPos.getX(), outerXBoundary);
            if (isDraggingMinKnot) clampedNewKnotY = Math.max(2 * eps + globalMinY, Math.min(newKnotDragKnotPos.getY(), -TOP_ALLOWED_HULL_HEIGHT + 2 * eps));
            else clampedNewKnotY = Math.max(2 * eps + globalMinY, Math.min(newKnotDragKnotPos.getY(), 2 * globalMaxY));
            Point2D clampedNewKnotDragKnotPos = new Point2D(clampedNewKnotX, clampedNewKnotY);
            newKnotDragKnotPos = clampedNewKnotDragKnotPos;

            // From the clamped knot position, convert back to screen coordinates space with reversed math for the drag indicator line end point
            double clampedNewKnotModelX = clampedNewKnotDragKnotPos.getX();
            double clampedNewKnotModelY = clampedNewKnotDragKnotPos.getY();
            double layoutX = hullGraphicPane.getLayoutX();
            double layoutY = hullGraphicPane.getLayoutY();
            double prefWidth = hullGraphicPane.getPrefWidth();
            double clampedNewKnotScreenX = layoutX + (clampedNewKnotModelX / modelWidth) * prefWidth;
            double clampedNewKnotScreenY = layoutY + (clampedNewKnotModelY / minusAbsHeight) * (ON_LOAD_SIDE_VIEW_PANE_HEIGHT * (hull.getMaxHeight() / ON_LOAD_HULL.getMaxHeight()));

            // Update the drag indicator line
            dragIndicatorLine.setStartX(knotEditingCurrentMouseX);
            dragIndicatorLine.setStartY(knotEditingCurrentMouseY - 1);
            dragIndicatorLine.setEndX(clampedNewKnotScreenX);
            dragIndicatorLine.setEndY(clampedNewKnotScreenY);
            dragIndicatorLine.setVisible(true);

            // Update the size of the hull pane since it scales with the min knot, acting as a height scale for the hull
            if (isDraggingMinKnot) {
                double sideViewPanelHeight = ON_LOAD_SIDE_VIEW_PANE_HEIGHT * (newKnotDragKnotPos.getY() / ON_LOAD_HULL.getMaxHeight());
                hullGraphicPane.setPrefHeight(sideViewPanelHeight);
                hullGraphicPane.setMaxHeight(sideViewPanelHeight);
                hullGraphicPane.setMinHeight(sideViewPanelHeight);
            }

            // Call HullGeometryService to calculate the new hull after dragging, then render it and update the UI
            knotDraggingPreviewHull = HullGeometryService.dragKnotPoint(initialKnotDragKnotPos, newKnotDragKnotPos, isDraggingMinKnot ? minKnot : null);
            if (knotDraggingPreviewHull != null) {
                renderHullGraphic(knotDraggingPreviewHull);
                updateHullIntersectionPointDisplay(initialKnotDragKnotPos, newKnotDragKnotPos);
                if (sectionPropertiesSelected) setBlankSectionProperties();
                else setHullProperties(knotDraggingPreviewHull);
            }
        }
        else if (dragIndicatorLine != null) dragIndicatorLine.setVisible(false);
    }

    /**
     * State and UI update for releasing the mouse when using the drag knot feature
     * Locks in a new geometry with an edited knot, graphics update to reflect that
     */
    private void handleKnotDragMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        // User-action independent state update
        knotEditingMouseButtonDown = false;

        if (isDraggingKnot) {

            // Dragging State & UI Updates
            isDraggingKnot = false;
            poiInfoLabel.setText("Click and Hold to Drag");
            hullViewAnchorPane.setCursor(Cursor.OPEN_HAND);

            // Prevent short drags which are likely a misclick from the user, reverting the drag transaction
            if (System.currentTimeMillis() - dragStartTime <= 400) {
                updateHullIntersectionPointDisplay(initialKnotDragKnotPos, null);
                poiDataLabel.setLayoutX(ON_LOAD_DATA_LABEL_POS);
                double knotScreenX = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getX(),
                        hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
                double knotScreenY = GraphicsUtils.getScaledFromModelToGraphic(initialKnotDragKnotPos.getY(),
                        hullGraphicPane.getPrefHeight(), -Math.abs(hull.getMaxHeight())) + hullGraphicPane.getLayoutY();
                dragIndicatorLine.setEndX(knotScreenX);
                dragIndicatorLine.setEndY(knotScreenY);
            }
            // Commit the drag transaction
            else {
                // Before committing the drag, use the original hull's knot list to find the index of the dragged knot.
                List<Point2D> originalKnots = CalculusUtils.getSplineKnots(hull.getSideViewSegments());
                int knotIndex = -1;
                for (int i = 0; i < originalKnots.size(); i++) {
                    if (originalKnots.get(i).distance(initialKnotDragKnotPos) < 1e-6) {
                        knotIndex = i;
                        break;
                    }
                }
                // Retrieve the new knot from the preview hull at the same index
                if (knotIndex != -1) {
                    List<Point2D> newKnots = CalculusUtils.getSplineKnots(knotDraggingPreviewHull.getSideViewSegments());
                    if (knotIndex < newKnots.size()) initialKnotDragKnotPos = newKnots.get(knotIndex);
                }
                else throw new RuntimeException("Could not find knot to set up new drag operation");

                // Updating state and UI
                hull = knotDraggingPreviewHull;
                updateHullIntersectionPointDisplay(newKnotDragKnotPos, null);
                poiDataLabel.setLayoutX(ON_LOAD_DATA_LABEL_POS);
            }

            // More State & UI updates independent of if the atomic drag transaction went through
            renderHullGraphic(hull);
            if (sectionPropertiesSelected) setBlankSectionProperties();
            else setHullProperties(hull);
            toggleOrUpdateKnotEditingHullCurveOverlay(false);
            newKnotDragKnotPos = null;
            isDraggingKnotPreview = true;
            initialKnotDragMousePos = new Point2D(event.getX(), event.getY());

            // Remove these dynamic handlers for the drag knot feature
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
            hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
        }
    }

    /**
     * Downloads the current Hull object as a YAML file using the generic exporter.
     */
    public void downloadHull() {
        File downloadedFile = MarshallingService.exportObjectToYAML(hull, mainController.getPrimaryStage(), "hull");
        if (downloadedFile != null) mainController.showSnackbar("Successfully downloaded hull as \"" + downloadedFile.getName() + "\"");
        else mainController.showSnackbar("Download cancelled");
    }

    /**
     * Upload a YAML file representing the Hull object model
     * This populates the list view and beam graphic with the new model
     */
    public void uploadHull() {
        WindowManagerService.openUtilityWindow("Alert", "view/upload-alert-view.fxml", 350, 230);
    }

    /**
     * Detects the release of any key on the keyboard (only shift key functionality implemented at the moment)
     * @param event contains information about the event where some key was released
     */
    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT) {
            poiModeLabel.setOpacity(1);
            // If a knot is currently being dragged, releasing shift cancels the dragging
            if (isDraggingKnot) {
                // UI & State Updates
                isDraggingKnot = false;
                initialKnotDragMousePos = null;
                knotDraggingPreviewHull = null;
                updateMouseXTrackerLine(knotEditingCurrentMouseX);
                updateHullIntersectionPointDisplay(initialKnotDragKnotPos, null);
                initialKnotDragKnotPos = null;
                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                poiInfoLabel.setText("Click and Hold to Drag");
                renderHullGraphic(hull);

                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleKnotDragMouseDragged);
                hullViewAnchorPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, this::handleKnotDragMouseReleased);
            }
            isShiftKeyPressed = false;
            isDraggingKnotPreview = false;

            // Then update UI components based on the current mouse position.
            if (isMouseOverHullViewAnchorPane(new Point2D(knotEditingCurrentMouseX, knotEditingCurrentMouseY))) {
                if (isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)) {
                    poiModeLabel.setText("Adding Knot Point");
                    poiInfoLabel.setText("");
                } else if (!isMouseInEdgeKnotPointZone(knotEditingCurrentMouseX) && isMouseInHullZone(knotEditingCurrentMouseX)) {
                    poiModeLabel.setText("Deleting Knot Point");

                    double poiX = knotEditingCurrentMouseX - hullGraphicPane.getLayoutX();
                    double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
                    Point2D editableKnotPoint = HullGeometryService.getEditableKnotPoint(functionSpaceX);
                    if (editableKnotPoint != null && Math.abs(Math.abs(editableKnotPoint.getY()) - Math.abs(hull.getMaxHeight())) < 1e-6)
                        poiModeLabel.setText("Minimum Knot Point");

                    poiInfoLabel.setText("Hold Shift to Drag Knot Point");
                } else if (isMouseInHullZone(knotEditingCurrentMouseX)) {
                    poiModeLabel.setText("Locked Knot Point");
                    poiInfoLabel.setText("");
                }
                else {
                    poiModeLabel.setOpacity(0);
                    poiInfoLabel.setText("");
                }

                hullViewAnchorPane.setCursor(Cursor.CROSSHAIR);
                dragIndicatorLine.setVisible(false);
                updateMouseXTrackerLine(knotEditingCurrentMouseX);
            }
        }
    }

    /**
     * Detects the pressing of any key on the keyboard (only shift key functionality implemented at the moment)
     * @param event contains information about the event where some key was pressed
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.SHIFT) {
            if (isMouseInHullZone(knotEditingCurrentMouseX)) poiModeLabel.setOpacity(1);
            isShiftKeyPressed = true;
            // Calculate the candidate knot point (in function space) to drag based on the current mouse X.
            double poiX = knotEditingCurrentMouseX - hullGraphicPane.getLayoutX();
            double functionSpaceX = (poiX / hullGraphicPane.getWidth()) * hull.getLength();
            Point2D draggableKnot = HullGeometryService.getEditableKnotPoint(functionSpaceX);
            if (!knotEditingMouseButtonDown && isMouseOverHullViewAnchorPane(new Point2D(knotEditingCurrentMouseX, knotEditingCurrentMouseY))) {
                // Initiate "dragging preview mode" if there is a draggable knot
                if (draggableKnot != null) {
                    // Update state
                    isDraggingKnotPreview = true;
                    initialKnotDragKnotPos = HullGeometryService.getEditableKnotPoint(functionSpaceX);

                    // Update the drag indicator line
                    double knotScreenX = GraphicsUtils.getScaledFromModelToGraphic(draggableKnot.getX(), hullGraphicPane.getPrefWidth(), hull.getLength()) + hullGraphicPane.getLayoutX();
                    double knotScreenY = GraphicsUtils.getScaledFromModelToGraphic(draggableKnot.getY(), hullGraphicPane.getPrefHeight(), -Math.abs(hull.getMaxHeight())) + hullGraphicPane.getLayoutY();
                    dragIndicatorLine.setStartX(knotEditingCurrentMouseX);
                    dragIndicatorLine.setStartY(knotEditingCurrentMouseY - 1);
                    dragIndicatorLine.setEndX(knotScreenX);
                    dragIndicatorLine.setEndY(knotScreenY);
                    dragIndicatorLine.setVisible(true);
                    hullViewAnchorPane.setCursor(Cursor.OPEN_HAND);

                    // Hide vertical tracker and POI indicators while dragging.
                    mouseXTrackerLine.setOpacity(0);
                    if (intersectionPoint != null) intersectionPoint.setOpacity(0);
                    if (intersectionXMark != null) intersectionXMark.setOpacity(0);
                    poiModeLabel.setText("Dragging Knot Point");
                    poiInfoLabel.setText("Click and Hold to Drag");
                }
                else if (isMouseInEdgeKnotPointZone(knotEditingCurrentMouseX)) mainController.showSnackbar("Cannot drag edge knot point");
            }
            // Extra state update
            shiftKeyPressHadMouseInDraggableKnotPointZone =
                    !isMouseInAddingKnotPointZone(knotEditingCurrentMouseX)
                            && draggableKnot != null;
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
        MarshallingService.setHullBuilderController(this);
        HullGeometryService.setTopMostAllowedHullHeight(0.2);
        HullGeometryService.setBottomMostAllowedHullHeight(0.5);

        // Layout
        initModuleToolBarButtons();
        layoutKnobs();
        layoutPanelButtons();

        // Set default hull
        hullGraphicPane = new AnchorPane();
        hull = ON_LOAD_HULL;
        hullGraphicPane.setPrefHeight(ON_LOAD_SIDE_VIEW_PANE_HEIGHT);
        hullGraphicPane.setMaxHeight(ON_LOAD_SIDE_VIEW_PANE_HEIGHT);
        hullGraphicPane.setMinHeight(ON_LOAD_SIDE_VIEW_PANE_HEIGHT);
        renderHullGraphic(hull);
        setBlankSectionProperties();

        // Knot Editor
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

        poiModeLabel.setText("");
        poiDataLabel.setText("");
        poiInfoLabel.setText("");

        // Initialize state (yes I know this is getting bloated...)
        previousPressedBefore = false;
        nextPressedBefore = false;
        selectedBezierSegment = null;
        selectedBezierSegmentIndex = -1;
        sectionPropertiesSelected = true;
        graphicsViewingState = 0;
        knotEditorEnabled = false;
        isDraggingKnot = false;
        isDraggingKnotPreview = false;
        initialKnotDragKnotPos = null;
        initialKnotDragMousePos = null;
        knotEditingCurrentMouseX = -1;
        knotEditingCurrentMouseY = -1;
        knotEditingMouseButtonDown = false;
        isShiftKeyPressed = false;
        shiftKeyPressHadMouseInDraggableKnotPointZone = false;
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

        selectNextHullSection(null);
    }
}