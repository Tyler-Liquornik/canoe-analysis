package com.wecca.canoeanalysis.graphics.diagrams;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.util.StringConverter;
import java.util.ArrayList;
import java.util.List;


// Utility class for an axis whose ticks are custom defined specific points
// Purpose is to show only points of interest on charts where at the bounds of piecewise intervals
// Source: https://stackoverflow.com/questions/49462938/javafx-manually-set-numberaxis-ticks
public class FixedTicksNumberAxis extends ValueAxis<Number>
{

    // List of ticks
    private final List<Number> ticks;

    // Formatter
    private final NumberAxis.DefaultFormatter defaultFormatter;

    public FixedTicksNumberAxis(List<Number> ticks) {
        super();
        this.ticks = ticks;
        this.defaultFormatter = new NumberAxis.DefaultFormatter(new NumberAxis());
    }

    @Override
    protected List<Number> calculateMinorTickMarks() {
        return new ArrayList<>();
    }

    @Override
    protected void setRange(Object range, boolean animate) {
        final double[] rangeProps = (double[]) range;
        final double lowerBound = rangeProps[0];
        final double upperBound = rangeProps[1];
        final double scale = rangeProps[2];
        setLowerBound(lowerBound);
        setUpperBound(upperBound);

        currentLowerBound.set(lowerBound);
        setScale(scale);
    }

    @Override
    protected Object getRange() {
        return new double[]{
                getLowerBound(),
                getUpperBound(),
                getScale(),
        };
    }

    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        return ticks;
    }

    @Override
    protected String getTickMarkLabel(Number value) {
        StringConverter<Number> formatter = getTickLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        return formatter.toString(value);
    }
}
