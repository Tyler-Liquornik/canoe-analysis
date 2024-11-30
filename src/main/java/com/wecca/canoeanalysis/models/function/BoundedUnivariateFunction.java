package com.wecca.canoeanalysis.models.function;

import javafx.geometry.Point2D;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * A function which has bounded y-values on its domain, some subset of R^+
 */
public interface BoundedUnivariateFunction extends UnivariateFunction {
    /**
     * Gets the minimum point of the function within a specific section.
     * @param section the section within which to find the minimum point
     * @return the point (x, y) where the function has its minimum value within the specified section
     */
    default Point2D getMinPoint(Section section) {
        double step = (section.getRx() - section.getX()) / 100;
        double xMin = section.getX();
        double yMin = value(xMin);

        for (double x = section.getX(); x <= section.getRx(); x += step) {
            double y = value(x);
            if (y < yMin) {
                yMin = y;
                xMin = x;
            }
        }

        return new Point2D(xMin, yMin);
    }

    /**
     * Gets the maximum point of the function within a specific section.
     * @param section the section within which to find the maximum point
     * @return the point (x, y) where the function has its maximum value within the specified section
     */
    default Point2D getMaxPoint(Section section) {
        double step = (section.getRx() - section.getX()) / 100;
        double xMax = section.getX();
        double yMax = value(xMax);

        for (double x = section.getX(); x <= section.getRx(); x += step) {
            double y = value(x);
            if (y > yMax) {
                yMax = y;
                xMax = x;
            }
        }

        return new Point2D(xMax, yMax);
    }

    /**
     * Returns the point of either the minimum or maximum, whichever y-value at the optimum has the higher absolute value
     * @param section the section within which to find the maximum point
     * @return the point (x, y), the function's optimum on the interval [x, rx]
     */
    default Point2D getMaxSignedValuePoint(Section section) {
        Point2D minPoint = getMinPoint(section);
        Point2D maxPoint = getMaxPoint(section);
        return Math.abs(minPoint.getY()) > Math.abs(maxPoint.getY()) ? minPoint : maxPoint;
    }

    /**
     * Gets the minimum value of the function within a specific section.
     * @param section the section within which to find the minimum value
     * @return the minimum value of the function within the specified section
     */
    default double getMinValue(Section section) {
        return getMinPoint(section).getY();
    }

    /**
     * Gets the maximum value of the function within a specific section.
     * @param section the section within which to find the maximum value
     * @return the maximum value of the function within the specified section
     */
    default double getMaxValue(Section section) {
        return getMaxPoint(section).getY();
    }

    /**
     * @param section the section within which to find the maximum point
     * @return the y value of the function's optimum
     */
    default double getMaxSignedValue(Section section) {
        return getMaxSignedValuePoint(section).getY();
    }
}
