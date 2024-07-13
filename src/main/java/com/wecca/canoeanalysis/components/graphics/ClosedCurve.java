package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Icon used for the canoe hull
 * Same as curve but with an extra line that closes the area
 */
@Getter @Setter
public class ClosedCurve extends Curve implements CurvedProfile {

    private Line closingLine;

    public ClosedCurve(BoundedUnivariateFunction function, Section section, double startX, double endX, double startY, double endY) {
        super(function, section, startX, endX, startY, endY);
        draw();
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        super.draw();
        closingLine = new Line(startX, startY, endX, startY);
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

