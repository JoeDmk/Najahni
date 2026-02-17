package controllers;

import exceptions.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
import tools.SceneHelper;
import services.*;

/**
 * Change password controller.
 */
public class ChangePasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private User currentUser;
    private UserService userService = UserService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleChangePassword() {
        String current = currentPasswordField.getText().trim();
        String newPass = newPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!newPass.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (!userService.verifyPassword(current, currentUser.getPassword())) {
            showError("Mot de passe actuel incorrect.");
            return;
        }

        try {
            userService.updatePassword(currentUser.getId(), newPass);

            // Send confirmation email
            EmailService emailService = new EmailService();
            emailService.sendPasswordChangeEmail(currentUser.getEmail());

            showSuccess("Mot de passe changé avec succès !");
        } catch (EmptyFieldException | IncorrectPasswordException | UserNotFoundException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent root = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(currentPasswordField), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
        if (successLabel != null) successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setVisible(true);
            successLabel.setStyle("-fx-text-fill: green;");
        }
        errorLabel.setVisible(false);
    }
}
