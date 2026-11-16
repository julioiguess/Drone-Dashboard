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

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import models.*;
import services.FarmDataService;
import services.JsonDataStore;

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
    private final drone.TelloController tello = new drone.TelloController(); 


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
        xAxis.setTickLabelFill(javafx.scene.paint.Color.web("#c9d5df"));

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Moisture (%)");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.web("#c9d5df"));

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

        Button edit = button("Edit Selected Drone Record", () -> {
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

        Button patrolBtn = button("Fly Quick Patrol", () ->
            runTelloAsync("Patrol Routine", () -> {
                tello.connectSdk();
                if ("ok".equalsIgnoreCase(tello.takeoff())) {
                    tello.moveForward(50);
                    tello.rotateClockwise(180);
                    tello.moveForward(50);
                    tello.land();
                }
            }));

        Button landBtn = button("Land Now", () ->
            runTelloAsync("Land", tello::land));

        Button emergencyBtn = button("EMERGENCY STOP", () ->
            runTelloAsync("EMERGENCY STOP", tello::emergency));
        emergencyBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox controls = new HBox(10, edit, connectBtn, patrolBtn, landBtn, emergencyBtn);

        page("Drones",
                "Manual record editing and live Tello TLW004 UDP hardware controls.",
                controls, table);
    }

    private void runTelloAsync(String actionName, Runnable flightTask) {
        statusLabel.setText("Tello: Running " + actionName + "...");
        Thread t = new Thread(() -> {
            flightTask.run();
            javafx.application.Platform.runLater(() ->
                statusLabel.setText("Tello: Finished " + actionName));
        });
        t.setDaemon(true);
        t.start();
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