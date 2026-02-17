package test;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import tools.SceneHelper;

public class testClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();

            // Starts maximized by default. Press F11 to toggle windowed, F for true fullscreen.
            SceneHelper.initStage(primaryStage, root, "Najahni");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
