module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires static lombok;


    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis;
    exports com.wecca.canoeanalysis.diagrams;
    opens com.wecca.canoeanalysis.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.graphics;
    opens com.wecca.canoeanalysis.graphics to javafx.fxml;
    exports com.wecca.canoeanalysis.models;
    opens com.wecca.canoeanalysis.models to javafx.fxml;
    exports com.wecca.canoeanalysis.util;
    opens com.wecca.canoeanalysis.util to javafx.fxml;
    exports com.wecca.canoeanalysis.controllers;
    opens com.wecca.canoeanalysis.controllers to javafx.fxml;
}