package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.components.graphics.*;
import com.wecca.canoeanalysis.components.graphics.hull.BeamHullGraphic;
import com.wecca.canoeanalysis.components.graphics.load.ArrowBoundCurvedGraphic;
import com.wecca.canoeanalysis.components.graphics.load.ArrowGraphic;
import com.wecca.canoeanalysis.components.graphics.load.TriangleStandGraphic;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;

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
        classOrder.put(ArrowBoundCurvedGraphic.class, 3);
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
     * @param length The original length of the canoe (before rotation).
     * @param angle The current rotation angle in degrees.
     */
    public static void setProjectedLengthToLabel(Label label, double length, double angle) {
        double rotationInRadians = Math.toRadians(angle);
        double projectedLength = length * Math.cos(rotationInRadians);
        label.setText(String.format("%.2f m", projectedLength));
    }

    /**
     * Maps a point from function space to graphic space using the bounds.
     */
    public static Point2D mapToGraphicSpace(Point2D point, Rectangle functionSpace, Rectangle graphicSpace) {
        double funcMinX = functionSpace.getX();
        double funcMaxX = functionSpace.getX() + functionSpace.getWidth();
        double funcMinY = functionSpace.getY();
        double funcMaxY = functionSpace.getY() + functionSpace.getHeight();

        double graphicMinX = graphicSpace.getX();
        double graphicMaxX = graphicSpace.getX() + graphicSpace.getWidth();
        double graphicMinY = graphicSpace.getY();
        double graphicMaxY = graphicSpace.getY() + graphicSpace.getHeight();

        double scaledX = graphicMinX + (point.getX() - funcMinX) / (funcMaxX - funcMinX) * (graphicMaxX - graphicMinX);
        double scaledY = graphicMaxY - (point.getY() - funcMinY) / (funcMaxY - funcMinY) * (graphicMaxY - graphicMinY); // Flip Y-axis for graphic space

        return new Point2D(scaledX, scaledY);
    }
}
