package com.wecca.canoeanalysis.controllers.modules;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.controls.IconButton;
import com.wecca.canoeanalysis.components.controls.Knob;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.components.graphics.hull.CubicBezierSplineHullGraphic;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.utils.SharkBaitHullLibrary;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class HullBuilderController implements Initializable
{
    private boolean sectionPropertiesSelected = true;
    @FXML
    private Label inter;

    @FXML
    private Label height;

    @FXML
    private Label volume;

    @FXML
    private Label mass;

    @FXML
    private Label PropertiesType;

    private boolean previousPressedBefore;
    private ListIterator<HullSection> listOfHullSections;
    private HullSection selectedHullSection;
    @Setter
    private static MainController mainController;

    @FXML
    private AnchorPane hullViewAnchorPane, curveParameterizationAnchorPane, propertiesAnchorPane;

    @Getter
    private Hull hull;
    @Getter
    @Setter
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
        hullViewAnchorPane.getChildren().add(hullGraphicPane);
    }

    /**
     * Set the side view hull to build
     *
     * @param hull to set from
     *             TODO
     */
    public void setTopViewHullGraphic(Hull hull) {
    }

    /**
     * Set the front view hull to build
     *
     * @param hull to set from
     *             TODO
     */
    public void setFrontViewHullGraphic(Hull hull) {
    }

    /**
     * Highlight the previous section to the left to view and edit (wraps w modulo)
     */
    public void selectPreviousHullSection(MouseEvent e)
    {
        // Entering this method indicates that the "previous" button was clicked.
        // This try block checks if the "next" button was clicked before the "previous" button.
        // If so, and the iterator is, for example, at index 3, we’ll need to call the iterator’s `previous` method twice
        // to correctly move it back to index 2.
        // This would be the first call of the previous method
        try
        {
            if(!previousPressedBefore)
            {
                selectedHullSection = listOfHullSections.previous();
            }
        }
        catch(Exception ignored) {}

        hullGraphic.colorPreviousBezierPointGroup();

        // Check if we're at the start or if previous index is -1 (no valid previous element)
        if (!listOfHullSections.hasPrevious())
        {
            // Reset to the end of the list by iterating through all elements
            while (listOfHullSections.hasNext())
            {
                selectedHullSection = listOfHullSections.next();

            }

            // Since the "next" method was called to reach the end, we need to call "previous" once to properly
            // set up for any additional "previous" calls that follow. This prepares the iterator position correctly.
            selectedHullSection = listOfHullSections.previous();
        }
        else
        {
            selectedHullSection = listOfHullSections.previous();


        }

        if(sectionPropertiesSelected) //sets all the section properties ( if that is what the user clicked on, other option is Canoe Properties)
            setSectionProperties(hull.getMaxHeight(),selectedHullSection.getVolume(), selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx(),false);

        previousPressedBefore = true;
    }

    /**
     * Highlight the next section to the right to view and edit (wraps w modulo)
     */
    public void selectNextHullSection(MouseEvent e)
    {
        // This if block checks if the "previous" button was clicked before the "next" button.
        // If so, and the iterator is, for example, at index 2, we’ll need to call the iterator’s `next` method twice
        // to correctly move it forward to index 3.
        // This would be the first call of the next method

        if(previousPressedBefore)
        {
            selectedHullSection = listOfHullSections.next();
        }

        hullGraphic.colorNextBezierPointGroup();

        // Check if we're at the end or if next index is null
      if (!listOfHullSections.hasNext())
        {
            // Reset to the start of the list by iterating through all elements
            while (listOfHullSections.hasPrevious())
            {
                selectedHullSection = listOfHullSections.previous();
            }

            // Since the "previous" method was called to reach the end, we need to call "next" once to properly
            // set up for any additional "next" calls that follow. This prepares the iterator position correctly.
            selectedHullSection = listOfHullSections.next();
        }
        else
        {
            selectedHullSection = listOfHullSections.next();
        }


        if(sectionPropertiesSelected)
            setSectionProperties(hull.getMaxHeight(), selectedHullSection.getVolume(),selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx(),false);

        previousPressedBefore = false;

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());
        initModuleToolBarButtons();

        // Add knobs
        Knob leftKnob = new Knob("a", 0, 0, 100, 45, 55);
        Knob middleKnob = new Knob("h", 0, 0, 100, 215, 55);
        Knob rightKnob = new Knob("k", 0, 0, 100, 385, 55);
        curveParameterizationAnchorPane.getChildren().addAll(leftKnob, middleKnob, rightKnob);

        // Add and position panel buttons
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

        // Set default hull
        hullGraphicPane = new AnchorPane();
        hull = SharkBaitHullLibrary.generateSharkBaitHullScaledFromBezier(6);
        setSideViewHullGraphic(hull);


        listOfHullSections = hull.getHullSections().listIterator();


    }

    public void setSectionProperties(double height, double volume, double mass, double x, double rx,boolean error) //sets up all section properties with corresponding attributes
    {
        if(!error)
        {
            String heightInfo = String.format("%.2fm",height);
            this.height.setText(heightInfo);

            String interval = "("+x+","+rx+")";
            this.inter.setText(interval);

            String volumeFormated = String.format("%.2fm",volume);
            this.volume.setText(volumeFormated);

            String massFormated = String.format("%.2fm",mass);
            this.mass.setText(massFormated);

        }
        else // Scenario in which no section is selected
        {
            String heightInfo = String.format("%.2fm",hull.getMaxHeight());
            this.height.setText(heightInfo);

            String interval = "N/A";
            this.inter.setText(interval);

            String volumeFormated = "N/A";
            this.volume.setText(volumeFormated);

            String massFormated = "N/A";
            this.mass.setText(massFormated);
        }
    }

    public void switchButton(MouseEvent e)
    {
        if(sectionPropertiesSelected)
        {
            sectionPropertiesSelected = false;
            PropertiesType.setText("Canoes Properties");
            setSectionProperties(hull.getMaxHeight(), hull.getTotalVolume(),hull.getMass(), 0, hull.getLength(),false);
        }
        else
        {
            sectionPropertiesSelected = true;
            PropertiesType.setText("Section Properties");
            try
            {
                setSectionProperties(hull.getMaxHeight(), selectedHullSection.getVolume(),selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx(),false);
            }
            catch (Exception ex)
            {
                setSectionProperties(hull.getMaxHeight(), selectedHullSection.getVolume(),selectedHullSection.getMass(), selectedHullSection.getX(), selectedHullSection.getRx(),true);
            }
        }


    }

}
