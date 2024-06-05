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
    private double m; // canoe mass (able to calculate this from exported solidworks data instead of manually?)
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
            // Do not add the load if it is zero valued
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
                    pLoad.setMag(pLoad.getMag() + l.getMag());
                    if (pLoad.getMag() == 0) {
                        pLoads.remove(pLoad);
                        return AddLoadResult.REMOVED;
                    }
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

    public double getMaxPLoad()
    {
        double max = 0;

        for (PointLoad p : pLoads)
        {
            double mag = Math.abs(p.getMag());

            if (mag > max)
            {
                max = mag;
            }
        }

        return max;
    }

    public double getMinPLoad()
    {
        double min = Integer.MAX_VALUE;

        for (PointLoad p : pLoads)
        {
            double mag = Math.abs(p.getMag());

            if (mag < min)
            {
                min = mag;
            }
        }

        return min;
    }

    public double getMaxDLoad()
    {
        double max = 0;

        for (UniformDistributedLoad d : dLoads)
        {
            double mag = Math.abs(d.getMag());

            if (mag > max)
            {
                max = mag;
            }
        }

        return max;
    }

    public double getMinDLoad()
    {
        double min = Integer.MAX_VALUE;

        for (UniformDistributedLoad d : dLoads)
        {
            double mag = Math.abs(d.getMag());

            if (mag < min)
            {
                min = mag;
            }
        }

        return min;
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
        for (PointLoad p : pLoads) {s.add(p.getX());}
        for (UniformDistributedLoad d : dLoads) {s.add(d.getX()); s.add(d.getRX());}

        // Add canoe endpoints to the set if they aren't already
        s.add(0.0);
        s.add(canoe.getLen());

        return s;
    }
}

