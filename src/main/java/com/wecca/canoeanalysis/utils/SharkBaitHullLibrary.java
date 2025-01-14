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
     * Does not scale (for now) <-- TODO
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     * @return the hull
     */
    // https://www.desmos.com/calculator/waebkhsl43
    public static Hull generateSharkBaitHullScaledFromBezier(double length) {

        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;

        // Side view Curve Construction Bezier Spline
        // Redundant lines are for readability, and for synchronization with desmos model
        double top = 0.00;
        double knot0 = 0.00;
        double knot1 = 0.50 * scalingFactor;
        double knot2 = 2.88 * scalingFactor;
        double knot3 = 5.50 * scalingFactor;
        double scaledLength = 6.00 * scalingFactor;

        double leftX1 = knot0;
        double leftY1 = top;
        double leftControlX1 = 0.07 * scalingFactor;
        double leftControlY1 = -0.21 * scalingFactor;
        double leftControlX2 = 0.29 * scalingFactor;
        double leftControlY2 = -0.29 * scalingFactor;
        double leftX2 = knot1;
        double midLeftY1 = -0.32 * scalingFactor;
        double leftY2 = midLeftY1;
        double leftSlope2 = (leftY2 - leftControlY2) / (leftX2 - leftControlX2);

        double midLeftX1 = leftX2;
        double midLeftSlope1 = leftSlope2;
        double midLeftControlX1 = 0.76 * scalingFactor;
        double midLeftControlY1 = midLeftSlope1 * (midLeftControlX1 - midLeftX1) + midLeftY1;
        double midLeftControlX2 = 1.40 * scalingFactor;
        double midLeftControlY2 = -0.39 * scalingFactor;
        double midLeftX2 = knot2;
        double midRightY1 = -0.39 * scalingFactor;
        double midLeftY2 = midRightY1;
        double midLeftSlope2 = (midLeftY2 - midLeftControlY2) / (midLeftX2 - midLeftControlX2);

        double midRightX1 = midLeftX2;
        double midRightSlope1 = midLeftSlope2;
        double midRightControlX1 = 3.31 * scalingFactor;
        double midRightControlY1 = midRightSlope1 * (midRightControlX1 - midRightX1) + midRightY1;
        double midRightControlX2 = 5.18 * scalingFactor;
        double midRightControlY2 = -0.39 * scalingFactor;
        double midRightX2 = knot3;
        double rightY1 = -0.34 * scalingFactor;
        double midRightY2 = rightY1;
        double midRightSlope2 = (midRightY2 - midRightControlY2) / (midRightX2 - midRightControlX2);

        double rightX1 = midRightX2;
        double rightSlope1 = midRightSlope2;
        double rightControlX1 = 5.77 * scalingFactor;
        double rightControlY1 = rightSlope1 * (rightControlX1 - rightX1) + rightY1;
        double rightControlX2 = 5.86 * scalingFactor;
        double rightControlY2 = -0.19 * scalingFactor;
        double rightX2 = scaledLength;
        double rightY2 = top;

        CubicBezierFunction leftCurve = new CubicBezierFunction(leftX1, leftY1, leftControlX1, leftControlY1, leftControlX2, leftControlY2, leftX2, leftY2);
        CubicBezierFunction midLeftCurve = new CubicBezierFunction(midLeftX1, midLeftY1, midLeftControlX1, midLeftControlY1, midLeftControlX2, midLeftControlY2, midLeftX2, midLeftY2);
        CubicBezierFunction midRightCurve = new CubicBezierFunction(midRightX1, midRightY1, midRightControlX1, midRightControlY1, midRightControlX2, midRightControlY2, midRightX2, midRightY2);
        CubicBezierFunction rightCurve = new CubicBezierFunction(rightX1, rightY1, rightControlX1, rightControlY1, rightControlX2, rightControlY2, rightX2, rightY2);

        // Still using parabola for now until desmos model is created
        double a = - 7.0 / (180.0 * scalingFactor);
        double h = 3.0 * scalingFactor;
        double k = 0.35 * scalingFactor;
        VertexFormParabolaFunction topCurve = new VertexFormParabolaFunction(a, h, k);

        List<HullSection> sections = new ArrayList<>();
        double thickness = 0.013 * scalingFactor;
        sections.add(new HullSection(leftCurve, topCurve, knot0, knot1, thickness, true));
        sections.add(new HullSection(midLeftCurve, topCurve, knot1, knot2, thickness, false));
        sections.add(new HullSection(midRightCurve, topCurve, knot2, knot3, thickness, false));
        sections.add(new HullSection(rightCurve, topCurve, knot3, scaledLength, thickness, true));

        return new Hull(1056, 28.82, sections);
    }

    /**
     * @deprecated by generateSharkBaitHullScaledFromBezier
     * Note: THIS IS A DIFFERENT VERSION OF THE OLD MODEL
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     *
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateSharkBaitHullScaledFromParabolasC1Smooth(double length) {

        // The two systems solved for parameters (paste into ChatGPT, just type "render"):
        // \frac{1}{67t}\left(\frac{1}{2}t-3t\right)^2-0.4t=a\left(\frac{1}{2}t-ht\right)^2-kt,\:\frac{2}{67t}\left(\frac{1}{2}t-3t\right)=2a\left(\frac{1}{2}t-ht\right),\:ah^2t^2-kt=0
        // \frac{1}{67t}\left(\frac{11}{2}t-3t\right)^2-0.4t=a\left(\frac{11}{2}t-ht\right)^2-kt,\:\frac{2}{67t}\left(\frac{11}{2}t-3t\right)=2a\left(\frac{11}{2}t-ht\right),\:a\left(6t-ht\right)^2-kt=0

        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;

        // Define hull shape
        double aL = (361.0 / (335.0 * scalingFactor));
        double hL = 193.0 / 361.0;
        double kL = 37249.0 / 120935.0;
        VertexFormParabolaFunction hullLeftEdgeCurve = new VertexFormParabolaFunction(aL, hL * scalingFactor, -kL * scalingFactor);

        double aR = aL;
        double hR = 1973.0 / 361.0;
        double kR = kL;
        VertexFormParabolaFunction hullRightEdgeCurve = new VertexFormParabolaFunction(aR, hR * scalingFactor, -kR * scalingFactor);

        double aM = 1.0 / (67.0 * scalingFactor);
        double hM = 3.0 * scalingFactor;
        double kM = -0.4 * scalingFactor;
        VertexFormParabolaFunction hullMidProfileCurve = new VertexFormParabolaFunction(aM, hM, kM);

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
     * @deprecated by generateSharkBaitHullScaledFromBezier, generateSharkBaitHullScaledFromParabolasC1Smooth
     * Note: THIS IS THE OLD MODEL
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateSharkBaitHullScaledFromParabolasC0Smooth(double length) {
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
     * This hull has no mass or weight, and a self weight distribution of f(x) = 0
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateDefaultHull(double length) {
        // Scale compared to the actual length of Shark Bait
        scalingFactor = length / SHARK_BAIT_LENGTH;
        return new Hull(length, 0.4 * scalingFactor, 0.7 * scalingFactor);
    }
}
