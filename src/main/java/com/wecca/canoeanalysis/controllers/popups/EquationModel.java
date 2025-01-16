package com.wecca.canoeanalysis.controllers.popups;

public class EquationModel {
    private String parameter;
    private String description;

    public EquationModel(String parameter, String description) {
        this.parameter = parameter;
        this.description = description;
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
}

