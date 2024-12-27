package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXTooltip;
import com.wecca.canoeanalysis.aop.Traceable;
import com.wecca.canoeanalysis.components.diagrams.FixedTicksNumberAxis;
import com.wecca.canoeanalysis.components.diagrams.DiagramInterval;
import com.wecca.canoeanalysis.services.color.ColorPaletteService;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.load.*;
import com.wecca.canoeanalysis.utils.CalculusUtils;
import javafx.geometry.Point2D;
import javafx.scene.chart.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.*;
import java.util.List;

public class DiagramService {

    /**
     * Sets up the chart for the diagram window.
     *
     * @param canoe  the canoe object containing section end points and length
     * @param points the list of diagram points to be plotted
     * @param yUnits the label for the Y-axis units
     * @return the configured AreaChart
     */
    private static boolean isHoveringOverCircle = false;
    private static double lastTooltipX = -1;
    private static double lastTooltipY = -1;
    private static final double TOOLTIP_UPDATE_THRESHOLD = 0.1;

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
        showMousePositionAsTooltip(chart);

        return chart;
    }
    private static List<Point2D> filterMaxMinPoints(List<Point2D> points) {
        List<Point2D> maxPoints = new ArrayList<>();
        List<Point2D> minPoints = new ArrayList<>();

        for (Point2D point : points) {
            if (point.getY() == 0) {
                continue; // Skip points with a Y-value of 0
            }
            findMaxPoints(maxPoints, point); // Identify and store maximum points
            findMinPoints(minPoints, point); // Identify and store minimum points
        }

        maxPoints.addAll(minPoints); // Combine max and min points
        return maxPoints;
    }

    /**
     * Updates the list of maximum points.
     *
     * @param points the list of current maximum points
     * @param point  the point to evaluate
     */
    private static void findMaxPoints(List<Point2D> points, Point2D point) {
        if (points.isEmpty() || point.getY() > points.get(0).getY()) {
            points.clear(); // Clear the list if a new maximum is found
            points.add(point);
        } else if (point.getY() == points.get(0).getY()) {
            points.add(point); // Add the point if it matches the current maximum
        }
    }

    /**
     * Updates the list of minimum points.
     *
     * @param points the list of current minimum points
     * @param point  the point to evaluate
     */
    private static void findMinPoints(List<Point2D> points, Point2D point) {
        if (points.isEmpty() || point.getY() < points.get(0).getY()) {
            points.clear(); // Clear the list if a new minimum is found
            points.add(point);
        } else if (point.getY() == points.get(0).getY()) {
            points.add(point); // Add the point if it matches the current minimum
        }
    }

    /**
     * Displays a tooltip on the chart to show mouse position values.
     *
     * @param chart the AreaChart to add tooltips to
     */
    public static void showMousePositionAsTooltip(AreaChart<Number, Number> chart) {
        Tooltip tooltip = new Tooltip(); // Initialize tooltip
        tooltip.setShowDelay(Duration.millis(0));
        tooltip.setHideDelay(Duration.millis(0));
        tooltip.setAutoHide(false);

        chart.setOnMouseMoved(event -> {
            tooltip.hide(); // Hide tooltip when the mouse moves
            if (!isHoveringOverCircle) {
                updateTooltip(event, chart, tooltip); // Update tooltip with the new position
            }
        });

        Tooltip.install(chart, tooltip); // Attach the tooltip to the chart
    }

    /**
     * Updates the tooltip content and position based on mouse movement.
     *
     * @param event   the mouse event
     * @param chart   the chart where the tooltip is displayed
     * @param tooltip the tooltip to update
     */
    private static void updateTooltip(MouseEvent event, AreaChart<Number, Number> chart, Tooltip tooltip) {
        double mouseX = event.getX() - chart.getXAxis().getLayoutX();
        double mouseY = event.getY() - chart.getYAxis().getLayoutY();
        Axis<Number> xAxis = chart.getXAxis();
        Axis<Number> yAxis = chart.getYAxis();

        if (xAxis instanceof ValueAxis && yAxis instanceof ValueAxis) {
            ValueAxis<Number> xValueAxis = (ValueAxis<Number>) xAxis;
            ValueAxis<Number> yValueAxis = (ValueAxis<Number>) yAxis;

            if (mouseX >= 0 && mouseX <= xValueAxis.getWidth() && mouseY >= 0 && mouseY <= yValueAxis.getHeight()) {
                double xValue = xValueAxis.getValueForDisplay(mouseX).doubleValue();
                double yValue = yValueAxis.getValueForDisplay(mouseY).doubleValue();

                // Update tooltip only if the mouse has moved significantly
                if (Math.abs(mouseX - lastTooltipX) > TOOLTIP_UPDATE_THRESHOLD ||
                        Math.abs(mouseY - lastTooltipY) > TOOLTIP_UPDATE_THRESHOLD) {

                    tooltip.setText(String.format("Distance: %.4f, Moment: %.4f", xValue, yValue));
                    tooltip.setX(event.getScreenX() + 10);
                    tooltip.setY(event.getScreenY() + 10);
                    tooltip.show(chart, event.getScreenX() + 10, event.getScreenY() + 10);

                    lastTooltipX = mouseX; // Update the last X position
                    lastTooltipY = mouseY; // Update the last Y position
                }
            } else {
                tooltip.hide(); // Hide tooltip if mouse is out of bounds
            }
        }
    }

    /**
     * Adds circles with tooltips to specific data points in the chart.
     *
     * @param data            the chart data point to annotate
     * @param filteredPoints  the list of filtered points to annotate
     */
    private static void addCircleToPoint(XYChart.Data<Number, Number> data, List<Point2D> filteredPoints) {
        double dataX = data.getXValue().doubleValue();
        double dataY = data.getYValue().doubleValue();

        for (Point2D point : filteredPoints) {
            if (point.getY() != dataY || point.getX() != dataX) {
                continue; // Skip if the point does not match
            }

            Circle circle = new Circle(5, ColorPaletteService.getColor("primary")); // Create a circle for the point
            data.setNode(circle);

            Tooltip tooltip = new Tooltip(String.format("Distance: %.4f, Moment: %.4f", point.getX(), point.getY()));
            Tooltip.install(circle, tooltip); // Attach a tooltip to the circle

            circle.setOnMouseEntered(event -> {
                isHoveringOverCircle = true;
                tooltip.show(circle, event.getScreenX() + 10, event.getScreenY() + 10); // Show tooltip on hover
                circle.setRadius(7); // Highlight the circle
                circle.setFill(ColorPaletteService.getColor("primary-light"));
            });

            circle.setOnMouseExited(event -> {
                isHoveringOverCircle = false;
                tooltip.hide(); // Hide tooltip when mouse exits
                circle.setRadius(5); // Reset circle size
                circle.setFill(ColorPaletteService.getColor("primary"));
            });
            return;
        }
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

    public static void addSeriesToChart(Canoe canoe, List<Point2D> points, String yUnits, AreaChart<Number, Number> chart) {

        TreeSet<Double> criticalPoints = canoe.getSectionEndpoints();
        List<Point2D> filteredPoints = filterMaxMinPoints(points);

        // Adding the sections of the pseudo piecewise function separately
        boolean set = false; // only need to set the name of the series once since its really one piecewise function
        List<List<Point2D>> intervals = partitionPoints(canoe, points, criticalPoints);
        for (List<Point2D> interval : intervals) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (Point2D point : interval) {
                XYChart.Data<Number, Number> data = new XYChart.Data<>(point.getX(), point.getY());
                addCircleToPoint(data, filteredPoints);
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

    /**
     * Consider the list of points is a "pseudo piecewise function"
     * This method breaks it into a set of "pseudo functions"
     * Pseudo because it's just a set of points rather with no clear partitions by their definition only
     * @param canoe to work with
     * @param points act together as a piecewise function
     * @param partitions the locations where the form of the piecewise changes
     * @return a list containing each section of the piecewise pseudo functions with unique form
     */
    private static List<List<Point2D>> partitionPoints(Canoe canoe, List<Point2D> points, TreeSet<Double> partitions) {
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
    @Traceable
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
    @Traceable
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

        LoggerService.logPoints(sfdPoints);

        // Return the generated points
        return new ArrayList<>(sfdPoints);
    }

    /**
     * Generate a list of points to comprise the Bending Moment Diagram.
     * @param canoe the canoe object with loads.
     * @return the list of points to render for the BMD.
     */
    @Traceable
    public static List<Point2D> generateBmdPoints(Canoe canoe) {
        // Gets the SFD points for the canoe
        List<Point2D> sfdPoints = generateSfdPoints(canoe);
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
