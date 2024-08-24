package com.wecca.canoeanalysis.components.graphics.hull;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.components.graphics.FunctionGraphic;
import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

/**
 * Icon used for an unset/simplified canoe hull
 * Technically a "curve" that it just flat (the function is y = c for some constant c)
 */
@Getter @Setter
public class BeamHullGraphic extends Group implements FunctionGraphic {

    private Rectangle encasingRectangle;
    private Line topBorder;
    private Line bottomBorder;
    private Line leftBorder;
    private Line rightBorder;
    private boolean isColored;
    private static final double borderExtension = 5; // Amount by which the borders extend beyond the beam

    /**
     * @param rectangle the graphics space coordinates of the beam rectangle
     * Note that the border extensions do stick out of this region
     */
    public BeamHullGraphic(Rectangle rectangle) {
        super();
        this.encasingRectangle = rectangle;
        this.isColored = false;

        draw();
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        // Creating the beam rectangle
        encasingRectangle.setFill(ColorPaletteService.getColor("above-surface"));

        // Creating the borders with extension
        topBorder = new Line(encasingRectangle.getX() - borderExtension, encasingRectangle.getY(), encasingRectangle.getX() + encasingRectangle.getWidth() + borderExtension, encasingRectangle.getY());
        topBorder.setStroke(ColorPaletteService.getColor("white"));
        bottomBorder = new Line(encasingRectangle.getX() - borderExtension, encasingRectangle.getY() + encasingRectangle.getHeight(), encasingRectangle.getX() + encasingRectangle.getWidth() + borderExtension, encasingRectangle.getY() + encasingRectangle.getHeight());
        bottomBorder.setStroke(ColorPaletteService.getColor("white"));

        // Creating the left and right borders
        leftBorder = new Line(encasingRectangle.getX(), encasingRectangle.getY(), encasingRectangle.getX(), encasingRectangle.getY() + encasingRectangle.getHeight());
        leftBorder.setStroke(ColorPaletteService.getColor("white"));
        rightBorder = new Line(encasingRectangle.getX() + encasingRectangle.getWidth(), encasingRectangle.getY(), encasingRectangle.getX() + encasingRectangle.getWidth(), encasingRectangle.getY() + encasingRectangle.getHeight());
        rightBorder.setStroke(ColorPaletteService.getColor("white"));

        // Adding elements to the group
        getChildren().addAll(encasingRectangle, topBorder, bottomBorder, leftBorder, rightBorder);
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

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        Color fillColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("above-surface");

        encasingRectangle.setFill(fillColor);
        topBorder.setStroke(outlineColor);
        bottomBorder.setStroke(outlineColor);
        leftBorder.setStroke(outlineColor);
        rightBorder.setStroke(outlineColor);
    }

    @Override
    public Section getSection() {
        return new Section(encasingRectangle.getX(), encasingRectangle.getX() + encasingRectangle.getWidth());
    }

    @Override
    public BoundedUnivariateFunction getFunction() {
        return x -> 0;
    }

    @Override
    public double getHeight(double functionX) {
        if (functionX < encasingRectangle.getX() || functionX > encasingRectangle.getX() + encasingRectangle.getWidth())
            throw new IllegalArgumentException("Cannot get rectangle.getHeight() at x = " + functionX + ", out of bounds");
        return encasingRectangle.getHeight();
    }

    public double getEndX() {
        return encasingRectangle.getX() + encasingRectangle.getWidth();
    }

    public double getEndY() {
        return  encasingRectangle.getY() + encasingRectangle.getHeight();
    }
}