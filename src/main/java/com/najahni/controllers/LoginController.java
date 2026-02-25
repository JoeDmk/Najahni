package com.najahni.controllers;

import com.najahni.MainApp;
import com.najahni.models.Role;
import com.najahni.models.User;
import com.najahni.services.SessionManager;
import com.najahni.services.UserService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Contrôleur de la page de connexion.
 * Route automatiquement :
 * - ADMIN / ENTREPRENEUR / MENTOR → Back-office (MainView)
 * - INVESTOR → Front-office (FrontOfficeView)
 */
public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    private final UserService userService;

    public LoginController() {
        this.userService = new UserService();
    }

    @FXML
    public void initialize() {
        // Enter key triggers login
        txtPassword.setOnAction(e -> handleLogin());
        txtEmail.setOnAction(e -> txtPassword.requestFocus());
    }

    @FXML
    public void handleLogin() {
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        // ── Validation ──
        if (email.isEmpty()) {
            showError("Veuillez saisir votre email.");
            return;
        }
        if (password.isEmpty()) {
            showError("Veuillez saisir votre mot de passe.");
            return;
        }

        btnLogin.setDisable(true);
        lblError.setText("");

        try {
            Optional<User> userOpt = userService.authenticate(email, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Store in session
                SessionManager.getInstance().login(user);
                System.out.println("✓ Connexion réussie : " + user.getName() + " [" + user.getRole() + "]");

                // Route based on role
                if (user.getRole() == Role.INVESTOR) {
                    navigateTo("/fxml/FrontOfficeView.fxml", "NAJAHNI — Espace Investisseur");
                } else if (user.getRole() == Role.ENTREPRENEUR) {
                    navigateTo("/fxml/FrontOfficeView.fxml", "NAJAHNI — Espace Entrepreneur");
                } else {
                    navigateTo("/fxml/MainView.fxml", "NAJAHNI — Back-Office");
                }
            } else {
                showError("Email ou mot de passe incorrect.");
                btnLogin.setDisable(false);
            }
        } catch (Exception e) {
            showError("Erreur de connexion : " + e.getMessage());
            btnLogin.setDisable(false);
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        // Shake animation
        FadeTransition fade = new FadeTransition(Duration.millis(100), lblError);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = MainApp.getPrimaryStage();
            Scene scene = new Scene(root, 1200, 700);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);

            // Fade-in effect
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
