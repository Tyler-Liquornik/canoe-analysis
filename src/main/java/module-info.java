module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis;
    exports com.wecca.canoeanalysis.diagrams;
    opens com.wecca.canoeanalysis.diagrams to javafx.fxml;
    exports com.wecca.canoeanalysis.customUI;
    opens com.wecca.canoeanalysis.customUI to javafx.fxml;
    exports com.wecca.canoeanalysis.models;
    opens com.wecca.canoeanalysis.models to javafx.fxml;
    exports com.wecca.canoeanalysis.utility;
    opens com.wecca.canoeanalysis.utility to javafx.fxml;
}