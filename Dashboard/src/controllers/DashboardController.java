// Edited by Samuel CUlp
// This file controls the dashboard view and displays info about farm. 


package controllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import models.MonitoringReading;
import services.FarmDataService;

public class DashboardController {

    @FXML
    private Label fieldsCountLabel;

    @FXML
    private Label dronesCountLabel;

    @FXML
    private Label soilMoistureLabel;

    @FXML
    private LineChart<String, Number> weeklyChart;
    @FXML
    private LineChart<String, Number> monthlyChart;

    private final FarmDataService farmDataService = new FarmDataService();

    @FXML
    public void initialize() {
        try {
            // Load farm fields and drones
            int fieldCount = farmDataService.getFields().size();
            int droneCount = farmDataService.getDrones().size();

            fieldsCountLabel.setText("Farm Fields: " + fieldCount);
            dronesCountLabel.setText(String.valueOf(droneCount));

            // Load monitoring readings
            List<MonitoringReading> readings =
                    farmDataService.getReadings();

            // Sort readings from oldest to newest
            readings.sort(
                Comparator.comparing(MonitoringReading::getTimestamp)
            );

            // Display soil moisture data
            loadWeeklyChart(readings);
            
            loadMonthlyChart(readings);

            // Update today's soil moisture value
            if (!readings.isEmpty()) {
                MonitoringReading latest =
                        readings.get(readings.size() - 1);

                soilMoistureLabel.setText(
                    String.format("%.0f%%",
                        latest.getSoilMoisturePercent())
                );
            } else {
                soilMoistureLabel.setText("No data");
            }

        } catch (IOException e) {
            fieldsCountLabel.setText("Error");
            dronesCountLabel.setText("Error");
            soilMoistureLabel.setText("Error");
            e.printStackTrace();
        }
    }

    private void loadWeeklyChart(List<MonitoringReading> readings) {

        weeklyChart.getData().clear();

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        series.setName("North Field");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM d");

        //  7 recent readings
        int start = Math.max(0, readings.size() - 7);

        for (int i = start; i < readings.size(); i++) {

            MonitoringReading reading = readings.get(i);

            String date = reading.getTimestamp().format(formatter);

            series.getData().add(
                new XYChart.Data<>(
                    date,
                    reading.getSoilMoisturePercent()
                )
            );
        }

        weeklyChart.getData().add(series);
    }

	private void loadMonthlyChart(List<MonitoringReading> readings) {
	
	    monthlyChart.getData().clear();
	
	    XYChart.Series<String, Number> series =
	            new XYChart.Series<>();
	
	    series.setName("North Field");
	
	    // Use the most recent 30 readings
	    int start = Math.max(0, readings.size() - 30);
	
	    int weekNumber = 1;
	
	    // Group readings into sets of 7
	    for (int i = start; i < readings.size(); i += 7) {
	
	        double totalMoisture = 0;
	        int count = 0;
	
	        // Calculate the average for this group
	        for (int j = i;
	             j < i + 7 && j < readings.size();
	             j++) {
	
	            totalMoisture +=
	                    readings.get(j).getSoilMoisturePercent();
	
	            count++;
	        }
	
	        double average = totalMoisture / count;
	
	        series.getData().add(
	            new XYChart.Data<>(
	                "Week " + weekNumber,
	                average
	            )
	        );
	
	        weekNumber++;
	    }
	
	    monthlyChart.getData().add(series);
	}

    
}

