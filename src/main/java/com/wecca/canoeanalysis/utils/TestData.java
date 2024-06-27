package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.Hull;
import com.wecca.canoeanalysis.models.HullSection;
import com.wecca.canoeanalysis.models.functions.VertexFormParabola;

import java.util.ArrayList;
import java.util.List;

// This is purely for testing and should not be included in prod
public class TestData {
    /**
     * Hardcoded temporary test function for the new canoe model TODO: remove once new model finished
     * This will serve as a benchmark to for results comparison for quality assurance with respect to business logic
     */
    public static Hull generateSharkBaitHull() {

        // Define hull shape
        double a = 1.0 / 67.0;
        double h = 3.0;
        double k = -0.4;
        VertexFormParabola hullBaseProfileCurve = new VertexFormParabola(a, h, k);

        double aEdges = 306716.0 / 250000.0;
        VertexFormParabola hullLeftEdgeCurve = new VertexFormParabola(aEdges, 0.5, hullBaseProfileCurve.value(0.5));
        VertexFormParabola hullRightEdgeCurve = new VertexFormParabola(aEdges, 5.5, hullBaseProfileCurve.value(5.5));

        List<HullSection> sections = new ArrayList<>();

        // Left edge curve
        sections.add(new HullSection(hullLeftEdgeCurve, 0.0, 0.1, 0.03, 0.013, true));
        sections.add(new HullSection(hullLeftEdgeCurve, 0.1, 0.2, 0.05, 0.013, true));
        sections.add(new HullSection(hullLeftEdgeCurve, 0.2, 0.3, 0.1, 0.013, true));
        sections.add(new HullSection(hullLeftEdgeCurve, 0.3, 0.4, 0.15, 0.013, true));
        sections.add(new HullSection(hullLeftEdgeCurve, 0.4, 0.5, 0.2, 0.013, true));

        // Generate sections along hullBaseProfileCurve with intervals of 0.1
        sections.add(new HullSection(hullBaseProfileCurve, 0.5, 0.6, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 0.6, 0.7, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 0.7, 0.8, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 0.8, 0.9, 0.35, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 0.9, 1.0, 0.35, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.0, 1.1, 0.35, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.1, 1.2, 0.4, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.2, 1.3, 0.45, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.3, 1.4, 0.5, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.4, 1.5, 0.55, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.5, 1.6, 0.6, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.6, 1.7, 0.65, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.7, 1.8, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.8, 1.9, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 1.9, 2.0, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.0, 2.1, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.1, 2.2, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.2, 2.3, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.3, 2.4, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.4, 2.5, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.5, 2.6, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.6, 2.7, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.7, 2.8, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.8, 2.9, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 2.9, 3.0, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.0, 3.1, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.1, 3.2, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.2, 3.3, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.3, 3.4, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.4, 3.5, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.5, 3.6, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.6, 3.7, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.7, 3.8, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.8, 3.9, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 3.9, 4.0, 0.7, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.0, 4.1, 0.65, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.1, 4.2, 0.6, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.2, 4.3, 0.55, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.3, 4.4, 0.5, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.4, 4.5, 0.45, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.5, 4.6, 0.4, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.6, 4.7, 0.35, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.7, 4.8, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.8, 4.9, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 4.9, 5.0, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 5.0, 5.1, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 5.1, 5.2, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 5.2, 5.3, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 5.3, 5.4, 0.3, 0.013, false));
        sections.add(new HullSection(hullBaseProfileCurve, 5.4, 5.5, 0.3, 0.013, false));

        // Right edge curve
        sections.add(new HullSection(hullRightEdgeCurve, 5.5, 5.6, 0.2, 0.013, true));
        sections.add(new HullSection(hullRightEdgeCurve, 5.6, 5.7, 0.15, 0.013, true));
        sections.add(new HullSection(hullRightEdgeCurve, 5.7, 5.8, 0.1, 0.013, true));
        sections.add(new HullSection(hullRightEdgeCurve, 5.8, 5.9, 0.05, 0.013, true));
        sections.add(new HullSection(hullRightEdgeCurve, 5.9, 6.0, 0.03, 0.013, true));

        return new Hull(1056, 28.82, sections);
    }
}
