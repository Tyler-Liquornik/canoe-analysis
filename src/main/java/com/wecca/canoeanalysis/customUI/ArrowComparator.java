package com.wecca.canoeanalysis.customUI;

import com.wecca.canoeanalysis.customUI.Arrow;

import java.util.Comparator;

public class ArrowComparator implements Comparator<Arrow>
{
    // Compare arrows by their starting x coordinate
    @Override
    public int compare(Arrow a1, Arrow a2)
    {
        return Double.compare(a1.getStartX(), a2.getStartX());
    }
}
