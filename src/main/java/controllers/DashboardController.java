package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.LoginHistory;
import models.Notification;
import models.User;
import tools.SceneHelper;
import services.*;
import util.Type;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin Dashboard Controller - manages all users (Entrepreneurs, Mentors, Investisseurs).
 */
public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Button btnToggleScreen;
    @FXML private Label totalUsersLabel;
    @FXML private Label entrepreneursLabel;
    @FXML private Label mentorsLabel;
    @FXML private Label investisseursLabel;
    @FXML private Label bannedLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> firstnameColumn;
    @FXML private TableColumn<User, String> lastnameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> verifiedColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;

    // NEW: Notification badge
    @FXML private Label notificationBadge;

    private UserService userService = UserService.getInstance();
    private NotificationService notificationService = NotificationService.getInstance();
    private ThemeService themeService = ThemeService.getInstance();
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Bienvenue, " + user.getFullName());
        }

        // Load all data in background to keep UI responsive
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Fetch all data on background thread
                int total = userService.countTotal();
                int entrepreneurs = userService.countByRole(Type.ENTREPRENEUR);
                int mentors = userService.countByRole(Type.MENTOR);
                int investisseurs = userService.countByRole(Type.INVESTISSEUR);
                int banned = userService.countBanned();
                java.util.List<User> users = userService.getUsers();
                int unread = user != null ? notificationService.getUnreadCount(user.getId()) : 0;

                // Update UI on FX thread
                javafx.application.Platform.runLater(() -> {
                    if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(total));
                    if (entrepreneursLabel != null) entrepreneursLabel.setText(String.valueOf(entrepreneurs));
                    if (mentorsLabel != null) mentorsLabel.setText(String.valueOf(mentors));
                    if (investisseursLabel != null) investisseursLabel.setText(String.valueOf(investisseurs));
                    if (bannedLabel != null) bannedLabel.setText(String.valueOf(banned));

                    usersList.setAll(users);
                    if (usersTable != null) usersTable.setItems(usersList);

                    if (notificationBadge != null) {
                        if (unread > 0) {
                            notificationBadge.setText(String.valueOf(unread));
                            notificationBadge.setVisible(true);
                        } else {
                            notificationBadge.setVisible(false);
                        }
                    }
                });
                return null;
            }
        };
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();

        // Apply theme (lightweight, OK on FX thread)
        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            themeService.applyTheme(welcomeLabel.getScene());
        }
    }

    @FXML
    public void initialize() {
        // Table columns setup
        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (firstnameColumn != null) firstnameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        if (lastnameColumn != null) lastnameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        if (emailColumn != null) emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (phoneColumn != null) phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if (roleColumn != null) roleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole() != null ? cellData.getValue().getRole().name() : ""));
        if (statusColumn != null) statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIsActiveDisplay()));
        if (verifiedColumn != null) verifiedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getVerifiedDisplay()));

        // Role filter
        if (roleFilterCombo != null) {
            roleFilterCombo.getItems().addAll("Tous", "Entrepreneur", "Mentor", "Investisseur", "Admin");
            roleFilterCombo.setValue("Tous");
            roleFilterCombo.setOnAction(e -> handleFilter());
        }

        // Search listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }
    }

    private void loadStats() {
        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(userService.countTotal()));
        if (entrepreneursLabel != null) entrepreneursLabel.setText(String.valueOf(userService.countByRole(Type.ENTREPRENEUR)));
        if (mentorsLabel != null) mentorsLabel.setText(String.valueOf(userService.countByRole(Type.MENTOR)));
        if (investisseursLabel != null) investisseursLabel.setText(String.valueOf(userService.countByRole(Type.INVESTISSEUR)));
        if (bannedLabel != null) bannedLabel.setText(String.valueOf(userService.countBanned()));
    }

    private void loadUsers(Type roleFilter) {
        List<User> users;
        if (roleFilter != null) {
            users = userService.getUsersByRole(roleFilter);
        } else {
            users = userService.getUsers();
        }
        usersList.setAll(users);
        if (usersTable != null) usersTable.setItems(usersList);
    }

    @FXML
    private void handleFilter() {
        String selected = roleFilterCombo.getValue();
        Type filter = switch (selected) {
            case "Entrepreneur" -> Type.ENTREPRENEUR;
            case "Mentor" -> Type.MENTOR;
            case "Investisseur" -> Type.INVESTISSEUR;
            case "Admin" -> Type.ADMIN;
            default -> null;
        };
        loadUsers(filter);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadUsers(null);
        } else {
            usersList.setAll(userService.searchUsers(keyword));
            usersTable.setItems(usersList);
        }
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterUser.fxml"));
            Parent root = loader.load();
            AjouterUserController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBanUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            if (selected.getIsBanned()) {
                userService.unbanUser(selected.getId());
            } else {
                userService.banUser(selected.getId());
            }
            loadUsers(null);
            loadStats();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet utilisateur ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.deleteUser(selected.getId());
                    loadUsers(null);
                    loadStats();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleViewProfile() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileDashboard.fxml"));
            Parent root = loader.load();
            ProfileDashboardController ctrl = loader.getController();
            ctrl.setUser(selected);
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModifierUser.fxml"));
            Parent root = loader.load();
            ModifierUserController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            ctrl.setUser(selected);
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/StatsView.fxml"));
            Parent root = loader.load();
            StatsController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
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
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyProfile() {
        try {
            if (currentUser == null) {
                currentUser = services.SessionManager.getCurrentUser();
            }
            if (currentUser == null) {
                System.err.println("handleMyProfile: No logged-in user found.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent root = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            SceneHelper.switchScene(SceneHelper.stageOf(usersTable), root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refreshData() {
        loadStats();
        loadUsers(null);
    }

    @FXML
    private void handleToggleScreen() {
        Stage stage = SceneHelper.stageOf(usersTable);
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
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("najahni_users.csv");
        File file = fileChooser.showSaveDialog(SceneHelper.stageOf(usersTable));
        if (file != null) {
            try {
                ExportService.getInstance().exportToCSV(new java.util.ArrayList<>(usersList), file);
                showAlert(Alert.AlertType.INFORMATION, "Export CSV réussi !\nFichier: " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur lors de l'export CSV: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("najahni_users.pdf");
        File file = fileChooser.showSaveDialog(SceneHelper.stageOf(usersTable));
        if (file != null) {
            try {
                ExportService.getInstance().exportToPDF(new java.util.ArrayList<>(usersList), file);
                showAlert(Alert.AlertType.INFORMATION, "Export PDF réussi !\nFichier: " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur lors de l'export PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBroadcast() {
        // Show broadcast dialog
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(usersTable));
        dialog.setTitle("Email Broadcast");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: #F0F2F5;");

        Label title = new Label("📧 Diffusion Email");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E;");

        TextField subjectField = new TextField();
        subjectField.setPromptText("Sujet de l'email...");
        subjectField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-background-radius: 8;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Corps du message...");
        messageArea.setPrefRowCount(8);
        messageArea.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-background-radius: 8;");

        List<String> emails = userService.getAllEmails();
        Label countLabel = new Label("Destinataires: " + emails.size() + " utilisateurs");
        countLabel.setStyle("-fx-text-fill: #636E72;");

        Button sendBtn = new Button("Envoyer à tous");
        sendBtn.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #8B85FF); " +
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 10 30; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> {
            if (subjectField.getText().trim().isEmpty() || messageArea.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Veuillez remplir le sujet et le message.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Envoyer cet email à " + emails.size() + " utilisateurs ?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    sendBtn.setDisable(true);
                    sendBtn.setText("Envoi en cours...");
                    String subject = subjectField.getText().trim();
                    String message = messageArea.getText().trim();
                    Task<Void> sendTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            new EmailService().sendBroadcastToAll(emails, subject, message);
                            return null;
                        }
                    };
                    sendTask.setOnSucceeded(ev -> {
                        showAlert(Alert.AlertType.INFORMATION, "Emails envoyés avec succès !");
                        dialog.close();
                    });
                    sendTask.setOnFailed(ev -> {
                        sendBtn.setDisable(false);
                        sendBtn.setText("Envoyer à tous");
                        showAlert(Alert.AlertType.ERROR, "Erreur lors de l'envoi des emails.");
                    });
                    Thread t = new Thread(sendTask);
                    t.setDaemon(true);
                    t.start();
                }
            });
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E1E8ED; " +
                "-fx-border-radius: 10; -fx-padding: 10 30; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, sendBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(title, new Label("Sujet:"), subjectField,
                new Label("Message:"), messageArea, countLabel, buttons);

        Scene scene = new Scene(layout, 500, 480);
        dialog.setScene(scene);
        dialog.show();
    }

    @FXML
    private void handleThemeToggle() {
        themeService.toggleTheme(usersTable.getScene());
        if (currentUser != null) {
            userService.saveThemePreference(currentUser.getId(), themeService.getThemeName());
        }
    }

    @FXML
    private void handleNotifications() {
        if (currentUser == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(usersTable));
        dialog.setTitle("Notifications");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F0F2F5;");

        Label title = new Label("🔔 Notifications");
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
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(usersTable));
        dialog.setTitle("Historique de connexion");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #F0F2F5;");

        Label title = new Label("📋 Historique de connexion (Admin)");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E;");

        TableView<LoginHistory> historyTable = new TableView<>();
        historyTable.setPrefHeight(400);

        TableColumn<LoginHistory, String> userCol = new TableColumn<>("Utilisateur");
        userCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUserFullName() != null ? c.getValue().getUserFullName() : "ID: " + c.getValue().getUserId()));
        userCol.setPrefWidth(150);

        TableColumn<LoginHistory, String> methodCol = new TableColumn<>("Méthode");
        methodCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLoginMethod()));
        methodCol.setPrefWidth(90);

        TableColumn<LoginHistory, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIpAddress()));
        ipCol.setPrefWidth(120);

        TableColumn<LoginHistory, String> deviceCol = new TableColumn<>("Appareil");
        deviceCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDeviceInfo()));
        deviceCol.setPrefWidth(200);

        TableColumn<LoginHistory, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSuccessDisplay()));
        statusCol.setPrefWidth(70);

        TableColumn<LoginHistory, String> timeCol = new TableColumn<>("Date");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLoginTime() != null
                        ? c.getValue().getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        : ""));
        timeCol.setPrefWidth(150);

        historyTable.getColumns().addAll(userCol, methodCol, ipCol, deviceCol, statusCol, timeCol);

        List<LoginHistory> history = LoginHistoryService.getInstance().getAllLoginHistory(100);
        historyTable.setItems(FXCollections.observableArrayList(history));

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(title, historyTable, closeBtn);

        Scene scene = new Scene(layout, 850, 520);
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

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }
}
