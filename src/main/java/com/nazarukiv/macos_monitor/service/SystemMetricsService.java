package com.nazarukiv.macos_monitor.service;

import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.Comparator;
import java.util.List;

public class SystemMetricsService {
    private static final long BYTES_PER_MEGABYTE = 1024 * 1024;
    private static final int MAX_PROCESSES = 10;

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final OperatingSystem operatingSystem;
    private long[] previousCpuTicks;

    public SystemMetricsService() {
        SystemInfo systemInfo = new SystemInfo();
        this.processor = systemInfo.getHardware().getProcessor();
        this.memory = systemInfo.getHardware().getMemory();
        this.operatingSystem = systemInfo.getOperatingSystem();
        this.previousCpuTicks = processor.getSystemCpuLoadTicks();
    }

    public CpuInfo getCpuInfo() {
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100;
        cpuUsage = Math.max(0, Math.min(cpuUsage, 100));

        previousCpuTicks = processor.getSystemCpuLoadTicks();
        return new CpuInfo(cpuUsage);
    }

    public MemoryInfo getMemoryInfo() {
        long totalMemory = bytesToMegabytes(memory.getTotal());
        long availableMemory = bytesToMegabytes(memory.getAvailable());
        long usedMemory = totalMemory - availableMemory;

        return new MemoryInfo(totalMemory, usedMemory);
    }

    public List<ProcessInfo> getProcesses() {
        return operatingSystem.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, MAX_PROCESSES).stream()
                .map(this::toProcessInfo)
                .sorted(Comparator.comparingDouble(ProcessInfo::getCpuUsage).reversed())
                .toList();
    }

    private long bytesToMegabytes(long bytes) {
        return bytes / BYTES_PER_MEGABYTE;
    }

    private ProcessInfo toProcessInfo(OSProcess process) {
        double cpuUsage = process.getProcessCpuLoadCumulative() * 100;
        cpuUsage = Math.max(cpuUsage, 0.1);

        long memoryUsage = process.getResidentSetSize() / (1024 * 1024);

        return new ProcessInfo(process.getName(), cpuUsage, memoryUsage);
    }
}
