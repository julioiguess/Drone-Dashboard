import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.animation.SequentialTransition;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.ArrayList;
import java.util.List;

public class MainDashboard extends Application {
    /*
    private static class SingletonHolder {
        private static final MainDashboardOld INSTANCE = new MainDashboardOld();
    }
    private MainDashboardOld() {}

    public static MainDashboardOld getInstance() {
        return SingletonHolder.INSTANCE;
    }*/

    @Override
    public void start(Stage stage) {
        stage.setTitle("Farm Dashboard");

        GridPane panel = new GridPane();

        /* --- Start of main farmPane logic --- */
        Pane farmPane = new Pane();
        farmPane.setPrefSize(800, 600);
        farmPane.setStyle("-fx-background-color: #3ec423;");

        FarmContainer scenario = ScenarioBuilder.buildTestScenario();
        scenario.draw(farmPane);

        panel.add(farmPane, 1, 1);
        /* ---- End of main farmPane logic ---- */
        Group itemSelectionGroup = new Group();

        List<FarmItemComponent> children = scenario.getChildren();

        /* --- Start of VBox containing commands on items --- */
        VBox itemCommands = new VBox();
        List<Button> itemCommandButtons = new ArrayList<>();
        
        Button renameButton = new Button("Rename");
        Button changeLocationButton = new Button("Change location");
        Button changePriceButton = new Button("Change price");
        Button changeDimensions = new Button("Change dimensions");
        Button deleteItem = new Button("Delete");

        itemCommandButtons.add(renameButton);
        itemCommandButtons.add(changeLocationButton);
        itemCommandButtons.add(changePriceButton);
        itemCommandButtons.add(changeDimensions);
        itemCommandButtons.add(deleteItem);

        for(Button button : itemCommandButtons)
            button.setPrefWidth(150);



        itemCommands.setAlignment(Pos.CENTER);
        Label label = new Label("Commands on Items");
        itemCommands.getChildren().add(label);
        for(Button button : itemCommandButtons)
            itemCommands.getChildren().add(button);

        itemCommands.setTranslateX(25);
        itemCommands.setTranslateY(-150);
        /* --- End of VBox containing commands on items --- */

        /* --- Start of VBox containing commands on item-containers --- */
        VBox containerCommands = new VBox();
        List<Button> containerCommandButtons = new ArrayList<>();
        
        Button renameContainerButton = new Button("Rename");
        Button changeContainerLocationButton = new Button("Change location");
        Button changeContainerPriceButton = new Button("Change price");
        Button changeContainerDimensions = new Button("Change dimensions");
        Button addItem = new Button("Add item");
        Button addItemContainer = new Button("Add item-container");
        Button deleteContainer = new Button("Delete");

        containerCommandButtons.add(renameContainerButton);
        containerCommandButtons.add(changeContainerLocationButton);
        containerCommandButtons.add(changeContainerPriceButton);
        containerCommandButtons.add(changeContainerDimensions);
        containerCommandButtons.add(addItem);
        containerCommandButtons.add(addItemContainer);
        containerCommandButtons.add(deleteContainer);

        for(Button button : containerCommandButtons)
            button.setPrefWidth(150);

        itemCommands.setAlignment(Pos.CENTER);
        Label label2 = new Label("Commands on Item Containers");
        containerCommands.getChildren().add(label2);
        for(Button button : containerCommandButtons)
            containerCommands.getChildren().add(button);

        containerCommands.setTranslateX(25);
        containerCommands.setTranslateY(-350);
        /* --- End of VBox containing commands on items --- */

        FarmItem droneItem = new FarmItem("Drone", 1200.0, 570, 70, 10, 10, 5);
        for(FarmItemComponent item : children) {
            if(item.getName().equals("Command Center")) {
                item.add(droneItem);
                break;
            }
        }
        double droneStartX = droneItem.getLocationX() + droneItem.getWidth() / 2;
        double droneStartY = droneItem.getLocationY() + droneItem.getHeight() / 2;
        Circle drone = new Circle(droneStartX, droneStartY, 12, Color.BLUE);
        farmPane.getChildren().add(drone);

        ComboBox<FarmItem> itemBox = new ComboBox<>();
        for(FarmItemComponent item : children) {
            if(item.isContainer()) {
                for(FarmItemComponent child : ((FarmContainer) item).getChildren()) {
                    if(child.isItem()) {
                        itemBox.getItems().add((FarmItem) child);
                    }
                }
            }
            if(item.isItem()) {
                itemBox.getItems().add((FarmItem) item);
            }
        }

        itemSelectionGroup.getChildren().addAll(itemBox, itemCommands, containerCommands);
        panel.setStyle("-fx-background-color: #a83928;");

        if (!children.isEmpty())
            itemBox.getSelectionModel().selectFirst();

        itemBox.setPrefWidth(200);
        itemBox.setPadding(new Insets(5));
        itemBox.setTranslateY(-400);
        panel.add(itemSelectionGroup, 0, 1);

        VBox droneActions = new VBox();
        Label droneActionsLabel = new Label("Drone Actions");

        Button visitButton = new Button("Visit Item/Container");
        visitButton.setPrefWidth(150);
        Button scanButton = new Button("Scan Farm");
        scanButton.setPrefWidth(150);
        droneActions.getChildren().addAll(droneActionsLabel, visitButton, scanButton);
        droneActions.setAlignment(Pos.CENTER);
        droneActions.setTranslateX(25);
        itemSelectionGroup.getChildren().add(droneActions);
        
        visitButton.setOnAction(e -> {
            FarmItem selected = itemBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                double targetX = selected.getLocationX() + selected.getWidth() / 2;
                double targetY = selected.getLocationY() + selected.getHeight() / 2;

                double toX = targetX - droneStartX;
                double toY = targetY - droneStartY;

                TranslateTransition outTransition = new TranslateTransition(Duration.seconds(0.8), drone);
                outTransition.setToX(toX);
                outTransition.setToY(toY);

                TranslateTransition homeTransition = new TranslateTransition(Duration.seconds(0.8), drone);
                homeTransition.setToX(0);
                homeTransition.setToY(0);

                SequentialTransition seq = new SequentialTransition(outTransition, homeTransition);
                seq.play();
            }
        });

        scanButton.setOnAction(e -> {
            List<FarmItemComponent> scannedItems = new ArrayList<>();
            for(FarmItemComponent item : children) {
                if(item.isContainer()) {
                    for(FarmItemComponent child : ((FarmContainer) item).getChildren()) {
                        scannedItems.add(child);
                    }
                }
                if(item.isItem()) {
                    scannedItems.add(item);
                }
            }
            SequentialTransition scanSeq = new SequentialTransition();
            for(FarmItemComponent item : scannedItems) {
                TranslateTransition scanTransition = new TranslateTransition(Duration.seconds(0.8), drone);
                scanTransition.setToX(item.getLocationX() + item.getWidth() / 2);
                scanTransition.setToY(item.getLocationY() + item.getHeight() / 2);
                scanSeq.getChildren().add(scanTransition);
            }
            TranslateTransition homeTransition = new TranslateTransition(Duration.seconds(0.8), drone);
            homeTransition.setToX(0);
            homeTransition.setToY(0);
            scanSeq.getChildren().add(homeTransition);
            scanSeq.play();
        });

        Scene scene = new Scene(panel, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String args[]) {
        launch(args);
    }
}
