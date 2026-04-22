package com.nazarukiv.macos_monitor.scheduler;

import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import com.nazarukiv.macos_monitor.ui.DashboardController;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsScheduler {
    private final SystemMetricsService service;
    private final DashboardController controller;
    private final ScheduledExecutorService executorService;

    public MetricsScheduler(SystemMetricsService service, DashboardController controller) {
        this.service = service;
        this.controller = controller;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        executorService.scheduleAtFixedRate(() -> {
            CpuInfo cpuInfo = service.getCpuInfo();
            MemoryInfo memoryInfo = service.getMemoryInfo();
            var processes = service.getProcesses();

            Platform.runLater(() -> {
                controller.updateMetrics(cpuInfo, memoryInfo);
                controller.updateProcesses(processes);
            });

        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executorService.shutdown();
    }
}
