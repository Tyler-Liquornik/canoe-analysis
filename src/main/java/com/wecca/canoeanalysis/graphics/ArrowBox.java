package com.wecca.canoeanalysis.graphics;

import com.wecca.canoeanalysis.utility.Positionable;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

// Uniform distributed loads are to be represented as "arrow boxes"
public class ArrowBox extends Group implements Colorable, Positionable
{
    // Fields
    private double lX;
    private double rX;
    private double startY;
    private double endY;

    private Arrow lArrow;
    private Arrow rArrow;
    private Rectangle box;
    private Line borderLine;

    // Constructor
    public ArrowBox(double lX, double startY, double rX, double endY)
    {
        super();

        this.lX = lX;
        this.rX = rX;
        this.startY = startY;
        this.endY = endY;

        // Arrows at left and right bound the rectangle
        lArrow = new Arrow(lX, startY, lX, endY);
        rArrow = new Arrow(rX, startY, rX, endY);

        // Box depends on if this is an upwards or downwards facing ArrowBox
        // Extra pixels to fill weird whitespaces
        if (startY < endY)
            box = new Rectangle(lX, startY, rX - lX, endY - startY + 2);
        else
            box = new Rectangle(lX, endY - 2, rX - lX, startY - endY + 2);

        box.setFill(lighten(Color.BLACK));
        borderLine = new Line(lX, startY, rX, startY);

        getChildren().addAll(box, lArrow, rArrow, borderLine);
    }

    // Accessors

    @Override
    public double getX() {return getLX();}
    public double getLX() {return lX;}
    public double getRX() {return rX;}
    public double getStartY() {return startY;}
    public double getEndY() {return endY;}

    // Mutators
    public void setlX(double lX) {this.lX = lX;}
    public void setrX(double rX) {this.rX = rX;}
    public void setStartY(double startY) {this.startY = startY;}
    public void setEndY(double endY) {this.endY = endY;}

    // Get a lightened version of a color
    public Color lighten(Color color)
    {
        double red = color.getRed() + (1 - color.getRed()) * LIGHTEN_FACTOR;
        double green = color.getGreen() + (1 - color.getGreen()) * LIGHTEN_FACTOR;
        double blue = color.getBlue() + (1 - color.getBlue()) * LIGHTEN_FACTOR;
        return new Color(red, green, blue, color.getOpacity());
    }

    // Change the color of the arrow box
    @Override
    public void recolor(Color color) {
        lArrow.setFill(color);
        rArrow.setFill(color);
        borderLine.setStroke(color);
        box.setFill(lighten(color));
    }
}
