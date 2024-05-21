package com.wecca.canoeanalysis.models;

import com.wecca.canoeanalysis.utility.Positionable;

import java.util.*;

/**
 * Singleton class representing the canoe for the application.
 */
public final class Canoe
{
    private static Canoe canoe = null;
    private double len; // canoe length
    private ArrayList<PointLoad> pLoads; // point loads on the canoe
    private ArrayList<UniformDistributedLoad> dLoads; // uniformly distributed loads on the canoe
    private double m; // canoe mass (able to calculate this from exported solidworks data instead of manually?)
    private ArrayList<Positionable> loads; // All the loads in one array

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

    public double getLen() {return len;}
    public void setLen(double len) {this.len = len;}

    public double getM() {
        return m;
    }
    public void setM(double m) {
        this.m = m;
    }

    public AddPointLoadResult addPLoad(PointLoad p) {

        // Do not add the load if it is zero valued
        if (p.getMag() == 0)
            return AddPointLoadResult.ADDED;

        // Search for other loads at the same position, and combine their magnitudes
        for (PointLoad pLoad : pLoads) {
            if (pLoad.getX() == p.getX()) {
                pLoad.setMag(pLoad.getMag() + p.getMag());
                if (pLoad.getMag() == 0) {
                    pLoads.remove(pLoad);
                    return AddPointLoadResult.REMOVED;
                }
                return AddPointLoadResult.COMBINED;
            }
        }

        pLoads.add(p);
        pLoads.sort(Comparator.comparingDouble(PointLoad::getX));
        loads.add(p);
        loads.sort(Comparator.comparingDouble(Positionable::getX));
        return AddPointLoadResult.ADDED;
    }
    public void addDLoad(UniformDistributedLoad d)
    {
        dLoads.add(d);
        dLoads.sort(Comparator.comparingDouble(UniformDistributedLoad::getX));
        loads.add(d);
        loads.sort(Comparator.comparingDouble(Positionable::getX));
    }
    public ArrayList<PointLoad> getPLoads() {return pLoads;}
    public ArrayList<UniformDistributedLoad> getDLoads() {return dLoads;}


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
            double mag = Math.abs(d.getW());

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
            double mag = Math.abs(d.getW());

            if (mag < min)
            {
                min = mag;
            }
        }

        return min;
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

