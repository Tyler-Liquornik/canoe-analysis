package com.wecca.canoeanalysis.services;

import com.wecca.canoeanalysis.components.diagrams.FixedTicksNumberAxis;
import com.wecca.canoeanalysis.components.diagrams.DiagramInterval;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.geometry.Point2D;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.*;
import java.util.List;

/**
 * The DiagramService class provides utility methods for setting up and managing diagrams such as the Shear Force Diagram (SFD) and Bending Moment Diagram (BMD) for a given canoe.
 * It is designed to handle point-wise defined piecewise functions with potential jump discontinuities
 * Overall the service aids in the visualization of structural parameters like shear forces and bending moments for more effective analysis.
 */
public class DiagramService {

    /**
     * Sets up the chart for the diagram window.
     *
     * @param canoe  the canoe object containing section end points and length
     * @param points the list of diagram points to be plotted
     * @param yUnits the label for the Y-axis units
     * @return the configured AreaChart
     */
    public static AreaChart<Number, Number> setupChart(Canoe canoe, List<Point2D> points, String yUnits) {
        // Setting up the axes
        NumberAxis yAxis = setupYAxis(yUnits);
        FixedTicksNumberAxis xAxis = setupXAxis(canoe);

        // Creating and styling the chart
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setPrefSize(1125, 750);
        chart.setLegendVisible(false);

        // Adding data to chart
        addSeriesToChart(canoe, points, yUnits, chart);
        // addTooltipsToChart(chart, yUnits);

        return chart;
    }

    /**
     * Sets up the Y-axis for the chart.
     *
     * @param yUnits the label for the Y-axis units
     * @return the configured NumberAxis
     */
    private static NumberAxis setupYAxis(String yUnits) {
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yUnits);
        return yAxis;
    }

    /**
     * Sets up the X-axis for the chart using critical points from the canoe.
     *
     * @param canoe the canoe object containing section end points and length
     * @return the configured FixedTicksNumberAxis
     */
    private static FixedTicksNumberAxis setupXAxis(Canoe canoe) {
        TreeSet<Double> criticalPoints = canoe.getSectionEndpoints();
        TreeSet<Double> roundedCriticalPoints = criticalPoints.stream()
                .map(point -> CalculusUtils.roundXDecimalDigits(point, 3)).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        FixedTicksNumberAxis xAxis = new FixedTicksNumberAxis(new ArrayList<>(roundedCriticalPoints));
        xAxis.setAutoRanging(false);
        xAxis.setLabel("Distance [m]");
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(canoe.getHull().getLength());
        return xAxis;
    }

    /**
     * Adds a series of points to an AreaChart to represent a piecewise function
     * for a given canoe object (used for SFD, BMD, and maybe more in the future).
     * Sections are partitioned into sections based on the critical points of the canoe,
     * and each section is added to the chart as a separate series, so we can manage them separately.
     *
     * @param canoe The Canoe object containing the section endpoints that define the critical points for partitioning the points.
     * @param points A list of points representing the data points to be plotted.
     * @param yUnits A string representing the units of the Y-axis (e.g., "meters", "kilograms").
     * @param chart to which the partitioned point series will be added.
     */
    public static void addSeriesToChart(Canoe canoe, List<Point2D> points, String yUnits, AreaChart<Number, Number> chart) {

        TreeSet<Double> criticalPoints = canoe.getSectionEndpoints();

        // Adding the sections of the pseudo piecewise function separately
        boolean set = false; // only need to set the name of the series once since its really one piecewise function
        List<List<Point2D>> intervals = partitionPoints(points, criticalPoints);
        for (List<Point2D> interval : intervals) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (Point2D point : interval) {
                XYChart.Data<Number, Number> data = new XYChart.Data<>(point.getX(), point.getY());
                series.getData().add(data);
            }

            if (!set) {
                series.setName(yUnits);
                set = true;
            }
            series.setName(yUnits);
            chart.getData().add(series);
        }
    }

//    public static void addTooltipsToChart(AreaChart<Number, Number> chart, String yUnits) {
//        for (XYChart.Series<Number, Number> series : chart.getData()) {
//            for (XYChart.Data<Number, Number> data : series.getData()) {
//                addTooltipToData(data, yUnits);
//            }
//        }
//    }
//
//    private static void addTooltipToData(XYChart.Data<Number, Number> data, String yUnits) {
//        JFXTooltip tooltip = new JFXTooltip();
//        tooltip.setText(
//                "x: " + data.getXValue() + "\n" +
//                "mag: " + data.getYValue() + " " + yUnits
//        );
//
//        // Apply CSS styling to the tooltip (placeholder needs to work properly with color services)
//        //  tooltip.setStyle("-fx-background-color: " + ColorManagerService.getColor("tooltipBackground") + ";"
//        //          + "-fx-text-fill: " + ColorManagerService.getColor("tooltipText") + ";"
//        //          + "-fx-padding: 10px;"
//        //          + "-fx-border-color: " + ColorManagerService.getColor("tooltipBorder") + ";"
//        //          + "-fx-border-width: 1px;");
//
//        data.getNode().setOnMouseEntered(event -> {
//            double mouseY = event.getScreenY();
//            double screenHeight = data.getNode().getScene().getWindow().getHeight();
//
//            // Determine tooltip position
//            if (mouseY > screenHeight / 2) {
//                tooltip.show(data.getNode(), event.getScreenX(), event.getScreenY() - tooltip.getHeight() - 15);
//            } else {
//                tooltip.show(data.getNode(), event.getScreenX(), event.getScreenY() + 15);
//            }
//        });
//
//        data.getNode().setOnMouseMoved(event -> {
//            double mouseY = event.getScreenY();
//            double screenHeight = data.getNode().getScene().getWindow().getHeight();
//
//            // Determine tooltip position
//            if (mouseY > screenHeight / 2) {
//                tooltip.show(data.getNode(), event.getScreenX(), event.getScreenY() - tooltip.getHeight() - 15);
//            } else {
//                tooltip.show(data.getNode(), event.getScreenX(), event.getScreenY() + 15);
//            }
//        });
//
//        data.getNode().setOnMouseExited(event -> tooltip.hide());
//    }

    /**
     * @param points act together as a point-wise defined C0 function
     * @param partitions the locations where the form of the piecewise changes
     * @return a list containing each section of the piecewise pseudo functions with unique form
     */
    private static List<List<Point2D>> partitionPoints(List<Point2D> points, TreeSet<Double> partitions) {
        // Initializing lists
        List<List<Point2D>> partitionedIntervals = new ArrayList<>();
        List<Double> partitionsList = new ArrayList<>(partitions);

        // If the first point is doubled up due to a jump discontinuity then throw away the first point (0, 0)
        // By "doubled up" I mean two points at the same x coordinate
        if (points.getFirst().getX() == 0 && points.get(1).getX() == 0)
            points.removeFirst();

        // Remove zero from the partition list (always first as the TreeSet is sorted ascending)
        if (partitionsList.getFirst() == 0)
            partitionsList.removeFirst();

        // Keep track of intervals and points that partition them
        int partitionIndex = 0;
        List<Point2D> interval = new ArrayList<>();

        // Put all the points into intervals
        for (int i = 0; i < points.size(); i++) {

            if (partitionIndex == partitionsList.size())
                break;

            // Get the current point
            Point2D point = points.get(i);
            Point2D nextPoint = i + 1 < points.size() ? points.get(i + 1) : null;
            double partition = partitionsList.get(partitionIndex);

            // Keep adding points to the interval until the partition index reached
            // Empty interval means this is the first point to be included
            if (!(point.getX() >= partition && nextPoint != null && nextPoint.getX() >= partition) || interval.isEmpty())
                interval.add(point);

            // Add the interval to the list of partitioned intervals and prepare for the next interval
            else {
                interval.add(point); // this is the partition point, which acts as the right endpoint of the interval

                // If not at the right boundary of the beam
                if (i != points.size() - 1) {
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
     * Convert the canoe's point loads to a map of [x coordinate] : [point load].
     * Note: a regular map suffices instead of a multimap as point loads at the same x value should always combine
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Map<Double, PointLoad> getPointLoadMap(Canoe canoe)
    {
        Map<Double, PointLoad> map = new HashMap<>();
        for (PointLoad load : canoe.getAllLoadsOfType(PointLoad.class))
        {
            double x = (double) Math.round(load.getX() * 100) / 100;
            if (map.containsKey(x))
            {
                // Supports will not combine with loads at the same position, so they are always included in the model
                // We need to combine them here to prevent duplicate key issues
                double mag = map.get(x).getMaxSignedValue();
                mag += load.getMaxSignedValue();
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
    private static Multimap<Double, UniformLoadDistribution> getDistributedLoadStartMap(Canoe canoe)
    {
        Multimap<Double, UniformLoadDistribution> map = ArrayListMultimap.create();
        for (UniformLoadDistribution load : canoe.getAllLoadsOfType(UniformLoadDistribution.class)) {
            map.put((double) Math.round(load.getX() * 100) / 100, load);
        }

        List<UniformLoadDistribution> distributionDLoads = new ArrayList<>();
        if (!canoe.getAllLoadsOfType(PiecewiseContinuousLoadDistribution.class).isEmpty()) {

            List<DiscreteLoadDistribution> discretizations = canoe.getAllLoadsOfType(PiecewiseContinuousLoadDistribution.class).stream()
                    .map(piecewise -> DiscreteLoadDistribution.fromPiecewiseContinuous(LoadType.DISCRETE_SECTION, piecewise, (int) (piecewise.getSection().getLength() * 100)))
                    .toList();

            for (DiscreteLoadDistribution loadDist : discretizations) {
                distributionDLoads.addAll(loadDist.getLoads());
            }
            for (UniformLoadDistribution load : distributionDLoads) {
                map.put((double) Math.round(load.getX() * 100) / 100, load);
            }
        }

        return map;
    }

    /**
     * Convert the canoe's distributed loads to a map of [ending x coordinate] : [distributed load].
     * @param canoe the canoe object.
     * @return the generated map.
     */
    private static Multimap<Double, UniformLoadDistribution> getDistributedLoadEndMap(Canoe canoe)
    {
        Multimap<Double, UniformLoadDistribution> map = ArrayListMultimap.create();
        for (UniformLoadDistribution load : canoe.getAllLoadsOfType(UniformLoadDistribution.class)) {map.put((double) Math.round(load.getRx() * 100) / 100, load);}

        List<UniformLoadDistribution> externalDistributionDLoads = new ArrayList<>();
        if (!canoe.getAllLoadsOfType(PiecewiseContinuousLoadDistribution.class).isEmpty()) {

            List<DiscreteLoadDistribution> discretizations = canoe.getAllLoadsOfType(PiecewiseContinuousLoadDistribution.class).stream()
                    .map(piecewise -> DiscreteLoadDistribution.fromPiecewiseContinuous(LoadType.DISCRETE_SECTION, piecewise, (int) (piecewise.getSection().getLength() * 100)))
                    .toList();

            for (DiscreteLoadDistribution loadDist : discretizations) {
                externalDistributionDLoads.addAll(loadDist.getLoads());
            }
            for (UniformLoadDistribution load : externalDistributionDLoads) {
                map.put((double) Math.round(load.getRx() * 100) / 100, load);
            }
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
    private static Map<String, Point2D> getDiagramPointMap(List<DiagramInterval> intervals, double canoeLength)
    {
        Map<String, Point2D> diagramPoints = new LinkedHashMap<>();
        double rollingMag = 0;
        for (DiagramInterval interval : intervals) {
            Point2D start = new Point2D(interval.getX(), rollingMag + interval.getMagnitude());
            if (!diagramPoints.containsKey(start.toString())) {
                diagramPoints.put(start.toString(), start);
                // Add the magnitude of the initial point load to the rolling magnitude
                rollingMag += interval.getMagnitude();
            }

            Point2D end = new Point2D(interval.getRx(), rollingMag + interval.getSlope() * interval.getLength());
            if (!diagramPoints.containsKey(end.toString())) {
                diagramPoints.put(end.toString(), end);
                // Add the magnitude of the ending distributed load to the rolling magnitude
                rollingMag += interval.getSlope() * interval.getLength();
            }
        }

        // Add an end point at (length, 0)
        Point2D end = new Point2D(canoeLength, 0);
        diagramPoints.put(end.toString(), end);
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
    private static List<Point2D> generateParabolicPoints(Point2D start, Point2D end, double startY)
    {
        List<Point2D> points = new ArrayList<>();

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
            points.add(new Point2D(CalculusUtils.roundXDecimalDigits(x, 6), CalculusUtils.roundXDecimalDigits(startY + sectionArea, 6)));
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
    public static List<Point2D> generateSfdPoints(Canoe canoe) {
        // Get maps for each load type for efficient processing
        Map<Double, PointLoad> pointLoadMap = getPointLoadMap(canoe);
        Multimap<Double, UniformLoadDistribution> distributedLoadStartMap = getDistributedLoadStartMap(canoe);
        Multimap<Double, UniformLoadDistribution> distributedLoadEndMap = getDistributedLoadEndMap(canoe);

        // Maintain the x coordinate, slope, and magnitude of the previous interval
        double prevX = 0;
        double slope = 0;
        double magnitude = 0;
        List<DiagramInterval> intervals = new ArrayList<>();
        // Go through each x index from 0 to [canoe length], incrementing it by 0.01 each time
        // This tests each possible starting point to check for a load beginning/ending/occurring at this x coordinate.
        for (int i = 0; i < canoe.getHull().getLength() * 100; i ++) {
            double x = (double) i / 100;

            // If a distributed load starts here
            if (distributedLoadStartMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new DiagramInterval(prevX, x, magnitude, slope));
                // Increment the slope, set the x coordinate, and clear the magnitude
                for (UniformLoadDistribution load : distributedLoadStartMap.get(x)) {slope += load.getMagnitude();}
                prevX = x;
                magnitude = 0;
            }
            // If a distributed load ends here
            if (distributedLoadEndMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new DiagramInterval(prevX, x, magnitude, slope));
                // Decrement the slope, set the x coordinate, and clear the magnitude
                for (UniformLoadDistribution load : distributedLoadEndMap.get(x)) {slope -= load.getMagnitude();}
                prevX = x;
                magnitude = 0;
            }
            // If a point load occurs here
            if (pointLoadMap.containsKey(x)) {
                // Apply the magnitude and the rolling slope
                intervals.add(new DiagramInterval(prevX, x, magnitude, slope));
                // Reset the magnitude and set the x coordinate
                magnitude = pointLoadMap.get(x).getMaxSignedValue();
                prevX = x;
            }
        }
        // Add a final interval to the end of the canoe with the current magnitude and slope
        intervals.add(new DiagramInterval(prevX, canoe.getHull().getLength(), magnitude, slope));

        // Sort the list, process them to a map of unique points, and return the points as a list
        intervals.sort(Comparator.comparingDouble(DiagramInterval::getX));
        Map<String, Point2D> diagramPoints = getDiagramPointMap(intervals, canoe.getHull().getLength());
        ArrayList<Point2D> sfdPoints = new ArrayList<>(diagramPoints.values());

        // Return the generated points
        return new ArrayList<>(sfdPoints);
    }

    /**
     * Generate a list of points to comprise the Bending Moment Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the BMD.
     */
    public static List<Point2D> generateBmdPoints(Canoe canoe) {
        return generateBmdPoints(canoe, generateSfdPoints(canoe));
    }

    /**
     * Generate a list of points to comprise the Bending Moment Diagram from precalculated SFD points
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the BMD.
     */
    public static List<Point2D> generateBmdPoints(Canoe canoe, List<Point2D> sfdPoints) {
        List<Point2D> bmdPoints = new ArrayList<>();
        Point2D firstPoint = sfdPoints.getFirst();

        bmdPoints.add(new Point2D(0,0));
        double currY = 0;
        for (int i = 1; i < sfdPoints.size(); i++) {
            Point2D curr = sfdPoints.get(i);
            // If the two consecutive points are on the same vertical line, create a diagonal line for the BMD
            if (curr.getY() == firstPoint.getY()) {
                bmdPoints.add(new Point2D(CalculusUtils.roundXDecimalDigits(curr.getX(), 3), CalculusUtils.roundXDecimalDigits(currY + firstPoint.getY() * (curr.getX() - firstPoint.getX()), 4)));
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
        bmdPoints.add(new Point2D(canoe.getHull().getLength(),0));

        return bmdPoints;
    }
}
