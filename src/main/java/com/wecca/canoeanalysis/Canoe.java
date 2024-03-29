package com.wecca.canoeanalysis;

import java.util.*;

/**
 * Singleton class representing the canoe for the application.
 */
public final class Canoe
{
    private static Canoe canoe = null;
    double len; // canoe length
    ArrayList<PointLoad> pLoads; // point loads on the canoe
    ArrayList<UniformDistributedLoad> dLoads; // uniformly distributed loads on the canoe
    double m; // canoe mass (able to calculate this from exported solidworks data instead of manually?)

    private Canoe() {
        this.len = 0;
        this.pLoads = new ArrayList<>();
        this.dLoads = new ArrayList<>();
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
        //Search for other loads at the same position, and combine their magnitudes
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
        pLoads.sort(new PointLoadComparator()); // Keep the ArrayList sorted
        return AddPointLoadResult.ADDED;
    }
    public void addDLoad(UniformDistributedLoad d) {dLoads.add(d); dLoads.sort(new UniformDistributedLoadComparator());}
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


    // Set of endpoints sections with unique equations on the SFD/BMD
    public Set<Double> getSectionEndPoints()
    {
        Set<Double> s = new HashSet<>();

        for (PointLoad p : pLoads) {s.add(p.getX());}
        for (UniformDistributedLoad d : dLoads) {s.add(d.getLX()); s.add(d.getRX());}

        return s;
    }
}

