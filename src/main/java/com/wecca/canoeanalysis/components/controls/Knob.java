package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
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
    private boolean locked;

    public Knob(String name, double value, double min, double max, double layoutX, double layoutY, double size) {
        this.name = name;
        this.locked = false;
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        setMin(min);
        setMax(max);
        setSkin(new KnobSkin(value, size));
    }

    @Getter @Setter
    public class KnobSkin extends SkinBase<Knob> {
        private AnimationTimer animationTimer;
        private boolean itsAPlusHold;
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
        private final double knobBaseSizeRadius = 40;
        private final double knobCenterLayoutOffset = 60;

        // Derived sizing and layout constants
        private final double knobHandleSizeRadius;
        private final double knobHandleArcRadius;
        private final double arcRadius;
        private final double iconSize;
        private final double knobBorderWidth;
        private final double arcWidth;

        public KnobSkin(double value, double size) {
            super(Knob.this);

            knobHandleSizeRadius = size / 10.0;
            knobHandleArcRadius = size * 0.65;
            arcRadius = size * 1.2;
            iconSize = size / 5.0;
            knobBorderWidth = size / 20.0;
            arcWidth = knobBorderWidth * 0.75;

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
            knobBaseCircle = new Circle(size, ColorPaletteService.getColor("above-surface"));
            knobBaseCircle.setStroke(ColorPaletteService.getColor("white"));
            knobBaseCircle.setStrokeWidth(knobBorderWidth);

            // Draggable Knob handle
            knobHandleCircle = new Circle(knobHandleSizeRadius, ColorPaletteService.getColor("white"));
            knobHandleCircle.setStrokeWidth(0);
            knobHandleCircle.setOnMouseEntered(e -> {if (!isLocked()) knobHandleCircle.setCursor(Cursor.HAND);});
            knobHandleCircle.setOnMouseDragged(e -> {
                if (!isLocked()) {
                    knobHandleCircle.setCursor(Cursor.HAND);
                    setValueOnDrag(e.getX(), e.getY());
                }
            });
            knobHandleCircle.setOnMousePressed(e -> {if (!isLocked()) knobHandleCircle.setCursor(Cursor.HAND);});
            knobHandleCircle.setOnMouseReleased(e -> knobHandleCircle.setCursor(Cursor.DEFAULT));

            // Plus and Minus buttons using FontAwesome icons
            plusButton = IconButton.getKnobPlusOrMinusButton(true, this::plusButtonPressed, this::plusButtonReleased, iconSize);
            minusButton = IconButton.getKnobPlusOrMinusButton(false, this::minusButtonPressed, this::minusButtonReleased, iconSize);
            plusButton.setOnMouseEntered(e -> plusButton.setCursor(isLocked() ? Cursor.DEFAULT : Cursor.HAND));
            minusButton.setOnMouseEntered(e -> minusButton.setCursor(isLocked() ? Cursor.DEFAULT : Cursor.HAND));

            // A timer that is called in each frame after starting, will check if the button clicked or held, and react accordingly
            animationTimer = new AnimationTimer() {
                @Override
                public void handle(long l) {
                    if (!isLocked()) {
                        if (itsAPlusHold && (System.currentTimeMillis() - currentTime) > 200) increaseValue(0.3);
                        if (itsAMinusHold && (System.currentTimeMillis() - currentTime) > 200) decreaseValue(0.3);
                    }
                }
            };

            // Value display label
            valueLabel = new Label(isLocked() ? String.format("%s: %s", getName(), "-") : String.format("%s: %.2f", getName(), getValue()));
            valueLabel.setTextFill(ColorPaletteService.getColor("white"));

            // Layout the elements
            Pane container = new Pane();
            container.getChildren().addAll(baseArc, valueArc, knobBaseCircle, knobHandleCircle, plusButton, minusButton, valueLabel);
            getChildren().add(container);
            layoutComponents(size);

            // Listen for value changes and update the arc
            valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isLocked()) {
                    valueLabel.setText(String.format("%s: %.2f", getName(), getValue()));
                    valueArc.setLength(getValueArcLength());
                    updateKnobHandle();
                }
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
         * Do a check if a button release was only a click
         * Increase the value of the knob
         * AnimationTimer is also stopped
         */
        private void plusButtonReleased(MouseEvent event) {
            itsAPlusHold = false;
            if((System.currentTimeMillis() - currentTime)<200)
                increaseValue(1);
            animationTimer.stop();
        }

        /**
         * Do a check if a button release was only a click
         * Decrease the value of the knob
         * AnimationTimer is also stopped
         */
        private void minusButtonReleased(MouseEvent event) {
            itsAMinusHold = false;
            if((System.currentTimeMillis() - currentTime)<200)
                decreaseValue(1);
            animationTimer.stop();
        }

        /**
         * Start the animation timer for pressing the plus button
         * @param event the press that triggered this method
         */
        private void plusButtonPressed(MouseEvent event) {
            itsAPlusHold = true;
            currentTime = System.currentTimeMillis();
            animationTimer.start();
        }

        /**
         * Start the animation timer for pressing the minus button
         * @param event the press that triggered this method
         */
        private void minusButtonPressed(MouseEvent event) {
            itsAMinusHold = true;
            currentTime = System.currentTimeMillis();
            animationTimer.start();
        }


        /**
         * Decrease the knob's value
         */
        private void decreaseValue(double dec) {
            if (getValue() > getMin())
                setValue(getValue() - dec);
        }

        /**
         * Increase the knob's value
         */
        private void increaseValue(double inc) {
            if (getValue() < getMax())
                setValue(getValue() + inc);
        }

        /**
         * @param size of the knob (radius)
         * Layout knob components relative to layout of overall knob
         */
        private void layoutComponents(double size) {
            baseArc.setLayoutX(knobCenterLayoutOffset);
            baseArc.setLayoutY(knobCenterLayoutOffset);
            valueArc.setLayoutX(knobCenterLayoutOffset);
            valueArc.setLayoutY(knobCenterLayoutOffset);
            knobBaseCircle.setLayoutX(knobCenterLayoutOffset);
            knobBaseCircle.setLayoutY(knobCenterLayoutOffset);
            plusButton.setLayoutX(knobCenterLayoutOffset + size - 15);
            plusButton.setLayoutY(knobCenterLayoutOffset + size - 15);
            minusButton.setLayoutX(knobCenterLayoutOffset - size - 10);
            minusButton.setLayoutY(knobCenterLayoutOffset + size - 15);
            valueLabel.setLayoutX(knobCenterLayoutOffset - size + 33);
            valueLabel.setLayoutY(knobCenterLayoutOffset + size + 5);

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

        /**
         * Set the knobs value unless disabled
         * @param value to set on the knob
         */
        public void setKnobValue(double value) {
            if (!isLocked()) {
                Knob.super.setValue(value);
                valueLabel.setText(String.format("%s: %.2f", getName(), value));
            } else valueLabel.setText("-");
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

        /**
         * Lock control of the knob
         * @param lock whether to lock or unlock the knob
         */
        public void setLocked(boolean lock) {
            plusButton.setDisable(lock);
            minusButton.setDisable(lock);
            if (lock) valueLabel.setText(String.format("%s: %s", getName(), "N/A"));
            else if (isLocked()) valueLabel.setText(String.format("%s: %s", getName(), "0.00"));
        }
    }

    // TODO: Really need to refactor Knob, this is bad duplicated code

    /**
     * Lock control of the knob
     * @param lock whether to lock or unlock the knob
     */
    public void setLocked(boolean lock) {
        KnobSkin skin = (KnobSkin) getSkin();
        this.locked = lock;
        skin.setLocked(lock);
    }

    public void setKnobValue(double value) {
        KnobSkin skin = (KnobSkin) getSkin();
        skin.setKnobValue(value);
    }
}