package com.wecca.canoeanalysis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Singleton class representing the canoe for the application.
 */
@Getter @Setter @EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PointLoad.class, name = "Point Load"),
        @JsonSubTypes.Type(value = UniformDistributedLoad.class, name = "Distributed Load")
})
public class Canoe
{
    private double length;
    private final ArrayList<Load> loads;

    public Canoe() {
        this.length = 0;
        this.loads = new ArrayList<>();
    }

    public AddLoadResult addLoad(Load l) {
        if (l instanceof PointLoad)
        {
            // Do not add the load if it is zero valued unless if is a support
            // Zero-valued supports are still added as markers for the model and ListView
            if (l.getMag() == 0)
                if (!((PointLoad) l).isSupport())
                    return AddLoadResult.ADDED;
                else
                    l.setMag(0.00); // In case mag is -0 so that the negative doesn't display to the user

            // Search for other loads at the same position, and combine their magnitudes
            for (PointLoad pLoad : getPLoads()) {
                if (pLoad.getX() == l.getX() && !((PointLoad) l).isSupport())
                {
                    double newMag = pLoad.getMag() + l.getMag();
                    if (newMag == 0)
                    {
                        removeLoad(loads.indexOf(pLoad));
                        return AddLoadResult.REMOVED;
                    }
                    loads.get(loads.indexOf(pLoad)).setMag(newMag);
                    pLoad.setMag(newMag);
                    return AddLoadResult.COMBINED;
                }
            }
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        else if (l instanceof UniformDistributedLoad)
        {
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        return AddLoadResult.ADDED;
    }

    @JsonIgnore
    public int getMaxLoadIndex() {
        if (loads.isEmpty()) {return -1;}

        int maxIndex = 0;
        double max = 0;
        for (int i = 0; i < loads.size(); i++)
        {
            double mag = Math.abs(loads.get(i).getMag());
            if (mag > max)
            {
                max = mag;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public void removeLoad(int index) {
        loads.remove(index);
    }

    public void clearLoads() {
        loads.clear();
    }

    @JsonIgnore
    public List<PointLoad> getPLoads() {

        List<PointLoad> pLoads = new ArrayList<>();

        for (Load load : loads)
        {
            if (load instanceof PointLoad pLoad)
                pLoads.add(pLoad);
        }

        return pLoads;
    }

    @JsonIgnore
    public List<UniformDistributedLoad> getDLoads() {

        List<UniformDistributedLoad> dLoads = new ArrayList<>();

        for (Load load : loads)
        {
            if (load instanceof UniformDistributedLoad dLoad)
                dLoads.add(dLoad);
        }

        return dLoads;
    }


    /**
     * Gets x values that separate piecewise intervals with unique equations on the SFD/BMD
     * @return the set of points as x values along the length of the canoe
     */
    @JsonIgnore
    public TreeSet<Double> getSectionEndPoints()
    {
        // Tree ensures sorting, set prevents duplicates
        TreeSet<Double> s = new TreeSet<>();

        // Points included are the locations of point loads and interval boundaries of distributed loads
        for (Load l : loads)
        {
            s.add(l.getX());
            if (l instanceof UniformDistributedLoad distributedLoad)
                s.add(distributedLoad.getRx());
        }

        // Add canoe endpoints to the set if they aren't already
        s.add(0.0);
        s.add(length);

        return s;
    }
}

