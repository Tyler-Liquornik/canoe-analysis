package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.services.StlImportService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Beta UI for importing STL geometry and creating an unvalidated preset draft. */
public class StlLoaderController implements Initializable, ModuleController {

    @Setter
    private static MainController mainController;

    @FXML
    private ComboBox<String> unitComboBox;
    @FXML
    private Label fileNameLabel;
    @FXML
    private Label formatValueLabel;
    @FXML
    private Label meshValueLabel;
    @FXML
    private Label dimensionsValueLabel;
    @FXML
    private Label axesValueLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Canvas previewCanvas;
    @FXML
    private JFXTextField presetNameField;
    @FXML
    private JFXTextField presetYearField;
    @FXML
    private JFXButton chooseFileButton;
    @FXML
    private JFXButton saveDraftButton;

    private Path selectedFile;
    private StlImportService.Analysis currentAnalysis;
    private long analysisRequest;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        mainController.resetToolBarButtons();

        unitComboBox.getItems().setAll(Arrays.stream(StlImportService.InputUnit.values())
                .map(StlImportService.InputUnit::getDisplayName)
                .toList());
        unitComboBox.getSelectionModel().select(StlImportService.InputUnit.MILLIMETRES.getDisplayName());
        unitComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedFile != null && newValue != null && !newValue.equals(oldValue)) {
                startAnalysis();
            }
        });
        presetYearField.setText(Integer.toString(Year.now().getValue()));
        drawEmptyPreview();
    }

    /** Lets the user select an STL and immediately analyses it off the UI thread. */
    @FXML
    private void chooseStlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose canoe STL");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("STL mesh (*.stl)", "*.stl", "*.STL"));
        File selected = chooser.showOpenDialog(mainController.getPrimaryStage());
        if (selected == null) {
            return;
        }

        selectedFile = selected.toPath();
        fileNameLabel.setText(selected.getName());
        if (presetNameField.getText() == null || presetNameField.getText().isBlank()) {
            presetNameField.setText(removeExtension(selected.getName()));
        }
        startAnalysis();
    }

    /** Re-runs geometry extraction when either the file or source units change. */
    private void startAnalysis() {
        StlImportService.InputUnit unit = StlImportService.InputUnit.fromDisplayName(
                unitComboBox.getValue());
        long request = ++analysisRequest;
        currentAnalysis = null;
        setBusy(true);
        statusLabel.setText("Reading mesh and deriving longitudinal profiles…");

        Task<StlImportService.Analysis> task = new Task<>() {
            @Override
            protected StlImportService.Analysis call() throws IOException {
                return StlImportService.analyse(selectedFile, unit);
            }
        };
        task.setOnSucceeded(event -> {
            if (request != analysisRequest) {
                return;
            }
            currentAnalysis = task.getValue();
            setBusy(false);
            updateSummary(currentAnalysis);
            drawPreview(currentAnalysis);
            statusLabel.setText(
                    "Geometry ready. Review the preview, then save an unvalidated preset draft.");
        });
        task.setOnFailed(event -> {
            if (request != analysisRequest) {
                return;
            }
            setBusy(false);
            drawEmptyPreview();
            Throwable error = task.getException();
            String message = error == null ? "Unknown STL import error" : error.getMessage();
            statusLabel.setText("Could not load this STL: " + message);
            mainController.showSnackbar("STL import failed: " + message);
        });

        Thread worker = new Thread(task, "paddl-stl-loader");
        worker.setDaemon(true);
        worker.start();
    }

    /** Saves geometry samples locally without registering an unsafe production preset. */
    @FXML
    private void savePresetDraft() {
        if (currentAnalysis == null) {
            mainController.showSnackbar("Choose and successfully load an STL first");
            return;
        }

        String presetName = presetNameField.getText();
        int year;
        try {
            year = Integer.parseInt(presetYearField.getText().trim());
        } catch (RuntimeException exception) {
            mainController.showSnackbar("Enter a valid four-digit preset year");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save beta preset draft");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PADDL preset draft (*.yaml)", "*.yaml"));
        chooser.setInitialFileName(safeFileName(presetName, year));
        File output = chooser.showSaveDialog(mainController.getPrimaryStage());
        if (output == null) {
            return;
        }

        try {
            StlImportService.writePresetDraft(output.toPath(), currentAnalysis, presetName, year);
            statusLabel.setText("Saved beta preset draft: " + output.getName());
            mainController.showSnackbar("Saved local preset draft");
        } catch (IOException | IllegalArgumentException exception) {
            statusLabel.setText("Could not save draft: " + exception.getMessage());
            mainController.showSnackbar("Preset draft was not saved: " + exception.getMessage());
        }
    }

    private void setBusy(boolean busy) {
        chooseFileButton.setDisable(busy);
        unitComboBox.setDisable(busy);
        saveDraftButton.setDisable(busy || currentAnalysis == null);
    }

    private void updateSummary(StlImportService.Analysis analysis) {
        formatValueLabel.setText(analysis.format() + " STL");
        meshValueLabel.setText(String.format(
                Locale.US,
                "%,d triangles • %,d unique vertices",
                analysis.triangleCount(),
                analysis.uniqueVertexCount()));
        dimensionsValueLabel.setText(String.format(
                Locale.US,
                "%.3f × %.3f × %.3f m",
                analysis.lengthMetres(),
                analysis.widthMetres(),
                analysis.heightMetres()));
        axesValueLabel.setText(String.format(
                "%s length • %s width • %s height",
                analysis.lengthAxis(),
                analysis.widthAxis(),
                analysis.heightAxis()));
    }

    private void drawEmptyPreview() {
        GraphicsContext graphics = previewCanvas.getGraphicsContext2D();
        fillPreviewBackground(graphics);
        graphics.setFill(Color.web("#8A8A8A"));
        graphics.setFont(Font.font(14));
        graphics.fillText("Choose an STL file to preview its side and top outlines.", 55, 165);
    }

    private void drawPreview(StlImportService.Analysis analysis) {
        GraphicsContext graphics = previewCanvas.getGraphicsContext2D();
        fillPreviewBackground(graphics);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font("Roboto", 13));
        graphics.fillText("SIDE PROFILE", 18, 23);
        graphics.fillText("TOP PROFILE", 18, 181);

        drawSideProfile(graphics, analysis.sideProfile(), 18, 35, 484, 118);
        drawTopProfile(graphics, analysis.topProfile(), 18, 192, 484, 108);
    }

    private void fillPreviewBackground(GraphicsContext graphics) {
        graphics.setFill(Color.web("#282828"));
        graphics.fillRoundRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight(), 12, 12);
    }

    private void drawSideProfile(
            GraphicsContext graphics,
            List<StlImportService.ProfilePoint> points,
            double left,
            double top,
            double width,
            double height) {
        double maximum = points.stream()
                .mapToDouble(StlImportService.ProfilePoint::valueMetres)
                .max()
                .orElse(1.0);
        maximum = Math.max(maximum, 1.0e-9);
        double length = points.getLast().xMetres();

        graphics.setStroke(Color.web("#BB86FC"));
        graphics.setLineWidth(2.0);
        graphics.beginPath();
        graphics.moveTo(left, top);
        for (StlImportService.ProfilePoint point : points) {
            double x = left + width * point.xMetres() / length;
            double y = top + height * point.valueMetres() / maximum;
            graphics.lineTo(x, y);
        }
        graphics.lineTo(left + width, top);
        graphics.closePath();
        graphics.stroke();
        drawSampleDots(graphics, points, left, top, width, height, length, maximum, false);
    }

    private void drawTopProfile(
            GraphicsContext graphics,
            List<StlImportService.ProfilePoint> points,
            double left,
            double top,
            double width,
            double height) {
        double maximum = points.stream()
                .mapToDouble(StlImportService.ProfilePoint::valueMetres)
                .max()
                .orElse(1.0);
        maximum = Math.max(maximum, 1.0e-9);
        double length = points.getLast().xMetres();
        double centre = top + height / 2.0;

        graphics.setStroke(Color.web("#BB86FC"));
        graphics.setLineWidth(2.0);
        graphics.beginPath();
        for (int i = 0; i < points.size(); i++) {
            StlImportService.ProfilePoint point = points.get(i);
            double x = left + width * point.xMetres() / length;
            double y = centre - height * 0.48 * point.valueMetres() / maximum;
            if (i == 0) {
                graphics.moveTo(x, y);
            } else {
                graphics.lineTo(x, y);
            }
        }
        for (int i = points.size() - 1; i >= 0; i--) {
            StlImportService.ProfilePoint point = points.get(i);
            double x = left + width * point.xMetres() / length;
            double y = centre + height * 0.48 * point.valueMetres() / maximum;
            graphics.lineTo(x, y);
        }
        graphics.closePath();
        graphics.stroke();
        drawSampleDots(graphics, points, left, centre, width, height * 0.48, length, maximum, true);
    }

    private void drawSampleDots(
            GraphicsContext graphics,
            List<StlImportService.ProfilePoint> points,
            double left,
            double originY,
            double width,
            double height,
            double length,
            double maximum,
            boolean mirror) {
        graphics.setFill(Color.WHITE);
        for (int i = 0; i < points.size(); i += 10) {
            StlImportService.ProfilePoint point = points.get(i);
            double x = left + width * point.xMetres() / length;
            double y = mirror
                    ? originY - height * point.valueMetres() / maximum
                    : originY + height * point.valueMetres() / maximum;
            graphics.fillOval(x - 2, y - 2, 4, 4);
            if (mirror) {
                double reflectedY = originY + height * point.valueMetres() / maximum;
                graphics.fillOval(x - 2, reflectedY - 2, 4, 4);
            }
        }
    }

    private static String removeExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String safeFileName(String presetName, int year) {
        String base = presetName == null ? "canoe" : presetName.trim().toLowerCase(Locale.ROOT);
        base = base.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "canoe";
        }
        return base + "-" + year + "-preset-draft.yaml";
    }
}
