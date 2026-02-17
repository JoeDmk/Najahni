package controllers;

import exceptions.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
import tools.SceneHelper;
import services.SessionService;
import services.UserService;
import util.Type;

/**
 * Controller for modifying an existing user from the admin dashboard.
 */
public class ModifierUserController {

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private CheckBox activeCheckBox;
    @FXML private CheckBox bannedCheckBox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private DashboardController dashboardController;
    private User userToEdit;

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    /**
     * Loads the selected user's data into the form fields.
     */
    public void setUser(User user) {
        this.userToEdit = user;
        if (user != null) {
            firstnameField.setText(user.getFirstname());
            lastnameField.setText(user.getLastname());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone() != null ? user.getPhone() : "");

            if (roleComboBox != null && user.getRole() != null) {
                String roleName = switch (user.getRole()) {
                    case ENTREPRENEUR -> "Entrepreneur";
                    case MENTOR -> "Mentor";
                    case INVESTISSEUR -> "Investisseur";
                    default -> "Entrepreneur";
                };
                roleComboBox.setValue(roleName);
            }

            activeCheckBox.setSelected(user.getIsActive());
            bannedCheckBox.setSelected(user.getIsBanned());
        }
    }

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Entrepreneur", "Mentor", "Investisseur");
            roleComboBox.setValue("Entrepreneur");
        }
    }

    @FXML
    private void handleSave() {
        try {
            String firstname = firstnameField.getText().trim();
            String lastname = lastnameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String selectedRole = roleComboBox.getValue();
            boolean isActive = activeCheckBox.isSelected();
            boolean isBanned = bannedCheckBox.isSelected();

            Type role = switch (selectedRole) {
                case "Mentor" -> Type.MENTOR;
                case "Investisseur" -> Type.INVESTISSEUR;
                default -> Type.ENTREPRENEUR;
            };

            // Update the user object with new values
            userToEdit.setFirstname(firstname);
            userToEdit.setLastname(lastname);
            userToEdit.setEmail(email);
            userToEdit.setPhone(phone);
            userToEdit.setRole(role);
            userToEdit.setIsActive(isActive);
            userToEdit.setIsBanned(isBanned);

            UserService.getInstance().updateUser(userToEdit);

            showSuccess("Utilisateur modifié avec succès !");

        } catch (EmptyFieldException | InvalidEmailException | InvalidPhoneNumberException |
                 IncorrectPasswordException | UserNotFoundException e) {
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
            SceneHelper.switchScene(SceneHelper.stageOf(firstnameField), root);
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
