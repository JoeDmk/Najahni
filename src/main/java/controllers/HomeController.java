package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import models.User;
import services.*;

/**
 * Home Controller for non-admin users (Entrepreneur, Mentor, Investisseur).
 */
public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label followingCountLabel;

    private User currentUser;
    private ConnectionService connectionService = ConnectionService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionService.getInstance().setCurrentUser(user);
        updateUI();
    }

    private void updateUI() {
        if (currentUser != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + currentUser.getFullName() + " !");
            if (roleLabel != null) roleLabel.setText(currentUser.getRole().name());
            if (followersCountLabel != null)
                followersCountLabel.setText(String.valueOf(connectionService.countFollowers(currentUser.getId())));
            if (followingCountLabel != null)
                followingCountLabel.setText(String.valueOf(connectionService.countFollowing(currentUser.getId())));
        }
    }

    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent root = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNetwork() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Network.fxml"));
            Parent root = loader.load();
            NetworkController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChangePassword.fxml"));
            Parent root = loader.load();
            ChangePasswordController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        SessionService.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
