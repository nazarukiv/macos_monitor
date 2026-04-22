package com.nazarukiv.macos_monitor.model;

public class CpuInfo {
    private double cpuUsage;

    public CpuInfo(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }



    @Override
    public String toString() {
        return "CpuInfo{" +
                "cpuUsage=" + cpuUsage +
                '}';
    }
}
