package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXTreeView;
import com.wecca.canoeanalysis.components.controls.LoadTreeItem;
import com.wecca.canoeanalysis.models.*;
import javafx.scene.control.TreeItem;
import java.util.List;

public class LoadTreeManagerService {
    private static JFXTreeView<String> loadsTreeView;

    private static final LoadTreeItem root = new LoadTreeItem();

    private static  int numPLoads;
    private static int numDLoads;
    private static int numLoadDists;

    public static void setLoadsTreeView(JFXTreeView<String> loadsTreeView) {
        LoadTreeManagerService.loadsTreeView = loadsTreeView;
        LoadTreeManagerService.loadsTreeView.setRoot(root);
    }

    /**
     * Build the tree view model from the canoe
     */
    public static void buildLoadTreeView(Canoe canoe) {
        numPLoads = numDLoads = numLoadDists = 0;
        root.getChildren().clear();
        List<Load> loads = canoe.getAllLoadsDiscretized();
        // Filter out all zero-valued loads except point supports
        List<Load> filteredLoads = loads.stream().filter(load -> {
            if (!(load instanceof PointLoad pointLoad && pointLoad.isSupport()))
                return load.getForce() != 0;
            else
                return true;
        }).toList();
        for (int i = 0; i < filteredLoads.size(); i++) {
            root.getChildren().add(createLoadTreeItem(i, filteredLoads.get(i)));
        }
        loadsTreeView.setRoot(root);
        loadsTreeView.setShowRoot(false);
    }

    /**
     * Clear the tree and add text as a filler
     * @param enable weather to enable the text with an empty tree
     * @param text the text to display
     */
    public static void enableEmptyTreeFiller(boolean enable, String text) {
        root.getChildren().clear();
        root.getChildrenLoadItems().clear();
        if (enable)
            root.addChild(new LoadTreeItem(-1, -1, text));
        loadsTreeView.setRoot(root);
        loadsTreeView.setShowRoot(false);
    }

    /**
     * @return the selected LoadTreeItem
     */
    public static LoadTreeItem getSelectedLoadTreeItem() {
        TreeItem<String> selectedItem = loadsTreeView.getSelectionModel().getSelectedItem();
        return selectedItem instanceof LoadTreeItem loadTreeItem ? loadTreeItem : null;
    }

    /**
     * @return the loadId of the selected LoadTreeItem
     */
    public static int getSelectedLoadId() {
        LoadTreeItem selectedLoadTreeItem = getSelectedLoadTreeItem();
        if (selectedLoadTreeItem != null) {
            return selectedLoadTreeItem.getLoadId();
        }
        return -1;
    }

    /**
     * @return the nestedId of the selected LoadTreeItem
     */
    public static int getSelectedNestedIndex() {
        LoadTreeItem selectedLoadTreeItem = getSelectedLoadTreeItem();
        if (selectedLoadTreeItem != null && selectedLoadTreeItem.getParent() instanceof LoadTreeItem parentLoadTreeItem) {
            return parentLoadTreeItem.getChildren().indexOf(selectedLoadTreeItem);
        }
        return -1;
    }

    public static int getNumberOfLoadsInTreeView() {
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
            case UniformLoadDistribution dLoad -> {
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
     * @param pLoad the load to create tree item for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createPLoadTreeItem(int loadId, PointLoad pLoad) {
            LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, numPLoads++, pLoad);
            loadTreeItem.addChild(new LoadTreeItem(loadId, 0, String.format("Force: %.2fkN", pLoad.getForce())));
            loadTreeItem.addChild(new LoadTreeItem(loadId, 1, String.format("Position: %.2fm", pLoad.getX())));

            return loadTreeItem;
    }

    /**
     * Create tree items for a given load with its fields as children.
     * @param dLoad the load to create tree items for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createDLoadTreeItem(int loadId, UniformLoadDistribution dLoad) {
        LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, numDLoads++, dLoad);

        loadTreeItem.addChild(new LoadTreeItem(loadId, 0, String.format("Force: %.2fkN/m", dLoad.getMagnitude())));
        loadTreeItem.addChild(new LoadTreeItem(loadId, 1, String.format("Interval L: %.2fm", dLoad.getX())));
        loadTreeItem.addChild(new LoadTreeItem(loadId, 2, String.format("Interval R: %.2fm", dLoad.getRx())));

        return loadTreeItem;
    }

    /**
     * Create a LoadTreeItem for a DiscreteLoadDistribution with its children as its dLoads that make it up
     */
    private static LoadTreeItem createLoadDistTreeItem(int loadId, DiscreteLoadDistribution loadDist) {
        LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, numLoadDists++, loadDist);
        for (int i = 0; i < loadDist.getLoads().size(); i++) {
            LoadTreeItem childLoadTreeItem = createNestedDLoadTreeItem(loadId, i, loadDist.getLoads().get(i));
            loadTreeItem.addChild(childLoadTreeItem);
        }
        return loadTreeItem;
    }


    /**
     * Create tree items for a given load with its fields as children.
     * @param dLoad the load to create tree items for
     * @return the root tree item representing the load
     */
    private static LoadTreeItem createNestedDLoadTreeItem(int loadId, int nestedLoadId, UniformLoadDistribution dLoad) {
        LoadTreeItem loadTreeItem = new LoadTreeItem(loadId, nestedLoadId, nestedLoadId, dLoad);

        loadTreeItem.addChild(new LoadTreeItem(loadId, 0, String.format("Avg Force: %.2fkN/m", dLoad.getMagnitude())));
        loadTreeItem.addChild(new LoadTreeItem(loadId, 1, String.format("Interval L: %.2fm", dLoad.getX())));
        loadTreeItem.addChild(new LoadTreeItem(loadId, 2, String.format("Interval R: %.2fm", dLoad.getRx())));

        return loadTreeItem;
    }
}
