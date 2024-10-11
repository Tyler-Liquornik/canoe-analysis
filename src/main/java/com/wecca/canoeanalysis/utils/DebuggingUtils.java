package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.function.Section;

import java.util.function.Function;

public class DebuggingUtils {

    /**
     * Log points to get a block of text theta looks like:
     * 01:44:25.557 [INFO] x: 0.0, y: 0.6160168685657584
     * 01:44:25.557 [INFO] x: 0.005, y: 0.6102826148344151
     * 01:44:25.557 [INFO] x: 0.01, y: 0.6046022417000867
     * 01:44:25.557 [INFO] x: 0.015, y: 0.5989757491627734
     * ... and so on for hundreds (or thousands...) of lines
     *
     * Then paste the whole block on text into log_data in
     * util/matplotlib/scatter-plot.py and run that script to make a plot to visualize the function
     *
     * @param function the function to log points for
     * @param section the section on which to log points for the function
     */
    public static void logPoints(Function<Double, Double> function, Section section) {
        // Sample the function over [start, end] and print x and y values
        double x = section.getX();
        double rx = section.getRx();
        System.out.println("Sampling the function over the domain [" + x + ", " + rx + "]:");
        int numSamples = 100; // Number of sample points
        double step = (rx - x) / numSamples;
        for (int i = 0; i <= numSamples; i++) {
            double xValue = x + i * step;
            double yValue = function.apply(xValue);
            System.out.println("x: " + xValue + ", y: " + yValue);
        }
    }
}
