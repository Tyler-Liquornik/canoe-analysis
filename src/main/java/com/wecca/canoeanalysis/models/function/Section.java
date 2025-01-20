package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an interval [x, rx] where 0 <= x < rx
 * In essence, this is just a sub-interval of R^+
 * Stricter than it's parent class Section in that rx > x is fine
 */
public class Section extends Range {

    @JsonCreator
    public Section(@JsonProperty("x") double x, @JsonProperty("rx") double rx) {
        super(x, rx);
        if (rx <= x || x < 0)
            throw new IllegalArgumentException("Invalid values: start must be non-negative and less than end.");
    }

    @Override
    public void setX(double x) {
        if (rx <= x || x < 0)
            throw new IllegalArgumentException("Invalid value: start must be non-negative and less than end.");
        this.x = x;
    }

    @Override
    public void setRx(double rx) {
        if (rx <= x)
            throw new IllegalArgumentException("Invalid value: start must be greater than end.");
        this.rx = rx;
    }
}