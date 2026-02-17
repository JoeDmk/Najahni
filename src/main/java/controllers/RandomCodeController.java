package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tools.SceneHelper;
import services.*;

/**
 * Random code verification for password reset.
 */
public class RandomCodeController {

    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Label infoLabel;

    private String email;

    public void setEmail(String email) {
        this.email = email;
        if (infoLabel != null) {
            infoLabel.setText("Un code de vérification a été envoyé à " + email);
        }
    }

    // Legacy compatibility
    public void setPhoneNumber(String phone) {
        // No longer using phone-based reset, using email instead
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            showError("Veuillez saisir le code.");
            return;
        }

        PasswordResetService resetService = PasswordResetService.getInstance();
        if (resetService.verifyCode(email, code)) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChangeMdp.fxml"));
                Parent root = loader.load();
                ChangeMdpController ctrl = loader.getController();
                ctrl.setEmail(email);
                SceneHelper.switchScene(SceneHelper.stageOf(codeField), root);
            } catch (Exception e) {
                showError("Erreur de redirection.");
                e.printStackTrace();
            }
        } else {
            showError("Code incorrect ou expiré.");
        }
    }

    @FXML
    private void handleResend() {
        PasswordResetService resetService = PasswordResetService.getInstance();
        String newCode = resetService.generateAndStoreCode(email);
        EmailService emailService = new EmailService();
        emailService.sendVerificationEmail(email, newCode);
        if (infoLabel != null) {
            infoLabel.setText("Nouveau code envoyé à " + email);
            infoLabel.setStyle("-fx-text-fill: green;");
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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
    }
}
