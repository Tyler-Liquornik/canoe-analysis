package com.wecca.canoeanalysis.diagrams;

import com.wecca.canoeanalysis.CanoeAnalysisController;
import com.wecca.canoeanalysis.models.Canoe;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DiagramLogic {

    /**
     * Set up the canvas/pane for a diagram.
     * @param canoe to work with
     * @param points the points to render on the diagram.
     * @param title the title of the diagram.
     * @param yUnits the units of the y-axis on the diagram.
     * @return a list of series where each series is a section of the overall piecewise function
     */
    public static List<XYChart.Series> setupDiagram(Canoe canoe, List<DiagramPoint> points, String title, String yUnits)
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
        chart.getStylesheets().add(CanoeAnalysisController.class.getResource("css/chart.css").toExternalForm());

        List<XYChart.Series> intervalsAsSeries = getIntervalsAsSeries(canoe, points, yUnits, criticalPoints, chart);

        // Creating the scene and adding the chart to it
        chartPane.getChildren().add(chart);
        Scene scene = new Scene(chartPane, 1125, 750);
        popupStage.setScene(scene);
        popupStage.show();

        return intervalsAsSeries;
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
        if (points.get(0).getX() == 0 && points.get(1).getX() == 0)
            points.remove(0);

        // Same idea for last two points if they double up
        if (points.get(points.size() - 1).getX() == canoe.getLen() && points.get(points.size() - 2).getX() == canoe.getLen())
            points.remove(points.size() - 1);

        // Remove zero from the partition list (always first as the TreeSet is sorted ascending)
        if (partitionsList.get(0) == 0)
            partitionsList.remove(0);

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
}
