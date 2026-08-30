package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an interval [x, rx] where 0 <= x < rx
 * In essence, this is just a sub-interval of R^+
 * Stricter than it's parent class Zone in that rx > x is not permitted (i.e. a Section is a forwards oriented Zone)
 * Note: see Zone for definition of orientation
 */
public class Section extends Zone {

    public Section() {
        super();
    }

    @JsonCreator
    public Section(@JsonProperty("x") double x, @JsonProperty("rx") double rx) {
        super(x, rx);
        if (!isOrientedForward() || x < 0)
            throw new IllegalArgumentException("Invalid values: start must be non-negative and less than end.");
    }

    @Override
    public void setX(double x) {
        if (!isOrientedForward() && !wouldZoneCollapse(x, rx) || x < 0)
            throw new IllegalArgumentException("Invalid value: start must be non-negative and less than end.");
        this.x = x;
    }

    @Override
    public void setRx(double rx) {
        if (!isOrientedForward() && !wouldZoneCollapse(x, rx))
            throw new IllegalArgumentException("Invalid value: start must be greater than end.");
        this.rx = rx;
    }
}