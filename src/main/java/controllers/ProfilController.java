package controllers;

import exceptions.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.User;
import tools.SceneHelper;
import services.*;
import util.Type;

import java.io.File;
import java.util.Optional;

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

    private User currentUser;
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
