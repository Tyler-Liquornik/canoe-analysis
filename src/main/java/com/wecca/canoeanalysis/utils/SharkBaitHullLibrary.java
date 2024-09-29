package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
import com.wecca.canoeanalysis.models.function.CubicBezierFunction;
import com.wecca.canoeanalysis.models.function.VertexFormParabola;

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
    public static Hull generateSharkBaitHullFromBezier() {

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
        double baseLeftY1 = -0.32;  // Base Left (M) Free 1
        double leftY2 = baseLeftY1;
        double leftSlope2 = (leftY2 - leftControlY2) / (leftX2 - leftControlX2);
        double baseLeftX1 = leftX2;
        double baseLeftSlope1 = leftSlope2;
        double baseLeftControlX1 = 0.76; // Base Left (M) Free 2
        double baseLeftControlY1 = baseLeftSlope1 * (baseLeftControlX1 - baseLeftX1) + baseLeftY1;
        double baseLeftControlX2 = 2.62; // Base Left (M) Free 3
        double baseLeftControlY2 = -0.39; // Base Left (M) Free 4
        double baseLeftX2 = knot2;
        double baseRightY1 = -0.39; // Base Right (N) Free 1
        double baseLeftY2 = baseRightY1;
        double baseLeftSlope2 = (baseLeftY2 - baseLeftControlY2) / (baseLeftX2 - baseLeftControlX2);
        double baseRightX1 = baseLeftX2;
        double baseRightSlope1 = baseLeftSlope2;
        double baseRightControlX1 = 3.31; // Base Right (N) Free 2
        double baseRightControlY1 = baseRightSlope1 * (baseRightControlX1 - baseRightX1) + baseRightY1;
        double baseRightControlX2 = 5.18; // Base Right (N) Free 3
        double baseRightControlY2 = -0.40; // Base Right (N) Free 4
        double baseRightX2 = knot3;
        double rightY1 = -0.34; // Right (R) Free 1
        double baseRightY2 = rightY1;
        double baseRightSlope2 = (baseRightY2 - baseRightControlY2) / (baseRightX2 - baseRightControlX2);
        double rightX1 = baseRightX2;
        double rightSlope1 = baseRightSlope2;
        double rightControlX1 = 5.77; // Right (R) Free 2
        double rightControlY1 = rightSlope1 * (rightControlX1 - rightX1) + rightY1;
        double rightControlX2 = 5.88; // Right (R) Free 3
        double rightControlY2 = -0.19; // Right (R) Free 4
        double rightX2 = knot4;
        double rightY2 = top;

        CubicBezierFunction leftCurve = new CubicBezierFunction(leftX1, leftY1, leftControlX1, leftControlY1, leftControlX2, leftControlY2, leftX2, leftY2);
        CubicBezierFunction baseLeftCurve = new CubicBezierFunction(baseLeftX1, baseLeftY1, baseLeftControlX1, baseLeftControlY1, baseLeftControlX2, baseLeftControlY2, baseLeftX2, baseLeftY2);
        CubicBezierFunction baseRightCurve = new CubicBezierFunction(baseRightX1, baseRightY1, baseRightControlX1, baseRightControlY1, baseRightControlX2, baseRightControlY2, baseRightX2, baseRightY2);
        CubicBezierFunction rightCurve = new CubicBezierFunction(rightX1, rightY1, rightControlX1, rightControlY1, rightControlX2, rightControlY2, rightX2, rightY2);

        // Still using parabola for top view for now (desmos model not made yet)
        double a = - 7.0 / 180.0;
        double h = 3.0;
        double k = 0.35;
        VertexFormParabola topCurve = new VertexFormParabola(a, h, k);

        List<HullSection> sections = new ArrayList<>();
        double thickness = 0.013;
        sections.add(new HullSection(leftCurve, topCurve, knot0, knot1, thickness, true));
        sections.add(new HullSection(baseLeftCurve, topCurve, knot1, knot2, thickness, false));
        sections.add(new HullSection(baseRightCurve, topCurve, knot2, knot3, thickness, false));
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
        VertexFormParabola hullBaseProfileCurve = new VertexFormParabola(a, h, k);

        double aEdges = 306716.0 / (250000.0 * scalingFactor);
        VertexFormParabola hullLeftEdgeCurve = new VertexFormParabola(aEdges, 0.5 * scalingFactor, hullBaseProfileCurve.value(0.5 * scalingFactor));
        VertexFormParabola hullRightEdgeCurve = new VertexFormParabola(aEdges, 5.5 * scalingFactor, hullBaseProfileCurve.value(5.5 * scalingFactor));

        double a1 = - 7.0 / (180.0 * scalingFactor);
        double h1 = 3.0 * scalingFactor;
        double k1 = 0.35 * scalingFactor;
        VertexFormParabola hullTopCurve = new VertexFormParabola(a1, h1, k1);

        List<HullSection> sections = new ArrayList<>();
        double thickness = 0.013;
        sections.add(new HullSection(hullLeftEdgeCurve, hullTopCurve, 0.0, 0.5 * scalingFactor, thickness * scalingFactor, true));
        sections.add(new HullSection(hullBaseProfileCurve, hullTopCurve, 0.5 * scalingFactor, 5.5 * scalingFactor, thickness * scalingFactor, false));
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
