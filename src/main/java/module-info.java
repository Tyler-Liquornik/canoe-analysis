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
    requires org.checkerframework.checker.qual;

    exports com.wecca.canoeanalysis;
    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis.components.diagrams;
    opens com.wecca.canoeanalysis.components.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.components.controls;
    opens com.wecca.canoeanalysis.components.controls to javafx.fxml;
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
    exports com.wecca.canoeanalysis.models.function;
    opens com.wecca.canoeanalysis.models.function to javafx.fxml;
    exports com.wecca.canoeanalysis.models.load;
    opens com.wecca.canoeanalysis.models.load to javafx.fxml;
    exports com.wecca.canoeanalysis.models.canoe;
    opens com.wecca.canoeanalysis.models.canoe to javafx.fxml;
    exports com.wecca.canoeanalysis.models.data;
    opens com.wecca.canoeanalysis.models.data to javafx.fxml;
}