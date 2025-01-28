package com.wecca.canoeanalysis.services;

import com.jfoenix.controls.JFXTreeView;
import com.wecca.canoeanalysis.components.controls.LoadTreeItem;
import com.wecca.canoeanalysis.controllers.modules.BeamController;
import com.wecca.canoeanalysis.models.canoe.Canoe;
import com.wecca.canoeanalysis.models.load.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Manager all functionality of a load tree for use in the beam module
 */
public class LoadTreeManagerService {
    private static JFXTreeView<String> loadsTreeView;
    @Getter
    private static final LoadTreeItem root = new LoadTreeItem();
    @Setter
    private static BeamController beamController;

    private static  int numPLoads;
    private static int numDLoads;
    private static int numLoadDists;

    public static void setLoadsTreeView(JFXTreeView<String> loadsTreeView) {
        LoadTreeManagerService.loadsTreeView = loadsTreeView;
        LoadTreeManagerService.loadsTreeView.setRoot(root);

        loadsTreeView.setEditable(true);
        loadsTreeView.setCellFactory(tv -> new TreeCell<String>() {
            private TextField textField;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                if (getItem() == null) {
                    return;
                }

                String fullText = getItem();


                int colonIndex = fullText.indexOf(':');
                final String prefix;
                String editablePart = fullText;

                if (colonIndex >= 0) {
                    prefix = fullText.substring(0, colonIndex + 1).trim(); // "Force:"
                    editablePart = fullText.substring(colonIndex + 1).trim(); // "-1.00kN/m"
                } else {
                    prefix = "";
                }


                if (textField == null) {
                    textField = new TextField();
                    textField.setStyle("-fx-text-fill: black;");


                    textField.setMaxWidth(calculateTextWidth(editablePart));

                    textField.setOnAction(e -> {
                        commitEdit(prefix + " " + textField.getText());
                    });
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            commitEdit(prefix + " " + textField.getText());
                        }
                    });
                }

                textField.setText(editablePart);

                Label prefixLabel = new Label(prefix);
                HBox hbox = new HBox(5);
                hbox.setAlignment(Pos.BASELINE_LEFT);
                hbox.getChildren().addAll(prefixLabel, textField);

                setText(null);
                setGraphic(hbox);

                textField.requestFocus();
                textField.selectAll();
            }

            /**
             * Calculate the width of the TextField based on the length of the editable part.
             */
            private double calculateTextWidth(String text) {
                Text tempText = new Text(text);
                return tempText.getLayoutBounds().getWidth() + 20;
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(textField);
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                getTreeItem().setValue(newValue);
                setText(newValue);
                setGraphic(null);

            }
        });

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
                return Math.abs(load.getForce()) != 0.0;
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
     * @return true if there are no tree items in the tree of there's only the empty tree filler (doesn't count)
     */
    public static boolean isTreeViewEmpty() {
        if (root.getChildren().isEmpty())
            return true;
        return root.getChildren().size() == 1 && root.getChildren().getFirst() instanceof LoadTreeItem loadTreeItem && loadTreeItem.getLoadId() == -1;
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

    public static int getNumberOfItemsInTreeView() {
        return root.getChildrenLoadItems().size();
    }

    private static LoadTreeItem findChildByLoadAndField(LoadTreeItem parent, int loadId, int fieldId) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child instanceof LoadTreeItem lti) {
                if (lti.getLoadId() == loadId && lti.getFieldId() == fieldId) {
                    return lti;
                }
                LoadTreeItem found = findChildByLoadAndField(lti, loadId, fieldId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static void editNestedItem(int loadId, int fieldId) {
        LoadTreeItem targetItem = findChildByLoadAndField(root, loadId, fieldId);
        if (targetItem == null) {
            System.out.println("No matching nested item found for loadId=" + loadId
                    + " fieldId=" + fieldId);
            return;
        }

        loadsTreeView.getSelectionModel().select(targetItem);
        loadsTreeView.layout();
        loadsTreeView.edit(targetItem);
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
        default -> throw new RuntimeException("There is no createLoadTreeItem strategy for load type " + load.getClass().getName());
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

    /**
     * @return the loadId of the hull weight, or -1 if no hull weight is present in the tree
     */
    public static int getHullLoadTreeItemLoadId() {
        for (TreeItem<String> child : root.getChildren()) {
            if (child instanceof LoadTreeItem loadTreeItem && loadTreeItem.getLoad() != null) {
                if (loadTreeItem.getLoad().getType() == LoadType.HULL) {
                    return loadTreeItem.getLoadId();
                }
            }
        }
        return -1;
    }
}
