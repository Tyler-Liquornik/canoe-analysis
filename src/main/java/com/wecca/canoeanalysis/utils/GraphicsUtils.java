package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.components.graphics.*;
import javafx.scene.Node;
import javafx.scene.control.Label;

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
        classOrder.put(CurvedGraphic.class, 0);
        classOrder.put(TriangleStandGraphic.class, 1);
        classOrder.put(ArrowGraphic.class, 2);
        classOrder.put(ArrowBoundCurveGraphic.class, 3);
        classOrder.put(BeamHullGraphic.class, 4);

        // Sort by type, and then by position
        graphics.sort(Comparator.comparingInt(l -> classOrder.getOrDefault(l.getClass(), -1)));
        graphics.sort(Comparator.comparingDouble(Graphic::getX));
    }

    /**
     * Calculates the ratio of the maximum possible load arrow to the max height of a CurvedGraphic
     * @param canoeGraphic the CurvedGraphic object
     * @return the loadMaxToHullMaxRatio
     */
    public static double calculateLoadMaxToCurvedGraphicMaxRatio(FunctionGraphic canoeGraphic) {
        return (acceptedBeamLoadGraphicHeightRange[1] - acceptedBeamLoadGraphicHeightRange[0]) / canoeGraphic.getEncasingRectangle().getHeight();
    }

    /**
     * Sets the given label's text to the projected length of the node as it rotates.
     * The projected length is calculated using Pythagoras' theorem based on the current rotation angle.
     *
     * @param label The label to set the projected length text to.
     * @param node The node representing the canoe graphic.
     * @param length The original length of the canoe (before rotation).
     */
    public static void setProjectedLengthToLabel(Label label, Node node, double length) {
        double rotationInDegrees = node.getRotate();
        double rotationInRadians = Math.toRadians(rotationInDegrees);
        double projectedLength = length * Math.cos(rotationInRadians);
        label.setText(String.format("%.2f m", projectedLength));
    }
}
