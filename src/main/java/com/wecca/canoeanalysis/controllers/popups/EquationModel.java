package com.wecca.canoeanalysis.controllers.popups;

import javafx.scene.text.TextFlow;

public class EquationModel {
    private TextFlow parameter;
    private TextFlow description;
    private TextFlow value;

    public EquationModel(TextFlow parameter, TextFlow description, TextFlow value) {
        this.parameter = parameter;
        this.description = description;
        this.value = value;
    }

    public TextFlow getParameter() {
        return parameter;
    }

    public void setParameter(TextFlow parameter) {
        this.parameter = parameter;
    }

    public TextFlow getDescription() {
        return description;
    }

    public void setDescription(TextFlow description) {
        this.description = description;
    }

    public TextFlow getValue() {
        return value;
    }

    public void setValue(TextFlow value) {
        this.value = value;
    }
}

