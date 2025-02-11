module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires fontawesomefx;
    requires java.desktop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.google.common;
    requires commons.math3;
    requires org.checkerframework.checker.qual;
    requires java.rmi;
    requires ij;
    requires org.slf4j;
    requires org.aspectj.weaver;
    requires ch.qos.logback.core;
    requires javaGeom;
    requires jama;

    exports com.wecca.canoeanalysis;
    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis.components.diagrams;
    opens com.wecca.canoeanalysis.components.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.components.controls;
    opens com.wecca.canoeanalysis.components.controls to javafx.fxml;
    exports com.wecca.canoeanalysis.services;
    opens com.wecca.canoeanalysis.services to javafx.fxml;
    exports com.wecca.canoeanalysis.controllers;
    opens com.wecca.canoeanalysis.controllers to javafx.fxml;
    exports com.wecca.canoeanalysis.components.graphics;
    opens com.wecca.canoeanalysis.components.graphics to javafx.fxml;
    exports com.wecca.canoeanalysis.services.color;
    opens com.wecca.canoeanalysis.services.color to javafx.fxml;
    exports com.wecca.canoeanalysis.models.function;
    opens com.wecca.canoeanalysis.models.function to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.wecca.canoeanalysis.models.load;
    opens com.wecca.canoeanalysis.models.load to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.wecca.canoeanalysis.models.canoe;
    opens com.wecca.canoeanalysis.models.canoe to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.wecca.canoeanalysis.models.data;
    opens com.wecca.canoeanalysis.models.data to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.wecca.canoeanalysis.controllers.modules;
    opens com.wecca.canoeanalysis.controllers.modules to javafx.fxml;
    exports com.wecca.canoeanalysis.controllers.popups;
    exports com.wecca.canoeanalysis.controllers.util;
    opens com.wecca.canoeanalysis.controllers.util to javafx.fxml;
    exports com.wecca.canoeanalysis.utils;
    opens com.wecca.canoeanalysis.utils to javafx.fxml;
    exports com.wecca.canoeanalysis.components.graphics.hull;
    opens com.wecca.canoeanalysis.components.graphics.hull to javafx.fxml;
    exports com.wecca.canoeanalysis.components.graphics.load;
    opens com.wecca.canoeanalysis.components.graphics.load to javafx.fxml;
    opens com.wecca.canoeanalysis.controllers.popups to com.fasterxml.jackson.databind, javafx.fxml;
}