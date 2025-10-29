module com.example.peakplatesapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.peakplatesapp to javafx.fxml;
    exports com.example.peakplatesapp;
}