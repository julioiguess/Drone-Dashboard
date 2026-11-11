Samuel Culp


FarmDataServices File

Purpose: this service class acts as the central data access layer for the monitoring system. It retrieves, updates, validates, and saves info stored in JSON files through the JsonDataStore. 

Purpose

This service class acts as the central data access layer for the Farm Drone Management System. It retrieves, updates, validates, and saves information stored in JSON files through JsonDataStore.

How It Works

JsonDataStore handles the actual reading and writing of JSON files.

FarmDataService provides business logic and data validation before information is saved or returned to the application.

The class loads data from the following JSON sources:

fields.json → Farm field information

drones.json → Drone inventory and status

readings.json → Sensor and monitoring readings

equipment.json → Farm equipment information

missions.json → Drone mission records

alerts.json → Farm alerts and notifications

Responsibilities

1. Field Management

Retrieves farm field records.

Finds fields by ID.

Retrieves the latest crop growth and harvest readiness percentages.

Determines whether a field meets the simulated harvest readiness threshold of 80%.

2. Equipment Management

Loads equipment information.

Updates equipment GPS coordinates.

Records the last detected timestamp.

Saves equipment updates back to JSON.

3. Drone Management

Loads drone information.

Finds drones by ID.

Updates drone status and battery levels.

Validates battery ranges before saving.

4. Drone Mission Management

Loads scheduled missions.

Creates and schedules new drone missions.

Prevents duplicate mission IDs.

Verifies that a drone exists and is available before assignment.

Updates mission status through approved transitions:

SCHEDULED → IN_PROGRESS → COMPLETED

Records mission completion timestamps.

5. Alert Management

Creates new farm alerts.

Prevents duplicate alert IDs.

Retrieves unread alerts.

Marks alerts as read.

Saves alert changes back to JSON.

6. Automated Monitoring

Examines the latest monitoring reading for a specified field.

Detects low soil moisture conditions.

Creates HIGH severity alerts when soil moisture falls below the simulated 30% threshold.

Prevents duplicate alerts for the same monitoring reading.

Methods and How to Use Them

getFields()

Purpose: Retrieves all farm fields.

Parameters: None.

Returns: List<FarmField>

Example: service.getFields()

getFieldById(String fieldId)

Purpose: Retrieves a specific farm field using its ID.

Parameters: Field ID, such as "F001".

Returns: FarmField, or null if not found.

Example: service.getFieldById("F001")

getLatestCropGrowth(String fieldId)

Purpose: Retrieves the latest recorded crop growth percentage for a field.

Parameters: Field ID.

Returns: double, or -1 when no reading exists.

Example: service.getLatestCropGrowth("F001")

isFieldReadyToHarvest(String fieldId)

Purpose: Checks whether a field's latest harvest readiness percentage is at least 80%.

Parameters: Field ID.

Returns: boolean.

Example: service.isFieldReadyToHarvest("F001")

updateEquipmentLocation(String equipmentId, double latitude, double longitude)

Purpose: Updates equipment GPS coordinates and the last detected timestamp.

Parameters: Equipment ID, latitude, and longitude.

Returns: boolean.

Example: service.updateEquipmentLocation("E001", 33.53, -86.815)

getDroneById(String droneId)

Purpose: Finds a drone by ID.

Parameters: Drone ID.

Returns: Drone, or null if not found.

Example: service.getDroneById("D001")

updateDroneStatus(String droneId, String newStatus, int newBatteryLevel)

Purpose: Updates a drone's status and battery level.

Parameters: Drone ID, status, and battery percentage.

Returns: boolean.

Example: service.updateDroneStatus("D002", "AVAILABLE", 100)

scheduleDroneMission(DroneMission mission)

Purpose: Creates and saves a mission when its ID is unique and its assigned drone is available.

Parameters: A DroneMission object.

Returns: boolean.

Example: service.scheduleDroneMission(mission)

updateMissionStatus(String missionId, String newStatus)

Purpose: Updates a mission through the permitted status transitions.

Parameters: Mission ID and new status.

Returns: boolean.

Example: service.updateMissionStatus("M001", "IN_PROGRESS")

getUnreadAlerts()

Purpose: Retrieves all unread farm alerts.

Parameters: None.

Returns: List<FarmAlert>.

Example: service.getUnreadAlerts()

markAlertAsRead(String alertId)

Purpose: Marks an alert as read and saves the change.

Parameters: Alert ID.

Returns: boolean.

Example: service.markAlertAsRead("A001")

checkSoilMoistureAlert(String fieldId)

Purpose: Checks the latest soil moisture reading and creates an alert when it falls below 30%.

Parameters: Field ID.

Returns: boolean indicating whether a new alert was created.

Example: service.checkSoilMoistureAlert("F001")

The remaining data-loading and alert-creation methods can be documented using the same format.

Data Flow

JSON Files ↔ JsonDataStore ↔ FarmDataService ↔ Controllers/UI



import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// Backend service
import services.FarmDataService;

// Data models
import models.FarmField;
import models.Drone;
import models.MonitoringReading;
import models.FarmEquipment;
import models.DroneMission;
import models.FarmAlert;

// Common JavaFX UI controls
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

