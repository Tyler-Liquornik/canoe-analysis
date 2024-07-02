package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.Hull;
import com.wecca.canoeanalysis.models.HullSection;
import com.wecca.canoeanalysis.models.functions.VertexFormParabola;

import java.util.ArrayList;
import java.util.List;

// This is purely for testing and should not be included in prod
// See: https://www.desmos.com/calculator/ejinswudqi, https://www.desmos.com/calculator/nxrhgy1l8w
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

        double a1 = - 7.0 / 180.0;
        double h1 = 3.0;
        double k1 = 0.35;
        VertexFormParabola hullTopCurve = new VertexFormParabola(a1, h1, k1);

        List<HullSection> sections = new ArrayList<>();

        sections.add(new HullSection(hullLeftEdgeCurve, hullTopCurve, 0.0, 0.5, 0.013, true));
        sections.add(new HullSection(hullBaseProfileCurve, hullTopCurve, 0.5, 5.5, 0.013, false));
        sections.add(new HullSection(hullRightEdgeCurve, hullTopCurve, 5.5, 6.0, 0.013, true));

        return new Hull(1056, 28.82, sections);
    }
}
