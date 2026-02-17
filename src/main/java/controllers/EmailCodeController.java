package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
import tools.SceneHelper;
import services.*;

/**
 * Email verification code controller.
 */
public class EmailCodeController {

    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Label infoLabel;

    private User user;
    private String verificationCode;

    public void setUserAndCode(User user, String verificationCode) {
        this.user = user;
        this.verificationCode = verificationCode;
        if (infoLabel != null) {
            infoLabel.setText("Un code de vérification a été envoyé à " + user.getEmail());
        }
    }

    @FXML
    private void handleVerify() {
        String enteredCode = codeField.getText().trim();

        if (enteredCode.isEmpty()) {
            showError("Veuillez saisir le code de vérification.");
            return;
        }

        if (enteredCode.equals(verificationCode)) {
            try {
                UserService.getInstance().verifyUser(user.getId());
                SessionManager.saveSession(user.getEmail(), user.getRole().name());
                SessionService.getInstance().setCurrentUser(user);

                // Redirect to home
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
                Parent root = loader.load();
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
                SceneHelper.switchScene(SceneHelper.stageOf(codeField), root);
            } catch (Exception e) {
                showError("Erreur lors de la vérification.");
                e.printStackTrace();
            }
        } else {
            showError("Code incorrect. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleResend() {
        try {
            verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);
            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(user.getEmail(), verificationCode);
            if (infoLabel != null) {
                infoLabel.setText("Nouveau code envoyé à " + user.getEmail());
                infoLabel.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            showError("Erreur lors du renvoi du code.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(codeField), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Skip verification — log in without verifying email.
     * The user's email will show as "Non vérifié" and can be verified later from the profile.
     */
    @FXML
    private void handleSkip() {
        try {
            SessionManager.saveSession(user.getEmail(), user.getRole().name());
            SessionService.getInstance().setCurrentUser(user);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(user);
            SceneHelper.switchScene(SceneHelper.stageOf(codeField), root);
        } catch (Exception e) {
            showError("Erreur lors de la redirection.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
    }
}
