package com.wecca.canoeanalysis.diagrams;

import com.wecca.canoeanalysis.Canoe;
import com.wecca.canoeanalysis.PointLoad;
import com.wecca.canoeanalysis.UniformDistributedLoad;

import java.util.*;

//Utility class for generating diagrams
public final class Diagram {
    public Diagram() {}

    /**
     * Inner class to hold load intervals (intermediate between point/distributed loads and diagram points).
     * Each interval is defined by its x coordinates (start - end), magnitude, and slope.
     * Solo point loads have a magnitude but no slope.
     * Solo distributed loads have a slope but no magnitude.
     * Combined loads may cause an interval to have both a magnitude and a slope.
     */
    private static class Interval {
        public double startX;
        public double endX;
        public double magnitude;
        public double slope;

        public Interval(double sX, double eX, double magnitude, double slope) {
            this.startX = sX;
            this.endX = eX;
            this.magnitude = magnitude;
            this.slope = slope;
        }

        //For testing
        public String toString() {
            return "Interval from x = " + startX + " to x = " + endX + " with magnitude = " + magnitude + " and slope = " + slope;
        }
    }

    /**
     * Convert the canoe's point loads to a map of [x coordinate] : [point load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, PointLoad> getPointLoadMap(Canoe canoe) {
        Map<Double, PointLoad> map = new HashMap<>();

        for (PointLoad load : canoe.getPLoads()) {
            map.put((double) Math.round(load.getX() * 100) / 100, load);
        }

        return map;
    }

    /**
     * Convert the canoe's distributed loads to a map of [starting x coordinate] : [distributed load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, UniformDistributedLoad> getDistributedLoadStartMap(Canoe canoe) {
        Map<Double, UniformDistributedLoad> map = new HashMap<>();

        for (UniformDistributedLoad load : canoe.getDLoads()) {
            map.put((double) Math.round(load.getLX() * 100) / 100, load);
        }

        return map;
    }

    /**
     * Convert the canoe's distributed loads to a map of [ending x coordinate] : [distributed load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, UniformDistributedLoad> getDistributedLoadEndMap(Canoe canoe) {
        Map<Double, UniformDistributedLoad> map = new HashMap<>();

        for (UniformDistributedLoad load : canoe.getDLoads()) {
            map.put((double) Math.round(load.getRX() * 100) / 100, load);
        }

        return map;
    }

    /**
     * Convert a list of intervals into a map of [stringified point] : [point object].
     * This uses a map to avoid duplicate points (unique by stringified coordinates).
     * @param intervals the list of intervals to convert.
     * @param canoeLength the length of the canoe.
     * @return the generated map.
     */
    private static Map<String, DiagramPoint> getDiagramPointMap(List<Interval> intervals, double canoeLength) {
        Map<String, DiagramPoint> diagramPoints = new LinkedHashMap<>();
        double rollingMag = 0;
        for (Interval interval : intervals) {
            DiagramPoint start = new DiagramPoint(interval.startX, rollingMag + interval.magnitude);
            if (!diagramPoints.containsKey(start.toString())) {
                diagramPoints.put(start.toString(), start);
                //Add the magnitude of the initial point load to the rolling magnitude
                rollingMag += interval.magnitude;
            }

            DiagramPoint end = new DiagramPoint(interval.endX, rollingMag + interval.slope * (interval.endX - interval.startX));
            if (!diagramPoints.containsKey(end.toString())) {
                diagramPoints.put(end.toString(), end);
                //Add the magnitude of the ending distributed load to the rolling magnitude
                rollingMag += interval.slope * (interval.endX - interval.startX);
            }
        }

        //Add an end point at (length, 0)
        DiagramPoint end = new DiagramPoint(canoeLength, 0);
        if (!diagramPoints.containsKey(end)) {
            diagramPoints.put(end.toString(), end);
        }
        return diagramPoints;
    }

    /**
     * Calculate the area of a section (assume it is a right triangle above a rectangle).
     * @param xLow the min x point.
     * @param xHigh the max x point.
     * @param yLow the min y point.
     * @param yHigh the max y point.
     * @return the area within the section.
     */
    private static double calculateArea(double xLow, double xHigh, double yLow, double yHigh) {
        double deltaX = xHigh - xLow;
        return deltaX * yLow + deltaX * 0.5 * (yHigh - yLow);
    }

    /**
     * Round a double to x digits.
     * @param num the number to round.
     * @param numDigits the number of digits to round to.
     * @return the rounded double.
     */
    private static double roundXDigits(double num, int numDigits) {
        return (double) ((int) (num * Math.pow(10, numDigits))) / Math.pow(10, numDigits);
    }

    /**
     * Generate a series of points for a parabola.
     * @param start the start point of the interval from the SFD.
     * @param end the end point of the interval from the SFD.
     * @param startY the baseline y value for the parabola.
     * @return the list of generated points along the parabola.
     */
    private static List<DiagramPoint> generateParabolicPoints(DiagramPoint start, DiagramPoint end, double startY) {
        List<DiagramPoint> points = new ArrayList<>();

        //Record the prevX, x, prevY, and y for the dy and dx
        double prevX;
        double prevY;
        double x = start.getX();
        double y = start.getY();
        //Record the length and slope of the SFD section
        double len = end.getX() - start.getX();
        double slope = (end.getY() - start.getY()) / len;
        //Iterate through each x point in the section
        while (x <= end.getX()) {
            prevX = x;
            x += 0.005;
            prevY = y;
            y += (x - prevX) * slope;
            //Calculate the area of the section (integral) and set the BMD value at x to this area
            double sectionArea = calculateArea(prevX, x, Math.min(prevY, y), Math.max(prevY, y));
            points.add(new DiagramPoint(roundXDigits(x, 3), roundXDigits(startY + sectionArea, 4)));
            startY += sectionArea;
        }

        return points;
    }

    /**
     * Generate a list of points to comprise the Shear Force Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the SFD.
     */
    public static List<DiagramPoint> generateSfdPoints(Canoe canoe) {
        //Get maps for each load type for efficient processing
        Map<Double, PointLoad> pointLoadMap = getPointLoadMap(canoe);
        Map<Double, UniformDistributedLoad> distributedLoadStartMap = getDistributedLoadStartMap(canoe);
        Map<Double, UniformDistributedLoad> distributedLoadEndMap = getDistributedLoadEndMap(canoe);

        //Maintain the x coordinate, slope, and magnitude of the previous interval
        double prevX = 0;
        double slope = 0;
        double magnitude = 0;
        List<Interval> intervals = new ArrayList<>();
        //Go through each x index from 0 to [canoe length], incrementing it by 0.01 each time
        //This tests each possible starting point to check for a load beginning/ending/occurring at this x coordinate.
        for (int i = 0; i < canoe.getLen() * 100; i ++) {
            double x = (double) i / 100;

            //If a distributed load starts here
            if (distributedLoadStartMap.containsKey(x)) {
                //Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                //Increment the slope, set the x coordinate, and clear the magnitude
                slope += distributedLoadStartMap.get(x).getW();
                prevX = x;
                magnitude = 0;
            }
            //If a distributed load ends here
            if (distributedLoadEndMap.containsKey(x)) {
                //Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                //Decrement the slope, set the x coordinate, and clear the magnitude
                slope -= distributedLoadEndMap.get(x).getW();
                prevX = x;
                magnitude = 0;
            }
            //If a point load occurs here
            if (pointLoadMap.containsKey(x)) {
                //Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                //Reset the magnitude and set the x coordinate
                magnitude = pointLoadMap.get(x).getMag();
                prevX = x;
            }
        }
        //Add a final interval to the end of the canoe with the current magnitude and slope
        intervals.add(new Interval(prevX, canoe.getLen(), magnitude, slope));

        //Sort the list, process them to a map of unique points, and return the points as a list
        intervals.sort((a, b) -> Double.compare(a.startX, b.startX));
        Map<String, DiagramPoint> diagramPoints = getDiagramPointMap(intervals, canoe.getLen());
        return new ArrayList<>(diagramPoints.values());
    }

    /**
     * Generate a list of points to comprise the Bending Moment Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the BMD.
     */
    public static List<DiagramPoint> generateBmdPoints(Canoe canoe) {
        //Gets the SFD points for the canoe
        List<DiagramPoint> sfdPoints = generateSfdPoints(canoe);
        List<DiagramPoint> bmdPoints = new ArrayList<>();
        DiagramPoint firstPoint = sfdPoints.get(0);

        bmdPoints.add(new DiagramPoint(0,0));
        double currY = 0;
        for (int i = 1; i < sfdPoints.size(); i++) {
            DiagramPoint curr = sfdPoints.get(i);
            //If the two consecutive points are on the same vertical line, create a diagonal line for the BMD
            if (curr.getY() == firstPoint.getY()) {
                bmdPoints.add(new DiagramPoint(roundXDigits(curr.getX(), 3), roundXDigits(currY + firstPoint.getY() * (curr.getX() - firstPoint.getX()), 4)));
            }
            //If the two consecutive points are connected via a diagonal line, create a parabola for the BMD
            if (curr.getX() != firstPoint.getX() && curr.getY() != firstPoint.getY()) {
                bmdPoints.addAll(generateParabolicPoints(firstPoint, curr, currY));
            }

            firstPoint = curr;
            if (!bmdPoints.isEmpty()) {
                currY = bmdPoints.get(bmdPoints.size() - 1).getY();
            }
        }
        bmdPoints.add(new DiagramPoint(canoe.getLen(),0));

        return bmdPoints;
    }
}
