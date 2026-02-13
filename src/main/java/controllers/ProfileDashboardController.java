package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.*;

/**
 * View user profile from admin dashboard.
 */
public class ProfileDashboardController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roleLabel;
    @FXML private Label bioLabel;
    @FXML private Label companyLabel;
    @FXML private Label linkedinLabel;
    @FXML private Label addressLabel;
    @FXML private Label dobLabel;
    @FXML private Label statusLabel;
    @FXML private Label verifiedLabel;
    @FXML private Label followersLabel;
    @FXML private Label followingLabel;

    private User user;

    public void setUser(User user) {
        this.user = user;
        populateFields();
    }

    private void populateFields() {
        if (user == null) return;
        if (nameLabel != null) nameLabel.setText(user.getFullName());
        if (emailLabel != null) emailLabel.setText(user.getEmail());
        if (phoneLabel != null) phoneLabel.setText(user.getPhone() != null ? user.getPhone() : "-");
        if (roleLabel != null) roleLabel.setText(user.getRole().name());
        if (bioLabel != null) bioLabel.setText(user.getBio() != null ? user.getBio() : "-");
        if (companyLabel != null) companyLabel.setText(user.getCompanyName() != null ? user.getCompanyName() : "-");
        if (linkedinLabel != null) linkedinLabel.setText(user.getLinkedinUrl() != null ? user.getLinkedinUrl() : "-");
        if (addressLabel != null) addressLabel.setText(user.getAddress() != null ? user.getAddress() : "-");
        if (dobLabel != null) dobLabel.setText(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "-");
        if (statusLabel != null) statusLabel.setText(user.getIsActiveDisplay() + " / " + user.getIsBannedDisplay());
        if (verifiedLabel != null) verifiedLabel.setText(user.getVerifiedDisplay());

        ConnectionService cs = ConnectionService.getInstance();
        if (followersLabel != null) followersLabel.setText(String.valueOf(cs.countFollowers(user.getId())));
        if (followingLabel != null) followingLabel.setText(String.valueOf(cs.countFollowing(user.getId())));
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();
            DashboardController ctrl = loader.getController();
            User admin = SessionService.getInstance().getCurrentUser();
            ctrl.setCurrentUser(admin);
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
