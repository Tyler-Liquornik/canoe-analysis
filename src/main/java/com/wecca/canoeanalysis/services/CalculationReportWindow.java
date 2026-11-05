package com.wecca.canoeanalysis.services;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.IOException;

/** Local report preview, clipboard and self-contained printable HTML export. */
public final class CalculationReportWindow {
    private CalculationReportWindow() {}
    public static void show(Window owner, CalculationReport report) {
        if (report == null || report.steps().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Run a calculation first to generate its algebra.");
            alert.initOwner(owner); alert.show(); return;
        }
        Stage stage = new Stage(); stage.initOwner(owner); stage.setTitle(report.title() + " — Report algebra");
        TextArea preview = new TextArea(report.text()); preview.setEditable(false); preview.setWrapText(true);
        preview.setStyle("-fx-font-family: 'Cambria Math'; -fx-font-size: 20px;");
        VBox equations = new VBox(22);
        equations.setPadding(new Insets(24)); equations.setStyle("-fx-background-color: white;");
        Label heading = new Label(report.title()); heading.setStyle("-fx-font-size: 22px; -fx-text-fill: #111;");
        equations.getChildren().add(heading);
        Label note = new Label("Calculated from the inputs at the time you pressed Calculate. Recalculate after editing.");
        note.setWrapText(true); note.setStyle("-fx-text-fill: #444;"); equations.getChildren().add(note);
        for (CalculationReport.Step step : report.steps()) {
            VBox block = new VBox(10,
                    ReportEquationView.equation(step.symbol() + " = " + step.equation()),
                    ReportEquationView.equation(step.symbol() + " = " + step.substitution()),
                    ReportEquationView.equation(step.symbol() + " = " + CalculationReport.number(step.value()) + " " + step.unit()));
            equations.getChildren().add(block);
        }
        ScrollPane scroll = new ScrollPane(equations); scroll.setFitToWidth(true);
        Tab visualTab = new Tab("Visual algebra", scroll); visualTab.setClosable(false);
        Tab textTab = new Tab("Text", preview); textTab.setClosable(false);
        TabPane tabs = new TabPane(visualTab, textTab);
        Button copy = new Button("Copy algebra");
        copy.setOnAction(event -> { ClipboardContent content = new ClipboardContent();
            content.putString(report.text()); content.putHtml(report.html()); Clipboard.getSystemClipboard().setContent(content); });
        Button export = new Button("Export report HTML");
        export.setOnAction(event -> {
            FileChooser chooser = new FileChooser(); chooser.setTitle("Save report algebra");
            chooser.setInitialFileName("paddl-calculations.html");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Report HTML", "*.html"));
            java.io.File file = chooser.showSaveDialog(stage);
            if (file != null) try { Files.writeString(file.toPath(), report.html(), StandardCharsets.UTF_8); }
            catch (IOException exception) { new Alert(Alert.AlertType.ERROR, "Could not save report: " + exception.getMessage()).show(); }
        });
        BorderPane pane = new BorderPane(tabs); pane.setBottom(new HBox(12, copy, export));
        stage.setScene(new Scene(pane, 900, 700)); stage.show();
    }
}
