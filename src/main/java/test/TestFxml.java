package test;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import tools.SceneHelper;

public class TestFxml extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();

            SceneHelper.initStage(primaryStage, root, "Najahni - Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
