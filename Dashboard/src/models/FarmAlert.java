// Edited by Samuel Culp

package models;
import java.time.LocalDateTime;

public class FarmAlert {

	private String id;
	private String fieldId;
	private String droneId;
	private String type;
	private String message;
	private String severity;
	private LocalDateTime timestamp;
	private boolean read;

	public FarmAlert() {

	}

	public FarmAlert(String id, String fieldId, String droneId,
			String type, String message, String severity,
			LocalDateTime timestamp, boolean read) {

		this.id = id;
		this.fieldId = fieldId;
		this.droneId = droneId;
		this.type = type;
		this.message = message;
		this.severity = severity;
		this.timestamp = timestamp;
		this.read = read;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

}