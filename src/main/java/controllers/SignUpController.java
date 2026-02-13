package controllers;

import exceptions.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.*;
import util.Type;

public class SignUpController {
    private UserService userService = UserService.getInstance();
    private ValidationService validationService = new ValidationService();

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Entrepreneur", "Mentor", "Investisseur");
            roleComboBox.setValue("Entrepreneur");
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
        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    public void handleSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
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
