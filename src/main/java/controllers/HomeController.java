package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tools.SceneHelper;
import models.LoginHistory;
import models.Notification;
import models.User;
import services.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Home Controller for non-admin users (Entrepreneur, Mentor, Investisseur).
 */
public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Button btnToggleScreen;
    @FXML private Label roleLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label followingCountLabel;
    @FXML private Label notificationBadge;
    @FXML private VBox suggestionsBox;

    private User currentUser;
    private ConnectionService connectionService = ConnectionService.getInstance();
    private NotificationService notificationService = NotificationService.getInstance();
    private ThemeService themeService = ThemeService.getInstance();
    private UserService userService = UserService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionService.getInstance().setCurrentUser(user);
        updateUI();
        updateNotificationBadge();
        loadSuggestions();

        // Apply theme
        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            themeService.applyTheme(welcomeLabel.getScene());
        }
    }

    private void updateUI() {
        if (currentUser != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + currentUser.getFullName() + " !");
            if (roleLabel != null) roleLabel.setText(currentUser.getRole().name());
            if (followersCountLabel != null)
                followersCountLabel.setText(String.valueOf(connectionService.countFollowers(currentUser.getId())));
            if (followingCountLabel != null)
                followingCountLabel.setText(String.valueOf(connectionService.countFollowing(currentUser.getId())));
        }
    }

    private void loadSuggestions() {
        if (suggestionsBox == null || currentUser == null) return;
        suggestionsBox.getChildren().clear();

        List<User> similar = userService.getSimilarUsers(currentUser.getId(), 5);
        if (similar.isEmpty()) {
            Label noSuggestions = new Label("Aucune suggestion disponible");
            noSuggestions.setStyle("-fx-text-fill: #636E72;");
            suggestionsBox.getChildren().add(noSuggestions);
            return;
        }

        for (User u : similar) {
            HBox card = new HBox(12);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-border-color: #E1E8ED; -fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");

            VBox info = new VBox(3);
            Label name = new Label(u.getFullName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2D3436;");
            Label role = new Label(u.getRole().name());
            role.setStyle("-fx-font-size: 11px; -fx-text-fill: #6C63FF;");
            Label company = new Label(u.getCompanyName() != null ? u.getCompanyName() : "");
            company.setStyle("-fx-font-size: 11px; -fx-text-fill: #636E72;");
            info.getChildren().addAll(name, role, company);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button followBtn = new Button("Suivre");
            followBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 12;");
            followBtn.setOnAction(e -> {
                connectionService.follow(currentUser.getId(), u.getId());
                followBtn.setText("\u2713");
                followBtn.setDisable(true);
                updateUI();
            });

            card.getChildren().addAll(info, spacer, followBtn);
            suggestionsBox.getChildren().add(card);
        }
    }

    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent root = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(welcomeLabel), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNetwork() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Network.fxml"));
            Parent root = loader.load();
            NetworkController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(welcomeLabel), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ChangePassword.fxml"));
            Parent root = loader.load();
            ChangePasswordController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(welcomeLabel), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        SessionService.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            SceneHelper.switchScene(SceneHelper.stageOf(welcomeLabel), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleScreen() {
        Stage stage = SceneHelper.stageOf(welcomeLabel);
        if (stage.isMaximized() || stage.isFullScreen()) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setWidth(1200);
            stage.setHeight(750);
            stage.centerOnScreen();
        } else {
            stage.setMaximized(true);
        }
    }

    // ==================== NEW FEATURES ====================

    @FXML
    private void handleThemeToggle() {
        themeService.toggleTheme(welcomeLabel.getScene());
        if (currentUser != null) {
            userService.saveThemePreference(currentUser.getId(), themeService.getThemeName());
        }
    }

    @FXML
    private void handleLanguageToggle() {
        LanguageService langService = LanguageService.getInstance();
        String current = langService.getCurrentLanguageCode();
        String next = switch (current) {
            case "fr" -> "en";
            case "en" -> "ar";
            default -> "fr";
        };
        langService.setLanguage(next);
        if (currentUser != null) {
            userService.saveLanguagePreference(currentUser.getId(), next);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Langue chang\u00e9e: " + langService.getCurrentLanguageName() +
                "\nLes changements seront appliqu\u00e9s au prochain chargement de page.");
        alert.showAndWait();
    }

    @FXML
    private void handleNotifications() {
        if (currentUser == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(welcomeLabel));
        dialog.setTitle("Notifications");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F0F2F5;");

        Label title = new Label("\uD83D\uDD14 Notifications");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E;");

        List<Notification> notifs = notificationService.getNotifications(currentUser.getId());

        ScrollPane scrollPane = new ScrollPane();
        VBox notifList = new VBox(8);
        notifList.setPadding(new Insets(5));

        if (notifs.isEmpty()) {
            notifList.getChildren().add(new Label("Aucune notification"));
        } else {
            for (Notification n : notifs) {
                VBox item = new VBox(4);
                item.setPadding(new Insets(12));
                item.setStyle(n.isRead()
                        ? "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E1E8ED; -fx-border-radius: 8;"
                        : "-fx-background-color: #F0EDFF; -fx-background-radius: 8; -fx-border-color: #6C63FF; -fx-border-radius: 8;");

                Label nTitle = new Label(n.getTypeIcon() + " " + n.getTitle());
                nTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label nMsg = new Label(n.getMessage());
                nMsg.setWrapText(true);
                nMsg.setStyle("-fx-text-fill: #636E72; -fx-font-size: 12px;");
                Label nTime = new Label(n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                nTime.setStyle("-fx-text-fill: #B0B0B0; -fx-font-size: 11px;");

                item.getChildren().addAll(nTitle, nMsg, nTime);
                notifList.getChildren().add(item);
            }
        }

        scrollPane.setContent(notifList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        Button markAllBtn = new Button("Tout marquer lu");
        markAllBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        markAllBtn.setOnAction(e -> {
            notificationService.markAllAsRead(currentUser.getId());
            updateNotificationBadge();
            dialog.close();
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E1E8ED; -fx-border-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, markAllBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(title, scrollPane, buttons);

        Scene scene = new Scene(layout, 480, 520);
        dialog.setScene(scene);
        dialog.show();
    }

    @FXML
    private void handleLoginHistory() {
        if (currentUser == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(welcomeLabel));
        dialog.setTitle("Historique de connexion");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F0F2F5;");

        Label title = new Label("\uD83D\uDCCB Mon historique de connexion");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E;");

        // Risk score display
        SuspiciousLoginService suspiciousService = SuspiciousLoginService.getInstance();
        int riskScore = suspiciousService.analyzeLogin(currentUser);
        String riskLevel = suspiciousService.getRiskLevel(riskScore);
        String riskColor = suspiciousService.getRiskColor(riskScore);

        HBox riskBox = new HBox(10);
        riskBox.setAlignment(Pos.CENTER_LEFT);
        riskBox.setPadding(new Insets(10));
        riskBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: " + riskColor + "; -fx-border-radius: 10;");

        Label riskLabel = new Label("\uD83D\uDEE1 Score de risque: " + riskScore + "/100");
        riskLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label riskLevelLabel = new Label("(" + riskLevel + ")");
        riskLevelLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + riskColor + "; -fx-font-weight: bold;");
        riskBox.getChildren().addAll(riskLabel, riskLevelLabel);

        TableView<LoginHistory> historyTable = new TableView<>();
        historyTable.setPrefHeight(300);

        TableColumn<LoginHistory, String> methodCol = new TableColumn<>("M\u00e9thode");
        methodCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLoginMethod()));
        methodCol.setPrefWidth(90);

        TableColumn<LoginHistory, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getIpAddress()));
        ipCol.setPrefWidth(120);

        TableColumn<LoginHistory, String> deviceCol = new TableColumn<>("Appareil");
        deviceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDeviceInfo()));
        deviceCol.setPrefWidth(200);

        TableColumn<LoginHistory, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSuccessDisplay()));
        statusCol.setPrefWidth(70);

        TableColumn<LoginHistory, String> timeCol = new TableColumn<>("Date");
        timeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getLoginTime() != null
                        ? c.getValue().getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        : ""));
        timeCol.setPrefWidth(150);

        historyTable.getColumns().addAll(methodCol, ipCol, deviceCol, statusCol, timeCol);

        List<LoginHistory> history = LoginHistoryService.getInstance().getLoginHistory(currentUser.getId(), 50);
        historyTable.setItems(FXCollections.observableArrayList(history));

        // Unique devices
        List<String> devices = LoginHistoryService.getInstance().getUniqueDevices(currentUser.getId());
        Label devicesLabel = new Label("\uD83D\uDCF1 Appareils connus: " + devices.size());
        devicesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636E72;");

        Button clearBtn = new Button("Effacer l'historique");
        clearBtn.setStyle("-fx-background-color: #E17055; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            LoginHistoryService.getInstance().clearHistory(currentUser.getId());
            historyTable.getItems().clear();
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, clearBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(title, riskBox, historyTable, devicesLabel, buttons);

        Scene scene = new Scene(layout, 700, 520);
        dialog.setScene(scene);
        dialog.show();
    }

    private void updateNotificationBadge() {
        if (notificationBadge != null && currentUser != null) {
            int unread = notificationService.getUnreadCount(currentUser.getId());
            if (unread > 0) {
                notificationBadge.setText(String.valueOf(unread));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        }
    }
}
