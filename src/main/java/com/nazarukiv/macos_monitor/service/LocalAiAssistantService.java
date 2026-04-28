package com.nazarukiv.macos_monitor.service;

import com.nazarukiv.macos_monitor.model.AssistantResponse;
import com.nazarukiv.macos_monitor.model.BatteryInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.NetworkInfo;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import com.nazarukiv.macos_monitor.model.SystemSnapshot;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocalAiAssistantService implements AiAssistantService {
    @Override
    public AssistantResponse analyzeConcern(SystemSnapshot snapshot) {
        ConcernType concernType = ConcernType.from(snapshot.getConcern());

        String analysis = switch (concernType) {
            case BATTERY -> analyzeBattery(snapshot);
            case NETWORK -> analyzeNetwork(snapshot);
            case MEMORY -> analyzeMemory(snapshot);
            case PERFORMANCE -> analyzePerformance(snapshot);
            case GENERAL -> analyzeGeneral(snapshot);
        };

        return new AssistantResponse("On-device diagnostic assistant", analysis);
    }

    private String analyzePerformance(SystemSnapshot snapshot) {
        double cpuUsage = snapshot.getCpuInfo().getCpuUsage();
        MemoryInfo memory = snapshot.getMemoryInfo();
        double memoryPercent = getMemoryUsagePercent(memory);
        List<ProcessInfo> hotProcesses = snapshot.getTopProcesses().stream().limit(3).toList();

        String likelyCause;
        if (cpuUsage >= 75) {
            likelyCause = "The system is under heavy CPU load, mainly from " + joinProcesses(hotProcesses) + ".";
        } else if (memoryPercent >= 85) {
            likelyCause = "Memory pressure looks more likely than raw CPU load right now.";
        } else {
            likelyCause = "The snapshot does not show extreme CPU or memory pressure right now, so the slowdown may be bursty or app-specific.";
        }

        return """
                What I found
                CPU usage is %.1f%%. Memory usage is %.0f%% of total RAM. Top active processes: %s.

                Likely cause
                %s

                What to do now
                1. Close or restart the top CPU-heavy apps first.
                2. If the issue returns, compare which process repeatedly appears at the top.
                3. If the Mac still feels slow while CPU and RAM stay moderate, the next area to inspect is disk or login items.

                When to worry
                Sustained CPU above 80%% or memory above 90%% for several minutes usually means a specific app or background workload needs attention.
                """
                .formatted(cpuUsage, memoryPercent, joinProcesses(hotProcesses), likelyCause);
    }

    private String analyzeBattery(SystemSnapshot snapshot) {
        BatteryInfo battery = snapshot.getBatteryInfo();
        double cpuUsage = snapshot.getCpuInfo().getCpuUsage();
        String topProcesses = joinProcesses(snapshot.getTopProcesses().stream().limit(3).toList());

        String likelyCause;
        if (battery.getPercentage() < 0) {
            likelyCause = "This Mac does not report a battery, so battery-specific diagnosis is unavailable.";
        } else if (cpuUsage >= 60) {
            likelyCause = "Battery drain is likely being accelerated by current CPU activity, especially from " + topProcesses + ".";
        } else if (snapshot.getNetworkInfo().getUploadSpeed() > 1 || snapshot.getNetworkInfo().getDownloadSpeed() > 1) {
            likelyCause = "Network-heavy activity may be contributing to battery drain.";
        } else {
            likelyCause = "Battery drain does not appear to be driven by major CPU or network pressure in this snapshot.";
        }

        return """
                What I found
                Battery is at %.0f%% and currently %s. Estimated time remaining: %s. CPU usage is %.1f%%.

                Likely cause
                %s

                What to do now
                1. Check the top active apps and close the ones you do not need.
                2. Lower screen brightness and unplug unnecessary peripherals if drain continues.
                3. If drain stays high while the Mac is mostly idle, add battery health metrics next.

                When to worry
                Rapid discharge during light usage usually means a runaway app, heavy browser workload, or degraded battery health.
                """
                .formatted(
                        Math.max(battery.getPercentage(), 0),
                        battery.getStatus(),
                        battery.getTimeRemaining(),
                        cpuUsage,
                        likelyCause
                );
    }

    private String analyzeNetwork(SystemSnapshot snapshot) {
        NetworkInfo network = snapshot.getNetworkInfo();

        String likelyCause;
        if (network.getPingMs() < 0) {
            likelyCause = "The snapshot could not confirm stable external connectivity.";
        } else if (network.getPingMs() > 120) {
            likelyCause = "High latency is the clearest issue right now.";
        } else if (network.getDownloadSpeed() < 0.05 && network.getUploadSpeed() < 0.05) {
            likelyCause = "The connection is present, but there is very little observed traffic during the measurement window.";
        } else {
            likelyCause = "The connection looks functional in this snapshot, so any issue may be intermittent or tied to a specific app or site.";
        }

        return """
                What I found
                Status is %s on %s. Ping is %s. Download is %.2f MB/s and upload is %.2f MB/s. Active connections: %d.

                Likely cause
                %s

                What to do now
                1. If latency is high, test again closer to the router or on another network.
                2. If only one app is slow, the issue is probably app-specific rather than system-wide.
                3. If Wi-Fi is unstable, compare this with Ethernet if available.

                When to worry
                Repeated high ping, disconnects, or zero throughput across multiple refreshes points to a real network issue rather than temporary inactivity.
                """
                .formatted(
                        network.getStatus(),
                        blankTo(network.getInterfaceName(), "Unknown interface"),
                        formatPing(network.getPingMs()),
                        Math.max(network.getDownloadSpeed(), 0),
                        Math.max(network.getUploadSpeed(), 0),
                        Math.max(network.getActiveConnections(), 0),
                        likelyCause
                );
    }

    private String analyzeMemory(SystemSnapshot snapshot) {
        MemoryInfo memory = snapshot.getMemoryInfo();
        double memoryPercent = getMemoryUsagePercent(memory);
        String topProcesses = joinProcesses(snapshot.getTopProcesses().stream().limit(3).toList());

        String likelyCause = memoryPercent >= 85
                ? "RAM usage is high enough that memory pressure may be affecting responsiveness."
                : "RAM usage is not extreme in this snapshot, so memory is probably not the main bottleneck right now.";

        return """
                What I found
                Memory usage is %.1f GB out of %.1f GB, about %.0f%% total. Top visible processes: %s.

                Likely cause
                %s

                What to do now
                1. Close the heaviest apps or browser tabs first.
                2. Watch whether the same processes keep climbing over time.
                3. Add swap and memory pressure metrics next if you want deeper macOS memory diagnosis.

                When to worry
                If RAM stays near full and the Mac stutters, swap and memory pressure are usually the next indicators to check.
                """
                .formatted(
                        memory.getUsedMemory() / 1024.0,
                        memory.getTotalMemory() / 1024.0,
                        memoryPercent,
                        topProcesses,
                        likelyCause
                );
    }

    private String analyzeGeneral(SystemSnapshot snapshot) {
        return """
                What I found
                CPU is %.1f%%, memory usage is %.0f%%, battery is %s, and network status is %s.

                Likely cause
                Your concern is broad, so I checked the main system signals first. The strongest signal right now is: %s

                What to do now
                1. Mention a specific symptom like slow apps, battery drain, or bad Wi-Fi for a more targeted explanation.
                2. Use the top process list to identify any app that looks consistently busy.
                3. Re-run the analysis when the problem is actively happening.

                When to worry
                The most useful diagnostic snapshots happen while the issue is occurring, not after the system has settled down.
                """
                .formatted(
                        snapshot.getCpuInfo().getCpuUsage(),
                        getMemoryUsagePercent(snapshot.getMemoryInfo()),
                        snapshot.getBatteryInfo().getPercentage() < 0
                                ? "not available"
                                : String.format("%.0f%%", snapshot.getBatteryInfo().getPercentage()),
                        snapshot.getNetworkInfo().getStatus(),
                        summarizePrimarySignal(snapshot)
                );
    }

    private double getMemoryUsagePercent(MemoryInfo memoryInfo) {
        if (memoryInfo.getTotalMemory() == 0) {
            return 0;
        }
        return (memoryInfo.getUsedMemory() * 100.0) / memoryInfo.getTotalMemory();
    }

    private String joinProcesses(List<ProcessInfo> processes) {
        if (processes.isEmpty()) {
            return "no dominant process detected";
        }

        return processes.stream()
                .map(process -> process.getName() + " (" + String.format("%.1f%% CPU", process.getCpuUsage()) + ")")
                .collect(Collectors.joining(", "));
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatPing(double pingMs) {
        return pingMs < 0 ? "N/A" : String.format("%.1f ms", pingMs);
    }

    private String summarizePrimarySignal(SystemSnapshot snapshot) {
        double cpu = snapshot.getCpuInfo().getCpuUsage();
        double memory = getMemoryUsagePercent(snapshot.getMemoryInfo());
        double ping = snapshot.getNetworkInfo().getPingMs();

        if (cpu >= 75) {
            return "high CPU usage";
        }
        if (memory >= 85) {
            return "high memory usage";
        }
        if (ping >= 120) {
            return "high network latency";
        }
        return "no single severe bottleneck";
    }

    private enum ConcernType {
        PERFORMANCE,
        BATTERY,
        NETWORK,
        MEMORY,
        GENERAL;

        private static ConcernType from(String concern) {
            String normalized = concern == null ? "" : concern.toLowerCase(Locale.ROOT);

            if (containsAny(normalized, "battery", "charge", "drain", "power")) {
                return BATTERY;
            }
            if (containsAny(normalized, "wifi", "wi-fi", "internet", "network", "latency", "ping", "connection")) {
                return NETWORK;
            }
            if (containsAny(normalized, "memory", "ram", "swap")) {
                return MEMORY;
            }
            if (containsAny(normalized, "slow", "lag", "hot", "freeze", "stuck", "performance", "cpu")) {
                return PERFORMANCE;
            }
            return GENERAL;
        }

        private static boolean containsAny(String source, String... values) {
            for (String value : values) {
                if (source.contains(value)) {
                    return true;
                }
            }
            return false;
        }
    }
}
