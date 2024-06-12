package com.wecca.canoeanalysis.components.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Setter;
import lombok.Getter;

@Getter @Setter
public class Beam extends Group implements Graphic {

    // Fields
    private double startX;
    private double startY;
    private double width;
    private double thickness;

    private final Rectangle beam;
    private final Line topBorder;
    private final Line bottomBorder;
    private final Line leftBorder;
    private final Line rightBorder;

    private final static double defaultThickness = 1;
    private final static double borderExtension = 5; // Amount by which the borders extend beyond the beam

    // Constructor
    public Beam(double startX, double startY, double width, double thickness) {
        super();

        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.thickness = thickness;

        // Creating the beam rectangle
        beam = new Rectangle(startX, startY, width, thickness);
        beam.setFill(ColorPaletteService.getColor("above-surface"));

        // Creating the borders with extension
        topBorder = new Line(startX - borderExtension, startY, startX + width + borderExtension, startY);
        topBorder.setStrokeWidth(defaultThickness);
        topBorder.setStroke(ColorPaletteService.getColor("white"));

        bottomBorder = new Line(startX - borderExtension, startY + thickness, startX + width + borderExtension, startY + thickness);
        bottomBorder.setStrokeWidth(defaultThickness);
        bottomBorder.setStroke(ColorPaletteService.getColor("white"));

        // Creating the left and right borders
        leftBorder = new Line(startX, startY, startX, startY + thickness);
        leftBorder.setStrokeWidth(defaultThickness);
        leftBorder.setStroke(ColorPaletteService.getColor("white"));

        rightBorder = new Line(startX + width, startY, startX + width, startY + thickness);
        rightBorder.setStrokeWidth(defaultThickness);
        rightBorder.setStroke(ColorPaletteService.getColor("white"));

        // Adding elements to the group
        getChildren().addAll(beam, topBorder, bottomBorder, leftBorder, rightBorder);

        JFXDepthManager.setDepth(this, 4);
    }

    // Accessors
    @Override
    public double getX() { return startX; }

    // Change the color of the beam
    @Override
    public void recolor(Color color) {
        beam.setFill(color);
        topBorder.setStroke(ColorPaletteService.getColor("white"));
        bottomBorder.setStroke(ColorPaletteService.getColor("white"));
        leftBorder.setStroke(ColorPaletteService.getColor("white"));
        rightBorder.setStroke(ColorPaletteService.getColor("white"));
    }
}
