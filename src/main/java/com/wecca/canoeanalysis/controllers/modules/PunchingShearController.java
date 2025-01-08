package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.services.DiagramService;
import com.wecca.canoeanalysis.services.ResourceManagerService;
import com.wecca.canoeanalysis.services.WindowManagerService;
import com.wecca.canoeanalysis.services.YamlMarshallingService;
import com.wecca.canoeanalysis.utils.InputParsingUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import lombok.Setter;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller class for Punching Shear calculations in the Canoe Analysis Application.
 * This class handles the logic for calculating one-way and two-way shear safety and provides methods
 * for input validation and result display in the UI.
 */
public class PunchingShearController implements Initializable {

    @FXML
    private JFXTextField hullThicknessTextField, hullWidthTextField, compressiveStrengthTextField,
            maxShearTextField, oneWayVfTextField, oneWayVcTextField, twoWayPCritTextField,
            twoWayACritTextField, twoWayVfTextField, twoWayVc1TextField, twoWayVc2TextField,
            twoWayVc3TextField, vcMinTextField;
    @FXML
    private Label oneWaySafeLabel, oneWayUnsafeLabel, twoWaySafeLabel, twoWayUnsafeLabel;
    @FXML
    private AnchorPane chartContainer;

    @Setter
    private static MainController mainController;

    // Constants for various factors used in shear calculations
    private final double internalColumn = 4;
    private final double lowDensityConcrete = 0.75;
    private final double squareColumn = 1;
    private final double concrete = 0.65;
    private final double kneeDiameter = 30;
    private final double punchingShearForce = 625.3875;

    // Variables to store user inputs
    private double canoeThickness;
    private double hullWidth;
    private double compressiveStrength;

    /**
     * Performs safety check for one-way shear.
     * Compares the calculated shear value to the maximum allowed shear and updates the UI to show
     * whether the structure is safe or unsafe.
     */
    public void safetyTest1() {
        safetyTest(oneWayVfTextField, oneWayVcTextField, oneWaySafeLabel, oneWayUnsafeLabel);
    }

    /**
     * Performs safety check for two-way shear.
     * Compares the calculated minimum shear value to the calculated Vf value and updates the UI
     * to show whether the structure is safe or unsafe.
     */
    public void safetyTest2() {
        safetyTest(twoWayVfTextField, vcMinTextField, twoWaySafeLabel, twoWayUnsafeLabel);
    }

    /**
     * Performs a generic shear safety test by comparing the values in two text fields.
     * If the value in the second text field (checkForSafetyTextField) exceeds the value in the first 
     * text field (safetyBaselineTextField), the safeLabel is highlighted, indicating a safe condition.
     * Otherwise, the unsafeLabel is highlighted.
     *
     * @param safetyBaselineTextField the text field containing the baseline value to compare against.
     * @param checkForSafetyTextField the text field containing the value to check for safety.
     * @param safeLabel the label to indicate a safe condition if the check passes.
     * @param unsafeLabel the label to indicate an unsafe condition if the check fails.
     */
    private void safetyTest(TextField safetyBaselineTextField, TextField checkForSafetyTextField, Label safeLabel, Label unsafeLabel) {
        if (InputParsingUtils.allTextFieldsAreDouble(Arrays.asList(safetyBaselineTextField, checkForSafetyTextField))) {
            double baselineValue = Double.parseDouble(safetyBaselineTextField.getText());
            double checkForSafetyValue = Double.parseDouble(checkForSafetyTextField.getText());

            if (checkForSafetyValue > baselineValue) {
                safeLabel.setOpacity(1);
                unsafeLabel.setOpacity(0.5);
            } else {
                safeLabel.setOpacity(0.5);
                unsafeLabel.setOpacity(1);
            }
        }
        else
            mainController.showSnackbar("Please fill all the fields with valid numeric values.");
    }

    /**
     * Populates the one-way shear force field with the value entered for the max shear text field.
     * Shows an error message if the max shear field is empty.
     */
    public void uploadShear() {
        if (!maxShearTextField.getText().isEmpty()) {
            oneWayVfTextField.setText(maxShearTextField.getText());
        } else {
            oneWayVfTextField.setText("");
            mainController.showSnackbar("Please input a value for max shear");
        }
    }

    /**
     * Calculates the one-way shear capacity (Vc) based on the input hull thickness, width, and compressive strength.
     * Displays the calculated value in the UI.
     */
    public void calculateOneWay() {
        if (InputParsingUtils.allTextFieldsAreDouble(Arrays.asList(hullThicknessTextField, hullWidthTextField, compressiveStrengthTextField))) {
            canoeThickness = Double.parseDouble(hullThicknessTextField.getText());
            hullWidth = Double.parseDouble(hullWidthTextField.getText());
            compressiveStrength = Double.parseDouble(compressiveStrengthTextField.getText());
            double vc = concrete * lowDensityConcrete * squareColumn * Math.sqrt(compressiveStrength) * hullWidth * canoeThickness;
            oneWayVcTextField.setText(String.format("%.2f", vc));
        }
        else
            mainController.showSnackbar("Please fill all the fields with valid numeric values.");
    }

    /**
     * Clears all fields and labels related to the one-way shear calculation.
     */
    public void clearOneWay() {
        oneWayVfTextField.clear();
        oneWayVcTextField.clear();
        oneWaySafeLabel.setOpacity(0.5);
        oneWayUnsafeLabel.setOpacity(0.5);
    }

    /**
     * Calculates the two-way shear force and related values based on the input hull thickness and compressive strength.
     * Displays the calculated values in the UI.
     */
    public void calculateTwoWay() {
        if (InputParsingUtils.allTextFieldsAreDouble(Arrays.asList(hullThicknessTextField, compressiveStrengthTextField))) {
            canoeThickness = Double.parseDouble(hullThicknessTextField.getText());
            compressiveStrength = Double.parseDouble(compressiveStrengthTextField.getText());

            double pCrit = 4 * (kneeDiameter + (2 * (canoeThickness / 2)));
            twoWayPCritTextField.setText(String.format("%.2f", pCrit));
            double aCrit = pCrit * canoeThickness;
            twoWayACritTextField.setText(String.format("%.2f", aCrit));
            double vf = punchingShearForce / aCrit;
            twoWayVfTextField.setText(String.format("%.2f", vf));
            double vc1 = ((1 + (2 / squareColumn)) * 0.19 * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength));
            twoWayVc1TextField.setText(String.format("%.4f", vc1));
            double vc2 = (0.19 + ((internalColumn * canoeThickness) / pCrit)) * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength);
            twoWayVc2TextField.setText(String.format("%.4f", vc2));
            double vc3 = 0.38 * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength);
            twoWayVc3TextField.setText(String.format("%.4f", vc3));
            vcMinTextField.setText(String.format("%.4f", Math.min(vc1, Math.min(vc2, vc3))));
        }
        else
            mainController.showSnackbar("Please fill all the fields with valid numeric values.");
    }

    /**
     * Clears all fields and labels related to the two-way shear calculation.
     */
    public void clearTwoWay() {
        twoWayPCritTextField.setText("");
        twoWayACritTextField.setText("");
        twoWayVfTextField.setText("");
        twoWayVc1TextField.setText("");
        twoWayVc2TextField.setText("");
        twoWayVc3TextField.setText("");
        vcMinTextField.setText("");
        twoWaySafeLabel.setOpacity(0.5);
        twoWayUnsafeLabel.setOpacity(0.5);
    }

    /**
     * Resets all fields and labels in both one-way and two-way shear tests to their default state.
     */
    public void reset() {
        hullThicknessTextField.clear();
        hullWidthTextField.clear();
        compressiveStrengthTextField.clear();
        maxShearTextField.clear();
        clearOneWay();
        clearTwoWay();
    }
    /**
     * Clears the toolbar of buttons from other modules and adds ones from this module
     * Currently, this provides buttons to download and upload the Canoe object as JSON
     */
    public void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> iconGlyphToFunctionMap = new LinkedHashMap<>();
        //iconGlyphToFunctionMap.put(IconGlyphType.DOWNLOAD, e -> downloadCanoe());
        iconGlyphToFunctionMap.put(IconGlyphType.UPLOAD, e -> uploadCanoe());

        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(iconGlyphToFunctionMap);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());

        initModuleToolBarButtons();
    }
    /**
     * Upload a YAML file representing the Canoe object model
     * This populates the list view and beam graphic with the new model
     */
    public void uploadCanoe() {
        YamlMarshallingService.setPunchingShearController(this);

        YamlMarshallingService.punchingShearImportCanoeFromYAML(mainController.getPrimaryStage());

    }

    /**
     *
     * @param canoe
     * This method sets the values in the punching shear module necessary to test
     * session max shear is given in kN so must be converted to N
     * thickenss and width are given in m and must be converted to mm
     */
    public void setValues(Canoe canoe){


        maxShearTextField.setText(String.format("%.2f",canoe.getSessionMaxShear()*1000));
        oneWayVfTextField.setText(String.format("%.2f",canoe.getSessionMaxShear()*1000));
        hullThicknessTextField.setText(String.format("%.2f",canoe.getMaxThickness()*1000));
        hullWidthTextField.setText(String.format("%.2f",canoe.getMaxWidth()*1000));


        displayChart(canoe);

    }

    /**
     *
     * @param canoe
     * this method calls on the diagram manager service and uses it to set up the chart and display it in module
     * this is displaying the chart of the canoe uploaded with the max absolute shear
     */
    public void displayChart(Canoe canoe ) {
        List<Point2D> points = DiagramService.generateSfdPoints(canoe);
        AreaChart<Number, Number> chart = DiagramService.setupChart(canoe, points, "kN", "Force");

        // Set size of chart to match anchor pane
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);

        chart.getStylesheets().add(ResourceManagerService.getResourceFilePathString("css/chart.css", false));

        // Add the chart to the container
        chartContainer.getChildren().add(chart);
    }
}
