package com.wecca.canoeanalysis.controllers.modules;

import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.popups.CanoePresetPopupController;
import com.wecca.canoeanalysis.services.FailureEnvelopeService;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.Analysis;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.EnvelopeLine;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.MohrCircle;
import com.wecca.canoeanalysis.services.FailureEnvelopeService.TangencyPoint;
import com.wecca.canoeanalysis.utils.CanoePreset;
import com.wecca.canoeanalysis.utils.GirRaftPreset;
import com.wecca.canoeanalysis.utils.RaftPunkPreset;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import lombok.Setter;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Connects the failure-envelope form to the pure engineering calculations and
 * renders all strength and demand Mohr circles on one responsive canvas.
 * Numerical work remains in {@link FailureEnvelopeService}; this controller is
 * responsible only for validation, UI state, and drawing.
 */
public class FailureEnvelopeController implements Initializable, ModuleController {

    // Fixed chart palette selected to remain legible on PADDL's dark surfaces.
    private static final Color COMPRESSION_COLOR = Color.web("#4FC3F7");
    private static final Color TENSION_COLOR = Color.web("#FFB74D");
    private static final Color BENDING_COLOR = Color.web("#BB86FC");
    private static final Color SHEAR_COLOR = Color.web("#66BB6A");
    private static final Color ENVELOPE_COLOR = Color.web("#F5F5F5");
    private static final Color GRID_COLOR = Color.web("#3A3A3A");
    private static final Color AXIS_COLOR = Color.web("#8C8C8C");
    private static final Color TEXT_COLOR = Color.web("#C9C9C9");

    // Editable section, action, and material inputs supplied by the FXML view.
    @FXML
    private JFXTextField compressionField;
    @FXML
    private JFXTextField tensionField;
    @FXML
    private JFXTextField qMax;
    @FXML
    private JFXTextField momentOfInertia;
    @FXML
    private JFXTextField maxMoment;
    @FXML
    private JFXTextField canoeThickness;
    @FXML
    private JFXTextField maxShear;
    @FXML
    private JFXTextField maxCompression;
    @FXML
    private JFXTextField maxTension;
    @FXML
    private JFXTextField maxTensile;
    @FXML
    private JFXTextField maxShearStress;

    // Host pane whose dimensions drive the single combined chart canvas.
    @FXML
    private Pane chartPane;

    @Setter
    private static MainController mainController;

    // Current chart state. The two preset-only strengths are cleared as soon
    // as the user edits an input so manual calculations cannot retain them.
    private Canvas chartCanvas;
    private Analysis currentAnalysis;
    private Double presetShearMomentOfInertia;
    private Double presetCompressiveStrength;

    /** Wires field validation and keeps the chart canvas synchronized with its pane. */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setMainController(CanoeAnalysisApplication.getMainController());
        initModuleToolBarButtons();

        chartCanvas = new Canvas();
        chartCanvas.setMouseTransparent(true);
        chartPane.getChildren().add(chartCanvas);
        chartPane.widthProperty().addListener((observable, oldValue, newValue) -> resizeAndDrawChart());
        chartPane.heightProperty().addListener((observable, oldValue, newValue) -> resizeAndDrawChart());

        inputFields().forEach(field -> field.textProperty().addListener((observable, oldValue, newValue) -> {
            field.getStyleClass().remove("invalid-input");
            presetShearMomentOfInertia = null;
            presetCompressiveStrength = null;
        }));

        Platform.runLater(this::resizeAndDrawChart);
    }

    /**
     * Validates all form inputs, performs the combined analysis, updates the
     * numerical stress outputs, and redraws the chart in its existing layout.
     */
    @FXML
    public void generateDiagrams() {
        inputFields().forEach(field -> field.getStyleClass().remove("invalid-input"));
        SectionInputs inputs = readSectionInputs();
        Double tensileStrength = readNumber(maxTensile, false);
        Double compressiveStrength = inputs == null
                ? null
                : presetCompressiveStrength == null
                        ? inputs.compressionEdgeDistance()
                        : presetCompressiveStrength;
        if (inputs == null || compressiveStrength == null || tensileStrength == null) {
            mainController.showSnackbar("Please fill every input with a valid number before generating the diagram.");
            return;
        }

        try {
            currentAnalysis = FailureEnvelopeService.analyze(
                    compressiveStrength,
                    tensileStrength,
                    inputs.compressionEdgeDistance(),
                    inputs.tensionEdgeDistance(),
                    inputs.firstMoment(),
                    inputs.bendingSecondMoment(),
                    presetShearMomentOfInertia == null
                            ? inputs.bendingSecondMoment()
                            : presetShearMomentOfInertia,
                    inputs.thickness(),
                    inputs.maximumMoment(),
                    inputs.maximumShear());
            displayCalculatedStresses(currentAnalysis.stresses());
            drawChart();
        } catch (IllegalArgumentException exception) {
            mainController.showSnackbar(exception.getMessage());
        }
    }

    /** Runs the original Calculate values workflow without changing the screen layout. */
    @FXML
    public void calculateValues() {
        sectionInputFields().forEach(field -> field.getStyleClass().remove("invalid-input"));
        SectionInputs inputs = readSectionInputs();
        if (inputs == null) {
            mainController.showSnackbar("Please fill every section input with a valid number before calculating values.");
            return;
        }

        try {
            FailureEnvelopeService.StressResults stresses = FailureEnvelopeService.calculateSectionStresses(
                    inputs.compressionEdgeDistance(),
                    inputs.tensionEdgeDistance(),
                    inputs.firstMoment(),
                    inputs.bendingSecondMoment(),
                    presetShearMomentOfInertia == null
                            ? inputs.bendingSecondMoment()
                            : presetShearMomentOfInertia,
                    inputs.thickness(),
                    inputs.maximumMoment(),
                    inputs.maximumShear());
            displayCalculatedStresses(stresses);
        } catch (IllegalArgumentException exception) {
            mainController.showSnackbar(exception.getMessage());
        }
    }

    /** Populate the report inputs and generate Raft Punk's combined envelope. */
    private void loadRaftPunkPreset() {
        // Bending geometry and longitudinal actions populate the visible fields.
        compressionField.setText(String.format(Locale.US, "%.5f", RaftPunkPreset.BENDING_COMPRESSION_EDGE_DISTANCE_M));
        tensionField.setText(String.format(Locale.US, "%.5f", RaftPunkPreset.BENDING_TENSION_EDGE_DISTANCE_M));
        qMax.setText(String.format(Locale.US, "%.7f", RaftPunkPreset.SHEAR_FIRST_MOMENT_OF_AREA_M3));
        momentOfInertia.setText(String.format(Locale.US, "%.7f", RaftPunkPreset.BENDING_SECOND_MOMENT_OF_AREA_M4));
        maxMoment.setText(String.format(Locale.US, "%.4f", RaftPunkPreset.MAXIMUM_MOMENT_KN_M));
        maxShear.setText(String.format(Locale.US, "%.4f", RaftPunkPreset.MAXIMUM_SHEAR_KN));
        canoeThickness.setText(String.format(Locale.US, "%.3f", RaftPunkPreset.NOMINAL_STRUCTURAL_THICKNESS_M));
        maxTensile.setText(String.format(Locale.US, "%.2f", RaftPunkPreset.TENSILE_STRENGTH_MPA));

        // The shear-critical section uses a different inertia, and compressive
        // strength has no dedicated legacy text field. Preserve both as preset
        // metadata until a user edit switches the form back to manual mode.
        presetShearMomentOfInertia = RaftPunkPreset.SHEAR_SECOND_MOMENT_OF_AREA_M4;
        presetCompressiveStrength = RaftPunkPreset.COMPRESSIVE_STRENGTH_MPA;

        calculateValues();
        generateDiagrams();
        mainController.showSnackbar("Loaded Raft Punk failure-envelope values");
    }

    /** Loads GirRaft's published critical-section and material values. */
    private void loadGirRaftPreset() {
        compressionField.setText(String.format(Locale.US, "%.5f", GirRaftPreset.COMPRESSION_EDGE_DISTANCE_M));
        tensionField.setText(String.format(Locale.US, "%.5f", GirRaftPreset.TENSION_EDGE_DISTANCE_M));
        qMax.setText(String.format(Locale.US, "%.7f", GirRaftPreset.FIRST_MOMENT_OF_AREA_M3));
        momentOfInertia.setText(String.format(Locale.US, "%.7f", GirRaftPreset.SECOND_MOMENT_OF_AREA_M4));
        maxMoment.setText(String.format(Locale.US, "%.4f", GirRaftPreset.MAXIMUM_MOMENT_KN_M));
        maxShear.setText(String.format(Locale.US, "%.4f", GirRaftPreset.FAILURE_ENVELOPE_MAXIMUM_SHEAR_KN));
        canoeThickness.setText(String.format(Locale.US, "%.3f", GirRaftPreset.NOMINAL_STRUCTURAL_THICKNESS_M));
        maxTensile.setText(String.format(Locale.US, "%.2f", GirRaftPreset.TENSILE_STRENGTH_MPA));

        // GirRaft uses the same published critical section for bending and shear.
        presetShearMomentOfInertia = GirRaftPreset.SECOND_MOMENT_OF_AREA_M4;
        presetCompressiveStrength = GirRaftPreset.COMPRESSIVE_STRENGTH_MPA;

        calculateValues();
        generateDiagrams();
        mainController.showSnackbar("Loaded GirRaft (2025) failure-envelope values");
    }

    /** Opens the shared yearly-canoe preset chooser. */
    public void openCanoePresetPopup() {
        CanoePresetPopupController.open(mainController, this::loadCanoePreset);
    }

    /** Routes a popup selection to the matching failure-envelope inputs. */
    private void loadCanoePreset(CanoePreset preset) {
        switch (preset) {
            case GIRRAFT_2025 -> loadGirRaftPreset();
            case RAFT_PUNK_2026 -> loadRaftPunkPreset();
        }
    }

    /** Installs the single canoe preset control without changing the form. */
    private void initModuleToolBarButtons() {
        LinkedHashMap<IconGlyphType, Consumer<MouseEvent>> buttons = new LinkedHashMap<>();
        buttons.put(IconGlyphType.CANOE, event -> openCanoePresetPopup());
        mainController.resetToolBarButtons();
        mainController.setIconToolBarButtons(buttons);
        mainController.getModuleToolBarButtons().getFirst().setTooltip(new Tooltip("Canoe presets"));
    }

    /** @return every editable field that invalidates preset-only metadata when changed */
    private List<JFXTextField> inputFields() {
        return List.of(
                compressionField,
                tensionField,
                qMax,
                momentOfInertia,
                maxMoment,
                canoeThickness,
                maxShear,
                maxTensile);
    }

    /** @return the geometry and action fields required for stress calculation */
    private List<JFXTextField> sectionInputFields() {
        return List.of(
                compressionField,
                tensionField,
                qMax,
                momentOfInertia,
                maxMoment,
                canoeThickness,
                maxShear);
    }

    /**
     * Parses the shared section/action fields as one immutable value object.
     * Returning {@code null} lets the caller display one consolidated message
     * while individual invalid fields remain highlighted.
     */
    private SectionInputs readSectionInputs() {
        Double compressionEdgeDistance = readNumber(compressionField, false);
        Double tensionEdgeDistance = readNumber(tensionField, false);
        Double firstMoment = readNumber(qMax, false);
        Double bendingSecondMoment = readNumber(momentOfInertia, false);
        Double thickness = readNumber(canoeThickness, false);
        Double maximumMoment = readNumber(maxMoment, true);
        Double maximumShear = readNumber(maxShear, true);

        if (compressionEdgeDistance == null
                || tensionEdgeDistance == null
                || firstMoment == null
                || bendingSecondMoment == null
                || thickness == null
                || maximumMoment == null
                || maximumShear == null) {
            return null;
        }
        return new SectionInputs(
                compressionEdgeDistance,
                tensionEdgeDistance,
                firstMoment,
                bendingSecondMoment,
                thickness,
                maximumMoment,
                maximumShear);
    }

    /**
     * Parses one finite field and applies the same invalid-input class used by
     * the rest of PADDL. Geometry must be positive; actions may be zero.
     */
    private Double readNumber(JFXTextField field, boolean zeroAllowed) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            boolean invalid = !Double.isFinite(value) || (zeroAllowed ? value < 0.0 : value <= 0.0);
            if (invalid) {
                markInvalid(field);
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            markInvalid(field);
            return null;
        }
    }

    /** Adds the invalid style once so repeated validation cannot duplicate it. */
    private void markInvalid(JFXTextField field) {
        if (!field.getStyleClass().contains("invalid-input")) {
            field.getStyleClass().add("invalid-input");
        }
    }

    /** Writes calculated MPa magnitudes into the three read-only result fields. */
    private void displayCalculatedStresses(FailureEnvelopeService.StressResults stresses) {
        maxCompression.setText(String.format(Locale.US, "%.3f", stresses.compressionMpa()));
        maxTension.setText(String.format(Locale.US, "%.3f", stresses.tensionMpa()));
        maxShearStress.setText(String.format(Locale.US, "%.3f", stresses.shearMpa()));
    }

    /** Resizes the backing canvas to the FXML pane before every redraw. */
    private void resizeAndDrawChart() {
        if (chartCanvas == null) {
            return;
        }
        chartCanvas.setWidth(Math.max(0.0, chartPane.getWidth()));
        chartCanvas.setHeight(Math.max(0.0, chartPane.getHeight()));
        drawChart();
    }

    /** Draws the complete, equally scaled failure-envelope chart. */
    private void drawChart() {
        if (chartCanvas == null) {
            return;
        }

        GraphicsContext graphics = chartCanvas.getGraphicsContext2D();
        double width = chartCanvas.getWidth();
        double height = chartCanvas.getHeight();
        graphics.clearRect(0.0, 0.0, width, height);

        if (width < 180.0 || height < 160.0) {
            return;
        }
        if (currentAnalysis == null) {
            drawPlaceholder(graphics, width, height);
            return;
        }

        // Reserve fixed margins for tick labels, the legend, and axis captions.
        double plotLeft = 62.0;
        double plotTop = 58.0;
        double plotRight = width - 22.0;
        double plotBottom = height - 54.0;
        double plotWidth = plotRight - plotLeft;
        double plotHeight = plotBottom - plotTop;

        MohrCircle compression = currentAnalysis.compressionStrengthCircle();
        MohrCircle tension = currentAnalysis.tensionStrengthCircle();
        MohrCircle bending = currentAnalysis.bendingStressCircle();
        MohrCircle shear = currentAnalysis.shearStressCircle();
        EnvelopeLine envelope = currentAnalysis.envelope();
        TangencyPoint compressionTangent = FailureEnvelopeService.upperTangencyPoint(compression, envelope);
        TangencyPoint tensionTangent = FailureEnvelopeService.upperTangencyPoint(tension, envelope);

        // Limit the displayed tangent lines to the strength circles plus a
        // small visual extension instead of drawing them to arbitrary infinity.
        double tangentSpan = Math.abs(tensionTangent.normalStressMpa() - compressionTangent.normalStressMpa());
        double tangentExtension = Math.max(tangentSpan * 0.18, 0.05);
        double envelopeStartX = Math.min(compressionTangent.normalStressMpa(), tensionTangent.normalStressMpa())
                - tangentExtension;
        double envelopeEndX = Math.max(compressionTangent.normalStressMpa(), tensionTangent.normalStressMpa())
                + tangentExtension;

        // Include every circle and both tangent endpoints in the data bounds.
        double minimumX = Math.min(
                Math.min(compression.minimumNormalStressMpa(), tension.minimumNormalStressMpa()),
                Math.min(Math.min(bending.minimumNormalStressMpa(), shear.minimumNormalStressMpa()), envelopeStartX));
        double maximumX = Math.max(
                Math.max(compression.maximumNormalStressMpa(), tension.maximumNormalStressMpa()),
                Math.max(Math.max(bending.maximumNormalStressMpa(), shear.maximumNormalStressMpa()), envelopeEndX));
        double xPadding = Math.max((maximumX - minimumX) * 0.08, 0.05);
        minimumX -= xPadding;
        maximumX += xPadding;

        double maximumAbsY = Math.max(
                Math.max(compression.radiusMpa(), tension.radiusMpa()),
                Math.max(bending.radiusMpa(), shear.radiusMpa()));
        maximumAbsY = Math.max(maximumAbsY, Math.abs(envelope.upperShearMpa(envelopeStartX)));
        maximumAbsY = Math.max(maximumAbsY, Math.abs(envelope.upperShearMpa(envelopeEndX)));
        maximumAbsY = Math.max(maximumAbsY * 1.12, 0.1);

        // Equal horizontal and vertical scales are essential: unequal scales
        // would turn valid Mohr circles into ellipses and misrepresent tangency.
        PlotTransform transform = PlotTransform.equalScale(
                plotLeft,
                plotTop,
                plotWidth,
                plotHeight,
                minimumX,
                maximumX,
                -maximumAbsY,
                maximumAbsY);

        drawGridAndAxes(graphics, transform);

        // Clip engineering curves to the plot rectangle while leaving labels
        // and the legend free to use the surrounding canvas margins.
        graphics.save();
        graphics.beginPath();
        graphics.rect(plotLeft, plotTop, plotWidth, plotHeight);
        graphics.closePath();
        graphics.clip();

        drawMohrCircle(graphics, transform, compression, COMPRESSION_COLOR, false);
        drawMohrCircle(graphics, transform, tension, TENSION_COLOR, false);
        drawMohrCircle(graphics, transform, bending, BENDING_COLOR, true);
        drawMohrCircle(graphics, transform, shear, SHEAR_COLOR, true);
        drawEnvelope(graphics, transform, envelope, envelopeStartX, envelopeEndX);
        graphics.restore();

        drawLegend(graphics, width, envelope);

        graphics.setFill(TEXT_COLOR);
        graphics.setFont(Font.font("Roboto", 11.0));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.BOTTOM);
        graphics.fillText("Normal stress, σ (MPa)   ·   compression  ←  0  →  tension", width / 2.0, height - 6.0);

        graphics.save();
        graphics.translate(13.0, (plotTop + plotBottom) / 2.0);
        graphics.rotate(-90.0);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.fillText("Shear stress, τ (MPa)", 0.0, 0.0);
        graphics.restore();
    }

    /** Draws compact line samples and the numeric envelope equation. */
    private void drawLegend(GraphicsContext graphics, double width, EnvelopeLine envelope) {
        Color[] colors = {COMPRESSION_COLOR, TENSION_COLOR, BENDING_COLOR, SHEAR_COLOR, ENVELOPE_COLOR};
        String[] labels = {"Compression", "Tension", "Bending", "Shear", "Envelope"};
        double slotWidth = (width - 24.0) / labels.length;

        graphics.setFont(Font.font("Roboto", 9.0));
        graphics.setTextAlign(TextAlignment.LEFT);
        graphics.setTextBaseline(VPos.CENTER);
        graphics.setLineWidth(2.0);
        for (int index = 0; index < labels.length; index++) {
            double x = 12.0 + index * slotWidth;
            graphics.setStroke(colors[index]);
            if (index == labels.length - 1) {
                graphics.setLineDashes(8.0, 5.0);
            } else if (index >= 2) {
                graphics.setLineDashes(5.0, 3.0);
            } else {
                graphics.setLineDashes();
            }
            graphics.strokeLine(x, 17.0, x + 14.0, 17.0);
            graphics.setFill(TEXT_COLOR);
            graphics.fillText(labels[index], x + 19.0, 17.0);
        }
        graphics.setLineDashes();
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFill(TEXT_COLOR);
        graphics.fillText(
                String.format(Locale.US, "τ = ±(%.3fσ + %.3f) MPa", envelope.slope(), envelope.interceptMpa()),
                width / 2.0,
                40.0);
    }

    /** Displays neutral chart text until a valid analysis has been generated. */
    private void drawPlaceholder(GraphicsContext graphics, double width, double height) {
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.CENTER);
        graphics.setFill(Color.web("#D8D8D8"));
        graphics.setFont(Font.font("Roboto", 15.0));
        graphics.fillText("Combined failure envelope", width / 2.0, height / 2.0);
    }

    /** Draws automatically spaced ticks, zero axes, grid lines, and the plot border. */
    private void drawGridAndAxes(GraphicsContext graphics, PlotTransform transform) {
        double xTickStep = niceTickStep(transform.visibleXRange() / 8.0);
        double yTickStep = niceTickStep(transform.visibleYRange() / 7.0);

        graphics.setFont(Font.font("Roboto", 10.0));
        graphics.setLineWidth(1.0);
        graphics.setTextBaseline(VPos.TOP);
        graphics.setTextAlign(TextAlignment.CENTER);

        double firstXTick = Math.ceil(transform.visibleMinimumX() / xTickStep) * xTickStep;
        for (double x = firstXTick; x <= transform.visibleMaximumX() + xTickStep * 0.1; x += xTickStep) {
            double pixelX = transform.toPixelX(x);
            graphics.setStroke(Math.abs(x) < xTickStep * 0.01 ? AXIS_COLOR : GRID_COLOR);
            graphics.strokeLine(pixelX, transform.plotTop(), pixelX, transform.plotBottom());
            graphics.setFill(TEXT_COLOR);
            graphics.fillText(formatTick(x), pixelX, transform.plotBottom() + 7.0);
        }

        graphics.setTextBaseline(VPos.CENTER);
        graphics.setTextAlign(TextAlignment.RIGHT);
        double firstYTick = Math.ceil(transform.visibleMinimumY() / yTickStep) * yTickStep;
        for (double y = firstYTick; y <= transform.visibleMaximumY() + yTickStep * 0.1; y += yTickStep) {
            double pixelY = transform.toPixelY(y);
            graphics.setStroke(Math.abs(y) < yTickStep * 0.01 ? AXIS_COLOR : GRID_COLOR);
            graphics.strokeLine(transform.plotLeft(), pixelY, transform.plotRight(), pixelY);
            graphics.setFill(TEXT_COLOR);
            graphics.fillText(formatTick(y), transform.plotLeft() - 8.0, pixelY);
        }

        graphics.setStroke(Color.web("#505050"));
        graphics.strokeRect(transform.plotLeft(), transform.plotTop(), transform.plotWidth(), transform.plotHeight());
    }

    /**
     * Draws one Mohr circle; demand circles are dashed to distinguish them
     * from the solid material-strength limits.
     */
    private void drawMohrCircle(
            GraphicsContext graphics,
            PlotTransform transform,
            MohrCircle circle,
            Color color,
            boolean demandCircle) {
        double centerX = transform.toPixelX(circle.centerMpa());
        double centerY = transform.toPixelY(0.0);
        double radiusPixels = circle.radiusMpa() * transform.scale();

        if (radiusPixels < 1.0) {
            graphics.setFill(color);
            graphics.fillOval(centerX - 3.0, centerY - 3.0, 6.0, 6.0);
            return;
        }

        graphics.setStroke(color);
        graphics.setLineWidth(demandCircle ? 2.4 : 2.1);
        graphics.setLineDashes(demandCircle ? new double[]{7.0, 4.0} : new double[]{});
        graphics.strokeOval(
                centerX - radiusPixels,
                centerY - radiusPixels,
                radiusPixels * 2.0,
                radiusPixels * 2.0);
        graphics.setLineDashes();
    }

    /** Draws the symmetric upper and lower tangent lines over the chosen x-span. */
    private void drawEnvelope(
            GraphicsContext graphics,
            PlotTransform transform,
            EnvelopeLine envelope,
            double startX,
            double endX) {
        graphics.setStroke(ENVELOPE_COLOR);
        graphics.setLineWidth(2.0);
        graphics.setLineDashes(8.0, 5.0);
        graphics.strokeLine(
                transform.toPixelX(startX),
                transform.toPixelY(envelope.upperShearMpa(startX)),
                transform.toPixelX(endX),
                transform.toPixelY(envelope.upperShearMpa(endX)));
        graphics.strokeLine(
                transform.toPixelX(startX),
                transform.toPixelY(envelope.lowerShearMpa(startX)),
                transform.toPixelX(endX),
                transform.toPixelY(envelope.lowerShearMpa(endX)));
        graphics.setLineDashes();
    }

    /**
     * Rounds a requested tick interval to the conventional 1, 2, 5, or 10
     * sequence so axis labels stay predictable at any window size.
     */
    private double niceTickStep(double roughStep) {
        double exponent = Math.floor(Math.log10(roughStep));
        double fraction = roughStep / Math.pow(10.0, exponent);
        double niceFraction;
        if (fraction <= 1.0) {
            niceFraction = 1.0;
        } else if (fraction <= 2.0) {
            niceFraction = 2.0;
        } else if (fraction <= 5.0) {
            niceFraction = 5.0;
        } else {
            niceFraction = 10.0;
        }
        return niceFraction * Math.pow(10.0, exponent);
    }

    /** Formats tick labels compactly while removing insignificant trailing zeros. */
    private String formatTick(double value) {
        if (Math.abs(value) < 1.0e-10) {
            return "0";
        }
        double absoluteValue = Math.abs(value);
        if (absoluteValue >= 1_000.0 || absoluteValue < 0.001) {
            return String.format(Locale.US, "%.1e", value);
        }
        String formatted = String.format(Locale.US, absoluteValue < 1.0 ? "%.2f" : "%.1f", value);
        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Parsed geometry and action inputs shared by both Calculate and Generate. */
    private record SectionInputs(
            double compressionEdgeDistance,
            double tensionEdgeDistance,
            double firstMoment,
            double bendingSecondMoment,
            double thickness,
            double maximumMoment,
            double maximumShear) {
    }

    /**
     * Maps engineering coordinates to canvas pixels with one shared scale.
     * The y conversion is inverted because JavaFX canvas coordinates increase
     * downward while positive shear is drawn upward.
     */
    private record PlotTransform(
            double plotLeft,
            double plotTop,
            double plotWidth,
            double plotHeight,
            double centerX,
            double centerY,
            double scale) {

        /** Creates a transform centred on the requested bounds without stretching either axis. */
        private static PlotTransform equalScale(
                double plotLeft,
                double plotTop,
                double plotWidth,
                double plotHeight,
                double minimumX,
                double maximumX,
                double minimumY,
                double maximumY) {
            double xRange = maximumX - minimumX;
            double yRange = maximumY - minimumY;
            double scale = Math.min(plotWidth / xRange, plotHeight / yRange);
            return new PlotTransform(
                    plotLeft,
                    plotTop,
                    plotWidth,
                    plotHeight,
                    (minimumX + maximumX) / 2.0,
                    (minimumY + maximumY) / 2.0,
                    scale);
        }

        private double plotRight() {
            return plotLeft + plotWidth;
        }

        private double plotBottom() {
            return plotTop + plotHeight;
        }

        private double visibleXRange() {
            return plotWidth / scale;
        }

        private double visibleYRange() {
            return plotHeight / scale;
        }

        private double visibleMinimumX() {
            return centerX - visibleXRange() / 2.0;
        }

        private double visibleMaximumX() {
            return centerX + visibleXRange() / 2.0;
        }

        private double visibleMinimumY() {
            return centerY - visibleYRange() / 2.0;
        }

        private double visibleMaximumY() {
            return centerY + visibleYRange() / 2.0;
        }

        /** Converts normal stress in MPa to a horizontal canvas coordinate. */
        private double toPixelX(double x) {
            return plotLeft + plotWidth / 2.0 + (x - centerX) * scale;
        }

        /** Converts shear stress in MPa to an inverted vertical canvas coordinate. */
        private double toPixelY(double y) {
            return plotTop + plotHeight / 2.0 - (y - centerY) * scale;
        }
    }
}
