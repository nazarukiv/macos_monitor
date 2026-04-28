package com.nazarukiv.macos_monitor.model;

import java.time.LocalDateTime;
import java.util.List;

public class SystemSnapshot {
    private final String concern;
    private final LocalDateTime capturedAt;
    private final CpuInfo cpuInfo;
    private final MemoryInfo memoryInfo;
    private final BatteryInfo batteryInfo;
    private final NetworkInfo networkInfo;
    private final SystemDetails systemDetails;
    private final List<ProcessInfo> topProcesses;

    public SystemSnapshot(
            String concern,
            LocalDateTime capturedAt,
            CpuInfo cpuInfo,
            MemoryInfo memoryInfo,
            BatteryInfo batteryInfo,
            NetworkInfo networkInfo,
            SystemDetails systemDetails,
            List<ProcessInfo> topProcesses
    ) {
        this.concern = concern;
        this.capturedAt = capturedAt;
        this.cpuInfo = cpuInfo;
        this.memoryInfo = memoryInfo;
        this.batteryInfo = batteryInfo;
        this.networkInfo = networkInfo;
        this.systemDetails = systemDetails;
        this.topProcesses = List.copyOf(topProcesses);
    }

    public String getConcern() {
        return concern;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public CpuInfo getCpuInfo() {
        return cpuInfo;
    }

    public MemoryInfo getMemoryInfo() {
        return memoryInfo;
    }

    public BatteryInfo getBatteryInfo() {
        return batteryInfo;
    }

    public NetworkInfo getNetworkInfo() {
        return networkInfo;
    }

    public SystemDetails getSystemDetails() {
        return systemDetails;
    }

    public List<ProcessInfo> getTopProcesses() {
        return topProcesses;
    }
}
