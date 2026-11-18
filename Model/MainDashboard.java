package model;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the farm dashboard.
 *
 * <p>JavaFX creates this class itself through its public constructor, so it cannot be a
 * Singleton. Instead, it hands the window JavaFX provides to the single {@link Dashboard}
 * instance, which builds and owns the whole UI.
 */
public class MainDashboard extends Application {
    /**
     * Called by JavaFX once the toolkit is running. Shows the single dashboard in the primary window.
     *
     * @param stage the primary window supplied by JavaFX
     */
    @Override
    public void start(Stage stage) {
        Dashboard.getInstance().show(stage);
    }

    /**
     * Entry point. Hands control to JavaFX, which creates this application and calls
     * {@link #start(Stage)}.
     *
     * @param args command-line arguments passed through to JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }
}