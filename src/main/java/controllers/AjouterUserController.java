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

/**
 * Add user from admin dashboard (replaces AjouterClient/AjouterGuide).
 */
public class AjouterUserController {

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private DashboardController dashboardController;

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Entrepreneur", "Mentor", "Investisseur");
            roleComboBox.setValue("Entrepreneur");
        }
    }

    @FXML
    private void handleAdd() {
        try {
            String firstname = firstnameField.getText().trim();
            String lastname = lastnameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = passwordField.getText().trim();
            String selectedRole = roleComboBox.getValue();

            Type role = switch (selectedRole) {
                case "Mentor" -> Type.MENTOR;
                case "Investisseur" -> Type.INVESTISSEUR;
                default -> Type.ENTREPRENEUR;
            };

            User user = new User(firstname, lastname, email, phone, password, role);
            UserService.getInstance().addUser(user);

            showSuccess("Utilisateur ajouté avec succès !");

            // Clear fields
            firstnameField.clear();
            lastnameField.clear();
            emailField.clear();
            phoneField.clear();
            passwordField.clear();

        } catch (EmptyFieldException | InvalidEmailException | InvalidPhoneNumberException |
                 IncorrectPasswordException | CustomIllegalStateException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();
            DashboardController ctrl = loader.getController();
            User admin = SessionService.getInstance().getCurrentUser();
            ctrl.setCurrentUser(admin);
            Stage stage = (Stage) firstnameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
        if (successLabel != null) successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setVisible(true);
            successLabel.setStyle("-fx-text-fill: green;");
        }
        errorLabel.setVisible(false);
    }
}
