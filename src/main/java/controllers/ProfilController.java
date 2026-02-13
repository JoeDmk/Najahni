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
 * Profile controller - view and edit user profile.
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

    private User currentUser;
    private UserService userService = UserService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;
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
    }

    @FXML
    private void handleSaveProfile() {
        try {
            currentUser.setFirstname(firstnameField.getText().trim());
            currentUser.setLastname(lastnameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setBio(bioField != null ? bioField.getText().trim() : null);
            currentUser.setCompanyName(companyField != null ? companyField.getText().trim() : null);
            currentUser.setLinkedinUrl(linkedinField != null ? linkedinField.getText().trim() : null);
            currentUser.setAddress(addressField != null ? addressField.getText().trim() : null);
            if (dateOfBirthPicker != null && dateOfBirthPicker.getValue() != null) {
                currentUser.setDateOfBirth(dateOfBirthPicker.getValue());
            }

            userService.updateProfile(currentUser);
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
            Stage stage = (Stage) firstnameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
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

            Stage stage = (Stage) firstnameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
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
