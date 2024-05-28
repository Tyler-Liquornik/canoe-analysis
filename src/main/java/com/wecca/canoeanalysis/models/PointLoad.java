package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class PointLoad implements Load
{
    // Fields
    private double mag; // force magnitude (kN). + / - sign indicates up / down direction
    private double x; // force position on the canoe (m)
    private boolean isSupport; // assumed false

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return this.x / canoeLength * containerWidth;
    }

    // Stringification distinguishes supports from regular loads
    @Override
    public String toString()
    {
        String label = isSupport ? "Support" : "Load";
        return String.format("%s: %.2fkN, %.2fm",label, mag, x);
    }
}
