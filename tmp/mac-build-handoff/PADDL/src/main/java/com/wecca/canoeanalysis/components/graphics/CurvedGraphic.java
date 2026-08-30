package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
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
import javafx.scene.shape.Rectangle;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Icon used for piecewise continuous load distributions
 * A curve with shaded area between the curve and the y-axis
 */
@Getter @Setter
public class CurvedGraphic extends Group implements FunctionGraphic {

    protected BoundedUnivariateFunction function;
    protected Section section;
    protected Rectangle encasingRectangle;
    protected Path linePath;
    protected Polygon area;
    protected boolean isColored;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    protected boolean isFilled;
    private final double maxSignedValue;

    /**
     * Deals with mapping between function space and graphic space
     * @param function the function definition in function space
     * @param section the interval of the function in function space
     * @param encasingRectangle the smallest region in function space that encloses all points in the function is mapped to this rectangle in graphics space
     */
    public CurvedGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle, boolean fillCurve) {
        this(function, section, encasingRectangle, fillCurve, false);
    }

    /**
     * @param useUnimodalOptimization, an extra flag to optimize the function faster for quick renders which only works for unimodal functions
     *                                 it is up to the implementer to understand if the function is unimodal as the cost to validate for this makes the optimization it provides redundant!
     */
    public CurvedGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle, boolean fillCurve, boolean useUnimodalOptimization) {
        super();
        this.function = function;
        this.section = section;
        this.encasingRectangle = encasingRectangle;
        this.isColored = false;
        this.isFilled = fillCurve;
        this.maxSignedValue = useUnimodalOptimization ? function.getMaxSignedValueUnimodal(section) : function.getMaxSignedValue(section);

        draw();
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        // Partition the section
        int numSamples = 200;
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
            currentX = CalculusUtils.roundXDecimalDigits((section.getX() + i * step), 10);
            double scaledY = encasingRectangle.getY() + ((effectiveFunction.value(currentX) - minValue) / valueRange) * encasingRectangle.getHeight();
            double scaledX = encasingRectangle.getX() + ((currentX - section.getX()) / (section.getRx() - section.getX())) * encasingRectangle.getWidth();
            linePath.getElements().add(new LineTo(scaledX, scaledY));
            area.getPoints().addAll(scaledX, scaledY);
        }

        // Close off the area and style
        area.getPoints().addAll(encasingRectangle.getX() + encasingRectangle.getWidth(), initialPathY);
        area.getPoints().addAll(encasingRectangle.getX(), initialPathY);
        if (isFilled) area.setFill(ColorPaletteService.getColor("above-surface"));
        else area.setFill(ColorPaletteService.getColor("above-surface").deriveColor(0, 1, 1, 0));
        linePath.setStroke(ColorPaletteService.getColor("white"));
        linePath.setStrokeWidth(1.5);

        // Group the elements
        getChildren().addAll(area, linePath);
    }

    @Override
    public double getX() {
        return encasingRectangle.getX();
    }

    @Override
    public double getY() {
        return encasingRectangle.getY();
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color lineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        Color areaColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("above-surface");

        linePath.setStroke(lineColor);
        if (isFilled) area.setFill(areaColor);
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
    public Section getSection() {
        return section;
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
