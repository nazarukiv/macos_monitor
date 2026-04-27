package com.nazarukiv.macos_monitor.model;

public class SystemDetails {
    private final String cpuName;
    private final String osNameAndVersion;
    private final String totalRam;
    private final String availableRam;
    private final String uptime;
    private final String diskTotal;
    private final String diskFree;

    public SystemDetails(
            String cpuName,
            String osNameAndVersion,
            String totalRam,
            String availableRam,
            String uptime,
            String diskTotal,
            String diskFree
    ) {
        this.cpuName = cpuName;
        this.osNameAndVersion = osNameAndVersion;
        this.totalRam = totalRam;
        this.availableRam = availableRam;
        this.uptime = uptime;
        this.diskTotal = diskTotal;
        this.diskFree = diskFree;
    }

    public String getCpuName() {
        return cpuName;
    }

    public String getOsNameAndVersion() {
        return osNameAndVersion;
    }

    public String getTotalRam() {
        return totalRam;
    }

    public String getAvailableRam() {
        return availableRam;
    }

    public String getUptime() {
        return uptime;
    }

    public String getDiskTotal() {
        return diskTotal;
    }

    public String getDiskFree() {
        return diskFree;
    }
}
