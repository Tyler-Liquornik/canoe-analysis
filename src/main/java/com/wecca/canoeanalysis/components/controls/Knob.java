package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.ControlUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.Setter;

/**
 * Reskin of a slider as a circular knob
 */
@Getter @Setter
public class Knob extends Slider {

    private String name;

    public Knob(String name,
                double value,
                double min,
                double max,
                double layoutX,
                double layoutY) {
        this.name = name;
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        setMin(min);
        setMax(max);
        setValue(value);
        setSkin(new KnobSkin());
    }

    @Getter @Setter
    public class KnobSkin extends SkinBase<Knob> {

        // Skin makeup
        private Arc baseArc;
        private Arc valueArc;
        private Circle circle;
        private Button plusButton;
        private Button minusButton;
        private Label valueLabel;

        // Sizing and layout constants
        private final double baseArcDegreesLength = 240;
        private final double baseArcStartDegrees = 210;
        private final double radius = 50;
        private final double arcRadius = radius * 1.2;
        private final double iconSize = radius / 5.0;
        private final double knobBorderWidth = radius / 20.0;
        private final double arcWidth = knobBorderWidth * 0.75;

        public KnobSkin() {
            super(Knob.this);

            // Base black knob arc
            baseArc = new Arc();
            baseArc.setRadiusX(arcRadius);
            baseArc.setRadiusY(arcRadius);
            baseArc.setStrokeWidth(arcWidth);
            baseArc.setStroke(Color.BLACK);
            baseArc.setStartAngle(baseArcStartDegrees);
            baseArc.setLength(-baseArcDegreesLength);
            baseArc.setFill(Color.TRANSPARENT);

            // White arc that scales with value
            valueArc = new Arc();
            valueArc.setRadiusX(arcRadius);
            valueArc.setRadiusY(arcRadius);
            valueArc.setStrokeWidth(arcWidth);
            valueArc.setStroke(ColorPaletteService.getColor("white"));
            valueArc.setStartAngle(baseArcStartDegrees);
            valueArc.setLength(getValueArcLength());
            valueArc.setFill(Color.TRANSPARENT);

            // Create the knob itself
            circle = new Circle(radius, ColorPaletteService.getColor("above-surface"));
            circle.setStroke(ColorPaletteService.getColor("white"));
            circle.setStrokeWidth(knobBorderWidth);

            // Create Plus and Minus buttons using FontAwesome icons
            plusButton = ControlUtils.getIconButton(IconGlyphType.PLUS, this::increaseValue, iconSize);
            minusButton = ControlUtils.getIconButton(IconGlyphType.MINUS, this::decreaseValue, iconSize);

            // Value display (Label instead of Text)
            valueLabel = new Label(String.format("%s: %.2f", getName(), getValue()));
            valueLabel.setTextFill(ColorPaletteService.getColor("white"));

            // Layout the elements
            Pane container = new Pane();
            container.getChildren().addAll(baseArc, valueArc, circle, plusButton, minusButton, valueLabel);
            getChildren().add(container);
            layoutComponents();

            // Listen for value changes and update the arc
            valueProperty().addListener((obs, oldVal, newVal) -> {
                valueLabel.setText(String.format("%s: %.2f", getName(), getValue()));
                valueArc.setLength(getValueArcLength());
            });
        }

        /**
         * @return The arc length of the value arc
         */
        private double getValueArcLength() {
            double range = getMax() - getMin();
            double percentage = (getValue() - getMin()) / range;
            return -baseArcDegreesLength * percentage;
        }

        /**
         * Increase the knob's value
         * @param event the event triggering the increase
         */
        private void increaseValue(ActionEvent event) {
            if (getValue() < getMax())
                setValue(getValue() + 1);
        }

        /**
         * Increase the knob's value
         * @param event the event triggering the increase
         */
        private void decreaseValue(ActionEvent event) {
            if (getValue() > getMin())
                setValue(getValue() - 1);
        }

        /**
         * Layout knob components relative to layout of overall knob
         */
        private void layoutComponents() {
            baseArc.setLayoutX(60);
            baseArc.setLayoutY(60);
            valueArc.setLayoutX(60);
            valueArc.setLayoutY(60);
            circle.setLayoutX(60);
            circle.setLayoutY(60);
            plusButton.setLayoutX(95);
            plusButton.setLayoutY(95);
            minusButton.setLayoutX(5);
            minusButton.setLayoutY(95);
            valueLabel.setLayoutX(43);
            valueLabel.setLayoutY(115);
        }
    }
}
