package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.controllers.modules.HullBuilderController;
import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullProperties;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.Range;
import com.wecca.canoeanalysis.models.function.Section;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import com.wecca.canoeanalysis.utils.SectionPropertyMapEntry;
import com.wecca.canoeanalysis.utils.HullLibrary;
import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import lombok.NonNull;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

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
    public static final double THICKNESS = HullLibrary.generateDefaultHull(HullLibrary.SHARK_BAIT_LENGTH).getMaxThickness();

    /**
     * In this service, we will prefer to only process and return data for the hull model
     * Setting the processed data to the hull model should occur instead at the controller level
     * @return a new memory reference to the hull equivalent to the one in HullBuilderController state
     */
    private static Hull getHull() {
        if (hullBuilderController == null)
            throw new IllegalStateException("HullBuilderController is not set");
        return MarshallingService.deepCopy(hullBuilderController.getHull());
    }

    /**
     * Calculates the maximum radius (r) such that a point at (r, theta) from an origin/knot (xO, yO)
     * remains within the rectangular bounds defined by x = 0, x = L, y = 0, y = -h.
     * @param knot the knot point which acts as the origin
     * @param thetaKnown the known angle in degrees (relative to the origin) at which the point lies.
     * @param selectedBezierSegmentIndex the index of the hullSection in the hull in which to bound the radius
     *                                 (not necessarily the current state hullSectionIndex in HullBuilderController)
     * @return The maximum radius (r) such that the point stays within the bounds.
     */
    public static double calculateMaxR(Point2D knot, double thetaKnown, int selectedBezierSegmentIndex) {
        Hull hull = getHull();
        double hullHeight = -hull.getMaxHeight();
        CubicBezierFunction bezier = hull.getSideViewSegments().get(selectedBezierSegmentIndex);
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
     * @param selectedBezierSegmentIndex The index of the hull section to process for bounding
     *                                  (not necessarily the current state hullSectionIndex in HullBuilderController)
     * @return A two-element array containing the minimum and maximum valid theta values
     */
    public static double[] calculateThetaBounds(Point2D knot, double rKnown, double currTheta, boolean isLeft, boolean boundWithAdjacentSections, int selectedBezierSegmentIndex) {
        Hull hull = getHull();
        CubicBezierFunction bezier = hull.getSideViewSegments().get(selectedBezierSegmentIndex);
        double l = bezier.getX2() - bezier.getX1();
        double h = -hull.getMaxHeight();
        Rectangle boundingRect = new Rectangle(bezier.getX1(), 0.0, l, h);

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
        int selectedBezierSegmentIndex = hullBuilderController.getSelectedBezierSegmentIndex();
        double thetaMin = isLeft ? 180 : 0;
        double thetaMax = isLeft ? 360 : 180;

        int adjacentHullSectionIndex = selectedBezierSegmentIndex + (isLeft ? -1 : 1);
        boolean hasAdjacentSection = isLeft
                ? selectedBezierSegmentIndex > 0
                : selectedBezierSegmentIndex < hull.getSideViewSegments().size() - 1;

        if (hasAdjacentSection) {
            CubicBezierFunction adjacentSegment = hull.getSideViewSegments().get(adjacentHullSectionIndex);
            Point2D siblingPoint = isLeft
                    ? adjacentSegment.getControlPoints().getLast()
                    : adjacentSegment.getControlPoints().getFirst();
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
     * @param adjacentBezier the adjacent section (either on the left or right) to adjust
     *                       the correct section must be passed in, this logic is not in the method
     * @param adjustLeftOfSelected whether to adjust the adjacent left section (otherwise right)
     * @param deltaX the amount by which to adjust the control point x in the adjacent section
     * @param deltaY the amount by which to adjust the control point y in the adjacent section
     */
    public static void adjustAdjacentSectionControlPoint(CubicBezierFunction adjacentBezier, boolean adjustLeftOfSelected, double deltaX, double deltaY) {
        // Adjust the control point by the delta
        if (adjustLeftOfSelected) {
            adjacentBezier.setControlX2(adjacentBezier.getControlX2() + deltaX);
            adjacentBezier.setControlY2(adjacentBezier.getControlY2() + deltaY);
        } else {
            adjacentBezier.setControlX1(adjacentBezier.getControlX1() + deltaX);
            adjacentBezier.setControlY1(adjacentBezier.getControlY1() + deltaY);
        }
    }

    /**
     * Computes the new control point and deltas for adjusting the control point of an adjacent hull section
     * to maintain C1 smoothness. The computation is based on polar coordinate transformations.
     *
     * @param adjacentBezier The adjacent bezier spline segment being adjusted.
     * @param isLeftAdjacentSection True if adjusting the left-adjacent section, false otherwise.
     * @param newThetaVal The new angle (theta) value for the selected control point.
     * @param selectedKnot The knot point used as the reference for polar transformations.
     * @return A Point2D array where:
     *         index 0 contains the deltaX,
     *         index 1 contains the deltaY.
     */
    public static Point2D calculateAdjacentControlDeltas(CubicBezierFunction adjacentBezier, boolean isLeftAdjacentSection, double newThetaVal, Point2D selectedKnot) {
        // Determine old control point and polar radius (r)
        Point2D adjacentSectionOldControl;
        double adjacentSectionRadius;
        double adjacentSectionNewThetaVal = (newThetaVal + 180) % 360;

        if (isLeftAdjacentSection)
            adjacentSectionOldControl = adjacentBezier.getControlPoints().getLast();
        else
            adjacentSectionOldControl = adjacentBezier.getControlPoints().getFirst();
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
        CubicBezierFunction bezier = hullBuilderController.getSelectedBezierSegment();
        int hullSectionIndex = hullBuilderController.getSelectedBezierSegmentIndex();

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
        CubicBezierFunction bezier = hullBuilderController.getSelectedBezierSegment();
        int hullSectionIndex = hullBuilderController.getSelectedBezierSegmentIndex();
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
        if (!(thetaParameterIndex == 1 || thetaParameterIndex == 3)) throw new IllegalArgumentException("thetaParameterIndex must be 1 or 3");
        CubicBezierFunction bezier = hullBuilderController.getSelectedBezierSegment();
        int hullSectionIndex = hullBuilderController.getSelectedBezierSegmentIndex();
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
        CubicBezierFunction bezier = hullBuilderController.getSelectedBezierSegment();

        // Convert control points to polar coordinates
        List<Point2D> knotAndControlPoints = bezier.getKnotAndControlPoints();
        Point2D polarL = CalculusUtils.toPolar(knotAndControlPoints.get(1), knotAndControlPoints.get(0));
        Point2D polarR = CalculusUtils.toPolar(knotAndControlPoints.get(2), knotAndControlPoints.get(3));
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
        int selectedBezierSegmentIndex = hullBuilderController.getSelectedBezierSegmentIndex();

        // Get knot points
        CubicBezierFunction selectedBezier = hull.getSideViewSegments().get(selectedBezierSegmentIndex);
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
        boolean adjustingLeft = parameterIndex == 1 && selectedBezierSegmentIndex > 0;
        boolean adjustingRight = parameterIndex == 3 && selectedBezierSegmentIndex < (hull.getSideViewSegments().size() - 1);

        if (adjustingLeft || adjustingRight) {
            int adjacentIndex = selectedBezierSegmentIndex + (adjustingLeft ? -1 : 1);
            CubicBezierFunction adjacentBezier = hull.getSideViewSegments().get(adjacentIndex);
            Point2D selectedKnot = adjustingLeft ? selectedLKnot : selectedRKnot;

            // Calculate deltas for the adjacent control point
            Point2D deltas = calculateAdjacentControlDeltas(
                    adjacentBezier, adjustingLeft, newParameterValue, selectedKnot);

            // Adjust the control point of the adjacent section
            adjustAdjacentSectionControlPoint(adjacentBezier, adjustingLeft, deltas.getX(), deltas.getY());
            hull.getSideViewSegments().set(adjacentIndex, adjacentBezier);
        }
        return hull;
    }

    public static Point2D getEditableKnotPoint(double functionSpaceX) {
        Hull hull = getHull();
        List<CubicBezierFunction> sideViewSegments = hull.getSideViewSegments();
        List<Range> overlaySections = hullBuilderController.getOverlaySections();

        for (int i = 0; i < overlaySections.size(); i++) {
            Range currOverlay = overlaySections.get(i);

            // Determine the "deleting section" if one exists.
            Section deletingSection;
            if (i != overlaySections.size() - 1) {
                Range nextOverlay = overlaySections.get(i + 1);
                double deletingSectionX = (currOverlay.getX() < currOverlay.getRx()) ? currOverlay.getRx() : currOverlay.getX();
                double deletingSectionRx = (nextOverlay.getX() < nextOverlay.getRx()) ? nextOverlay.getX() : nextOverlay.getRx();
                deletingSection = new Section(deletingSectionX, deletingSectionRx);
            } else deletingSection = null;

            // Use the new model field to extract geometry.
            CubicBezierFunction bezier = sideViewSegments.get(i);
            double lKnotX = bezier.getX1();
            double rKnotX = bezier.getX2();

            // Check if functionSpaceX lies within the knot interval.
            if (currOverlay.getX() > currOverlay.getRx() && lKnotX <= functionSpaceX && functionSpaceX <= rKnotX) {
                double lControlX = bezier.getControlX1();
                double rControlX = bezier.getControlX2();
                double midpoint = rControlX + ((lControlX - rControlX) / 2);

                if (functionSpaceX < midpoint) {
                    if (i == 0) return null; // Cannot delete the first knot.
                    double knotY = bezier.value(lKnotX);
                    return new Point2D(lKnotX, knotY);
                } else {
                    if (i == overlaySections.size() - 1) return null; // Cannot delete the last knot.
                    CubicBezierFunction nextBezier = sideViewSegments.get(i + 1);
                    double knotY = nextBezier.value(rKnotX);
                    return new Point2D(rKnotX, knotY);
                }
            }
            // Check if functionSpaceX lies within a deletion zone.
            else if (deletingSection != null &&
                    deletingSection.getX() <= functionSpaceX && functionSpaceX <= deletingSection.getRx()) {
                double knotX = sideViewSegments.stream()
                        .flatMap(seg -> seg.getKnotPoints().stream())
                        .mapToDouble(Point2D::getX)
                        .distinct()
                        .filter(x -> x >= deletingSection.getX() && x <= deletingSection.getRx())
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Cannot find knot point in the section"));
                double knotY = bezier.value(knotX);
                return new Point2D(knotX, knotY);
            }
        }
        // No matching section found.
        return null;
    }

    /**
     * Updates the hull geometry (new model) by dragging a knot point in the side view.
     * Only the side–view Bézier curve(s) are updated; the top–view curves remain unchanged.
     * The new knot is clamped within a rectangular envelope (global vertical bounds, and per–segment horizontal bounds).
     * The control point is repositioned to preserve its original polar offset (distance and angle) relative to the knot.
     * If the candidate control point falls outside its allowed horizontal (from control points) or vertical (global)
     * envelope, it is retracted along its ray until it lies on the boundary.
     * @param knotPos the original position of the knot in the side view
     * @param newKnotPos the new (requested) position for the knot in the side view
     * @return a new Hull instance with updated side–view curves and refreshed hull properties
     */
    public static Hull dragKnotPoint(@NonNull Point2D knotPos, @NonNull Point2D newKnotPos) {
        Hull hull = getHull();
        double eps = OPEN_INTERVAL_TOLERANCE;
        double globalMaxY = -eps;
        double globalMinY = -hull.getMaxHeight() + eps;
        boolean isDraggingLeft = newKnotPos.getX() < knotPos.getX();
        double plusMinusEps = !isDraggingLeft ? eps : -eps;
        double adjacentSectionPointX = knotPos.getX() + plusMinusEps;
        CubicBezierFunction adjacentBezier = CalculusUtils.getSegmentForX(hull.getSideViewSegments(), adjacentSectionPointX);
        double outerXBoundary = isDraggingLeft ? adjacentBezier.getControlX1() : adjacentBezier.getControlX2(); // boundary against which the user might be dragging
        outerXBoundary -= 2 * plusMinusEps;

        // Clamp new knot position vertically using global vertical bounds.
        // For horizontal, we allow the new knot to be any value (it might be moved by the user)
        double clampedNewKnotX;
        if (isDraggingLeft) clampedNewKnotX = Math.max(newKnotPos.getX(), outerXBoundary);
        else clampedNewKnotX = Math.min(newKnotPos.getX(), outerXBoundary);
        double clampedNewKnotY = Math.max(hull.getLength() * eps + globalMinY, Math.min(newKnotPos.getY(), hull.getLength() * globalMaxY));
        Point2D clampedNewKnot = new Point2D(clampedNewKnotX, clampedNewKnotY);

        // Locate the segment whose left knot matches the one being dragged.
        for (int i = 0; i < hull.getSideViewSegments().size(); i++) {
            CubicBezierFunction bezier = hull.getSideViewSegments().get(i);
            if (bezier.getKnotPoints().getFirst().distance(knotPos) < 1e-6) {
                // Update the current segment's knot to the new clamped position.
                bezier.setX1(clampedNewKnot.getX());
                bezier.setY1(clampedNewKnot.getY());

                // Compute the original offset vector from the old knot to its control point.
                Point2D oldControl = new Point2D(bezier.getControlX1(), bezier.getControlY1());
                Point2D offset = oldControl.subtract(knotPos); // original polar offset

                // Candidate new control point preserving the same offset.
                Point2D candidateControl = clampedNewKnot.add(offset);

                // Determine allowed horizontal bounds for the current segment's left control point.
                double allowedMinX = bezier.getKnotPoints().getFirst().getX();
                double allowedMaxX = bezier.getControlX2() - eps; // from this segment’s right control point

                // Compute horizontal & vertical scale factor
                candidateControl = adjustCandidateControl(globalMaxY, globalMinY, clampedNewKnot, offset, candidateControl, allowedMinX, allowedMaxX);
                bezier.setControlX1(candidateControl.getX());
                bezier.setControlY1(candidateControl.getY());

                // Update left-adjacent segment if it exists.
                if (i > 0) {
                    CubicBezierFunction prevBezier = hull.getSideViewSegments().get(i - 1);
                    prevBezier.setX2(clampedNewKnot.getX());
                    prevBezier.setY2(clampedNewKnot.getY());
                    Point2D oldControlPrev = new Point2D(prevBezier.getControlX2(), prevBezier.getControlY2());
                    Point2D offsetPrev = oldControlPrev.subtract(knotPos);
                    Point2D candidateControlPrev = clampedNewKnot.add(offsetPrev);
                    // Allowed horizontal bounds for the left-adjacent segment's right control point:
                    double prevAllowedMinX = prevBezier.getControlX1() + eps;
                    double prevAllowedMaxX = clampedNewKnot.getX(); // new knot x becomes upper bound
                    candidateControlPrev = adjustCandidateControl(globalMaxY, globalMinY, clampedNewKnot, offsetPrev, candidateControlPrev, prevAllowedMinX, prevAllowedMaxX);
                    prevBezier.setControlX2(candidateControlPrev.getX());
                    prevBezier.setControlY2(candidateControlPrev.getY());
                }

                // Refresh hull properties (bulkhead and thickness maps) using helper methods.
                return updateHullProperties(hull);
            }
        }
        throw new IllegalArgumentException("No side-view knot found at position: " + knotPos);
    }


    /**
     * This helper method ensures that the candidate control point, computed as
     * (clampedKnotPos + offsetPrev), falls within the allowed horizontal range [prevAllowedMinX, prevAllowedMaxX]
     * and within the global vertical bounds [globalMinY, globalMaxY]. If the candidate is out-of-bounds
     * along the offset vector, scale factors for the x and y components are computed, and the candidate
     * is retracted along its ray (using the smaller scale factor) so that it lies exactly on the boundary.
     */
    private static Point2D adjustCandidateControl(double globalMaxY, double globalMinY, Point2D clampedKnotPos, Point2D offsetPrev, Point2D candidateControlPrev, double prevAllowedMinX, double prevAllowedMaxX) {
        double scaleXPrev = 1.0;
        if (offsetPrev.getX() != 0) {
            if (candidateControlPrev.getX() < prevAllowedMinX) scaleXPrev = (prevAllowedMinX - clampedKnotPos.getX()) / offsetPrev.getX();
            else if (candidateControlPrev.getX() > prevAllowedMaxX) scaleXPrev = (prevAllowedMaxX - clampedKnotPos.getX()) / offsetPrev.getX();
        }
        double scaleYPrev = 1.0;
        if (offsetPrev.getY() != 0) {
            if (candidateControlPrev.getY() < globalMinY) scaleYPrev = (globalMinY - clampedKnotPos.getY()) / offsetPrev.getY();
            else if (candidateControlPrev.getY() > globalMaxY) scaleYPrev = (globalMaxY - clampedKnotPos.getY()) / offsetPrev.getY();
        }
        double scalePrev = Math.min(scaleXPrev, scaleYPrev);
        if (scalePrev < 1.0) candidateControlPrev = clampedKnotPos.add(offsetPrev.multiply(scalePrev));
        return candidateControlPrev;
    }

    /**
     * Helper method to update the hull properties when the x coordinates of the side view knots change
     * Note: This is fine to not dopy the hull because it's a helper (i.e. it's private!)
     * @param hull the hull to update
     * @return the updated hull
     */
    public static Hull updateHullProperties(Hull hull) {
        List<CubicBezierFunction> sideView = hull.getSideViewSegments();
        // Thickness is the previous hull thickness average across map entries or 13mm as a fallback
        double thickness = THICKNESS;
        if (!hull.getHullProperties().getThicknessMap().isEmpty()) {
            try {
                thickness = hull.getHullProperties().getThicknessMap().stream()
                        .mapToDouble(tm ->
                                Double.parseDouble(tm.getValue()))
                        .average()
                        .orElseThrow(() -> new RuntimeException("Error calculating average hull thickness"));
            } catch (Exception e) {e.printStackTrace();}
        }
        // Update the maps using helper methods
        hull.getHullProperties().setThicknessMap(HullLibrary.buildUniformThicknessList(sideView, thickness));
        hull.getHullProperties().setBulkheadMap(HullLibrary.buildDefaultBulkheadList(sideView));
        return hull;
    }

    /**
     * Adds a new knot point to the hull by splitting the relevant Bézier section with de Casteljau's algorithm at the knot's X-coordinate.
     * 1. Finds the segment (by index) in which the new knot’s x–coordinate lies.
     * 2. Splits that segment’s side view and top view curves using de Casteljau’s algorithm.
     * 3. Updates the thickness and bulkhead maps for the split segment.
     * 4. Rebuilds new lists of side and top segments by inserting the two new segments in place of the old one.
     * 5. Constructs and returns a new Hull via the new model constructor.
     * @param knotPointToAdd the new knot point (x,y) on the existing curve
     * @return a new Hull with the updated side and top segments and updated property maps
     * @throws IllegalArgumentException if the new knot’s x–coordinate is out of range.
     */
    public static Hull addKnotPoint(@NonNull Point2D knotPointToAdd) {
        // Get a deep copy of the current hull.
        Hull hull = getHull();
        if (hull == null) throw new RuntimeException("Marshalling error deep copying the hull");
        double newX = knotPointToAdd.getX();

        // Get current side and top view segments and property maps.
        List<CubicBezierFunction> oldSideSegments = hull.getSideViewSegments();
        List<CubicBezierFunction> oldTopSegments = hull.getTopViewSegments();
        List<SectionPropertyMapEntry> oldThicknessMap = hull.getHullProperties().getThicknessMap();
        List<SectionPropertyMapEntry> oldBulkheadMap = hull.getHullProperties().getBulkheadMap();

        // Find the segment index whose interval [x, rx] covers newX.
        int splitIndex = -1;
        for (int i = 0; i < oldSideSegments.size(); i++) {
            CubicBezierFunction seg = oldSideSegments.get(i);
            if (newX >= seg.getX1() && newX <= seg.getRx()) {
                splitIndex = i;
                break;
            }
        }
        if (splitIndex < 0) throw new IllegalArgumentException(String.format("x = %.3f is out of range for every section.", newX));

        // Split the side–view segment
        CubicBezierFunction oldSide = oldSideSegments.get(splitIndex);
        double t = oldSide.getT(newX);
        CubicBezierFunction[] splitSide = deCasteljauBezierSplit(oldSide, t);
        CubicBezierFunction leftSide = splitSide[0];
        CubicBezierFunction rightSide = splitSide[1];

        // Clamp the control points if needed.
        // (Assume adjustBezierWithMinKnot is already defined for the new model)
        // Find global min among all side knots:
        Point2D globalMinSideKnot = hull.getSideViewSegments().stream()
                .flatMap(cbf -> cbf.getKnotPoints().stream())
                .min(Comparator.comparingDouble(Point2D::getY))
                .orElseThrow(() -> new RuntimeException("No minimum side knot found"));
        double globalMinY = globalMinSideKnot.getY();
        adjustBezierWithMinKnot(leftSide, globalMinY, splitIndex);
        adjustBezierWithMinKnot(rightSide, globalMinY, splitIndex);

        // Split the top–view segment
        CubicBezierFunction oldTop = oldTopSegments.get(splitIndex);
        double tTop = oldTop.getT(newX);
        CubicBezierFunction[] splitTop = deCasteljauBezierSplit(oldTop, tTop);
        CubicBezierFunction leftTop = splitTop[0];
        CubicBezierFunction rightTop = splitTop[1];

        // Find global min among all top knots:
        Point2D globalMinTopKnot = hull.getSideViewSegments().stream()
                .flatMap(cbf -> cbf.getKnotPoints().stream())
                .min(Comparator.comparingDouble(Point2D::getY))
                .orElseThrow(() -> new RuntimeException("No minimum top knot found"));
        double globalMinYTop = globalMinTopKnot.getY();
        adjustBezierWithMinKnot(leftTop, globalMinYTop, splitIndex);
        adjustBezierWithMinKnot(rightTop, globalMinYTop, splitIndex);

        // Rebuild the lists for side segments, top segments, and maps
        List<CubicBezierFunction> newSideSegments = new ArrayList<>(oldSideSegments.size() + 1);
        List<CubicBezierFunction> newTopSegments = new ArrayList<>(oldTopSegments.size() + 1);
        List<SectionPropertyMapEntry> newThicknessMap = new ArrayList<>(oldThicknessMap.size() + 1);
        List<SectionPropertyMapEntry> newBulkheadMap = new ArrayList<>(oldBulkheadMap.size() + 1);

        // Loop over the old lists, and when we reach the split index, insert the two new segments and map entries.
        for (int i = 0; i < oldSideSegments.size(); i++) {
            if (i == splitIndex) {
                // For the split segment, get its original map entries.
                SectionPropertyMapEntry oldThickness = oldThicknessMap.get(i);
                SectionPropertyMapEntry oldBulkhead = oldBulkheadMap.get(i);
                SectionPropertyMapEntry leftThicknessEntry = new SectionPropertyMapEntry(oldThickness.getX(), newX, oldThickness.getValue());
                SectionPropertyMapEntry rightThicknessEntry = new SectionPropertyMapEntry(newX, oldThickness.getRx(), oldThickness.getValue());
                SectionPropertyMapEntry leftBulkheadEntry = new SectionPropertyMapEntry(oldBulkhead.getX(), newX, oldBulkhead.getValue());
                SectionPropertyMapEntry rightBulkheadEntry = new SectionPropertyMapEntry(newX, oldBulkhead.getRx(), oldBulkhead.getValue());
                newSideSegments.add(leftSide);
                newSideSegments.add(rightSide);
                newTopSegments.add(leftTop);
                newTopSegments.add(rightTop);
                newThicknessMap.add(leftThicknessEntry);
                newThicknessMap.add(rightThicknessEntry);
                newBulkheadMap.add(leftBulkheadEntry);
                newBulkheadMap.add(rightBulkheadEntry);
            }
            // Copy over unchanged segments and map entries.
            else {
                newSideSegments.add(oldSideSegments.get(i));
                newTopSegments.add(oldTopSegments.get(i));
                newThicknessMap.add(oldThicknessMap.get(i));
                newBulkheadMap.add(oldBulkheadMap.get(i));
            }
        }
        HullProperties newProps = new HullProperties(newThicknessMap, newBulkheadMap);
        return new Hull(hull.getConcreteDensity(), hull.getBulkheadDensity(), newProps, newSideSegments, newTopSegments);
    }


    /**
     * Adjusts a given CubicBezierFunction after an operation has been done it to ensure its validity
     * If a control point lies below the provided minimum Y (minY), its radial coordinate (r)
     * is clamped into a valid range by computing the maximum allowed radius (via calculateMaxR) for that knot.
     * @param bezier the CubicBezierFunction to adjust
     * @param minY the minimum allowed Y value (from the global minimum knot)
     * @param sectionIndex the index of the section (used in calculateMaxR)
     */
    private static void adjustBezierWithMinKnot(CubicBezierFunction bezier, double minY, int sectionIndex) {
        // Adjust the first control point (relative to the first knot)
        Point2D firstControl = bezier.getControlPoints().getFirst();
        if (firstControl.getY() < minY) {
            double theta = CalculusUtils.toPolar(firstControl, bezier.getKnotPoints().getFirst()).getY();
            double maxR = calculateMaxR(bezier.getKnotPoints().getFirst(), Math.toDegrees(theta), sectionIndex);
            Point2D updatedPoint = CalculusUtils.toCartesian(new Point2D(maxR, theta), bezier.getKnotPoints().getFirst());
            bezier.setControlX1(updatedPoint.getX());
            bezier.setControlY1(updatedPoint.getY());
        }
        // Adjust the last control point (relative to the last knot)
        Point2D lastControl = bezier.getControlPoints().getLast();
        if (lastControl.getY() < minY) {
            double theta = CalculusUtils.toPolar(lastControl, bezier.getKnotPoints().getLast()).getY();
            double maxR = calculateMaxR(bezier.getKnotPoints().getLast(), Math.toDegrees(theta), sectionIndex);
            Point2D updatedPoint = CalculusUtils.toCartesian(new Point2D(maxR, theta), bezier.getKnotPoints().getLast());
            bezier.setControlX2(updatedPoint.getX());
            bezier.setControlY2(updatedPoint.getY());
        }
    }

    /**
     * Splits a cubic Bezier at parameter t in [0..1].
     * @param bezier the curve to split with de Casteljau's algorithm
     * @param t the parameter in [0, 1] which parameterize the Bézier curve at which to split it at
     * @return  an array [leftCurve, rightCurve].
     */
    private static CubicBezierFunction[] deCasteljauBezierSplit(CubicBezierFunction bezier, double t) {
        // Get the bezier's 8 parameters (the x-y pair of each of the 4 points, 2 control and 2 knot points)
        double x0 = bezier.getX1();
        double y0 = bezier.getY1();
        double cx1 = bezier.getControlX1();
        double cy1 = bezier.getControlY1();
        double cx2 = bezier.getControlX2();
        double cy2 = bezier.getControlY2();
        double x3 = bezier.getX2();
        double y3 = bezier.getY2();

        // Execute De Casteljau's algorithm for bezier curve splitting
        // For more: https://en.wikipedia.org/wiki/De_Casteljau%27s_algorithm
        double p01x = lerp(x0,  cx1, t);
        double p01y = lerp(y0,  cy1, t);
        double p12x = lerp(cx1, cx2, t);
        double p12y = lerp(cy1, cy2, t);
        double p23x = lerp(cx2, x3, t);
        double p23y = lerp(cy2, y3, t);
        double p012x = lerp(p01x, p12x, t);
        double p012y = lerp(p01y, p12y, t);
        double p123x = lerp(p12x, p23x, t);
        double p123y = lerp(p12y, p23y, t);
        double p0123x = lerp(p012x, p123x, t);
        double p0123y = lerp(p012y, p123y, t);
        CubicBezierFunction left = new CubicBezierFunction(x0, y0, p01x, p01y, p012x, p012y, p0123x, p0123y);
        CubicBezierFunction right = new CubicBezierFunction(p0123x, p0123y, p123x, p123y, p23x, p23y, x3, y3);
        return new CubicBezierFunction[] {left, right};
    }

    /**
     * Performs linear interpolation between two values.
     * @param a The starting value
     * @param b The ending value
     * @param t The interpolation factor between 0 and 1 (where 0 returns a and 1 returns b)
     * @return The interpolated value between a and b
     */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
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
        for (int i = 0; i < hull.getSideViewSegments().size(); i++) {
            CubicBezierFunction bezier = hull.getSideViewSegments().get(i);
            Point2D knotPoint = bezier.getKnotPoints().getFirst();
            double knotX = knotPoint.getX();
            double knotY = knotPoint.getY();
            if (Math.abs(knotX - knotPointToDelete.getX()) < 1e-6 && Math.abs(knotY - knotPointToDelete.getY()) < 1e-6) {
                if (i > 0) return getHullWithMergedAdjacentSections(i);
                else return null;
            }
        }
        throw new RuntimeException("Failed to delete knot.");
    }

    /**
     * Merges two adjacent hull sections (new model) to maintain continuity after a knot point is deleted.
     * This method updates the side–view segments, top–view segments, and the associated thickness and bulkhead maps.
     * It assumes that the lists in hullProperties match the size of the side–view and top–view segment lists.
     * @param rightIndex the index of the right section (in the side–view list) of the two sections to merge. Must be >= 1.
     * @return the updated Hull with merged segments.
     * @throws IllegalArgumentException if rightIndex is less than 1 or if any segment is non–Bezier.
     */
    private static Hull getHullWithMergedAdjacentSections(int rightIndex) {
        Hull hull = getHull();

        if (rightIndex <= 0) throw new IllegalArgumentException("Index must be at least 1");
        int leftIndex = rightIndex - 1;

        // Retrieve the new-model lists.
        List<CubicBezierFunction> sideSegments = hull.getSideViewSegments();
        List<CubicBezierFunction> topSegments = hull.getTopViewSegments();
        HullProperties props = hull.getHullProperties();
        List<SectionPropertyMapEntry> thicknessMap = props.getThicknessMap();
        List<SectionPropertyMapEntry> bulkheadMap = props.getBulkheadMap();

        // Merge side–view segments
        CubicBezierFunction leftBezier = sideSegments.get(leftIndex);
        CubicBezierFunction rightBezier = sideSegments.get(rightIndex);
        Point2D rightKnot = rightBezier.getKnotPoints().getLast();
        Point2D rightControl = rightBezier.getControlPoints().getLast();
        leftBezier.setX2(rightKnot.getX());
        leftBezier.setY2(rightKnot.getY());
        leftBezier.setControlX2(rightControl.getX());
        leftBezier.setControlY2(rightControl.getY());
        thicknessMap.get(leftIndex).setRx(rightKnot.getX());
        bulkheadMap.get(leftIndex).setRx(rightKnot.getX());

        // Merge top–view segments
        CubicBezierFunction leftTopBezier = topSegments.get(leftIndex);
        CubicBezierFunction rightTopBezier = topSegments.get(rightIndex);
        Point2D rightKnotTop = rightTopBezier.getKnotPoints().getLast();
        Point2D rightControlTop = rightTopBezier.getControlPoints().getLast();
        leftTopBezier.setX2(rightKnotTop.getX());
        leftTopBezier.setY2(rightKnotTop.getY());
        leftTopBezier.setControlX2(rightControlTop.getX());
        leftTopBezier.setControlY2(rightControlTop.getY());
        sideSegments.remove(rightIndex);
        topSegments.remove(rightIndex);
        thicknessMap.remove(rightIndex);
        bulkheadMap.remove(rightIndex);

        // Update the hull
        hull.setSideViewSegments(sideSegments);
        hull.setTopViewSegments(topSegments);
        props.setThicknessMap(thicknessMap);
        props.setBulkheadMap(bulkheadMap);

        // Compute the global minimum knot (by y-value) across all side–view segments.
        Point2D globalMinKnot = sideSegments.stream()
                .flatMap(seg -> seg.getKnotPoints().stream())
                .min(Comparator.comparingDouble(Point2D::getY))
                .orElseThrow(() -> new RuntimeException("No minimum knot found"));
        double minY = globalMinKnot.getY();

        // Adjust all side–view segments in one loop.
        IntStream.range(0, sideSegments.size()).forEach(i -> {
            CubicBezierFunction seg = sideSegments.get(i);
            adjustBezierWithMinKnot(seg, minY, i);
        });

        return hull;
    }

    /**
     * Extracts a subsegment of the given cubic Bézier curve between parameter values t0 and t1.
     * This is achieved by first splitting the curve at t0 (using de Casteljau's algorithm),
     * then splitting the resulting right segment at a normalized parameter value corresponding
     * to t1. The resulting sub–curve exactly represents the portion of the original curve between t0 and t1.
     * @param original the original cubic Bézier function.
     * @param t0 the starting parameter value (in [0,1]).
     * @param t1 the ending parameter value (in [0,1]), with t1 > t0.
     * @return a new CubicBezierFunction representing the extracted segment.
     */
    public static CubicBezierFunction extractBezierSegment(@NonNull CubicBezierFunction original, double t0, double t1) {
        // First, split the original curve at t0.
        CubicBezierFunction[] firstSplit = deCasteljauBezierSplit(original, t0);
        CubicBezierFunction rightCurve = firstSplit[1];
        // Map t1 from the original parameter space to the parameter space of the rightCurve.
        double newT = (t1 - t0) / (1 - t0);
        // Split the rightCurve at newT to extract the subsegment.
        CubicBezierFunction[] secondSplit = deCasteljauBezierSplit(rightCurve, newT);
        // The left segment of the second split is the desired sub–curve.
        return secondSplit[0];
    }
}
