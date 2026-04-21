module com.nazarukiv.macos_monitor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.nazarukiv.macos_monitor to javafx.fxml;
    exports com.nazarukiv.macos_monitor;
}