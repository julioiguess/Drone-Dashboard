package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a composite container node (e.g., Barn, Storage Building, Command Center, Livestock Area)
 * that can hold other items and containers.
 */
public class FarmContainer implements FarmItemComponent {
    private String name;
    private double price;
    private double locationX;
    private double locationY;
    private double length;
    private double width;
    private double height;
    private List<FarmItemComponent> children;

    public FarmContainer(String name, double price, double locationX, double locationY, double length, double width, double height) {
        this.name = name;
        this.price = price;
        this.locationX = locationX;
        this.locationY = locationY;
        this.length = length;
        this.width = width;
        this.height = height;
        this.children = new ArrayList<>();
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

    @Override
    public void add(FarmItemComponent component) {
        children.add(component);
    }

    @Override
    public void remove(FarmItemComponent component) {
        children.remove(component);
    }

    @Override
    public FarmItemComponent getChild(int index) {
        return children.get(index);
    }

    public List<FarmItemComponent> getChildren() {
        return children;
    }
}
