package com.wecca.canoeanalysis;

import java.util.*;

public class Canoe
{
    double len; // canoe length
    ArrayList<PointLoad> pLoads; // point loads on the canoe
    ArrayList<UniformDistributedLoad> dLoads; // uniformly distributed loads on the canoe
    double m; // canoe mass (able to calculate this from exported solidworks data instead of manually?)

    public Canoe(double len, ArrayList<PointLoad> pLoads, ArrayList<UniformDistributedLoad> dLoads)
    {
        this.len = len;
        this.pLoads = pLoads; pLoads.sort(new PointLoadComparator());
        this.dLoads = dLoads; dLoads.sort(new UniformDistributedLoadComparator());
    }

    public double getLen() {return len;}
    public void setLen(double len) {this.len = len;}

    // Keep the ArrayList sorted
    public void addPLoad(PointLoad p) {pLoads.add(p); pLoads.sort(new PointLoadComparator());}
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

