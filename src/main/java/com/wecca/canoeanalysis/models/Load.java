package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.util.Positionable;

public interface Load extends Positionable {
    double getX();
    void setMag(double mag);
    double getMag();
}
