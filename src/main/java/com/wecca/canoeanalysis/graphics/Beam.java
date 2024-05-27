package com.wecca.canoeanalysis.graphics;

import com.wecca.canoeanalysis.utility.Positionable;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;

public class Beam extends Group implements Positionable, Colorable {

    // Fields
    private double startX;
    private double startY;
    private double width;
    private double thickness;

    private Rectangle beam;
    private Line topBorder;
    private Line bottomBorder;

    private final static double defaultThickness = 2;
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
        beam.setFill(Color.LIGHTGREY);

        // Creating the borders with extension
        topBorder = new Line(startX - borderExtension, startY, startX + width + borderExtension, startY);
        topBorder.setStrokeWidth(defaultThickness);
        topBorder.setStroke(Color.BLACK);

        bottomBorder = new Line(startX - borderExtension, startY + thickness, startX + width + borderExtension, startY + thickness);
        bottomBorder.setStrokeWidth(defaultThickness);
        bottomBorder.setStroke(Color.BLACK);

        // Adding elements to the group
        getChildren().addAll(beam, topBorder, bottomBorder);
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
        topBorder.setStroke(Color.BLACK);
        bottomBorder.setStroke(Color.BLACK);
    }
}
