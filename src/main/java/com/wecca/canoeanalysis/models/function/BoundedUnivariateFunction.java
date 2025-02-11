package com.wecca.canoeanalysis.models.function;

import com.wecca.canoeanalysis.aop.Traceable;
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
 */
public interface BoundedUnivariateFunction extends UnivariateFunction {

    /**
     * @param section the section within which to find the minimum
     * @return the minimum point (x, y) of the function within the specified section.
     */
    default Point2D getMinPoint(Section section) {
        return optimize(section, GoalType.MINIMIZE);
    }

    /**
     * @param section the section within which to find the maximum
     * @return the maximum point (x, y) of the function within the specified section.
     */
    default Point2D getMaxPoint(Section section) {
        return optimize(section, GoalType.MAXIMIZE);
    }

    /**
     * Optimizes the function to find the minimum or maximum point within a section.
     * @param section  the section within which to perform the optimization
     * @param goalType the optimization goal (minimize or maximize)
     * @return the optimal point (x, y)
     */
    private Point2D optimize(Section section, GoalType goalType) {
        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-8);
        UnivariatePointValuePair result = optimizer.optimize(
                new MaxEval(1000),
                new UnivariateObjectiveFunction(this),
                goalType,
                new SearchInterval(section.getX(), section.getRx())
        );
        double x = result.getPoint();
        double y = result.getValue();
        return new Point2D(x, y);
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
