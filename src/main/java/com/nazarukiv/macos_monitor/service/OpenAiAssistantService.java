package com.nazarukiv.macos_monitor.service;

import com.nazarukiv.macos_monitor.model.AssistantResponse;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import com.nazarukiv.macos_monitor.model.SystemSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenAiAssistantService implements AiAssistantService {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final Pattern OUTPUT_TEXT_PATTERN = Pattern.compile(
            "\"type\"\\s*:\\s*\"output_text\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"",
            Pattern.DOTALL
    );
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_MODEL = "gpt-5";

    private final LocalAiAssistantService fallbackService;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public OpenAiAssistantService() {
        this(new LocalAiAssistantService(), HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public OpenAiAssistantService(LocalAiAssistantService fallbackService, HttpClient httpClient) {
        this.fallbackService = fallbackService;
        this.httpClient = httpClient;
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.model = readModel();
    }

    @Override
    public AssistantResponse analyzeConcern(SystemSnapshot snapshot) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackService.analyzeConcern(snapshot);
        }

        try {
            String requestBody = buildRequestBody(snapshot);
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackWithNote(snapshot, "OpenAI request failed with HTTP " + response.statusCode() + ".");
            }

            String outputText = extractOutputText(response.body());
            if (outputText.isBlank()) {
                return fallbackWithNote(snapshot, "OpenAI response did not include assistant text.");
            }

            return new AssistantResponse("OpenAI (" + model + ")", outputText.trim());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return fallbackWithNote(snapshot, "OpenAI request failed, so the app used its local diagnostic rules instead.");
        }
    }

    private AssistantResponse fallbackWithNote(SystemSnapshot snapshot, String note) {
        AssistantResponse fallback = fallbackService.analyzeConcern(snapshot);
        return new AssistantResponse(
                fallback.getProvider(),
                "Note\n" + note + "\n\n" + fallback.getSummary()
        );
    }

    private String buildRequestBody(SystemSnapshot snapshot) {
        String instructions = """
                You are a concise macOS diagnostics assistant inside a monitoring app.
                Use only the provided system snapshot.
                Do not invent metrics or claim certainty when signals are weak.
                Answer with these exact sections:
                What I found
                Likely cause
                What to do now
                When to worry
                Keep the answer practical and short.
                """;

        String userPrompt = """
                User concern:
                %s

                System snapshot:
                %s
                """.formatted(snapshot.getConcern(), buildSnapshotSummary(snapshot));

        return """
                {
                  "model": "%s",
                  "reasoning": { "effort": "low" },
                  "instructions": "%s",
                  "input": "%s"
                }
                """
                .formatted(
                        escapeJson(model),
                        escapeJson(instructions),
                        escapeJson(userPrompt)
                );
    }

    private String buildSnapshotSummary(SystemSnapshot snapshot) {
        String processSummary = snapshot.getTopProcesses().stream()
                .limit(5)
                .map(this::formatProcess)
                .collect(Collectors.joining(", "));

        return """
                Captured at: %s
                CPU usage: %.1f%%
                Memory: %d MB used of %d MB
                Battery: %.0f%%, %s, %s remaining
                Network: status=%s, ping=%s, download=%.2f MB/s, upload=%.2f MB/s, interface=%s, ssid=%s, activeConnections=%d
                System: cpu=%s, os=%s, uptime=%s, diskFree=%s of %s
                Top processes: %s
                """.formatted(
                        snapshot.getCapturedAt().format(TIMESTAMP_FORMAT),
                        snapshot.getCpuInfo().getCpuUsage(),
                        snapshot.getMemoryInfo().getUsedMemory(),
                        snapshot.getMemoryInfo().getTotalMemory(),
                        Math.max(snapshot.getBatteryInfo().getPercentage(), 0),
                        snapshot.getBatteryInfo().getStatus(),
                        snapshot.getBatteryInfo().getTimeRemaining(),
                        snapshot.getNetworkInfo().getStatus(),
                        snapshot.getNetworkInfo().getPingMs() < 0 ? "N/A" : String.format("%.1f ms", snapshot.getNetworkInfo().getPingMs()),
                        Math.max(snapshot.getNetworkInfo().getDownloadSpeed(), 0),
                        Math.max(snapshot.getNetworkInfo().getUploadSpeed(), 0),
                        emptyTo(snapshot.getNetworkInfo().getInterfaceName(), "Unknown"),
                        emptyTo(snapshot.getNetworkInfo().getSsid(), "Unknown"),
                        Math.max(snapshot.getNetworkInfo().getActiveConnections(), 0),
                        snapshot.getSystemDetails().getCpuName(),
                        snapshot.getSystemDetails().getOsNameAndVersion(),
                        snapshot.getSystemDetails().getUptime(),
                        snapshot.getSystemDetails().getDiskFree(),
                        snapshot.getSystemDetails().getDiskTotal(),
                        processSummary.isBlank() ? "none" : processSummary
                );
    }

    private String formatProcess(ProcessInfo process) {
        return process.getName() + " (" + String.format("%.1f%% CPU, %d MB RAM", process.getCpuUsage(), process.getMemoryUsage()) + ")";
    }

    private String emptyTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String extractOutputText(String responseBody) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = OUTPUT_TEXT_PATTERN.matcher(responseBody);
        while (matcher.find()) {
            builder.append(unescapeJson(matcher.group(1)));
        }
        return builder.toString();
    }

    private String readModel() {
        String configuredModel = System.getenv("OPENAI_MODEL");
        return configuredModel == null || configuredModel.isBlank() ? DEFAULT_MODEL : configuredModel;
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }

    private String unescapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    default -> builder.append(next);
                }
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
