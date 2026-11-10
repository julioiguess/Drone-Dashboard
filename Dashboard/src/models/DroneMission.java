// Edited by Samuel Culp

package models;
import java.time.LocalDateTime;

public class DroneMission{
private String id;
private String droneId;
private String fieldId;
private String missionType;
private LocalDateTime scheduledTime;
private String status;
private LocalDateTime completedTime;

public DroneMission() {
	
}

public DroneMission(String id, String droneId, String fieldId, 
		String missionType, LocalDateTime scheduledTime, String status,
		LocalDateTime completedTime) {
	
}

public String getId() {
	return id;
}

public void setId(String id) {
	this.id = id;
}
public String getDroneId() {
	return droneId;
}
public void setDroneId(String droneId) {
	this.droneId = droneId;
}
public String getFieldId() {
	return fieldId;
}
public void setFieldId(String fieldId) {
	this.fieldId = fieldId;
}
public String getMissionType() {
	return missionType;
}
public void setMissionType(String missionType) {
	this.missionType = missionType;
}
public LocalDateTime getscheduledTime() {
	return scheduledTime;
}
public void setScheduledTime(LocalDateTime scheduledTime) {
	this.scheduledTime = scheduledTime;
}
public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}
public LocalDateTime getCompletedTime() {
	return completedTime;
}
public void setCompletedTime (LocalDateTime completedTime) {
	this.completedTime = completedTime;
}




}