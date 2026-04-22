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

        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        DashboardController controller = fxmlLoader.getController();
        SystemMetricsService service = new SystemMetricsService();

        scheduler = new MetricsScheduler(service, controller);

        stage.setTitle("Dashboard");
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
