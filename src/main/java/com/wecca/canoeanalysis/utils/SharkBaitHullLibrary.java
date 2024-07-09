package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.canoe.HullSection;
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
     * Generate the hull for 2024's Shark Bait canoe scaled to the specified length
     * This serves as a placeholder for the user defining their own hull in the hull builder until that is developed
     * @param length the length to scale to
     * @return the scaled hull
     */
    public static Hull generateSharkBaitHullScaled(double length) {
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

        sections.add(new HullSection(hullLeftEdgeCurve, hullTopCurve, 0.0, 0.5 * scalingFactor, 0.013 * scalingFactor, true));
        sections.add(new HullSection(hullBaseProfileCurve, hullTopCurve, 0.5 * scalingFactor, 5.5 * scalingFactor, 0.013 * scalingFactor, false));
        sections.add(new HullSection(hullRightEdgeCurve, hullTopCurve, 5.5 * scalingFactor, 6.0 * scalingFactor, 0.013 * scalingFactor, true));

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
