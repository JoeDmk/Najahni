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
import tools.SceneHelper;
import models.User;
import services.*;
import util.Type;

import java.io.File;

public class SignUpController {
    private UserService userService = UserService.getInstance();
    private ValidationService validationService = new ValidationService();

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField passwordVisibleField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button btnTogglePassword;
    @FXML private Button btnToggleConfirmPassword;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private StackPane profileImageContainer;
    @FXML private Circle profileImageClip;
    @FXML private ImageView profileImageView;
    @FXML private Label lblUploadHint;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    private String selectedImagePath = null;

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Entrepreneur", "Mentor", "Investisseur");
            roleComboBox.setValue("Entrepreneur");
        }

        // Setup circular clip for profile image
        if (profileImageView != null && profileImageClip != null) {
            Circle clip = new Circle(48, 48, 48);
            profileImageView.setClip(clip);
        }

        // Click on the image container to upload
        if (profileImageContainer != null) {
            profileImageContainer.setOnMouseClicked(e -> handleProfileImageUpload());
        }

        // Bind password toggle fields
        if (passwordVisibleField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (confirmPasswordVisibleField != null) {
            confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        }
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        passwordVisibleField.setVisible(passwordVisible);
        passwordVisibleField.setManaged(passwordVisible);
        btnTogglePassword.setText(passwordVisible ? "🙈" : "👁");
    }

    @FXML
    private void handleToggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;
        confirmPasswordField.setVisible(!confirmPasswordVisible);
        confirmPasswordField.setManaged(!confirmPasswordVisible);
        confirmPasswordVisibleField.setVisible(confirmPasswordVisible);
        confirmPasswordVisibleField.setManaged(confirmPasswordVisible);
        btnToggleConfirmPassword.setText(confirmPasswordVisible ? "🙈" : "👁");
    }

    private void handleProfileImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        Stage stage = (Stage) registerButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedImagePath = file.toURI().toString();
            Image image = new Image(selectedImagePath, 96, 96, false, true);
            profileImageView.setImage(image);
            profileImageView.setVisible(true);
            if (lblUploadHint != null) lblUploadHint.setVisible(false);
        }
    }

    @FXML
    public void handleRegister() {
        try {
            String firstname = firstnameField.getText().trim();
            String lastname = lastnameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();
            String selectedRole = roleComboBox != null ? roleComboBox.getValue() : "Entrepreneur";

            // Validate fields
            validateFields(firstname, lastname, email, phone, password, confirmPassword);

            // Map role
            Type role = switch (selectedRole) {
                case "Mentor" -> Type.MENTOR;
                case "Investisseur" -> Type.INVESTISSEUR;
                default -> Type.ENTREPRENEUR;
            };

            // Create user
            User user = new User(firstname, lastname, email, phone, password, role);
            if (selectedImagePath != null) {
                user.setProfilePicture(selectedImagePath);
            }

            // Register
            userService.addUser(user);

            // Get saved user
            User savedUser = userService.getUserbyEmail(email);

            // Send verification email
            String verificationCode = generateVerificationCode();
            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationCode);

            // Redirect to email verification
            redirectToEmailCode(savedUser, verificationCode);

        } catch (EmptyFieldException | InvalidEmailException | InvalidPhoneNumberException |
                 IncorrectPasswordException | PasswordMismatchException e) {
            showError(e.getMessage());
        } catch (CustomIllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Une erreur s'est produite lors de l'inscription.");
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }

    private void redirectToEmailCode(User user, String verificationCode) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EmailCode.fxml"));
        Parent root = loader.load();
        EmailCodeController ctrl = loader.getController();
        ctrl.setUserAndCode(user, verificationCode);
        SceneHelper.switchScene(SceneHelper.stageOf(registerButton), root);
    }

    @FXML
    public void handleSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(registerButton), root);
        } catch (Exception e) {
            showError("Impossible de charger la page de connexion.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
    }

    private void validateFields(String firstname, String lastname, String email, String phone,
                                String password, String confirmPassword)
            throws EmptyFieldException, InvalidEmailException, InvalidPhoneNumberException,
            IncorrectPasswordException, PasswordMismatchException {

        if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            throw new EmptyFieldException("Veuillez remplir tous les champs obligatoires.");
        }
        if (!validationService.isValidEmail(email)) {
            throw new InvalidEmailException("Format d'email invalide.");
        }
        if (!phone.isEmpty() && !validationService.isValidPhoneNumber(phone)) {
            throw new InvalidPhoneNumberException("Format de numéro de téléphone invalide.");
        }
        if (!validationService.isValidPassword(password)) {
            throw new IncorrectPasswordException("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et faire au moins 6 caractères.");
        }
        if (!password.equals(confirmPassword)) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas.");
        }
    }
}
