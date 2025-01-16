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
    public void initialize() {
        // Define columns
        parameterColumn.setCellValueFactory(new PropertyValueFactory<>("parameter"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Add data
        ObservableList<EquationModel> data = FXCollections.observableArrayList(
                new EquationModel("P_crit", "Perimeter effected by knee (mm)"),
                new EquationModel("A_crit", "Area effected by knee (mm^2)"),
                new EquationModel("V_p", "Punching shear force (N)"),
                new EquationModel("v_f", "Punching shear stress (MPa)"),
                new EquationModel("t", "Effective critical depth (mm)"),
                new EquationModel("b_w", "Hull width (mm)"),
                new EquationModel("f_c", "Compressive strength (MPa)"),
                new EquationModel("alpha", "Internal column"),
                new EquationModel("lambda", "Low density concrete"),
                new EquationModel("beta_c", "Square column"),
                new EquationModel("phi", "Concrete"),
                new EquationModel("d", "Diameter of knee")
        );

        dataTable.setItems(data);
    }
}


