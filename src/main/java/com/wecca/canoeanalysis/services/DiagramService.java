package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.CanoeAnalysisApplication;
import com.wecca.canoeanalysis.components.diagrams.DiagramPoint;
import com.wecca.canoeanalysis.components.diagrams.FixedTicksNumberAxis;
import com.wecca.canoeanalysis.components.diagrams.Interval;
import com.wecca.canoeanalysis.models.Canoe;
import com.wecca.canoeanalysis.models.PointLoad;
import com.wecca.canoeanalysis.models.UniformDistributedLoad;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.*;

public class DiagramService {

    /**
     * Set up the canvas/pane for a diagram.
     *
     * @param canoe  to work with
     * @param points the points to render on the diagram.
     * @param title  the title of the diagram.
     * @param yUnits the units of the y-axis on the diagram.
     */
    public static void setupDiagram(Canoe canoe, List<DiagramPoint> points, String title, String yUnits)
    {
        // Initializing the stage and main pane
        Stage popupStage = new Stage();
        popupStage.setTitle(title);
        Pane chartPane = new Pane();
        chartPane.setPrefSize(1125, 750);

        // Adding Logo Icon
        Image icon = new Image("file:src/main/resources/com/wecca/canoeanalysis/canoe.png");
        popupStage.getIcons().add(icon);

        // Setting the axes of the chart
        NumberAxis yAxis = new NumberAxis();
        TreeSet<Double> criticalPoints = canoe.getSectionEndPoints();
        FixedTicksNumberAxis xAxis = new FixedTicksNumberAxis(new ArrayList<>(criticalPoints));
        xAxis.setAutoRanging(false);
        xAxis.setLabel("Distance [m]");
        yAxis.setLabel(yUnits);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(canoe.getLen());

        // Creating and styling the line chart
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setPrefSize(1125, 750);
        chart.setLegendVisible(false);
        chart.getStylesheets().add(CanoeAnalysisApplication.class.getResource("css/chart.css").toExternalForm());

        List<XYChart.Series> intervalsAsSeries = getIntervalsAsSeries(canoe, points, yUnits, criticalPoints, chart);

        // Creating the scene and adding the chart to it
        chartPane.getChildren().add(chart);
        Scene scene = new Scene(chartPane, 1125, 750);
        popupStage.setScene(scene);
        popupStage.show();

    }

    private static List<XYChart.Series> getIntervalsAsSeries(Canoe canoe, List<DiagramPoint> points, String yUnits, TreeSet<Double> criticalPoints, AreaChart<Number, Number> chart) {
        // Adding the sections of the pseudo piecewise function separately
        boolean set = false; // only need to set the name of the series once since its really one piecewise function
        List<List<DiagramPoint>> intervals = partitionPoints(canoe, points, criticalPoints);
        List<XYChart.Series> intervalsAsSeries = new ArrayList<>();
        for (List<DiagramPoint> interval : intervals)
        {
            XYChart.Series series = new XYChart.Series();
            for (DiagramPoint point : interval)
            {
                series.getData().add(new XYChart.Data<>(point.getX(), point.getY()));
            }

            if (!set)
            {
                series.setName(yUnits);
                set = true;
            }
            series.setName(yUnits);
            chart.getData().add(series);
            intervalsAsSeries.add(series);
        }
        return intervalsAsSeries;
    }

    /**
     * Consider the list of points is a "pseudo piecewise function" (pseudo as discrete points are defined rather than a continuous function)
     * This method breaks it into a set of "pseudo functions"
     * @param canoe to work with
     * @param points act together as a piecewise function
     * @param partitions the locations where the form of the piecewise changes
     * @return a list containing each section of the piecewise pseudo functions with unique form
     */
    private static List<List<DiagramPoint>> partitionPoints(Canoe canoe, List<DiagramPoint> points, TreeSet<Double> partitions)
    {
        // Initializing lists
        List<List<DiagramPoint>> partitionedIntervals = new ArrayList<>();
        List<Double> partitionsList = new ArrayList<>(partitions);

        // Testing
        System.out.println("\nPartition Points:");
        for (double point : partitionsList)
        {
            System.out.println("point = " + point);
        }

        // If the first point is doubled up due to a jump discontinuity then throw away the first point (0, 0)
        // By "doubled up" I mean two points at the same x coordinate
        if (points.getFirst().getX() == 0 && points.get(1).getX() == 0)
            points.removeFirst();

        // Same idea for last two points if they double up
        if (points.getLast().getX() == canoe.getLen() && points.get(points.size() - 2).getX() == canoe.getLen())
            points.removeLast();

        // Remove zero from the partition list (always first as the TreeSet is sorted ascending)
        if (partitionsList.getFirst() == 0)
            partitionsList.removeFirst();

        // Keep track of intervals and points that partition them
        int partitionIndex = 0;
        List<DiagramPoint> interval = new ArrayList<>();

        // Put all the points into intervals
        for (int i = 0; i < points.size(); i++)
        {
            // Get the current point
            DiagramPoint point = points.get(i);

            // Keep adding points to the interval until the partition index reached
            // Empty interval means this is the first point to be included
            if (point.getX() != partitionsList.get(partitionIndex) || interval.isEmpty())
                interval.add(point);

            // Add the interval to the list of partitioned intervals and prepare for the next interval
            else
            {
                interval.add(point); // this is the partition point, which acts as the right endpoint of the interval

                // If not at the right boundary of the beam
                if (i != points.size() - 1)
                {
                    // If no jump discontinuity, create a duplicate point to act as the left endpoint of the next interval
                    if (point.getX() != points.get(i + 1).getX())
                        i--;
                }

                // Add a copy of the interval to the intervals list, and prep for the next interval
                partitionIndex++;
                partitionedIntervals.add(new ArrayList<>(interval));
                interval.clear();
            }
        }

        return partitionedIntervals;
    }

    /**
     * Round a double to x digits.
     * @param num the number to round.
     * @param numDigits the number of digits to round to.
     * @return the rounded double.
     */
    public static double roundXDigits(double num, int numDigits) {
        double factor = Math.pow(10, numDigits);
        return Math.round(num * factor) / factor;
    }

    /**
     * Convert the canoe's point loads to a map of [x coordinate] : [point load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, PointLoad> getPointLoadMap(Canoe canoe)
    {
        Map<Double, PointLoad> map = new HashMap<>();
        for (PointLoad load : canoe.getPLoads())
        {
            double x = (double) Math.round(load.getX() * 100) / 100;
            if (map.containsKey(x))
            {
                // Supports will not combine with loads at the same position, so they are always included in the model
                // We need to combine them here to prevent duplicate key issues
                double mag = map.get(x).getMag();
                mag += load.getMag();
                load = new PointLoad(mag, x, false);
            }
            map.put(x, load);
        }
        return map;
    }

    /**
     * Convert the canoe's distributed loads to a map of [starting x coordinate] : [distributed load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, UniformDistributedLoad> getDistributedLoadStartMap(Canoe canoe)
    {
        Map<Double, UniformDistributedLoad> map = new HashMap<>();
        for (UniformDistributedLoad load : canoe.getDLoads()) {map.put((double) Math.round(load.getX() * 100) / 100, load);}
        return map;
    }

    /**
     * Convert the canoe's distributed loads to a map of [ending x coordinate] : [distributed load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, UniformDistributedLoad> getDistributedLoadEndMap(Canoe canoe)
    {
        Map<Double, UniformDistributedLoad> map = new HashMap<>();
        for (UniformDistributedLoad load : canoe.getDLoads()) {map.put((double) Math.round(load.getRX() * 100) / 100, load);}
        return map;
    }

    /**
     * Convert a list of intervals into a map of [stringified point] : [point object].
     * This uses a map to avoid duplicate points (unique by stringified coordinates).
     * @param intervals the list of intervals to convert.
     * @param canoeLength the length of the canoe.
     * @return the generated map.
     */
    private static Map<String, DiagramPoint> getDiagramPointMap(List<Interval> intervals, double canoeLength)
    {
        Map<String, DiagramPoint> diagramPoints = new LinkedHashMap<>();
        double rollingMag = 0;
        for (Interval interval : intervals) {
            DiagramPoint start = new DiagramPoint(interval.startX, rollingMag + interval.magnitude);
            if (!diagramPoints.containsKey(start.toString())) {
                diagramPoints.put(start.toString(), start);
                // Add the magnitude of the initial point load to the rolling magnitude
                rollingMag += interval.magnitude;
            }

            DiagramPoint end = new DiagramPoint(interval.endX, rollingMag + interval.slope * (interval.endX - interval.startX));
            if (!diagramPoints.containsKey(end.toString())) {
                diagramPoints.put(end.toString(), end);
                // Add the magnitude of the ending distributed load to the rolling magnitude
                rollingMag += interval.slope * (interval.endX - interval.startX);
            }
        }

        // Add an end point at (length, 0)
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
    private static double calculateArea(double xLow, double xHigh, double yLow, double yHigh)
    {
        double deltaX = xHigh - xLow;
        return deltaX * yLow + deltaX * 0.5 * (yHigh - yLow);
    }

    /**
     * Generate a series of points for a parabola.
     * @param start the start point of the interval from the SFD.
     * @param end the end point of the interval from the SFD.
     * @param startY the baseline y value for the parabola.
     * @return the list of generated points along the parabola.
     */
    private static List<DiagramPoint> generateParabolicPoints(DiagramPoint start, DiagramPoint end, double startY)
    {
        List<DiagramPoint> points = new ArrayList<>();

        // Record the prevX, x, prevY, and y for the dy and dx
        double prevX;
        double prevY;
        double x = start.getX();
        double y = start.getY();
        // Record the length and slope of the SFD section
        double len = end.getX() - start.getX();
        double slope = (end.getY() - start.getY()) / len;
        // Iterate through each x point in the section
        while (x < end.getX()) {
            prevX = x;
            x += 0.001;
            prevY = y;
            y += (x - prevX) * slope;
            // Calculate the area of the section (integral) and set the BMD value at x to this area
            double sectionArea = calculateArea(prevX, x, Math.min(prevY, y), Math.max(prevY, y));
            points.add(new DiagramPoint(roundXDigits(x, 3), roundXDigits(startY + sectionArea, 4)));
            startY += sectionArea;
        }

        // Remove extra point out of bounds (caused by floating point error maybe?)
        if (points.getLast().getX() > end.getX())
            points.removeLast();

        return points;
    }

    /**
     * Generate a list of points to comprise the Shear Force Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the SFD.
     */
    public static List<DiagramPoint> generateSfdPoints(Canoe canoe)
    {
        // Get maps for each load type for efficient processing
        Map<Double, PointLoad> pointLoadMap = getPointLoadMap(canoe);
        Map<Double, UniformDistributedLoad> distributedLoadStartMap = getDistributedLoadStartMap(canoe);
        Map<Double, UniformDistributedLoad> distributedLoadEndMap = getDistributedLoadEndMap(canoe);

        // Maintain the x coordinate, slope, and magnitude of the previous interval
        double prevX = 0;
        double slope = 0;
        double magnitude = 0;
        List<Interval> intervals = new ArrayList<>();
        // Go through each x index from 0 to [canoe length], incrementing it by 0.01 each time
        // This tests each possible starting point to check for a load beginning/ending/occurring at this x coordinate.
        for (int i = 0; i < canoe.getLen() * 100; i ++) {
            double x = (double) i / 100;

            // If a distributed load starts here
            if (distributedLoadStartMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                // Increment the slope, set the x coordinate, and clear the magnitude
                slope += distributedLoadStartMap.get(x).getMag();
                prevX = x;
                magnitude = 0;
            }
            // If a distributed load ends here
            if (distributedLoadEndMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                // Decrement the slope, set the x coordinate, and clear the magnitude
                slope -= distributedLoadEndMap.get(x).getMag();
                prevX = x;
                magnitude = 0;
            }
            // If a point load occurs here
            if (pointLoadMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new Interval(prevX, x, magnitude, slope));
                // Reset the magnitude and set the x coordinate
                magnitude = pointLoadMap.get(x).getMag();
                prevX = x;
            }
        }
        // Add a final interval to the end of the canoe with the current magnitude and slope
        intervals.add(new Interval(prevX, canoe.getLen(), magnitude, slope));

        // Sort the list, process them to a map of unique points, and return the points as a list
        intervals.sort(Comparator.comparingDouble(a -> a.startX));
        Map<String, DiagramPoint> diagramPoints = getDiagramPointMap(intervals, canoe.getLen());
        ArrayList<DiagramPoint> sfdPoints = new ArrayList<>(diagramPoints.values());

        // Return the generated points
        return new ArrayList<>(sfdPoints);
    }

    /**
     * Generate a list of points to comprise the Bending Moment Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the BMD.
     */
    public static List<DiagramPoint> generateBmdPoints(Canoe canoe)
    {
        // Gets the SFD points for the canoe
        List<DiagramPoint> sfdPoints = generateSfdPoints(canoe);
        List<DiagramPoint> bmdPoints = new ArrayList<>();
        DiagramPoint firstPoint = sfdPoints.getFirst();

        bmdPoints.add(new DiagramPoint(0,0));
        double currY = 0;
        for (int i = 1; i < sfdPoints.size(); i++) {
            DiagramPoint curr = sfdPoints.get(i);
            // If the two consecutive points are on the same vertical line, create a diagonal line for the BMD
            if (curr.getY() == firstPoint.getY()) {
                bmdPoints.add(new DiagramPoint(roundXDigits(curr.getX(), 3), roundXDigits(currY + firstPoint.getY() * (curr.getX() - firstPoint.getX()), 4)));
            }
            // If the two consecutive points are connected via a diagonal line, create a parabola for the BMD
            if (curr.getX() != firstPoint.getX() && curr.getY() != firstPoint.getY()) {
                bmdPoints.addAll(generateParabolicPoints(firstPoint, curr, currY));
            }

            firstPoint = curr;
            if (!bmdPoints.isEmpty()) {
                currY = bmdPoints.getLast().getY();
            }
        }
        bmdPoints.add(new DiagramPoint(canoe.getLen(),0));

        return bmdPoints;
    }
}
