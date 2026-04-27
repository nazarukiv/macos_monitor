package com.nazarukiv.macos_monitor.scheduler;

import com.nazarukiv.macos_monitor.model.BatteryInfo;
import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import com.nazarukiv.macos_monitor.ui.DashboardController;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsScheduler {
    private static final double CPU_ALERT_THRESHOLD = 80.0;
    private static final double CPU_ALERT_RESET_THRESHOLD = 70.0;
    private static final double RAM_ALERT_THRESHOLD = 0.90;
    private static final double RAM_ALERT_RESET_THRESHOLD = 0.80;

    private final SystemMetricsService service;
    private final DashboardController controller;
    private final ScheduledExecutorService executorService;
    private boolean cpuAlertShown;
    private boolean ramAlertShown;

    public MetricsScheduler(SystemMetricsService service, DashboardController controller) {
        this.service = service;
        this.controller = controller;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        executorService.scheduleAtFixedRate(() -> {
            if (!controller.isRunning()) {
                return;
            }

            CpuInfo cpuInfo = service.getCpuInfo();
            MemoryInfo memoryInfo = service.getMemoryInfo();
            BatteryInfo batteryInfo = service.getBatteryInfo();
            var processes = service.getProcesses();

            Platform.runLater(() -> {
                controller.updateMetrics(cpuInfo, memoryInfo);
                controller.updateBattery(batteryInfo);
                controller.updateProcesses(processes);
                handleAlerts(cpuInfo, memoryInfo);
            });

        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executorService.shutdown();
    }

    private void handleAlerts(CpuInfo cpuInfo, MemoryInfo memoryInfo) {
        double cpuUsage = cpuInfo.getCpuUsage();
        double memoryUsage = memoryInfo.getTotalMemory() == 0
                ? 0
                : (double) memoryInfo.getUsedMemory() / memoryInfo.getTotalMemory();

        if (cpuUsage > CPU_ALERT_THRESHOLD && !cpuAlertShown) {
            controller.showAlert("\u26A0 High CPU usage detected");
            cpuAlertShown = true;
        } else if (cpuUsage < CPU_ALERT_RESET_THRESHOLD) {
            cpuAlertShown = false;
        }

        if (memoryUsage > RAM_ALERT_THRESHOLD && !ramAlertShown) {
            controller.showAlert("\u26A0 High memory usage detected");
            ramAlertShown = true;
        } else if (memoryUsage < RAM_ALERT_RESET_THRESHOLD) {
            ramAlertShown = false;
        }
    }
}
