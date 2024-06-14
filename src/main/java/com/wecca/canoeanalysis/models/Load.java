package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public abstract class Load{
    String type;
    double mag;
    double x;

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return (this.x / canoeLength) * containerWidth;
    }
}
