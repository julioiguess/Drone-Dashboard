package model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.Pane;
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

    @Override
    public boolean isContainer() { return false; }

    @Override 
    public boolean isItem() { return true; }

    @Override 
    public int getSize() { return 1; }

    @Override 
    public void draw(Pane pane) {
        Rectangle rect = new Rectangle(locationX, locationY, width, height);
        rect.setFill(Color.web("#14533f"));
        rect.setArcWidth(8);
        rect.setArcHeight(8);
        rect.setStroke(Color.web("#34d399"));
        rect.setStrokeWidth(1.5);

        Text text = new Text(locationX + 5, locationY + 18, name);

        text.setFill(Color.web("#e2e8f0"));
        text.setFont(javafx.scene.text.Font.font(
                "System", javafx.scene.text.FontWeight.BOLD, 13));
        text.setMouseTransparent(true);
        pane.getChildren().addAll(rect, text);
    }

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
