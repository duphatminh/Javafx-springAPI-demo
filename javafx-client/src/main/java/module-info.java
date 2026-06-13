module com.example.javafxclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http; // API
    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    opens com.example.javafxclient to javafx.fxml;
    exports com.example.javafxclient;
    exports com.example.javafxclient.model to com.fasterxml.jackson.databind;
}