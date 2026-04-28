package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.BatteryInfo;
import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.ProcessDetails;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import com.nazarukiv.macos_monitor.service.AiAssistantService;
import com.nazarukiv.macos_monitor.service.OpenAiAssistantService;
import com.nazarukiv.macos_monitor.service.SystemMetricsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DashboardController {
    private static final int MAX_POINTS = 30;
    private static final String BATTERY_LOW_STYLE = "battery-low";
    private static final String BATTERY_HIGH_STYLE = "battery-high";

    @FXML
    private Label cpuUsageLabel;

    @FXML
    private Label ramUsageLabel;

    @FXML
    private Label batteryLabel;

    @FXML
    private Label batteryStatusLabel;

    @FXML
    private Label batteryTimeLabel;

    @FXML
    private ProgressBar cpuBar;

    @FXML
    private ProgressBar ramBar;

    @FXML
    private LineChart<Number, Number> cpuChart;

    @FXML
    private NumberAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TableView<ProcessInfo> processTable;

    @FXML
    private TextField searchField;

    @FXML
    private ToggleButton refreshToggle;

    @FXML
    private Button systemInfoButton;

    @FXML
    private Button connectionButton;

    @FXML
    private Button assistantButton;

    private FilteredList<ProcessInfo> filteredProcesses;
    private ObservableList<ProcessInfo> masterData;
    private XYChart.Series<Number, Number> cpuSeries;
    private int timeCounter = 0;
    private boolean isRunning = true;
    private double maxCpu = 0;
    private SystemMetricsService systemMetricsService;
    private final AiAssistantService aiAssistantService = new OpenAiAssistantService();
    private Stage networkStage;
    private NetworkController networkController;
    private Stage assistantStage;

    @FXML
    private void initialize() {
        masterData = FXCollections.observableArrayList();
        filteredProcesses = new FilteredList<>(masterData, process -> true);

        cpuSeries = new XYChart.Series<>();
        cpuChart.getData().add(cpuSeries);
        cpuChart.setCreateSymbols(false);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(MAX_POINTS - 1);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);

        TableColumn<ProcessInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(160);
        nameColumn.getStyleClass().add("name-column");

        TableColumn<ProcessInfo, String> cpuColumn = new TableColumn<>("CPU (1 core %)");
        cpuColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.1f", cell.getValue().getCpuUsage())
                )
        );
        cpuColumn.setPrefWidth(80);
        cpuColumn.getStyleClass().add("cpu-column");
        cpuColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<ProcessInfo, Long> memoryColumn = new TableColumn<>("Memory");
        memoryColumn.setCellValueFactory(new PropertyValueFactory<>("memoryUsage"));
        memoryColumn.setPrefWidth(120);
        memoryColumn.getStyleClass().add("memory-column");
        memoryColumn.setStyle("-fx-alignment: CENTER;");

        processTable.getColumns().setAll(nameColumn, cpuColumn, memoryColumn);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        processTable.setItems(filteredProcesses);
        processTable.setRowFactory(tv -> {
            TableRow<ProcessInfo> row = new TableRow<>() {
                @Override
                protected void updateItem(ProcessInfo item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setStyle("");
                        return;
                    }

                    if (Double.compare(item.getCpuUsage(), maxCpu) == 0) {
                        setStyle("-fx-background-color: rgba(255, 214, 153, 0.45); -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openProcessDetailsWindow(row.getItem());
                }
            });

            return row;
        });
        refreshToggle.setSelected(true);
        refreshToggle.setOnAction(event -> {
            isRunning = refreshToggle.isSelected();
            refreshToggle.setText(isRunning ? "Pause" : "Resume");
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();

            filteredProcesses.setPredicate(process -> {
                if (filter.isEmpty()) {
                    return true;
                }

                return process.getName().toLowerCase().contains(filter);
            });
        });
    }

    public void updateMetrics(CpuInfo cpu, MemoryInfo memory) {
        double cpuUsage = cpu.getCpuUsage();
        double memoryUsage = memory.getTotalMemory() == 0
                ? 0
                : (double) memory.getUsedMemory() / memory.getTotalMemory();

        cpuUsageLabel.setText("CPU: " + String.format("%.1f", cpuUsage) + "%");
        cpuBar.setProgress(cpuUsage / 100.0);

        double usedGb = memory.getUsedMemory() / 1024.0;
        double totalGb = memory.getTotalMemory() / 1024.0;

        ramUsageLabel.setText(
                "RAM: " + String.format("%.1f", usedGb) + " / " +
                        String.format("%.1f", totalGb) + " GB"
        );
        ramBar.setProgress(memoryUsage);

        cpuSeries.getData().add(new XYChart.Data<>(timeCounter++, cpuUsage));
        if (cpuSeries.getData().size() > MAX_POINTS) {
            cpuSeries.getData().remove(0);
        }

        int lowerBound = Math.max(0, timeCounter - MAX_POINTS);
        xAxis.setLowerBound(lowerBound);
        xAxis.setUpperBound(lowerBound + MAX_POINTS - 1);
    }

    public void updateProcesses(List<ProcessInfo> processes) {
        maxCpu = getMaxCpu(processes);
        masterData.setAll(processes);
        processTable.refresh();
    }

    public void updateBattery(BatteryInfo battery) {
        batteryLabel.getStyleClass().removeAll(BATTERY_LOW_STYLE, BATTERY_HIGH_STYLE);

        if (battery.getPercentage() < 0) {
            batteryLabel.setText("Battery: N/A");
            batteryStatusLabel.setText("No battery");
            batteryTimeLabel.setText("");
            return;
        }

        batteryLabel.setText("Battery: " + String.format("%.0f%%", battery.getPercentage()));
        batteryStatusLabel.setText(battery.getStatus());
        batteryTimeLabel.setText("Time left: " + battery.getTimeRemaining());

        if (battery.getPercentage() < 20) {
            batteryLabel.getStyleClass().add(BATTERY_LOW_STYLE);
        } else if (battery.getPercentage() > 80) {
            batteryLabel.getStyleClass().add(BATTERY_HIGH_STYLE);
        }
    }

    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("System Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setSystemMetricsService(SystemMetricsService systemMetricsService) {
        this.systemMetricsService = systemMetricsService;
    }

    @FXML
    private void openSystemInfoWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/system-info.fxml")
        );

        Scene scene = new Scene(loader.load(), 420, 280);
        applyStyles(scene);
        SystemInfoController controller = loader.getController();
        controller.setSystemInfo(systemMetricsService.getSystemInfo());

        Stage stage = new Stage();
        stage.setTitle("System Info");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    private void openConnectionWindow() throws IOException {
        if (networkStage != null && networkStage.isShowing()) {
            networkStage.toFront();
            networkStage.requestFocus();
            return;
        }

        if (networkStage == null) {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/network-view.fxml")
            );

            Scene scene = new Scene(loader.load(), 380, 260);
            applyStyles(scene);
            networkController = loader.getController();
            networkController.setSystemMetricsService(systemMetricsService);

            networkStage = new Stage();
            networkStage.setTitle("Check My Connection");
            networkStage.setScene(scene);
            networkStage.setResizable(false);
        } else if (networkController != null) {
            networkController.refreshConnectionTest();
        }

        networkStage.show();
        networkStage.centerOnScreen();
        networkStage.toFront();
        networkStage.requestFocus();
    }

    @FXML
    private void openAssistantWindow() throws IOException {
        if (assistantStage != null && assistantStage.isShowing()) {
            assistantStage.toFront();
            assistantStage.requestFocus();
            return;
        }

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/ai-assistant-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 620, 560);
        applyStyles(scene);

        AiAssistantController controller = loader.getController();
        controller.setDependencies(systemMetricsService, aiAssistantService);

        assistantStage = new Stage();
        assistantStage.setTitle("AI Diagnostic Assistant");
        assistantStage.setScene(scene);
        assistantStage.setMinWidth(540);
        assistantStage.setMinHeight(500);
        assistantStage.show();
        assistantStage.centerOnScreen();
        assistantStage.toFront();
        assistantStage.requestFocus();
    }

    private void openProcessDetailsWindow(ProcessInfo processInfo) {
        ProcessDetails details = systemMetricsService.getProcessDetails(processInfo.getPid());
        if (details == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/process-details.fxml")
            );

            Scene scene = new Scene(loader.load(), 420, 260);
            applyStyles(scene);
            ProcessDetailsController controller = loader.getController();
            controller.setProcessDetails(details);

            Stage stage = new Stage();
            stage.setTitle("Process Details");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to open process details window", exception);
        }
    }

    private double getMaxCpu(List<ProcessInfo> processes) {
        return processes.stream()
                .mapToDouble(ProcessInfo::getCpuUsage)
                .max()
                .orElse(0);
    }

    private void applyStyles(Scene scene) {
        scene.getStylesheets().add(
                HelloApplication.class.getResource("/com/nazarukiv/macos_monitor/styles.css").toExternalForm()
        );
    }
}
