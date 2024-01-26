package com.wecca.canoeanalysis;

import java.util.Comparator;

public class PointLoadComparator implements Comparator<PointLoad> {
    @Override
    public int compare(PointLoad p1, PointLoad p2)
    {
        // Compare PointLoads by their x position
        return Double.compare(p1.getX(), p2.getX());
    }
}