// TEsting 



package services;

import java.io.IOException;
import java.util.List;

import models.FarmField;
import models.Drone;

public class DataStoreTest {

    public static void main(String[] args) {

        JsonDataStore store = new JsonDataStore();

        try {
            // Test loading farm fields
            List<FarmField> fields =
                    store.loadList("fields.json", FarmField.class);

            System.out.println("=== FARM FIELDS ===");

            for (FarmField field : fields) {
                System.out.println(
                        field.getName() + " - " + field.getCropType()
                );
            }

            // Test loading drones
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
