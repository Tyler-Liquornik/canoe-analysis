package com.wecca.canoeanalysis.graphics;
import com.wecca.canoeanalysis.utility.Positionable;
import javafx.scene.Group;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;

// Pinned support icons
public class SupportTriangle extends Group implements Colorable, Positionable
{
    // Fields
    private static final double defaultSideLength = 20.0;
    private double tipX;
    private double tipY;

    private Polygon triangle;
    private Line baseLine;

    // Constructor
    public SupportTriangle(double tipX, double tipY, double sideLength)
    {
        super();

        this.tipX = tipX;
        this.tipY = tipY;

        makeTriangle(tipX, tipY, sideLength);
    }

    // Simpler constructor with default side length
    public SupportTriangle(double tipX, double tipY)
    {
        this(tipX, tipY, defaultSideLength);
    }

    // Accessors

    @Override
    public double getX() {return getTipX();}
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

        // Create the triangle polygon
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
                tipX, tipY,
                x1, y,
                x2, y
        );
        triangle.setFill(lighten(Color.BLACK));
        triangle.setStroke(Color.BLACK);

        // Create the lines sticking out
        Line baseLine = new Line(x1 - sideLength / 2, y, x2 + sideLength / 2, y);

        // Set fields
        this.triangle = triangle;
        this.baseLine = baseLine;

        // Add the shapes to the group
        this.getChildren().addAll(triangle, baseLine);
    }

    // Get a lightened version of a color
    public Color lighten(Color color)
    {
        double red = color.getRed() + (1 - color.getRed()) * LIGHTEN_FACTOR;
        double green = color.getGreen() + (1 - color.getGreen()) * LIGHTEN_FACTOR;
        double blue = color.getBlue() + (1 - color.getBlue()) * LIGHTEN_FACTOR;
        return new Color(red, green, blue, color.getOpacity());
    }

    // Change the color of the support triangle
    @Override
    public void recolor(Color color) {
        baseLine.setStroke(color);
        triangle.setStroke(color);
        triangle.setFill(lighten(color));
    }
}
