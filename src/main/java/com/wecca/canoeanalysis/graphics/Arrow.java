package com.wecca.canoeanalysis.graphics;

import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import com.wecca.canoeanalysis.utility.Positionable;

public class Arrow extends Path implements Colorable, Positionable {

    // Fields
    private static final double defaultArrowHeadSize = 10;
    private static final double defaultThickness = 2;

    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private double thickness;

    public Arrow(double startX, double startY, double endX, double endY, double arrowHeadSize, double thickness) {
        super();

        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.thickness = thickness;

        strokeProperty().bind(fillProperty());
        setFill(ColorPalette.ICON.getColor());

        makeArrow(startX, startY, endX, endY, arrowHeadSize);
    }

    public Arrow(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, defaultArrowHeadSize, defaultThickness);
    }

    private void makeArrow(double startX, double startY, double endX, double endY, double arrowHeadSize) {
        // Line
        setStrokeWidth(thickness);
        getElements().add(new MoveTo(startX, startY));
        getElements().add(new LineTo(endX, endY));

        // Arrow Head
        double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Adjust arrow head size for a less bloated appearance
        arrowHeadSize *= 0.6;

        // Left point
        double x1 = (-0.5 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y1 = (-0.5 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;
        // Right Point
        double x2 = (0.5 * cos + Math.sqrt(3) / 2 * sin) * arrowHeadSize + endX;
        double y2 = (0.5 * sin - Math.sqrt(3) / 2 * cos) * arrowHeadSize + endY;

        // Close arrow head triangle
        getElements().add(new LineTo(x1, y1));
        getElements().add(new LineTo(x2, y2));
        getElements().add(new LineTo(endX, endY));
    }

    // Accessors
    @Override
    public double getX() {
        return Math.min(startX, endX); // Leftmost point is considered
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    // Mutators
    public void setStartX(double startX) {
        this.startX = startX;
    }

    public void setStartY(double startY) {
        this.startY = startY;
    }

    public void setEndX(double endX) {
        this.endX = endX;
    }

    public void setEndY(double endY) {
        this.endY = endY;
    }

    @Override
    public void recolor(Color color) {
        setFill(color);
    }
}
