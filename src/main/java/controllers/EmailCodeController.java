package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.User;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import tools.SceneHelper;
import services.*;
import util.Type;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Email verification code controller.
 * After verification, offers optional Face ID registration.
 */
public class EmailCodeController {

    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Label infoLabel;

    private User user;
    private String verificationCode;
    private FaceRecognitionService faceService = FaceRecognitionService.getInstance();
    private UserService userService = UserService.getInstance();

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
                user.setVerified(true);
                SessionManager.saveSession(user.getEmail(), user.getRole().name());
                SessionService.getInstance().setCurrentUser(user);

                // Offer face registration before going home
                offerFaceRegistration();
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
     * Still offers face registration as an optional step.
     */
    @FXML
    private void handleSkip() {
        try {
            SessionManager.saveSession(user.getEmail(), user.getRole().name());
            SessionService.getInstance().setCurrentUser(user);

            // Offer face registration before going home
            offerFaceRegistration();
        } catch (Exception e) {
            showError("Erreur lors de la redirection.");
            e.printStackTrace();
        }
    }

    // ==================== Face Registration Option ====================

    /**
     * Shows a dialog asking the user if they want to register Face ID.
     * If yes → starts webcam enrollment. If no → goes to Home.
     */
    private void offerFaceRegistration() {
        Alert faceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        faceAlert.setTitle("Face ID");
        faceAlert.setHeaderText("Voulez-vous enregistrer votre Face ID ?");
        faceAlert.setContentText(
                "Vous pourrez vous connecter rapidement avec votre visage.\n" +
                "Vous pouvez aussi le faire plus tard depuis votre profil.");

        ButtonType btnYes = new ButtonType("Oui, enregistrer");
        ButtonType btnLater = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);
        faceAlert.getButtonTypes().setAll(btnYes, btnLater);

        Optional<ButtonType> result = faceAlert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            startFaceEnrollment();
        } else {
            redirectToHome();
        }
    }

    /**
     * Starts the face enrollment process with live webcam preview.
     * Captures face samples, trains the model, and updates the database.
     */
    private void startFaceEnrollment() {
        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.initOwner(codeField.getScene().getWindow());
        previewStage.setTitle("Enregistrement Face ID — Caméra");
        previewStage.setResizable(false);

        ImageView camView = new ImageView();
        camView.setFitWidth(480);
        camView.setFitHeight(360);
        camView.setPreserveRatio(false);

        Label lblProgress = new Label("Ouverture de la caméra...");
        lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #6C63FF;");

        Label lblHint = new Label("Regardez la caméra et tournez légèrement la tête");
        lblHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Button btnCancel = new Button("Passer cette étape");
        btnCancel.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 20; " +
                "-fx-padding: 8 24; -fx-cursor: hand;");

        VBox layout = new VBox(12, camView, lblProgress, progressBar, lblHint, btnCancel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(layout, 520, 500);
        previewStage.setScene(scene);

        AtomicBoolean cancelled = new AtomicBoolean(false);

        btnCancel.setOnAction(e -> {
            cancelled.set(true);
            previewStage.close();
        });
        previewStage.setOnCloseRequest(e -> cancelled.set(true));

        previewStage.show();

        new Thread(() -> {
            VideoCapture camera = null;
            try {
                camera = faceService.openWebcam();
                if (camera == null) {
                    Platform.runLater(() -> {
                        previewStage.close();
                        showInfo("Caméra non disponible. Vous pourrez enregistrer Face ID depuis votre profil.");
                        redirectToHome();
                    });
                    return;
                }

                // Delete old data first (shouldn't exist for new user, but just in case)
                faceService.deleteFaceData(user.getId());

                int totalSamples = FaceRecognitionService.ENROLLMENT_SAMPLES;
                int captured = 0;
                int emptyFrameCount = 0;
                final int MAX_EMPTY_FRAMES = 60;

                while (captured < totalSamples && !cancelled.get()) {
                    Mat frame = faceService.captureFrame(camera);
                    if (frame.empty()) {
                        frame.close();
                        emptyFrameCount++;
                        if (emptyFrameCount >= MAX_EMPTY_FRAMES) {
                            if (camera != null && camera.isOpened()) camera.release();
                            Platform.runLater(() -> {
                                previewStage.close();
                                showInfo("Problème de caméra. Vous pourrez enregistrer Face ID depuis votre profil.");
                                redirectToHome();
                            });
                            return;
                        }
                        Thread.sleep(100);
                        continue;
                    }
                    emptyFrameCount = 0;

                    RectVector faces = faceService.detectFaces(frame);

                    if (faces.size() > 0) {
                        Scalar green = new Scalar(0, 255, 0, 255);
                        faceService.drawFaceRectangles(frame, faces, green);
                        green.close();

                        Rect faceRect = faces.get(0);
                        Mat faceROI = faceService.extractFaceROI(frame, faceRect);
                        faceService.saveFaceSample(user.getId(), captured, faceROI);
                        faceROI.close();
                        captured++;

                        final int count = captured;
                        final double progress = (double) count / totalSamples;
                        Platform.runLater(() -> {
                            lblProgress.setText("📸 Capture " + count + " / " + totalSamples);
                            progressBar.setProgress(progress);
                        });

                        Thread.sleep(400);
                    } else {
                        Platform.runLater(() -> {
                            lblProgress.setText("Recherche de visage...");
                            lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E17055;");
                        });
                    }

                    // Update live preview
                    javafx.scene.image.Image fxImage = faceService.matToJavaFXImage(frame);
                    if (fxImage != null && !cancelled.get()) {
                        Platform.runLater(() -> {
                            camView.setImage(fxImage);
                            lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");
                        });
                    }

                    frame.close();
                    faces.close();
                    Thread.sleep(50);
                }

                if (cancelled.get()) {
                    if (camera != null && camera.isOpened()) camera.release();
                    // Clean up incomplete face data
                    faceService.deleteFaceData(user.getId());
                    Platform.runLater(this::redirectToHome);
                    return;
                }

                // Release camera before training
                camera.release();
                camera = null;

                Platform.runLater(() -> {
                    lblProgress.setText("⏳ Entraînement du modèle...");
                    lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F39C12;");
                    lblHint.setText("Veuillez patienter...");
                    progressBar.setProgress(-1);
                });

                boolean trained = faceService.trainUserModel(user.getId());

                Platform.runLater(() -> {
                    previewStage.close();
                    if (trained) {
                        userService.setFaceRegistered(user.getId(), true);
                        user.setFaceRegistered(true);
                        showInfo("Face ID enregistré avec succès !");
                    } else {
                        showInfo("L'enregistrement a échoué. Vous pourrez réessayer depuis votre profil.");
                    }
                    redirectToHome();
                });

            } catch (Exception e) {
                e.printStackTrace();
                final VideoCapture cam = camera;
                Platform.runLater(() -> {
                    previewStage.close();
                    showInfo("Erreur : " + e.getMessage() + ". Vous pourrez réessayer depuis votre profil.");
                    redirectToHome();
                });
                if (cam != null && cam.isOpened()) cam.release();
            }
        }, "FaceEnrollment-SignUp").start();
    }

    // ==================== Navigation ====================

    private void redirectToHome() {
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

            SceneHelper.switchScene(SceneHelper.stageOf(codeField), root);
        } catch (Exception e) {
            System.err.println("Error redirecting to home: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== UI Helpers ====================

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    private void showInfo(String message) {
        if (infoLabel != null) {
            infoLabel.setText(message);
            infoLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        }
    }
}
