package model;

import javafx.scene.layout.Pane;

/**
 * Common interface for individual farm items and item-containers.
 */
public interface FarmItemComponent {

    // Shared properties
    String getName();
    double getPrice();
    double getLocationX();
    double getLocationY();
    double getLength();
    double getWidth();
    double getHeight();

    // Property editing
    void setName(String name);
    void setPrice(double price);
    void setLocationX(double locationX);
    void setLocationY(double locationY);
    void setLength(double length);
    void setWidth(double width);
    void setHeight(double height);

    // Component identification
    boolean isContainer();
    boolean isItem();

    // Size and visualization
    int getSize();
    void draw(Pane pane);

    // Composite operations
    void add(FarmItemComponent component);
    void remove(FarmItemComponent component);
    FarmItemComponent getChild(int index);
}
