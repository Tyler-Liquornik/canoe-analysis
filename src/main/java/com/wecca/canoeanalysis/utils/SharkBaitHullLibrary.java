package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.VertexFormParabolaFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 2024's Shark Bait serves as the base reference for all future canoes with respect to PADDL development
 * This library has a few different versions of shark bait to work with
 */
public class SharkBaitHullLibrary {

    public static final double SHARK_BAIT_LENGTH = 6.0;
    public static double scalingFactor = 1.0;

    /**
     * Note: THIS IS THE NEW MODEL
     * Does not scale (for now) WIP
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     * @return the hull
     */
    // https://www.desmos.com/calculator/waebkhsl43
    public static Hull generateSharkBaitHullFromBezier(double length) {

        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;

        // The code here will stay messy to keep it aligned with the Desmos model for now
        double top = 0.00;
        double knot0 = 0.00;
        double knot1 = 0.50;
        double knot2 = 2.88;
        double knot3 = 5.50;
        double knot4 = 6.00;
        double leftX1 = knot0;
        double leftY1 = top;
        double leftControlX1 = 0.07; // Left (L) Free 1
        double leftControlY1 = -0.21; // Left (L) Free 2
        double leftControlX2 = 0.27; // Left (L) Free 3
        double leftControlY2 = -0.28; // Left (L) Free 4
        double leftX2 = knot1;
        double midLeftY1 = -0.32;  // mid Left (M) Free 1
        double leftY2 = midLeftY1;
        double leftSlope2 = (leftY2 - leftControlY2) / (leftX2 - leftControlX2);
        double midLeftX1 = leftX2;
        double midLeftSlope1 = leftSlope2;
        double midLeftControlX1 = 0.76; // mid Left (M) Free 2
        double midLeftControlY1 = midLeftSlope1 * (midLeftControlX1 - midLeftX1) + midLeftY1;
        double midLeftControlX2 = 2.62; // mid Left (M) Free 3
        double midLeftControlY2 = -0.39; // mid Left (M) Free 4
        double midLeftX2 = knot2;
        double midRightY1 = -0.39; // mid Right (N) Free 1
        double midLeftY2 = midRightY1;
        double midLeftSlope2 = (midLeftY2 - midLeftControlY2) / (midLeftX2 - midLeftControlX2);
        double midRightX1 = midLeftX2;
        double midRightSlope1 = midLeftSlope2;
        double midRightControlX1 = 3.31; // mid Right (N) Free 2
        double midRightControlY1 = midRightSlope1 * (midRightControlX1 - midRightX1) + midRightY1;
        double midRightControlX2 = 5.18; // mid Right (N) Free 3
        double midRightControlY2 = -0.40; // mid Right (N) Free 4
        double midRightX2 = knot3;
        double rightY1 = -0.34; // Right (R) Free 1
        double midRightY2 = rightY1;
        double midRightSlope2 = (midRightY2 - midRightControlY2) / (midRightX2 - midRightControlX2);
        double rightX1 = midRightX2;
        double rightSlope1 = midRightSlope2;
        double rightControlX1 = 5.77; // Right (R) Free 2
        double rightControlY1 = rightSlope1 * (rightControlX1 - rightX1) + rightY1;
        double rightControlX2 = 5.88; // Right (R) Free 3
        double rightControlY2 = -0.19; // Right (R) Free 4
        double rightX2 = knot4;
        double rightY2 = top;

        CubicBezierFunction leftCurve = new CubicBezierFunction(leftX1, leftY1, leftControlX1, leftControlY1, leftControlX2, leftControlY2, leftX2, leftY2);
        CubicBezierFunction midLeftCurve = new CubicBezierFunction(midLeftX1, midLeftY1, midLeftControlX1, midLeftControlY1, midLeftControlX2, midLeftControlY2, midLeftX2, midLeftY2);
        CubicBezierFunction midRightCurve = new CubicBezierFunction(midRightX1, midRightY1, midRightControlX1, midRightControlY1, midRightControlX2, midRightControlY2, midRightX2, midRightY2);
        CubicBezierFunction rightCurve = new CubicBezierFunction(rightX1, rightY1, rightControlX1, rightControlY1, rightControlX2, rightControlY2, rightX2, rightY2);

        // Still using parabola for top view for now (desmos model not made yet)
        double a = - 7.0 / 180.0;
        double h = 3.0;
        double k = 0.35;
        VertexFormParabolaFunction topCurve = new VertexFormParabolaFunction(a, h, k);

        List<HullSection> sections = new ArrayList<>();
        double thickness = 0.013;
        sections.add(new HullSection(leftCurve, topCurve, knot0, knot1, thickness, true));
        sections.add(new HullSection(midLeftCurve, topCurve, knot1, knot2, thickness, false));
        sections.add(new HullSection(midRightCurve, topCurve, knot2, knot3, thickness, false));
        sections.add(new HullSection(rightCurve, topCurve, knot3, knot4, thickness, true));

        return new Hull(1056, 28.82, sections);
    }

    /**
     * Note: THIS IS THE OLD MODEL
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateSharkBaitHullScaledFromParabolas(double length) {
        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;

        // Define hull shape
        double a = 1.0 / (67.0 * scalingFactor);
        double h = 3.0 * scalingFactor;
        double k = -0.4 * scalingFactor;
        VertexFormParabolaFunction hullMidProfileCurve = new VertexFormParabolaFunction(a, h, k);

        double aEdges = 306716.0 / (250000.0 * scalingFactor);
        VertexFormParabolaFunction hullLeftEdgeCurve = new VertexFormParabolaFunction(aEdges, 0.5 * scalingFactor, hullMidProfileCurve.value(0.5 * scalingFactor));
        VertexFormParabolaFunction hullRightEdgeCurve = new VertexFormParabolaFunction(aEdges, 5.5 * scalingFactor, hullMidProfileCurve.value(5.5 * scalingFactor));

        double a1 = - 7.0 / (180.0 * scalingFactor);
        double h1 = 3.0 * scalingFactor;
        double k1 = 0.35 * scalingFactor;
        VertexFormParabolaFunction hullTopCurve = new VertexFormParabolaFunction(a1, h1, k1);

        List<HullSection> sections = new ArrayList<>();
        double thickness = 0.013;
        sections.add(new HullSection(hullLeftEdgeCurve, hullTopCurve, 0.0, 0.5 * scalingFactor, thickness * scalingFactor, true));
        sections.add(new HullSection(hullMidProfileCurve, hullTopCurve, 0.5 * scalingFactor, 5.5 * scalingFactor, thickness * scalingFactor, false));
        sections.add(new HullSection(hullRightEdgeCurve, hullTopCurve, 5.5 * scalingFactor, 6.0 * scalingFactor, thickness * scalingFactor, true));

        return new Hull(1056, 28.82, sections);
    }

    /**
     * Returns a hull with the geometry of the rectangular prism that encases the scaled Shark Bait Hull
     * This means that the hull has no mass of weight, and a self weight distribution of f(x) = 0
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateDefaultHull(double length) {
        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;
        return new Hull(length, 0.4 * scalingFactor, 0.7 * scalingFactor);
    }
}
