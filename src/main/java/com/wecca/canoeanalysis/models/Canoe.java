package com.wecca.canoeanalysis.models;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Singleton class representing the canoe for the application.
 */
@Getter @Setter
public final class Canoe
{
    private double len; // canoe length
    private final ArrayList<PointLoad> pLoads; // point loads on the canoe
    private final ArrayList<UniformDistributedLoad> dLoads; // uniformly distributed loads on the canoe
    private final ArrayList<Load> loads; // will sync with listview and loadContainer order
    private static Canoe canoe = null;

    private Canoe() {
        this.len = 0;
        this.pLoads = new ArrayList<>();
        this.dLoads = new ArrayList<>();
        this.loads = new ArrayList<>();
    }

    /**
     * Get the Canoe singleton instance. Use this rather than a constructor.
     * @return the Canoe singleton.
     */
    public static Canoe getInstance() {
        if (canoe == null) {
            canoe = new Canoe();
        }
        return canoe;
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
            for (PointLoad pLoad : pLoads) {
                if (pLoad.getX() == l.getX() && !((PointLoad) l).isSupport())
                {
                    double newMag = pLoad.getMag() + l.getMag();
                    if (newMag == 0)
                    {
                        removeLoad(loads.indexOf(pLoad));
                        pLoads.remove(pLoad);
                        return AddLoadResult.REMOVED;
                    }
                    loads.get(loads.indexOf(pLoad)).setMag(newMag);
                    pLoad.setMag(newMag);
                    return AddLoadResult.COMBINED;
                }
            }

            pLoads.add((PointLoad) l);
            pLoads.sort(Comparator.comparingDouble(PointLoad::getX));
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        else if (l instanceof UniformDistributedLoad)
        {
            dLoads.add((UniformDistributedLoad) l);
            dLoads.sort(Comparator.comparingDouble(UniformDistributedLoad::getX));
            loads.add(l);
            loads.sort(Comparator.comparingDouble(Load::getX));
        }

        return AddLoadResult.ADDED;
    }

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

    public int getMinLoadIndex() {
        if (loads.isEmpty()) {return -1;}

        int minIndex = 0;
        double min = 0;
        for (int i = 0; i < loads.size(); i++)
        {
            double mag = Math.abs(loads.get(i).getMag());
            if (mag < min)
            {
                min = mag;
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * Regenerates the pLoads and dLoads lists from the main loads list.
     */
    private void regenerateLoads() {
        pLoads.clear();
        dLoads.clear();

        for (Load load : loads) {
            if (load instanceof PointLoad) {
                pLoads.add((PointLoad) load);
            } else if (load instanceof UniformDistributedLoad) {
                dLoads.add((UniformDistributedLoad) load);
            }
        }

        // Sort the regenerated lists
        pLoads.sort(Comparator.comparingDouble(PointLoad::getX));
        dLoads.sort(Comparator.comparingDouble(UniformDistributedLoad::getX));
    }

    public void removeLoad(int index) {
        loads.remove(index);
        regenerateLoads();
    }

    public void clearLoads() {
        loads.clear();
        pLoads.clear();
        dLoads.clear();
    }


    /**
     * Gets x values that separate piecewise intervals with unique equations on the SFD/BMD
     * @return the set of points as x values along the length of the canoe
     */
    public TreeSet<Double> getSectionEndPoints()
    {
        // Tree ensures sorting, set prevents duplicates
        TreeSet<Double> s = new TreeSet<>();

        // Points included are the locations of point loads and interval boundaries of distributed loads
        for (Load l : loads) {s.add(l.getX());}

        // Add canoe endpoints to the set if they aren't already
        s.add(0.0);
        s.add(canoe.getLen());

        return s;
    }
}

