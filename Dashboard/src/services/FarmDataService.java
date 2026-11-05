// edited by Samuel Culp 

//This file access all farm related data stored in the json files through JsonDataStore

package services;

import java.io.IOException;
import java.util.List;

// From models folder
import models.Drone;
import models.DroneMission;
import models.FarmAlert;
import models.FarmEquipment;
import models.FarmField;
import models.MonitoringReading;

public class FarmDataService {
	
	private final JsonDataStore store;
	
	public FarmDataService() {
		store = new JsonDataStore();
	}
	
	// load farm fields
	public List<FarmField> getFields() throws IOException {
		return store.loadList("fields.json", FarmField.class);
	}
	
    // Load drones
    public List<Drone> getDrones() throws IOException {
        return store.loadList("drones.json", Drone.class);
    }

    // Load monitoring readings
    public List<MonitoringReading> getReadings() throws IOException {
        return store.loadList("readings.json", MonitoringReading.class);
    }

    // Load farm equipment
    public List<FarmEquipment> getEquipment() throws IOException {
        return store.loadList("equipment.json", FarmEquipment.class);
    }

    // load drone missions
    public List<DroneMission> getMissions() throws IOException {
        return store.loadList("missions.json", DroneMission.class);
    }

    // Load farm alerts
    public List<FarmAlert> getAlerts() throws IOException {
        return store.loadList("alerts.json", FarmAlert.class);
    }
	
}
