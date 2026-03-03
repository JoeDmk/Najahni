package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import models.User;
import tools.SceneHelper;
import services.*;

import java.util.List;

/**
 * Network controller - view users, follow/unfollow, see followers/following.
 */
public class NetworkController {

    @FXML private ListView<User> suggestionsListView;
    @FXML private ListView<User> followingListView;
    @FXML private ListView<User> followersListView;
    @FXML private Label followersCountLabel;
    @FXML private Label followingCountLabel;
    @FXML private TextField searchField;

    private User currentUser;
    private ConnectionService connectionService = ConnectionService.getInstance();
    private UserService userService = UserService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void loadData() {
        if (currentUser == null) return;

        // Load counts
        if (followersCountLabel != null)
            followersCountLabel.setText(String.valueOf(connectionService.countFollowers(currentUser.getId())));
        if (followingCountLabel != null)
            followingCountLabel.setText(String.valueOf(connectionService.countFollowing(currentUser.getId())));

        // Load suggestions
        if (suggestionsListView != null) {
            List<User> suggestions = connectionService.getSuggestions(currentUser.getId(), 10);
            suggestionsListView.setItems(FXCollections.observableArrayList(suggestions));
            suggestionsListView.setCellFactory(listView -> new UserCell(true));
        }

        // Load following
        if (followingListView != null) {
            List<User> following = connectionService.getFollowing(currentUser.getId());
            followingListView.setItems(FXCollections.observableArrayList(following));
            followingListView.setCellFactory(listView -> new UserCell(false));
        }

        // Load followers
        if (followersListView != null) {
            List<User> followers = connectionService.getFollowers(currentUser.getId());
            followersListView.setItems(FXCollections.observableArrayList(followers));
            followersListView.setCellFactory(listView -> new UserCellReadOnly());
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadData();
            return;
        }
        List<User> results = userService.searchUsers(keyword);
        // Remove current user from results
        results.removeIf(u -> u.getId() == currentUser.getId());
        if (suggestionsListView != null) {
            suggestionsListView.setItems(FXCollections.observableArrayList(results));
        }
    }

    @FXML
    private void handleBack() {
        tools.NavigationHelper.goHome(searchField, currentUser);
    }

    /** Cell for users with follow/unfollow button */
    private class UserCell extends ListCell<User> {
        private boolean isFollowButton; // true = show Follow, false = show Unfollow

        UserCell(boolean isFollowButton) {
            this.isFollowButton = isFollowButton;
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }

            HBox hbox = new HBox(10);
            Label nameLabel = new Label(user.getFullName());
            Label roleLabel = new Label("(" + user.getRole().name() + ")");
            roleLabel.setStyle("-fx-text-fill: gray;");
            Button actionBtn = new Button(isFollowButton ? "Suivre" : "Ne plus suivre");

            actionBtn.setOnAction(e -> {
                if (isFollowButton) {
                    connectionService.follow(currentUser.getId(), user.getId());
                } else {
                    connectionService.unfollow(currentUser.getId(), user.getId());
                }
                loadData(); // Refresh
            });

            hbox.getChildren().addAll(nameLabel, roleLabel, actionBtn);
            setGraphic(hbox);
        }
    }

    /** Read-only cell for followers list */
    private class UserCellReadOnly extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }

            HBox hbox = new HBox(10);
            Label nameLabel = new Label(user.getFullName());
            Label roleLabel = new Label("(" + user.getRole().name() + ")");
            roleLabel.setStyle("-fx-text-fill: gray;");
            hbox.getChildren().addAll(nameLabel, roleLabel);
            setGraphic(hbox);
        }
    }
}
