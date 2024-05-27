package com.wecca.canoeanalysis.graphics;

import com.jfoenix.effects.JFXDepthManager;
import com.wecca.canoeanalysis.utility.Positionable;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class Beam extends Group implements Positionable, Colorable {

    // Fields
    private double startX;
    private double startY;
    private double width;
    private double thickness;

    private Rectangle beam;
    private Line topBorder;
    private Line bottomBorder;
    private Line leftBorder;
    private Line rightBorder;

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
        beam.setFill(ColorPalette.ABOVE_SURFACE.getColor());

        // Creating the borders with extension
        topBorder = new Line(startX - borderExtension, startY, startX + width + borderExtension, startY);
        topBorder.setStrokeWidth(defaultThickness);
        topBorder.setStroke(ColorPalette.ICON.getColor());

        bottomBorder = new Line(startX - borderExtension, startY + thickness, startX + width + borderExtension, startY + thickness);
        bottomBorder.setStrokeWidth(defaultThickness);
        bottomBorder.setStroke(ColorPalette.ICON.getColor());

        // Creating the left and right borders
        leftBorder = new Line(startX, startY, startX, startY + thickness);
        leftBorder.setStrokeWidth(defaultThickness);
        leftBorder.setStroke(ColorPalette.ICON.getColor());

        rightBorder = new Line(startX + width, startY, startX + width, startY + thickness);
        rightBorder.setStrokeWidth(defaultThickness);
        rightBorder.setStroke(ColorPalette.ICON.getColor());

        // Adding elements to the group
        getChildren().addAll(beam, topBorder, bottomBorder, leftBorder, rightBorder);

        JFXDepthManager.setDepth(this, 4);
    }

    // Accessors
    @Override
    public double getX() { return startX; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getWidth() { return width; }
    public double getThickness() { return thickness; }

    // Mutators
    public void setStartX(double startX) { this.startX = startX; }
    public void setStartY(double startY) { this.startY = startY; }
    public void setWidth(double width) { this.width = width; }
    public void setThickness(double thickness) { this.thickness = thickness; }

    // Change the color of the beam
    @Override
    public void recolor(Color color) {
        beam.setFill(color);
        topBorder.setStroke(ColorPalette.ICON.getColor());
        bottomBorder.setStroke(ColorPalette.ICON.getColor());
        leftBorder.setStroke(ColorPalette.ICON.getColor());
        rightBorder.setStroke(ColorPalette.ICON.getColor());
    }
}
