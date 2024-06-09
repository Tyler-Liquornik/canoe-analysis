package com.wecca.canoeanalysis.components.controls;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import com.wecca.canoeanalysis.controllers.CanoeAnalysisController;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import lombok.Getter;

@Getter
public class EditableLoadListCell extends ListCell<String> {

    private JFXTextField jfxTextField;
    private LoadStringPattern loadPattern;

    public EditableLoadListCell() {
        createJFXTextField();
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (jfxTextField == null) {
            createJFXTextField();
        }
        setText(null);
        setGraphic(jfxTextField);
        jfxTextField.setText(getItem());
        jfxTextField.selectAll();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                jfxTextField.setText(item);
                setText(null);
                setGraphic(jfxTextField);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    private void createJFXTextField() {
        this.jfxTextField = new JFXTextField();
        jfxTextField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                attemptSaveCellContent();
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        jfxTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                attemptSaveCellContent();
            }
        });
    }

    //// Chat Template ^
    //// My work v

    private void attemptSaveCellContent() {
        CanoeAnalysisController canoeAnalysisController = new CanoeAnalysisController();

        // Do not call with support (cannot edit support after solve)
        if (isValidCellInput(jfxTextField.getText())) {
            commitEdit(jfxTextField.getText());
            canoeAnalysisController.showSnackbar("Successful edit");
        } else {
            cancelEdit();
            String template = loadPattern.getTemplate();
            canoeAnalysisController.showSnackbar("Load must be formatted as: '" + template + "'");
        }
    }

    // Should be set only once when the cell is first created and not changed upon edit
    // Trying to prevent the use from changing the template
    public void setLoadPatternTypeFromText() {
        String pattern = this.getText();

        if (pattern.matches(LoadStringPattern.POINT_LOAD.getPattern()))
            this.loadPattern = LoadStringPattern.POINT_LOAD;
        else if (pattern.matches(LoadStringPattern.SUPPORT.getPattern()))
            this.loadPattern = LoadStringPattern.SUPPORT;
        else if (pattern.matches(LoadStringPattern.DISTRIBUTED_LOAD.getPattern()))
            this.loadPattern = LoadStringPattern.DISTRIBUTED_LOAD;
        else
            throw new RuntimeException("Attempting to set invalid pattern");
    }

    public boolean isValidCellInput(String input) {
        LoadStringPattern pattern = this.getLoadPattern();
        return input.matches(pattern.getPattern());
    }

}