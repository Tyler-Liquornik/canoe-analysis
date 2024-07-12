package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Icon used for the canoe
 * Same as curve but with an extra line that closes the area
 */
public class ClosedCurve extends Curve {

    private Line closingLine;

    public ClosedCurve(UnivariateFunction curve, Section section, double startX, double endX, double startY, double endY) {
        super(curve, section, startX, endX, startY, endY);
        draw();
    }

    public void draw() {
        super.draw();
        double closingY = isNonNegative() ? endY : startY;

        // Create and add the closing line
        closingLine = new Line(startX, closingY, endX, closingY);
        closingLine.setStroke(ColorPaletteService.getColor("white"));

        // Add the closing line to the children of the Group
        getChildren().add(closingLine);
    }

    @Override
    public void recolor(boolean setColored) {
        super.recolor(setColored);

        Color lineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        closingLine.setStroke(lineColor);
    }

    /**
     * The "height" of the function is really the distance to the y-axis (so height > 0 still for a non-positive curve)
     * @param x the x value to get the height at
     */
    public double getHeight(double x) {
        double valueAtX = curve.value(x);
        double maxValue = CalculusUtils.getMaxSignedValue(curve, section);
        double minValue = CalculusUtils.getMaxOrMinValue(curve, section, false);
        double rangeY = endY - startY;
        double valueRange = maxValue - minValue;
        double scaledValue = (valueAtX - minValue) / valueRange * rangeY;
        return Math.abs(scaledValue);
    }
}

