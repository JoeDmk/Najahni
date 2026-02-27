package tools;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * Utility for scene transitions with fullscreen / windowed mode support.
 * <ul>
 *   <li><b>F11</b> toggles between maximized and windowed mode.</li>
 *   <li><b>F</b>   toggles true fullscreen (no window chrome).</li>
 *   <li><b>Escape</b> exits true fullscreen back to maximized.</li>
 * </ul>
 */
public final class SceneHelper {

    /** Default windowed size */
    private static final double DEFAULT_WIDTH  = 1200;
    private static final double DEFAULT_HEIGHT = 750;

    /** Whether the app should start maximized (true) or windowed (false). */
    private static boolean startMaximized = true;

    private SceneHelper() {}

    /* ------------------------------------------------------------ */
    /*  Public API                                                    */
    /* ------------------------------------------------------------ */

    /** Change the default start mode before the first scene is shown. */
    public static void setStartMaximized(boolean maximized) {
        startMaximized = maximized;
    }

    /**
     * Switch the scene of {@code stage} to show {@code root}.
     * Reuses the existing Scene object when possible to preserve stylesheets
     * and avoid unnecessary Scene creation overhead.
     */
    public static void switchScene(Stage stage, Parent root) {
        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();

        Scene scene = stage.getScene();
        if (scene != null) {
            // Reuse existing scene — preserves stylesheets and avoids GC overhead
            scene.setRoot(root);
        } else {
            scene = new Scene(root);
            stage.setScene(scene);
        }
        stage.setResizable(true);

        if (wasFullScreen) {
            stage.setFullScreen(true);
        } else if (wasMaximized) {
            stage.setMaximized(true);
        }

        wireKeyboardShortcuts(stage, scene);
        stage.show();
    }

    /**
     * Initial setup for the very first scene (called from Application.start).
     * Applies the configured start mode.
     */
    public static void initStage(Stage stage, Parent root, String title) {
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(true);

        if (startMaximized) {
            stage.setMaximized(true);
        }

        wireKeyboardShortcuts(stage, scene);
        stage.show();
    }

    /** Convenience: obtain the current Stage from any node in the scene graph. */
    public static Stage stageOf(javafx.scene.Node node) {
        return (Stage) node.getScene().getWindow();
    }

    /* ------------------------------------------------------------ */
    /*  Private helpers                                               */
    /* ------------------------------------------------------------ */

    /**
     * Wire keyboard shortcuts on the given scene:
     *   F11  → toggle maximized ↔ windowed
     *   F    → toggle true fullscreen
     *   ESC  → exit true fullscreen (JavaFX default, but we make it explicit)
     */
    private static void wireKeyboardShortcuts(Stage stage, Scene scene) {
        // Allow ESC to exit fullscreen without closing
        stage.setFullScreenExitKeyCombination(KeyCombination.keyCombination("Escape"));

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                // Toggle maximized ↔ windowed
                if (stage.isFullScreen()) {
                    stage.setFullScreen(false);
                    stage.setMaximized(false);
                    stage.setWidth(DEFAULT_WIDTH);
                    stage.setHeight(DEFAULT_HEIGHT);
                    stage.centerOnScreen();
                } else {
                    stage.setMaximized(!stage.isMaximized());
                    if (!stage.isMaximized()) {
                        stage.setWidth(DEFAULT_WIDTH);
                        stage.setHeight(DEFAULT_HEIGHT);
                        stage.centerOnScreen();
                    }
                }
            } else if (event.getCode() == KeyCode.F && !event.isControlDown() && !event.isAltDown()
                       && !(scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl)) {
                // Toggle true fullscreen (only when not typing in a text field)
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }
}
