package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import models.User;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import services.*;
import tools.SceneHelper;
import util.Type;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for the Face ID login screen.
 *
 * FLOW:
 * 1. Opens webcam with live preview
 * 2. Continuously detects faces in each frame
 * 3. When a face is found, attempts recognition against all registered users
 * 4. If matched with high confidence → auto-login
 * 5. Animated feedback: green glow on success, red on failure
 */
public class FaceLoginController {

    @FXML private ImageView webcamView;
    @FXML private StackPane webcamContainer;
    @FXML private Rectangle scanFrame;
    @FXML private Label lblInstruction;
    @FXML private Label lblStatus;
    @FXML private Label lblNoCamera;
    @FXML private Label lblError;
    @FXML private Label lblConfidence;
    @FXML private Label lblMatchName;
    @FXML private Label lblMatchEmail;
    @FXML private ProgressBar progressBar;
    @FXML private HBox confidenceBox;
    @FXML private VBox matchResultBox;
    @FXML private Button btnRetry;

    private FaceRecognitionService faceService;
    private UserService userService;
    private SessionService sessionService;
    private VideoCapture camera;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean recognized = new AtomicBoolean(false);
    private Thread webcamThread;
    private Timeline scanAnimation;

    // Consecutive match counter for stability
    private int consecutiveMatches = 0;
    private int lastMatchedUserId = -1;
    private static final int REQUIRED_CONSECUTIVE_MATCHES = 5;

    @FXML
    public void initialize() {
        faceService = FaceRecognitionService.getInstance();
        userService = UserService.getInstance();
        sessionService = SessionService.getInstance();

        // Start scanning animation on the frame border
        startScanAnimation();

        // Start webcam in a background thread
        Platform.runLater(this::startWebcam);
    }

    // ==================== Webcam & Recognition Loop ====================

    private void startWebcam() {
        camera = faceService.openWebcam();
        if (camera == null) {
            Platform.runLater(() -> {
                lblNoCamera.setVisible(true);
                lblStatus.setText("Caméra non disponible");
                lblStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 14px;");
                showError("Impossible d'ouvrir la caméra. Vérifiez qu'elle est connectée.");
            });
            return;
        }

        // Pre-load the global face model
        boolean modelLoaded = faceService.loadGlobalModel();
        if (!modelLoaded) {
            Platform.runLater(() -> {
                lblStatus.setText("Aucun visage enregistré");
                lblStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 14px;");
                showError("Aucun utilisateur n'a enregistré son visage. Enregistrez-vous d'abord depuis votre profil.");
            });
            releaseCamera();
            return;
        }

        running.set(true);
        recognized.set(false);
        consecutiveMatches = 0;
        lastMatchedUserId = -1;

        Platform.runLater(() -> {
            lblStatus.setText("Recherche de visage...");
            lblStatus.setStyle("-fx-text-fill: #6C63FF; -fx-font-weight: bold; -fx-font-size: 14px;");
        });

        webcamThread = new Thread(() -> {
            int emptyFrameCount = 0;
            final int MAX_EMPTY_FRAMES = 60; // ~3 seconds of failures → give up

            while (running.get() && !recognized.get()) {
                try {
                    Mat frame = faceService.captureFrame(camera);
                    if (frame.empty()) {
                        frame.close();
                        emptyFrameCount++;
                        if (emptyFrameCount >= MAX_EMPTY_FRAMES) {
                            Platform.runLater(() -> {
                                showError("La caméra ne renvoie aucune image. Vérifiez qu'aucune autre application ne l'utilise.");
                                btnRetry.setVisible(true);
                                lblStatus.setText("Erreur caméra");
                                lblStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 14px;");
                            });
                            running.set(false);
                            break;
                        }
                        Thread.sleep(50);
                        continue;
                    }
                    emptyFrameCount = 0; // Reset on successful frame

                    // Detect faces
                    RectVector faces = faceService.detectFaces(frame);
                    int faceCount = (int) faces.size();

                    if (faceCount > 0) {
                        // Draw green rectangle around detected face
                        Scalar green = new Scalar(0, 255, 0, 255);
                        faceService.drawFaceRectangles(frame, faces, green);

                        // Try to recognize the first detected face
                        Rect faceRect = faces.get(0);
                        Mat faceROI = faceService.extractFaceROI(frame, faceRect);

                        FaceRecognitionService.RecognitionResult result = faceService.recognizeFace(faceROI);
                        faceROI.close();

                        if (result.isMatched()) {
                            // Check if same user matched consecutively
                            if (result.getUserId() == lastMatchedUserId) {
                                consecutiveMatches++;
                            } else {
                                consecutiveMatches = 1;
                                lastMatchedUserId = result.getUserId();
                            }

                            int confPercent = result.getConfidencePercent();
                            double progress = (double) consecutiveMatches / REQUIRED_CONSECUTIVE_MATCHES;

                            Platform.runLater(() -> {
                                lblStatus.setText("Visage détecté — Vérification...");
                                lblStatus.setStyle("-fx-text-fill: #00B894; -fx-font-weight: bold; -fx-font-size: 14px;");
                                confidenceBox.setVisible(true);
                                lblConfidence.setText(confPercent + "%");
                                progressBar.setVisible(true);
                                progressBar.setProgress(Math.min(progress, 1.0));
                                progressBar.setStyle("-fx-accent: #00B894;");
                                scanFrame.setStroke(Color.web("#00B894"));
                            });

                            // If enough consecutive matches → login!
                            if (consecutiveMatches >= REQUIRED_CONSECUTIVE_MATCHES) {
                                recognized.set(true);
                                final int matchedUserId = result.getUserId();
                                final int matchConfidence = confPercent;

                                // Draw thick green rectangle for final match
                                Scalar brightGreen = new Scalar(0, 255, 100, 255);
                                faceService.drawFaceRectangles(frame, faces, brightGreen);

                                // Update UI with final frame
                                javafx.scene.image.Image fxImage = faceService.matToJavaFXImage(frame);
                                Platform.runLater(() -> {
                                    webcamView.setImage(fxImage);
                                    handleSuccessfulRecognition(matchedUserId, matchConfidence);
                                });
                            }
                        } else {
                            // Face detected but not recognized
                            consecutiveMatches = 0;
                            lastMatchedUserId = -1;

                            // Draw orange rectangle for unknown face
                            Scalar orange = new Scalar(0, 165, 255, 255);
                            faceService.drawFaceRectangles(frame, faces, orange);

                            Platform.runLater(() -> {
                                lblStatus.setText("Visage non reconnu");
                                lblStatus.setStyle("-fx-text-fill: #E17055; -fx-font-weight: bold; -fx-font-size: 14px;");
                                progressBar.setProgress(0);
                                scanFrame.setStroke(Color.web("#E17055"));
                            });
                        }

                        green.close();
                    } else {
                        // No face detected
                        consecutiveMatches = 0;
                        lastMatchedUserId = -1;

                        Platform.runLater(() -> {
                            lblStatus.setText("Recherche de visage...");
                            lblStatus.setStyle("-fx-text-fill: #6C63FF; -fx-font-weight: bold; -fx-font-size: 14px;");
                            confidenceBox.setVisible(false);
                            progressBar.setVisible(false);
                            progressBar.setProgress(0);
                            scanFrame.setStroke(Color.web("#6C63FF"));
                        });
                    }

                    // Update webcam view
                    javafx.scene.image.Image fxImage = faceService.matToJavaFXImage(frame);
                    if (fxImage != null && !recognized.get()) {
                        Platform.runLater(() -> webcamView.setImage(fxImage));
                    }

                    frame.close();
                    faces.close();

                    // Small delay to avoid hogging CPU
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Webcam loop error: " + e.getMessage());
                }
            }
        }, "FaceLogin-Webcam");
        webcamThread.setDaemon(true);
        webcamThread.start();
    }

    // ==================== Success Handler ====================

    private void handleSuccessfulRecognition(int userId, int confidence) {
        running.set(false);

        try {
            User user = userService.getUserbyID(userId);

            // Check if banned
            if (user.getRole() != Type.ADMIN && user.getIsBanned()) {
                showError("Votre compte est banni.");
                btnRetry.setVisible(true);
                return;
            }

            // Animate success
            scanFrame.setStroke(Color.web("#00B894"));
            scanFrame.setStrokeWidth(5);
            webcamContainer.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16; " +
                    "-fx-border-color: #00B894; -fx-border-width: 3; -fx-border-radius: 16;");

            // Stop scan animation
            if (scanAnimation != null) scanAnimation.stop();

            // Show match result
            lblStatus.setText("✓ Identifié avec succès !");
            lblStatus.setStyle("-fx-text-fill: #00B894; -fx-font-weight: bold; -fx-font-size: 16px;");

            matchResultBox.setVisible(true);
            lblMatchName.setText(user.getFullName());
            lblMatchEmail.setText(user.getEmail());

            progressBar.setProgress(1.0);
            progressBar.setStyle("-fx-accent: #00B894;");
            lblConfidence.setText(confidence + "%");

            // Auto-login after a brief success animation
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                releaseCamera();

                // Record login history
                try {
                    LoginHistoryService.getInstance().recordLogin(user.getId(), "FACE_ID", true);
                } catch (Exception ex) {
                    System.err.println("Failed to record login history: " + ex.getMessage());
                }

                // Check suspicious login
                try {
                    SuspiciousLoginService suspiciousService = SuspiciousLoginService.getInstance();
                    int riskScore = suspiciousService.analyzeLogin(user);
                    if (riskScore >= 30) {
                        suspiciousService.handleSuspiciousLogin(user, riskScore);
                    }
                } catch (Exception ex) {
                    System.err.println("Suspicious login check failed: " + ex.getMessage());
                }

                // Load theme/language preferences
                try {
                    ThemeService.getInstance().loadPreference(user.getPreferredTheme());
                    LanguageService.getInstance().setLanguage(user.getPreferredLanguage());
                } catch (Exception ex) {
                    System.err.println("Failed to load preferences: " + ex.getMessage());
                }

                // Set session
                sessionService.setCurrentUser(user);
                SessionManager.saveSession(user.getEmail(), user.getRole().name());
                System.out.println("Face ID login successful for: " + user.getEmail());

                // Redirect
                redirectToHome(user);
            });
            pause.play();

        } catch (Exception e) {
            showError("Erreur lors de la connexion: " + e.getMessage());
            btnRetry.setVisible(true);
        }
    }

    // ==================== Animation ====================

    private void startScanAnimation() {
        if (scanFrame == null) return;

        // Pulsing opacity animation on the scan frame
        scanAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scanFrame.opacityProperty(), 0.4)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(scanFrame.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(scanFrame.opacityProperty(), 0.4))
        );
        scanAnimation.setCycleCount(Timeline.INDEFINITE);
        scanAnimation.play();
    }

    // ==================== Navigation ====================

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

            SceneHelper.switchScene(SceneHelper.stageOf(webcamView), root);
        } catch (Exception e) {
            System.err.println("Error redirecting after Face ID login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        stopAndCleanup();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(webcamView), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRetry() {
        recognized.set(false);
        consecutiveMatches = 0;
        lastMatchedUserId = -1;
        btnRetry.setVisible(false);
        matchResultBox.setVisible(false);
        confidenceBox.setVisible(false);
        progressBar.setVisible(false);
        lblError.setVisible(false);
        scanFrame.setStroke(Color.web("#6C63FF"));
        scanFrame.setStrokeWidth(3);
        webcamContainer.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 16;");

        if (scanAnimation != null) scanAnimation.play();

        startWebcam();
    }

    // ==================== Cleanup ====================

    private void stopAndCleanup() {
        running.set(false);
        if (scanAnimation != null) scanAnimation.stop();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            camera = null;
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}
