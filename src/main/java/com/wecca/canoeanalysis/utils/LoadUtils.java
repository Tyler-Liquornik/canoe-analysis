package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.models.canoe.Canoe;
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
            if (l1 instanceof PointLoad p1 && l2 instanceof PointLoad p2) {
                boolean isSupport1 = p1.isSupport();
                boolean isSupport2 = p2.isSupport();
                if (isSupport1 != isSupport2)
                    return Boolean.compare(isSupport2, isSupport1); // support load comes first
            }

            // Equal order precedence, i.e. l1 = l2 for sorting purposes
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
        classOrder.put(PiecewiseContinuousLoadDistribution.class, 0);
        classOrder.put(DiscreteLoadDistribution.class, 1);
        classOrder.put(PointLoad.class, 2);
        classOrder.put(UniformLoadDistribution.class, 3);
        return classOrder;
    }

    /**
     * Insert a hull into a list of loads as a load distribution in O(n) time
     * @param loads the initial list
     * @param hull to add to the list
     * @return the list with the hull added
     */
    public static List<Load> addHullAsLoad(List<Load> loads, Hull hull) {
        // Simple cases
        if (loads == null)
            return null;
        if (hull == null || hull.getSelfWeightDistribution().getForce() == 0)
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
     * @param bulkheadMap a map describing where bulkheads are for typing the sections with LoadType objects
     * @return a new list of loads with PiecewiseContinuousLoadDistributions as DiscreteLoadDistributions
     * Note: discretization also separates out LoadTypes
     */
    public static List<Load> discretizeLoads(List<Load> loads, List<SectionPropertyMapEntry> bulkheadMap) {
        loads = loads.stream().map(load -> {
            if (load instanceof PiecewiseContinuousLoadDistribution piecewise)
                return DiscreteLoadDistribution.fromPiecewiseTyped(load.getType(), piecewise, bulkheadMap);
            else
                return load;
        }).toList();
        return loads;
    }

    /**
     * @deprecated by new floating solver algorithm which can solve asymmetrical load cases so we no longer need to check for symmetry
     * Flips a list of loads across a hull's length
     * Used in the process of checking for symmetrical loading
     * @param rightHalf the list of loads on the right half of the canoe
     * @param hullLength the length of the hull
     * @return the flipped list of loads
     */
    public static List<Load> flipLoadsFromRightHalf(List<Load> rightHalf, double hullLength) {
        List<Load> flippedRightHalf = new ArrayList<>();
        for (int i = rightHalf.size() - 1; i >= 0; i--) {
            Load load = rightHalf.get(i);
            Load flippedLoad = switch (load) {
                case PointLoad pLoad -> new PointLoad(pLoad.getForce(), CalculusUtils.roundXDecimalDigits(hullLength - pLoad.getX(), 6), pLoad.isSupport());
                case UniformLoadDistribution dLoad -> {
                    double flippedX = CalculusUtils.roundXDecimalDigits(hullLength - dLoad.getX(), 6);
                    double flippedRx = CalculusUtils.roundXDecimalDigits(hullLength - dLoad.getSection().getRx(),6 );
                    yield new UniformLoadDistribution(dLoad.getMagnitude(), flippedRx, flippedX);
                }
                default -> throw new IllegalArgumentException("Cannot process load of type: " + load.getClass());
            };
            flippedRightHalf.add(flippedLoad);
        }
        return flippedRightHalf;
    }

    /**
     * @param canoe the canoe on which to check other loads for comparison
     * @param load the load to compare in the ratio
     * @return the absolute ratio of load the canoe's maximum load value (of any given individual load) to this load
     */
    public static double getLoadMagnitudeRatio(Canoe canoe, Load load) {
        double loadMagnitudeRatio = Math.abs(load.getMaxSignedValue() / canoe.getMaxLoadValue());

        // Clip load length if too small (i.e. ratio is too large)
        if (loadMagnitudeRatio < Math.abs(GraphicsUtils.acceptedBeamLoadGraphicHeightRange[0] / GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1]))
            loadMagnitudeRatio = Math.abs(GraphicsUtils.acceptedBeamLoadGraphicHeightRange[0] / GraphicsUtils.acceptedBeamLoadGraphicHeightRange[1]);
        return loadMagnitudeRatio;
    }
}
