package com.nazarukiv.macos_monitor.service;

import com.nazarukiv.macos_monitor.model.BatteryInfo;
import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.NetworkInfo;
import com.nazarukiv.macos_monitor.model.ProcessDetails;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import com.nazarukiv.macos_monitor.model.SystemDetails;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemMetricsService {
    private static final long BYTES_PER_MEGABYTE = 1024 * 1024;
    private static final int MAX_PROCESSES = 10;
    private static final String UNKNOWN = "Unknown";
    private static final String NOT_AVAILABLE = "N/A";
    private static final String IDLE = "Idle";
    private static final String CONNECTED = "Connected";
    private static final String DISCONNECTED = "Disconnected";
    private static final String WIFI = "WiFi";
    private static final String ETHERNET = "Ethernet";
    private static final String AIRPORT_PATH =
            "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport";
    private static final Pattern PING_TIME_PATTERN = Pattern.compile("time=([0-9.]+)\\s*ms");

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final OperatingSystem operatingSystem;
    private final HardwareAbstractionLayer hardware;
    private long[] previousCpuTicks;

    public SystemMetricsService() {
        SystemInfo systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
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

    public BatteryInfo getBatteryInfo() {
        List<PowerSource> powerSources = hardware.getPowerSources();
        if (powerSources.isEmpty()) {
            return new BatteryInfo(-1, "No battery", "N/A");
        }

        PowerSource source = powerSources.getFirst();
        source.updateAttributes();

        double percentage = source.getRemainingCapacityPercent() * 100;
        String status = source.isCharging() ? "Charging \u26a1" : "Discharging";
        String timeRemaining = formatBatteryTimeRemaining(source.getTimeRemainingEstimated());

        return new BatteryInfo(percentage, status, timeRemaining);
    }

    public List<ProcessInfo> getProcesses() {
        return operatingSystem.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, MAX_PROCESSES).stream()
                .map(this::toProcessInfo)
                .sorted(Comparator.comparingDouble(ProcessInfo::getCpuUsage).reversed())
                .toList();
    }

    public ProcessDetails getProcessDetails(int pid) {
        OSProcess process = operatingSystem.getProcess(pid);
        if (process == null) {
            return null;
        }

        return new ProcessDetails(
                process.getProcessID(),
                process.getName(),
                process.getProcessCpuLoadCumulative() * 100,
                bytesToMegabytes(process.getResidentSetSize()),
                process.getThreadCount(),
                formatProcessUptime(process.getUpTime()),
                process.getPath() == null || process.getPath().isBlank() ? "N/A" : process.getPath()
        );
    }

    public SystemDetails getSystemInfo() {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long uptimeSeconds = operatingSystem.getSystemUptime();
        long totalDiskSpace = 0;
        long freeDiskSpace = 0;

        for (OSFileStore store : operatingSystem.getFileSystem().getFileStores()) {
            if ("/".equals(store.getMount())) {
                totalDiskSpace = store.getTotalSpace();
                freeDiskSpace = store.getUsableSpace();
                break;
            }
        }

        return new SystemDetails(
                processor.getProcessorIdentifier().getName(),
                operatingSystem.getFamily() + " " + operatingSystem.getVersionInfo(),
                formatBytes(totalMemory),
                formatBytes(availableMemory),
                formatUptime(uptimeSeconds),
                formatBytes(totalDiskSpace),
                formatBytes(freeDiskSpace)
        );
    }

    public NetworkInfo getNetworkSpeed() {
        List<NetworkIF> networkInterfaces = hardware.getNetworkIFs(true);
        if (networkInterfaces.isEmpty()) {
            return buildNetworkInfo(-1, -1, null);
        }

        long initialReceived = 0;
        long initialSent = 0;
        for (NetworkIF networkIF : networkInterfaces) {
            networkIF.updateAttributes();
            initialReceived += networkIF.getBytesRecv();
            initialSent += networkIF.getBytesSent();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return buildNetworkInfo(-1, -1, null);
        }

        long finalReceived = 0;
        long finalSent = 0;
        for (NetworkIF networkIF : networkInterfaces) {
            networkIF.updateAttributes();
            finalReceived += networkIF.getBytesRecv();
            finalSent += networkIF.getBytesSent();
        }

        double downloadSpeed = Math.max(finalReceived - initialReceived, 0) / (1024.0 * 1024.0);
        double uploadSpeed = Math.max(finalSent - initialSent, 0) / (1024.0 * 1024.0);
        NetworkIF activeInterface = findActiveInterface(networkInterfaces);

        return buildNetworkInfo(downloadSpeed, uploadSpeed, activeInterface);
    }

    private NetworkInfo buildNetworkInfo(double downloadSpeed, double uploadSpeed, NetworkIF activeInterface) {
        String interfaceName = getInterfaceDisplayName(activeInterface, downloadSpeed, uploadSpeed);
        String connectionType = resolveConnectionType(activeInterface);
        String ssid = WIFI.equals(connectionType) ? getWifiSsid() : UNKNOWN;
        String localIp = getLocalIpAddress();
        double pingMs = getPingMs();
        String status = pingMs >= 0 ? CONNECTED : DISCONNECTED;
        int activeConnections = getActiveConnectionsCount();

        return new NetworkInfo(
                downloadSpeed,
                uploadSpeed,
                ssid,
                localIp,
                pingMs,
                status,
                interfaceName,
                activeConnections
        );
    }

    private long bytesToMegabytes(long bytes) {
        return bytes / BYTES_PER_MEGABYTE;
    }

    private String formatBytes(long bytes) {
        double gigabytes = bytes / (1024.0 * 1024 * 1024);
        return String.format("%.1f GB", gigabytes);
    }

    private String formatUptime(long totalSeconds) {
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;

        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, minutes);
        }
        return String.format("%02dh %02dm", hours, minutes);
    }

    private String formatBatteryTimeRemaining(double totalSeconds) {
        if (totalSeconds < 0) {
            return "Calculating...";
        }

        long hours = (long) totalSeconds / 3_600;
        long minutes = ((long) totalSeconds % 3_600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }

    private ProcessInfo toProcessInfo(OSProcess process) {
        double cpuUsage = process.getProcessCpuLoadCumulative() * 100;
        cpuUsage = Math.max(cpuUsage, 0.1);

        long memoryUsage = bytesToMegabytes(process.getResidentSetSize());

        return new ProcessInfo(process.getProcessID(), process.getName(), cpuUsage, memoryUsage);
    }

    private String formatProcessUptime(long uptimeMillis) {
        long totalSeconds = uptimeMillis / 1000;
        long hours = totalSeconds / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private NetworkIF findActiveInterface(List<NetworkIF> networkInterfaces) {
        List<NetworkIF> connectedInterfaces = new ArrayList<>();

        for (NetworkIF networkIF : networkInterfaces) {
            networkIF.updateAttributes();
            if (networkIF.getIfOperStatus() == NetworkIF.IfOperStatus.UP) {
                connectedInterfaces.add(networkIF);
            }
        }

        for (NetworkIF networkIF : connectedInterfaces) {
            if (networkIF.getBytesRecv() > 0 || networkIF.getBytesSent() > 0) {
                return networkIF;
            }
        }

        return connectedInterfaces.isEmpty() ? null : connectedInterfaces.getFirst();
    }

    private String getInterfaceDisplayName(NetworkIF networkIF, double downloadSpeed, double uploadSpeed) {
        if (networkIF == null || (downloadSpeed <= 0 && uploadSpeed <= 0)) {
            return IDLE;
        }

        String displayName = networkIF.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }

        String name = networkIF.getName();
        return name == null || name.isBlank() ? IDLE : name;
    }

    private String resolveConnectionType(NetworkIF networkIF) {
        if (networkIF == null) {
            return IDLE;
        }

        String name = networkIF.getName() == null ? "" : networkIF.getName().toLowerCase();
        String displayName = networkIF.getDisplayName() == null ? "" : networkIF.getDisplayName().toLowerCase();
        String combined = name + " " + displayName;

        if (combined.contains("wi-fi") || combined.contains("wifi") || combined.contains("airport")
                || combined.contains("wlan")) {
            return WIFI;
        }

        if (combined.contains("ethernet") || name.startsWith("en")) {
            return ETHERNET;
        }

        return IDLE;
    }

    private String getWifiSsid() {
        try {
            Process process = new ProcessBuilder(AIRPORT_PATH, "-I")
                    .redirectErrorStream(true)
                    .start();

            String output = readProcessOutput(process);
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return UNKNOWN;
            }

            for (String line : output.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("SSID:")) {
                    String ssid = trimmed.substring("SSID:".length()).trim();
                    return ssid.isBlank() ? UNKNOWN : ssid;
                }
            }
        } catch (IOException exception) {
            return UNKNOWN;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return UNKNOWN;
        }

        return UNKNOWN;
    }

    private String getLocalIpAddress() {
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            return address == null || address.isBlank() ? NOT_AVAILABLE : address;
        } catch (IOException exception) {
            return NOT_AVAILABLE;
        }
    }

    private double getPingMs() {
        try {
            Process process = new ProcessBuilder("ping", "-c", "1", "google.com")
                    .redirectErrorStream(true)
                    .start();

            String output = readProcessOutput(process);
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }

            Matcher matcher = PING_TIME_PATTERN.matcher(output);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (IOException exception) {
            return -1;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return -1;
        }

        return -1;
    }

    private int getActiveConnectionsCount() {
        InternetProtocolStats internetProtocolStats = operatingSystem.getInternetProtocolStats();
        if (internetProtocolStats == null) {
            return 0;
        }

        return internetProtocolStats.getConnections().size();
    }

    private String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString();
        }
    }
}
