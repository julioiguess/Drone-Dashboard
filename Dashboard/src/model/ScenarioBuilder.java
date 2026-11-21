package model;

/**
 * Starter farm arrangement with larger, more readable components.
 * Uses the current renderer's width/height screen dimensions.
 */
public class ScenarioBuilder {

    public static FarmContainer buildTestScenario() {
        FarmContainer root = new FarmContainer(
                "Farm Root", 0.0,
                0, 0, 0, 800, 600);

        FarmContainer barn = new FarmContainer(
                "Barn", 15000.0,
                30, 40, 200, 300, 200);

        FarmContainer livestockArea = new FarmContainer(
                "Livestock Area", 2000.0,
                45, 75, 80, 150, 135);

        livestockArea.add(new FarmItem(
                "Cow", 1500.0,
                65, 115, 5, 95, 60));

        barn.add(livestockArea);

        barn.add(new FarmItem(
                "Milk Storage", 5000.0,
                205, 110, 30, 110, 85));

        root.add(barn);

        FarmContainer storageBuilding = new FarmContainer(
                "Storage Building", 10000.0,
                360, 40, 180, 250, 200);

        storageBuilding.add(new FarmItem(
                "Tractor", 25000.0,
                375, 95, 40, 110, 75));

        storageBuilding.add(new FarmItem(
                "Tiller", 3000.0,
                500, 110, 15, 90, 60));

        root.add(storageBuilding);

        FarmContainer commandCenter = new FarmContainer(
                "Command Center", 8000.0,
                630, 40, 100, 155, 200);

        commandCenter.add(new FarmItem(
                "Drone", 1200.0,
                650, 115, 10, 105, 60));

        root.add(commandCenter);

        root.add(new FarmItem(
                "Soy Crop", 4000.0,
                40, 290, 200, 300, 200));

        return root;
    }
}
