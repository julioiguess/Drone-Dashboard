package controllers;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import models.FarmField;
import models.MonitoringReading;
import services.JsonDataStore;

/**
 * Explicitly simulated monitoring controls.
 * Preview does not modify saved readings or operate physical hardware.
 */
public class DemoReadingsPanel extends VBox {

    private final List<FarmField> fields;
    private final ComboBox<String> selection;
    private final Supplier<List<MonitoringReading>> readings;
    private final Consumer<MonitoringReading> preview;
    private final Runnable cancel;
    private final Runnable saved;

    private final Slider moisture = slider();
    private final Slider growth = slider();
    private final Slider harvest = slider();

    private final ComboBox<String> health = new ComboBox<>();
    private final Label message = new Label(
            "Move a slider to preview simulated values. Nothing is saved yet.");

    private boolean syncing;

    public DemoReadingsPanel(
            List<FarmField> fields,
            ComboBox<String> selection,
            Supplier<List<MonitoringReading>> readings,
            Consumer<MonitoringReading> preview,
            Runnable cancel,
            Runnable saved) {

        super(10);

        this.fields = fields;
        this.selection = selection;
        this.readings = readings;
        this.preview = preview;
        this.cancel = cancel;
        this.saved = saved;

        getStyleClass().add("panel");
        setStyle(
                "-fx-background-color: #1a2332;"
                + "-fx-padding: 16;"
                + "-fx-background-radius: 8;");

        Label title = new Label("DEMO DATA / SIMULATION");
        title.setStyle(
                "-fx-text-fill: #38bdf8;"
                + "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;");

        Label explanation = new Label(
                "Preview a new sample for the selected monitoring field. "
                + "This is not live sensor data. "
                + "Only Save Sample or Generate History changes the JSON file.");
        explanation.setWrapText(true);

        health.getItems().addAll("HEALTHY", "STRESSED", "UNKNOWN");
        health.setValue("UNKNOWN");

        moisture.valueProperty().addListener((o, a, b) -> publish());
        growth.valueProperty().addListener((o, a, b) -> publish());
        harvest.valueProperty().addListener((o, a, b) -> publish());
        health.setOnAction(event -> publish());

        Button save = new Button("Save Sample");
        save.setOnAction(event -> saveSample());

        Button generate = new Button("Generate Demo History");
        generate.setOnAction(event -> generateHistory());

        Button reset = new Button("Cancel Preview");
        reset.setOnAction(event -> {
            cancel.run();
            loadSelected();
            message.setText(
                    "Preview canceled. Saved values restored; no file changed.");
        });

        FlowPane actions = new FlowPane(10, 8);
        actions.getChildren().addAll(save, generate, reset);

        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #c7a96b;");

        getChildren().addAll(
                title,
                explanation,
                row("Soil moisture", moisture),
                row("Crop growth", growth),
                row("Harvest readiness", harvest),
                new Label("Simulated crop health"),
                health,
                actions,
                message);

        setDisable(fields.isEmpty());
    }

    private static Slider slider() {
        Slider value = new Slider(0, 100, 50);
        value.setShowTickLabels(true);
        value.setShowTickMarks(true);
        value.setMajorTickUnit(25);
        value.setBlockIncrement(1);
        return value;
    }

    private VBox row(String title, Slider slider) {
        Label label = new Label();

        label.textProperty().bind(
                javafx.beans.binding.Bindings.format(
                        title + ": %.0f%%", slider.valueProperty()));

        return new VBox(5, label, slider);
    }

    private FarmField selectedField() {
        int index = selection.getSelectionModel().getSelectedIndex();
        return index < 0 || index >= fields.size()
                ? null : fields.get(index);
    }

    public void loadSelected() {
        FarmField field = selectedField();
        if (field == null) return;

        MonitoringReading latest = readings.get().stream()
                .filter(r -> field.getId().equals(r.getFieldId()))
                .filter(r -> r.getTimestamp() != null)
                .max(Comparator.comparing(MonitoringReading::getTimestamp))
                .orElse(null);

        syncing = true;

        moisture.setValue(latest == null
                ? 50 : latest.getSoilMoisturePercent());

        growth.setValue(latest == null
                ? 50 : latest.getCropGrowthPercent());

        harvest.setValue(latest == null
                ? 50 : latest.getHarvestReadinessPercent());

        String state = latest == null
                ? "UNKNOWN" : latest.getCropHealthStatus();

        health.setValue(health.getItems().contains(state)
                ? state : "UNKNOWN");

        syncing = false;

        message.setText(latest == null
                ? "No saved reading. Sliders start at demo defaults of 50%; "
                    + "those defaults are not measurements."
                : "Loaded saved values for " + field.getName()
                    + ". Move a slider to create an unsaved preview.");
    }

    private LocalDateTime sampleTime() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime latest = readings.get().stream()
                .map(MonitoringReading::getTimestamp)
                .filter(t -> t != null)
                .max(Comparator.naturalOrder())
                .orElse(now);

        return latest.isBefore(now) ? now : latest.plusNanos(1);
    }

    private MonitoringReading sample(String id) {
        FarmField field = selectedField();

        return new MonitoringReading(
                id,
                field.getId(),
                "SIMULATED",
                sampleTime(),
                Math.round(moisture.getValue()),
                Math.round(growth.getValue()),
                Math.round(harvest.getValue()),
                health.getValue());
    }

    private void publish() {
        if (syncing || selectedField() == null) return;

        preview.accept(sample("DEMO-PREVIEW"));
        message.setText(
                "UNSAVED PREVIEW. Graph endpoint and right sidebar "
                + "include this simulated sample. Click Save Sample to keep it.");
    }

    private boolean confirm(String text) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                text,
                ButtonType.OK,
                ButtonType.CANCEL);

        alert.setHeaderText("Save simulated data?");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void saveSample() {
        if (selectedField() == null) return;

        if (!confirm(
                "Append one DEMO reading for "
                + selectedField().getName()
                + " to readings.json?\n"
                + "Existing records will be retained and a backup created.")) {
            return;
        }

        try {
            JsonDataStore store = new JsonDataStore();

            List<MonitoringReading> all = new ArrayList<>(
                    store.loadList("readings.json", MonitoringReading.class));

            all.add(sample("DEMO-" + UUID.randomUUID()));

            writeWithBackup(store, all);
            saved.run();
            loadSelected();

            message.setText(
                    "Demo sample saved. Existing readings retained. "
                    + "A backup was created in the data folder's backups directory.");
        } catch (IOException | RuntimeException ex) {
            failure(ex);
        }
    }

    private void generateHistory() {
        if (!confirm(
                "Add up to 30 simulated daily readings for every field?\n"
                + "Dates cover the previous 30 days.\n"
                + "Existing readings are retained. "
                + "Previously generated matching demo IDs are skipped.")) {
            return;
        }

        try {
            JsonDataStore store = new JsonDataStore();

            List<MonitoringReading> all = new ArrayList<>(
                    store.loadList("readings.json", MonitoringReading.class));

            Set<String> ids = new HashSet<>();
            for (MonitoringReading reading : all) {
                ids.add(reading.getId());
            }

            int added = 0;
            LocalDate today = LocalDate.now();

            for (int f = 0; f < fields.size(); f++) {
                FarmField field = fields.get(f);

                for (int day = 0; day < 30; day++) {
                    LocalDate date = today.minusDays(30 - day);

                    String id = "DEMO-HISTORY-"
                            + field.getId() + "-" + date;

                    if (!ids.add(id)) continue;

                    double wet = Math.round(
                            50 + 21 * Math.sin(day * 0.55 + f * 1.4));

                    double crop = Math.min(100,
                            30 + day * 1.7 + f * 5);

                    double ready = Math.min(100,
                            20 + day * 2.0 + f * 7);

                    all.add(new MonitoringReading(
                            id,
                            field.getId(),
                            "SIMULATED",
                            date.atTime(9, 0),
                            wet,
                            Math.round(crop),
                            Math.round(ready),
                            wet < 35 ? "STRESSED" : "HEALTHY"));

                    added++;
                }
            }

            if (added == 0) {
                message.setText(
                        "No new history added. Matching demo IDs already exist.");
                return;
            }

            writeWithBackup(store, all);
            saved.run();
            loadSelected();

            message.setText(
                    "Added " + added + " simulated readings. "
                    + "Existing readings retained and backed up.");
        } catch (IOException | RuntimeException ex) {
            failure(ex);
        }
    }

    private void writeWithBackup(
            JsonDataStore store,
            List<MonitoringReading> all) throws IOException {

        Path directory = store.getDataDirectory();
        Path original = directory.resolve("readings.json");
        Path backups = directory.resolve("backups");

        Files.createDirectories(backups);

        Files.copy(
                original,
                backups.resolve(
                        "readings-before-demo-"
                        + UUID.randomUUID() + ".json"));

        String temporaryName =
                "demo-readings-" + UUID.randomUUID() + ".json";

        Path temporary = directory.resolve(temporaryName);

        try {
            store.saveList(temporaryName, all);

            try {
                Files.move(
                        temporary,
                        original,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(
                        temporary,
                        original,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void failure(Exception ex) {
        ex.printStackTrace();

        message.setText(
                "Operation failed. Check the error and backup folder "
                + "before retrying.");

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Demo data operation failed");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }
}
