package com.wecca.canoeanalysis.components.graphics.hull;

import com.wecca.canoeanalysis.components.graphics.Graphic;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import lombok.Getter;
import lombok.Setter;

/**
 * A graphical component for displaying the slope at Bézier spline join points.
 * Can highlight only one half of the slope within either the left or right section.
 */
@Getter @Setter
public class BezierTangentGraphic extends Group implements Graphic {

    private Point2D centerPoint;
    private Point2D lControlPoint;
    private Point2D rControlPoint;

    private BezierPointGraphic tangentPointGraphic;
    private BezierPointGraphic lControlPointGraphic;
    private BezierPointGraphic rControlPointGraphic;

    private Line lineToLControl;
    private Line lineToRControl;

    private boolean colored;
    private boolean isLeftColored;

    /**
     * @param centerPoint The main point representing the center of the slope.
     * @param lControlPoint The left control point (can be null).
     * @param rControlPoint The right control point (can be null).
     * Note that lControlPoint and rControlPoint cannot both be null.
     */
    public BezierTangentGraphic(Point2D centerPoint, Point2D lControlPoint, Point2D rControlPoint) {
        this.centerPoint = centerPoint;
        this.lControlPoint = lControlPoint;
        this.rControlPoint = rControlPoint;
        this.colored = false;
        this.isLeftColored = false;

        // Validation
        if (lControlPoint == null && rControlPoint == null)
            throw new IllegalArgumentException("At least one of the left or right control points must be specified.");
        validateSlope();

        ColorManagerService.registerInColorPalette(this);
        draw();
    }

    /**
     * Validates that the slopes from the main point to the left and right control points (if present) are equal.
     * This is a requirement for C1 smoothness in the model and the graphic should reflect that.
     * Note that if only one control point is present, it's valid by default.
     */
    private void validateSlope() {
        if (lControlPoint != null && rControlPoint != null) {
            double leftSlope = (lControlPoint.getY() - centerPoint.getY()) / (lControlPoint.getX() - centerPoint.getX());
            double rightSlope = (rControlPoint.getY() - centerPoint.getY()) / (rControlPoint.getX() - centerPoint.getX());
            if (Math.abs(leftSlope - rightSlope) > 1e-6)
                throw new IllegalArgumentException("Left slope " + leftSlope + " and right slope " + rightSlope +
                        " are not equal, C1 smoothness violated");
        }
    }

    @Override
    public void draw() {
        // Create the center point circle
        tangentPointGraphic = new BezierPointGraphic(centerPoint.getX(), centerPoint.getY(), 5);

        // Create the lines to the control points for whichever exist
        // +1 shifts graphics misalignment from points getY
        if (lControlPoint != null) {
            lineToLControl = new Line(centerPoint.getX(), centerPoint.getY() + 1, lControlPoint.getX(), lControlPoint.getY() + 1);
            lineToLControl.setStroke(ColorPaletteService.getColor("white"));
            lineToLControl.setStrokeWidth(1.5);
            lineToLControl.getStrokeDashArray().addAll(3.0, 3.0);
            lControlPointGraphic = new BezierPointGraphic(lControlPoint.getX(), lControlPoint.getY(), 4);
            this.getChildren().addAll(lineToLControl, lControlPointGraphic);
        }

        if (rControlPoint != null) {
            lineToRControl = new Line(centerPoint.getX(), centerPoint.getY() + 1, rControlPoint.getX(), rControlPoint.getY() + 1);
            lineToRControl.setStroke(ColorPaletteService.getColor("white"));
            lineToRControl.setStrokeWidth(1.5);
            lineToRControl.getStrokeDashArray().addAll(3.0, 3.0);
            rControlPointGraphic = new BezierPointGraphic(rControlPoint.getX(), rControlPoint.getY(), 4);
            this.getChildren().addAll(lineToRControl, rControlPointGraphic);
        }

        // Add the main slope point to the graphic
        this.getChildren().add(tangentPointGraphic);
    }

    @Override
    public void recolor(boolean setColored) {
        // Update colored state
        if (setColored) {
            if (!colored && !isLeftColored)
                isLeftColored = true;
            else
                isLeftColored = !isLeftColored;
        }
        this.colored = setColored;

        // Recolor points
        tangentPointGraphic.recolor(setColored);
        if (lControlPointGraphic != null && lineToLControl != null)
            lControlPointGraphic.recolor(setColored);
        if (rControlPointGraphic != null && lineToRControl != null)
            rControlPointGraphic.recolor(setColored);
    }

    @Override
    public double getX() {
        return tangentPointGraphic.getX();
    }
}
