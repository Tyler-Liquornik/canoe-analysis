package com.wecca.canoeanalysis.models;

import lombok.Getter;

@Getter
public class Section {
    protected double start;
    protected double end;

    public Section(double x, double rx) {
        if (rx <= x || x < 0) {
            throw new IllegalArgumentException("Invalid values: rx must be greater than x, and x must be non-negative.");
        }
        this.start = x;
        this.end = rx;
    }

    public double getLength() {
        return end - start;
    }

    public void setStart(double start) {
        if (end <= start || start < 0) {
            throw new IllegalArgumentException("Invalid value: x must be non-negative and less than rx.");
        }
        this.start = start;
    }

    public void setEnd(double end) {
        if (end <= start) {
            throw new IllegalArgumentException("Invalid value: rx must be greater than x.");
        }
        this.end = end;
    }
}