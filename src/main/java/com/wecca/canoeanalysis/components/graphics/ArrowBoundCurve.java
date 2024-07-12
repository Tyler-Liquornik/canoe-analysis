package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
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

    private double lX;
    private double rX;
    private double startY;
    private double endY;
    private Arrow lArrow;
    private Arrow rArrow;
    private Curve borderCurve;
    private boolean isColored;
    private Section section;

    public ArrowBoundCurve(UnivariateFunction function, Section section, double startX, double startY, double endX, double endY) {
        super();
        this.section = section;
        this.lX = startX;
        this.rX = endX;
        this.startY = startY;
        this.endY = endY;
        this.isColored = false;

        lArrow = new Arrow(lX, lX, startY, endY);
        rArrow = new Arrow(rX, rX, startY, endY);
        borderCurve = new Curve(function, section, lX, rX, startY, endY);
        draw();

        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        getChildren().addAll(borderCurve, lArrow, rArrow);
    }

    @Override
    public double getX() {
        return lX;
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
