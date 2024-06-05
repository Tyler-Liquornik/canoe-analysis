package com.wecca.canoeanalysis.util;

import com.wecca.canoeanalysis.diagrams.DiagramPoint;

import java.util.HashMap;

// TODO: Use this to get equations for the piecewise graph, and eventually get LateX from that
public class EquationMathUtils {
    /**
     * Finds the coefficients of the line mx + b from a list of 2 of its points
     * @param points the three points on the parabola, preferred to be the interval endpoints to reduce error due to the numerical nature of point generation
     * @return the coefficients [a, b]
     */
    public static double[] getLinearCoefficients(DiagramPoint[] points)
    {
        double x1 = points[0].getX();
        double y1 = points[0].getY();
        double x2 = points[1].getX();
        double y2 = points[1].getY();

        double m = (y2 - y1) / (x2 - x1);
        double b = y1 - m * x1;

        return new double[] {m, b};
    }

    /**
     * Get the max value of a line on interval [l, r]
     * @param coefficients in the form [m, c] for line mx + c
     * @param l the left bound of the interval to check
     * @param r the right bound of the interval to check
     * @return the maximum value on the interval as <X, Y>
     */
    public static HashMap<Double, Double> getLinearMax(double[] coefficients, double l, double r)
    {
        HashMap<Double, Double> critical = new HashMap<>();

       double m = coefficients[0];
       double c = coefficients[1];
       double y1 = m * l + c;
       double y2 = m * r + c;

       if (y1 >= y2)
           critical.put(l, y1);
       else
           critical.put(r, y2);

       return critical;
    }

    /**
     * Finds the coefficients of the quadratic curve ax^2 + bx + c from a list of 3 of its points
     * @param points the three points on the parabola, preferred to be the interval endpoints and critical points to reduce error due to the numerical nature of point generation
     * @return the coefficients [a, b, c]
     */
    public static double[] getQuadraticCoefficients(DiagramPoint[] points)
    {
        double x1 = points[0].getX();
        double y1 = points[0].getY();
        double x2 = points[1].getX();
        double y2 = points[1].getY();
        double x3 = points[2].getX();
        double y3 = points[2].getY();

        double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
        double a    = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
        double b    = (x3*x3 * (y1 - y2) + x2*x2 * (y3 - y1) + x1*x1 * (y2 - y3)) / denom;
        double c    = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

        return new double[]{a, b, c};
    }

    /**
     * Get the critical (max or min) value of a parabola on interval [l, r]
     * @param coefficients in the form [a, b, c] for parabola ax^2 + bx + c
     * @param l the left bound of the interval to check
     * @param r the right bound of the interval to check
     * @return the critical point on the interval as <X, Y>
     */
    public static HashMap<Double, Double> getQuadraticCritical(double[] coefficients, double l, double r)
    {
        HashMap<Double, Double> critical = new HashMap<>();

        double a = coefficients[0];
        double b = coefficients[1];
        double c = coefficients[2];

        double xCritical = -b / (2 * a);
        double yCritical = c - (b * b) / (4 * a);

        critical.put(xCritical, yCritical);
        return critical;
    }
}
