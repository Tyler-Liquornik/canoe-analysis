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

public class HullBuilderController implements Initializable {

    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @FXML
    private Label intervalLabel, heightLabel, volumeLabel, massLabel, propertiesPanelTitleLabel;

    private boolean sectionPropertiesSelected = true;
    private boolean previousPressedBefore;
    private boolean nextPressedBefore;
    private List<Knob> knobs;
    private List<ChangeListener<Number>> knobListeners;
    private HullSection selectedHullSection;
    private int selectedHullSectionIndex = -1;

    @Setter
    private static MainController mainController;
    @Getter
    private Hull hull;
    @Getter @Setter
    private CubicBezierSplineHullGraphic hullGraphic;
    private AnchorPane hullGraphicPane;


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
        System.out.println("selectedHullSectionIndex = " + selectedHullSectionIndex);
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
     * @param theta the angle in degrees (relative to the origin) at which the point lies.
     * @return The maximum radius (r) such that the point stays within the bounds.
     */
    public double calculateMaxR(Point2D knot, double theta) {
        double rMax = Double.MAX_VALUE;
        double thetaRad = Math.toRadians((theta + 90) % 360);
        double cosTheta = Math.cos(thetaRad);
        double sinTheta = Math.sin(thetaRad);

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
     * Calculates the minimum and maximum angles (theta) such that the control point at (r, theta) relative to a knot
     * remains within the rectangular bounds defined by x = 0, x = L, y = 0, y = -h.
     *
     * @param knot the knot point which acts as the origin
     * @param r the radius of the control point relative to the knot
     * @param isLeft whether the control point belongs to the left knot or right knot
     * @param boundWithAdjacentSections prevents stack overflow since
     *                                  calculateThetaBounds and calculateAdjacentSectionThetaBounds call each other
     * @return [minTheta, maxTheta]
     */
    @Traceable
    public double[] calculateThetaBounds(Point2D knot, double r, boolean isLeft, boolean boundWithAdjacentSections) {
        double minTheta = isLeft ? 180 : 0;
        double maxTheta = isLeft ? 360 : 180;
        double xKnot = knot.getX();
        double yKnot = knot.getY();
        double l = hull.getLength();
        double h = -hull.getMaxHeight();

        // Binary search for minTheta
        double start = minTheta;
        double end = minTheta + 180;
        while (end - start > 1e-3) {
            double mid = (start + end) / 2;
            if (isPointInBounds(mid, xKnot, yKnot, l, h, r)) end = mid;
            else start = mid;
        }
        minTheta = start;

        // Binary search for maxTheta
        start = maxTheta - 180;
        end = maxTheta;
        while (end - start > 1e-3) {
            double mid = (start + end) / 2;
            if (isPointInBounds(mid, xKnot, yKnot, l, h, r)) start = mid;
            else end = mid;
        }
        maxTheta = start;

        // Incorporate additional bounds from adjacent sections
        if (boundWithAdjacentSections) {
            double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, isLeft);
            minTheta = Math.max(minTheta, additionalThetaBounds[0]);
            maxTheta = Math.min(maxTheta, additionalThetaBounds[1]);
        }

        return new double[] {minTheta, maxTheta};
    }

    /**
     * Helper for calculateThetaBounds to check if the control point is within bounds of the rectangle
     */
    private boolean isPointInBounds(double thetaGuess, double xKnot, double yKnot, double l, double h, double rKnown) {
        // Get control point position
        double xControl = xKnot + rKnown * Math.cos(Math.toRadians((thetaGuess + 90) % 360));
        double yControl = yKnot + rKnown * Math.sin(Math.toRadians((thetaGuess + 90) % 360));

        // Fix floating point error
        if (Math.abs(yControl - 0) < 1e-6) yControl = 0;
        if (Math.abs(xControl - 0) < 1e-6) xControl = 0;

        // Check bounds
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
            Point2D coPoint = ((CubicBezierFunction) leftAdjacentHullSection.getSideProfileCurve()).getControlPoints().getLast();
            double coPointR = CalculusUtils.toPolar(coPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, coPointR, false, false);
            additionalMinTheta = Math.max(additionalMinTheta, (thetaBounds[0] + 180) % 360);
            additionalMaxTheta = Math.min(additionalMaxTheta, (thetaBounds[1] + 180) % 360);
        }

        // Handle right adjacent section
        if (!isLeft && selectedHullSectionIndex < hull.getHullSections().size() - 1) {
            HullSection rightAdjacentHullSection = hull.getHullSections().get(selectedHullSectionIndex + 1);
            Point2D coPoint = ((CubicBezierFunction) rightAdjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
            double coPointR = CalculusUtils.toPolar(coPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, coPointR, true, false);
            additionalMinTheta = Math.max(additionalMinTheta, (thetaBounds[0] - 180) % 360);
            additionalMaxTheta = Math.min(additionalMaxTheta, (thetaBounds[1] - 180) % 360);
        }

        return new double[]{additionalMinTheta, additionalMaxTheta};
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
     * @param knobIndex the index of the knob that was changed (knobs indexed L to R in increasing order from 0)
     * @param newVal the new value for the knob (after user interaction)
     */
    private void updateHullFromKnob(int knobIndex, double oldVal, double newVal) {
        if (selectedHullSection == null) return;

        // Update the relevant knob value
        knobs.get(knobIndex).setKnobValue(newVal);

        // Get current knob values
        double rL = knobs.get(0).getValue();
        double thetaL = knobs.get(1).getValue();
        double rR = knobs.get(2).getValue();
        double thetaR = knobs.get(3).getValue();

        // Update the relevant control point from polar coordinates
        List<Point2D> knotPoints = ((CubicBezierFunction) selectedHullSection.getSideProfileCurve()).getKnotPoints();
        Point2D lKnot = knotPoints.getFirst();
        Point2D rKnot = knotPoints.getLast();
        CubicBezierFunction bezier = (CubicBezierFunction) selectedHullSection.getSideProfileCurve();
        if (knobIndex == 0 || knobIndex == 1) { // Left control point
            Point2D lControl = CalculusUtils.toCartesian(new Point2D(rL, thetaL), lKnot);
            bezier.setControlX1(lControl.getX());
            bezier.setControlY1(lControl.getY());
        } else if (knobIndex == 2 || knobIndex == 3) { // Right control point
            Point2D rControl = CalculusUtils.toCartesian(new Point2D(rR, thetaR), rKnot);
            bezier.setControlX2(rControl.getX());
            bezier.setControlY2(rControl.getY());
        }

        // When changing theta we need to adjust adjacent sections to maintain C1 smoothness
        boolean adjustingLeftAdjacentSection = knobIndex == 1 && selectedHullSectionIndex != 0;
        boolean adjustingRightAdjustSection = knobIndex == 3 && selectedHullSectionIndex != (hull.getHullSections().size() - 1);
        if (adjustingLeftAdjacentSection || adjustingRightAdjustSection) {
            // Compute delta for the updated control point
            Point2D oldControl, newControl;
            if (adjustingLeftAdjacentSection) {
                oldControl = CalculusUtils.toCartesian(new Point2D(rL, oldVal), lKnot);
                newControl = CalculusUtils.toCartesian(new Point2D(rL, newVal), lKnot);
            } else {
                oldControl = CalculusUtils.toCartesian(new Point2D(rR, oldVal), rKnot);
                newControl = CalculusUtils.toCartesian(new Point2D(rR, newVal), rKnot);
            }
            double deltaX = newControl.getX() - oldControl.getX();
            double deltaY = newControl.getY() - oldControl.getY();

            // Propagate the change to the adjacent control point,
            // Inverting deltas since adjacent section control point is mirrored across the knot
            HullSection adjustedSection = adjustAdjacentSectionControlPoints(knobIndex == 1, -deltaX, -deltaY);
            hull.getHullSections().set(
                    selectedHullSectionIndex + (adjustingLeftAdjacentSection ? -1 : 1),
                    adjustedSection
            );
        }

        // Redraw the hull graphic
        hullGraphicPane.getChildren().clear();
        setSideViewHullGraphic(hull);
    }

    /**
     * Adjusts the control points of the adjacent hull sections to maintain C1 continuity.
     * @param adjustLeftOfSelected whether to adjust the adjacent left section (otherwise right)
     * @param deltaX the amount by which to adjust the control point x in the adjacent section
     * @param deltaY the amount by which to adjust the control point y in the adjacent section
     */
    private HullSection adjustAdjacentSectionControlPoints(boolean adjustLeftOfSelected, double deltaX, double deltaY) {
        // Get the adjacent section needing adjustment
        HullSection adjacentSection = adjustLeftOfSelected ? hull.getHullSections().get(selectedHullSectionIndex - 1)
                : hull.getHullSections().get(selectedHullSectionIndex + 1);

        // Update the control point in the adjacent section
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
            knobListeners.add((observable, oldValue, newValue) -> updateHullFromKnob(finalI, oldValue.doubleValue(), newValue.doubleValue()));
            knob.valueProperty().addListener(knobListeners.get(i));
        }

        curveParameterizationAnchorPane.getChildren().addAll(knobs);
    }

    /**
     * TODO: button on hull view panel: eye -> transparent eye -> closed eye for viewing graphics on hull
     * Add and position blue buttons to corners of panels
     */
    private void layoutPanelButtons() {
        IconButton nextLeftSectionButton = IconButton.getPanelButton(IconGlyphType.LEFT, this::selectPreviousHullSection, 12);
        IconButton nextRightSectionButton = IconButton.getPanelButton(IconGlyphType.RIGHT, this::selectNextHullSection, 12);
        IconButton curveParameterizationPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);
        IconButton canoePropertiesSwitchButton = IconButton.getPanelButton(IconGlyphType.SWITCH, this::switchButton, 14);
        IconButton canoePropertiesPlusButton = IconButton.getPanelButton(IconGlyphType.PLUS, this::dummyOnClick, 14);

        nextRightSectionButton.getIcon().setTranslateX(1.5);
        nextRightSectionButton.getIcon().setTranslateY(0.5);
        nextLeftSectionButton.getIcon().setTranslateX(-0.5);
        nextLeftSectionButton.getIcon().setTranslateY(0.5);
        curveParameterizationPlusButton.getIcon().setTranslateY(1);
        canoePropertiesPlusButton.getIcon().setTranslateY(1);

        hullViewAnchorPane.getChildren().addAll(nextLeftSectionButton, nextRightSectionButton);
        curveParameterizationAnchorPane.getChildren().add(curveParameterizationPlusButton);
        propertiesAnchorPane.getChildren().addAll(canoePropertiesSwitchButton, canoePropertiesPlusButton);

        double marginToPanel = 15;
        double marginBetween = 10;
        double buttonWidth = nextLeftSectionButton.prefWidth(-1);

        AnchorPane.setTopAnchor(nextLeftSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextLeftSectionButton, marginToPanel + marginBetween + buttonWidth);
        AnchorPane.setTopAnchor(nextRightSectionButton, marginToPanel);
        AnchorPane.setRightAnchor(nextRightSectionButton, marginToPanel);
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
    }
}
