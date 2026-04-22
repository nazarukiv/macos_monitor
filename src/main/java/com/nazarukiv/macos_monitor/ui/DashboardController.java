package com.nazarukiv.macos_monitor.ui;

import com.nazarukiv.macos_monitor.model.CpuInfo;
import com.nazarukiv.macos_monitor.model.MemoryInfo;
import com.nazarukiv.macos_monitor.model.ProcessInfo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class DashboardController {
    private static final int MAX_POINTS = 30;

    @FXML
    private Label cpuUsageLabel;

    @FXML
    private Label ramUsageLabel;

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

    private XYChart.Series<Number, Number> cpuSeries;
    private int timeCounter = 0;

    @FXML
    private void initialize() {
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

        TableColumn<ProcessInfo, String> cpuColumn = new TableColumn<>("CPU (1 core %)");
        cpuColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.1f", cell.getValue().getCpuUsage())
                )
        );
        cpuColumn.setPrefWidth(80);

        TableColumn<ProcessInfo, Long> memoryColumn = new TableColumn<>("Memory");
        memoryColumn.setCellValueFactory(new PropertyValueFactory<>("memoryUsage"));
        memoryColumn.setPrefWidth(120);

        processTable.getColumns().setAll(nameColumn, cpuColumn, memoryColumn);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
        processTable.getItems().setAll(processes);
    }
}
