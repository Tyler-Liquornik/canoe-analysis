package com.wecca.canoeanalysis.components.graphics.hull;

import com.wecca.canoeanalysis.components.graphics.CurvedGraphic;
import com.wecca.canoeanalysis.components.graphics.FunctionGraphic;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

/**
 * Icon used for the canoe hull
 * Same as curve but with an extra line that closes off the area
 */
@Getter @Setter
public class HullGraphic extends Group implements FunctionGraphic {

    private Line closingTopLine;
    private CurvedGraphic curvedGraphic;
    private boolean isColored;

    /**
     * Deals with mapping between function space and graphic space
     * @param function the function definition in function space
     * @param section the interval of the function in function space
     * @param encasingRectangle the smallest region in function space that encloses all points in the function
     *                          is mapped to this rectangle in graphics space
     */
    public HullGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle) {
        this(function, section, encasingRectangle, false);
    }

    /**
     * @param useUnimodalOptimization, an extra flag to optimize the function faster for quick renders which only works for unimodal functions
     *                                 it is up to the implementer to understand if the function is unimodal as the cost to validate for this makes the optimization it provides redundant!
     *                                 logically, a hull shape should probably almost always meet these criteria anyway!
     */
    public HullGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle, boolean useUnimodalOptimization) {
        this.closingTopLine = new Line(encasingRectangle.getX(), encasingRectangle.getY(), encasingRectangle.getX() + encasingRectangle.getWidth(), encasingRectangle.getY());
        this.curvedGraphic = new CurvedGraphic(function, section, encasingRectangle, true, useUnimodalOptimization);
        draw();
    }

    public void draw() {
        closingTopLine.setStroke(ColorPaletteService.getColor("white"));
        closingTopLine.setStrokeWidth(3);
        getChildren().addAll(closingTopLine, curvedGraphic);
    }

    @Override
    public void recolor(boolean setColored) {
        Color lineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        closingTopLine.setStroke(lineColor);
        curvedGraphic.recolor(setColored);
        this.isColored = setColored;
    }

    @Override
    public double getX() {
        return curvedGraphic.getX();
    }

    @Override
    public Section getSection() {
        return curvedGraphic.getSection();
    }

    @Override
    public BoundedUnivariateFunction getFunction() {
        return curvedGraphic.getFunction();
    }

    @Override
    public double getEndX() {
        return curvedGraphic.getEndX();
    }

    @Override
    public double getY() {
        return curvedGraphic.getY();
    }

    @Override
    public double getEndY() {
        return curvedGraphic.getEndY();
    }

    @Override
    public Rectangle getEncasingRectangle() {
        return curvedGraphic.getEncasingRectangle();
    }

    @Override
    public double getHeight(double functionX) {
        return curvedGraphic.getHeight(functionX);
    }
}
