package controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

import models.*;
import services.FarmDataService;
import services.JsonDataStore;
import Drone.TelloController;

public class DashboardController {

    @FXML private BorderPane contentPane;
    @FXML private Label statusLabel;
    @FXML private Label dataPathLabel;
    @FXML private VBox recentAlertsBox;
    @FXML private Label sidebarMoistureLabel;
    @FXML private Label sidebarHealthLabel;
    @FXML private Label sidebarDronesLabel;
    @FXML private Label sidebarHarvestLabel;
    @FXML private Label sidebarReadingLabel;

    private final FarmDataService service = new FarmDataService();
    private final TelloController tello = new TelloController();

    private List<FarmField> fields = new ArrayList<>();
    private List<Drone> drones = new ArrayList<>();
    private List<MonitoringReading> readings = new ArrayList<>();
    private List<FarmEquipment> equipment = new ArrayList<>();
    private List<DroneMission> missions = new ArrayList<>();
    private List<FarmAlert> alerts = new ArrayList<>();

    private boolean loaded;

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        dataPathLabel.setText(
                "Data folder: " + new JsonDataStore().getDataDirectory());
        reloadData();
    }

    @FXML
    private void reloadData() {
        loaded = false;

        try {
            List<FarmField> newFields = service.getFields();
            List<Drone> newDrones = service.getDrones();
            List<MonitoringReading> newReadings = service.getReadings();
            List<FarmEquipment> newEquipment = service.getEquipment();
            List<DroneMission> newMissions = service.getMissions();
            List<FarmAlert> newAlerts = service.getAlerts();

            newReadings.sort(Comparator.comparing(
                    MonitoringReading::getTimestamp,
                    Comparator.nullsFirst(Comparator.naturalOrder())));

            fields = newFields;
            drones = newDrones;
            readings = newReadings;
            equipment = newEquipment;
            missions = newMissions;
            alerts = newAlerts;

            loaded = true;
            showOverview();

            statusLabel.setText(
                    "Loaded saved records. Changes are saved to JSON; "
                    + "no physical device commands are sent.");
        } catch (IOException | RuntimeException ex) {
            page("Data could not be loaded",
                    "Check the data folder and JSON contents.",
                    text("Details: " + ex.getMessage()));

            statusLabel.setText("Load failed. No changes were saved.");
            error(ex);
        }
    }

    private boolean ready() {
        if (!loaded) {
            info("Data unavailable",
                    "Use Reload Saved Data after fixing the loading error.");
        }
        return loaded;
    }

    @FXML
    private void showOverview() {
        if (!ready()) return;

        updateRightSidebar();

        VBox main = new VBox(12);
        main.getStyleClass().add("main-content");
        main.setPadding(new Insets(4));

        // FARM OVERVIEW PANEL
        Label heading = text("FARM OVERVIEW");
        heading.getStyleClass().add("panel-heading");

        Label mapPlaceholder = text("[ FARM / FIELD MAP ]");
        mapPlaceholder.getStyleClass().add("map-placeholder");
        mapPlaceholder.setMinHeight(95);
        mapPlaceholder.setMaxWidth(Double.MAX_VALUE);
        mapPlaceholder.setAlignment(javafx.geometry.Pos.CENTER);

        Label counts = text(
                "Farm Fields: " + fields.size()
                + "     Drones: " + drones.size()
                + "     Equipment: " + equipment.size());

        Label mapNote = text(
                "Saved farm records are loaded. "
                + "The interactive layout visualization is not implemented yet.");
        mapNote.setStyle("-fx-text-fill: #aab8c5;");

        HBox overviewActions = new HBox(
                10,
                button("View Fields", this::showFields),
                button("View Drones", this::showDrones),
                button("View Equipment", this::showEquipment));

        VBox overview = new VBox(
                12,
                heading,
                new Separator(),
                mapPlaceholder,
                counts,
                mapNote,
                new Separator(),
                overviewActions);

        overview.getStyleClass().add("panel");
        overview.setPadding(new Insets(18));

        // SHARED FIELD SELECTION FOR BOTH CHARTS
        Label fieldHeading = text("MONITORING FIELD");
        fieldHeading.getStyleClass().add("section-heading");

        ComboBox<String> fieldChoice = new ComboBox<>();

        for (FarmField field : fields) {
            fieldChoice.getItems().add(
                    field.getId() + " — " + field.getName());
        }

        fieldChoice.setMaxWidth(Double.MAX_VALUE);

        Label readingSummary = text(
                "Select a field to view its saved readings.");

        VBox fieldPanel = new VBox(
                10, fieldHeading, fieldChoice, readingSummary);

        fieldPanel.getStyleClass().add("panel");
        fieldPanel.setPadding(new Insets(18));

        // WEEKLY-STYLE PANEL: ACTUALLY THE LATEST 7 READINGS
        LineChart<String, Number> weekly =
                createOverviewChart("Reading date");

        VBox weeklyPanel = overviewChartPanel(
                "SOIL MOISTURE — LATEST 7 READINGS",
                weekly);

        // MONTHLY-STYLE PANEL: ACTUALLY THE LATEST 30 READINGS
        LineChart<String, Number> monthly =
                createOverviewChart("Reading date");

        monthly.setCreateSymbols(false);

        VBox monthlyPanel = overviewChartPanel(
                "SOIL MOISTURE — LATEST 30 READINGS",
                monthly);

        Runnable updateCharts = () -> {
            int index = fieldChoice.getSelectionModel().getSelectedIndex();

            if (index < 0) {
                weekly.getData().clear();
                monthly.getData().clear();
                readingSummary.setText("No saved fields are available.");
                return;
            }

            FarmField field = fields.get(index);
            List<MonitoringReading> matching = readingsFor(field.getId());

            fillOverviewChart(weekly, field.getName(), matching, 7);
            fillOverviewChart(monthly, field.getName(), matching, 30);

            if (matching.isEmpty()) {
                readingSummary.setText(
                        "No monitoring readings saved for "
                        + field.getName() + ".");
            } else {
                MonitoringReading latest =
                        matching.get(matching.size() - 1);

                readingSummary.setText(
                        "Latest saved reading: "
                        + latest.getSoilMoisturePercent()
                        + "% soil moisture — "
                        + date(latest.getTimestamp())
                        + ". These are saved records, not live measurements.");
            }
        };

        fieldChoice.setOnAction(event -> updateCharts.run());

        main.getChildren().addAll(
                overview, fieldPanel, weeklyPanel, monthlyPanel);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("main-scroll");
        scroll.setStyle(
                "-fx-background: #101820;"
                + "-fx-background-color: #101820;");

        contentPane.setCenter(scroll);

        if (!fields.isEmpty()) {
            fieldChoice.getSelectionModel().selectFirst();
        }

        updateCharts.run();
    }

    private LineChart<String, Number> createOverviewChart(String xLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setTickLabelFill(Color.web("#c9d5df"));

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Moisture (%)");
        yAxis.setTickLabelFill(Color.web("#c9d5df"));

        LineChart<String, Number> chart =
                new LineChart<>(xAxis, yAxis);

        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(240);
        chart.setPrefHeight(280);

        return chart;
    }

    private VBox overviewChartPanel(
            String title,
            LineChart<String, Number> chart) {

        Label heading = text(title);
        heading.getStyleClass().add("chart-title");

        VBox panel = new VBox(10, heading, chart);
        panel.getStyleClass().add("chart-panel");
        panel.setPadding(new Insets(18));

        VBox.setVgrow(chart, Priority.ALWAYS);

        return panel;
    }

    private void fillOverviewChart(
            LineChart<String, Number> chart,
            String fieldName,
            List<MonitoringReading> matching,
            int limit) {

        chart.getData().clear();

        if (matching.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        series.setName(fieldName);

        int start = Math.max(0, matching.size() - limit);

        for (int i = start; i < matching.size(); i++) {
            MonitoringReading reading = matching.get(i);

            String label = reading.getTimestamp().format(
                    DateTimeFormatter.ofPattern("MMM d HH:mm"))
                    + " [" + reading.getId() + "]";

            series.getData().add(new XYChart.Data<>(
                    label,
                    reading.getSoilMoisturePercent()));
        }

        chart.getData().add(series);
    }

    private void updateRightSidebar() {
        sidebarDronesLabel.setText(String.valueOf(drones.size()));

        MonitoringReading latest = readings.stream()
                .filter(reading -> reading.getTimestamp() != null)
                .max(Comparator.comparing(MonitoringReading::getTimestamp))
                .orElse(null);

        if (latest == null) {
            sidebarMoistureLabel.setText("No data");
            sidebarHealthLabel.setText("No data");
            sidebarReadingLabel.setText("No saved monitoring readings.");
        } else {
            sidebarMoistureLabel.setText(
                    String.format("%.0f%%", latest.getSoilMoisturePercent()));

            sidebarHealthLabel.setText(
                    latest.getCropHealthStatus() == null
                            ? "Unknown"
                            : latest.getCropHealthStatus());

            String fieldName = fields.stream()
                    .filter(field -> field.getId().equals(latest.getFieldId()))
                    .map(FarmField::getName)
                    .findFirst()
                    .orElse(latest.getFieldId());

            sidebarReadingLabel.setText(
                    "Moisture and health: " + fieldName
                    + "\nRecorded: " + date(latest.getTimestamp()));
        }

        int readyCount = 0;
        int missingCount = 0;

        for (FarmField field : fields) {
            MonitoringReading reading = latestFor(field.getId());

            if (reading == null) {
                missingCount++;
            } else if (reading.getHarvestReadinessPercent() >= 80) {
                readyCount++;
            }
        }

        sidebarHarvestLabel.setText(
                readyCount + " fields"
                + (missingCount > 0
                        ? "\n" + missingCount + " unknown"
                        : ""));

        recentAlertsBox.getChildren().clear();

        List<FarmAlert> recent = new ArrayList<>(alerts);

        recent.sort(Comparator.comparing(
                FarmAlert::getTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));

        if (recent.isEmpty()) {
            recentAlertsBox.getChildren().add(
                    text("No saved alerts."));
            return;
        }

        for (int i = 0; i < Math.min(3, recent.size()); i++) {
            FarmAlert alert = recent.get(i);

            Label message = text(
                    (alert.isRead() ? "[Read] " : "[Unread] ")
                    + alert.getMessage());

            message.setStyle(
                    "-fx-text-fill: "
                    + (alert.isRead() ? "#aab8c5" : "#f0bd70")
                    + ";");

            Label timestamp = text(date(alert.getTimestamp()));
            timestamp.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #94a7b8;");

            recentAlertsBox.getChildren().add(
                    new VBox(4, message, timestamp));
        }
    }

    @FXML
    private void showFields() {
        if (!ready()) return;

        TableView<FarmField> table = table(fields);

        column(table, "ID", FarmField::getId);
        column(table, "Name", FarmField::getName);
        column(table, "Crop", FarmField::getCropType);
        column(table, "Acres", field -> field.getAreaAcres());
        column(table, "Latitude", field -> field.getLatitude());
        column(table, "Longitude", field -> field.getLongitude());

        page("Farm Fields",
                "These are saved geographic field records, "
                        + "not the assignment's editable layout objects.",
                table);
    }

    @FXML
    private void showFarmMap() {
        page("Farm Map / Layout",
                "The assignment's farm-layout editor is not implemented here.",
                text("The required editor needs an 800 × 600 plot, "
                        + "nested items and item-containers, and editable "
                        + "x/y coordinates, dimensions, names, and prices."),
                text("The existing FarmField records contain latitude, "
                        + "longitude, and acreage instead. Those records "
                        + "must not be treated as pixel coordinates."),
                button("View Saved Farm Fields", this::showFields));
    }

    @FXML
    private void showSoilMoisture() {
        if (!ready()) return;

        if (fields.isEmpty()) {
            page("Soil Moisture", "No saved fields are available.");
            return;
        }

        ComboBox<String> selection = new ComboBox<>();
        for (FarmField field : fields) {
            selection.getItems().add(
                    field.getId() + " — " + field.getName());
        }

        Label latestLabel = text("");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Reading timestamp");

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Soil moisture (%)");

        LineChart<String, Number> chart =
                new LineChart<>(xAxis, yAxis);

        chart.setAnimated(false);
        chart.setMinHeight(400);
        chart.setTitle("Latest 7 saved readings for the selected field");

        Runnable update = () -> {
            int index = selection.getSelectionModel().getSelectedIndex();
            if (index < 0) return;

            FarmField field = fields.get(index);
            List<MonitoringReading> matching = readingsFor(field.getId());

            chart.getData().clear();

            if (matching.isEmpty()) {
                latestLabel.setText(
                        "No monitoring readings saved for this field.");
                return;
            }

            MonitoringReading latest = matching.get(matching.size() - 1);

            latestLabel.setText(
                    "Latest saved soil moisture: "
                    + latest.getSoilMoisturePercent() + "%"
                    + " | Recorded: " + date(latest.getTimestamp()));

            XYChart.Series<String, Number> series =
                    new XYChart.Series<>();

            series.setName(field.getName());

            int start = Math.max(0, matching.size() - 7);

            for (int i = start; i < matching.size(); i++) {
                MonitoringReading reading = matching.get(i);

                series.getData().add(new XYChart.Data<>(
                        date(reading.getTimestamp()) + " [" + reading.getId() + "]",
                        reading.getSoilMoisturePercent()));
            }

            chart.getData().add(series);
        };

        selection.setOnAction(event -> update.run());

        page("Soil Moisture",
                "Filtered by field. Seven readings do not necessarily "
                        + "represent seven days.",
                selection, latestLabel, chart);

        selection.getSelectionModel().selectFirst();
        update.run();
    }

    @FXML
    private void showCropGrowth() {
        showFieldMetrics(false);
    }

    @FXML
    private void showHarvest() {
        showFieldMetrics(true);
    }

    private void showFieldMetrics(boolean harvest) {
        if (!ready()) return;

        TableView<FarmField> table = table(fields);

        column(table, "Field", FarmField::getName);
        column(table, "Crop", FarmField::getCropType);

        column(table, harvest ? "Readiness" : "Growth", field -> {
            MonitoringReading reading = latestFor(field.getId());
            if (reading == null) return "No data";

            return (harvest
                    ? reading.getHarvestReadinessPercent()
                    : reading.getCropGrowthPercent()) + "%";
        });

        column(table, "Latest reading", field -> {
            MonitoringReading reading = latestFor(field.getId());
            return reading == null ? "No data" : date(reading.getTimestamp());
        });

        if (harvest) {
            column(table, "Meets 80% threshold", field -> {
                MonitoringReading reading = latestFor(field.getId());
                if (reading == null) return "Unknown — no data";
                return reading.getHarvestReadinessPercent() >= 80
                        ? "Yes, at saved timestamp"
                        : "No, at saved timestamp";
            });
        } else {
            column(table, "Recorded health status", field -> {
                MonitoringReading reading = latestFor(field.getId());
                return reading == null
                        ? "No data" : reading.getCropHealthStatus();
            });
        }

        page(harvest ? "Harvest" : "Crop Growth",
                "Latest saved reading per field. "
                        + "Missing readings are shown as unknown, not zero.",
                table);
    }

    @FXML
    private void showEquipment() {
        if (!ready()) return;

        TableView<FarmEquipment> table = table(equipment);

        column(table, "ID", FarmEquipment::getId);
        column(table, "Name", FarmEquipment::getName);
        column(table, "Type", FarmEquipment::getType);
        column(table, "Status", FarmEquipment::getStatus);
        column(table, "Latitude", item -> item.getLatitude());
        column(table, "Longitude", item -> item.getLongitude());
        column(table, "Last recorded", item -> date(item.getLastDetected()));

        Button edit = button("Edit Selected Location", () -> {
            FarmEquipment item = selected(table);
            if (item == null) return;

            try {
                String latitude = prompt("Latitude",
                        "Enter latitude from -90 to 90:",
                        String.valueOf(item.getLatitude()));
                if (latitude == null) return;

                String longitude = prompt("Longitude",
                        "Enter longitude from -180 to 180:",
                        String.valueOf(item.getLongitude()));
                if (longitude == null) return;

                double lat = Double.parseDouble(latitude);
                double lon = Double.parseDouble(longitude);

                if (!Double.isFinite(lat) || !Double.isFinite(lon)
                        || lat < -90 || lat > 90
                        || lon < -180 || lon > 180) {
                    throw new IllegalArgumentException(
                            "Enter valid latitude and longitude values.");
                }

                if (!service.updateEquipmentLocation(item.getId(), lat, lon)) {
                    throw new IllegalArgumentException(
                            "Equipment record was not found.");
                }

                refreshAfterSave(this::showEquipment,
                        "Equipment location saved. "
                        + "The service updated its recorded timestamp.");
            } catch (IOException | IllegalArgumentException ex) {
                error(ex);
            }
        });

        page("Equipment",
                "Manual edits update saved records; they are not GPS detections.",
                edit, table);
    }

    @FXML
    private void showDrones() {
        if (!ready()) return;

        TableView<Drone> table = table(drones);

        column(table, "ID", Drone::getId);
        column(table, "Name", Drone::getName);
        column(table, "Battery", drone -> drone.getBatteryLevel() + "%");
        column(table, "Status", Drone::getStatus);
        column(table, "Last telemetry timestamp",
                drone -> date(drone.getLastUpdated()));

        Button edit = button("Edit Record", () -> {
            Drone drone = selected(table);
            if (drone == null) return;

            try {
                String status = choose("Drone status",
                        List.of("AVAILABLE", "CHARGING",
                                "IN_MISSION", "MAINTENANCE"),
                        drone.getStatus());
                if (status == null) return;

                String battery = prompt("Battery",
                        "Enter saved battery percentage from 0 to 100:",
                        String.valueOf(drone.getBatteryLevel()));
                if (battery == null) return;

                int level = Integer.parseInt(battery);

                if (!service.updateDroneStatus(drone.getId(), status, level)) {
                    throw new IllegalArgumentException(
                            "Battery must be 0–100 and the drone must exist.");
                }

                refreshAfterSave(this::showDrones,
                        "Drone record saved. No command was sent to a drone.");
            } catch (IOException | IllegalArgumentException ex) {
                error(ex);
            }
        });

        Button connectBtn = button("Connect Tello", () ->
            runTelloAsync("Connect SDK", () -> {
                String res = tello.connectSdk();
                String bat = tello.getBattery();
                System.out.println("SDK: " + res + " | Battery: " + bat + "%");
            }));

        Button takeoffBtn = button("Takeoff", () -> runTelloAsync("Takeoff", tello::takeoff));
        Button landBtn = button("Land", () -> runTelloAsync("Land", tello::land));
        Button emergencyBtn = button("EMERGENCY", () -> runTelloAsync("Emergency", tello::emergency));
        emergencyBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox coreActions = new HBox(8, edit, connectBtn, takeoffBtn, landBtn, emergencyBtn);

        // --- 4x4 FT MAPPED GRID WITH EXPLICIT X/Y METRIC AXES & FARM OBJECTS ---
        Label gridLabel = text("4x4 ft Flight Path Grid Map (X & Y Metric Axes)");
        gridLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c9d5df;");

        final double canvasSize = 440.0;
        Canvas pathCanvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext gc = pathCanvas.getGraphicsContext2D();

        final double originX = 42.0;
        final double originY = 14.0;
        final double cellSize = 90.0;
        final double gridSize = cellSize * 4.0; // 360px (0ft to 4ft)

        // Current live drone position (starts at center 2ft, 2ft, heading 0 deg)
        final int[] gridCoords = {2, 2, 0};
        final double[] dronePos = {originX + (2.0 * cellSize), originY + (2.0 * cellSize), 0.0};
        List<javafx.geometry.Point2D> flightTrail = new ArrayList<>();
        flightTrail.add(new javafx.geometry.Point2D(dronePos[0], dronePos[1]));

        // Planned path preview points computed immediately when queuing commands
        final int[] planCoords = {2, 2, 0};
        List<javafx.geometry.Point2D> plannedPath = new ArrayList<>();
        plannedPath.add(new javafx.geometry.Point2D(dronePos[0], dronePos[1]));

        final boolean[] isTurning = {false};
        final boolean[] turnLightState = {false};
        final double[] dashOffset = {0.0};

        Runnable redrawGrid = () -> {
            gc.setFill(Color.web("#0b1017"));
            gc.fillRect(0, 0, canvasSize, canvasSize);

            // Draw Metric Grid Lines & X/Y Measurement Labels
            gc.setStroke(Color.web("#1e293b"));
            gc.setLineWidth(1.5);
            gc.setFont(Font.font("System", 10));

            for (int i = 0; i <= 4; i++) {
                double xPos = originX + (i * cellSize);
                gc.strokeLine(xPos, originY, xPos, originY + gridSize);
                gc.setFill(Color.web("#94a3b8"));
                double xLabelOffset = (i == 4) ? 14.0 : 8.0;
                gc.fillText(i + "ft", xPos - xLabelOffset, originY + gridSize + 15);

                double yPos = originY + (i * cellSize);
                gc.strokeLine(originX, yPos, originX + gridSize, yPos);
                double yLabelOffset = (i == 0) ? -2.0 : 4.0;
                gc.fillText(i + "ft", originX - 32, yPos + yLabelOffset);
            }

            // Render mapped Farm Fields onto metric coordinates
            for (int i = 0; i < fields.size(); i++) {
                FarmField f = fields.get(i);
                double fx = originX + 24 + (i % 2) * (cellSize * 1.6);
                double fy = originY + 24 + (i / 2) * (cellSize * 1.6);
                gc.setFill(Color.web("#065f46", 0.4));
                gc.fillRect(fx, fy, 100, 78);
                gc.setStroke(Color.web("#34d399"));
                gc.setLineWidth(1);
                gc.strokeRect(fx, fy, 100, 78);
                gc.setFill(Color.web("#a7f3d0"));
                gc.fillText(f.getName(), fx + 6, fy + 16);
            }

            // Render mapped Farm Equipment onto metric coordinates
            for (int i = 0; i < equipment.size(); i++) {
                FarmEquipment eq = equipment.get(i);
                double eqx = originX + 36 + (i * 105) % 240;
                double eqy = originY + 275 - (i * 42);
                gc.setFill(Color.web("#b45309", 0.6));
                gc.fillOval(eqx, eqy, 20, 20);
                gc.setStroke(Color.web("#fbbf24"));
                gc.strokeOval(eqx, eqy, 20, 20);
                gc.setFill(Color.web("#fde68a"));
                gc.fillText(eq.getType(), eqx + 24, eqy + 14);
            }

            // Pre-drawn queued path preview (amber dashed line)
            if (plannedPath.size() > 1) {
                gc.setStroke(Color.web("#f59e0b", 0.75));
                gc.setLineWidth(2.0);
                gc.setLineDashes(4, 6);
                for (int i = 0; i < plannedPath.size() - 1; i++) {
                    javafx.geometry.Point2D p1 = plannedPath.get(i);
                    javafx.geometry.Point2D p2 = plannedPath.get(i + 1);
                    gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                }
                gc.setLineDashes(0);
            }

            // Animated crawling dashes for executed flight trail (cyan)
            gc.setStroke(Color.web("#38bdf8"));
            gc.setLineWidth(2.5);
            gc.setLineDashes(6, 6);
            gc.setLineDashOffset(dashOffset[0]);
            for (int i = 0; i < flightTrail.size() - 1; i++) {
                javafx.geometry.Point2D p1 = flightTrail.get(i);
                javafx.geometry.Point2D p2 = flightTrail.get(i + 1);
                gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            }
            gc.setLineDashes(0);
            gc.setLineDashOffset(0);

            // Red directional arrow drone
            gc.setFill(Color.web("#ef4444"));
            Affine transform = new Affine();
            transform.appendTranslation(dronePos[0], dronePos[1]);
            transform.appendRotation(dronePos[2]);
            gc.save();
            gc.transform(transform);
            gc.fillPolygon(new double[]{0, -9, 9}, new double[]{-14, 11, 11}, 3);

            if (isTurning[0] && turnLightState[0]) {
                gc.setFill(Color.web("#f59e0b", 0.95));
                gc.fillOval(-5, -20, 10, 10);
                gc.setStroke(Color.web("#fef3c7"));
                gc.setLineWidth(1.5);
                gc.strokeOval(-7, -22, 14, 14);
            }

            gc.restore();
        };

        redrawGrid.run();

        Timeline dashAnimator = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    dashOffset[0] = (dashOffset[0] - 1.0) % 12.0;
                    redrawGrid.run();
                })
        );
        dashAnimator.setCycleCount(Timeline.INDEFINITE);
        dashAnimator.play();

        record QueuedCommand(String label, Runnable udpAction) {}

        List<QueuedCommand> commandQueue = new ArrayList<>();
        final java.util.concurrent.atomic.AtomicBoolean isFlyingQueue =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean abortQueue =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        TextArea queueArea = new TextArea("Queue: Idle (Build path, then click Takeoff)");
        queueArea.setEditable(false);
        queueArea.setWrapText(true);
        queueArea.setPrefRowCount(7);
        queueArea.setMaxWidth(280);
        queueArea.setStyle(
                "-fx-control-inner-background: #0b1017; "
                + "-fx-text-fill: #38bdf8; "
                + "-fx-font-family: monospace; "
                + "-fx-font-size: 11px; "
                + "-fx-border-color: #2e3b4e; "
                + "-fx-border-radius: 4;"
        );

        Runnable updateQueueDisplay = () -> Platform.runLater(() -> {
            if (commandQueue.isEmpty()) {
                queueArea.setText(isFlyingQueue.get()
                        ? "Queue: Flight complete"
                        : "Queue: Idle (Build path, then click Takeoff)");
            } else {
                List<String> labels = commandQueue.stream()
                        .map(QueuedCommand::label)
                        .toList();
                String prefix = isFlyingQueue.get()
                        ? "EXECUTING QUEUE ("
                        : "PLANNED QUEUE (";
                queueArea.setText(prefix + labels.size() + "):\n• " + String.join("\n• ", labels));
            }
        });

        Runnable rebuildPlannedPath = () -> {
            planCoords[0] = gridCoords[0];
            planCoords[1] = gridCoords[1];
            planCoords[2] = gridCoords[2];

            plannedPath.clear();
            plannedPath.add(new javafx.geometry.Point2D(
                    originX + (planCoords[0] * cellSize),
                    originY + (planCoords[1] * cellSize)));

            for (QueuedCommand cmd : commandQueue) {
                String label = cmd.label();
                int heading = ((planCoords[2] % 360) + 360) % 360;
                int dx = (heading == 90) ? 1 : (heading == 270) ? -1 : 0;
                int dy = (heading == 180) ? 1 : (heading == 0) ? -1 : 0;

                if (label.contains("Fwd") || label.contains("Back")) {
                    int dir = label.contains("Fwd") ? 1 : -1;
                    int nextX = Math.max(0, Math.min(4, planCoords[0] + (dx * dir)));
                    int nextY = Math.max(0, Math.min(4, planCoords[1] + (dy * dir)));
                    planCoords[0] = nextX;
                    planCoords[1] = nextY;
                    plannedPath.add(new javafx.geometry.Point2D(
                            originX + (planCoords[0] * cellSize),
                            originY + (planCoords[1] * cellSize)));
                } else if (label.contains("Left")) {
                    planCoords[2] = (planCoords[2] - 90 + 360) % 360;
                } else if (label.contains("Right")) {
                    planCoords[2] = (planCoords[2] + 90) % 360;
                }
            }
            redrawGrid.run();
        };

        Button clearGridBtn = button("Reset Grid & Position", () -> {
            abortQueue.set(true);
            commandQueue.clear();
            gridCoords[0] = 2;
            gridCoords[1] = 2;
            gridCoords[2] = 0;
            dronePos[0] = originX + (2.0 * cellSize);
            dronePos[1] = originY + (2.0 * cellSize);
            dronePos[2] = 0.0;
            flightTrail.clear();
            flightTrail.add(new javafx.geometry.Point2D(dronePos[0], dronePos[1]));
            rebuildPlannedPath.run();
            updateQueueDisplay.run();
            statusLabel.setText("Grid and queue reset.");
        });

        Button clearQueueBtn = button("Clear Queue", () -> {
            abortQueue.set(true);
            commandQueue.clear();
            rebuildPlannedPath.run();
            updateQueueDisplay.run();
            statusLabel.setText("Planned queue cleared.");
        });

        HBox gridControls = new HBox(8, clearGridBtn, clearQueueBtn);
        gridControls.setAlignment(javafx.geometry.Pos.CENTER);
        VBox gridPane = new VBox(6, gridLabel, pathCanvas, gridControls);
        gridPane.setAlignment(javafx.geometry.Pos.CENTER);
        gridPane.setStyle("-fx-background-color: #1a2332; -fx-padding: 10; -fx-background-radius: 6;");

        // --- INSTANT D-PAD & EXPANDED QUEUE BOX ---
        Label manualLabel = text("Controller D-Pad");
        manualLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c9d5df;");

        Timeline turnBlinker = new Timeline(
                new KeyFrame(Duration.millis(200), e -> {
                    turnLightState[0] = !turnLightState[0];
                    redrawGrid.run();
                })
        );
        turnBlinker.setCycleCount(Timeline.INDEFINITE);

        Timeline turnDurationTimer = new Timeline(
                new KeyFrame(Duration.millis(800), e -> {
                    turnBlinker.stop();
                    isTurning[0] = false;
                    redrawGrid.run();
                })
        );

        java.util.function.BiConsumer<Runnable, Runnable> animateMovement = (onUpdate, onFinished) -> {
            Timeline flightAnimator = new Timeline(
                    new KeyFrame(Duration.millis(16), e -> {
                        onUpdate.run();
                        redrawGrid.run();
                    })
            );
            flightAnimator.setCycleCount(20);
            flightAnimator.setOnFinished(e -> {
                if (onFinished != null) onFinished.run();
            });
            flightAnimator.play();
        };

        java.util.function.BiConsumer<String, Runnable> enqueueCommand = (label, udpAction) -> {
            commandQueue.add(new QueuedCommand(label, udpAction));
            rebuildPlannedPath.run();
            updateQueueDisplay.run();
            statusLabel.setText("Queued: " + label + " (Click Takeoff to fly queue)");
        };

        java.util.function.BiConsumer<QueuedCommand, Runnable> runSingleQueuedStep = (cmd, onStepComplete) -> {
            String label = cmd.label();
            Runnable udpAction = cmd.udpAction();

            Thread worker = new Thread(() -> {
                if (abortQueue.get()) {
                    Platform.runLater(onStepComplete);
                    return;
                }

                Platform.runLater(() -> statusLabel.setText("Tello: Running " + label + "..."));
                java.util.concurrent.CountDownLatch animLatch = new java.util.concurrent.CountDownLatch(1);

                Platform.runLater(() -> {
                    int heading = ((gridCoords[2] % 360) + 360) % 360;
                    int dx = (heading == 90) ? 1 : (heading == 270) ? -1 : 0;
                    int dy = (heading == 180) ? 1 : (heading == 0) ? -1 : 0;

                    if (label.contains("Fwd") || label.contains("Back")) {
                        int dir = label.contains("Fwd") ? 1 : -1;
                        int nextX = Math.max(0, Math.min(4, gridCoords[0] + (dx * dir)));
                        int nextY = Math.max(0, Math.min(4, gridCoords[1] + (dy * dir)));

                        gridCoords[0] = nextX;
                        gridCoords[1] = nextY;
                        double targetX = originX + (gridCoords[0] * cellSize);
                        double targetY = originY + (gridCoords[1] * cellSize);
                        double stepX = (targetX - dronePos[0]) / 20.0;
                        double stepY = (targetY - dronePos[1]) / 20.0;

                        animateMovement.accept(() -> {
                            dronePos[0] += stepX;
                            dronePos[1] += stepY;
                            flightTrail.add(new javafx.geometry.Point2D(dronePos[0], dronePos[1]));
                        }, () -> {
                            dronePos[0] = targetX;
                            dronePos[1] = targetY;
                            flightTrail.add(new javafx.geometry.Point2D(targetX, targetY));
                            redrawGrid.run();
                            animLatch.countDown();
                        });
                    } else if (label.contains("Left") || label.contains("Right")) {
                        int delta = label.contains("Right") ? 90 : -90;
                        int targetAngle = (gridCoords[2] + delta + 360) % 360;
                        gridCoords[2] = targetAngle;
                        double angleStep = delta / 20.0;

                        isTurning[0] = true;
                        turnBlinker.playFromStart();
                        turnDurationTimer.playFromStart();

                        animateMovement.accept(() -> {
                            dronePos[2] = (dronePos[2] + angleStep + 360) % 360;
                        }, () -> {
                            dronePos[2] = targetAngle;
                            redrawGrid.run();
                            animLatch.countDown();
                        });
                    } else {
                        animLatch.countDown();
                    }
                });

                try {
                    udpAction.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                try {
                    animLatch.await();
                } catch (InterruptedException ignored) {}

                Platform.runLater(onStepComplete);
            });
            worker.setDaemon(true);
            worker.start();
        };

        final Runnable[] runNextInQueue = new Runnable[1];
        runNextInQueue[0] = () -> {
            if (abortQueue.get() || commandQueue.isEmpty()) {
                isFlyingQueue.set(false);
                updateQueueDisplay.run();
                if (!abortQueue.get()) {
                    statusLabel.setText("Tello: Takeoff & queued flight complete.");
                }
                return;
            }

            QueuedCommand nextCmd = commandQueue.remove(0);
            updateQueueDisplay.run();

            runSingleQueuedStep.accept(nextCmd, () -> {
                if (!commandQueue.isEmpty() && !abortQueue.get()) {
                    runNextInQueue[0].run();
                } else {
                    isFlyingQueue.set(false);
                    updateQueueDisplay.run();
                    statusLabel.setText("Tello: Queued flight complete.");
                }
            });
        };

        takeoffBtn.setOnAction(event -> {
            abortQueue.set(false);
            isFlyingQueue.set(true);
            updateQueueDisplay.run();
            statusLabel.setText("Tello: Taking off before starting queue...");

            Thread takeoffThread = new Thread(() -> {
                try {
                    tello.takeoff();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                Platform.runLater(() -> {
                    if (!abortQueue.get() && !commandQueue.isEmpty()) {
                        runNextInQueue[0].run();
                    } else {
                        isFlyingQueue.set(false);
                        updateQueueDisplay.run();
                        statusLabel.setText("Tello: Takeoff finished.");
                    }
                });
            });
            takeoffThread.setDaemon(true);
            takeoffThread.start();
        });

        Button fwdBtn = new Button("▲\nFwd");
        Button backBtn = new Button("▼\nBack");
        Button leftBtn = new Button("↺\nLeft");
        Button rightBtn = new Button("↻\nRight");

        for (Button b : List.of(fwdBtn, backBtn, leftBtn, rightBtn)) {
            b.setMinWidth(72);
            b.setPrefWidth(72);
            b.setMinHeight(52);
            b.setPrefHeight(52);
            b.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");
        }

        fwdBtn.setOnAction(e -> enqueueCommand.accept("Fwd 1ft", () -> tello.moveForward(30)));
        backBtn.setOnAction(e -> enqueueCommand.accept("Back 1ft", () -> tello.sendCommand("back 30")));
        leftBtn.setOnAction(e -> enqueueCommand.accept("Left 90°", () -> tello.sendCommand("ccw 90")));
        rightBtn.setOnAction(e -> enqueueCommand.accept("Right 90°", () -> tello.rotateClockwise(90)));

        GridPane dpadGrid = new GridPane();
        dpadGrid.setHgap(6);
        dpadGrid.setVgap(6);
        dpadGrid.setAlignment(javafx.geometry.Pos.CENTER);
        dpadGrid.add(fwdBtn, 1, 0);
        dpadGrid.add(leftBtn, 0, 1);
        dpadGrid.add(rightBtn, 2, 1);
        dpadGrid.add(backBtn, 1, 2);

        Button upBtn = new Button("▲ Up 20cm");
        Button downBtn = new Button("▼ Down 20cm");
        for (Button b : List.of(upBtn, downBtn)) {
            b.setMinWidth(120);
            b.setPrefWidth(120);
            b.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        }

        upBtn.setOnAction(e -> enqueueCommand.accept("Up 20cm", () -> tello.sendCommand("up 20")));
        downBtn.setOnAction(e -> enqueueCommand.accept("Down 20cm", () -> tello.sendCommand("down 20")));

        HBox altRow = new HBox(8, upBtn, downBtn);
        altRow.setAlignment(javafx.geometry.Pos.CENTER);

        VBox manualPad = new VBox(12, manualLabel, dpadGrid, altRow, queueArea);
        manualPad.setStyle("-fx-background-color: #1a2332; -fx-padding: 14; -fx-background-radius: 6;");
        manualPad.setMinWidth(290);

        HBox interactivePanels = new HBox(18, manualPad, gridPane);
        VBox droneLayout = new VBox(12, coreActions, interactivePanels, table);
        droneLayout.setPadding(new Insets(10));

        page("Drones",
                "Controller, queue, grid map with farm objects, and flight tracking.",
                droneLayout);
    }

    @FXML
    private void showMissions() {
        if (!ready()) return;

        TableView<DroneMission> table = table(missions);

        column(table, "ID", DroneMission::getId);
        column(table, "Drone", DroneMission::getDroneId);
        column(table, "Field", DroneMission::getFieldId);
        column(table, "Type", DroneMission::getMissionType);
        column(table, "Scheduled", mission -> date(mission.getScheduledTime()));
        column(table, "Status", DroneMission::getStatus);
        column(table, "Completed", mission -> date(mission.getCompletedTime()));

        Button schedule = button("Schedule Mission", this::scheduleMission);

        Button advance = button("Advance Selected Status", () -> {
            DroneMission mission = selected(table);
            if (mission == null) return;

            String next;

            if ("SCHEDULED".equals(mission.getStatus())) {
                next = "IN_PROGRESS";
            } else if ("IN_PROGRESS".equals(mission.getStatus())) {
                next = "COMPLETED";
            } else {
                info("No next status",
                        "Only SCHEDULED → IN_PROGRESS → COMPLETED "
                        + "is supported.");
                return;
            }

            if (!confirm("Change saved mission status",
                    mission.getId() + ": " + mission.getStatus()
                    + " → " + next
                    + "\nThis changes a record only. It does not fly a drone.")) {
                return;
            }

            try {
                if (!service.updateMissionStatus(mission.getId(), next)) {
                    throw new IllegalArgumentException(
                            "The mission changed or the transition is invalid.");
                }

                refreshAfterSave(this::showMissions,
                        "Mission status saved. No flight was performed.");
            } catch (IOException | IllegalArgumentException ex) {
                error(ex);
            }
        });

        page("Drone Missions",
                "Saved mission planning only. This screen does not implement "
                        + "flight animation or physical flight control.",
                new HBox(10, schedule, advance), table);
    }

    private void scheduleMission() {
        List<String> available = drones.stream()
                .filter(drone ->
                        "AVAILABLE".equalsIgnoreCase(drone.getStatus()))
                .map(Drone::getId)
                .toList();

        if (available.isEmpty() || fields.isEmpty()) {
            info("Cannot schedule",
                    "At least one AVAILABLE drone and one field are required.");
            return;
        }

        try {
            String droneId = choose("Choose Drone", available, available.get(0));
            if (droneId == null) return;

            List<String> fieldIds = fields.stream()
                    .map(FarmField::getId).toList();

            String fieldId = choose("Choose Field", fieldIds, fieldIds.get(0));
            if (fieldId == null) return;

            String type = choose("Mission Type",
                    List.of("SOIL_MONITORING", "CROP_MONITORING", "SURVEY"),
                    "SOIL_MONITORING");
            if (type == null) return;

            String time = prompt("Scheduled Time",
                    "Use yyyy-MM-ddTHH:mm, for example 2026-11-16T09:00",
                    LocalDateTime.now().plusHours(1)
                            .withSecond(0).withNano(0).toString());
            if (time == null) return;

            LocalDateTime scheduled;

            try {
                scheduled = LocalDateTime.parse(time);
            } catch (java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException(
                        "Invalid date/time. Use yyyy-MM-ddTHH:mm.");
            }

            if (!scheduled.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "Choose a scheduled time in the future.");
            }

            DroneMission mission = new DroneMission(
                    "M-" + UUID.randomUUID(),
                    droneId,
                    fieldId,
                    type,
                    scheduled,
                    "SCHEDULED",
                    null);

            if (!confirm("Save Mission",
                    "Drone: " + droneId
                    + "\nField: " + fieldId
                    + "\nType: " + type
                    + "\nScheduled: " + date(scheduled)
                    + "\n\nThis saves a plan only; it does not reserve "
                    + "the drone or detect scheduling conflicts.")) {
                return;
            }

            if (!service.scheduleDroneMission(mission)) {
                throw new IllegalArgumentException(
                        "Mission could not be saved. "
                        + "Reload data and check drone availability.");
            }

            refreshAfterSave(this::showMissions, "Mission plan saved.");
        } catch (IOException | IllegalArgumentException ex) {
            error(ex);
        }
    }

    @FXML
    private void showAlerts() {
        if (!ready()) return;

        TableView<FarmAlert> table = table(alerts);

        column(table, "ID", FarmAlert::getId);
        column(table, "Field", FarmAlert::getFieldId);
        column(table, "Severity", FarmAlert::getSeverity);
        column(table, "Message", FarmAlert::getMessage);
        column(table, "Recorded", alert -> date(alert.getTimestamp()));
        column(table, "Read", alert -> alert.isRead() ? "Yes" : "No");

        Button markRead = button("Mark Selected as Read", () -> {
            FarmAlert alert = selected(table);
            if (alert == null) return;

            try {
                if (!service.markAlertAsRead(alert.getId())) {
                    throw new IllegalArgumentException("Alert was not found.");
                }

                refreshAfterSave(this::showAlerts, "Alert marked as read.");
            } catch (IOException | IllegalArgumentException ex) {
                error(ex);
            }
        });

        page("Alerts",
                "Saved alert history. Old alerts do not establish "
                        + "current farm conditions.",
                markRead, table);
    }

    @FXML
    private void showLivestock() {
        page("Livestock",
                "No livestock records exist in the supplied data model.",
                text("Livestock search needs livestock items from the "
                        + "team's Composite farm model. "
                        + "This screen does not invent animal locations."));
    }

    @FXML
    private void showIrrigation() {
        page("Irrigation / Water Zones",
                "No water-zone model or irrigation-control service is present.",
                text("The saved equipment includes an irrigation pump, "
                        + "but changing its record would not operate it."),
                button("View Equipment Records", this::showEquipment));
    }

    private void runTelloAsync(String actionName, Runnable flightTask) {
        statusLabel.setText("Tello: Running " + actionName + "...");
        Thread t = new Thread(() -> {
            flightTask.run();
            Platform.runLater(() ->
                statusLabel.setText("Tello: Finished " + actionName));
        });
        t.setDaemon(true);
        t.start();
    }

    private List<MonitoringReading> readingsFor(String fieldId) {
        return readings.stream()
                .filter(reading -> fieldId.equals(reading.getFieldId()))
                .filter(reading -> reading.getTimestamp() != null)
                .toList();
    }

    private MonitoringReading latestFor(String fieldId) {
        List<MonitoringReading> matching = readingsFor(fieldId);
        return matching.isEmpty() ? null : matching.get(matching.size() - 1);
    }

    private void refreshAfterSave(Runnable view, String message) {
        reloadData();

        if (loaded) {
            view.run();
            statusLabel.setText(message);
        }
    }

    private void page(String title, String description, Node... nodes) {
        Label heading = text(title);
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox box = new VBox(14, heading, text(description));
        box.setPadding(new Insets(6));

        for (Node node : nodes) {
            box.getChildren().add(node);

            if (node instanceof TableView<?> || node instanceof LineChart<?, ?>) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }

        contentPane.setCenter(box);
    }

    private Label text(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        return label;
    }

    private Button button(String title, Runnable action) {
        Button button = new Button(title);
        button.setOnAction(event -> action.run());
        return button;
    }

    private <T> TableView<T> table(List<T> items) {
        TableView<T> table =
                new TableView<>(FXCollections.observableArrayList(items));
        table.setPlaceholder(new Label("No saved records."));
        table.setPrefHeight(480);
        return table;
    }

    private <T> void column(TableView<T> table, String title,
                            Function<T, ?> getter) {
        TableColumn<T, String> column = new TableColumn<>(title);

        column.setCellValueFactory(cell -> {
            Object value = getter.apply(cell.getValue());
            return new ReadOnlyStringWrapper(
                    value == null ? "—" : value.toString());
        });

        column.setPrefWidth("Message".equals(title) ? 330 : 155);
        table.getColumns().add(column);
    }

    private <T> T selected(TableView<T> table) {
        T item = table.getSelectionModel().getSelectedItem();

        if (item == null) {
            info("Select a record", "Click a table row first.");
        }

        return item;
    }

    private String prompt(String title, String message, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(message);

        return dialog.showAndWait()
                .map(String::trim)
                .orElse(null);
    }

    private String choose(String title, List<String> options, String initial) {
        String defaultValue = options.contains(initial)
                ? initial : options.get(0);

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(defaultValue, options);

        dialog.setTitle(title);
        dialog.setHeaderText(title);

        return dialog.showAndWait().orElse(null);
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, message,
                ButtonType.OK, ButtonType.CANCEL);

        alert.setTitle(title);
        alert.setHeaderText(title);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void error(Exception ex) {
        ex.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operation failed");
        alert.setHeaderText("The operation could not be completed");
        alert.setContentText(
                ex.getMessage() == null
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage());
        alert.showAndWait();
    }

    private String date(LocalDateTime value) {
        return value == null ? "Not recorded" : value.format(DISPLAY);
    }
}
