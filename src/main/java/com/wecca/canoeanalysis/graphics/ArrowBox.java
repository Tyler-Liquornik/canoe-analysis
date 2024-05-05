package com.wecca.canoeanalysis.graphics;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class ArrowBox extends Group
{
    private double lX;
    private double rX;
    private double startY;
    private double endY;

    private Arrow lArrow;
    private Arrow rArrow;
    private Rectangle box;

    private Line borderLine;

    public ArrowBox(double lX, double startY, double rX, double endY)
    {
        super();

        // Arrows at left and right bound the rectangle
        lArrow = new Arrow(lX, startY, lX, endY);
        rArrow = new Arrow(rX, startY, rX, endY);

        // Box depends on if this is an upwards or downwards facing ArrowBox
        // Extra pixels to fill weird whitespaces
        if (startY < endY)
            box = new Rectangle(lX, startY, rX - lX, endY - startY + 2);
        else
            box = new Rectangle(lX, endY - 2, rX - lX, startY - endY + 2);

        box.setFill(Color.LIGHTGREY);
        borderLine = new Line(lX, startY, rX, startY);

        getChildren().addAll(box, lArrow, rArrow, borderLine);
    }

    public double getLX() {return lX;}
    public double getRX() {return rX;}
    public double getStartY() {return startY;}
    public double getEndY() {return endY;}
    public Arrow getLArrow() {return lArrow;}
    public Arrow getRArrow() {return rArrow;}
    public Rectangle getBox() {return box;}
    public Line getBorderLine() {return borderLine;}
}
