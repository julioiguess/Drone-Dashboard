package model;

/**
 * Common component interface for the Composite pattern.
 * Defines shared properties and operations for both individual farm items and item-containers.
 */
public interface FarmItemComponent {
    // Getters
    String getName();
    double getPrice();
    double getLocationX();
    double getLocationY();
    double getLength();
    double getWidth();
    double getHeight();

    // Command methods to edit properties
    void setName(String name);
    void setPrice(double price);
    void setLocationX(double locationX);
    void setLocationY(double locationY);
    void setLength(double length);
    void setWidth(double width);
    void setHeight(double height);

    // Composite management methods (supported by containers, defaulted for leaf items)
    void add(FarmItemComponent component);
    void remove(FarmItemComponent component);
    FarmItemComponent getChild(int index);
}
