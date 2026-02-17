package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tools.SceneHelper;
import services.*;

/**
 * Controller for resetting password after code verification.
 */
public class ChangeMdpController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleReset() {
        String newPass = newPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (newPass.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (!newPass.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        ValidationService validationService = new ValidationService();
        if (!validationService.isValidPassword(newPass)) {
            showError("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et 6 caractères.");
            return;
        }

        try {
            PasswordResetService.getInstance().changePassword(newPass, email);

            // Unlock the account
            SessionService.getInstance().unlockAccount(email);

            // Send confirmation email
            EmailService emailService = new EmailService();
            emailService.sendPasswordChangeEmail(email);

            showSuccess("Mot de passe réinitialisé avec succès !");

            // Redirect to sign in after 2 seconds
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(e -> redirectToSignIn());
            pause.play();

        } catch (Exception e) {
            showError("Erreur lors de la réinitialisation.");
            e.printStackTrace();
        }
    }

    private void redirectToSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(newPasswordField), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        redirectToSignIn();
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
