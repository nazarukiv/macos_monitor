package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.AssistantResponse;
import com.nazarukiv.macos_monitor.model.SystemSnapshot;
import com.nazarukiv.macos_monitor.service.AiAssistantService;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class AiAssistantController {
    @FXML
    private TextArea concernInput;

    @FXML
    private TextArea responseOutput;

    @FXML
    private Label statusLabel;

    @FXML
    private Label providerLabel;

    @FXML
    private Button analyzeButton;

    private SystemMetricsService systemMetricsService;
    private AiAssistantService aiAssistantService;
    private volatile boolean analysisRunning;

    public void setDependencies(SystemMetricsService systemMetricsService, AiAssistantService aiAssistantService) {
        this.systemMetricsService = systemMetricsService;
        this.aiAssistantService = aiAssistantService;
    }

    @FXML
    private void analyzeConcern() {
        if (analysisRunning || systemMetricsService == null || aiAssistantService == null) {
            return;
        }

        String concern = concernInput.getText() == null ? "" : concernInput.getText().trim();
        if (concern.isBlank()) {
            statusLabel.setText("Describe the problem first.");
            return;
        }

        analysisRunning = true;
        analyzeButton.setDisable(true);
        statusLabel.setText("Collecting system data and generating explanation...");
        providerLabel.setText("Provider: preparing snapshot");
        responseOutput.setText("");

        Thread worker = new Thread(() -> {
            SystemSnapshot snapshot = systemMetricsService.captureSnapshot(concern);
            AssistantResponse assistantResponse = aiAssistantService.analyzeConcern(snapshot);

            Platform.runLater(() -> {
                responseOutput.setText(assistantResponse.getSummary());
                providerLabel.setText("Provider: " + assistantResponse.getProvider());
                statusLabel.setText("Analysis complete.");
                analyzeButton.setDisable(false);
                analysisRunning = false;
            });
        }, "ai-assistant-analysis");
        worker.setDaemon(true);
        worker.start();
    }
}
