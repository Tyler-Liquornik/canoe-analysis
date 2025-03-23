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
public class CubicBezierSplineHullGraphic extends HullGraphic {

    private List<CubicBezierFunction> beziers;
    private List<BezierHandleGraphic> slopeGraphics;
    private int coloredSectionIndex;

    /**
     * Deals with mapping between function space and graphic space.
     * @param beziers The list of Bézier functions defining the hull.
     * @param encasingRectangle The smallest region in function space that encloses all points in the function,
     *                          which is mapped to this rectangle in graphics space.
     */
    public CubicBezierSplineHullGraphic(List<CubicBezierFunction> beziers, Rectangle encasingRectangle) {
        this(beziers, encasingRectangle, false);
    }

    /**
     * @param useUnimodalOptimization, an extra flag to optimize the function faster for quick renders which only works for unimodal functions
     *                                 it is up to the implementer to understand if the function is unimodal as the cost to validate for this makes the optimization it provides redundant!
     *                                 logically, a hull shape should probably almost always meet these criteria anyway!
     */
    public CubicBezierSplineHullGraphic(List<CubicBezierFunction> beziers, Rectangle encasingRectangle, boolean useUnimodalOptimization) {
        super(CalculusUtils.createBezierSplineFunctionShiftedPositive(beziers, useUnimodalOptimization), new Section(beziers.getFirst().getX(), beziers.getLast().getRx()), encasingRectangle, useUnimodalOptimization);
        this.beziers = beziers;
        this.slopeGraphics = new ArrayList<>();
        this.getCurvedGraphic().setEncasingRectangle(encasingRectangle);
        this.coloredSectionIndex = -1;
        draw(beziers);
    }

    private void draw(List<CubicBezierFunction> beziers) {
        // Collect all control points from all Bézier curves, removing duplicated knot points
        List<Point2D> knotAndControlPoints = beziers.stream()
                .flatMap(bezier -> bezier.getKnotAndControlPoints().stream())
                .distinct()
                .toList();

        // Create the function space rectangle to map to based on the Bézier curve control points
        double functionMinX = beziers.getFirst().getX();
        double functionMaxX = beziers.getLast().getRx();
        double functionMinY = knotAndControlPoints.stream().mapToDouble(Point2D::getY).min().orElse(0);
        double functionMaxY = knotAndControlPoints.stream().mapToDouble(Point2D::getY).max().orElse(0);
        Rectangle functionSpace = new Rectangle(functionMinX, functionMinY, functionMaxX - functionMinX, functionMaxY - functionMinY);
        Rectangle graphicSpace = getCurvedGraphic().getEncasingRectangle();

        // Loop through the control points and create slope graphics for each join point
        for (int i = 0; i < knotAndControlPoints.size(); i += 3) {
            Point2D knotPoint = knotAndControlPoints.get(i);
            Point2D lControlPoint = i > 0 ? knotAndControlPoints.get(i - 1) : null;
            Point2D rControlPoint = i + 1 < knotAndControlPoints.size() ? knotAndControlPoints.get(i + 1) : null;

            // Map knot and control points from function space to graphic space using Rectangles
            Point2D mappedKnotPoint = GraphicsUtils.mapToGraphicSpace(knotPoint, functionSpace, graphicSpace);
            Point2D mappedLControlPoint = lControlPoint != null ? GraphicsUtils.mapToGraphicSpace(lControlPoint, functionSpace, graphicSpace) : null;
            Point2D mappedRControlPoint = rControlPoint != null ? GraphicsUtils.mapToGraphicSpace(rControlPoint, functionSpace, graphicSpace) : null;

            // Create the slope graphic with the mapped points
            BezierHandleGraphic slopeGraphic = new BezierHandleGraphic(mappedLControlPoint, mappedKnotPoint, mappedRControlPoint);
            slopeGraphics.add(slopeGraphic);
            this.getChildren().add(slopeGraphic.getNode());
        }
    }

    /**
     * Recolors all slope graphics based on the given boolean flag.
     * If true, all points are colored. If false, all points are uncolored.
     */
    @Override
    public void recolor(boolean setColored) {
        colorBezierPointGroup(coloredSectionIndex, setColored);
    }

    /**
     * ReColors a specific point group by its index.
     * Point groups are groups of 4 bezier construction points
     * The L & R (or x and rx) section endpoints (knots), and their respective control points
     * @param sectionIndex the index of the section to color.
     * sectionIndex lines up with iterating through sections of the hull that this model represents
     */
    public void colorBezierPointGroup(int sectionIndex, boolean setColored) {
        // One less section than the number of section endpoints (one slope graphic on every section endpoint)
        if (sectionIndex < 0 || sectionIndex >= slopeGraphics.size() - 1)
            throw new IndexOutOfBoundsException("sectionIndex: " + sectionIndex + " out of bounds");

        // Set the current colored section index
        coloredSectionIndex = sectionIndex;

        // Uncolor everything first
        for (BezierHandleGraphic tangentGraphic : slopeGraphics) {
            tangentGraphic.recolor(false);
        }

        // Recolor the right side of the first tangent and the left side of the second tangent
        if (setColored) {
            BezierHandleGraphic firstTangent = slopeGraphics.get(sectionIndex);
            BezierHandleGraphic secondTangent = slopeGraphics.get(sectionIndex + 1);
            firstTangent.recolorRight(true);
            secondTangent.recolorLeft(true);
        }
    }

    /**
     * Switches to the next section and recolors it.
     */
    public void colorNextBezierPointGroup() {
        coloredSectionIndex = (coloredSectionIndex + 1) % (slopeGraphics.size() - 1);
        colorBezierPointGroup(coloredSectionIndex, true);
    }

    /**
     * Switches to the previous section and recolors it.
     */
    public void colorPreviousBezierPointGroup() {
        if (coloredSectionIndex != -1)
            coloredSectionIndex = (coloredSectionIndex + slopeGraphics.size() - 2) % (slopeGraphics.size() - 1);
        else
            coloredSectionIndex = slopeGraphics.size() - 2;
        colorBezierPointGroup(coloredSectionIndex, true);
    }

    @Override
    public boolean isColored() {
        return slopeGraphics.stream().anyMatch(BezierHandleGraphic::isColored);
    }

    @Override
    public Section getSection() {
        return new Section(getCurvedGraphic().getEncasingRectangle().getX(), getCurvedGraphic().getEncasingRectangle().getX() + getCurvedGraphic().getEncasingRectangle().getWidth());
    }

    @Override
    public double getHeight(double functionX) {
        return beziers.stream().mapToDouble(bezier -> bezier.value(functionX)).max().orElse(0);
    }
}
