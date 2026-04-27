package com.nazarukiv.macos_monitor.model;

public class ProcessInfo {
    private int pid;
    private String name;
    private double cpuUsage;
    private long memoryUsage;

    public ProcessInfo(int pid, String name, double cpuUsage, long memoryUsage) {
        this.pid = pid;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    @Override
    public String toString() {
        return "ProcessInfo{" +
                "pid=" + pid +
                ", name='" + name + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", memoryUsage=" + memoryUsage +
                '}';
    }
}
