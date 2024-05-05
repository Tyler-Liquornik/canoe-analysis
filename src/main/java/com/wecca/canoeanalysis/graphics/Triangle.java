package com.wecca.canoeanalysis.graphics;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

// Supports are to be represented as upright equilateral triangles
public class Triangle extends Path
{
    // Fields
    private static final double defaultSideLength = 20.0;
    private double tipX;
    private double tipY;


    // Constructor
    public Triangle(double topX, double topY, double sideLength)
    {
        super();

        this.tipX = topX;
        this.tipY = topY;

        strokeProperty().bind(fillProperty());
        setFill(Color.BLACK);

        makeTriangle(topX, topY, sideLength);
    }

    // Simpler constructor with default side length
    public Triangle(double tipX, double tipY)
    {
        this(tipX, tipY, defaultSideLength);
    }

    // Accessors
    public double getTipX() {return tipX;}
    public double getTipY() {return tipY;}

    // Mutators
    public void setTipX(double tipX) {this.tipX = tipX;}
    public void setTipY(double tipY) {this.tipY = tipY;}

    // Create the triangle
    private void makeTriangle(double tipX, double tipY, double sideLength)
    {
        // Bottom left point and bottom right points on the triangle (they are on the same y coordinate)
        double x1 = -0.5 * sideLength + tipX;
        double x2 = 0.5 * sideLength + tipX;
        double y = (Math.sqrt(3) / 2) * sideLength + tipY;

        // Create triangle
        getElements().add(new MoveTo(tipX, tipY));
        getElements().add(new LineTo(x1, y));
        getElements().add(new LineTo(x2, y));
        getElements().add(new LineTo(tipX, tipY));
    }
}
