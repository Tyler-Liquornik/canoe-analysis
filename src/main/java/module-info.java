module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires org.burningwave.core;
    requires fontawesomefx;
    requires java.desktop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.google.common;
    requires commons.math3;

    exports com.wecca.canoeanalysis;
    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis.components.diagrams;
    opens com.wecca.canoeanalysis.components.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.models;
    opens com.wecca.canoeanalysis.models to javafx.fxml;
    exports com.wecca.canoeanalysis.services;
    opens com.wecca.canoeanalysis.services to javafx.fxml;
    exports com.wecca.canoeanalysis.controllers;
    opens com.wecca.canoeanalysis.controllers to javafx.fxml;
    exports com.wecca.canoeanalysis.components.graphics;
    opens com.wecca.canoeanalysis.components.graphics to javafx.fxml;
    exports com.wecca.canoeanalysis.services.color;
    opens com.wecca.canoeanalysis.services.color to javafx.fxml;
    exports com.wecca.canoeanalysis.models.functions;
    opens com.wecca.canoeanalysis.models.functions to javafx.fxml;
}