package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import models.User;
import tools.SceneHelper;
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
    @FXML private Label emailVerifiedBadge;
    @FXML private Label phoneVerifiedBadge;
    @FXML private StackPane profileImageContainer;
    @FXML private ImageView profileImageView;
    @FXML private Label lblInitials;

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

        // Profile picture
        loadProfileImage();

        // Email verification badge
        updateEmailVerificationBadge();

        // Phone verification badge
        updatePhoneVerificationBadge();
    }

    private void loadProfileImage() {
        if (profileImageView == null) return;

        // Apply circular clip
        Circle clip = new Circle(52, 52, 52);
        profileImageView.setClip(clip);

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                Image img = new Image(user.getProfilePicture(), 104, 104, false, true);
                profileImageView.setImage(img);
                profileImageView.setVisible(true);
                if (lblInitials != null) lblInitials.setVisible(false);
            } catch (Exception e) {
                showInitials();
            }
        } else {
            showInitials();
        }
    }

    private void showInitials() {
        if (lblInitials == null) return;
        String initials = "";
        if (user.getFirstname() != null && !user.getFirstname().isEmpty())
            initials += user.getFirstname().substring(0, 1).toUpperCase();
        if (user.getLastname() != null && !user.getLastname().isEmpty())
            initials += user.getLastname().substring(0, 1).toUpperCase();
        lblInitials.setText(initials.isEmpty() ? "?" : initials);
        lblInitials.setVisible(true);
        if (profileImageView != null) profileImageView.setVisible(false);
    }

    private void updateEmailVerificationBadge() {
        if (emailVerifiedBadge == null) return;
        if (user.isVerified()) {
            emailVerifiedBadge.setText("✓ Vérifié");
            emailVerifiedBadge.setStyle("-fx-font-size: 13; -fx-text-fill: #27AE60; -fx-font-weight: bold;");
        } else {
            emailVerifiedBadge.setText("✗ Non vérifié");
            emailVerifiedBadge.setStyle("-fx-font-size: 13; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        }
    }

    private void updatePhoneVerificationBadge() {
        if (phoneVerifiedBadge == null) return;
        if (user.isPhoneVerified()) {
            phoneVerifiedBadge.setText("✓ Vérifié");
            phoneVerifiedBadge.setStyle("-fx-font-size: 13; -fx-text-fill: #27AE60; -fx-font-weight: bold;");
        } else {
            phoneVerifiedBadge.setText("✗ Non vérifié");
            phoneVerifiedBadge.setStyle("-fx-font-size: 13; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");
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
            SceneHelper.switchScene(SceneHelper.stageOf(nameLabel), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
