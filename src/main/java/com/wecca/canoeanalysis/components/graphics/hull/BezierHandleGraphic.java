package com.wecca.canoeanalysis.components.graphics.hull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.graphics.Graphic;
import com.wecca.canoeanalysis.services.color.ColorManagerService;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A graphical component for displaying the slope at Bézier spline join points as a handle.
 * Can highlight only one half of the handle within either the left or right section.
 */
@Getter @Setter
public class BezierHandleGraphic extends Group implements Graphic {

    private Point2D lControlPoint;
    private Point2D centerPoint;
    private Point2D rControlPoint;

    private PointGraphic lControlPointGraphic;
    private PointGraphic tangentPointGraphic;
    private PointGraphic rControlPointGraphic;

    private Line lineToLControl;
    private Line lineToRControl;

    private boolean isLeftColored;
    private boolean isRightColored;

    /**
     * @param lControlPoint The left control point (can be null).
     * @param centerPoint The main point representing the center of the handle.
     * @param rControlPoint The right control point (can be null).
     * Note that lControlPoint and rControlPoint cannot both be null.
     */
    public BezierHandleGraphic(Point2D lControlPoint, Point2D centerPoint, Point2D rControlPoint) {
        this.lControlPoint = lControlPoint;
        this.centerPoint = centerPoint;
        this.rControlPoint = rControlPoint;
        this.isLeftColored = false;
        this.isRightColored = false;

        // Validation
        if (lControlPoint == null && rControlPoint == null)
            throw new IllegalArgumentException("At least one of the left or right control points must be specified.");

        // Commented out to speed up rendering, but should be enforced, use this to test!
        // validateSlope();

        ColorManagerService.registerInColorPalette(this);
        draw();
    }

    /**
     * Validates that the slopes from the main point to the left and right control points (if present) are equal.
     * This is a requirement for C1 smoothness in the model and the graphic should reflect that.
     * Note that if only one control point is present, it's valid by default (these are the edge sections, S_0 and S_n).
     */
    @Traceable
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
        tangentPointGraphic = new PointGraphic(centerPoint.getX(), centerPoint.getY(), 4);

        // Create the lines to the control points for whichever exist
        // +1 shifts graphics misalignment from points getY
        if (lControlPoint != null) {
            lineToLControl = new Line(centerPoint.getX(), centerPoint.getY() + 1, lControlPoint.getX(), lControlPoint.getY() + 1);
            lineToLControl.setStroke(ColorPaletteService.getColor("white"));
            lineToLControl.setStrokeWidth(2);
            lineToLControl.getStrokeDashArray().addAll(3.0, 3.0);
            lControlPointGraphic = new PointGraphic(lControlPoint.getX(), lControlPoint.getY(), 3);
            this.getChildren().addAll(lineToLControl, lControlPointGraphic);
        }

        if (rControlPoint != null) {
            lineToRControl = new Line(centerPoint.getX(), centerPoint.getY() + 1, rControlPoint.getX(), rControlPoint.getY() + 1);
            lineToRControl.setStroke(ColorPaletteService.getColor("white"));
            lineToRControl.setStrokeWidth(2);
            lineToRControl.getStrokeDashArray().addAll(3.0, 3.0);
            rControlPointGraphic = new PointGraphic(rControlPoint.getX(), rControlPoint.getY(), 3);
            this.getChildren().addAll(lineToRControl, rControlPointGraphic);
        }

        this.getChildren().add(tangentPointGraphic);
    }

    @Override
    public void recolor(boolean setColored) {
        Color lineColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("white");
        tangentPointGraphic.recolor(setColored);
        if (lControlPointGraphic != null && lineToLControl != null) {
            lControlPointGraphic.recolor(setColored);
            lineToLControl.setStroke(lineColor);
        }
        if (rControlPointGraphic != null && lineToRControl != null) {
            rControlPointGraphic.recolor(setColored);
            lineToRControl.setStroke(lineColor);
        }
    }

    /**
     * Recolor the left control point and the line to it.
     * @param setColored whether the left point should be colored or uncolored.
     */
    public void recolorLeft(boolean setColored) {
        Color leftLineColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("white");
        if (lControlPointGraphic != null && lineToLControl != null) {
            lControlPointGraphic.recolor(setColored);
            lineToLControl.setStroke(leftLineColor);
        }
        tangentPointGraphic.recolor(isRightColored || setColored);
        this.isLeftColored = setColored;
    }

    /**
     * Recolor the right control point and the line to it.
     * @param setColored whether the right point should be colored or uncolored.
     */
    public void recolorRight(boolean setColored) {
        Color rightLineColor = setColored ? ColorPaletteService.getColor("primary-light") : ColorPaletteService.getColor("white");
        if (rControlPointGraphic != null && lineToRControl != null) {
            rControlPointGraphic.recolor(setColored);
            lineToRControl.setStroke(rightLineColor);
        }
        tangentPointGraphic.recolor(isLeftColored || setColored);
        this.isRightColored = setColored;
    }

    @Override
    public double getX() {
        return tangentPointGraphic.getX();
    }

    @Override
    public boolean isColored() {
        return isLeftColored || isRightColored;
    }

    @JsonIgnore
    public List<Node> getAllGraphicsButCenterPoint() {
        return Stream.of(lControlPointGraphic,rControlPointGraphic, lineToLControl, lineToRControl).filter(Objects::nonNull).toList();
    }
}
