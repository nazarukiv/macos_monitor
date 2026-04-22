module com.nazarukiv.macos_monitor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.github.oshi;

    opens com.nazarukiv.macos_monitor.ui to javafx.fxml;
    opens com.nazarukiv.macos_monitor.model to javafx.base;

    exports com.nazarukiv.macos_monitor;
    exports com.nazarukiv.macos_monitor.ui;
}