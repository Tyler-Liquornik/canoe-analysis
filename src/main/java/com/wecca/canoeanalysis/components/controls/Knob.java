package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.components.graphics.IconGlyphType;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.ControlUtils;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
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

    public Knob(String name, double value, double min, double max, double layoutX, double layoutY) {
        this.name = name;
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        setMin(min);
        setMax(max);
        setSkin(new KnobSkin(value));
    }

    @Getter @Setter
    public class KnobSkin extends SkinBase<Knob> {
        private AnimationTimer animationTimer;
        private boolean itsAHold;
        private boolean itsAMinusHold;
        private Long currentTime = (long) 0;

        // Skin makeup
        private Arc baseArc;
        private Arc valueArc;
        private Circle knobBaseCircle;
        private Circle knobHandleCircle;
        private Button plusButton;
        private Button minusButton;
        private Label valueLabel;

        // Sizing and layout constants
        private final double baseArcDegreesAngleSpan = 240;
        private final double valueArcStartDegrees = 210;
        private final double knobBaseSizeRadius = 50;
        private final double knobCenterLayoutOffset = 60;

        // Derived sizing and layout constants
        private final double knobHandleSizeRadius = knobBaseSizeRadius / 10.0;
        private final double knobHandleArcRadius = knobBaseSizeRadius * 0.65;
        private final double arcRadius = knobBaseSizeRadius * 1.2;
        private final double iconSize = knobBaseSizeRadius / 5.0;
        private final double knobBorderWidth = knobBaseSizeRadius / 20.0;
        private final double arcWidth = knobBorderWidth * 0.75;

        public KnobSkin(double value)
        {
            super(Knob.this);

            // Base black knob arc
            baseArc = new Arc();
            baseArc.setRadiusX(arcRadius);
            baseArc.setRadiusY(arcRadius);
            baseArc.setStrokeWidth(arcWidth);
            baseArc.setStroke(Color.BLACK);
            baseArc.setStartAngle(valueArcStartDegrees);
            baseArc.setLength(-baseArcDegreesAngleSpan);
            baseArc.setFill(Color.TRANSPARENT);

            // White arc that scales with value
            valueArc = new Arc();
            valueArc.setRadiusX(arcRadius);
            valueArc.setRadiusY(arcRadius);
            valueArc.setStrokeWidth(arcWidth);
            valueArc.setStroke(ColorPaletteService.getColor("white"));
            valueArc.setStartAngle(valueArcStartDegrees);
            valueArc.setLength(getValueArcLength());
            valueArc.setFill(Color.TRANSPARENT);

            // Knob base
            knobBaseCircle = new Circle(knobBaseSizeRadius, ColorPaletteService.getColor("above-surface"));
            knobBaseCircle.setStroke(ColorPaletteService.getColor("white"));
            knobBaseCircle.setStrokeWidth(knobBorderWidth);

            // Draggable Knob handle
            knobHandleCircle = new Circle(knobHandleSizeRadius, ColorPaletteService.getColor("white"));
            knobHandleCircle.setStrokeWidth(0);
            knobHandleCircle.setOnMouseDragged(e -> setValueOnDrag(e.getX(), e.getY()));

            // Plus and Minus buttons using FontAwesome icons
            plusButton = ControlUtils.getIconButton(IconGlyphType.PLUS, MouseEvent.MOUSE_PRESSED, this::plusButtonPressed, iconSize, true);
            minusButton = ControlUtils.getIconButton(IconGlyphType.MINUS,MouseEvent.MOUSE_PRESSED ,this::minusButtonPressed, iconSize, true);

            plusButton.setOnMouseReleased(event ->
                    {
                        plusButtonReleased();
                    }
            );

            minusButton.setOnMouseReleased(event ->
            {
                minusButtonReleased();
            });

            animationTimer = new AnimationTimer() //A timer that is called in each frame after starting, will check if the button clicked or held, and react accordingly
            {
                @Override
                public void handle(long l)
                {
                    if(itsAHold && (System.currentTimeMillis() - currentTime)>200)
                    {
                        increaseValue(0.3);
                    }

                    if(itsAMinusHold && (System.currentTimeMillis() - currentTime)>200)
                    {
                        decreaseValue(0.3);
                    }

                }
            };





            // Value display label
            valueLabel = new Label(String.format("%s: %.2f", getName(), getValue()));
            valueLabel.setTextFill(ColorPaletteService.getColor("white"));

            // Layout the elements
            Pane container = new Pane();
            container.getChildren().addAll(baseArc, valueArc, knobBaseCircle, knobHandleCircle, plusButton, minusButton, valueLabel);
            getChildren().add(container);
            layoutComponents();

            // Listen for value changes and update the arc
            valueProperty().addListener((obs, oldVal, newVal) -> {
                valueLabel.setText(String.format("%s: %.2f", getName(), getValue()));
                valueArc.setLength(getValueArcLength());
                updateKnobHandle();
            });

            // Set the initial knob value
            setKnobValue(value);

            // Bind the value label x position to value to ensure it's always centered
            valueLabel.layoutXProperty().bind(valueLabel.widthProperty().divide(-2).add(knobCenterLayoutOffset));
        }

        /**
         * Calculate the position of the knob handle based on the current value
         */
        private void updateKnobHandle() {
            double angle = valueArcStartDegrees - getValueArcLength() - 60;
            double radians = Math.toRadians(angle);
            double handleX = knobCenterLayoutOffset + knobHandleArcRadius * Math.cos(radians);
            double handleY = knobCenterLayoutOffset + knobHandleArcRadius * Math.sin(radians);
            knobHandleCircle.setLayoutX(handleX);
            knobHandleCircle.setLayoutY(handleY);
        }

        /**
         * @return The arc length of the value arc
         */
        private double getValueArcLength() {
            double range = getMax() - getMin();
            double percentage = (getValue() - getMin()) / range;
            return -baseArcDegreesAngleSpan * percentage;
        }

        /**
         * Increase the knob's value
         * @param event the event triggering the increase
         */


        //if Button is released, a check is done & it was only a click, value is adjusted accordingly. AnimationTimer is also stopped
        private void plusButtonReleased()
        {
            itsAHold = false;

            if((System.currentTimeMillis() - currentTime)<200)
            {
                increaseValue(1);
            }
            animationTimer.stop();
        }

        private void minusButtonReleased()
        {
            itsAMinusHold = false;

            if((System.currentTimeMillis() - currentTime)<200)
            {
                decreaseValue(1);
            }
            animationTimer.stop();
        }

        //animationTimer starts on button press
        private void plusButtonPressed(MouseEvent event)
        {
            itsAHold = true;
            currentTime = System.currentTimeMillis();
            animationTimer.start();
        }

        private void minusButtonPressed(MouseEvent event)
        {
            itsAMinusHold = true;
            currentTime = System.currentTimeMillis();
            animationTimer.start();
        }


        /**
         * Increase the knob's value
         * @param event the event triggering the increase
         */
        private void decreaseValue(double dec) {
            if (getValue() > getMin())
                setValue(getValue() - dec);
        }
        private void increaseValue(double inc)
        {
            if (getValue() < getMax())
                setValue(getValue() + inc);
        }

        /**
         * Layout knob components relative to layout of overall knob
         */
        private void layoutComponents() {
            baseArc.setLayoutX(knobCenterLayoutOffset);
            baseArc.setLayoutY(knobCenterLayoutOffset);
            valueArc.setLayoutX(knobCenterLayoutOffset);
            valueArc.setLayoutY(knobCenterLayoutOffset);
            knobBaseCircle.setLayoutX(knobCenterLayoutOffset);
            knobBaseCircle.setLayoutY(knobCenterLayoutOffset);
            plusButton.setLayoutX(knobCenterLayoutOffset + 35);
            plusButton.setLayoutY(knobCenterLayoutOffset + 35);
            minusButton.setLayoutX(knobCenterLayoutOffset - 55);
            minusButton.setLayoutY(knobCenterLayoutOffset + 35);
            valueLabel.setLayoutX(knobCenterLayoutOffset - 17);
            valueLabel.setLayoutY(knobCenterLayoutOffset + 55);

            // Need to calculate knob handle position based on value of the knob
            updateKnobHandle();
        }

        /**
         * Handler for dragging the knob handle to set the value of the knob
         * @param mouseX the x position of the mouse on click (start of dragging)
         * @param mouseY the y position of the mouse on click (start of dragging)
         */
        private void setValueOnDrag(double mouseX, double mouseY) {
            // Calculate the angle of change
            double deltaX = mouseX - knobCenterLayoutOffset;
            double deltaY = mouseY - knobCenterLayoutOffset;
            double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
            double adjustedAngle = (angle - valueArcStartDegrees + 360) % 360;

            // Don't set past max angle span
            if (adjustedAngle <= baseArcDegreesAngleSpan) {
                double percentage = adjustedAngle / baseArcDegreesAngleSpan;
                double range = getMax() - getMin();
                double newValue = getMin() + percentage * range;

                // Limit the value change to 1% of the range to avoid jumpy behaviour
                double maxChange =  0.01 * range;
                double currentValue = getValue();
                double clampedValue = (newValue > currentValue)
                        ? Math.min(newValue, currentValue + maxChange)
                        : Math.max(newValue, currentValue - maxChange);

                // Prevent setting past min or max
                if (clampedValue < getMin())
                    clampedValue = getMin();
                if (clampedValue > getMax())
                    clampedValue = getMax();

                setKnobValue(clampedValue);
            }
        }

        public void setKnobValue(double value) {
            Knob.super.setValue(value);
            valueLabel.setText(String.format("%s: %.2f", getName(), value));
        }


        /**
         * Calculate the current angle based on the current value of the knob.
         * The angle is computed based on the percentage of the value within the min-max range.
         * @return The current angle of the knob in degrees.
         */
        private double getCurrentAngle() {
            double percentage = (getValue() - getMin()) / (getMax() - getMin());
            return valueArcStartDegrees - (percentage * baseArcDegreesAngleSpan);
        }
    }
}
