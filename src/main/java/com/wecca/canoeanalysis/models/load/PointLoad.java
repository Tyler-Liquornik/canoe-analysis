package com.wecca.canoeanalysis.models.load;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor @EqualsAndHashCode(callSuper = true)
public class PointLoad extends Load
{
    @JsonProperty("force")
    private double force;
    @JsonProperty("x")
    private double x;
    @JsonIgnore
    private boolean isSupport;

    @JsonCreator
    public PointLoad(@JsonProperty("type") LoadType type,
                     @JsonProperty("force") double force,
                     @JsonProperty("x") double x) {
        super(type);
        this.force = force;
        this.x = x;
        this.isSupport = false;
    }

    public PointLoad(LoadType type, double force, double x, boolean isSupport) {
        super(type);
        this.force = force;
        this.x = x;
        this.isSupport = isSupport;
    }

    public PointLoad(double force, double x, boolean isSupport) {
        this(LoadType.POINT_LOAD, force, x, isSupport);
    }

    @Override
    @JsonIgnore
    public double getMaxSignedValue() {
        return getForce();
    }
}
