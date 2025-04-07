package com.wecca.canoeanalysis.models.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * A Zone is a numerical range defined by two positive numbers [x, rx], where x != rx so that range can't collapse to a point.
 * Zones have a sort of "direction" in the sense that if rx > x, they can be said to be 'oriented backward'
 * Section instances, an extension of Zones, have the added restriction that x > rx, (i.e. 'oriented forward')
 */
@Getter @Setter @EqualsAndHashCode
public class Zone {

    @JsonProperty("x")
    protected double x;
    @JsonProperty("rx")
    protected double rx;

    public Zone() {}

    @JsonCreator
    public Zone(@JsonProperty("x") double x, @JsonProperty("rx") double rx) {
        if (wouldZoneCollapse(x, rx) || x < 0 || rx < 0)
            throw new IllegalArgumentException(
                    String.format("Invalid values: x = %f, rx = %f. Both must be positive and not equal, or the zone's range would collapse.", x, rx)
            );
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
        if (wouldZoneCollapse(x, rx))
            throw new IllegalArgumentException("Invalid value: " + x + ", range " + "[" + x + ", " + x + "] would collapse the zone");
        this.x = x;
    }

    public void setRx(double rx) {
        if (wouldZoneCollapse(x, rx))
            throw new IllegalArgumentException("Invalid value: " + rx + ", range " + "[" + rx + ", " + rx + "] would collapse the zone");
        this.rx = rx;
    }

    /**
     * Yes, this is a very simple method. it's an abstraction made on purpose :)
     */
    @JsonIgnore
    public boolean isOrientedForward() {
        return rx > x;
    }

    /**
     * Yes, this is a very simple method. it's an abstraction made on purpose :)
     */
    protected boolean wouldZoneCollapse(double x, double rx) {
        return x == rx;
    }
}
