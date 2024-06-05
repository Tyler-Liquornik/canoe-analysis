package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.util.Positionable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public abstract class Load implements Positionable {

    double x;
    double mag;

    // Scaled on the canoe to the size of the canoe (beam) container in pixels on the GUI
    public double getXScaled(double containerWidth, double canoeLength)
    {
        return (this.x / canoeLength) * containerWidth;
    }
}
