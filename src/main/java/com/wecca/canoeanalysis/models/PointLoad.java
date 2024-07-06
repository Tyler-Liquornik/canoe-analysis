package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class PointLoad extends Load
{
    private double force;
    private double x;
    @JsonIgnore
    private boolean isSupport;

    public PointLoad(LoadType type, double force, double x, boolean isSupport) {
        super(type);
        this.force = force;
        this.x = x;
        this.isSupport = isSupport;
    }

    public PointLoad(double force, double x, boolean isSupport) {
        this(LoadType.POINT_LOAD, force, x, isSupport);
    }
}
