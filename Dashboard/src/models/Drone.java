// Edited by Samuel Culp

package models;
import java.time.LocalDateTime;

public class Drone {
	
	private String id;
	private String name;
	private int batteryLevel;
	private String status;
	private double latitude;
	private LocalDateTime lastUpdated;
	private double longitude;
	
	public Drone() {
		
	}
	
	public Drone(String id, String name, int batteryLevel,
			String status, double latitude, LocalDateTime lastUpdated, double longitude) {
		
		this.id = id;
		this.name = name;
		this.batteryLevel = batteryLevel;
		this.status = status;
		this.latitude = latitude;
		this.lastUpdated = lastUpdated;
		this.longitude = longitude;
		
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getBatteryLevel() {
		return batteryLevel;
	}
	public void setBatteryLevel(int batteryLevel) {
		this.batteryLevel = batteryLevel;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus (String status) {
		this.status = status;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}
	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public double getLongitude() {
	    return longitude;
	}

	public void setLongitude(double longitude) {
	    this.longitude = longitude;
	}
	
}



























// (;