package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.controllers.MainController;
import com.wecca.canoeanalysis.controllers.modules.HullBuilderController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Range;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import lombok.NonNull;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs geometry-related operations on the canoe's hull.
 * This is the main engine for driving simple user interactions to modify hull geometry
 * This class is responsible for updating and managing control points, knot points,
 * and parameters such as radius and angle to ensure the geometric integrity of the hull,
 * and the bounds on what the user can do the keep the geometry reasonable for a hull
 * (one central theme for "reasonable" geometry is C1 continuity between adjacent hull sections).
 * This ultimately operates as a bridge between the data model (hull and sections) and the UI
 *
 */
@Traceable
public class HullGeometryService {

    @Setter
    private static HullBuilderController hullBuilderController;

    // Numerical 'dx' used at bounds to prevent unpredictable boundary behaviour
    public static final double OPEN_INTERVAL_TOLERANCE = 1e-3;

    /**
     * In this service, we will prefer to only process and return data for the hull model
     * Setting the processed data to the hull model should occur instead at the controller level
     * @return a new memory reference to the hull equivalent to the one in HullBuilderController state
     */
    private static Hull getHull() {
        if (hullBuilderController == null)
            throw new IllegalStateException("HullBuilderController is not set");
        return YamlMarshallingService.deepCopy(hullBuilderController.getHull());
    }

    /**
     * Calculates the maximum radius (r) such that a point at (r, theta) from an origin/knot (xO, yO)
     * remains within the rectangular bounds defined by x = 0, x = L, y = 0, y = -h.
     * @param knot the knot point which acts as the origin
     * @param thetaKnown the known angle in degrees (relative to the origin) at which the point lies.
     * @param selectedHullSectionIndex the index of the hullSection in the hull in which to bound the radius
     *                                 (not necessarily the current state hullSectionIndex in HullBuilderController)
     * @return The maximum radius (r) such that the point stays within the bounds.
     */
    public static double calculateMaxR(Point2D knot, double thetaKnown, int selectedHullSectionIndex) {
        Hull hull = getHull();
        HullSection hullSection = hull.getHullSections().get(selectedHullSectionIndex);
        double hullHeight = -hull.getMaxHeight();
        CubicBezierFunction bezier = (CubicBezierFunction) hullSection.getSideProfileCurve();
        double xL = bezier.getX1();
        double xR = bezier.getX2();

        double rMax = Double.MAX_VALUE;
        double thetaRad = Math.toRadians((thetaKnown + 90) % 360);
        double cosTheta =  Math.cos(thetaRad);
        double sinTheta = Math.sin(thetaRad);
        double approximatelyZeroRadians = Math.toRadians(OPEN_INTERVAL_TOLERANCE);

        // Use CAST rule to determine which rectangle boundaries to calculate rMax
        // Edge cases of 0/90/180/270/360 handled appropriately
        double sinOfApproximatelyZeroRadians = Math.sin(approximatelyZeroRadians);
        if (Math.abs(sinTheta) != Math.abs(sinOfApproximatelyZeroRadians)) {
            if (cosTheta < 0) {
                double rFromLeft = (knot.getX() - xL) / -cosTheta;
                rMax = Math.min(rMax, rFromLeft);
            } else if (cosTheta > 0) {
                double rFromRight = (xR - knot.getX()) / cosTheta;
                rMax = Math.min(rMax, rFromRight);
            }
        }
        double cosOfApproximatelyZeroRadians = Math.cos(approximatelyZeroRadians);
        if (Math.abs(cosTheta) != Math.abs(cosOfApproximatelyZeroRadians)) {
            if (sinTheta < 0) {
                double rFromBottom = (hullHeight - knot.getY()) / sinTheta;
                rMax = Math.min(rMax, rFromBottom);
            } else if (sinTheta > 0) {
                double rFromTop = -knot.getY() / sinTheta;
                rMax = Math.min(rMax, rFromTop);
            }
        }

        return Math.max(0, rMax);
    }

    /**
     * Returns the allowable angular range for a control point on a circle within a bounding rectangle.
     * Merges constraints from adjacent hull sections if needed, and applies a small tolerance so the range
     * does not collapse. If it does collapse, the range is set to a single value.
     * @param knot The reference point for polar coordinates
     * @param rKnown The radial distance from the knot to the control point
     * @param currTheta The current angle (in degrees)
     * @param isLeft True if the control point is on the left side (180–360 degrees), false otherwise (0–180)
     * @param boundWithAdjacentSections True if the range should account for constraints from adjacent sections
     * @param selectedHullSectionIndex The index of the hull section to process for bounding
     *                                  (not necessarily the current state hullSectionIndex in HullBuilderController)
     * @return A two-element array containing the minimum and maximum valid theta values
     */
    public static double[] calculateThetaBounds(Point2D knot, double rKnown, double currTheta, boolean isLeft, boolean boundWithAdjacentSections, int selectedHullSectionIndex) {
        Hull hull = getHull();
        HullSection hullSection = hull.getHullSections().get(selectedHullSectionIndex);
        double l = hullSection.getLength();
        double h = -hull.getMaxHeight();
        Rectangle boundingRect = new Rectangle(hullSection.getX(), 0.0, l, h);

        double[] rawThetaBounds = calculateRawThetaBounds(boundingRect, knot, rKnown, isLeft, currTheta);
        double minTheta = rawThetaBounds[0];
        double maxTheta = rawThetaBounds[1];

        if (boundWithAdjacentSections) {
            double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, isLeft, currTheta);
            minTheta = additionalThetaBounds != null
                    ? Math.max(minTheta, additionalThetaBounds[0])
                    : minTheta;
            maxTheta = additionalThetaBounds != null
                    ? Math.min(maxTheta, additionalThetaBounds[1])
                    : maxTheta;
            if (maxTheta < minTheta) maxTheta = minTheta;
        }

        if (Math.abs(minTheta - maxTheta) < 1e-2) {
            double avg = 0.5 * (minTheta + maxTheta);
            return new double[] {avg, avg};
        }

        if (Math.abs(minTheta - currTheta) <= OPEN_INTERVAL_TOLERANCE) minTheta = currTheta;
        else minTheta += OPEN_INTERVAL_TOLERANCE;
        if (Math.abs(maxTheta - currTheta) <= OPEN_INTERVAL_TOLERANCE) maxTheta = currTheta;
        else maxTheta -= OPEN_INTERVAL_TOLERANCE;
        return new double[] {minTheta, maxTheta};
    }

    /**
     * Finds the valid range of angles where a control point on a circle stays within the specified rectangle.
     * Determines the domain based on whether it's the left side (180–360) or the right side (0–180),
     * then solves circle-rectangle intersections and selects the smallest segment containing currTheta.
     * @param boundingRect The rectangle bounding the control point
     * @param center The circle's center
     * @param radius The circle's radius
     * @param isLeft True if searching 180–360, false if 0–180
     * @param currTheta The current angle (in degrees) for which to constrain the range
     * @return An array with [minTheta, maxTheta] that confines the control point within the rectangle
     */
    private static double[] calculateRawThetaBounds(Rectangle boundingRect, Point2D center, double radius, boolean isLeft, double currTheta) {
        // Initialize the search domain
        double thetaMin = isLeft ? 180 : 0;
        double thetaMax = isLeft ? 360 : 180;

        // Rectangle Bounds
        double xMin = boundingRect.getX();
        double xMax = xMin + boundingRect.getWidth();
        double yMax = boundingRect.getY();
        double yMin = yMax + boundingRect.getHeight(); // negative height

        // Build candidate angle bounds list alpha_i as POIs of the rectangle and circular arc
        // Circle formed by sweeping the search domain at the given radius
        List<Double> angles = new ArrayList<>();
        addIntersectionAngles(angles, center, radius, xMin, true);
        addIntersectionAngles(angles, center, radius, xMax, true);
        addIntersectionAngles(angles, center, radius, yMin, false);
        addIntersectionAngles(angles, center, radius, yMax, false);
        angles = angles.stream().map(x -> ((x % 360) + 360) % 360).filter(x -> (x > thetaMin && x < thetaMax)).sorted().toList();


        // Handle cases for number of POIs alpha_i
        int n = angles.size();
        if (n == 0) return new double[] {thetaMin, thetaMax};
        if (n == 2) return new double[] {angles.getFirst(), angles.getLast()};
        else if (n == 1) {
            double alpha = angles.getFirst();
            if (currTheta < alpha) {
                double mid = 0.5 * (thetaMin + alpha);
                if (isPointInBounds(radius, mid, center, boundingRect)) return new double[] {thetaMin, alpha};
                else return new double[] {alpha, thetaMax};
            } else {
                double mid = 0.5 * (alpha + thetaMax);
                if (isPointInBounds(radius, mid, center, boundingRect)) return new double[] {alpha, thetaMax};
                else return new double[] {thetaMin, alpha};
            }
        }
        // > 2 POIs found, find the range containing currTheta
        else {
            for (int i = 0; i < n - 1; i++) {
                double start = angles.get(i);
                double end = angles.get(i + 1);
                if (currTheta >= start && currTheta <= end) return new double[] {start, end};
            }
            return new double[] {thetaMin, thetaMax};
        }
    }

    /**
     * Finds intersection angles for a circle and a vertical (x=val) or horizontal (y=val) line.
     * The angles are calculated using the calculus convention where the 0-degree reference is shifted 90 degrees forward.
     *
     * @param angles List to store the calculated angles.
     * @param knot The center of the circle.
     * @param r The radius of the circle.
     * @param lineVal The value of the vertical or horizontal line (x or y).
     * @param isX True for vertical line (x=val), false for horizontal line (y=val).
     */
    private static void addIntersectionAngles(List<Double> angles, Point2D knot, double r, double lineVal, boolean isX) {
        double knot1 = isX ? knot.getX() : knot.getY();
        double knot2 = isX ? knot.getY() : knot.getX();
        double distance = lineVal - knot1;
        double sq = r * r - distance * distance;

        // Required condition for intersection angles
        if (sq >= 0) {
            double tolerance = 1e-8 * r * r;
            if (Math.abs(sq) < tolerance) { // Numerical tangency, only one intersection point
                double angle = CalculusUtils.toPolar(isX ? new Point2D(lineVal, knot2) : new Point2D(knot2, lineVal), knot).getY();
                angles.add(angle);
            } else { // There must be exactly 2 intersection points
                double root = Math.sqrt(sq);
                double intersect1 = knot2 + root;
                double intersect2 = knot2 - root;
                angles.add(CalculusUtils.toPolar(isX ? new Point2D(lineVal, intersect1) : new Point2D(intersect1, lineVal), knot).getY());
                angles.add(CalculusUtils.toPolar(isX ? new Point2D(lineVal, intersect2) : new Point2D(intersect2, lineVal), knot).getY());
            }
        }
    }

    /**
     * Checks if a control point, specified in polar coordinates, lies within the bounds of a given rectangle.
     * @param rKnown The radius of the control point in polar coordinates.
     * @param thetaGuess The angle (in radians) of the control point in polar coordinates.
     * @param knot The knot point (reference point) to which the polar coordinates are relative.
     * @return True if the control point lies within the bounds of the rectangle;
     */
    private static boolean isPointInBounds(double rKnown, double thetaGuess, Point2D knot, Rectangle boundingRect) {
        // Convert polar to Cartesian
        Point2D cartesianControl = CalculusUtils.toCartesian(new Point2D(rKnown, thetaGuess), knot);
        double xControl = cartesianControl.getX();
        double yControl = cartesianControl.getY();

        // Rectangle bounds
        double xMin = boundingRect.getX();
        double xMax = xMin + boundingRect.getWidth();
        double yMax = boundingRect.getY();
        double yMin = yMax + boundingRect.getHeight(); // negative height

        // Fix floating point proximity
        if (Math.abs(xControl - xMin) < 1e-6) xControl = xMin;
        if (Math.abs(xControl - xMax) < 1e-6) xControl = xMax;
        if (Math.abs(yControl - yMin) < 1e-6) yControl = yMin;
        if (Math.abs(yControl - yMax) < 1e-6) yControl = yMax;

        // Check standard rectangle inclusion
        return (xControl >= xMin && xControl <= xMax &&
                yControl >= yMin && yControl <= yMax);
    }

    /**
     * Calculates additional theta bounds based on adjacent sections' control points to ensure smoothness and continuity.
     *
     * @param knot the knot point which acts as the origin
     * @param isLeft whether the control point belongs to the left knot or right knot
     * @param currTheta, the current theta value before the updated geometry, of the original section (NOT the adjacent section)
     * @return [additionalMinTheta, additionalMaxTheta], or null for an edge knot (the first and last knot with no adjacent sections they are shared with)
     */
    public static double[] calculateAdjacentSectionThetaBounds(Point2D knot, boolean isLeft, double currTheta) {
        Hull hull = getHull();
        int selectedHullSectionIndex = hullBuilderController.getSelectedHullSectionIndex();
        double thetaMin = isLeft ? 180 : 0;
        double thetaMax = isLeft ? 360 : 180;

        int adjacentHullSectionIndex = selectedHullSectionIndex + (isLeft ? -1 : 1);
        boolean hasAdjacentSection = isLeft
                ? selectedHullSectionIndex > 0
                : selectedHullSectionIndex < hull.getHullSections().size() - 1;

        if (hasAdjacentSection) {
            HullSection adjacentHullSection = hull.getHullSections().get(adjacentHullSectionIndex);
            Point2D siblingPoint = isLeft
                    ? ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getLast()
                    : ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
            double siblingPointR = CalculusUtils.toPolar(siblingPoint, knot).getX();
            double[] thetaBounds = calculateThetaBounds(knot, siblingPointR, (currTheta + 180) % 360, !isLeft, false, adjacentHullSectionIndex);

            thetaMin = Math.max(thetaMin, (thetaBounds[0] + 180) % 360);
            thetaMax = Math.min(thetaMax, (thetaBounds[1] + 180) % 360);
            return new double[] {thetaMin, thetaMax};
        }
        else return null;
    }

    /**
     * Adjusts the control points of the adjacent hull sections to maintain C1 continuity.
     *
     * @param adjacentSection      the adjacent section (either on the left or right) to adjust
     *                             the correct section must be passed in, this logic is not in the method
     * @param adjustLeftOfSelected whether to adjust the adjacent left section (otherwise right)
     * @param deltaX               the amount by which to adjust the control point x in the adjacent section
     * @param deltaY               the amount by which to adjust the control point y in the adjacent section
     */
    public static void adjustAdjacentSectionControlPoint(HullSection adjacentSection, boolean adjustLeftOfSelected, double deltaX, double deltaY) {
        CubicBezierFunction adjacentBezier = (CubicBezierFunction) adjacentSection.getSideProfileCurve();

        // Adjust the control point by the delta
        if (adjustLeftOfSelected) {
            adjacentBezier.setControlX2(adjacentBezier.getControlX2() + deltaX);
            adjacentBezier.setControlY2(adjacentBezier.getControlY2() + deltaY);
        } else {
            adjacentBezier.setControlX1(adjacentBezier.getControlX1() + deltaX);
            adjacentBezier.setControlY1(adjacentBezier.getControlY1() + deltaY);
        }

        // Update the section's side profile curve
        adjacentSection.setSideProfileCurve(adjacentBezier);
    }

    /**
     * Computes the new control point and deltas for adjusting the control point of an adjacent hull section
     * to maintain C1 smoothness. The computation is based on polar coordinate transformations.
     *
     * @param adjacentHullSection       The adjacent hull section being adjusted.
     * @param isLeftAdjacentSection     True if adjusting the left-adjacent section, false otherwise.
     * @param newThetaVal               The new angle (theta) value for the selected control point.
     * @param selectedKnot              The knot point used as the reference for polar transformations.
     * @return A Point2D array where:
     *         index 0 contains the deltaX,
     *         index 1 contains the deltaY.
     */
    public static Point2D calculateAdjacentControlDeltas(HullSection adjacentHullSection, boolean isLeftAdjacentSection, double newThetaVal, Point2D selectedKnot) {
        // Determine old control point and polar radius (r)
        Point2D adjacentSectionOldControl;
        double adjacentSectionRadius;
        double adjacentSectionNewThetaVal = (newThetaVal + 180) % 360;

        if (isLeftAdjacentSection)
            adjacentSectionOldControl = ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getLast();
        else
            adjacentSectionOldControl = ((CubicBezierFunction) adjacentHullSection.getSideProfileCurve()).getControlPoints().getFirst();
        adjacentSectionRadius = CalculusUtils.toPolar(adjacentSectionOldControl, selectedKnot).getX();

        // Compute new control point in Cartesian coordinates
        Point2D adjacentSectionNewControl = CalculusUtils.toCartesian(
                new Point2D(adjacentSectionRadius, adjacentSectionNewThetaVal), selectedKnot);

        // Calculate and return deltas
        double deltaX = adjacentSectionNewControl.getX() - adjacentSectionOldControl.getX();
        double deltaY = adjacentSectionNewControl.getY() - adjacentSectionOldControl.getY();
        return new Point2D(deltaX, deltaY);
    }

    /**
     * Calculates the bounds for rL, thetaL, rR, and thetaR for the control points of the selected hull section.
     * Ensures that the control points stay within the rectangle defined by:
     * x = 0, x = L (length of the hull section), y = 0, y = -h (negative of the maximum height of the hull).
     * Returns an array in the format:
     * [rLMin, rLMax, thetaLMin, thetaLMax, rRMin, rRMax, thetaRMin, thetaRMax].
     */
    public static double[] calculateParameterBounds() {
        // Get hull and section details
        HullSection selectedHullSection = hullBuilderController.getSelectedHullSection();
        int hullSectionIndex = hullBuilderController.getSelectedHullSectionIndex();
        if (!(selectedHullSection.getSideProfileCurve() instanceof CubicBezierFunction bezier)) {
            throw new IllegalArgumentException("Cannot work with non-bezier hull curve");
        }

        // Get bezier knot points
        List<Point2D> knotPoints = bezier.getKnotPoints();
        Point2D lKnot = knotPoints.getFirst();
        Point2D rKnot = knotPoints.getLast();

        // Get polar coordinates for control points
        Point2D polarL = CalculusUtils.toPolar(new Point2D(bezier.getControlX1(), bezier.getControlY1()), lKnot);
        Point2D polarR = CalculusUtils.toPolar(new Point2D(bezier.getControlX2(), bezier.getControlY2()), rKnot);
        double rL = polarL.getX();
        double rR = polarR.getX();
        double thetaL = polarL.getY();
        double thetaR = polarR.getY();

        // Calculate bounds
        double rMin = HullGeometryService.OPEN_INTERVAL_TOLERANCE;
        double rLMax = calculateMaxR(lKnot, thetaL, hullSectionIndex);
        double rRMax = calculateMaxR(rKnot, thetaR, hullSectionIndex);
        double[] thetaLBounds = calculateThetaBounds(lKnot, rL, thetaL, true, true, hullSectionIndex);
        double[] thetaRBounds = calculateThetaBounds(rKnot, rR, thetaR, false, true, hullSectionIndex);

        // Return the min and max of each parameter (i.e. its bounds)
        return new double[] {
                rMin, rLMax,
                thetaLBounds[0], thetaLBounds[1],
                rMin, rRMax,
                thetaRBounds[0], thetaRBounds[1]
        };
    }


    /**
     * Calculates the bounds for θ (theta) when r (radius) is updated.
     * Accounts for the geometry of the current section and optionally the adjacent sections.
     * @param parameterIndex The index of the parameter being updated (0 or 2 for rL and rR).
     * @param siblingTheta The siblings initial θ value before being adjusted.
     * @param selectedR The current radius value for the parameter value being updated by the user.
     * @return An interval [thetaMin, thetaMax]
     */
    public static double[] calculateSiblingThetaBounds(int parameterIndex, double siblingTheta, double selectedR) {
        HullSection selectedHullSection = hullBuilderController.getSelectedHullSection();
        int hullSectionIndex = hullBuilderController.getSelectedHullSectionIndex();

        CubicBezierFunction bezier = (CubicBezierFunction) selectedHullSection.getSideProfileCurve();
        Point2D knot = (parameterIndex == 0) ? bezier.getKnotPoints().getFirst() : bezier.getKnotPoints().getLast();

        // Calculate theta bounds for the current section
        double[] thetaBounds = calculateThetaBounds(knot, selectedR, siblingTheta, parameterIndex == 0, false, hullSectionIndex);

        // Merge bounds with adjacent sections if applicable
        double[] additionalThetaBounds = calculateAdjacentSectionThetaBounds(knot, parameterIndex == 0, siblingTheta);
        double minTheta = (additionalThetaBounds != null)
                ? Math.max(thetaBounds[0], additionalThetaBounds[0])
                : thetaBounds[0];
        double maxTheta = (additionalThetaBounds != null)
                ? Math.max(Math.min(thetaBounds[1], additionalThetaBounds[1]), minTheta)
                : Math.max(thetaBounds[1], minTheta);

        // Clip bounds to provide a buffer for user interaction
        double adjustedMinTheta = Math.abs(minTheta - siblingTheta) < 0.5 ? siblingTheta : minTheta;
        double adjustedMaxTheta = Math.abs(maxTheta - siblingTheta) < 0.5 ? siblingTheta : maxTheta;
        return new double[] {adjustedMinTheta, adjustedMaxTheta};
    }

    /**
     * Calculates the maximum r (radius) when θ (theta) is updated.
     * @param thetaParameterIndex The index of the parameter being updated (1 or 3 for θL and θR).
     * @param theta The current θ value for the parameter in the hull model being updated.
     * @return The maximum r value that keeps the control point within bounds.
     */
    public static double calculateSiblingRMax(int thetaParameterIndex, double theta) {
        HullSection selectedHullSection = hullBuilderController.getSelectedHullSection();
        int hullSectionIndex = hullBuilderController.getSelectedHullSectionIndex();
        CubicBezierFunction bezier = (CubicBezierFunction) selectedHullSection.getSideProfileCurve();
        Point2D knot = (thetaParameterIndex == 1) ? bezier.getKnotPoints().getFirst() : bezier.getKnotPoints().getLast();
        return calculateMaxR(knot, theta, hullSectionIndex);
    }

    /**
     * Converts the knot and control points of the selected hull section into polar coordinates.
     * Returns a list of polar coordinate values for the control points in the format:
     * [rL, θL, rR, θR].
     *
     * @return A list of polar coordinate values for the control points.
     */
    public static List<Double> getPolarParameterValues() {
        // Get hull and section details
        HullSection selectedHullSection = hullBuilderController.getSelectedHullSection();

        if (!(selectedHullSection.getSideProfileCurve() instanceof CubicBezierFunction bezier))
            throw new IllegalArgumentException("Cannot work with non-bezier hull curve");

        // Get knot and control points
        List<Point2D> knotAndControlPoints = bezier.getKnotAndControlPoints();

        // Convert control points to polar coordinates
        Point2D polarL = CalculusUtils.toPolar(knotAndControlPoints.get(1), knotAndControlPoints.get(0));
        Point2D polarR = CalculusUtils.toPolar(knotAndControlPoints.get(2), knotAndControlPoints.get(3));

        // Return polar values as a list
        return List.of(polarL.getX(), polarL.getY(), polarR.getX(), polarR.getY());
    }

    /**
     * Updates the control points of the selected hull section based on the parameter index in the list of parameters and new value.
     * Also adjusts the adjacent section's control points if necessary to maintain C1 smoothness.
     * @param parameterIndex The index of the parameter being adjusted (0-3 for rL, θL, rR, θR).
     * @param newParameterValue The new value for the parameter after user interaction.
     * @param parameterValues Array of current parameter values: [rL, θL, rR, θR]
     */
    public static Hull updateHullParameter(int parameterIndex, double newParameterValue, double[] parameterValues) {
        if (parameterValues.length != 4)
            throw new IllegalArgumentException("parameterValues must have exactly 4 elements: [rL, θL, rR, θR]");

        Hull hull = getHull();
        int selectedHullSectionIndex = hullBuilderController.getSelectedHullSectionIndex();
        HullSection selectedHullSection = hull.getHullSections().get(selectedHullSectionIndex);

        if (!(selectedHullSection.getSideProfileCurve() instanceof CubicBezierFunction selectedBezier))
            throw new IllegalArgumentException("Cannot work with non-bezier hull curve");

        // Get knot points
        List<Point2D> selectedKnotPoints = selectedBezier.getKnotPoints();
        Point2D selectedLKnot = selectedKnotPoints.getFirst();
        Point2D selectedRKnot = selectedKnotPoints.getLast();

        // Update the relevant control point
        if (parameterIndex == 0 || parameterIndex == 1) { // Left control point
            Point2D lControl = CalculusUtils.toCartesian(
                    new Point2D(parameterValues[0], parameterValues[1]), selectedLKnot);
            selectedBezier.setControlX1(lControl.getX());
            selectedBezier.setControlY1(lControl.getY());
        } else if (parameterIndex == 2 || parameterIndex == 3) { // Right control point
            Point2D rControl = CalculusUtils.toCartesian(
                    new Point2D(parameterValues[2], parameterValues[3]), selectedRKnot);
            selectedBezier.setControlX2(rControl.getX());
            selectedBezier.setControlY2(rControl.getY());
        }

        // Adjust adjacent sections if needed for C1 smoothness
        boolean adjustingLeft = parameterIndex == 1 && selectedHullSectionIndex > 0;
        boolean adjustingRight = parameterIndex == 3 && selectedHullSectionIndex < (hull.getHullSections().size() - 1);

        if (adjustingLeft || adjustingRight) {
            int adjacentIndex = selectedHullSectionIndex + (adjustingLeft ? -1 : 1);
            HullSection adjacentSection = hull.getHullSections().get(adjacentIndex);
            Point2D selectedKnot = adjustingLeft ? selectedLKnot : selectedRKnot;

            // Calculate deltas for the adjacent control point
            Point2D deltas = calculateAdjacentControlDeltas(
                    adjacentSection, adjustingLeft, newParameterValue, selectedKnot);

            // Adjust the control point of the adjacent section
            adjustAdjacentSectionControlPoint(adjacentSection, adjustingLeft, deltas.getX(), deltas.getY());
            hull.getHullSections().set(adjacentIndex, adjacentSection);
        }
        return hull;
    }

    public static Point2D getDeletableKnotPoint(double functionSpaceX) {
        Hull hull = getHull();
        List<Range> overlaySections = hullBuilderController.getOverlaySections();

        // Iterate through overlay sections to find where functionSpaceX falls
        for (int i = 0; i < overlaySections.size(); i++) {
            Range currOverlay = overlaySections.get(i);

            // "Deleting Sections" are between control points
            Range nextOverlay;
            double deletingSectionRx;
            Section deletingSection;
            if (i != overlaySections.size() - 1) {
                nextOverlay = overlaySections.get(i + 1);
                double deletingSectionX = currOverlay.getX() < currOverlay.getRx() ? currOverlay.getRx() : currOverlay.getX();
                deletingSectionRx = nextOverlay.getX() < nextOverlay.getRx() ? nextOverlay.getX() : nextOverlay.getRx();
                deletingSection = new Section(deletingSectionX, deletingSectionRx);
            } else deletingSection = null;


            // Left control point further to the right than the right support point
            // Split deletable zone between the left and right knot point about the midpoint of the overlapping range
            HullSection hullSection = hull.getHullSections().get(i);
            CubicBezierFunction bezier = (CubicBezierFunction) hullSection.getSideProfileCurve();
            double lKnotX = bezier.getX1();
            double rKnotX = bezier.getX2();
            if (currOverlay.getX() > currOverlay.getRx() && lKnotX <= functionSpaceX && functionSpaceX <= rKnotX) {
                // Midpoint determines in which zones each knot point is deletable
                double lControlX = bezier.getControlX1();
                double rControlX = bezier.getControlX2();
                double midpoint = rControlX + ((lControlX - rControlX) / 2);
                if (functionSpaceX < midpoint) {
                    if (i == 0) return null;
                    double knotY = bezier.value(lKnotX);
                    return new Point2D(lKnotX, knotY);
                } else {
                    if (i == overlaySections.size() - 1) return null;
                    HullSection nextHullSection = hull.getHullSections().get(i + 1);
                    CubicBezierFunction nextBezier = (CubicBezierFunction) nextHullSection.getSideProfileCurve();
                    double knotY = nextBezier.value(rKnotX);
                    return new Point2D(rKnotX, knotY);
                }
            }
            else if (deletingSection != null && deletingSection.getX() <= functionSpaceX && deletingSection.getRx() >= functionSpaceX) {
                // Find the knot point corresponding to deletingSection's rx
                double knotX = hull.getHullSections().stream()
                        .flatMap(hs -> ((CubicBezierFunction) hs.getSideProfileCurve()).getKnotPoints().stream())
                        .mapToDouble(Point2D::getX)
                        .distinct()
                        .filter(x -> x >= deletingSection.getX() && x <= deletingSection.getRx())
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Cannot find knot point in the section"));

                // Get the y-coordinate of the knot point from the corresponding Bezier curve
                double knotY = hull.getHullSections().get(i).getSideProfileCurve().value(knotX);

                return new Point2D(knotX, knotY);
            }
        }

        // No matching section found
        return null;
    }

    /**
     * @param knotPointToAdd the knot point to add to the hull model
     */
    public static void addKnotPoint(@NonNull Point2D knotPointToAdd) {

    }

    /**
     * Deletes a knot point from the hull model.
     * Merges adjacent sections around the knot point to maintain continuity.
     * @param knotPointToDelete the knot point to delete from the hull model
     * @return the updated Hull
     */
    public static Hull deleteKnotPoint(Point2D knotPointToDelete) {
        Hull hull = getHull();
        if (knotPointToDelete == null) return null;
        for (int i = 0; i < hull.getHullSections().size(); i++) {
            HullSection section = hull.getHullSections().get(i);
            if (section.getSideProfileCurve() instanceof CubicBezierFunction bezier) {
                Point2D knotPoint = bezier.getKnotPoints().getFirst();
                double knotX = knotPoint.getX();
                double knotY = knotPoint.getY();
                if (Math.abs(knotX - knotPointToDelete.getX()) < 1e-6 && Math.abs(knotY - knotPointToDelete.getY()) < 1e-6) {
                    if (i > 0) return getHullWithMergedAdjacentSections(i);
                    else return null;
                }
            } else throw new IllegalArgumentException("Cannot work with non-bezier hull");
        }
        throw new RuntimeException("Failed to delete knot.");
    }

    /**
     * Merges two adjacent hull sections to maintain continuity after a knot point is deleted.
     * @param rightIndex the index of the right section of the two adjacent hull sections
     * @return the updated Hull
     */
     private static Hull getHullWithMergedAdjacentSections(int rightIndex) {
        // Do not change the model from the service, pass the hull back up and set it in the controller
        Hull currHull = getHull();
        Hull hull = YamlMarshallingService.deepCopy(currHull);
        if (hull == null) throw new RuntimeException("Marshalling error deep copying the hull");
        if (rightIndex <= 0 ) throw new IllegalArgumentException("index must be at least 1");
        int leftIndex = rightIndex - 1;
        HullSection leftSection = hull.getHullSections().get(leftIndex);
        HullSection rightSection = hull.getHullSections().get(rightIndex);

        if (leftSection.getSideProfileCurve() instanceof CubicBezierFunction leftBezier &&
                rightSection.getSideProfileCurve() instanceof CubicBezierFunction rightBezier) {

            // Get the updated hull model
            Point2D rightKnot = rightBezier.getKnotPoints().getLast();
            Point2D rightControl = rightBezier.getControlPoints().getLast();
            leftBezier.setX2(rightKnot.getX());
            leftBezier.setY2(rightKnot.getY());
            leftBezier.setControlX2(rightControl.getX());
            leftBezier.setControlY2(rightControl.getY());
            leftSection.setRx(rightSection.getRx());

            // Replace the updated left section and remove the right section
            hull.getHullSections().set(leftIndex, leftSection);
            hull.getHullSections().remove(rightIndex);
        } else throw new IllegalArgumentException("Cannot work with non-bezier hull");

        return hull;
    }
}
