package com.wecca.canoeanalysis.components.controls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wecca.canoeanalysis.models.DiscreteLoadDistribution;
import com.wecca.canoeanalysis.models.Load;
import com.wecca.canoeanalysis.models.PointLoad;
import com.wecca.canoeanalysis.models.UniformLoadDistribution;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter @Setter
public class LoadTreeItem extends TreeItem<String> {
    private int loadId; // base load (pLoad, non-nested dLoad, loadDist)
    private int loadTypeId; // within type
    private int nestedLoadId; // within nesting of another load (loadDist)
    private int fieldId; // within fields of a load
    private LoadTreeItem parentLoadItems;
    private List<LoadTreeItem> childrenLoadItems;

    // Root node
    public LoadTreeItem() {
        super("");
        this.loadId = -1;
        this.nestedLoadId = -1;
        this.fieldId = -1;
        this.childrenLoadItems = new ArrayList<>();
    }

    // Non-nested load field
    public LoadTreeItem(int loadId, int fieldId, String value) {
        super(value);
        this.loadId = loadId;
        this.loadTypeId = -1;
        this.nestedLoadId = -1;
        this.fieldId = fieldId;
        this.childrenLoadItems = new ArrayList<>();
    }

    // Nested load field
    public LoadTreeItem(int loadId, int nestedLoadId, int fieldId, String value) {
        super(value);
        this.loadId = loadId;
        this.loadTypeId = -1;
        this.nestedLoadId = nestedLoadId;
        this.fieldId = fieldId;
        this.childrenLoadItems = new ArrayList<>();
    }

    // Non-nested load (pLoad, dLoad, loadDist)
    public LoadTreeItem(int loadId, int loadTypeId, Load load) {
        super(String.format("%s: %c_%d", load.getType().getDescription(), load.getType().getVariable(), (loadTypeId + 1)));
        this.loadId = loadId;
        this.loadTypeId = loadTypeId;
        this.nestedLoadId = -1;
        this.fieldId = -1;
        this.childrenLoadItems = new ArrayList<>();
    }

    // Nested load (dLoad child of loadDist)
    public LoadTreeItem(int loadId, int nestedLoadId, int loadTypeId, Load load) {
        super(String.format("%s: %c_%s", load.getType().getDescription(), load.getType().getVariable(), (loadTypeId + 1)));
        this.loadId = loadId;
        this.loadTypeId = loadTypeId;
        this.nestedLoadId = nestedLoadId;
        this.fieldId = -1;
        this.childrenLoadItems = new ArrayList<>();
    }

    /**
     * @param child the child to add, need to ensure that the child's indices are correct
     */
    public void addChild(LoadTreeItem child) {
        child.setParentLoadItems(this);
        childrenLoadItems.add(child);
        childrenLoadItems.sort(Comparator.comparingInt(LoadTreeItem::getLoadId));
        getChildren().setAll(childrenLoadItems);
    }

    public boolean isNested() {
        return nestedLoadId > -1;
    }

    public boolean isField() {
        return  fieldId > -1;
    }
}
