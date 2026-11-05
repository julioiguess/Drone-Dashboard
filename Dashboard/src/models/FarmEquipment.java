// Edited by Samuel Culp

package models;
import java.time.LocalDateTime;

public class FarmEquipment {

	private String id;
	private String name;
	private String type;
	private String status;
	private double latitude;
	private double longitude;
	private LocalDateTime lastDetected;

	public FarmEquipment() {

	}

	public FarmEquipment(String id, String name, String type,
			String status, double latitude, double longitude,
			LocalDateTime lastDetected) {

		this.id = id;
		this.name = name;
		this.type = type;
		this.status = status;
		this.latitude = latitude;
		this.longitude = longitude;
		this.lastDetected = lastDetected;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public LocalDateTime getLastDetected() {
		return lastDetected;
	}

	public void setLastDetected(LocalDateTime lastDetected) {
		this.lastDetected = lastDetected;
	}

}