package com.wecca.canoeanalysis.models.function;

import javafx.geometry.Point2D;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

/**
 * A function which has bounded y-values on its domain, some subset of R^+
 *
 * The implementer can either use methods from the original API, or the newer unimodal API
 * BeamController was originally implemented with the old API, and the new API was designed for HullBuilderController
 * The new API using Brent's method requires the function to be unimodal (one maximum in the section)
 * Constant value uniformly distributed loads break this because each point is equal and there are infinite maximums then
 * Thus to avoid changing old BeamController code which may break it, the API has been expanded
 * The option to use the new version of the API is left up to the implementer, they must manually switch over the optimized API
 */
public interface BoundedUnivariateFunction extends UnivariateFunction {

    // ===== Original API: Manual Optimization (100 steps) =====

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
     * Returns the point of either the minimum or maximum (whichever has the higher absolute y-value)
     * @param section the section within which to find the optimum
     * @return the optimum point (x, y) on the interval [x, rx]
     */
    default Point2D getMaxSignedValuePoint(Section section) {
        Point2D minPoint = getMinPoint(section);
        Point2D maxPoint = getMaxPoint(section);
        return Math.abs(minPoint.getY()) > Math.abs(maxPoint.getY()) ? minPoint : maxPoint;
    }

    /**
     * Gets the minimum value of the function within a specific section.
     * @param section the section within which to find the minimum value
     * @return the minimum value within the specified section
     */
    default double getMinValue(Section section) {
        return getMinPoint(section).getY();
    }

    /**
     * Gets the maximum value of the function within a specific section.
     * @param section the section within which to find the maximum value
     * @return the maximum value within the specified section
     */
    default double getMaxValue(Section section) {
        return getMaxPoint(section).getY();
    }

    /**
     * Gets the optimum (max signed value) of the function within a specific section.
     * @param section the section within which to find the optimum
     * @return the y value of the function's optimum within the specified section
     */
    default double getMaxSignedValue(Section section) {
        return getMaxSignedValuePoint(section).getY();
    }

    // ===== New API: Unimodal Optimization Using Brent's Method =====

    /**
     * Optimizes the function using a hybrid manual search (10 steps) plus Brent's method.
     * @param section  the section within which to perform the optimization
     * @param goalType the optimization goal (minimize or maximize)
     * @return the optimum point (x, y)
     */
    private Point2D optimizeUnimodal(Section section, GoalType goalType) {
        double xStart = section.getX();
        double xEnd = section.getRx();
        double step = (xEnd - xStart) / 10.0;
        double bestX = xStart;
        double bestY = value(xStart);
        for (double x = xStart; x <= xEnd; x += step) {
            double y = value(x);
            if ((goalType == GoalType.MINIMIZE && y < bestY) ||
                    (goalType == GoalType.MAXIMIZE && y > bestY)) {
                bestX = x;
                bestY = y;
            }
        }
        double lower = Math.max(xStart, bestX - step);
        double upper = Math.min(xEnd, bestX + step);
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-8);
        UnivariatePointValuePair result = optimizer.optimize(
                new MaxEval(1000),
                new UnivariateObjectiveFunction(this),
                goalType,
                new SearchInterval(lower, upper)
        );
        return new Point2D(result.getPoint(), result.getValue());
    }

    /**
     * Gets the minimum point of the function within a specific section using unimodal optimization.
     * @param section the section within which to find the minimum point
     * @return the point (x, y) where the function has its minimum value within the specified section
     */
    default Point2D getMinPointUnimodal(Section section) {
        return optimizeUnimodal(section, GoalType.MINIMIZE);
    }

    /**
     * Gets the maximum point of the function within a specific section using unimodal optimization.
     * @param section the section within which to find the maximum point
     * @return the point (x, y) where the function has its maximum value within the specified section
     */
    default Point2D getMaxPointUnimodal(Section section) {
        return optimizeUnimodal(section, GoalType.MAXIMIZE);
    }

    /**
     * Returns the optimum point (with the higher absolute y-value) using unimodal optimization.
     * @param section the section within which to find the optimum
     * @return the optimum point (x, y) on the interval [x, rx]
     */
    default Point2D getMaxSignedValuePointUnimodal(Section section) {
        Point2D minPoint = getMinPointUnimodal(section);
        Point2D maxPoint = getMaxPointUnimodal(section);
        return Math.abs(minPoint.getY()) > Math.abs(maxPoint.getY()) ? minPoint : maxPoint;
    }

    /**
     * Gets the minimum value of the function within a specific section using unimodal optimization.
     * @param section the section within which to find the minimum value
     * @return the minimum value within the specified section
     */
    default double getMinValueUnimodal(Section section) {
        return getMinPointUnimodal(section).getY();
    }

    /**
     * Gets the maximum value of the function within a specific section using unimodal optimization.
     * @param section the section within which to find the maximum value
     * @return the maximum value within the specified section
     */
    default double getMaxValueUnimodal(Section section) {
        return getMaxPointUnimodal(section).getY();
    }

    /**
     * Gets the optimum (max signed value) of the function within a specific section using unimodal optimization.
     * @param section the section within which to find the optimum
     * @return the y value of the function's optimum within the specified section
     */
    default double getMaxSignedValueUnimodal(Section section) {
        return getMaxSignedValuePointUnimodal(section).getY();
    }
}
