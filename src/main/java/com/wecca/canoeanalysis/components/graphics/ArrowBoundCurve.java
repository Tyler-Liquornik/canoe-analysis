package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.List;

/**
 * Icon used for a uniformly distributed load over a curved surface
 */
@Getter @Setter
public class ArrowBoundCurve extends Group implements Graphic {
    private Arrow lArrow;
    private Arrow rArrow;
    private Curve borderCurve;
    private boolean isColored;
    private Section section;

    public ArrowBoundCurve(BoundedUnivariateFunction function, Section section, Arrow lArrow, Arrow rArrow) {
        super();
        this.section = section;
        this.isColored = false;
        this.lArrow = lArrow;
        this.rArrow = rArrow;
        borderCurve = new Curve(function, section, lArrow.getX(), rArrow.getX(), lArrow.getStartY(), rArrow.getEndY());
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
