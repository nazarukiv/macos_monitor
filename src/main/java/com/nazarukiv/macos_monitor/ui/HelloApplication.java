package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.scheduler.MetricsScheduler;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private MetricsScheduler scheduler;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/hello-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1100, 760);
        scene.getStylesheets().add(
                HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/styles.css").toExternalForm()
        );

        DashboardController controller = fxmlLoader.getController();
        SystemMetricsService service = new SystemMetricsService();
        controller.setSystemMetricsService(service);

        scheduler = new MetricsScheduler(service, controller);

        stage.setTitle("Dashboard");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        scheduler.start();
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}
