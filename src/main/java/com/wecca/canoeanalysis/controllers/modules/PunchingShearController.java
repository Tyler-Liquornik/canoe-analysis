package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import lombok.Setter;

import java.net.URL;
import java.util.ResourceBundle;

public class PunchingShearController implements Initializable {

    @FXML
    private JFXTextField CanoeThickness, HullWidth, CompressiveStrength, maxShear, MaxShear, Shear, pCrit, aCrit, vf, vc1, vc2, vc3, vcMin;
    @FXML
    private Label oneWaySafe, oneWayUnsafe, twoWaySafe, twoWayUnsafe;
    @Setter
    private static MainController mainController;

    // Constants

    private final double internalColumn = 4;
    private final double lowDensityConcrete = 0.75;
    private final double squareColumn = 1;
    private final double concrete = 0.65;
    private final double kneeDiameter = 30;
    private final double PunchingShearForce = 625.3875;

    // Variables

    private double canoeThickness;
    private double hullWidth;
    private double compressiveStrength;

    @FXML
    public void safetyTest1(){
        try {

            if (MaxShear.getText().isEmpty() || Shear.getText().isEmpty()){
                throw new IllegalArgumentException("Shear values cannot be empty");
            }

            double maxShear = Double.parseDouble(MaxShear.getText());
            double shear = Double.parseDouble(Shear.getText());

            if (shear > maxShear){
                oneWaySafe.setOpacity(1);
                oneWayUnsafe.setOpacity(0.5);
            }
            else{
                oneWaySafe.setOpacity(0.5);
                oneWayUnsafe.setOpacity(1);
            }
        } catch (NumberFormatException e) {
            mainController.showSnackbar("Invalid input: Please enter numeric values.");
        } catch (IllegalArgumentException e) {
            mainController.showSnackbar(e.getMessage());
        } catch (Exception e) {
            mainController.showSnackbar("An unexpected error occurred. Please try again.");
        }

    }

    @FXML
    public void safetyTest2(){
        try {

            if (vf.getText().isEmpty() || vcMin.getText().isEmpty()) {
                throw new IllegalArgumentException("Values cannot be empty.");
            }

            double Vf = Double.parseDouble(vf.getText());
            double VcMin = Double.parseDouble(vcMin.getText());

            if (VcMin > Vf){
                twoWaySafe.setOpacity(1);
                twoWayUnsafe.setOpacity(0.5);
            }
            else{
                twoWaySafe.setOpacity(0.5);
                twoWayUnsafe.setOpacity(1);
            }
        }
        catch (NumberFormatException e) {
            mainController.showSnackbar("Invalid input: Please enter numeric values.");
        } catch (IllegalArgumentException e) {
            mainController.showSnackbar(e.getMessage());
        } catch (Exception e) {
            mainController.showSnackbar("An unexpected error occurred. Please try again.");
        }
    }

    @FXML
    public void UploadShear(){
        if (!maxShear.getText().isEmpty()){
            MaxShear.setText(maxShear.getText());
        }
        else {
            MaxShear.setText("");
            mainController.showSnackbar("Please input a value for max shear");
        }
    }

    @FXML
    public void CalculateOneWay(){
        if (!CanoeThickness.getText().isEmpty() && !HullWidth.getText().isEmpty() && !CompressiveStrength.getText().isEmpty()){
            canoeThickness = Double.parseDouble(CanoeThickness.getText());
            hullWidth = Double.parseDouble(HullWidth.getText());
            compressiveStrength = Double.parseDouble(CompressiveStrength.getText());

            double Vc = concrete * lowDensityConcrete * squareColumn * Math.sqrt(compressiveStrength) * hullWidth * canoeThickness;
            Shear.setText(String.format("%.2f", Vc));
        }
        else {
            Shear.setText("");
            mainController.showSnackbar("Please fill all the Hull Builder fields");
        }
    }

    @FXML
    public void ClearOneWay(){
        MaxShear.clear();
        Shear.clear();
        oneWaySafe.setOpacity(0.5);
        oneWayUnsafe.setOpacity(0.5);
    }

    @FXML
    public void CalculateTwoWay(){
        if (!CanoeThickness.getText().isEmpty() && !CompressiveStrength.getText().isEmpty()){
            canoeThickness = Double.parseDouble(CanoeThickness.getText());
            compressiveStrength = Double.parseDouble(CompressiveStrength.getText());

            double Pcrit = 4*(kneeDiameter + (2*(canoeThickness/2)));
            pCrit.setText(String.format("%.2f", Pcrit));

            double Acrit = Pcrit * canoeThickness;
            aCrit.setText(String.format("%.2f", Acrit));

            double Vf = PunchingShearForce / Acrit;
            vf.setText(String.format("%.2f", Vf));

            double Vc1 = ((1 + (2/squareColumn)) * 0.19 * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength));
            vc1.setText(String.format("%.4f", Vc1));

            double Vc2 = (0.19 + ((internalColumn*canoeThickness)/Pcrit)) * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength);
            vc2.setText(String.format("%.4f", Vc2));

            double Vc3 = 0.38 * lowDensityConcrete * concrete * Math.sqrt(compressiveStrength);
            vc3.setText(String.format("%.4f", Vc3));

            vcMin.setText(String.format("%.4f", Math.min(Vc1, Math.min(Vc2, Vc3))));
        }
        else {
            pCrit.setText("");
            aCrit.setText("");
            vf.setText("");
            vc1.setText("");
            vc2.setText("");
            vc3.setText("");
            vcMin.setText("");
            mainController.showSnackbar("Please fill all the Hull Builder fields");
        }
    }

    @FXML
    public void ClearTwoWay(){
        pCrit.setText("");
        aCrit.setText("");
        vf.setText("");
        vc1.setText("");
        vc2.setText("");
        vc3.setText("");
        vcMin.setText("");
        twoWaySafe.setOpacity(0.5);
        twoWayUnsafe.setOpacity(0.5);
    }

    @FXML
    public void Reset(){
        CanoeThickness.clear();
        HullWidth.clear();
        CompressiveStrength.clear();
        maxShear.clear();
        MaxShear.clear();
        Shear.clear();
        pCrit.clear();
        aCrit.clear();
        vf.clear();
        vc1.clear();
        vc2.clear();
        vc3.clear();
        vcMin.clear();
        oneWaySafe.setOpacity(0.5);
        oneWayUnsafe.setOpacity(0.5);
        twoWaySafe.setOpacity(0.5);
        twoWayUnsafe.setOpacity(0.5);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        mainController.resetToolBarButtons();
    }
}
