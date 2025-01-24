package com.wecca.canoeanalysis.controllers.popups;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ShearEquationsController {

    @FXML
    private TableView<EquationModel> dataTable;

    @FXML
    private TableColumn<EquationModel, String> parameterColumn;

    @FXML
    private TableColumn<EquationModel, String> descriptionColumn;

    @FXML
    private TableColumn<EquationModel, String> valueColumn;

    @FXML
    public void initialize() {
        // Define columns
        parameterColumn.setCellValueFactory(new PropertyValueFactory<>("parameter"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Add data
        ObservableList<EquationModel> data = FXCollections.observableArrayList(
                new EquationModel("P_critical", "Perimeter effected by knee (mm)", "4[b + 2(d / 2)]"),
                new EquationModel("A_critical", "Area effected by knee (mm^2)", "P_critical * t"),
                new EquationModel("Vp", "Punching shear force (N)", "Talk to Jared"),
                new EquationModel("vf", "Punching shear stress (MPa)", "Vf / A_critical"),
                new EquationModel("t", "Effective critical depth (mm)", "Talk to Jared"),
                new EquationModel("b", "Hull width (mm)", "Talk to Jared"),
                new EquationModel("fc", "Compressive strength (MPa)", "Talk to Jared"),
                new EquationModel("alpha", "Internal column", "4"),
                new EquationModel("lambda", "Low density concrete", "0.75"),
                new EquationModel("beta_c", "Square column", "1"),
                new EquationModel("phi_c", "Concrete", "0.65"),
                new EquationModel("d", "Diameter of knee", "13"),
                new EquationModel("vc1","First formula for calculating Vc", "(1 + 2 / beta_c)(0.19)(lambda)(phi_c)(√fc)"),
                new EquationModel("vc2", "Second formula for calculating Vc", "(0.19 + (alpha * d / P_critical) * lambda * phi_c * √fc"),
                new EquationModel("vc3", "Third equation for calculating Vc", "0.38 * lambda * phi_c * √fc"),
                new EquationModel("Vc", "Ask Jared", "min(vc1, vc2, vc3)"));

        dataTable.setItems(data);
    }
}


