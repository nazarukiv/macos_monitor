package com.nazarukiv.macos_monitor.model;

public class NetworkInfo {
    private final double downloadSpeed;
    private final double uploadSpeed;
    private final String ssid;
    private final String localIp;
    private final double pingMs;
    private final String status;
    private final String interfaceName;
    private final int activeConnections;

    public NetworkInfo(double downloadSpeed, double uploadSpeed, String ssid, String localIp, double pingMs,
                       String status, String interfaceName, int activeConnections) {
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.ssid = ssid;
        this.localIp = localIp;
        this.pingMs = pingMs;
        this.status = status;
        this.interfaceName = interfaceName;
        this.activeConnections = activeConnections;
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public String getSsid() {
        return ssid;
    }

    public String getLocalIp() {
        return localIp;
    }

    public double getPingMs() {
        return pingMs;
    }

    public String getStatus() {
        return status;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public int getActiveConnections() {
        return activeConnections;
    }
}
