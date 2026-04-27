package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.ProcessDetails;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProcessDetailsController {
    @FXML
    private Label nameLabel;

    @FXML
    private Label pidLabel;

    @FXML
    private Label cpuLabel;

    @FXML
    private Label memoryLabel;

    @FXML
    private Label threadsLabel;

    @FXML
    private Label uptimeLabel;

    @FXML
    private Label pathLabel;

    public void setProcessDetails(ProcessDetails details) {
        nameLabel.setText(details.getName());
        pidLabel.setText(String.valueOf(details.getPid()));
        cpuLabel.setText(String.format("%.1f%%", details.getCpuUsage()));
        memoryLabel.setText(details.getMemoryUsage() + " MB");
        threadsLabel.setText(String.valueOf(details.getThreadCount()));
        uptimeLabel.setText(details.getUptime());
        pathLabel.setText(details.getPath());
    }
}
