package com.nazarukiv.macos_monitor.model;

public class BatteryInfo {
    private final double percentage;
    private final String status;
    private final String timeRemaining;

    public BatteryInfo(double percentage, String status, String timeRemaining) {
        this.percentage = percentage;
        this.status = status;
        this.timeRemaining = timeRemaining;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getStatus() {
        return status;
    }

    public String getTimeRemaining() {
        return timeRemaining;
    }
}
