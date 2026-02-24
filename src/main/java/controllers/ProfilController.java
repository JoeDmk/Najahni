package controllers;

import exceptions.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.User;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import tools.SceneHelper;
import services.*;
import util.Type;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Profile controller - view and edit user profile.
 * Supports email verification, phone verification, and profile image upload.
 */
public class ProfilController {

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea bioField;
    @FXML private TextField companyField;
    @FXML private TextField linkedinField;
    @FXML private TextField addressField;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private Label roleLabel;
    @FXML private Label verifiedLabel;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label emailVerifiedBadge;
    @FXML private Label phoneVerifiedBadge;
    @FXML private Button btnVerifyEmail;
    @FXML private Button btnVerifyPhone;
    @FXML private StackPane profileImageContainer;
    @FXML private Circle profileImageClip;
    @FXML private ImageView profileImageView;
    @FXML private Label lblUploadHint;
    @FXML private Label lblFaceStatus;
    @FXML private Button btnRegisterFace;
    @FXML private Button btnDeleteFace;

    private User currentUser;
    private FaceRecognitionService faceService = FaceRecognitionService.getInstance();
    private UserService userService = UserService.getInstance();
    private String originalEmail;
    private String originalPhone;

    @FXML
    public void initialize() {
        // Setup circular clip for profile image
        if (profileImageView != null && profileImageClip != null) {
            Circle clip = new Circle(52, 52, 52);
            profileImageView.setClip(clip);
        }
        // Click to upload profile image
        if (profileImageContainer != null) {
            profileImageContainer.setOnMouseClicked(e -> handleProfileImageUpload());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.originalEmail = user.getEmail();
        this.originalPhone = user.getPhone() != null ? user.getPhone() : "";
        populateFields();
    }

    private void populateFields() {
        if (currentUser == null) return;

        if (firstnameField != null) firstnameField.setText(currentUser.getFirstname());
        if (lastnameField != null) lastnameField.setText(currentUser.getLastname());
        if (emailField != null) emailField.setText(currentUser.getEmail());
        if (phoneField != null) phoneField.setText(currentUser.getPhone());
        if (bioField != null) bioField.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
        if (companyField != null) companyField.setText(currentUser.getCompanyName() != null ? currentUser.getCompanyName() : "");
        if (linkedinField != null) linkedinField.setText(currentUser.getLinkedinUrl() != null ? currentUser.getLinkedinUrl() : "");
        if (addressField != null) addressField.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
        if (dateOfBirthPicker != null && currentUser.getDateOfBirth() != null) dateOfBirthPicker.setValue(currentUser.getDateOfBirth());
        if (roleLabel != null) roleLabel.setText(currentUser.getRole().name());
        if (verifiedLabel != null) verifiedLabel.setText(currentUser.getVerifiedDisplay());

        // Load profile image
        if (profileImageView != null && currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                Image img = new Image(currentUser.getProfilePicture(), 104, 104, false, true);
                profileImageView.setImage(img);
                profileImageView.setVisible(true);
                if (lblUploadHint != null) lblUploadHint.setVisible(false);
            } catch (Exception e) {
                // Invalid image path, keep placeholder
            }
        }

        updateEmailVerificationBadge();
        updatePhoneVerificationBadge();
        updateFaceIdStatus();
    }

    private void updateEmailVerificationBadge() {
        if (emailVerifiedBadge == null) return;
        if (currentUser.isVerified()) {
            emailVerifiedBadge.setText("✓ Vérifié");
            emailVerifiedBadge.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-color: #E8F8F0; -fx-background-radius: 12;");
            if (btnVerifyEmail != null) { btnVerifyEmail.setVisible(false); btnVerifyEmail.setManaged(false); }
        } else {
            emailVerifiedBadge.setText("✗ Non vérifié");
            emailVerifiedBadge.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-color: #FDEDEC; -fx-background-radius: 12;");
            if (btnVerifyEmail != null) { btnVerifyEmail.setVisible(true); btnVerifyEmail.setManaged(true); }
        }
    }

    private void updatePhoneVerificationBadge() {
        if (phoneVerifiedBadge == null) return;
        String phone = currentUser.getPhone();
        if (phone == null || phone.isEmpty()) {
            phoneVerifiedBadge.setText("");
            if (btnVerifyPhone != null) { btnVerifyPhone.setVisible(false); btnVerifyPhone.setManaged(false); }
            return;
        }
        if (currentUser.isPhoneVerified()) {
            phoneVerifiedBadge.setText("✓ Vérifié");
            phoneVerifiedBadge.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-color: #E8F8F0; -fx-background-radius: 12;");
            if (btnVerifyPhone != null) { btnVerifyPhone.setVisible(false); btnVerifyPhone.setManaged(false); }
        } else {
            phoneVerifiedBadge.setText("✗ Non vérifié");
            phoneVerifiedBadge.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-color: #FDEDEC; -fx-background-radius: 12;");
            if (btnVerifyPhone != null) { btnVerifyPhone.setVisible(true); btnVerifyPhone.setManaged(true); }
        }
    }

    @FXML
    private void handleVerifyEmail() {
        try {
            // Generate and send verification code
            String code = String.valueOf((int) (Math.random() * 9000) + 1000);
            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(currentUser.getEmail(), code);

            // Show input dialog
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Vérification Email");
            dialog.setHeaderText("Un code a été envoyé à " + currentUser.getEmail());
            dialog.setContentText("Code de vérification :");
            Optional<String> result = dialog.showAndWait();

            if (result.isPresent() && result.get().trim().equals(code)) {
                userService.verifyUser(currentUser.getId());
                currentUser.setVerified(true);
                updateEmailVerificationBadge();
                if (verifiedLabel != null) verifiedLabel.setText(currentUser.getVerifiedDisplay());
                showSuccess("Email vérifié avec succès !");
            } else if (result.isPresent()) {
                showError("Code incorrect.");
            }
        } catch (Exception e) {
            showError("Erreur lors de l'envoi du code de vérification.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVerifyPhone() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            showError("Veuillez saisir un numéro de téléphone.");
            return;
        }
        try {
            // Generate and send SMS code via Twilio
            String code = String.valueOf((int) (Math.random() * 9000) + 1000);
            TwilioService twilioService = new TwilioService();
            twilioService.sendSms(phone, "Votre code de vérification Najahni : " + code);

            // Show input dialog to enter the code
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Vérification Téléphone");
            dialog.setHeaderText("Un code SMS a été envoyé à " + phone);
            dialog.setContentText("Code de vérification :");
            Optional<String> result = dialog.showAndWait();

            if (result.isPresent() && result.get().trim().equals(code)) {
                userService.verifyPhone(currentUser.getId());
                currentUser.setPhoneVerified(true);
                updatePhoneVerificationBadge();
                showSuccess("Numéro de téléphone vérifié avec succès !");
            } else if (result.isPresent()) {
                showError("Code incorrect.");
            }
        } catch (Exception e) {
            showError("Erreur lors de l'envoi du SMS.");
            e.printStackTrace();
        }
    }

    private void handleProfileImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        Stage stage = (Stage) firstnameField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String imagePath = file.toURI().toString();
            Image image = new Image(imagePath, 104, 104, false, true);
            profileImageView.setImage(image);
            profileImageView.setVisible(true);
            if (lblUploadHint != null) lblUploadHint.setVisible(false);
            currentUser.setProfilePicture(imagePath);
        }
    }

    @FXML
    private void handleSaveProfile() {
        try {
            String newEmail = emailField.getText().trim();
            String newPhone = phoneField.getText().trim();

            currentUser.setFirstname(firstnameField.getText().trim());
            currentUser.setLastname(lastnameField.getText().trim());
            currentUser.setEmail(newEmail);
            currentUser.setPhone(newPhone);
            currentUser.setBio(bioField != null ? bioField.getText().trim() : null);
            currentUser.setCompanyName(companyField != null ? companyField.getText().trim() : null);
            currentUser.setLinkedinUrl(linkedinField != null ? linkedinField.getText().trim() : null);
            currentUser.setAddress(addressField != null ? addressField.getText().trim() : null);
            if (dateOfBirthPicker != null && dateOfBirthPicker.getValue() != null) {
                currentUser.setDateOfBirth(dateOfBirthPicker.getValue());
            }

            userService.updateProfile(currentUser);

            // If email changed, mark as unverified
            if (!newEmail.equals(originalEmail)) {
                userService.unverifyEmail(currentUser.getId());
                currentUser.setVerified(false);
                originalEmail = newEmail;
                updateEmailVerificationBadge();
                if (verifiedLabel != null) verifiedLabel.setText(currentUser.getVerifiedDisplay());
            }

            // If phone changed, mark phone as unverified
            if (!newPhone.equals(originalPhone)) {
                userService.unverifyPhone(currentUser.getId());
                currentUser.setPhoneVerified(false);
                originalPhone = newPhone;
                updatePhoneVerificationBadge();
            }

            showSuccess("Profil mis à jour avec succès !");
        } catch (EmptyFieldException | InvalidEmailException | InvalidPhoneNumberException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChangePassword.fxml"));
            Parent root = loader.load();
            ChangePasswordController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(firstnameField), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== Face ID Methods ====================

    private void updateFaceIdStatus() {
        if (lblFaceStatus == null || btnRegisterFace == null || btnDeleteFace == null) return;
        if (currentUser == null) return;

        boolean hasFace = faceService.hasFaceData(currentUser.getId()) || currentUser.isFaceRegistered();
        if (hasFace) {
            lblFaceStatus.setText("✓ Face ID enregistré");
            lblFaceStatus.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 12px; -fx-font-weight: bold;");
            btnRegisterFace.setText("Réenregistrer Face ID");
            btnDeleteFace.setVisible(true);
            btnDeleteFace.setManaged(true);
        } else {
            lblFaceStatus.setText("Aucun Face ID enregistré");
            lblFaceStatus.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px;");
            btnRegisterFace.setText("Enregistrer Face ID");
            btnDeleteFace.setVisible(false);
            btnDeleteFace.setManaged(false);
        }
    }

    /**
     * Face enrollment flow with LIVE WEBCAM PREVIEW:
     * 1. Opens a popup dialog with the webcam feed
     * 2. Captures 15 face samples with visual countdown
     * 3. Trains a LBPH model for this user
     * 4. Updates face_registered in the database
     */
    @FXML
    private void handleRegisterFace() {
        // Confirm with user
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Enregistrement Face ID");
        confirm.setHeaderText("Enregistrer votre visage");
        confirm.setContentText(
                "La caméra va s'ouvrir pour capturer 15 échantillons de votre visage.\n" +
                "Restez face à la caméra et tournez légèrement la tête.\n\n" +
                "Voulez-vous continuer ?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        // Create the preview popup
        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.initOwner(firstnameField.getScene().getWindow());
        previewStage.setTitle("Enregistrement Face ID — Caméra");
        previewStage.setResizable(false);

        ImageView camView = new ImageView();
        camView.setFitWidth(480);
        camView.setFitHeight(360);
        camView.setPreserveRatio(false);

        Label lblProgress = new Label("Ouverture de la caméra...");
        lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        ProgressBar progressBarEnroll = new ProgressBar(0);
        progressBarEnroll.setPrefWidth(400);
        progressBarEnroll.setStyle("-fx-accent: #6C63FF;");

        Label lblHint = new Label("Regardez la caméra et tournez légèrement la tête");
        lblHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 24; -fx-cursor: hand;");

        VBox layout = new VBox(12, camView, lblProgress, progressBarEnroll, lblHint, btnCancel);
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

        // Show the stage first, then start webcam thread
        previewStage.show();

        new Thread(() -> {
            VideoCapture camera = null;
            try {
                camera = faceService.openWebcam();
                if (camera == null) {
                    Platform.runLater(() -> {
                        previewStage.close();
                        showError("Impossible d'ouvrir la caméra.");
                    });
                    return;
                }

                // Delete old data first
                faceService.deleteFaceData(currentUser.getId());

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
                                showError("La caméra ne renvoie aucune image. Vérifiez votre webcam et réessayez.");
                            });
                            return;
                        }
                        Thread.sleep(100);
                        continue;
                    }
                    emptyFrameCount = 0;

                    RectVector faces = faceService.detectFaces(frame);

                    if (faces.size() > 0) {
                        // Draw green rectangle on detected face
                        Scalar green = new Scalar(0, 255, 0, 255);
                        faceService.drawFaceRectangles(frame, faces, green);
                        green.close();

                        Rect faceRect = faces.get(0);
                        Mat faceROI = faceService.extractFaceROI(frame, faceRect);
                        faceService.saveFaceSample(currentUser.getId(), captured, faceROI);
                        faceROI.close();
                        captured++;

                        final int count = captured;
                        final double progress = (double) count / totalSamples;
                        Platform.runLater(() -> {
                            lblProgress.setText("📸 Capture " + count + " / " + totalSamples);
                            progressBarEnroll.setProgress(progress);
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
                    Platform.runLater(() -> showError("Enregistrement annulé."));
                    return;
                }

                // Release camera before training
                camera.release();
                camera = null;

                Platform.runLater(() -> {
                    lblProgress.setText("⏳ Entraînement du modèle...");
                    lblProgress.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F39C12;");
                    lblHint.setText("Veuillez patienter...");
                    progressBarEnroll.setProgress(-1); // indeterminate
                });

                boolean trained = faceService.trainUserModel(currentUser.getId());

                Platform.runLater(() -> {
                    previewStage.close();
                    if (trained) {
                        userService.setFaceRegistered(currentUser.getId(), true);
                        currentUser.setFaceRegistered(true);
                        showSuccess("Face ID enregistré avec succès ! Vous pouvez maintenant vous connecter avec votre visage.");
                        updateFaceIdStatus();
                    } else {
                        showError("Erreur lors de l'entraînement du modèle. Réessayez.");
                        updateFaceIdStatus();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                final VideoCapture cam = camera;
                Platform.runLater(() -> {
                    previewStage.close();
                    showError("Erreur : " + e.getMessage());
                    updateFaceIdStatus();
                });
                if (cam != null && cam.isOpened()) cam.release();
            }
        }, "FaceEnrollment").start();
    }

    /**
     * Deletes the user's face data and updates the database.
     */
    @FXML
    private void handleDeleteFace() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer Face ID");
        confirm.setHeaderText("Supprimer votre Face ID ?");
        confirm.setContentText("Vous ne pourrez plus vous connecter avec votre visage.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    faceService.deleteFaceData(currentUser.getId());
                    userService.setFaceRegistered(currentUser.getId(), false);
                    currentUser.setFaceRegistered(false);
                    updateFaceIdStatus();
                    showSuccess("Face ID supprimé avec succès.");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression du compte");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        confirm.setContentText("Cette action est irréversible. Toutes vos données seront perdues.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.deleteUser(currentUser.getId());
                    SessionManager.clearSession();
                    SessionService.getInstance().logout();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
                    Parent root = loader.load();
                    SceneHelper.switchScene(SceneHelper.stageOf(firstnameField), root);
                } catch (Exception e) {
                    showError("Erreur lors de la suppression du compte : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader;
            if (currentUser.getRole() == Type.ADMIN) {
                loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
            }
            Parent root = loader.load();

            if (currentUser.getRole() == Type.ADMIN) {
                DashboardController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
            } else {
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
            }

            SceneHelper.switchScene(SceneHelper.stageOf(firstnameField), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: red;");
        }
        if (successLabel != null) successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setVisible(true);
            successLabel.setStyle("-fx-text-fill: green;");
        }
        if (errorLabel != null) errorLabel.setVisible(false);
    }
}
