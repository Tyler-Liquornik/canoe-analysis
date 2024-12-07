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
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
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

    // State
    private boolean previousPressedBefore;
    private boolean nextPressedBefore;
    private HullSection selectedHullSection;
    private int selectedHullSectionIndex;
    private boolean sectionPropertiesSelected;
    private int graphicsViewingState;

    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        // TODO: add functions to buttons
        iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.SCISSORS, e -> dummy());
        iconGlyphToFunctionMap.put(IconGlyphType.CHAIN, e -> dummy());
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    public void dummy() {
    }

    public void dummyOnClick(MouseEvent event) {
    }

    /**
     * Set the side view hull to build
     *
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
        String heightInfo = String.format("%.2f m",height);
        this.heightLabel.setText(heightInfo);
        String interval = "("+x+" m, "+rx+" m)";
        this.intervalLabel.setText(interval);
        String volumeFormated = String.format("%.2f m^3",volume);
        this.volumeLabel.setText(volumeFormated);
        String massFormated = String.format("%.2f kg",mass);
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
     * Sets the bounds of the knobs so that the control points stay in the rectangle bounded by:
     * x = 0, x = L, y = 0, y = -h.
     * The height h comes from the front view and corresponds to the lowest y-value of the knot point.
     *
     * MUST BE DONE AFTER SETTING KNOB VALUES
     *
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
        double rLMax = calculateMaxR(lKnot, thetaL);
        double rRMax = calculateMaxR(rKnot, thetaR);
        double[] thetaLBounds = calculateThetaBounds(lKnot, rL, true, true);
        double thetaLMin = thetaLBounds[0];
        double thetaLMax = thetaLBounds[1];
        double[] thetaRBounds = calculateThetaBounds(rKnot, rR, false, true);
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
     * @return The maximum radius (r) such that the point stays within the bounds.
     */
    public double calculateMaxR(Point2D knot, double thetaKnown) {
        double rMax = Double.MAX_VALUE;
        double thetaRad = Math.toRadians((thetaKnown + 90) % 360);
        double cosTheta =  CalculusUtils.roundXDecimalDigits(Math.cos(thetaRad), 3);
        double sinTheta = CalculusUtils.roundXDecimalDigits(Math.sin(thetaRad), 3);

        // Check quadrant of R^2 using CAST rule to determine which bounds of the rectangle to check
        // Only need to check the two outer edges of the quadrant
        if (cosTheta < 0) {
            double rFromLeft = -knot.getX() / cosTheta;
            rMax = Math.min(rMax, rFromLeft);
        } else if (cosTheta > 0) {
            double rFromRight = (hull.getLength() - knot.getX()) / cosTheta;
            rMax = Math.min(rMax, rFromRight);
        }
        if (sinTheta < 0) {
            double rFromBottom = (-hull.getMaxHeight() - knot.getY()) / sinTheta;
            rMax = Math.min(rMax, rFromBottom);
        } else if (sinTheta > 0) {
            double rFromTop = -knot.getY() / sinTheta;
            rMax = Math.min(rMax, rFromTop);
        }

        return Math.max(0, rMax);
    }

    /**
     * Calculates the minimum and maximum angles (theta) such that the control point at (rKnown, theta) relative to a knot
     * remains within the rectangular bounds defined by x = 0, x = L, y = 0, y = -h.
     *
     * @param knot the knot point which acts as the origin
     * @param rKnown the known radius of the control point relative to the knot
     * @param isLeft whether the control point belongs to the left knot or right knot
     * @param boundWithAdjacentSections prevents stack overflow since calculateThetaBounds and calculateAdjacentSectionThetaBounds
     *                                  call each other. Always set to true except in calculateAdjacentSectionThetaBounds
     * @return [minTheta, maxTheta]
     */
    public double[] calculateThetaBounds(Point2D knot, double rKnown, boolean isLeft, boolean boundWithAdjacentSections) {
        double minTheta = isLeft ? 180 : 0;
        double maxTheta = isLeft ? 360 : 180;
        double l = hull.getLength();
        double h = -hull.getMaxHeight();

        // Binary search for minTheta
        double start = minTheta;
        double end = minTheta + 180;
        while (end - start > 1e-3) {
            double thetaGuess = (start + end) / 2;
            if (isPointInBounds(rKnown, thetaGuess, knot, l, h)) end = thetaGuess;
            else start = thetaGuess;
        }
        minTheta = start;

        // Binary search for maxTheta
        start = maxTheta - 180;
        end = maxTheta;
        while (end - start > 1e-3) {
            double thetaGuess = (start + end) / 2;
            if (isPointInBounds(rKnown, thetaGuess, knot, l, h)) start = thetaGuess;
            else end = thetaGuess;
        }
        maxTheta = start;

        // Incorporate additional bounds from adjacent sections
        if (boundWithAdjacentSections) {
            double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, isLeft);
            minTheta = Math.max(minTheta, additionalThetaBounds[0]);
            maxTheta = Math.max(Math.min(maxTheta, additionalThetaBounds[1]), minTheta);
        }

        return new double[] {minTheta, maxTheta};
    }

    /**
     * Checks if a control point, specified in polar coordinates, lies within the bounds of a given rectangle.
     * @param rKnown The radius of the control point in polar coordinates.
     * @param thetaGuess The angle (in radians) of the control point in polar coordinates.
     * @param knot The knot point (reference point) to which the polar coordinates are relative.
     * @param l the length of the rectangle stretching rightward from 0
     * @param h the height, should be inputted as negative to signify stretching downward from 0
     * @return True if the control point lies within the bounds of the rectangle;
     */
    private boolean isPointInBounds(double rKnown, double thetaGuess, Point2D knot, double l, double h) {
        // Get control point position
        Point2D cartesianControl = CalculusUtils.toCartesian(new Point2D(rKnown, thetaGuess), knot);
        double xControl = cartesianControl.getX();
        double yControl = cartesianControl.getY();

        // Fix floating point error
        if (Math.abs(yControl - 0) < 1e-6) yControl = 0;
        if (Math.abs(xControl - 0) < 1e-6) xControl = 0;

        // Check rectangle bounds
        return xControl >= 0 && xControl <= l && yControl <= 0 && yControl >= h;
    }

    /**
     * Calculates additional theta bounds based on adjacent sections' control points to ensure smoothness and continuity.
     *
     * @param knot the knot point which acts as the origin
     * @param isLeft whether the control point belongs to the left knot or right knot
     * @return [additionalMinTheta, additionalMaxTheta]
     */
    private double[] calculateAdjacentSectionThetaBounds(Point2D knot, boolean isLeft) {
        double additionalMinTheta = isLeft ? 180 : 0;
        double additionalMaxTheta = isLeft ? 360 : 180;

        // Handle left adjacent section
        if (isLeft && selectedHullSectionIndex > 0) {
            HullSection leftAdjacentHullSection = hull.getHullSections().get(selectedHullSectionIndex - 1);
            Point2D siblingPoint = ((CubicBezierFunction) leftAdjacentHullSection.getSideProfileCurve()).getControlPoints().getLast();
            double siblingPointR = CalculusUtils.toPolar(siblingPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, siblingPointR, false, false);
            additionalMinTheta = Math.max(additionalMinTheta, (thetaBounds[0] + 180) % 360);
            additionalMaxTheta = Math.min(additionalMaxTheta, (thetaBounds[1] + 180) % 360);
        }

        // Handle right adjacent section
        if (!isLeft && selectedHullSectionIndex < hull.getHullSections().size() - 1) {
            HullSection rightAdjacentHullSection = hull.getHullSections().get(selectedHullSectionIndex + 1);
            Point2D siblingPoint = ((CubicBezierFunction) rightAdjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
            double siblingPointR = CalculusUtils.toPolar(siblingPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, siblingPointR, true, false);
            additionalMinTheta = Math.max(additionalMinTheta, (thetaBounds[0] - 180) % 360);
            additionalMaxTheta = Math.min(additionalMaxTheta, (thetaBounds[1] - 180) % 360);
        }

        return new double[] {additionalMinTheta, additionalMaxTheta};
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
     * Used before setting the knob values to prevent setting the knob value sout of bounds
     */
    public void unboundKnobs() {
        double minR = 0.001;
        double maxPossibleR = hull.getLength();
        double minPossibleTheta = 0;
        double maxPossibleTheta = 360;

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
     *
     * IMPORTANT DISTINCTION:
     *
     * In order to maintain C1 smoothness, the angle must match for both sides of all bezier handles.
     * Since handles lie of the edges of sections, adjusting a handle requires moving the control point in the adjacent section
     * Thus we differentiate between the data of the selected and adjacent hull sections
     *
     * @param knobIndex the index of the knob that was changed (knobs indexed L to R in increasing order from 0)
     * @param newThetaVal the new value for the knob (after user interaction)
     */
    @Traceable
    private void updateHullFromKnob(int knobIndex, double newThetaVal) {
        if (selectedHullSection == null) return;

        // Update the relevant knob value
        knobs.get(knobIndex).setKnobValue(newThetaVal);

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
            Point2D deltas = computeAdjacentControlDeltas(adjacentHullSection, adjustingLeftAdjacentSection, newThetaVal, selectedKnot);
            double deltaX = deltas.getX();
            double deltaY = deltas.getY();

            // Propagate the change to the adjacent control point
            HullSection adjustedAdjacentSection = adjustAdjacentSectionControlPoint(
                    adjacentHullSection, knobIndex == 1, deltaX, deltaY);
            hull.getHullSections().set(adjacentHullSectionIndex, adjustedAdjacentSection);
        }

        // Redraw the hull graphic
        hullGraphicPane.getChildren().clear();
        setSideViewHullGraphic(hull);
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
    private Point2D computeAdjacentControlDeltas(HullSection adjacentHullSection, boolean isLeftAdjacentSection, double newThetaVal, Point2D selectedKnot) {
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
            knobListeners.add((observable, oldValue, newValue) -> updateHullFromKnob(finalI, newValue.doubleValue()));
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
     * TODO: button on hull view panel: eye -> transparent eye -> closed eye for viewing graphics on hull
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
    }
}
