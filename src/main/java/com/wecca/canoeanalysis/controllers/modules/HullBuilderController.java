package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.components.graphics.hull.CubicBezierSplineHullGraphic;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

import javafx.geometry.Point2D;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

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
public class HullBuilderController implements Initializable {

    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @FXML
    private Label intervalLabel, heightLabel, volumeLabel, massLabel, propertiesPanelTitleLabel;

    @Setter
    private static MainController mainController;
    @Getter
    private Hull hull;
    @Getter @Setter
    private CubicBezierSplineHullGraphic hullGraphic;
    private AnchorPane hullGraphicPane;
    private List<Knob> knobs;
    private List<ChangeListener<Number>> knobListeners;
    private Line mouseXTrackerLine;

    // State
    private boolean previousPressedBefore;
    private boolean nextPressedBefore;
    private HullSection selectedHullSection;
    private int selectedHullSectionIndex;
    private boolean sectionPropertiesSelected;
    private int graphicsViewingState;
    private boolean sectionEditorEnabled;

    // Numerical 'dx'
    private final double OPEN_INTERVAL_TOLERANCE = 1e-3;

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
     *
     * @param event the click event
     */
    private void toggleSectionsEditorMode(MouseEvent event) {
        sectionEditorEnabled = !sectionEditorEnabled;
        nextPressedBefore = false;
        previousPressedBefore = false;
        selectedHullSectionIndex = -1;

        List<Button> toolBarButtons = mainController.getModuleToolBarButtons();
        Button toggledIconButton = IconButton.getToolbarButton(sectionEditorEnabled ? IconGlyphType.X__PENCIL : IconGlyphType.PENCIL, this::toggleSectionsEditorMode);
        toolBarButtons.set(2, toggledIconButton);
        mainController.addOrSetToolBarButtons(toolBarButtons);
        hullViewAnchorPane.setCursor(sectionEditorEnabled ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        hullViewAnchorPane.setOnMouseEntered(sectionEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.CROSSHAIR) : null);
        hullViewAnchorPane.setOnMouseExited(sectionEditorEnabled ? e -> hullViewAnchorPane.setCursor(Cursor.DEFAULT) : null);
        hullViewAnchorPane.setOnMouseMoved(sectionEditorEnabled ? this::handleMouseMovedHullViewPane : null);
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
                setBlankSectionProperties();
                unboundKnobs();
                knobs.forEach(knobs -> knobs.setKnobValue(0));
            }
            knobs.forEach(knob -> knob.setLocked(sectionEditorEnabled));
            if (!sectionEditorEnabled) {
                for (int i = 0; i < knobs.size(); i++) {
                    knobs.get(i).valueProperty().addListener(knobListeners.get(i));
                }
            }
        }

        // Update mouse X tracker line visibility and style
        mouseXTrackerLine.setVisible(sectionEditorEnabled);
        if (sectionEditorEnabled) {
            mouseXTrackerLine.setStroke(ColorPaletteService.getColor("white"));
            mouseXTrackerLine.setOpacity(0.7);
            mouseXTrackerLine.getStrokeDashArray().setAll(5.0, 9.0);
            mouseXTrackerLine.setStartX(hullViewAnchorPane.getWidth() / 2);
            mouseXTrackerLine.setEndX(hullViewAnchorPane.getWidth() / 2);
            mouseXTrackerLine.setStartY(0);
            mouseXTrackerLine.setEndY(hullViewAnchorPane.getHeight());
        }

        // Notify the user
        mainController.showSnackbar(sectionEditorEnabled
                ? "Sections Editor Enabled"
                : "Sections Editor Disabled");
    }

    /**
     * Set the side view hull to build
     * @param hull to set from
     */
    public void setSideViewHullGraphic(Hull hull) {
        // Set and layout parent pane
        double sideViewPanelWidth = 700;
        double sideViewPanelHeight = 45;
        double paneX = hullViewAnchorPane.prefWidth(-1) / 2 - sideViewPanelWidth / 2;
        double paneY = hullViewAnchorPane.prefHeight(-1) / 2 - sideViewPanelHeight / 2;
        hullGraphicPane.setPrefSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setMaxSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setMinSize(sideViewPanelWidth, sideViewPanelHeight);
        hullGraphicPane.setLayoutX(paneX);
        hullGraphicPane.setLayoutY(paneY);

        // Setup graphic
        Rectangle rect = new Rectangle(0, 0, sideViewPanelWidth, sideViewPanelHeight);
        List<CubicBezierFunction> beziers = hull.getHullSections().stream().map(section -> {
            BoundedUnivariateFunction sideProfileCurveFunction = section.getSideProfileCurve();
            if (sideProfileCurveFunction instanceof CubicBezierFunction bezier)
                return bezier;
            else
                throw new RuntimeException("Not a bezier hull");
        }).toList();
        hullGraphic = new CubicBezierSplineHullGraphic(beziers, rect);

        // Add graphic to pane
        hullGraphicPane.getChildren().clear();
        hullGraphicPane.getChildren().add(hullGraphic);
        hullViewAnchorPane.getChildren().set(1, hullGraphicPane);
        applyGraphicsViewingState();

        // Keep the selected hull section colored
        if (selectedHullSection != null) {
            int selectedIndex = hull.getHullSections().indexOf(selectedHullSection);
            hullGraphic.colorBezierPointGroup(selectedIndex, true);
        }
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
        String heightInfo = String.format("%.4f m",height);
        this.heightLabel.setText(heightInfo);
        String interval = "("+x+" m, "+rx+" m)";
        this.intervalLabel.setText(interval);
        String volumeFormated = String.format("%.4f m^3",volume);
        this.volumeLabel.setText(volumeFormated);
        String massFormated = String.format("%.4f kg",mass);
        this.massLabel.setText(massFormated);
    }

    /**
     * Calculates and sets the knob values in polar coordinates, for when a user goes to the next/prev section
      */
    private void setKnobValues() {
        if (!(selectedHullSection.getSideProfileCurve() instanceof CubicBezierFunction bezier))
            throw new IllegalArgumentException("Cannot work in Hull Builder with non-bezier hull curve");

        List<Point2D> knotAndControlPoints = bezier.getKnotAndControlPoints();
        Point2D polarL = CalculusUtils.toPolar(knotAndControlPoints.get(1), knotAndControlPoints.get(0));
        Point2D polarR = CalculusUtils.toPolar(knotAndControlPoints.get(2), knotAndControlPoints.get(3));
        List<Double> knobValues = List.of(polarL.getX(), polarL.getY(), polarR.getX(), polarR.getY());

        // Batch update values, add back listeners
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().removeListener(knobListeners.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).setKnobValue(knobValues.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().addListener(knobListeners.get(i));}
    }

    /**
     * In an r-θ pair of knobs, the bounds of one variable are relative to the other.
     * Thus, if we update r, we must adjust the θ bounds, and vice versa.
     */
    @Traceable
    private void setSiblingKnobBounds(int knobIndex, double currTheta) {
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

        // Get the corresponding knot point
        CubicBezierFunction bezier = (CubicBezierFunction) selectedHullSection.getSideProfileCurve();
        Point2D knot = (knobIndex < 2) ? bezier.getKnotPoints().getFirst() : bezier.getKnotPoints().getLast();

        // Update bounds for the sibling knob
        if (knobIndex % 2 == 0) { // Updating r, adjust θ bounds
            double[] thetaBounds = calculateThetaBounds(knot, r, currTheta, knobIndex == 0, false, selectedHullSectionIndex);

            // Apply adjacent section theta bounds if applicable (returns null if the knot is an edge knot)
            double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, knobIndex == 0, currTheta);
            double minTheta = additionalThetaBounds != null
                    ? Math.max(thetaBounds[0], additionalThetaBounds[0])
                    : thetaBounds[0];
            double maxTheta = additionalThetaBounds != null
                    ? Math.max(Math.min(thetaBounds[1], additionalThetaBounds[1]), minTheta)
                    : Math.max(thetaBounds[1], minTheta);

            // This prevents the user the ignore the bounds if they hold down the plus/minus knob button
            // Essentially clipping the last 0.5 of a degree off the bounds as a buffer
            // This is needed because we clip the point when increasing r to 1e-3 less than the max
            // This leaves some wiggle room on the theta bounds which we want to remove to prevent increasing past the bounds
            double adjustedMinTheta = Math.abs(minTheta - currTheta) < 0.5 ? currTheta : minTheta;
            double adjustedMaxTheta = Math.abs(maxTheta - currTheta) < 0.5 ? currTheta : maxTheta;

            siblingKnob.setKnobMin(adjustedMinTheta);
            siblingKnob.setKnobMax(adjustedMaxTheta);
        } else { // Updating θ, adjust r bounds
            double rMax = calculateMaxR(knot, theta, selectedHullSectionIndex);
            siblingKnob.setKnobMax(rMax);
        }
    }

    /**
     * Sets the bounds of the knobs so that the control points stay in the rectangle bounded by:
     * x = 0, x = L, y = 0, y = -h.
     * The height h comes from the front view and corresponds to the lowest y-value of the knot point.
     *
     * MUST BE DONE AFTER SETTING KNOB VALUES WHEN SWITCHING SECTIONS
     */
    private void setKnobBounds() {
        // Validation
        if (!(selectedHullSection.getSideProfileCurve() instanceof CubicBezierFunction bezier))
            throw new IllegalArgumentException("Cannot work with non-bezier hull curve");

        // Get bezier knot points
        List<Point2D> knotPoints = bezier.getKnotPoints();
        Point2D lKnot = knotPoints.getFirst();
        Point2D rKnot = knotPoints.getLast();

        // Get r and theta values
        Point2D polarL = CalculusUtils.toPolar(new Point2D(bezier.getControlX1(), bezier.getControlY1()), lKnot);
        Point2D polarR = CalculusUtils.toPolar(new Point2D(bezier.getControlX2(), bezier.getControlY2()), rKnot);
        double rL = polarL.getX();
        double rR = polarR.getX();
        double thetaL = polarL.getY();
        double thetaR = polarR.getY();

        // Calculate r and theta bounds for the control point to stay in the rectangle
        double rMin = 0.001;
        double rLMax = calculateMaxR(lKnot, thetaL, selectedHullSectionIndex);
        double rRMax = calculateMaxR(rKnot, thetaR, selectedHullSectionIndex);
        double[] thetaLBounds = calculateThetaBounds(lKnot, rL, thetaL, true, true, selectedHullSectionIndex);
        double thetaLMin = thetaLBounds[0];
        double thetaLMax = thetaLBounds[1];
        double[] thetaRBounds = calculateThetaBounds(rKnot, rR, thetaR, false, true, selectedHullSectionIndex);
        double thetaRMin = thetaRBounds[0];
        double thetaRMax = thetaRBounds[1];

        // Set bounds
        knobs.get(0).setKnobMin(rMin);
        knobs.get(0).setKnobMax(rLMax);
        knobs.get(1).setKnobMin(thetaLMin);
        knobs.get(1).setKnobMax(thetaLMax);
        knobs.get(2).setKnobMin(rMin);
        knobs.get(2).setKnobMax(rRMax);
        knobs.get(3).setKnobMin(thetaRMin);
        knobs.get(3).setKnobMax(thetaRMax);
    }

    /**
     * Calculates the maximum radius (r) such that a point at (r, theta) from an origin/knot (xO, yO)
     * remains within the rectangular bounds defined by x = 0, x = L, y = 0, y = -h.
     *
     * @param knot the knot point which acts as the origin
     * @param thetaKnown the known angle in degrees (relative to the origin) at which the point lies.
     * @param hullSectionIndex the index of the hullSection in the hull in which to bound the radius
     * @return The maximum radius (r) such that the point stays within the bounds.
     */
    public double calculateMaxR(Point2D knot, double thetaKnown, int hullSectionIndex) {
        HullSection hullSection = hull.getHullSections().get(hullSectionIndex);
        double hullHeight = -hull.getMaxHeight();
        CubicBezierFunction bezier = (CubicBezierFunction) hullSection.getSideProfileCurve();
        double xL = bezier.getX1();
        double xR = bezier.getX2();

        double rMax = Double.MAX_VALUE;
        double thetaRad = Math.toRadians((thetaKnown + 90) % 360);
        double cosTheta =  Math.cos(thetaRad);
        double sinTheta = Math.sin(thetaRad);
        double approximatelyZeroRadians = Math.toRadians(OPEN_INTERVAL_TOLERANCE);

        // Use CAST rule to determine which rectangle boundaries to calculate rMax
        // Edge cases of 0/90/180/270/360 handled appropriately
        double sinOfApproximatelyZeroRadians = Math.sin(approximatelyZeroRadians);
        if (Math.abs(sinTheta) != Math.abs(sinOfApproximatelyZeroRadians)) {
            if (cosTheta < 0) {
                double rFromLeft = (knot.getX() - xL) / -cosTheta;
                rMax = Math.min(rMax, rFromLeft);
            } else if (cosTheta > 0) {
                double rFromRight = (xR - knot.getX()) / cosTheta;
                rMax = Math.min(rMax, rFromRight);
            }
        }
        double cosOfApproximatelyZeroRadians = Math.cos(approximatelyZeroRadians);
        if (Math.abs(cosTheta) != Math.abs(cosOfApproximatelyZeroRadians)) {
            if (sinTheta < 0) {
                double rFromBottom = (hullHeight - knot.getY()) / sinTheta;
                rMax = Math.min(rMax, rFromBottom);
            } else if (sinTheta > 0) {
                double rFromTop = -knot.getY() / sinTheta;
                rMax = Math.min(rMax, rFromTop);
            }
        }

       return Math.max(0, rMax);
    }

    /**
     * Returns the allowable angular range for a control point on a circle within a bounding rectangle.
     * Merges constraints from adjacent hull sections if needed, and applies a small tolerance so the range
     * does not collapse. If it does collapse, the range is set to a single value.
     *
     * @param knot The reference point for polar coordinates
     * @param rKnown The radial distance from the knot to the control point
     * @param currTheta The current angle (in degrees)
     * @param isLeft True if the control point is on the left side (180–360 degrees), false otherwise (0–180)
     * @param boundWithAdjacentSections True if the range should account for constraints from adjacent sections
     * @param hullSectionIndex The index of the hull section to process for bounding
     * @return A two-element array containing the minimum and maximum valid theta values
     */
    public double[] calculateThetaBounds(Point2D knot, double rKnown, double currTheta, boolean isLeft, boolean boundWithAdjacentSections, int hullSectionIndex) {
        HullSection hullSection = hull.getHullSections().get(hullSectionIndex);
        double l = hullSection.getLength();
        double h = -hull.getMaxHeight();
        Rectangle boundingRect = new Rectangle(hullSection.getX(), 0.0, l, h);

        double[] rawThetaBounds = calculateRawThetaBounds(boundingRect, knot, rKnown, isLeft, currTheta);
        double minTheta = rawThetaBounds[0];
        double maxTheta = rawThetaBounds[1];

        if (boundWithAdjacentSections) {
            double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, isLeft, currTheta);
            minTheta = additionalThetaBounds != null
                    ? Math.max(minTheta, additionalThetaBounds[0])
                    : minTheta;
            maxTheta = additionalThetaBounds != null
                    ? Math.min(maxTheta, additionalThetaBounds[1])
                    : maxTheta;
            if (maxTheta < minTheta) maxTheta = minTheta;
        }

        if (Math.abs(minTheta - maxTheta) < 1e-2) {
            double avg = 0.5 * (minTheta + maxTheta);
            return new double[] {avg, avg};
        }

        if (Math.abs(minTheta - currTheta) <= OPEN_INTERVAL_TOLERANCE) minTheta = currTheta;
        else minTheta += OPEN_INTERVAL_TOLERANCE;
        if (Math.abs(maxTheta - currTheta) <= OPEN_INTERVAL_TOLERANCE) maxTheta = currTheta;
        else maxTheta -= OPEN_INTERVAL_TOLERANCE;
        return new double[] {minTheta, maxTheta};
    }

    /**
     * Finds the valid range of angles where a control point on a circle stays within the specified rectangle.
     * Determines the domain based on whether it's the left side (180–360) or the right side (0–180),
     * then solves circle-rectangle intersections and selects the smallest segment containing currTheta.
     *
     * @param boundingRect The rectangle bounding the control point
     * @param center The circle's center
     * @param radius The circle's radius
     * @param isLeft True if searching 180–360, false if 0–180
     * @param currTheta The current angle (in degrees) for which to constrain the range
     * @return An array with [minTheta, maxTheta] that confines the control point within the rectangle
     */
    private double[] calculateRawThetaBounds(Rectangle boundingRect, Point2D center, double radius, boolean isLeft, double currTheta) {
        // Initialize the search domain
        double thetaMin = isLeft ? 180 : 0;
        double thetaMax = isLeft ? 360 : 180;

        // Rectangle Bounds
        double xMin = boundingRect.getX();
        double xMax = xMin + boundingRect.getWidth();
        double yMax = boundingRect.getY();
        double yMin = yMax + boundingRect.getHeight(); // negative height

        // Build candidate angle bounds list alpha_i as POIs of the rectangle and circular arc
        // Circle formed by sweeping the search domain at the given radius
        List<Double> angles = new ArrayList<>();
        addIntersectionAngles(angles, center, radius, xMin, true);
        addIntersectionAngles(angles, center, radius, xMax, true);
        addIntersectionAngles(angles, center, radius, yMin, false);
        addIntersectionAngles(angles, center, radius, yMax, false);
        angles = angles.stream().map(x -> ((x % 360) + 360) % 360).filter(x -> (x > thetaMin && x < thetaMax)).sorted().toList();


        // Handle cases for number of POIs alpha_i
        int n = angles.size();
        if (n == 0) return new double[] {thetaMin, thetaMax};
        if (n == 2) return new double[] {angles.getFirst(), angles.getLast()};
        else if (n == 1) {
            double alpha = angles.getFirst();
            if (currTheta < alpha) {
                double mid = 0.5 * (thetaMin + alpha);
                if (isPointInBounds(radius, mid, center, boundingRect)) return new double[] {thetaMin, alpha};
                else return new double[] {alpha, thetaMax};
            } else {
                double mid = 0.5 * (alpha + thetaMax);
                if (isPointInBounds(radius, mid, center, boundingRect)) return new double[] {alpha, thetaMax};
                else return new double[] {thetaMin, alpha};
            }
        }
        // > 2 POIs found, find the range containing currTheta
        else {
            for (int i = 0; i < n - 1; i++) {
                double start = angles.get(i);
                double end = angles.get(i + 1);
                if (currTheta >= start && currTheta <= end) return new double[] {start, end};
            }
            return new double[] {thetaMin, thetaMax};
        }
    }

    /**
     * Finds intersection angles for a circle and a vertical (x=val) or horizontal (y=val) line.
     * The angles are calculated using the calculus convention where the 0-degree reference is shifted 90 degrees forward.
     *
     * @param angles List to store the calculated angles.
     * @param knot The center of the circle.
     * @param r The radius of the circle.
     * @param lineVal The value of the vertical or horizontal line (x or y).
     * @param isX True for vertical line (x=val), false for horizontal line (y=val).
     */
    private void addIntersectionAngles(List<Double> angles, Point2D knot, double r, double lineVal, boolean isX) {
        double knot1 = isX ? knot.getX() : knot.getY();
        double knot2 = isX ? knot.getY() : knot.getX();
        double distance = lineVal - knot1;
        double sq = r * r - distance * distance;

        // Required condition for intersection angles
        if (sq >= 0) {
            double tolerance = 1e-8 * r * r;
            if (Math.abs(sq) < tolerance) { // Numerical tangency, only one intersection point
                double angle = CalculusUtils.toPolar(isX ? new Point2D(lineVal, knot2) : new Point2D(knot2, lineVal), knot).getY();
                angles.add(angle);
            } else { // There must be exactly 2 intersection points
                double root = Math.sqrt(sq);
                double intersect1 = knot2 + root;
                double intersect2 = knot2 - root;
                angles.add(CalculusUtils.toPolar(isX ? new Point2D(lineVal, intersect1) : new Point2D(intersect1, lineVal), knot).getY());
                angles.add(CalculusUtils.toPolar(isX ? new Point2D(lineVal, intersect2) : new Point2D(intersect2, lineVal), knot).getY());
            }
        }
    }

    /**
     * Checks if a control point, specified in polar coordinates, lies within the bounds of a given rectangle.
     * @param rKnown The radius of the control point in polar coordinates.
     * @param thetaGuess The angle (in radians) of the control point in polar coordinates.
     * @param knot The knot point (reference point) to which the polar coordinates are relative.
     * @return True if the control point lies within the bounds of the rectangle;
     */
    private boolean isPointInBounds(double rKnown, double thetaGuess, Point2D knot, Rectangle boundingRect) {
        // Convert polar to Cartesian
        Point2D cartesianControl = CalculusUtils.toCartesian(new Point2D(rKnown, thetaGuess), knot);
        double xControl = cartesianControl.getX();
        double yControl = cartesianControl.getY();

        // Rectangle bounds
        double xMin = boundingRect.getX();
        double xMax = xMin + boundingRect.getWidth();
        double yMax = boundingRect.getY();
        double yMin = yMax + boundingRect.getHeight(); // negative height

        // Fix floating point proximity
        if (Math.abs(xControl - xMin) < 1e-6) xControl = xMin;
        if (Math.abs(xControl - xMax) < 1e-6) xControl = xMax;
        if (Math.abs(yControl - yMin) < 1e-6) yControl = yMin;
        if (Math.abs(yControl - yMax) < 1e-6) yControl = yMax;

        // Check standard rectangle inclusion
        return (xControl >= xMin && xControl <= xMax &&
                yControl >= yMin && yControl <= yMax);
    }

    /**
     * Calculates additional theta bounds based on adjacent sections' control points to ensure smoothness and continuity.
     *
     * @param knot the knot point which acts as the origin
     * @param isLeft whether the control point belongs to the left knot or right knot
     * @param currTheta, the current theta value before the updated geometry, of the original section (NOT the adjacent section)
     * @return [additionalMinTheta, additionalMaxTheta], or null for an edge knot (the first and last knot with no adjacent sections they are shared with)
     */
    private double[] calculateAdjacentSectionThetaBounds(Point2D knot, boolean isLeft, double currTheta) {
        double thetaMin = isLeft ? 180 : 0;
        double thetaMax = isLeft ? 360 : 180;

        int adjacentHullSectionIndex = selectedHullSectionIndex + (isLeft ? -1 : 1);
        boolean hasAdjacentSection = isLeft
                ? selectedHullSectionIndex > 0
                : selectedHullSectionIndex < hull.getHullSections().size() - 1;

        if (hasAdjacentSection) {
            HullSection adjacentHullSection = hull.getHullSections().get(adjacentHullSectionIndex);
            Point2D siblingPoint = isLeft
                    ? ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getLast()
                    : ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
            double siblingPointR = CalculusUtils.toPolar(siblingPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, siblingPointR, (currTheta + 180) % 360, !isLeft, false, adjacentHullSectionIndex);

            thetaMin = Math.max(thetaMin, (thetaBounds[0] + 180) % 360);
            thetaMax = Math.min(thetaMax, (thetaBounds[1] + 180) % 360);
            return new double[] {thetaMin, thetaMax};
        }
        else return null;
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
        double minR = OPEN_INTERVAL_TOLERANCE;
        double maxPossibleR = hull.getLength();
        double minPossibleTheta = OPEN_INTERVAL_TOLERANCE;
        double maxPossibleTheta = 360 - OPEN_INTERVAL_TOLERANCE;

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
     *
     * IMPORTANT DISTINCTION:
     *
     * In order to maintain C1 smoothness, the angle must match for both sides of all bezier handles.
     * Since handles lie of the edges of sections, adjusting a handle requires moving the control point in the adjacent section
     * Thus we differentiate between the data of the selected and adjacent hull sections
     *
     * @param knobIndex the index of the knob that was changed (knobs indexed L to R in increasing order from 0)
     * @param newROrThetaVal the new value for the knob (after user interaction)
     */
    @Traceable
    private void updateSystemFromKnob(int knobIndex, double newROrThetaVal) {
        if (selectedHullSection == null) return;

        // Update the relevant knob value
        knobs.get(knobIndex).setKnobValue(newROrThetaVal);

        // Get current knob values
        double selectedRL = knobs.get(0).getValue();
        double selectedThetaL = knobs.get(1).getValue();
        double selectedRR = knobs.get(2).getValue();
        double selectedThetaR = knobs.get(3).getValue();

        // Update the relevant control point from polar coordinates
        List<Point2D> selectedKnotPoints = ((CubicBezierFunction) selectedHullSection.getSideProfileCurve()).getKnotPoints();
        Point2D selectedLKnot = selectedKnotPoints.getFirst();
        Point2D selectedRKnot = selectedKnotPoints.getLast();
        CubicBezierFunction selectedBezier = (CubicBezierFunction) selectedHullSection.getSideProfileCurve();
        if (knobIndex == 0 || knobIndex == 1) { // Left control point
            Point2D lControl = CalculusUtils.toCartesian(new Point2D(selectedRL, selectedThetaL), selectedLKnot);
            selectedBezier.setControlX1(lControl.getX());
            selectedBezier.setControlY1(lControl.getY());
        } else if (knobIndex == 2 || knobIndex == 3) { // Right control point
            Point2D rControl = CalculusUtils.toCartesian(new Point2D(selectedRR, selectedThetaR), selectedRKnot);
            selectedBezier.setControlX2(rControl.getX());
            selectedBezier.setControlY2(rControl.getY());
        }

        // When changing theta we need to adjust adjacent sections to maintain C1 smoothness
        // Calculate new control point in Cartesian coordinates, derived by adjusting the angle (theta)
        // while keeping the distance (radius) constant relative to selectedLKnot.
        boolean adjustingLeftAdjacentSection = knobIndex == 1 && selectedHullSectionIndex != 0;
        boolean adjustingRightAdjacentSection = knobIndex == 3 && selectedHullSectionIndex != (hull.getHullSections().size() - 1);

        // Compute deltas for the adjacent control point
        if (adjustingLeftAdjacentSection || adjustingRightAdjacentSection) {
            int adjacentHullSectionIndex = selectedHullSectionIndex + (adjustingLeftAdjacentSection ? -1 : 1);
            HullSection adjacentHullSection = hull.getHullSections().get(adjacentHullSectionIndex);
            Point2D selectedKnot = adjustingLeftAdjacentSection ? selectedLKnot : selectedRKnot;
            Point2D deltas = calculateAdjacentControlDeltas(adjacentHullSection, adjustingLeftAdjacentSection, newROrThetaVal, selectedKnot);
            double deltaX = deltas.getX();
            double deltaY = deltas.getY();

            // Propagate the change to the adjacent control point
            HullSection adjustedAdjacentSection = adjustAdjacentSectionControlPoint(
                    adjacentHullSection, knobIndex == 1, deltaX, deltaY);
            hull.getHullSections().set(adjacentHullSectionIndex, adjustedAdjacentSection);
        }

        // Update UI
        hullGraphicPane.getChildren().clear();
        setSideViewHullGraphic(hull);
        recalculateAndDisplayHullProperties();

        double currTheta = (knobIndex == 0 || knobIndex == 1) ? selectedThetaL : selectedThetaR;
        setSiblingKnobBounds(knobIndex, currTheta);
    }

    /**
     * Adjusts the control points of the adjacent hull sections to maintain C1 continuity.
     * @param adjacentSection the adjacent section (either on the left or right) to adjust
     *                        the correct section must be passed in, this logic is not in the method
     * @param adjustLeftOfSelected whether to adjust the adjacent left section (otherwise right)
     * @param deltaX the amount by which to adjust the control point x in the adjacent section
     * @param deltaY the amount by which to adjust the control point y in the adjacent section
     */
    private HullSection adjustAdjacentSectionControlPoint(HullSection adjacentSection, boolean adjustLeftOfSelected, double deltaX, double deltaY) {
        CubicBezierFunction adjacentBezier = (CubicBezierFunction) adjacentSection.getSideProfileCurve();

        // Adjust the control point by the delta
        if (adjustLeftOfSelected) {
            adjacentBezier.setControlX2(adjacentBezier.getControlX2() + deltaX);
            adjacentBezier.setControlY2(adjacentBezier.getControlY2() + deltaY);
        } else {
            adjacentBezier.setControlX1(adjacentBezier.getControlX1() + deltaX);
            adjacentBezier.setControlY1(adjacentBezier.getControlY1() + deltaY);
        }

        // Update the section's side profile curve
        adjacentSection.setSideProfileCurve(adjacentBezier);
        return adjacentSection;
    }

    /**
     * Computes the new control point and deltas for adjusting the control point of an adjacent hull section
     * to maintain C1 smoothness. The computation is based on polar coordinate transformations.
     *
     * @param adjacentHullSection       The adjacent hull section being adjusted.
     * @param isLeftAdjacentSection     True if adjusting the left-adjacent section, false otherwise.
     * @param newThetaVal               The new angle (theta) value for the selected control point.
     * @param selectedKnot              The knot point used as the reference for polar transformations.
     * @return A Point2D array where:
     *         index 0 contains the deltaX,
     *         index 1 contains the deltaY.
     */
    private Point2D calculateAdjacentControlDeltas(HullSection adjacentHullSection, boolean isLeftAdjacentSection, double newThetaVal, Point2D selectedKnot) {
        // Determine old control point and polar radius (r)
        Point2D adjacentSectionOldControl;
        double adjacentSectionRadius;
        double adjacentSectionNewThetaVal = (newThetaVal + 180) % 360;

        if (isLeftAdjacentSection)
            adjacentSectionOldControl = ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getLast();
        else
            adjacentSectionOldControl = ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
        adjacentSectionRadius = CalculusUtils.toPolar(adjacentSectionOldControl, selectedKnot).getX();

        // Compute new control point in Cartesian coordinates
        Point2D adjacentSectionNewControl = CalculusUtils.toCartesian(
                new Point2D(adjacentSectionRadius, adjacentSectionNewThetaVal), selectedKnot);

        // Calculate and return deltas
        double deltaX = adjacentSectionNewControl.getX() - adjacentSectionOldControl.getX();
        double deltaY = adjacentSectionNewControl.getY() - adjacentSectionOldControl.getY();
        return new Point2D(deltaX, deltaY);
    }

    /**
     * Update the UI based on the current property selection
     */
    private void recalculateAndDisplayHullProperties() {
        if (sectionPropertiesSelected && selectedHullSection != null) {
            setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),
                    selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        } else {
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
            knobListeners.add((observable, oldValue, newValue) -> updateSystemFromKnob(finalI, newValue.doubleValue()));
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
     * Handles mouse movement within the hull view pane. Updates the vertical tracker line and checks for hull intersection.
     */
    private void handleMouseMovedHullViewPane(MouseEvent event) {
        double mouseX = event.getX();
        double paneHeight = hullViewAnchorPane.getHeight();

        // Update the vertical line's position
        mouseXTrackerLine.setStartX(mouseX);
        mouseXTrackerLine.setEndX(mouseX);
        mouseXTrackerLine.setStartY(0);
        mouseXTrackerLine.setEndY(paneHeight - 1); // weird out of panel bounds bug without -1px

        // TODO
        // Check for intersection with the hull
//            Point2D hullIntersection = findHullIntersection(mouseX);
//            if (hullIntersection != null) {
//                // Show the intersection point on the hull
//                hullGraphic.displayIntersectionPoint(hullIntersection);
//            } else {
//                hullGraphic.hideIntersectionPoint();
//            }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());
        initModuleToolBarButtons();

        // Layout
        layoutKnobs();
        layoutPanelButtons();

        // Set default hull
        hullGraphicPane = new AnchorPane();
        hull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromBezier(6);
        setSideViewHullGraphic(hull);
        setBlankSectionProperties();

        // Initialize state
        previousPressedBefore = false;
        nextPressedBefore = false;
        selectedHullSection = null;
        selectedHullSectionIndex = -1;
        sectionPropertiesSelected = true;
        graphicsViewingState = 0;

        // Mouse tracker line
        mouseXTrackerLine = new Line();
        mouseXTrackerLine.setVisible(false);
        hullViewAnchorPane.getChildren().addLast(mouseXTrackerLine);
    }
}
