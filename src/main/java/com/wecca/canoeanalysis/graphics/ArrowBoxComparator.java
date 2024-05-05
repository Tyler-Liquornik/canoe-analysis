package com.wecca.canoeanalysis.graphics;

import java.util.Comparator;

public class ArrowBoxComparator implements Comparator<ArrowBox>
{
    // Comparison by leftmost point
    @Override
    public int compare(ArrowBox a1, ArrowBox a2) {
        return Double.compare(a1.getLX(), a2.getLX());
    }
}
