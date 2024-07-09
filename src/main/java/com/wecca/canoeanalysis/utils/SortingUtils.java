package com.wecca.canoeanalysis.utils;

import com.wecca.canoeanalysis.components.graphics.*;
import com.wecca.canoeanalysis.models.load.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortingUtils {
    public static void sortLoads(List<Load> loads) {
        // Define the order to sort by type
        Map<Class<? extends Load>, Integer> classOrder = new HashMap<>();
        classOrder.put(PointLoad.class, 0);
        classOrder.put(UniformLoadDistribution.class, 1);
        classOrder.put(DiscreteLoadDistribution.class, 2);
        classOrder.put(PiecewiseContinuousLoadDistribution.class, 3);

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

    public static void sortGraphics(List<Graphic> graphics) {
        // Define the order to sort by type
        Map<Class<? extends Graphic>, Integer> classOrder = new HashMap<>();
        classOrder.put(TriangleStand.class, 0);
        classOrder.put(Arrow.class, 1);
        classOrder.put(ArrowBox.class, 2);
        classOrder.put(Curve.class, 3);
        classOrder.put(Beam.class, 4);

        // Sort by type, and then by position
        graphics.sort(Comparator.comparingInt(l -> classOrder.getOrDefault(l.getClass(), -1)));
        graphics.sort(Comparator.comparingDouble(Graphic::getX));
    }
}
