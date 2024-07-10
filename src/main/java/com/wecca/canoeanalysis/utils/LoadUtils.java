package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.models.load.*;

import java.util.*;

public class LoadUtils {
    /**
     * Sort a list of loads according to class order, and then by x position
     * @param loads the list of loads to sort
     */
    public static void sortLoads(List<Load> loads) {
        // Define the order to sort by type
        Map<Class<? extends Load>, Integer> classOrder = getLoadsClassOrderSortingMap();

        // Sort by class order, isSupport pLoads go first
        loads.sort((l1, l2) -> {
            // First compare by class order
            int order1 = classOrder.getOrDefault(l1.getClass(), -1);
            int order2 = classOrder.getOrDefault(l2.getClass(), -1);
            if (order1 != order2)
                return Integer.compare(order1, order2);

            // If both are PointLoad, compare by isSupport
            if (l1 instanceof PointLoad && l2 instanceof PointLoad) {
                boolean isSupport1 = ((PointLoad) l1).isSupport();
                boolean isSupport2 = ((PointLoad) l2).isSupport();
                if (isSupport1 != isSupport2) {
                    return Boolean.compare(isSupport2, isSupport1); // true (isSupport) comes first
                }
            }

            // Equal order
            return 0;
        });

        loads.sort(Comparator.comparingDouble(Load::getX));
    }

    /**
     * Describes a map for which load subtype classes should be sorted (ascending by map entry value for a given class)
     * @return a map of Load Subclass : Priority (lower number = higher priority)
     */
    public static Map<Class<? extends Load>, Integer> getLoadsClassOrderSortingMap() {
        Map<Class<? extends Load>, Integer> classOrder = new HashMap<>();
        classOrder.put(PointLoad.class, 0);
        classOrder.put(UniformLoadDistribution.class, 1);
        classOrder.put(DiscreteLoadDistribution.class, 2);
        classOrder.put(PiecewiseContinuousLoadDistribution.class, 3);
        return classOrder;
    }

    /**
     * Insert a hull into a list of loads as a load distribution in O(n) time
     * @param loads the initial list
     * @param hull to add to the list
     * @return the list with the hull added
     */
    public static List<Load> addHullPreserveLoadSorting(List<Load> loads, Hull hull) {
        // Simple cases
        if (loads == null)
            return null;
        if (hull == null)
            return loads;

        Load hullLoad = hull.getSelfWeightDistribution();

        // Define the order to sort by type
        Map<Class<? extends Load>, Integer> classOrder = LoadUtils.getLoadsClassOrderSortingMap();

        // Create a new dupe list to avoid ConcurrentModificationException
        List<Load> loadsList = new ArrayList<>(loads);

        // Find the correct position to insert the hull load
        int insertIndex = 0;
        if (!loadsList.isEmpty()) {
            for (int i = 0; i < loadsList.size(); i++) {
                // Get the load and discretize if necessary
                Load currentLoad = loadsList.get(i);

                // If x-coordinates are equal, compare by class order
                if (hullLoad.getX() == currentLoad.getX()) {
                    int hullClassOrder = classOrder.getOrDefault(hullLoad.getClass(), -1);
                    int currentClassOrder = classOrder.getOrDefault(currentLoad.getClass(), -1);
                    if (hullClassOrder <= currentClassOrder) {
                        insertIndex = i;
                        break;
                    }
                }

                // Compare by x-coordinate
                if (hullLoad.getX() < currentLoad.getX()) {
                    insertIndex = i;
                    break;
                }
            }
        }
        // Add the hull load at the correct position in the temporary list
        loadsList.add(insertIndex, hullLoad);
        return loadsList;
    }

    /**
     * @param loads the list to operate on
     * @return a new list of loads with PiecewiseContinuousLoadDistributions as DiscreteLoadDistributions
     */
    public static List<Load> discretizeLoads(List<Load> loads) {
        loads = loads.stream().map(load -> {
            if (load instanceof PiecewiseContinuousLoadDistribution piecewise)
                return DiscreteLoadDistribution.fromPiecewiseContinuous(load.getType(), piecewise);
            else
                return load;
        }).toList();
        return loads;
    }
}
