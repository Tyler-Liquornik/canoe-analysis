package com.wecca.canoeanalysis.components.graphics;

import com.wecca.canoeanalysis.models.function.BoundedUnivariateFunction;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.GraphicsUtils;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the graphical visualization of a cubic bezier spline-based hull.
 * Includes graphically showing control points to demonstrate the geometrical construction of the spline.
 */
@Getter @Setter
public class CubicBezierSplineHullGraphic extends CurvedHullGraphic {

    private List<CubicBezierFunction> beziers;
    private List<BezierSlopeGraphic> slopeGraphics;

    /**
     * Deals with mapping between function space and graphic space.
     * @param beziers The list of Bézier functions defining the hull.
     * @param encasingRectangle The smallest region in function space that encloses all points in the function,
     *                          which is mapped to this rectangle in graphics space.
     */
    public CubicBezierSplineHullGraphic(List<CubicBezierFunction> beziers, Rectangle encasingRectangle) {
        super(CalculusUtils.createCompositeBezierFunctionShiftedPositive(beziers), new Section(beziers.getFirst().getX(), beziers.getLast().getRx()), encasingRectangle);
        this.beziers = beziers;
        this.slopeGraphics = new ArrayList<>();
        this.encasingRectangle = encasingRectangle;
        draw(beziers);
    }

    public void draw(List<CubicBezierFunction> beziers) {
        // Collect all control points from all Bézier curves, removing duplicated knot points
        List<Point2D> allBezierCurveConstructionPoints = getAllKnotAndControlPoints(beziers);

        // Create the function space rectangle to map to based on the Bézier curve control points
        double functionMinX = beziers.getFirst().getX();
        double functionMaxX = beziers.getLast().getRx();
        double functionMinY = allBezierCurveConstructionPoints.stream().mapToDouble(Point2D::getY).min().orElse(0);
        double functionMaxY = allBezierCurveConstructionPoints.stream().mapToDouble(Point2D::getY).max().orElse(0);
        Rectangle functionSpace = new Rectangle(functionMinX, functionMinY, functionMaxX - functionMinX, functionMaxY - functionMinY);
        Rectangle graphicSpace = encasingRectangle;

        // Loop through the control points and create slope graphics for each join point
        for (int i = 0; i < allBezierCurveConstructionPoints.size(); i += 3) {
            Point2D knotPoint = allBezierCurveConstructionPoints.get(i);
            Point2D lControlPoint = i > 0 ? allBezierCurveConstructionPoints.get(i - 1) : null;
            Point2D rControlPoint = i + 1 < allBezierCurveConstructionPoints.size() ? allBezierCurveConstructionPoints.get(i + 1) : null;

            // Map knot and control points from function space to graphic space using Rectangles
            Point2D mappedKnotPoint = GraphicsUtils.mapToGraphicSpace(knotPoint, functionSpace, graphicSpace);
            Point2D mappedLControlPoint = lControlPoint != null ? GraphicsUtils.mapToGraphicSpace(lControlPoint, functionSpace, graphicSpace) : null;
            Point2D mappedRControlPoint = rControlPoint != null ? GraphicsUtils.mapToGraphicSpace(rControlPoint, functionSpace, graphicSpace) : null;

            // Create the slope graphic with the mapped points
            BezierSlopeGraphic slopeGraphic = new BezierSlopeGraphic(mappedKnotPoint, mappedLControlPoint, mappedRControlPoint);
            slopeGraphics.add(slopeGraphic);
            this.getChildren().add(slopeGraphic.getNode());
        }
    }

    private List<Point2D> getAllKnotAndControlPoints(List<CubicBezierFunction> beziers) {
        return beziers.stream()
                .flatMap(bezier -> bezier.getKnotAndControlPoints().stream())
                .distinct()
                .toList();
    }

    @Override
    public void recolor(boolean setColored) {
        for (BezierSlopeGraphic slopeGraphic : slopeGraphics) {
            slopeGraphic.recolor(setColored);
        }
    }

    @Override
    public boolean isColored() {
        return slopeGraphics.stream().anyMatch(BezierSlopeGraphic::isColored);
    }

    @Override
    public Section getSection() {
        return new Section(encasingRectangle.getX(), encasingRectangle.getX() + encasingRectangle.getWidth());
    }

    @Override
    public double getHeight(double functionX) {
        return beziers.stream().mapToDouble(bezier -> bezier.value(functionX)).max().orElse(0);
    }

    /**
     * A graphical component for displaying the slope at Bézier spline join points.
     * Can highlight only one half of the slope within either the left or right section.
     */
    @Getter @Setter
    public static class BezierSlopeGraphic extends Group implements Graphic {

        private Point2D centerPoint;
        private Point2D lControlPoint;
        private Point2D rControlPoint;

        private Circle centerCircle;
        private Circle lControlCircle;
        private Circle rControlCircle;

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
        public BezierSlopeGraphic(Point2D centerPoint, Point2D lControlPoint, Point2D rControlPoint) {
            this.centerPoint = centerPoint;
            this.lControlPoint = lControlPoint;
            this.rControlPoint = rControlPoint;
            this.colored = false;
            this.isLeftColored = false;

            // Validation
            if (lControlPoint == null && rControlPoint == null)
                throw new IllegalArgumentException("At least one of the left or right control points must be specified.");
            validateSlope();

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
            centerCircle = new Circle(centerPoint.getX(), centerPoint.getY(), 3);
            centerCircle.setFill(ColorPaletteService.getColor("white"));

            // Create the lines to the control points for whichever exist
            if (lControlPoint != null) {
                lineToLControl = new Line(centerPoint.getX(), centerPoint.getY(), lControlPoint.getX(), lControlPoint.getY());
                lineToLControl.setStroke(ColorPaletteService.getColor("white"));
                lineToLControl.setStrokeWidth(1.5);
                lineToLControl.getStrokeDashArray().addAll(3.0, 3.0);

                lControlCircle = new Circle(lControlPoint.getX(), lControlPoint.getY(), 3);
                lControlCircle.setFill(ColorPaletteService.getColor("white"));

                this.getChildren().addAll(lineToLControl, lControlCircle);
            }

            if (rControlPoint != null) {
                lineToRControl = new Line(centerPoint.getX(), centerPoint.getY(), rControlPoint.getX(), rControlPoint.getY());
                lineToRControl.setStroke(ColorPaletteService.getColor("white"));
                lineToRControl.setStrokeWidth(1.5);
                lineToRControl.getStrokeDashArray().addAll(3.0, 3.0);

                rControlCircle = new Circle(rControlPoint.getX(), rControlPoint.getY(), 3);
                rControlCircle.setFill(ColorPaletteService.getColor("white"));

                this.getChildren().addAll(lineToRControl, rControlCircle);
            }

            // Add the main slope point to the graphic
            this.getChildren().add(centerCircle);
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

            // Update graphics colors
            Color primaryOrWhite = setColored ? ColorPaletteService.getColor("primary")
                    : ColorPaletteService.getColor("white");
            Color primaryLightOrWhite = setColored ? ColorPaletteService.getColor("primary-light")
                    : ColorPaletteService.getColor("white");

            centerCircle.setFill(primaryOrWhite);
            if (lControlCircle != null && lineToLControl != null) {
                lControlCircle.setFill(isLeftColored ? primaryOrWhite : primaryLightOrWhite);
                lineToLControl.setStroke(isLeftColored ? primaryLightOrWhite : ColorPaletteService.getColor("white"));
            }

            if (rControlCircle != null && lineToRControl != null) {
                rControlCircle.setFill(!isLeftColored ? primaryOrWhite : primaryLightOrWhite);
                lineToRControl.setStroke(!isLeftColored ? primaryLightOrWhite : ColorPaletteService.getColor("white"));
            }
        }

        @Override
        public double getX() {
            return centerCircle.getCenterX();
        }
    }
}
