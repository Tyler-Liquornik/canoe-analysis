package com.wecca.canoeanalysis.utils;

public class GraphicsUtils {
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
}
