package com.wecca.canoeanalysis.components.graphics.hull;

import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.GraphicsUtils;
import javafx.geometry.Point2D;
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
    private List<BezierTangentGraphic> slopeGraphics;

    /**
     * Deals with mapping between function space and graphic space.
     * @param beziers The list of Bézier functions defining the hull.
     * @param encasingRectangle The smallest region in function space that encloses all points in the function,
     *                          which is mapped to this rectangle in graphics space.
     */
    public CubicBezierSplineHullGraphic(List<CubicBezierFunction> beziers, Rectangle encasingRectangle) {
        super(CalculusUtils.createBezierSplineFunctionShiftedPositive(beziers), new Section(beziers.getFirst().getX(), beziers.getLast().getRx()), encasingRectangle);
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
            BezierTangentGraphic slopeGraphic = new BezierTangentGraphic(mappedKnotPoint, mappedLControlPoint, mappedRControlPoint);
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

    // TODO
    @Override
    public void recolor(boolean setColored) {
        for (BezierTangentGraphic slopeGraphic : slopeGraphics) {
            slopeGraphic.recolor(setColored);
        }
    }

    @Override
    public boolean isColored() {
        return slopeGraphics.stream().anyMatch(BezierTangentGraphic::isColored);
    }

    @Override
    public Section getSection() {
        return new Section(encasingRectangle.getX(), encasingRectangle.getX() + encasingRectangle.getWidth());
    }

    @Override
    public double getHeight(double functionX) {
        return beziers.stream().mapToDouble(bezier -> bezier.value(functionX)).max().orElse(0);
    }
}
