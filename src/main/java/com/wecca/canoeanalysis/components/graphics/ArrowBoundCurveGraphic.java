package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

/**
 * Icon used for a uniformly distributed load over a curved surface
 */
@Getter @Setter
public class ArrowBoundCurveGraphic extends Group implements Graphic {
    private ArrowGraphic lArrow;
    private ArrowGraphic rArrow;
    private CurvedHullGraphicBase borderCurve;
    private boolean isColored;
    private Section section;

    public ArrowBoundCurveGraphic(BoundedUnivariateFunction function, Section section, Rectangle encasingRectangle, ArrowGraphic lArrow, ArrowGraphic rArrow) {
        super();
        this.section = section;
        this.isColored = false;
        this.lArrow = lArrow;
        this.rArrow = rArrow;
        this.borderCurve = new CurvedHullGraphicBase(function, section, encasingRectangle);
        draw();

        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        getChildren().addAll(borderCurve, lArrow, rArrow);
    }

    @Override
    public double getX() {
        return lArrow.getX();
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");

        lArrow.setFill(outlineColor);
        rArrow.setFill(outlineColor);
        borderCurve.recolor(setColored);
    }
}
