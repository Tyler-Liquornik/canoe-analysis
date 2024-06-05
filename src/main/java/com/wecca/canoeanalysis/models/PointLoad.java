package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class PointLoad extends Load
{
    private boolean isSupport; // assumed false

    public PointLoad(double mag, double x, boolean isSupport) {
        super(x, mag);
        this.isSupport = isSupport;
    }

    // Stringification distinguishes supports from regular loads
    @Override
    public String toString()
    {
        String label = isSupport ? "Support" : "Load";
        return String.format("%s: %.2fkN, %.2fm",label, mag, x);
    }
}
