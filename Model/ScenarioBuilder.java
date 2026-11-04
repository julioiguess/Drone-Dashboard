package model;

/**
 * Builds the required testing scenario for the farm dashboard.
 */
public class ScenarioBuilder {
    
    public static FarmContainer buildTestScenario() {
        // Root container (Farm Root)
        FarmContainer root = new FarmContainer("Farm Root", 0.0, 0, 0, 800, 600, 0);

        // 1. Add barn to root
        FarmContainer barn = new FarmContainer("Barn", 15000.0, 50, 50, 200, 150, 50);
            // Add livestock-area (item-container) to the barn
            FarmContainer livestockArea = new FarmContainer("Livestock Area", 2000.0, 60, 60, 80, 70, 20);
                // Add cow (item) to the livestock-area
                livestockArea.add(new FarmItem("Cow", 1500.0, 70, 70, 5, 3, 4));
            barn.add(livestockArea);
            // Add milk storage (item) to the barn
            barn.add(new FarmItem("Milk Storage", 5000.0, 140, 60, 30, 30, 25));
        root.add(barn);

        // 2. Add storage building (item-container) to the root
        FarmContainer storageBuilding = new FarmContainer("Storage Building", 10000.0, 300, 50, 180, 120, 40);
            // Add tractor (item) to storage building
            storageBuilding.add(new FarmItem("Tractor", 25000.0, 320, 70, 40, 20, 25));
            // Add tiller (item) to storage building
            storageBuilding.add(new FarmItem("Tiller", 3000.0, 370, 70, 15, 10, 10));
        root.add(storageBuilding);

        // 3. Add command center (item-container) to root
        FarmContainer commandCenter = new FarmContainer("Command Center", 8000.0, 550, 50, 100, 100, 30);
            // Add drone (item) to command center (Starting Position of Drone)
            commandCenter.add(new FarmItem("Drone", 1200.0, 570, 70, 10, 10, 5));
        root.add(commandCenter);

        // 4. Add soy crop (item)
        root.add(new FarmItem("Soy Crop", 4000.0, 50, 300, 200, 100, 10));

        return root;
    }
}
