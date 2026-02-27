package controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import tools.CaptchaGenerator;
import tools.SceneHelper;
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
    @FXML private HBox captchaBox;            // The clickable "I'm not a robot" panel
    @FXML private CheckBox chkCaptcha;        // The checkbox inside the panel
    @FXML private Label lblCaptchaStatus;     // Status label (shows ✓ Vérifié or instructions)
    @FXML private Button btnGoogle;            // "Continue with Google" button
    @FXML private Button btnFaceLogin;         // "Face ID" login button

    private UserService userService = UserService.getInstance();
    private SessionService sessionService = SessionService.getInstance();
    private Map<String, Integer> loginAttemptsMap = new HashMap<>();
    private CaptchaGenerator captchaGenerator = new CaptchaGenerator(); // CAPTCHA engine
    private GoogleOAuthService googleOAuthService = new GoogleOAuthService(); // Google Sign-In

    @FXML
    public void initialize() {
        // Set Google "G" logo on the Google button
        setupGoogleButtonGraphic();

        // Check existing session in background thread to avoid blocking UI
        Task<User> sessionTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                String[] session = SessionManager.loadSession();
                if (session != null && session.length == 3) {
                    String email = session[0];
                    LocalDateTime lastLogin = LocalDateTime.parse(session[2]);
                    if (lastLogin.isAfter(LocalDateTime.now().minusHours(24))) {
                        try {
                            return userService.getUserbyEmail(email);
                        } catch (Exception e) {
                            // Session invalid
                        }
                    }
                }
                return null;
            }
        };
        sessionTask.setOnSucceeded(e -> {
            User user = sessionTask.getValue();
            if (user != null) {
                redirectToHome(user);
            }
        });
        Thread sessionThread = new Thread(sessionTask);
        sessionThread.setDaemon(true);
        sessionThread.start();
    }

    private void setupGoogleButtonGraphic() {
        if (btnGoogle == null) return;

        // Use a local "G" label instead of downloading from the internet
        Label googleIcon = new Label("G");
        googleIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4285F4; " +
                "-fx-min-width: 20; -fx-min-height: 20; -fx-alignment: center;");

        btnGoogle.setGraphic(googleIcon);
        btnGoogle.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btnGoogle.setAlignment(javafx.geometry.Pos.CENTER);
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

            SceneHelper.switchScene(SceneHelper.stageOf(txtEmail), root);
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

        // --- CAPTCHA VALIDATION ---
        if (!captchaGenerator.isVerified()) {
            showError("Veuillez cocher \"Je ne suis pas un robot\".");
            return;
        }

        // Disable login button to prevent double-click
        btnLogin.setDisable(true);

        // Run all DB + bcrypt work on a background thread
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
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

                    // Record successful login
                    LoginHistoryService.getInstance().recordLogin(user.getId(), "PASSWORD", true);

                    // Suspicious login detection (background - don't block redirect)
                    SuspiciousLoginService suspiciousService = SuspiciousLoginService.getInstance();
                    int riskScore = suspiciousService.analyzeLogin(user);
                    if (riskScore >= 30) {
                        suspiciousService.handleSuspiciousLogin(user, riskScore);
                    }

                    // Load user preferences
                    ThemeService.getInstance().loadPreference(user.getPreferredTheme());

                    return user;
                } else {
                    int attempts = loginAttemptsMap.getOrDefault(email, 0) + 1;
                    loginAttemptsMap.put(email, attempts);

                    // Record failed login attempt
                    try {
                        LoginHistoryService.getInstance().recordLogin(user.getId(), "PASSWORD", false);
                    } catch (Exception ignored) {}

                    if (attempts >= sessionService.MAX_LOGIN_ATTEMPTS) {
                        if (user.getRole() != Type.ADMIN) {
                            sessionService.lockAccount(email);
                            throw new AccountLockedException("Trop de tentatives. Votre compte est verrouillé.");
                        } else {
                            throw new IncorrectPasswordException("Trop de tentatives.");
                        }
                    } else {
                        throw new IncorrectPasswordException("Email ou mot de passe incorrect. Tentatives restantes: " +
                                (sessionService.MAX_LOGIN_ATTEMPTS - attempts));
                    }
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            btnLogin.setDisable(false);
            User user = loginTask.getValue();
            if (user != null) {
                loginAttemptsMap.put(email, 0);
                redirectToHome(user);
            }
        });

        loginTask.setOnFailed(e -> {
            btnLogin.setDisable(false);
            Throwable ex = loginTask.getException();
            if (ex instanceof AccountLockedException) {
                showError(ex.getMessage());
            } else if (ex instanceof UserNotFoundException) {
                showError("Aucun utilisateur trouvé avec cet email.");
            } else if (ex instanceof IncorrectPasswordException) {
                showError(ex.getMessage());
            } else {
                showError("Erreur lors de la connexion.");
                ex.printStackTrace();
            }
        });

        Thread loginThread = new Thread(loginTask);
        loginThread.setDaemon(true);
        loginThread.start();
    }

    @FXML
    private void handleForgotPassword() {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            showError("Veuillez saisir votre email d'abord.");
            return;
        }

        Task<String> resetTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                userService.getUserbyEmail(email); // Verify user exists
                PasswordResetService resetService = PasswordResetService.getInstance();
                String code = resetService.generateAndStoreCode(email);
                new EmailService().sendVerificationEmail(email, code);
                return code;
            }
        };

        resetTask.setOnSucceeded(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RandomCode.fxml"));
                Parent root = loader.load();
                RandomCodeController ctrl = loader.getController();
                ctrl.setEmail(email);
                SceneHelper.switchScene(SceneHelper.stageOf(txtEmail), root);
            } catch (Exception ex) {
                showError("Erreur lors du chargement de la page.");
                ex.printStackTrace();
            }
        });

        resetTask.setOnFailed(e -> {
            Throwable ex = resetTask.getException();
            if (ex instanceof UserNotFoundException) {
                showError("Aucun utilisateur trouvé avec cet email.");
            } else {
                showError("Erreur lors de l'envoi du code.");
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(resetTask);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Called when the user clicks the "I'm not a robot" checkbox.
     *
     * FLOW:
     * 1. User clicks the checkbox → this handler fires.
     * 2. A dialog pops up with a simple math question (e.g. "7 + 4 = ?").
     * 3. If answered correctly → checkbox stays checked, panel turns green, "✓ Vérifié" shown.
     * 4. If wrong or cancelled → checkbox unchecked, panel turns red briefly, then resets.
     * 5. The login button checks captchaGenerator.isVerified() before proceeding.
     */
    @FXML
    private void handleCaptchaClick() {
        if (chkCaptcha.isSelected()) {
            // User just checked the box — show the math challenge
            boolean passed = captchaGenerator.showChallengeDialog();

            if (passed) {
                // SUCCESS: mark as verified, show green state
                chkCaptcha.setSelected(true);
                chkCaptcha.setDisable(true); // Lock it so they can't uncheck
                lblCaptchaStatus.setText("✓ Vérifié");
                lblCaptchaStatus.setStyle("-fx-text-fill: #00B894; -fx-font-weight: bold; -fx-font-size: 13px;");
                captchaBox.setStyle("-fx-background-color: #F0FFF4; -fx-border-color: #00B894; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 16; -fx-cursor: hand;");
            } else {
                // FAILED: uncheck, show red flash, reset
                chkCaptcha.setSelected(false);
                captchaGenerator.reset();
                lblCaptchaStatus.setText("✗ Incorrect, réessayez");
                lblCaptchaStatus.setStyle("-fx-text-fill: #E17055; -fx-font-weight: bold; -fx-font-size: 13px;");
                captchaBox.setStyle("-fx-background-color: #FFF5F5; -fx-border-color: #E17055; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 16; -fx-cursor: hand;");

                // Reset to default style after 2 seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> {
                    lblCaptchaStatus.setText("Je ne suis pas un robot");
                    lblCaptchaStatus.setStyle("-fx-text-fill: #2D3436; -fx-font-size: 13px;");
                    captchaBox.setStyle(null);  // Reset to CSS class defaults
                    captchaBox.getStyleClass().setAll("captcha-box");
                });
                pause.play();
            }
        } else {
            // User unchecked manually — reset verification
            captchaGenerator.reset();
            lblCaptchaStatus.setText("Je ne suis pas un robot");
            lblCaptchaStatus.setStyle("-fx-text-fill: #2D3436; -fx-font-size: 13px;");
            captchaBox.setStyle(null);
            captchaBox.getStyleClass().setAll("captcha-box");
        }
    }

    /**
     * Handles "Continue with Google" button click.
     *
     * FLOW:
     * 1. Opens a WebView popup showing Google's sign-in page.
     * 2. User logs into their Google account and grants permission.
     * 3. Google redirects to our local callback server with an auth code.
     * 4. We exchange the code for an access token.
     * 5. We fetch the user's Google profile (email, name, picture).
     * 6. We check if a user with that Google ID or email already exists:
     *    - If YES → log them in directly (skip password/captcha).
     *    - If NO  → auto-create a new ENTREPRENEUR account with their Google info.
     * 7. Redirect to the appropriate dashboard.
     */
    @FXML
    private void handleGoogleLogin() {
        javafx.stage.Stage currentStage = (javafx.stage.Stage) txtEmail.getScene().getWindow();

        googleOAuthService.signIn(currentStage).thenAccept(userInfo -> {
            Platform.runLater(() -> {
                if (userInfo == null) {
                    showError("Connexion Google annulée ou échouée.");
                    return;
                }

                try {
                    // Find existing user or create a new one from Google profile
                    User user = userService.findOrCreateGoogleUser(
                            userInfo.getGoogleId(),
                            userInfo.getEmail(),
                            userInfo.getFirstName(),
                            userInfo.getLastName(),
                            userInfo.getPictureUrl()
                    );

                    if (user == null) {
                        showError("Erreur lors de la connexion avec Google.");
                        return;
                    }

                    // Check if the user is banned
                    if (user.getRole() != Type.ADMIN && user.getIsBanned()) {
                        showError("Votre compte est banni.");
                        return;
                    }

                    // Success! Set session and redirect
                    sessionService.setCurrentUser(user);
                    SessionManager.saveSession(user.getEmail(), user.getRole().name());

                    // Record Google login
                    LoginHistoryService.getInstance().recordLogin(user.getId(), "GOOGLE", true);

                    // Load user preferences
                    ThemeService.getInstance().loadPreference(user.getPreferredTheme());

                    System.out.println("Google login successful for: " + user.getEmail());
                    redirectToHome(user);

                } catch (Exception e) {
                    showError("Erreur lors de la connexion avec Google.");
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Handles "Face ID" login button click.
     * Opens the FaceLogin.fxml window which starts the webcam and recognition process.
     */
    @FXML
    private void handleFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FaceLogin.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(txtEmail), root);
        } catch (Exception e) {
            showError("Impossible d'ouvrir la connexion Face ID.");
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
            SceneHelper.switchScene(SceneHelper.stageOf(txtEmail), root);
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
