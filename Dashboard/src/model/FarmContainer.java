package model;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;

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
    public boolean isContainer() { return true; }

    @Override 
    public boolean isItem() { return false; }

    @Override 
    public int getSize() { return children.size(); }

    @Override
    public void draw(Pane pane) {
        // Crucial: Container draws itself FIRST so its outline sits behind children
        Rectangle rect = new Rectangle(locationX, locationY, width, height);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.web("#38bdf8"));
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        rect.setStrokeWidth(2);

        Text text = new Text(locationX + 6, locationY + 19, name);

        text.setFill(Color.web("#e2e8f0"));
        text.setFont(javafx.scene.text.Font.font(
                "System", javafx.scene.text.FontWeight.BOLD, 13));
        text.setMouseTransparent(true);
        pane.getChildren().addAll(rect, text);

        // Recursively draw children
        for (FarmItemComponent child : children) {
            child.draw(pane);
        }
    }

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