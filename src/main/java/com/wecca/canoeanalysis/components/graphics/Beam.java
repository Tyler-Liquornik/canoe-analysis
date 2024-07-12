package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Icon used for an unset/simplified canoe hull
 */
@Getter @Setter
public class Beam extends Group implements Graphic, CurvedProfile {

    private double startX;
    private double startY;
    private double length;
    private double height;
    private Rectangle beam;
    private Line topBorder;
    private Line bottomBorder;
    private Line leftBorder;
    private Line rightBorder;
    private boolean isColored;
    private static final double borderExtension = 5; // Amount by which the borders extend beyond the beam

    public Beam(double startX, double startY, double height, double length) {
        super();
        this.startX = startX;
        this.startY = startY;
        this.length = height;
        this.height = length;
        this.isColored = false;

        draw();
        JFXDepthManager.setDepth(this, 4);
        ColorManagerService.registerInColorPalette(this);
    }

    public void draw() {
        // Creating the beam rectangle
        beam = new Rectangle(startX, startY, length, height);
        beam.setFill(ColorPaletteService.getColor("above-surface"));

        // Creating the borders with extension
        topBorder = new Line(startX - borderExtension, startY, startX + length + borderExtension, startY);
        topBorder.setStroke(ColorPaletteService.getColor("white"));
        bottomBorder = new Line(startX - borderExtension, startY + height, startX + length + borderExtension, startY + height);
        bottomBorder.setStroke(ColorPaletteService.getColor("white"));

        // Creating the left and right borders
        leftBorder = new Line(startX, startY, startX, startY + height);
        leftBorder.setStroke(ColorPaletteService.getColor("white"));
        rightBorder = new Line(startX + length, startY, startX + length, startY + height);
        rightBorder.setStroke(ColorPaletteService.getColor("white"));

        // Adding elements to the group
        getChildren().addAll(beam, topBorder, bottomBorder, leftBorder, rightBorder);
    }

    @Override
    public double getX() {
        return startX;
    }

    @Override
    public void recolor(boolean setColored) {
        this.isColored = setColored;

        Color outlineColor = setColored ? ColorPaletteService.getColor("primary") : ColorPaletteService.getColor("white");
        Color fillColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("above-surface");

        beam.setFill(fillColor);
        topBorder.setStroke(outlineColor);
        bottomBorder.setStroke(outlineColor);
        leftBorder.setStroke(outlineColor);
        rightBorder.setStroke(outlineColor);
    }

    @Override
    public Section getSection() {
        return new Section(startX, startX + length);
    }

    @Override
    public UnivariateFunction getFunction() {
        return x -> 0;
    }

    @Override
    public double getHeight(double x) {
        if (x < startX || x > startX + length)
            throw new IllegalArgumentException("Cannot get height at x = " + x + ", out of bounds");
        return height;
    }
}