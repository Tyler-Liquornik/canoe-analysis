package com.wecca.canoeanalysis;

import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

// Modified from:
// Source: https://gist.github.com/kn0412/2086581e98a32c8dfa1f69772f14bca4


public class Arrow extends Path
{
    // Fields
    private static final double defaultArrowHeadSize = 5.0;

    private double startX;
    private double startY;
    private double endX;
    private double endY;

    public Arrow(double startX, double startY, double endX, double endY, double arrowHeadSize)
    {
        super();

        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;

        strokeProperty().bind(fillProperty());
        setFill(Color.BLACK);

        makeArrow(startX, startY, endX, endY, arrowHeadSize);
    }

    public Arrow(double startX, double startY, double endX, double endY){
        this(startX, startY, endX, endY, defaultArrowHeadSize);
    }

    public void makeArrow(double startX, double startY, double endX, double endY, double arrowHeadSize)
    {
        // Line
        getElements().add(new MoveTo(startX, startY));
        getElements().add(new LineTo(endX, endY));

        // Arrow Head
        double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Left point
        double x1 = (- 1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y1 = (- 1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;
        // Right Point
        double x2 = (1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y2 = (1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;

        // Close arrow head triangle
        getElements().add(new LineTo(x1, y1));
        getElements().add(new LineTo(x2, y2));
        getElements().add(new LineTo(endX, endY));
    }

    // Accessors
    public double getStartX() {return startX;}
    public double getStartY() {return startY;}
    public double getEndX() {return endX;}
    public double getEndY() {return endY;}
}