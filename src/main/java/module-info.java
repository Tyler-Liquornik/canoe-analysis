module com.wecca.canoeanalysis {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.wecca.canoeanalysis to javafx.fxml;
    exports com.wecca.canoeanalysis;
}