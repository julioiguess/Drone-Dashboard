package services;

import java.io.IOException;
import java.util.List;

import models.Drone;
import models.FarmField;
import models.FarmEquipment;


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

        } catch (IOException e) {
            System.err.println("Error loading farm data:");
            e.printStackTrace();
        }
    }
}
