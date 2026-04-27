package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.SystemDetails;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SystemInfoController {
    @FXML
    private Label cpuNameLabel;

    @FXML
    private Label osLabel;

    @FXML
    private Label totalRamLabel;

    @FXML
    private Label availableRamLabel;

    @FXML
    private Label uptimeLabel;

    @FXML
    private Label diskTotalLabel;

    @FXML
    private Label diskFreeLabel;

    public void setSystemInfo(SystemDetails systemDetails) {
        cpuNameLabel.setText(systemDetails.getCpuName());
        osLabel.setText(systemDetails.getOsNameAndVersion());
        totalRamLabel.setText(systemDetails.getTotalRam());
        availableRamLabel.setText(systemDetails.getAvailableRam());
        uptimeLabel.setText(systemDetails.getUptime());
        diskTotalLabel.setText(systemDetails.getDiskTotal());
        diskFreeLabel.setText(systemDetails.getDiskFree());
    }
}
