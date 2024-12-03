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
import com.wecca.canoeanalysis.services.LoggerService;
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
    private ListIterator<HullSection> hullSectionsListIterator;
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
     * Highlight the previous section to the left to view and edit (wraps w modulo)
     */
    public void selectPreviousHullSection(MouseEvent e) {
        unlockKnobsOnFirstSectionSelect();

        // Entering this method indicates that the "previous" button was clicked.
        // This try block checks if the "next" button was clicked before the "previous" button.
        // If so, and the iterator is, for example, at index 3, we’ll need to call the iterator’s `previous` method twice
        // to correctly move it back to index 2.
        // This would be the first call of the previous method
        try {
            if(!previousPressedBefore) {
                selectedHullSection = hullSectionsListIterator.previous();
                selectedHullSectionIndex = 0;
            }
        }
        catch(Exception ignored) {}

        hullGraphic.colorPreviousBezierPointGroup();

        // Check if we're at the start or if previous index is -1 (no valid previous element)
        if (!hullSectionsListIterator.hasPrevious()) {
            // Reset to the end of the list by iterating through all elements
            while (hullSectionsListIterator.hasNext()) {
                selectedHullSection = hullSectionsListIterator.next();
            }
            selectedHullSectionIndex = hull.getHullSections().size() - 1;
        }
        // Since the "next" method was called to reach the end, we need to call "previous" once to properly
        // set up for any additional "previous" calls that follow. This prepares the iterator position correctly.
        selectedHullSection = hullSectionsListIterator.previous();
        selectedHullSectionIndex--;

        if (sectionPropertiesSelected) //sets all the section properties ( if that is what the user clicked on, other option is Canoe Properties)
            setSectionProperties(selectedHullSection.getHeight(),selectedHullSection.getVolume(), selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        previousPressedBefore = true;

        setKnobValuesToHullSection(selectedHullSection);
    }

    /**
     * Highlight the next section to the right to view and edit (wraps w modulo)
     */
    public void selectNextHullSection(MouseEvent e) {
        unlockKnobsOnFirstSectionSelect();

        // This if block checks if the "previous" button was clicked before the "next" button.
        // If so, and the iterator is, for example, at index 2, we’ll need to call the iterator’s `next` method twice
        // to correctly move it forward to index 3.
        // This would be the first call of the next method
        if(previousPressedBefore) {
            selectedHullSection = hullSectionsListIterator.next();
            selectedHullSectionIndex = hull.getHullSections().size() - 1;
        }
        hullGraphic.colorNextBezierPointGroup();

        // Check if we're at the end or if next index is null
        if (!hullSectionsListIterator.hasNext()) {
            // Reset to the start of the list by iterating through all elements
            while (hullSectionsListIterator.hasPrevious()) {
                selectedHullSection = hullSectionsListIterator.previous();
            }
            selectedHullSectionIndex = 0;
        }
        // Since the "previous" method was called to reach the end, we need to call "next" once to properly
        // set up for any additional "next" calls that follow. This prepares the iterator position correctly.
        selectedHullSection = hullSectionsListIterator.next();
        selectedHullSectionIndex++;

        if (sectionPropertiesSelected)
            setSectionProperties(selectedHullSection.getHeight(), selectedHullSection.getVolume(),selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx());
        previousPressedBefore = false;
        nextPressedBefore = true;

        setKnobValuesToHullSection(selectedHullSection);
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
     * Calculates and sets the knob values in polar coordinates
     * @param hullSection the (required bezier) hull section currently selected by the user to set knob values from
     */
    private void setKnobValuesToHullSection(HullSection hullSection) {

        if (!(hullSection.getSideProfileCurve() instanceof CubicBezierFunction bezier))
            throw new IllegalArgumentException("Cannot work in Hull Builder with non-bezier hull curve");

        List<Point2D> knotAndControlPoints = bezier.getKnotAndControlPoints();
        Point2D pL = CalculusUtils.toPolar(knotAndControlPoints.get(1), knotAndControlPoints.get(0));
        Point2D pR = CalculusUtils.toPolar(knotAndControlPoints.get(2), knotAndControlPoints.get(3));
        List<Double> knobValues = List.of(pL.getX(), pL.getY(), pR.getX(), pR.getY());

        // Remove listeners, batch update values, add back listeners
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().removeListener(knobListeners.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).setKnobValue(knobValues.get(i));}
        for (int i = 0; i < knobs.size(); i++) {knobs.get(i).valueProperty().addListener(knobListeners.get(i));}
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
            } else { // adjustingRightAdjacentSection
                oldControl = CalculusUtils.toCartesian(new Point2D(rR, oldVal), rKnot);
                newControl = CalculusUtils.toCartesian(new Point2D(rR, newVal), rKnot);
            }
            // Inverted (*-1) because the adjacent section control point is mirrored across
            double deltaX = oldControl.getX() - newControl.getX();
            double deltaY = oldControl.getY() - newControl.getY();

            // Propagate the change to the adjacent control point
            HullSection adjustedSection = adjustAdjacentSectionControlPoints(knobIndex == 1, deltaX, deltaY);
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

        // Adjust the control point by subtracting delta.
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
        List<Double> minValues = List.of(0.0, 0.0, 0.0, 0.0);
        List<Double> maxValues = List.of(1.5, 360.0, 1.5, 360.0);
        List<Double> layoutXs = List.of(30.0, 140.0, 290.0, 400.0);

        knobs = new ArrayList<>();
        knobListeners = new ArrayList<>();

        for (int i = 0; i < knobLabels.size(); i++) {
            Knob knob = new Knob(knobLabels.get(i), minValues.get(i), 0, maxValues.get(i), layoutXs.get(i), layoutY, knobSize);
            knob.setLocked(true);
            knobs.add(knob);
            int finalI = i;
            knobListeners.add((observable, oldValue, newValue) -> updateHullFromKnob(finalI, oldValue.doubleValue(), newValue.doubleValue()));
            knob.valueProperty().addListener(knobListeners.get(i));
        }

        curveParameterizationAnchorPane.getChildren().addAll(knobs);
    }

    /**
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
        hullSectionsListIterator = hull.getHullSections().listIterator();
        setBlankSectionProperties();
    }
}
