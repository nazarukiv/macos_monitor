package com.nazarukiv.macos_monitor.model;

public class MemoryInfo {
    private long totalMemory;
    private long usedMemory;

    public MemoryInfo(long totalMemory, long usedMemory) {
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }

    @Override
    public String toString() {
        return "MemoryInfo{" +
                "totalMemory=" + totalMemory +
                ", usedMemory=" + usedMemory +
                '}';
    }
}
