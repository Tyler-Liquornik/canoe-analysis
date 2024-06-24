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

    public PointLoad(double force, double x, boolean isSupport) {
        super("Point Load");
        this.force = force;
        this.x = x;
        this.isSupport = isSupport;
    }

    // Stringification distinguishes supports from regular loads
    @Override
    public String toString()
    {
        String label = isSupport ? "Support" : "Load";
        return String.format("%s: %.2fkN, %.2fm",label, force, x);
    }
}
