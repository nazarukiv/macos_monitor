package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.NetworkInfo;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class NetworkController {
    @FXML
    private Label downloadSpeedLabel;

    @FXML
    private Label uploadSpeedLabel;

    @FXML
    private Label testingLabel;

    @FXML
    private Label ssidLabel;

    @FXML
    private Label ipLabel;

    @FXML
    private Label connectionTypeLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pingLabel;

    @FXML
    private Label interfaceLabel;

    @FXML
    private Label connectionsLabel;

    @FXML
    private Button refreshButton;

    private SystemMetricsService systemMetricsService;
    private volatile boolean testRunning;

    public void setSystemMetricsService(SystemMetricsService systemMetricsService) {
        this.systemMetricsService = systemMetricsService;
        refreshConnectionTest();
    }

    @FXML
    public void refreshConnectionTest() {
        if (systemMetricsService == null || testRunning) {
            return;
        }

        testRunning = true;
        testingLabel.setText("Measuring network speed...");
        refreshButton.setDisable(true);
        downloadSpeedLabel.setText("Download: --");
        uploadSpeedLabel.setText("Upload: --");
        resetExtendedInfo();

        Thread worker = new Thread(() -> {
            NetworkInfo networkInfo = systemMetricsService.getNetworkSpeed();
            Platform.runLater(() -> {
                updateNetworkInfo(networkInfo);
                refreshButton.setDisable(false);
                testRunning = false;
            });
        }, "network-speed-check");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
        if (networkInfo.getDownloadSpeed() < 0 || networkInfo.getUploadSpeed() < 0) {
            testingLabel.setText("No active connection");
            downloadSpeedLabel.setText("Download: N/A");
            uploadSpeedLabel.setText("Upload: N/A");
        } else if (networkInfo.getDownloadSpeed() < 0.01 && networkInfo.getUploadSpeed() < 0.01) {
            testingLabel.setText("Very low activity");
            downloadSpeedLabel.setText("Download: Idle");
            uploadSpeedLabel.setText("Upload: Idle");
        } else if (networkInfo.getDownloadSpeed() < 0.05 && networkInfo.getUploadSpeed() < 0.05) {
            testingLabel.setText("No network activity");
            downloadSpeedLabel.setText("Download: Idle");
            uploadSpeedLabel.setText("Upload: Idle");
        } else {
            testingLabel.setText("Latest result");
            downloadSpeedLabel.setText("Download: " + String.format("%.1f MB/s", networkInfo.getDownloadSpeed()));
            uploadSpeedLabel.setText("Upload: " + String.format("%.1f MB/s", networkInfo.getUploadSpeed()));
        }

        ssidLabel.setText("WiFi (SSID): " + fallback(networkInfo.getSsid(), "Unknown"));
        ipLabel.setText("IP address: " + fallback(networkInfo.getLocalIp(), "N/A"));
        connectionTypeLabel.setText("Connection type: " + resolveConnectionType(networkInfo));
        statusLabel.setText("Status: " + fallback(networkInfo.getStatus(), "Disconnected"));
        pingLabel.setText("Ping: " + formatPing(networkInfo.getPingMs()));
        interfaceLabel.setText("Network interface: " + fallback(networkInfo.getInterfaceName(), "Idle"));
        connectionsLabel.setText("Active connections: " + Math.max(networkInfo.getActiveConnections(), 0));
    }

    private void resetExtendedInfo() {
        ssidLabel.setText("WiFi (SSID): --");
        ipLabel.setText("IP address: --");
        connectionTypeLabel.setText("Connection type: --");
        statusLabel.setText("Status: --");
        pingLabel.setText("Ping: --");
        interfaceLabel.setText("Network interface: --");
        connectionsLabel.setText("Active connections: --");
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String formatPing(double pingMs) {
        return pingMs < 0 ? "N/A" : String.format("%.1f ms", pingMs);
    }

    private String resolveConnectionType(NetworkInfo networkInfo) {
        String interfaceName = networkInfo.getInterfaceName() == null
                ? ""
                : networkInfo.getInterfaceName().toLowerCase();
        String ssid = networkInfo.getSsid();

        if (ssid != null && !"Unknown".equalsIgnoreCase(ssid)) {
            return "WiFi";
        }

        if (interfaceName.contains("wi-fi") || interfaceName.contains("wifi") || interfaceName.contains("airport")) {
            return "WiFi";
        }

        if (interfaceName.contains("ethernet") || interfaceName.startsWith("en")) {
            return "Ethernet";
        }

        return "Connected".equalsIgnoreCase(networkInfo.getStatus()) ? "Ethernet" : "Idle";
    }
}
