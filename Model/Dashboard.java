import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX dashboard for viewing and editing a farm built with the Composite pattern.
 *
 * <p>The window has two regions:
 * <ul>
 *   <li>A sidebar (left) with a dropdown of every farm component, a panel of item commands,
 *       a panel of item-container commands, and drone actions.</li>
 *   <li>A farm pane (right) where the farm tree is drawn and the drone is animated.</li>
 * </ul>
 *
 * <p>Every edit is validated so that a component always lies entirely within the area of the
 * container that holds it, and the farm pane is redrawn after each successful change.
 *
 * <p>This class is a Singleton: its constructor is private, so the only way to obtain a dashboard
 * is {@link #getInstance()}, which always returns the same object. It is kept separate from the
 * JavaFX {@code Application} class ({@link MainDashboard}) because JavaFX requires that class to
 * have a public constructor.
 */
public final class Dashboard {
    /**
     * Holds the single instance. Java loads this nested class, and therefore creates the instance,
     * only the first time {@link #getInstance()} is called. Class loading is thread-safe, so no
     * locking is needed.
     */
    private static class SingletonHolder {
        private static final Dashboard INSTANCE = new Dashboard();
    }

    /** Private so no other class can create a second dashboard. */
    private Dashboard() {}

    /**
     * Returns the one and only dashboard, creating it on the first call.
     *
     * <p>The first call must happen after JavaFX has started (for example, from
     * {@code Application.start}). The constructor creates no JavaFX objects, but {@link #show}
     * does, and JavaFX objects cannot be created before the toolkit is running.
     *
     * @return the single dashboard instance
     */
    public static Dashboard getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /** Width of the farm pane in pixels; matches the farm root's width. */
    private static final double FARM_WIDTH = 800;
    /** Height of the farm pane in pixels; matches the farm root's height. */
    private static final double FARM_HEIGHT = 600;
    /** Preferred width shared by every sidebar button. */
    private static final double BUTTON_WIDTH = 150;
    /** Radius of the blue circle that represents the drone. */
    private static final double DRONE_RADIUS = 12;
    /** Time the drone takes to fly one leg of a route (to a stop or back home). */
    private static final Duration DRONE_LEG_DURATION = Duration.seconds(0.8);

    /** Root container of the farm tree; its area is the whole farm. */
    private FarmContainer scenario;
    /** Model object for the drone; its location is the drone's home position. */
    private FarmItem droneItem;
    /** Pane the farm tree and the drone are drawn on. */
    private Pane farmPane;
    /** Visual representation of the drone, animated by translating it away from home. */
    private Circle drone;
    /** Dropdown listing every component in the farm; commands act on its selection. */
    private ComboBox<FarmItemComponent> componentBox;
    /** Panel of commands that apply only to leaf items. */
    private VBox itemCommands;
    /** Panel of commands that apply only to item-containers. */
    private VBox containerCommands;
    /** Drone animation currently playing, kept so it can be stopped before another starts. */
    private SequentialTransition droneAnimation;
    /** Window the dashboard is shown in, or {@code null} until {@link #show} is first called. */
    private javafx.scene.Parent view;
    
    /**
     * Builds the whole dashboard and shows it in {@code stage}.
     *
     * <p>The dashboard is built only once. If it is already showing, later calls ignore the
     * stage they are given and bring the existing window to the front, so a second copy of the
     * farm can never be created.
     *
     * <p>Steps on the first call:
     * <ol>
     *   <li>Builds the test farm with {@link ScenarioBuilder} and places the drone inside the
     *       "Command Center" container. If no such container exists, the drone is added to the
     *       farm root instead.</li>
     *   <li>Creates the farm pane and the drone circle, then draws everything with
     *       {@link #redrawFarm()}.</li>
     *   <li>Creates the component dropdown. Its converter displays each component's name, and a
     *       selection listener enables only the command panel that matches the selected
     *       component's type.</li>
     *   <li>Creates the item, container, and drone command panels, wiring each button to its
     *       handler method.</li>
     *   <li>Fills the dropdown, lays the sidebar and farm pane side by side in a grid, and shows
     *       the 1000x600 window.</li>
     * </ol>
     *
     * @param stage window to show the dashboard in
     */
    public javafx.scene.Parent getView() {
        if (view != null) {
            return view;
        }

        scenario = ScenarioBuilder.buildTestScenario();

        FarmItemComponent existingDrone = findByName(scenario, "Drone");

        if (existingDrone instanceof FarmItem) {
            droneItem = (FarmItem) existingDrone;
        } else {
            droneItem = new FarmItem(
                    "Drone", 1200.0, 570, 70, 10, 10, 5);

            FarmItemComponent commandCenter =
                    findByName(scenario, "Command Center");

            if (commandCenter != null && commandCenter.isContainer()) {
                commandCenter.add(droneItem);
            } else {
                scenario.add(droneItem);
            }
        }
        
        /* --- Main farm pane --- */
        farmPane = new Pane();

        farmPane.setMinSize(FARM_WIDTH, FARM_HEIGHT);
        farmPane.setPrefSize(FARM_WIDTH, FARM_HEIGHT);
        farmPane.setMaxSize(FARM_WIDTH, FARM_HEIGHT);

        farmPane.setStyle("-fx-background-color: #a8c990;");
        drone = new Circle(DRONE_RADIUS, Color.BLUE);
        redrawFarm();

        /* --- Item / container selection --- */
        componentBox = new ComboBox<>();
        componentBox.setPrefWidth(200);
        componentBox.setPadding(new Insets(5));
        componentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FarmItemComponent component) {
                return component == null ? "" : component.getName();
            }

            @Override
            public FarmItemComponent fromString(String name) {
                return null;
            }
        });
        componentBox.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSelection, newSelection) -> updateCommandAvailability(newSelection));

        /* --- Command panels --- */
        itemCommands = createCommandBox("Commands on Items",
            createButton("Rename", e -> renameSelected()),
            createButton("Change location", e -> changeSelectedLocation()),
            createButton("Change price", e -> changeSelectedPrice()),
            createButton("Change dimensions", e -> changeSelectedDimensions()),
            createButton("Delete", e -> deleteSelected()));

        containerCommands = createCommandBox("Commands on Item Containers",
            createButton("Rename", e -> renameSelected()),
            createButton("Change location", e -> changeSelectedLocation()),
            createButton("Change price", e -> changeSelectedPrice()),
            createButton("Change dimensions", e -> changeSelectedDimensions()),
            createButton("Add item", e -> addToSelected(false)),
            createButton("Add item-container", e -> addToSelected(true)),
            createButton("Delete", e -> deleteSelected()));

        VBox droneActions = createCommandBox("Drone Actions",
            createButton("Visit Item/Container", e -> visitSelected()),
            createButton("Scan Farm", e -> scanFarm()));

        refreshComponentBox(null);

        VBox sidebar = new VBox(10, componentBox, itemCommands, containerCommands, droneActions);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(200);

        GridPane panel = new GridPane();
        panel.setStyle("-fx-background-color: #17232e;");

        sidebar.setStyle("-fx-background-color: #17232e;");

        panel.add(sidebar, 0, 0);
        panel.add(farmPane, 1, 0);

        // Keep text readable on the dark editor sidebar.
        // Farm labels use Text nodes and retain their existing styling.
        panel.getStylesheets().add(
                getClass().getResource("/application/dashboard.css")
                        .toExternalForm());

        view = panel;
        return view;
    }

    /* ------------------------- UI construction ------------------------- */

    /**
     * Creates a sidebar button with the shared width and a click handler.
     *
     * @param text   label shown on the button
     * @param action handler run when the button is clicked
     * @return the configured button
     */
    private Button createButton(String text, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setPrefWidth(BUTTON_WIDTH);
        button.setOnAction(action);
        return button;
    }

    /**
     * Creates a centered vertical panel with a title label followed by the given buttons in order.
     *
     * @param title   text of the label placed above the buttons
     * @param buttons buttons to stack under the title
     * @return the panel containing the label and buttons
     */
    private VBox createCommandBox(String title, Button... buttons) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(new Label(title));
        box.getChildren().addAll(buttons);
        return box;
    }

    /**
     * Enables only the command panel that applies to the current selection.
     *
     * <p>The item panel is enabled only when a leaf item is selected, and the container panel only
     * when an item-container is selected. Both are disabled when nothing is selected. Disabling a
     * panel also disables every button inside it.
     *
     * @param selected the newly selected component, or {@code null} if the selection was cleared
     */
    private void updateCommandAvailability(FarmItemComponent selected) {
        itemCommands.setDisable(selected == null || !selected.isItem());
        containerCommands.setDisable(selected == null || !selected.isContainer());
    }

    /* ------------------------- Rendering ------------------------- */

    /**
     * Redraws the entire farm pane from the current state of the farm tree.
     *
     * <p>Stops any running drone animation, clears the pane, and asks the farm root to draw itself
     * (containers draw their outline and then their children recursively). The drone circle is then
     * re-centered on the drone item's current location with its translation reset to zero, so it
     * sits at home, and is added last so it appears on top of everything else.
     */
    private void redrawFarm() {
        if (droneAnimation != null)
            droneAnimation.stop();

        farmPane.getChildren().clear();
        scenario.draw(farmPane);

        drone.setCenterX(centerX(droneItem));
        drone.setCenterY(centerY(droneItem));
        drone.setTranslateX(0);
        drone.setTranslateY(0);
        farmPane.getChildren().add(drone);
    }

    /**
     * Rebuilds the dropdown so it lists the farm root followed by every component in the farm,
     * in depth-first order.
     *
     * <p>The selection is cleared before the list is replaced. Without this, renaming a component
     * that stays selected would leave the old name displayed. Afterwards, {@code toSelect} is
     * selected if it is still in the farm; otherwise the first entry (the farm root) is selected.
     *
     * @param toSelect component to select after the refresh, or {@code null} to select the first entry
     */
    private void refreshComponentBox(FarmItemComponent toSelect) {
        List<FarmItemComponent> components = new ArrayList<>();
        components.add(scenario);
        collectDescendants(scenario, components);

        componentBox.getSelectionModel().clearSelection();
        componentBox.getItems().setAll(components);
        if (toSelect != null && components.contains(toSelect))
            componentBox.getSelectionModel().select(toSelect);
        else
            componentBox.getSelectionModel().selectFirst();
    }

    /**
     * Brings the view up to date after the farm tree changes by redrawing the farm pane and
     * rebuilding the dropdown.
     *
     * @param toSelect component to select in the dropdown afterwards
     */
    private void refreshAfterChange(FarmItemComponent toSelect) {
        redrawFarm();
        refreshComponentBox(toSelect);
    }

    /* ------------------------- Item / container commands ------------------------- */

    /**
     * Returns the component currently selected in the dropdown.
     *
     * @return the selected component, or {@code null} if nothing is selected
     */
    private FarmItemComponent getSelected() {
        return componentBox.getSelectionModel().getSelectedItem();
    }

    /**
     * Handles the "Rename" button for both items and containers.
     *
     * <p>Opens a dialog pre-filled with the current name. Surrounding whitespace is trimmed, and an
     * empty name is rejected with an error. On success, the name is updated and the view refreshed.
     * Cancelling the dialog changes nothing.
     */
    private void renameSelected() {
        FarmItemComponent selected = getSelected();
        if (selected == null)
            return;

        promptFields("Rename", new String[]{"Name"}, new String[]{selected.getName()})
            .ifPresent(values -> {
                if (values[0].isEmpty()) {
                    showError("Name cannot be empty.");
                    return;
                }
                selected.setName(values[0]);
                refreshAfterChange(selected);
            });
    }

    /**
     * Handles the "Change location" button for both items and containers.
     *
     * <p>Opens a dialog pre-filled with the current X and Y. The move is rejected if:
     * <ul>
     *   <li>the selection is the farm root, which always covers the whole farm;</li>
     *   <li>either value is not a number; or</li>
     *   <li>the component, at its current width and height, would not lie entirely within its
     *       parent container's area.</li>
     * </ul>
     *
     * <p>When a container moves, every component inside it (at every depth) moves by the same
     * offset via {@link #shiftChildren}, so its contents stay where they are relative to it and
     * remain within its area. This includes the drone when its container moves.
     */
    private void changeSelectedLocation() {
        FarmItemComponent selected = getSelected();
        if (selected == null)
            return;
        if (selected == scenario) {
            showError("The farm root cannot be moved.");
            return;
        }

        promptNumbers("Change location", new String[]{"X", "Y"},
                new double[]{selected.getLocationX(), selected.getLocationY()})
            .ifPresent(values -> {
                double x = values[0];
                double y = values[1];
                FarmContainer parent = findParent(scenario, selected);
                if (!fitsInside(x, y, selected.getWidth(), selected.getHeight(), parent)) {
                    showError(outOfBoundsMessage(selected.getName(), parent));
                    return;
                }

                double dx = x - selected.getLocationX();
                double dy = y - selected.getLocationY();
                selected.setLocationX(x);
                selected.setLocationY(y);
                if (selected.isContainer())
                    shiftChildren((FarmContainer) selected, dx, dy);
                refreshAfterChange(selected);
            });
    }

    /**
     * Handles the "Change price" button for both items and containers.
     *
     * <p>Opens a dialog pre-filled with the current price. A value that is not a number, or is
     * negative, is rejected with an error. Otherwise the price is updated and the view refreshed.
     */
    private void changeSelectedPrice() {
        FarmItemComponent selected = getSelected();
        if (selected == null)
            return;

        promptNumbers("Change price", new String[]{"Price"}, new double[]{selected.getPrice()})
            .ifPresent(values -> {
                if (!isValidPrice(values[0]))
                    return;
                selected.setPrice(values[0]);
                refreshAfterChange(selected);
            });
    }

    /**
     * Handles the "Change dimensions" button for both items and containers.
     *
     * <p>Opens a dialog pre-filled with the current length, width, and height. Width and height are
     * the on-screen size; length is stored but not drawn. The change is rejected if:
     * <ul>
     *   <li>the selection is the farm root, whose size is fixed to the farm area;</li>
     *   <li>any value is not a number, or is not greater than zero;</li>
     *   <li>the resized component would extend past its parent container's area; or</li>
     *   <li>the selection is a container and the new size would leave any of its direct children
     *       sticking out. Checking direct children is enough, because their own contents are
     *       already guaranteed to lie within them.</li>
     * </ul>
     * The component's location (its top-left corner) is unchanged.
     */
    private void changeSelectedDimensions() {
        FarmItemComponent selected = getSelected();
        if (selected == null)
            return;
        if (selected == scenario) {
            showError("The farm root's dimensions are fixed to the farm area.");
            return;
        }

        promptNumbers("Change dimensions", new String[]{"Length", "Width", "Height"},
                new double[]{selected.getLength(), selected.getWidth(), selected.getHeight()})
            .ifPresent(values -> {
                double length = values[0];
                double width = values[1];
                double height = values[2];
                if (!areValidDimensions(length, width, height))
                    return;

                double x = selected.getLocationX();
                double y = selected.getLocationY();
                FarmContainer parent = findParent(scenario, selected);
                if (!fitsInside(x, y, width, height, parent)) {
                    showError(outOfBoundsMessage(selected.getName(), parent));
                    return;
                }
                if (selected.isContainer() && !childrenFitInside((FarmContainer) selected, x, y, width, height)) {
                    showError("The new dimensions of " + selected.getName() + " are too small to hold all of its contents.");
                    return;
                }

                selected.setLength(length);
                selected.setWidth(width);
                selected.setHeight(height);
                refreshAfterChange(selected);
            });
    }

    /**
     * Handles the "Add item" and "Add item-container" buttons.
     *
     * <p>Opens one dialog asking for name, price, X, Y, length, width, and height. The position
     * defaults to the selected container's top-left corner, and each dimension defaults to 10. The
     * new component is rejected if:
     * <ul>
     *   <li>the name is empty;</li>
     *   <li>any numeric field is not a number;</li>
     *   <li>the price is negative or any dimension is not greater than zero; or</li>
     *   <li>it would not lie entirely within the selected container's area.</li>
     * </ul>
     * On success, a {@link FarmContainer} or {@link FarmItem} is created, added to the selected
     * container, and selected in the dropdown.
     *
     * @param asContainer {@code true} to create an item-container, {@code false} to create a leaf item
     */
    private void addToSelected(boolean asContainer) {
        FarmItemComponent selected = getSelected();
        if (selected == null || !selected.isContainer())
            return;
        FarmContainer parent = (FarmContainer) selected;

        String[] labels = {"Name", "Price", "X", "Y", "Length", "Width", "Height"};
        String[] defaults = {"", "0",
            formatNumber(parent.getLocationX()), formatNumber(parent.getLocationY()), "10", "10", "10"};

        promptFields(asContainer ? "Add item-container" : "Add item", labels, defaults)
            .ifPresent(text -> {
                String name = text[0];
                if (name.isEmpty()) {
                    showError("Name cannot be empty.");
                    return;
                }
                double[] values = parseNumbers(text, labels, 1);
                if (values == null)
                    return;

                double price = values[0], x = values[1], y = values[2];
                double length = values[3], width = values[4], height = values[5];
                if (!isValidPrice(price) || !areValidDimensions(length, width, height))
                    return;
                if (!fitsInside(x, y, width, height, parent)) {
                    showError(outOfBoundsMessage(name, parent));
                    return;
                }

                FarmItemComponent component = asContainer
                    ? new FarmContainer(name, price, x, y, length, width, height)
                    : new FarmItem(name, price, x, y, length, width, height);
                parent.add(component);
                refreshAfterChange(component);
            });
    }

    /**
     * Handles the "Delete" button for both items and containers.
     *
     * <p>Removes the selected component, along with everything inside it, from its parent container,
     * then selects the parent. Deletion is refused for:
     * <ul>
     *   <li>the farm root, since everything else lives inside it; and</li>
     *   <li>the drone, or any container that holds the drone at any depth, because the drone
     *       actions depend on the drone existing.</li>
     * </ul>
     */
    private void deleteSelected() {
        FarmItemComponent selected = getSelected();
        if (selected == null)
            return;
        if (selected == scenario) {
            showError("The farm root cannot be deleted.");
            return;
        }
        if (selected == droneItem || (selected.isContainer() && findParent((FarmContainer) selected, droneItem) != null)) {
            showError("The drone and the container holding it cannot be deleted.");
            return;
        }

        FarmContainer parent = findParent(scenario, selected);
        parent.remove(selected);
        refreshAfterChange(parent);
    }

    /* ------------------------- Drone actions ------------------------- */

    /**
     * Handles the "Visit Item/Container" button. The drone flies to the center of the selected
     * component and then back home. Any item or container can be visited, including the farm root
     * (the drone flies to the center of the farm).
     */
    private void visitSelected() {
        FarmItemComponent selected = getSelected();
        if (selected != null)
            playDroneRoute(List.of(selected));
    }

    /**
     * Handles the "Scan Farm" button. The drone visits the center of every item and container in
     * the farm in depth-first order (each container is visited just before its contents), skipping
     * itself, and then returns home.
     */
    private void scanFarm() {
        List<FarmItemComponent> stops = new ArrayList<>();
        collectDescendants(scenario, stops);
        stops.remove(droneItem);
        playDroneRoute(stops);
    }

    /**
     * Animates the drone through a list of stops and then back home.
     *
     * <p>Any animation already in progress is stopped first. The new route starts from wherever the
     * drone currently is, so clicking a drone action mid-flight never makes the drone jump. Each leg
     * is built with {@link #flyTo}, and the final leg flies to the drone item itself, which is home.
     *
     * @param stops components to visit, in order
     */
    private void playDroneRoute(List<FarmItemComponent> stops) {
        if (droneAnimation != null)
            droneAnimation.stop();

        droneAnimation = new SequentialTransition();
        for (FarmItemComponent stop : stops)
            droneAnimation.getChildren().add(flyTo(stop));
        droneAnimation.getChildren().add(flyTo(droneItem));
        droneAnimation.play();
    }

    /**
     * Creates one leg of a drone flight that ends at the center of {@code target}.
     *
     * <p>A {@link TranslateTransition} moves a node by an offset from its original position, not to
     * an absolute coordinate. The offset is therefore the target's center minus the drone circle's
     * home center. Targeting the drone item itself gives an offset of (0, 0), which flies the drone
     * home.
     *
     * @param target component whose center the drone should fly to
     * @return a transition lasting {@link #DRONE_LEG_DURATION}
     */
    private TranslateTransition flyTo(FarmItemComponent target) {
        TranslateTransition transition = new TranslateTransition(DRONE_LEG_DURATION, drone);
        transition.setToX(centerX(target) - drone.getCenterX());
        transition.setToY(centerY(target) - drone.getCenterY());
        return transition;
    }

    /* ------------------------- Tree helpers ------------------------- */

    /**
     * Recursively appends every component inside {@code container}, at every depth, to
     * {@code result} in depth-first order: each child is added just before its own contents.
     * The container itself is not added.
     *
     * @param container container whose contents are collected
     * @param result    list the components are appended to
     */
    private static void collectDescendants(FarmContainer container, List<FarmItemComponent> result) {
        for (FarmItemComponent child : container.getChildren()) {
            result.add(child);
            if (child.isContainer())
                collectDescendants((FarmContainer) child, result);
        }
    }

    /**
     * Searches everything inside {@code container}, at every depth, for a component with an exactly
     * matching (case-sensitive) name.
     *
     * @param container container to search within (not itself checked)
     * @param name      name to look for
     * @return the first match in depth-first order, or {@code null} if none is found
     */
    private static FarmItemComponent findByName(FarmContainer container, String name) {
        List<FarmItemComponent> components = new ArrayList<>();
        collectDescendants(container, components);
        for (FarmItemComponent component : components)
            if (component.getName().equals(name))
                return component;
        return null;
    }

    /**
     * Recursively finds the container that directly holds {@code target}.
     *
     * <p>Components are matched by identity ({@code ==}), not by name, so components with duplicate
     * names are handled correctly. A non-null result also means that {@code target} lies somewhere
     * inside {@code container}, which {@link #deleteSelected()} uses to detect containers holding
     * the drone.
     *
     * @param container container to search within
     * @param target    component whose parent is wanted
     * @return the direct parent of {@code target}, or {@code null} if it is not inside {@code container}
     */
    private static FarmContainer findParent(FarmContainer container, FarmItemComponent target) {
        for (FarmItemComponent child : container.getChildren()) {
            if (child == target)
                return container;
            if (child.isContainer()) {
                FarmContainer parent = findParent((FarmContainer) child, target);
                if (parent != null)
                    return parent;
            }
        }
        return null;
    }

    /**
     * Recursively moves every component inside {@code container}, at every depth, by the same
     * offset. Used when a container moves so its contents move with it. The container's own
     * location is not changed here.
     *
     * @param container container whose contents are moved
     * @param dx        horizontal offset in pixels
     * @param dy        vertical offset in pixels
     */
    private static void shiftChildren(FarmContainer container, double dx, double dy) {
        for (FarmItemComponent child : container.getChildren()) {
            child.setLocationX(child.getLocationX() + dx);
            child.setLocationY(child.getLocationY() + dy);
            if (child.isContainer())
                shiftChildren((FarmContainer) child, dx, dy);
        }
    }

    /* ------------------------- Geometry / validation ------------------------- */

    /**
     * Returns the horizontal center of a component on the farm pane: its X plus half its width.
     *
     * @param component component to measure
     * @return the X coordinate of its center
     */
    private static double centerX(FarmItemComponent component) {
        return component.getLocationX() + component.getWidth() / 2;
    }

    /**
     * Returns the vertical center of a component on the farm pane: its Y plus half its height.
     *
     * @param component component to measure
     * @return the Y coordinate of its center
     */
    private static double centerY(FarmItemComponent component) {
        return component.getLocationY() + component.getHeight() / 2;
    }

    /**
     * Checks whether one rectangle lies entirely within another. Touching an edge counts as inside.
     *
     * @param x          left edge of the inner rectangle
     * @param y          top edge of the inner rectangle
     * @param width      width of the inner rectangle
     * @param height     height of the inner rectangle
     * @param areaX      left edge of the outer rectangle
     * @param areaY      top edge of the outer rectangle
     * @param areaWidth  width of the outer rectangle
     * @param areaHeight height of the outer rectangle
     * @return {@code true} if all four edges of the inner rectangle are within the outer one
     */
    private static boolean fitsInside(double x, double y, double width, double height,
                                      double areaX, double areaY, double areaWidth, double areaHeight) {
        return x >= areaX && y >= areaY
            && x + width <= areaX + areaWidth
            && y + height <= areaY + areaHeight;
    }

    /**
     * Checks whether a rectangle lies entirely within a component's on-screen area (its location,
     * width, and height).
     *
     * @param x      left edge of the rectangle
     * @param y      top edge of the rectangle
     * @param width  width of the rectangle
     * @param height height of the rectangle
     * @param area   component whose area must contain the rectangle
     * @return {@code true} if the rectangle fits within {@code area}
     */
    private static boolean fitsInside(double x, double y, double width, double height, FarmItemComponent area) {
        return fitsInside(x, y, width, height,
            area.getLocationX(), area.getLocationY(), area.getWidth(), area.getHeight());
    }

    /**
     * Checks whether every direct child of {@code container} would still fit if the container
     * occupied the given rectangle. Used to stop a container from being shrunk smaller than its
     * contents.
     *
     * @param container container whose children are checked
     * @param x         proposed left edge of the container
     * @param y         proposed top edge of the container
     * @param width     proposed width of the container
     * @param height    proposed height of the container
     * @return {@code true} if all children fit within the proposed rectangle
     */
    private static boolean childrenFitInside(FarmContainer container, double x, double y, double width, double height) {
        for (FarmItemComponent child : container.getChildren())
            if (!fitsInside(child.getLocationX(), child.getLocationY(), child.getWidth(), child.getHeight(),
                    x, y, width, height))
                return false;
        return true;
    }

    /**
     * Builds the error message shown when a component would extend past its container. The message
     * names the container and gives its X and Y ranges, so the user knows which values are allowed.
     *
     * @param name name of the component being placed
     * @param area container it must lie within
     * @return the message, for example "Tractor must lie entirely within Storage Building (x 300 to 440, y 50 to 135)."
     */
    private static String outOfBoundsMessage(String name, FarmItemComponent area) {
        return name + " must lie entirely within " + area.getName()
            + " (x " + formatNumber(area.getLocationX()) + " to " + formatNumber(area.getLocationX() + area.getWidth())
            + ", y " + formatNumber(area.getLocationY()) + " to " + formatNumber(area.getLocationY() + area.getHeight()) + ").";
    }

    /**
     * Checks that a price is not negative, showing an error if it is. Zero is allowed.
     *
     * @param price price to check
     * @return {@code true} if the price is valid
     */
    private boolean isValidPrice(double price) {
        if (price < 0) {
            showError("Price cannot be negative.");
            return false;
        }
        return true;
    }

    /**
     * Checks that every dimension is greater than zero, showing an error if any is not.
     *
     * @param length proposed length
     * @param width  proposed width
     * @param height proposed height
     * @return {@code true} if all three dimensions are valid
     */
    private boolean areValidDimensions(double length, double width, double height) {
        if (length <= 0 || width <= 0 || height <= 0) {
            showError("Length, width, and height must all be greater than 0.");
            return false;
        }
        return true;
    }

    /* ------------------------- Dialogs ------------------------- */

    /**
     * Shows a modal dialog with one labeled text field per entry in {@code labels}, plus OK and
     * Cancel buttons, and waits for the user to close it.
     *
     * <p>The fields are laid out in a two-column grid (label, field) and pre-filled from
     * {@code defaults}. When OK is pressed, each field's text is returned with surrounding
     * whitespace trimmed, in the same order as {@code labels}. No validation happens here.
     *
     * @param title    window title of the dialog
     * @param labels   label for each field
     * @param defaults initial text for each field; must be the same length as {@code labels}
     * @return the entered values, or an empty {@code Optional} if the dialog was cancelled or closed
     */
    private Optional<String[]> promptFields(String title, String[] labels, String[] defaults) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        TextField[] fields = new TextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = new TextField(defaults[i]);
            grid.addRow(i, new Label(labels[i] + ":"), fields[i]);
        }
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK)
                return null;
            String[] values = new String[fields.length];
            for (int i = 0; i < fields.length; i++)
                values[i] = fields[i].getText().trim();
            return values;
        });
        return dialog.showAndWait();
    }

    /**
     * Shows a dialog where every field is numeric and parses the results.
     *
     * <p>The defaults are formatted with {@link #formatNumber}, so whole numbers appear without a
     * trailing ".0". The dialog itself comes from {@link #promptFields}, and parsing is done by
     * {@link #parseNumbers}.
     *
     * @param title    window title of the dialog
     * @param labels   label for each field
     * @param defaults initial value for each field
     * @return the parsed values, or an empty {@code Optional} if the dialog was cancelled or any field
     *         was not a valid number (an error has already been shown in that case)
     */
    private Optional<double[]> promptNumbers(String title, String[] labels, double[] defaults) {
        String[] defaultText = new String[defaults.length];
        for (int i = 0; i < defaults.length; i++)
            defaultText[i] = formatNumber(defaults[i]);

        return promptFields(title, labels, defaultText)
            .map(text -> parseNumbers(text, labels, 0));
    }

    /**
     * Parses the entries of {@code text} from index {@code start} onward as numbers.
     *
     * <p>Parsing stops at the first entry that is not a finite number: blank text, letters,
     * "NaN", and "Infinity" are all rejected. An error naming that field (using {@code labels}) is
     * shown and {@code null} is returned. Starting past index 0 lets {@link #addToSelected} skip its
     * leading name field.
     *
     * @param text   raw field values
     * @param labels field labels, used to name the offending field in the error
     * @param start  index of the first entry to parse
     * @return the parsed numbers (length {@code text.length - start}), or {@code null} on failure
     */
    private double[] parseNumbers(String[] text, String[] labels, int start) {
        double[] values = new double[text.length - start];
        for (int i = start; i < text.length; i++) {
            try {
                values[i - start] = Double.parseDouble(text[i]);
            } catch (NumberFormatException ex) {
                values[i - start] = Double.NaN;
            }
            if (!Double.isFinite(values[i - start])) {
                showError(labels[i] + " must be a number.");
                return null;
            }
        }
        return values;
    }

    /**
     * Formats a number for display. Whole numbers have no decimal part ({@code 50.0} becomes "50");
     * other values keep their decimals ({@code 12.5} stays "12.5").
     *
     * @param value number to format
     * @return its text form
     */
    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /**
     * Shows a modal error dialog with the given message and waits for the user to dismiss it.
     *
     * @param message explanation of what went wrong
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
