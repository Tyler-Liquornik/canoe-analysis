package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.components.graphics.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphicsUtils {
    // Size rules for load graphics (prevents them from rendering too big/small)
    public static double[] acceptedBeamLoadGraphicHeightRange;

    /**
     * Scales a value from the model for dimensioning to be displayed in a container in the UI
     * @param value the value to scale
     * @param UIContainerDistance the length / width of the container
     * @param modelDistance the length / width in the model
     * @return the value scaled
     */
    public static double getScaledFromModelToGraphic(double value, double UIContainerDistance, double modelDistance) {
        return (value / modelDistance) * UIContainerDistance;
    }

    /**
     * Sort graphics by class type
     * @param graphics the list of graphics to sort
     */
    public static void sortGraphics(List<Graphic> graphics) {
        // Define the order to sort by type
        Map<Class<? extends Graphic>, Integer> classOrder = new HashMap<>();
        classOrder.put(Curve.class, 0);
        classOrder.put(TriangleStand.class, 1);
        classOrder.put(Arrow.class, 2);
        classOrder.put(ArrowBoundCurve.class, 3);
        classOrder.put(Beam.class, 4);

        // Sort by type, and then by position
        graphics.sort(Comparator.comparingInt(l -> classOrder.getOrDefault(l.getClass(), -1)));
        graphics.sort(Comparator.comparingDouble(Graphic::getX));
    }

    /**
     * Calculates the ratio of the maximum possible load arrow to the max height of a CurvedProfile
     * @param canoeGraphic the CurvedProfile object
     * @return the loadMaxToHullMaxRatio
     */
    public static double calculateLoadMaxToCurvedProfileMaxRatio(CurvedProfile canoeGraphic) {
        return (acceptedBeamLoadGraphicHeightRange[1] - acceptedBeamLoadGraphicHeightRange[0]) / canoeGraphic.getEncasingRectangle().getHeight();
    }
}
