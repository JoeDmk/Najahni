package test;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import tools.SceneHelper;
import controllers.FrontOfficeShellController;
import models.User;
import services.UserService;
import services.SessionService;

public class testClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Auto-login: load a default user from the database
            User user = null;
            try {
                user = UserService.getInstance().getUserbyEmail("malek@esprit.tn");
            } catch (Exception e) {
                System.err.println("Auto-login failed, falling back to SignIn: " + e.getMessage());
            }

            if (user != null) {
                // Go directly to the FrontOffice home page
                SessionService.getInstance().setCurrentUser(user);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FrontOffice.fxml"));
                Parent root = loader.load();
                FrontOfficeShellController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
                SceneHelper.initStage(primaryStage, root, "Najahni");
            } else {
                // Fallback to login page
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
                Parent root = loader.load();
                SceneHelper.initStage(primaryStage, root, "Najahni");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
