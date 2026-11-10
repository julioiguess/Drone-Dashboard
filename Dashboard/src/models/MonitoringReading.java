// Edited by Samuel Culp

package models;
import java.time.LocalDateTime;

public class MonitoringReading {

	private String id;
	private String fieldId;
	private String droneId;
	private LocalDateTime timestamp;
	private double soilMoisturePercent;
	private double cropGrowthPercent;
	private double harvestReadinessPercent;
	private String cropHealthStatus;

	public MonitoringReading() {

	}

	public MonitoringReading(String id, String fieldId, String droneId,
			LocalDateTime timestamp, double soilMoisturePercent,
			double cropGrowthPercent, double harvestReadinessPercent,
			String cropHealthStatus) {

		this.id = id;
		this.fieldId = fieldId;
		this.droneId = droneId;
		this.timestamp = timestamp;
		this.soilMoisturePercent = soilMoisturePercent;
		this.cropGrowthPercent = cropGrowthPercent;
		this.harvestReadinessPercent = harvestReadinessPercent;
		this.cropHealthStatus = cropHealthStatus;

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFieldId() {
		return fieldId;
	}

	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}

	public String getDroneId() {
		return droneId;
	}

	public void setDroneId(String droneId) {
		this.droneId = droneId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public double getSoilMoisturePercent() {
		return soilMoisturePercent;
	}

	public void setSoilMoisturePercent(double soilMoisturePercent) {
		this.soilMoisturePercent = soilMoisturePercent;
	}

	public double getCropGrowthPercent() {
		return cropGrowthPercent;
	}

	public void setCropGrowthPercent(double cropGrowthPercent) {
		this.cropGrowthPercent = cropGrowthPercent;
	}

	public double getHarvestReadinessPercent() {
		return harvestReadinessPercent;
	}

	public void setHarvestReadinessPercent(double harvestReadinessPercent) {
		this.harvestReadinessPercent = harvestReadinessPercent;
	}

	public String getCropHealthStatus() {
		return cropHealthStatus;
	}

	public void setCropHealthStatus(String cropHealthStatus) {
		this.cropHealthStatus = cropHealthStatus;
	}

}