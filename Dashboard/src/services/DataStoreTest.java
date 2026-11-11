package services;

import java.io.IOException;

import java.util.List;

import models.Drone;
import models.FarmField;
import models.FarmEquipment;
import models.DroneMission;
import java.time.LocalDateTime;
import models.FarmAlert;
import models.MonitoringReading;

// Edited by Samuel Culp
// Tests JSON data loading and farm monitoring functionality.

public class DataStoreTest {

    public static void main(String[] args) {

        JsonDataStore store = new JsonDataStore();
        FarmDataService service = new FarmDataService();

        try {

            // =====================================
            // TEST 1: LOAD FARM FIELDS
            // =====================================

            List<FarmField> fields =
                    store.loadList("fields.json", FarmField.class);

            System.out.println("=== FARM FIELDS ===");

            for (FarmField field : fields) {
                System.out.println(
                        field.getName() + " - " + field.getCropType()
                );
            }

            // =====================================
            // TEST 2: LOAD DRONES
            // =====================================

            List<Drone> drones =
                    store.loadList("drones.json", Drone.class);

            System.out.println("\n=== DRONES ===");

            for (Drone drone : drones) {
                System.out.println(
                        drone.getName() + " - " +
                        drone.getBatteryLevel() + "% - " +
                        drone.getStatus()
                );
            }

            // =====================================
            // TEST 3: MONITOR CROP GROWTH
            // =====================================

            System.out.println("\n=== CROP GROWTH ===");

            double growth = service.getLatestCropGrowth("F001");

            if (growth == -1) {
                System.out.println("No crop growth data available.");
            } else {
                System.out.println("North Field Growth: " + growth + "%");
            }
            
            System.out.println("\n=== HARVEST READINESS ===");

            double readiness = service.getLatestHarvestReadiness("F001");

            if (readiness == -1) {
                System.out.println("No harvest readiness data available.");
            } else {
                System.out.println("North Field Harvest Readiness: "
                        + readiness + "%");
            }
            
            System.out.println("\n=== HARVEST DECISION ===");

            boolean ready = service.isFieldReadyToHarvest("F001");

            if (ready) {
                System.out.println("North Field is READY to harvest!");
            } else {
                System.out.println("North Field is NOT ready to harvest.");
            }
            
            System.out.println("\n=== FARM EQUIPMENT ===");

            for (FarmEquipment equipment : service.getEquipment()) {
                System.out.println(
                    equipment.getName() + " - " +
                    equipment.getStatus() + " - " +
                    equipment.getLatitude() + ", " +
                    equipment.getLongitude()
                );
            }
            
            System.out.println("\n=== UPDATE EQUIPMENT LOCATION ===");

            boolean updated = service.updateEquipmentLocation(
                    "E001",
                    33.5300,
                    -86.8150
            );

            if (updated) {
                System.out.println("Equipment location updated successfully!");
            } else {
                System.out.println("Equipment not found.");
            }
            
            System.out.println("\n=== SCHEDULE DRONE MISSION ===");

            DroneMission mission = new DroneMission(
                    "M002",
                    "D002",
                    "F002",
                    "SOIL_MONITORING",
                    LocalDateTime.of(2026, 11, 12, 9, 0),
                    "SCHEDULED",
                    null
            );

            boolean scheduled = service.scheduleDroneMission(mission);

            if (scheduled) {
                System.out.println("Drone mission scheduled successfully!");
            } else {
                System.out.println("Mission could not be scheduled.");
            }
            
            System.out.println("\n=== UPDATE MISSION STATUS ===");

            boolean started = service.updateMissionStatus(
                "M001", "IN_PROGRESS"
            );

            System.out.println("Mission started: " + started);

            boolean completed = service.updateMissionStatus(
                "M001", "COMPLETED"
            );

            System.out.println("Mission completed: " + completed);
            

			System.out.println("\n=== VIEW DRONE STATUS ===");
			
			Drone drone = service.getDroneById("D001");
			
			if (drone != null) {
			
			    System.out.println("Drone: " + drone.getName());
			    System.out.println("Battery: " + drone.getBatteryLevel() + "%");
			    System.out.println("Status: " + drone.getStatus());
			
			} else {
			    System.out.println("Drone not found.");
			}
			
			System.out.println("\n=== UPDATE DRONE STATUS ===");

			boolean updatedDrone = service.updateDroneStatus(
			    "D002", "AVAILABLE", 100
			);

			System.out.println("Drone updated: " + updatedDrone);

			Drone updatedDroneInfo = service.getDroneById("D002");

			if (updatedDroneInfo != null) {
			    System.out.println("Drone: " + updatedDroneInfo.getName());
			    System.out.println("Battery: " + updatedDroneInfo.getBatteryLevel() + "%");
			    System.out.println("Status: " + updatedDroneInfo.getStatus());
			}
			
			System.out.println("\n=== CREATE FARM ALERT ===");

			FarmAlert alert = new FarmAlert(
			    "A002",
			    "F002",
			    "D002",
			    "LOW_SOIL_MOISTURE",
			    "North Field soil moisture is too low!",
			    "HIGH",
			    LocalDateTime.now(),
			    false
			);

			boolean created = service.createFarmAlert(alert);

			if (created) {
			    System.out.println("Farm alert created successfully!");
			} else {
			    System.out.println("Alert ID already exists.");
			}
			
			System.out.println("\n=== UNREAD FARM ALERTS ===");

			List<FarmAlert> unreadAlerts = service.getUnreadAlerts();

			for (FarmAlert alert1 : unreadAlerts) {
			    System.out.println(
			        alert1.getId() + " - " +
			        alert1.getMessage() + " - " +
			        alert1.getSeverity()
			    );
			}
			
			System.out.println("\n=== MARK ALERT AS READ ===");

			boolean marked = service.markAlertAsRead("A001");

			System.out.println("Alert marked as read: " + marked);

			System.out.println("\n=== REMAINING UNREAD ALERTS ===");

			List<FarmAlert> remainingAlerts = service.getUnreadAlerts();

			for (FarmAlert alert1 : remainingAlerts) {
			    System.out.println(
			        alert1.getId() + " - " +
			        alert1.getMessage()
			    );
			}

			
			System.out.println("\n=== AUTOMATIC SOIL MOISTURE ALERT ===");

			boolean alertCreated =
			    service.checkSoilMoistureAlert("F001");

			System.out.println("Automatic alert created: " + alertCreated);
			
			
			System.out.println("\n=== SOIL MOISTURE DEBUG ===");

			for (MonitoringReading reading : service.getReadings()) {
			    if (reading.getFieldId().equals("F001")) {
			        System.out.println(
			            reading.getId() + " - " +
			            reading.getTimestamp() + " - " +
			            reading.getSoilMoisturePercent() + "%"
			        );
			    }
			}
			
			System.out.println("\n=== FARM MAP FIELD LOCATION ===");

			FarmField mapField = service.getFieldById("F001");

			if (mapField != null) {

			    System.out.println("Field: " + mapField.getName());
			    System.out.println("Crop: " + mapField.getCropType());
			    System.out.println("Area: " + mapField.getAreaAcres() + " acres");
			    System.out.println("Latitude: " + mapField.getLatitude());
			    System.out.println("Longitude: " + mapField.getLongitude());

			} else {
			    System.out.println("Field not found.");
			}
			
			for (FarmField field : service.getFields()) {

			    System.out.println(
			        field.getName() +
			        " | Latitude: " + field.getLatitude() +
			        " | Longitude: " + field.getLongitude()
			    );
			}

			// Display all equipment locations
			System.out.println("\n--- FARM EQUIPMENT ---");

			for (FarmEquipment equipment : service.getEquipment()) {

			    System.out.println(
			        equipment.getName() +
			        " | Latitude: " + equipment.getLatitude() +
			        " | Longitude: " + equipment.getLongitude()
			    );
			}

        } catch (IOException e) {
            System.err.println("Error loading farm data:");
            e.printStackTrace();
        }
    }
}
