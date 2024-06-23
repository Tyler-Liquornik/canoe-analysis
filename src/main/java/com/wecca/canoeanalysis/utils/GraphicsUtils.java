package com.wecca.canoeanalysis.utils;

public class GraphicsUtils {
    /**
     * Scales an x value from a larger GUI container down to the canoe's for GUI positioning
     * @param x the value to scale
     * @param containerWidth the width of the
     * @param canoeLength the length of the canoe
     * @return the value scaled
     */
    public static double getXScaled(double x, double containerWidth, double canoeLength) {
        return (x/ canoeLength) * containerWidth;
    }
}
