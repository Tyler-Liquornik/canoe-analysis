package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.utility.Positionable;

// Added to be used only for loads to separate loads from graphics which are both positionable
public interface Load extends Positionable {
    double getX();
}
