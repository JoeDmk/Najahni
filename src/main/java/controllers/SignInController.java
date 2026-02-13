package controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.User;
import exceptions.*;
import services.*;
import util.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SignInController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private CheckBox chkRemember;
    @FXML private Label lblError;
    @FXML private Hyperlink linkForgotPassword;

    private UserService userService = UserService.getInstance();
    private SessionService sessionService = SessionService.getInstance();
    private Map<String, Integer> loginAttemptsMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Check existing session
        String[] session = SessionManager.loadSession();
        if (session != null && session.length == 3) {
            String email = session[0];
            LocalDateTime lastLogin = LocalDateTime.parse(session[2]);

            if (lastLogin.isAfter(LocalDateTime.now().minusHours(24))) {
                try {
                    User user = userService.getUserbyEmail(email);
                    if (user != null) {
                        final User finalUser = user;
                        javafx.application.Platform.runLater(() -> redirectToHome(finalUser));
                        return;
                    }
                } catch (UserNotFoundException e) {
                    // Session invalid
                }
            }
        }
    }

    private void redirectToHome(User user) {
        try {
            FXMLLoader loader;
            if (user.getRole() == Type.ADMIN) {
                loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
            }
            Parent root = loader.load();

            if (user.getRole() == Type.ADMIN) {
                DashboardController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            } else {
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
            }

            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            System.err.println("Error redirecting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        try {
            if (sessionService.isAccountLocked(email)) {
                throw new AccountLockedException("Votre compte est banni. Réinitialisez votre mot de passe.");
            }

            User user = userService.getUserbyEmail(email);

            if (user.getRole() != Type.ADMIN && user.getIsBanned()) {
                throw new AccountLockedException("Votre compte est banni.");
            }

            if (userService.verifyPassword(password, user.getPassword())) {
                sessionService.setCurrentUser(user);
                SessionManager.saveSession(user.getEmail(), user.getRole().name());
                loginAttemptsMap.put(email, 0);
                redirectToHome(user);
            } else {
                int attempts = loginAttemptsMap.getOrDefault(email, 0) + 1;
                loginAttemptsMap.put(email, attempts);

                if (attempts >= sessionService.MAX_LOGIN_ATTEMPTS) {
                    if (user.getRole() != Type.ADMIN) {
                        sessionService.lockAccount(email);
                        showError("Trop de tentatives. Votre compte est verrouillé.");
                    } else {
                        showError("Trop de tentatives.");
                    }
                } else {
                    showError("Email ou mot de passe incorrect. Tentatives restantes: " +
                            (sessionService.MAX_LOGIN_ATTEMPTS - attempts));
                }
            }
        } catch (AccountLockedException e) {
            showError(e.getMessage());
        } catch (UserNotFoundException e) {
            showError("Aucun utilisateur trouvé avec cet email.");
        } catch (Exception e) {
            showError("Erreur lors de la connexion.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            showError("Veuillez saisir votre email d'abord.");
            return;
        }

        try {
            userService.getUserbyEmail(email); // Verify user exists
            PasswordResetService resetService = PasswordResetService.getInstance();
            String code = resetService.generateAndStoreCode(email);

            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(email, code);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RandomCode.fxml"));
            Parent root = loader.load();
            RandomCodeController ctrl = loader.getController();
            ctrl.setEmail(email);
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (UserNotFoundException e) {
            showError("Aucun utilisateur trouvé avec cet email.");
        } catch (Exception e) {
            showError("Erreur lors de l'envoi du code.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDemoLogin() {
        // Quick demo login - tries to log in as admin
        try {
            User admin = userService.getUserbyEmail("admin@najahni.tn");
            if (admin != null) {
                sessionService.setCurrentUser(admin);
                SessionManager.saveSession(admin.getEmail(), admin.getRole().name());
                redirectToHome(admin);
            }
        } catch (UserNotFoundException e) {
            showError("Aucun compte admin démo trouvé. Créez un admin d'abord.");
        } catch (Exception e) {
            showError("Erreur connexion démo.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignUp.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            showError("Impossible de charger la page d'inscription.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(event -> lblError.setVisible(false));
        pause.play();
    }
}
