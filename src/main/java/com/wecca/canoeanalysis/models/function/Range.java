package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * A range defined by two positive numbers [x, rx], where x != rx so that range can't collapse to a point.
 */
@Getter @Setter @EqualsAndHashCode
public class Range {
    @JsonProperty("x")
    protected double x;
    @JsonProperty("rx")
    protected double rx;

    public Range() {}

    @JsonCreator
    public Range(@JsonProperty("x") double x, @JsonProperty("rx") double rx) {
        if (x == rx || x < 0 || rx < 0)
            throw new IllegalArgumentException("Invalid values: x and rx must be positive and not equal or the range would collapse");
        this.x = x;
        this.rx = rx;
    }

    /**
     * @return the length of the interval regardless of the order of the pair: |rx - x|
     */
    @JsonIgnore
    public double getLength() {
        return Math.abs(rx - x);
    }

    public void setX(double x) {
        if (x == rx)
            throw new IllegalArgumentException("Invalid value: " + x + ", range " + "[" + x + ", " + x + "] would collapse");
        this.x = x;
    }

    public void setRx(double rx) {
        if (x == rx)
            throw new IllegalArgumentException("Invalid value: " + rx + ", range " + "[" + rx + ", " + rx + "] would collapse");
        this.rx = rx;
    }
}
