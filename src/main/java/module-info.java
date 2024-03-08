module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis;
    exports com.wecca.canoeanalysis.diagrams;
    opens com.wecca.canoeanalysis.diagrams to javafx.fxml;
}