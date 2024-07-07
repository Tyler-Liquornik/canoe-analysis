package com.wecca.canoeanalysis.models.functions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For now, this will serve as the only function to define hull shape
 * More will be added later
 */
@Getter @Setter
public class VertexFormParabola implements StringableUnivariateFunction {

    @JsonIgnore
    private double a;
    @JsonIgnore
    private double h;
    @JsonIgnore
    private double k;

    public VertexFormParabola(double a, double h, double k) {
        this.a = a;
        this.h = h;
        this.k = k;
    }

    public VertexFormParabola(String stringRepresentation) {
        Matcher matcher = getFunctionPattern().matcher(stringRepresentation);
        if (matcher.matches()) {
            this.a = Double.parseDouble(matcher.group(1));
            this.h = Double.parseDouble(matcher.group(3));
            this.k = Double.parseDouble(matcher.group(5));
        } else {
            throw new IllegalArgumentException("Invalid function format: " + stringRepresentation);
        }
    }

    @Override
    public String getStringRepresentation() {
        char hSign = h > 0 ? '-' : '+';
        char kSign = k < 0 ? '-' : '+';

        return String.format("%f * (x %s %f)^2 %s %f", a, hSign, Math.abs(h), kSign, Math.abs(k));
    }

    @Override
    public Pattern getFunctionPattern() {
        return Pattern.compile("([+-]?\\s?(?=\\.\\d|\\d)(?:\\d+)?\\.?\\d*)(?:[Ee]([+-]?\\d+))?\\(x\\s?[+-]?\\s?((?=\\.\\d|\\d)(?:\\d+)?\\.?\\d*)(?:[Ee]([+-]?\\d+))?\\)\\s?\\^\\s?2\\s?[+-]?\\s?((?=\\.\\d|\\d)(?:\\d+)?\\.?\\d*)(?:[Ee]([+-]?\\d+))?");
    }

    public Function<Double, Double> getFunction() {
        return x -> a * Math.pow((x - h), 2) + k;
    }

    @Override
    public double value(double v) {
        return getFunction().apply(v);
    }
}
