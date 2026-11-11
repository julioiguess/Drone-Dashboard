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
    
    
    public double getLatestCropGrowth(String fieldId) throws IOException {
    	
    	List<MonitoringReading> readings = getReadings();
    	
    	MonitoringReading latest = null;
    	
    	for(MonitoringReading reading : readings) {
    		 if (reading.getFieldId().equals(fieldId)) {
    			 if (latest ==null ||
    			 		reading.getTimestamp().isAfter(latest.getTimestamp())) {
    				 		latest = reading;
    			 }
    		 }
    	}
    	
    	if (latest ==null) {
    		return -1;
    	}
    	return latest.getCropGrowthPercent();
    	
    }
    
    // Get the latest harvest readiness percentage for a field
    public double getLatestHarvestReadiness(String fieldId)
            throws IOException {

        List<MonitoringReading> readings = getReadings();

        MonitoringReading latest = null;

        for (MonitoringReading reading : readings) {
      //beans
            if (reading.getFieldId().equals(fieldId)) {

                if (latest == null ||
                    reading.getTimestamp().isAfter(latest.getTimestamp())) {

                    latest = reading;
                }
            }
        }

        if (latest == null) {
            return -1;
        }

        return latest.getHarvestReadinessPercent();
    }
    
    public boolean isFieldReadyToHarvest(String fieldId) throws IOException { 
    	
    	double readiness = getLatestHarvestReadiness(fieldId);
    	
    	// no data found
    	if (readiness == -1) {
    		return false; 
    	}
    	
    	// Field is ready when readiness is at 80%
    	return readiness >= 80;
    	
    }
    
    // update equipment location and save it to JSON
    public boolean updateEquipmentLocation(
            String equipmentId,
            double latitude,
            double longitude) throws IOException {
    	
    	List<FarmEquipment> equipmentList = getEquipment();
    	
    	for (FarmEquipment equipment : equipmentList) {
    		if (equipment.getId().equals(equipmentId)) {
    			
    			equipment.setLatitude(latitude);
    			equipment.setLongitude(longitude);
    			equipment.setLastDetected(
    				java.time.LocalDateTime.now());
    		
    		store.saveList("equipment.json", equipmentList);
    		
    		return true;
    		
    	}
    	
    }
    
    	return false;
    }
    
    	//method
    // schedule drone mission and save to JSON
    public boolean scheduleDroneMission(DroneMission mission) throws IOException {
    	
    	List<DroneMission> missions = getMissions();
    	
    	// prevent dup missions IDs
    	for (DroneMission existing : missions) {
    		if (existing.getId().equals(mission.getId())) {
    			return false;
    		}
    	}
    	
    	// verify drone exists and is available

		boolean droneAvailable = false;
		
		System.out.println("Requested Drone ID: " + mission.getDroneId());
		
		for (Drone drone : getDrones()) {
		
		    System.out.println("Checking Drone ID: " + drone.getId());
		    System.out.println("Drone Status: " + drone.getStatus());
		
		    if (drone.getId().equals(mission.getDroneId())
		            && "AVAILABLE".equalsIgnoreCase(drone.getStatus())) {
		
		        droneAvailable = true;
		        break;
		    }
		}
		
		System.out.println("Drone Available: " + droneAvailable);

    	
    	//stop if the drone is not available
    	if(!droneAvailable) {
    		return false;
    	}
    	
    	//add the new mission
    	missions.add(mission);
    	//save mission to JSON
    	store.saveList("missions.json", missions);
    	return true;
    	
    }
    
    //method
    public boolean updateMissionStatus(String missionId, String newStatus) throws IOException {
    	
    	List<DroneMission> missions = getMissions();
    	
    	for (DroneMission mission : missions) {
    		if (mission.getId().equals(missionId)) {
    			
    			String currentStatus = mission.getStatus();
    			
    			//allow only valid status
    			boolean validTransition = ("SCHEDULED".equals(currentStatus)
    					&& "IN_PROGRESS".equals(newStatus)) || ("IN_PROGRESS".equals(currentStatus)
    							&& "COMPLETED".equals(newStatus));
    			
    			if (!validTransition) {
    				return false;
    			}
    			
    			mission.setStatus(newStatus);
    			
    			if("COMPLETED".equals(newStatus)) {
    				mission.setCompletedTime(
    						(java.time.LocalDateTime.now()));
    			}
    			store.saveList("missions.json", missions);
    			return true;
    		}
    	}
    	return false;
    }
    		//method 
	    //find drone by ID
	    public Drone getDroneById(String droneId) throws IOException{
	    	List<Drone> drones=getDrones();
	    	
	    	for (Drone drone:drones) {
	    		if (drone.getId().equals(droneId)) {
	    			return drone;
	    		}
	    	}
	    	// no match return null
	    	return null;
	    }
	    public boolean updateDroneStatus(String droneId, String newStatus, int newBatteryLevel) throws IOException{
	    	
	    	// checking battery level is valid
	    	if (newBatteryLevel<0 || newBatteryLevel > 100) {
	    		return false;
	    	}
	    	
	    	List<Drone> drones = getDrones();
	    	
	    	for (Drone drone:drones) {
	    		if(drone.getId().equals(droneId)) {
	    			drone.setStatus(newStatus);
	    			drone.setBatteryLevel(newBatteryLevel);
	    			
	    			store.saveList("drones.json", drones);
	    			
	    			return true;
	    		}
	    	}
	    	return false;
	    }
	    //method
	    //create a new farm alert and save it to JSON
	    public boolean createFarmAlert(FarmAlert alert) throws IOException{
	    	List<FarmAlert> alerts = getAlerts();
	    	
	    	//prevent dups
	    	for(FarmAlert existing : alerts) {
	    		if(existing.getId().equals(alert.getId())) {
	    			return false;
	    		}
	    	}
	    	
	    	//add new alerts
	    	alerts.add(alert);
	    	
	    	// save to JSON
	    	store.saveList("alerts.json", alerts);
	    	return true;
	    	
	    }
	    
	    //method
	    //get all the farm alerts that have not been read
	    //loads alerts from alerts.json checks each read value and returns only those marked false
	    public List<FarmAlert> getUnreadAlerts() throws IOException{
	    	
	    	List<FarmAlert> alerts = getAlerts();
	    	List<FarmAlert> unreadAlerts = new java.util.ArrayList<>();
	    	
	    	for (FarmAlert alert:alerts) {
	    		if(!alert.isRead()) {
	    			unreadAlerts.add(alert);
	    		}
	    	}
	    	return unreadAlerts;
	    	
	    }
	    
	//method
	    //mark alerts as read and save it to JSON
	    public boolean markAlertAsRead(String alertId) throws IOException{
	    	
	    	List<FarmAlert> alerts= getAlerts();
	    	
	    	for (FarmAlert alert : alerts ) {
	    		if (alert.getId().equals(alertId)) {
	    			
	    			//mark alert as read
	    			alert.setRead(true);
	    			
	    			//save updated alerts to JSON file
	    			store.saveList("alerts.json", alerts);
	    			
	    			return true;
	    		}
	    	}
	    	// alert id was not found
	    	return false;
	    }
	    
	    //method
	    // generates alerts when latest reading is below 30%
	    public boolean checkSoilMoistureAlert(String fieldId) throws IOException{
	    	
	    	MonitoringReading latest=null;
	    	
	    	///find latest reading for this field
	    	for (MonitoringReading reading:getReadings()) {
	    		if (fieldId.equals(reading.getFieldId())) {
	    			if(latest==null || reading.getTimestamp().isAfter(latest.getTimestamp())) {
	    				latest=reading;
	    			}
	    		}
	    	}
	    	
	    	// no reading or soil moisture normal
	    	if(latest==null ||latest.getSoilMoisturePercent() >=30) {
	    		return false;
	    	}
	    	
	    	// use the reading ID to prevent dup alerts
	    	String alertId="SOIL-" +latest.getId();
	    	
	    	FarmAlert alert = new FarmAlert(
	    			alertId, 
	    			fieldId, 
	    			latest.getDroneId(), 
	    			"LOW_SOIL_MOISTURE", 
	    			"low soil moisture detected: " + latest.getSoilMoisturePercent()+"%", "HIGH",
	    			java.time.LocalDateTime.now(), 
	    			false
	    			);
	    	return createFarmAlert(alert);
	    }
	    
	    //method
	    // find farm field by ID
	    public FarmField getFieldById(String fieldId) throws IOException{
	    	List<FarmField> fields = getFields();
	    	
	    	for (FarmField field :fields) {
	    		if(field.getId().equals(fieldId)) {
	    			return field;
	    		}
	    	}
	    	
	    	//field not found
	    	return null;
	    	
	    }
	    
	    
} 
