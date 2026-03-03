package controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.User;
import services.*;

import java.util.List;

/**
 * Controller for the home content panel (loaded inside FrontOffice shell).
 * Displays welcome, stats, module quick-access cards, and user suggestions.
 */
public class HomeContentController {

    @FXML private Label heroWelcome;
    @FXML private Label roleLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label followingCountLabel;
    @FXML private VBox suggestionsBox;

    private User currentUser;
    private final ConnectionService connectionService = ConnectionService.getInstance();
    private final UserService userService = UserService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;

        if (currentUser != null) {
            if (heroWelcome != null) heroWelcome.setText("Bienvenue, " + currentUser.getFullName() + " !");
            if (roleLabel != null) roleLabel.setText(currentUser.getRole().name());
        }

        // Load stats in background
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int followers = connectionService.countFollowers(currentUser.getId());
                int following = connectionService.countFollowing(currentUser.getId());
                List<User> similar = userService.getSimilarUsers(currentUser.getId(), 5);

                javafx.application.Platform.runLater(() -> {
                    if (followersCountLabel != null) followersCountLabel.setText(String.valueOf(followers));
                    if (followingCountLabel != null) followingCountLabel.setText(String.valueOf(following));
                    buildSuggestionCards(similar);
                });
                return null;
            }
        };
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    private void buildSuggestionCards(List<User> similar) {
        if (suggestionsBox == null) return;
        suggestionsBox.getChildren().clear();

        if (similar.isEmpty()) {
            Label noSuggestions = new Label("Aucune suggestion disponible");
            noSuggestions.setStyle("-fx-text-fill: #64748b;");
            suggestionsBox.getChildren().add(noSuggestions);
            return;
        }

        for (User u : similar) {
            HBox card = new HBox(12);
            card.setPadding(new Insets(12, 16, 12, 16));
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);");

            VBox info = new VBox(3);
            Label name = new Label(u.getFullName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
            Label role = new Label(u.getRole().name());
            role.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563eb; -fx-font-weight: bold;");
            Label company = new Label(u.getCompanyName() != null ? u.getCompanyName() : "");
            company.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            info.getChildren().addAll(name, role, company);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button followBtn = new Button("Suivre");
            followBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-weight: bold;");
            followBtn.setOnAction(e -> {
                followBtn.setDisable(true);
                followBtn.setText("...");
                Task<Void> followTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        connectionService.follow(currentUser.getId(), u.getId());
                        int followers = connectionService.countFollowers(currentUser.getId());
                        int following = connectionService.countFollowing(currentUser.getId());
                        javafx.application.Platform.runLater(() -> {
                            followBtn.setText("\u2713");
                            followBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                                    "-fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 6 14;");
                            if (followersCountLabel != null) followersCountLabel.setText(String.valueOf(followers));
                            if (followingCountLabel != null) followingCountLabel.setText(String.valueOf(following));
                        });
                        return null;
                    }
                };
                Thread ft = new Thread(followTask);
                ft.setDaemon(true);
                ft.start();
            });

            card.getChildren().addAll(info, spacer, followBtn);
            suggestionsBox.getChildren().add(card);
        }
    }
}
