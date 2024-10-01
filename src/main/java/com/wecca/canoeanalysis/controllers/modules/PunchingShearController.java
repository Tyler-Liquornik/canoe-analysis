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
    private JFXTextField CanoeThickness, HullWidth,CompressiveStrength, MaxShear, Shear;
    @FXML
    private Label oneWaySafe, oneWayUnsafe;
    @Setter
    private static MainController mainController;

    @FXML
    public void safetyTest(){
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
    }

    @FXML
    public void CalculateVc(){
        if (!CanoeThickness.getText().isEmpty() && !HullWidth.getText().isEmpty() && !CompressiveStrength.getText().isEmpty() ){
            double canoeThickness = Double.parseDouble(CanoeThickness.getText());
            double hullWidth = Double.parseDouble(HullWidth.getText());
            double compressiveStrength = Double.parseDouble(CompressiveStrength.getText());
            double VcFormula = 0.65 * 0.75 * 1 * Math.sqrt(compressiveStrength) * hullWidth * canoeThickness;
            Shear.setText(String.format("%.2f", VcFormula));
        }
        else {
            Shear.setText("");
        }
    }

    public void Clear(){
        MaxShear.clear();
        Shear.clear();
        oneWaySafe.setOpacity(0.5);
        oneWayUnsafe.setOpacity(0.5);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set the local instance of the main controller
        setMainController(CanoeAnalysisApplication.getMainController());

        mainController.resetToolBarButtons();
    }
}
