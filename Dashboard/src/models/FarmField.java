
 // Edited by Samuel Culp

package models;

public class FarmField {

    private String id;
    private String name;
    private String cropType;
    private double areaAcres;
    private double latitude;
    private double longitude;

    // Empty constructor
    public FarmField() {
    }

    // Constructor with all attributes
    public FarmField(String id, String name,
                     String cropType, double areaAcres,
                     double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.cropType = cropType;
        this.areaAcres = areaAcres;
        this.latitude = latitude;
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

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public double getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(double areaAcres) {
        this.areaAcres = areaAcres;
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
}
