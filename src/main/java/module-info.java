module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires static lombok;
    requires com.fasterxml.jackson.databind;

    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis;
    exports com.wecca.canoeanalysis.graphics.diagrams;
    opens com.wecca.canoeanalysis.graphics.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.graphics;
    opens com.wecca.canoeanalysis.graphics to javafx.fxml;
    exports com.wecca.canoeanalysis.models;
    opens com.wecca.canoeanalysis.models to javafx.fxml;
    exports com.wecca.canoeanalysis.services;
    opens com.wecca.canoeanalysis.services to javafx.fxml;
    exports com.wecca.canoeanalysis.controllers;
    opens com.wecca.canoeanalysis.controllers to javafx.fxml;
    exports com.wecca.canoeanalysis.graphics.ui;
    opens com.wecca.canoeanalysis.graphics.ui to javafx.fxml;
}