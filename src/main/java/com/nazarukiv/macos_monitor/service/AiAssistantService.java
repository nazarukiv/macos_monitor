package com.nazarukiv.macos_monitor.service;

import com.nazarukiv.macos_monitor.model.AssistantResponse;
import com.nazarukiv.macos_monitor.model.SystemSnapshot;

public interface AiAssistantService {
    AssistantResponse analyzeConcern(SystemSnapshot snapshot);
}
