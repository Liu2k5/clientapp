module com.hsf {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.net.http;

    opens com.hsf to javafx.fxml;
    exports com.hsf;
}
