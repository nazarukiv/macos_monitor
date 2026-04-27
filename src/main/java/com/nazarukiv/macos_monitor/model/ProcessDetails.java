package com.nazarukiv.macos_monitor.model;

public class ProcessDetails {
    private final int pid;
    private final String name;
    private final double cpuUsage;
    private final long memoryUsage;
    private final int threadCount;
    private final String uptime;
    private final String path;

    public ProcessDetails(
            int pid,
            String name,
            double cpuUsage,
            long memoryUsage,
            int threadCount,
            String uptime,
            String path
    ) {
        this.pid = pid;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.threadCount = threadCount;
        this.uptime = uptime;
        this.path = path;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getUptime() {
        return uptime;
    }

    public String getPath() {
        return path;
    }
}
