package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * Represents an interval [x, rx] where 0 <= x < rx
 */
@Getter
public class Section {
    protected double x;
    protected double rx;

    public Section(double x, double rx) {
        if (rx <= x || x < 0) {
            throw new IllegalArgumentException("Invalid values: start must be non-negative and less than end.");
        }
        this.x = x;
        this.rx = rx;
    }

    @JsonIgnore
    public double getLength() {
        return rx - x;
    }

    public void setX(double x) {
        if (rx <= x || x < 0)
            throw new IllegalArgumentException("Invalid value: start must be non-negative and less than end.");
        this.x = x;
    }

    public void setRx(double rx) {
        if (rx <= x)
            throw new IllegalArgumentException("Invalid value: start must be greater than end.");
        this.rx = rx;
    }
}