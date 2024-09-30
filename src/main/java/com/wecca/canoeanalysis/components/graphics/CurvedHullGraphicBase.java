package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.FunctionSection;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

import java.util.List;

/**
 * Icon used for piecewise continuous load distributions
 * A curve with shaded area between the curve and the y-axis
 */
@Getter
public class CurvedHullGraphicBase extends Group implements HullGraphic {

    protected BoundedUnivariateFunction function;
    protected FunctionSection section;
    protected Rectangle encasingRectangle;
    protected Path linePath;
    protected Polygon area;
    protected boolean isColored;
    private final double maxSignedValue;

    public CurvedHullGraphicBase(BoundedUnivariateFunction function, FunctionSection section, Rectangle encasingRectangle) {
        super();
        this.function = function;
        this.section = section;
        this.encasingRectangle = encasingRectangle;
        this.maxSignedValue = function.getMaxSignedValue(section); // precalculated to save time fetching
        this.isColored = false;

        CalculusUtils.validatePiecewiseAsUpOrDown(List.of(function), List.of(section));
        draw();
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        // Partition the section
        int numSamples = 1000;
        double step = (section.getRx() - section.getX()) / numSamples;
        double currentX = section.getX();

        // Flip the area of the curve so that it always fills in its area towards  y = 0
        BoundedUnivariateFunction effectiveFunction = getEffectiveFunction();
        double minValue = effectiveFunction.getMinValue(section);
        double valueRange = effectiveFunction.getMaxValue(section) - minValue;
        double initialPathY = encasingRectangle.getY() + ((effectiveFunction.value(currentX) - minValue) / valueRange) * encasingRectangle.getHeight();
        double initialPathX = encasingRectangle.getX() + ((currentX - section.getX()) / (section.getLength())) * encasingRectangle.getWidth();

        // Set up
        linePath = new Path();
        linePath.getElements().add(new MoveTo(initialPathX, initialPathY));
        area = new Polygon();
        area.getPoints().addAll(initialPathX, initialPathY);

        // Build the curve and contained area
        for (int i = 1; i <= numSamples; i++) {
            currentX = section.getX() + i * step;
            double scaledY = encasingRectangle.getY() + ((effectiveFunction.value(currentX) - minValue) / valueRange) * encasingRectangle.getHeight();
            double scaledX = encasingRectangle.getX() + ((currentX - section.getX()) / (section.getRx() - section.getX())) * encasingRectangle.getWidth();
            linePath.getElements().add(new LineTo(scaledX, scaledY));
            area.getPoints().addAll(scaledX, scaledY);
        }

        // Close off the area and style
        area.getPoints().addAll(encasingRectangle.getX() + encasingRectangle.getWidth(), initialPathY);
        area.getPoints().addAll(encasingRectangle.getX(), initialPathY);
        area.setFill(ColorPaletteService.getColor("above-surface"));
        linePath.setStroke(ColorPaletteService.getColor("white"));

        // Group the elements
        getChildren().addAll(area, linePath);
    }

    @Override
    public double getX() {
        return encasingRectangle.getX();
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color lineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        Color areaColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("above-surface");

        linePath.setStroke(lineColor);
        area.setFill(areaColor);
    }

    protected boolean isNonNegative() {
        return function.value((section.getX() + section.getRx()) / 2) >= 0;
    }

    protected BoundedUnivariateFunction getEffectiveFunction() {
        return isNonNegative() ? x -> maxSignedValue - function.value(x) : function;
    }

    /**
     * The "height" of the function is really the distance to the y-axis (so height > 0 still for a non-positive curve)
     * @param functionX the x value to get the height at
     * Note that the input x here is in the function domain, the output y is in the scaled graphic domain
     */
    @Override
    public double getHeight(double functionX) {
        double valueAtX = getEffectiveFunction().value(functionX);
        double maxValue = function.getMaxPoint(section).getY();
        double minValue = function.getMinPoint(section).getY();

        // Scale the value based on the rendered height
        double valueRange = maxValue - minValue;
        double scaledValue = (valueAtX - minValue) / valueRange * encasingRectangle.getHeight();

        return Math.abs(scaledValue);
    }

    @Override
    public FunctionSection getSection() {
        return section;
    }

    @Override
    public BoundedUnivariateFunction getFunction() {
        return function;
    }

    @Override
    public double getEndX() {
        return encasingRectangle.getX() + encasingRectangle.getWidth();
    }

    @Override
    public double getEndY() {
        return encasingRectangle.getY() + encasingRectangle.getHeight();
    }
}
