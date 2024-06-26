package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXTreeView;
import com.wecca.canoeanalysis.components.controls.LoadTreeItem;
import com.wecca.canoeanalysis.models.*;
import lombok.Setter;
import java.util.List;

public class LoadTreeManagerService {

    @Setter
    private static Canoe canoe;
    @Setter
    private static JFXTreeView<String> loadsTreeView;

    private static final LoadTreeItem root = new LoadTreeItem();

    /**
     * Build the tree view model from the canoe
     */
    public static void buildLoadTreeView() {
        List<Load> loads = canoe.getAllLoads();
        for (int i = 0; i < loads.size(); i++) {
            root.getChildren().add(createLoadTreeItem(i, loads.get(i)));
        }
        updateTreeView();
    }

    public static void addLoadToTreeView(Load load) {
        root.addChild(createLoadTreeItem(getNumberOfLoadsInTreeView(),load));
        updateTreeView();
    }

    public static void updateTreeView() {
        loadsTreeView.setRoot(root);
        loadsTreeView.setShowRoot(false);
    }

    public static void deleteSelectedLoadFromTreeView() {
    }

    private static int getLoadTreeItemIndex() {
        return 0;
    }

    private static int getLoadTreeItemNestedIndex() {
        // If isNested
        return 0;
    }

    private static int getNumberOfLoadsInTreeView() {
        return root.getChildren().size();
    }

    /**
     * Create a lod tree item for a given load
     * @param load the load to create tree item for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createLoadTreeItem(int loadId, Load load) {
        switch (load) {
            case PointLoad pLoad -> {
                return createPLoadTreeItem(loadId, pLoad);
            }
            case UniformlyDistributedLoad dLoad -> {
                return createDLoadTreeItem(loadId, dLoad);
            }
            case DiscreteLoadDistribution loadDist -> {
                return createLoadDistTreeItem(loadId, loadDist);
            }
        default -> throw new RuntimeException("Cannot process type " + load.getClass().getName());
        }
    }

    /**
     * Create tree items for a given load with its fields as children.
     * @param load the load to create tree item for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createPLoadTreeItem(int loadId, Load load) {
        if (load instanceof PointLoad pLoad) {
            LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, load);

            loadTreeItem.addChild(new LoadTreeItem(loadId, String.format("Force: %.2fkN/m", pLoad.getForce())));
            loadTreeItem.addChild(new LoadTreeItem(loadId, String.format("Position: %.2fm", pLoad.getX())));

            return loadTreeItem;
        }
        else
            throw new IllegalArgumentException("Cannot process type" + load.getClass().getName());
    }

    /**
     * Create tree items for a given load with its fields as children.
     * @param load the load to create tree items for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createDLoadTreeItem(int loadId, Load load) {
        if (load instanceof UniformlyDistributedLoad dLoad) {
            LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, load);

            loadTreeItem.addChild(new LoadTreeItem(loadId, String.format("Force: %.2fkN/m", dLoad.getMagnitude())));
            loadTreeItem.addChild(new LoadTreeItem(loadId, String.format("Position: [%.2fm, %.2fm]", load.getX(), dLoad.getRx())));

            return loadTreeItem;
        }
        else
            throw new IllegalArgumentException("Cannot process type" + load.getClass().getName());
    }

    /**
     * Create a LoadTreeItem for a DiscreteLoadDistribution with its children as its dLoads that make it up
     */
    private static LoadTreeItem createLoadDistTreeItem(int loadId, Load load) {
        if (load instanceof DiscreteLoadDistribution loadDist) {
            LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, load);
            for (int i = 0; i < loadDist.getLoads().size(); i++) {
                LoadTreeItem childLoadTreeItem = createNestedDLoadTreeItem(loadId, i, loadDist.getLoads().get(i));
                loadTreeItem.addChild(childLoadTreeItem);
            }
            return loadTreeItem;
        }
        else
            throw new IllegalArgumentException("Cannot process type" + load.getClass().getName());
    }


    /**
     * Create tree items for a given load with its fields as children.
     * @param load the load to create tree items for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createNestedDLoadTreeItem(int loadId, int nestedLoadId, Load load) {
        if (load instanceof UniformlyDistributedLoad dLoad) {
            LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, nestedLoadId, load);

            loadTreeItem.addChild(new LoadTreeItem(loadId, nestedLoadId, String.format("Force: %.2fkN/m", dLoad.getMagnitude())));
            loadTreeItem.addChild(new LoadTreeItem(loadId, nestedLoadId, String.format("Position: [%.2fm, %.2fm]", load.getX(), dLoad.getRx())));

            return loadTreeItem;
        }
        else
            throw new IllegalArgumentException("Cannot process type" + load.getClass().getName());
    }
}
