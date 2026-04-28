package com.nazarukiv.macos_monitor.model;

public class AssistantResponse {
    private final String provider;
    private final String summary;

    public AssistantResponse(String provider, String summary) {
        this.provider = provider;
        this.summary = summary;
    }

    public String getProvider() {
        return provider;
    }

    public String getSummary() {
        return summary;
    }
}
