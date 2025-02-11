package com.wecca.canoeanalysis.controllers.popups;

import com.wecca.canoeanalysis.models.data.Equation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ShearEquationsController {

    @FXML
    private TableView<Equation> dataTable;

    @FXML
    private TableColumn<Equation, TextFlow> parameterColumn;

    @FXML
    private TableColumn<Equation, TextFlow> descriptionColumn;

    @FXML
    private TableColumn<Equation, TextFlow> valueColumn;

    @FXML
    public void initialize() {

        parameterColumn.setCellValueFactory(new PropertyValueFactory<>("parameter"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Set cell factories to display TextFlow correctly
        setTextFlowColumnFactory(parameterColumn);
        setTextFlowColumnFactory(descriptionColumn);
        setTextFlowColumnFactory(valueColumn);
        setRowHeight();

        // Creating the ObservableList with correctly formatted subscripts
        ObservableList<Equation> data = FXCollections.observableArrayList(
                new Equation(createSubscriptText("P", "critical"),
                        new TextFlow(new Text("Perimeter affected by knee (mm)")),
                        new TextFlow(new Text("4[b + 2(d / 2)]"))),

                new Equation(createSubscriptText("A", "critical"),
                        new TextFlow(new Text("Area affected by knee (mm²)")),
                        new TextFlow(createSubscriptText("P", "critical"), new Text(" * "), createSubscriptText("t", ""))),

                new Equation(createSubscriptText("V", "p"),
                        new TextFlow(new Text("Punching shear force (N)")),
                        new TextFlow(new Text("625.3875"))),

                new Equation(createSubscriptText("v", "f"),
                        new TextFlow(new Text("Punching shear stress (MPa)")),
                        new TextFlow(createSubscriptText("V", "f"), new Text(" / "), createSubscriptText("A", "critical"))),

                new Equation(createSubscriptText("t", ""),
                        new TextFlow(new Text("Canoe thickness (mm)")),
                        new TextFlow(new Text("Input / from hull builder"))),

                new Equation(createSubscriptText("b", ""),
                        new TextFlow(new Text("Hull width (mm)")),
                        new TextFlow(new Text("Input / from hull builder"))),

                new Equation(createSubscriptText("f", "c"),
                        new TextFlow(new Text("Compressive strength (MPa)")),
                        new TextFlow(new Text("Input"))),

                new Equation(createSubscriptText("α", ""),
                        new TextFlow(new Text("Internal column")),
                        new TextFlow(new Text("4"))),

                new Equation(createSubscriptText("λ", ""),
                        new TextFlow(new Text("Low-density concrete")),
                        new TextFlow(new Text("0.75"))),

                new Equation(createSubscriptText("β", "c"),
                        new TextFlow(new Text("Square column")),
                        new TextFlow(new Text("1"))),

                new Equation(createSubscriptText("φ", "c"),
                        new TextFlow(new Text("Concrete")),
                        new TextFlow(new Text("0.65"))),

                new Equation(createSubscriptText("d", ""),
                        new TextFlow(new Text("Diameter of knee")),
                        new TextFlow(new Text("13"))),

                new Equation(createSubscriptText("v", "c1"),
                        new TextFlow(new Text("First formula for calculating vc min")),
                        new TextFlow(new Text("(1 + 2 / "), createSubscriptText("β", "c"), new Text(")(0.19)("), createSubscriptText("λ", ""), new Text(")("),
                                createSubscriptText("φ", "c"), new Text(")(√"), createSubscriptText("f", "c"), new Text(")"))),

                new Equation(createSubscriptText("v", "c2"),
                        new TextFlow(new Text("Second formula for calculating vc min")),
                        new TextFlow(new Text("(0.19 + (α * d / "), createSubscriptText("P", "critical"), new Text(") * "), createSubscriptText("λ", ""),
                                new Text(" * "), createSubscriptText("φ", "c"), new Text(" * √"), createSubscriptText("f", "c"), new Text(")"))),

                new Equation(createSubscriptText("v", "c3"),
                        new TextFlow(new Text("Third equation for calculating vc min")),
                        new TextFlow(new Text("0.38 * "), createSubscriptText("λ", ""), new Text(" * "), createSubscriptText("φ", "c"),
                                new Text(" * √"), createSubscriptText("f", "c"))),

                new Equation(createSubscriptText("v", "c min"),
                        new TextFlow(new Text("Lowest of vc1,2,3")),
                        new TextFlow(new Text("min("), createSubscriptText("v", "c1"), new Text(", "), createSubscriptText("v", "c2"),
                                new Text(", "), createSubscriptText("v", "c3"), new Text(")"))),

                new Equation(
                        createSubscriptText("V", "c"),
                        new TextFlow(new Text("Shear strength of concrete")),
                        new TextFlow(
                                createSubscriptText("φ", ""),
                                new Text(" * "),
                                createSubscriptText("λ", ""),
                                new Text(" * "),
                                createSubscriptText("β", ""),
                                new Text(" * √"),
                                createSubscriptText("f", "c"),
                                new Text("'"),
                                new Text(" * "),
                                createSubscriptText("b", "w"),
                                new Text(" * "),
                                createSubscriptText("d", "v")
                        )
                )

        );

        // **Fixed missing semicolon**
        dataTable.setItems(data);
    }

    // Method to create subscripts properly using TextFlow
    private TextFlow createSubscriptText(String base, String subscript) {
        Text mainText = new Text(base);
        mainText.setStyle("-fx-fill: white;");
        if (subscript.isEmpty()) {
            return new TextFlow(mainText);
        }
        Text subText = new Text(subscript);
        subText.setStyle("-fx-font-size: 10px; -fx-fill: white; -fx-translate-y: 3px;"); // Adjust font size & position for subscript
        return new TextFlow(mainText, subText);
    }

    private void setRowHeight() {
        dataTable.setRowFactory(tv -> new TableRow<Equation>() {
            @Override
            protected void updateItem(Equation item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setPrefHeight(25);
                }
            }
        });
    }


    private void setTextFlowColumnFactory(TableColumn<Equation, TextFlow> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TextFlow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    for (Text node : item.getChildren().filtered(n -> n instanceof Text).toArray(Text[]::new)) {
                        if (!node.getStyle().contains("-fx-font-size: 10px;")) { // Preserve subscript formatting
                            node.setStyle("-fx-fill: white;"); // Set text color only, keep subscript styles
                        }
                    }
                    item.setMaxHeight(20);
                    item.setLineSpacing(0);
                    setGraphic(item);
                }
            }
        });
    }
}