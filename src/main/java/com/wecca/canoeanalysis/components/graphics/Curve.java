package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.List;

/**
 * Icon used for piecewise continuous load distributions
 * A curve with shaded area between the curve and the y-axis
 */
@Getter @Setter
public class Curve extends Group implements Graphic {

    protected UnivariateFunction function;
    protected Section section;
    protected double startX;
    protected double endX;
    protected double startY;
    protected double endY;
    protected Path linePath;
    protected Polygon area;
    protected boolean isColored;

    public Curve(UnivariateFunction function, Section section, double startX, double endX, double startY, double endY) {
        super();
        this.function = function;
        this.section = section;
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;

        CalculusUtils.validatePiecewiseAsUpOrDown(List.of(function), List.of(section));
        draw();
        JFXDepthManager.setDepth(this, 4);
    }

    public void draw() {
        int numSamples = 1000;
        double step = (section.getRx() - section.getX()) / numSamples;
        double currentX = section.getX();
        double rangeY = endY - startY;

        // Find the min and max values of the function in the range [section.getX(), section.getRx()] for scaling based on samples
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= numSamples; i++) {
            double value = function.value(section.getX() + i * step);
            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
        }

        double valueRange = maxValue - minValue;

        // Flip the area of the curve so that it always fills in its area towards  y = 0
        double initialPathY = startY + ((getEffectiveFunction().value(currentX) - minValue) / valueRange) * rangeY;
        double initialPathX = startX + ((currentX - section.getX()) / (section.getRx() - section.getX())) * (endX - startX);

        linePath = new Path();
        linePath.getElements().add(new MoveTo(initialPathX, initialPathY));

        area = new Polygon();
        area.getPoints().addAll(initialPathX, initialPathY);

        for (int i = 1; i <= numSamples; i++) {
            currentX = section.getX() + i * step;
            double scaledY = startY + ((getEffectiveFunction().value(currentX) - minValue) / valueRange) * rangeY;
            double scaledX = startX + ((currentX - section.getX()) / (section.getRx() - section.getX())) * (endX - startX);
            linePath.getElements().add(new LineTo(scaledX, scaledY));
            area.getPoints().addAll(scaledX, scaledY);
        }

        area.getPoints().addAll(endX, initialPathY);
        area.getPoints().addAll(startX, initialPathY);
        area.setFill(ColorPaletteService.getColor("above-surface"));
        linePath.setStroke(ColorPaletteService.getColor("white"));

        getChildren().addAll(area, linePath);

        this.isColored = false;
        ColorManagerService.registerInColorPalette(this);
    }

    @Override
    public double getX() {
        return startX;
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

    protected UnivariateFunction getEffectiveFunction() {
        return isNonNegative() ? x -> -function.value(x) + CalculusUtils.getMaxSignedValue(function, section) : function;
    }
}
