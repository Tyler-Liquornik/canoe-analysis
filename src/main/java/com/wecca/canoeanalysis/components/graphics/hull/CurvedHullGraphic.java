package com.wecca.canoeanalysis.components.graphics.hull;

import com.wecca.canoeanalysis.components.graphics.CurvedGraphic;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
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
public class CurvedHullGraphic extends CurvedGraphic {

    private Line closingLine;

    /**
     * Deals with mapping between function space and graphic space
     * @param function the function definition in function space
     * @param section the interval of the function in function space
     * @param encasingRectangle the smallest region in function space that encloses all points in the function
     *                          is mapped to this rectangle in graphics space
     */
    public CurvedHullGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle) {
        super(function, section, encasingRectangle);
        draw();
    }

    public void draw() {
        super.draw();
        closingLine = new Line(encasingRectangle.getX(), encasingRectangle.getY(), encasingRectangle.getX() + encasingRectangle.getWidth(), encasingRectangle.getY());
        closingLine.setStroke(ColorPaletteService.getColor("white"));
        closingLine.setStrokeWidth(1.5);
        linePath.setStrokeWidth(1.5);
        getChildren().add(closingLine);
    }

    @Override
    public void recolor(boolean setColored) {
        super.recolor(setColored);

        Color lineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        closingLine.setStroke(lineColor);
    }
}
