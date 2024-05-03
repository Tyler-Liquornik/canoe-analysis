package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.models.UniformDistributedLoad;

import java.util.Comparator;

public class UniformDistributedLoadComparator implements Comparator<UniformDistributedLoad>
{
    @Override
    public int compare(UniformDistributedLoad d1, UniformDistributedLoad d2)
    {
        // Compare by left endpoints
        return Double.compare(d1.getLX(), d2.getLX());
    }
}
