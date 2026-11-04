package model;

/**
 * Represents a leaf node in the Composite pattern (e.g., Cow, Tractor, Tiller, Drone, Soy Crop).
 */
public class FarmItem implements FarmItemComponent {
    private String name;
    private double price;
    private double locationX;
    private double locationY;
    private double length;
    private double width;
    private double height;

    public FarmItem(String name, double price, double locationX, double locationY, double length, double width, double height) {
        this.name = name;
        this.price = price;
        this.locationX = locationX;
        this.locationY = locationY;
        this.length = length;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getName() { return name; }
    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public double getPrice() { return price; }
    @Override
    public void setPrice(double price) { this.price = price; }

    @Override
    public double getLocationX() { return locationX; }
    @Override
    public void setLocationX(double locationX) { this.locationX = locationX; }

    @Override
    public double getLocationY() { return locationY; }
    @Override
    public void setLocationY(double locationY) { this.locationY = locationY; }

    @Override
    public double getLength() { return length; }
    @Override
    public void setLength(double length) { this.length = length; }

    @Override
    public double getWidth() { return width; }
    @Override
    public void setWidth(double width) { this.width = width; }

    @Override
    public double getHeight() { return height; }
    @Override
    public void setHeight(double height) { this.height = height; }

    // Leaf nodes do not contain sub-components
    @Override
    public void add(FarmItemComponent component) {
        throw new UnsupportedOperationException("Cannot add child to a leaf FarmItem.");
    }

    @Override
    public void remove(FarmItemComponent component) {
        throw new UnsupportedOperationException("Cannot remove child from a leaf FarmItem.");
    }

    @Override
    public FarmItemComponent getChild(int index) {
        throw new UnsupportedOperationException("Leaf FarmItem has no children.");
    }
}
