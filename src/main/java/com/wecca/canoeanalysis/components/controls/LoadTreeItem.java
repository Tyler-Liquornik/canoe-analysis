package com.wecca.canoeanalysis.components.controls;

import com.wecca.canoeanalysis.models.Load;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

public class LoadTreeItem extends TreeItem<String> {
    @Getter @Setter
    private int loadId;
    @Getter @Setter
    private int nestedLoadId;
    private final ObservableList<LoadTreeItem> children = FXCollections.observableArrayList();

    // Root node (should be set to not show)
    public LoadTreeItem() {
        super("");
        this.loadId = -1;
        this.nestedLoadId = -1;
    }

    // Non-nested load field
    public LoadTreeItem(int loadId, String value) {
        super(value);
        this.loadId = loadId;
        this.nestedLoadId = -1;
    }

    // Nested load field
    public LoadTreeItem(int loadId, int nestedLoadId, String value) {
        super(value);
        this.loadId = loadId;
        this.nestedLoadId = nestedLoadId;
    }

    // Non-nested load (pLoad, dLoad, loadDist)
    public LoadTreeItem(int loadId, Load load) {
        super(load.getType());
        this.loadId = loadId;
        this.nestedLoadId = -1;
    }

    // Nested load (dLoad child of loadDist)
    public LoadTreeItem(int loadId, int nestedLoadId, Load load) {
        super(load.getType());
        this.loadId = loadId;
        this.nestedLoadId = nestedLoadId;
    }

    // Method to add child to the overridden children list and ensure children stay sorted
    public void addChild(LoadTreeItem child) {
        children.add(child);
        for (int i = 0; i < children.size(); i++) {children.get(i).setLoadId(i);}
        children.sort(Comparator.comparingInt(LoadTreeItem::getLoadId));
        getChildren().setAll(children);
    }

    public boolean isNested() {
        return nestedLoadId > -1;
    }
}
