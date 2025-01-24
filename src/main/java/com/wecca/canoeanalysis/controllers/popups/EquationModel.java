package com.wecca.canoeanalysis.controllers.popups;

public class EquationModel {
    private String parameter;
    private String description;
    private String value;

    public EquationModel(String parameter, String description, String value) {
        this.parameter = parameter;
        this.description = description;
        this.value = value;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

