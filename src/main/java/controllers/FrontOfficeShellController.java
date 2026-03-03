package controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.LoginHistory;
import models.Notification;
import models.User;
import services.*;
import tools.SceneHelper;

import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Front-Office Shell Controller for non-admin users.
 * Provides sidebar navigation with dynamic content loading (like AdminBackOfficeController).
 */
public class FrontOfficeShellController {

    // --- Top bar ---
    @FXML private Label welcomeLabel;
    @FXML private Label notificationBadge;
    @FXML private Button btnToggleScreen;
    @FXML private Button btnTheme;

    // --- Sidebar nav buttons ---
    @FXML private Button btnHome;
    @FXML private Button btnCommunity;
    @FXML private Button btnInvestissement;
    @FXML private Button btnMentorat;
    @FXML private Button btnProjets;
    @FXML private Button btnApprentissage;
    @FXML private Button btnProfile;
    @FXML private Button btnNetwork;
    @FXML private Button btnPassword;
    @FXML private Button btnHistory;

    // --- Content ---
    @FXML private StackPane contentArea;
    @FXML private HBox navBar;
    @FXML private Button btnBack;
    @FXML private Label breadcrumbLabel;
    @FXML private Label moduleIcon;
    @FXML private Label moduleTitle;
    @FXML private Label moduleDescription;

    // --- State ---
    private User currentUser;
    private Button[] allNavButtons;

    // --- Navigation history ---
    private final Deque<String> navigationHistory = new ArrayDeque<>();
    private String currentPage = "Accueil";
    private static final java.util.Map<String, String> PAGE_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("/views/HomeContent.fxml", "Accueil"),
            java.util.Map.entry("/views/community/CommunityHomePage.fxml", "Communaut\u00e9"),
            java.util.Map.entry("/views/investissement/FrontOfficeView.fxml", "Investissement"),
            java.util.Map.entry("/views/mentorat/FrontMentorat.fxml", "Mentorat"),
            java.util.Map.entry("/views/projets/ClientDashboard.fxml", "Projets"),
            java.util.Map.entry("/views/apprentissage/FrontOfficeView.fxml", "Apprentissage"),
            java.util.Map.entry("/views/Profil.fxml", "Mon Profil"),
            java.util.Map.entry("/views/Network.fxml", "R\u00e9seau"),
            java.util.Map.entry("/views/ChangePassword.fxml", "Mot de passe")
    );

    // --- Module metadata: icon, description, accent color ---
    private record ModuleMeta(String icon, String description, String accentColor) {}
    private static final java.util.Map<String, ModuleMeta> MODULE_META = java.util.Map.ofEntries(
            java.util.Map.entry("Accueil",         new ModuleMeta("\uD83C\uDFE0", "Tableau de bord personnel", "#3b82f6")),
            java.util.Map.entry("Communaut\u00e9", new ModuleMeta("\uD83C\uDFD8", "Forums, groupes & \u00e9v\u00e9nements", "#8b5cf6")),
            java.util.Map.entry("Investissement",  new ModuleMeta("\uD83D\uDCB0", "Portefeuille & analyses", "#f59e0b")),
            java.util.Map.entry("Mentorat",        new ModuleMeta("\uD83C\uDF93", "Sessions & mentors", "#10b981")),
            java.util.Map.entry("Projets",         new ModuleMeta("\uD83D\uDCCA", "Gestion de projets", "#ef4444")),
            java.util.Map.entry("Apprentissage",   new ModuleMeta("\uD83D\uDCDA", "Cours & badges", "#06b6d4")),
            java.util.Map.entry("Mon Profil",      new ModuleMeta("\uD83D\uDC64", "Informations personnelles", "#6366f1")),
            java.util.Map.entry("R\u00e9seau",     new ModuleMeta("\uD83D\uDC65", "Abonn\u00e9s & abonnements", "#ec4899")),
            java.util.Map.entry("Mot de passe",    new ModuleMeta("\uD83D\uDD12", "S\u00e9curit\u00e9 du compte", "#f97316")),
            java.util.Map.entry("Historique",       new ModuleMeta("\uD83D\uDCCB", "Historique de connexion", "#64748b"))
    );

    // --- Services ---
    private final NotificationService notificationService = NotificationService.getInstance();
    private final ThemeService themeService = ThemeService.getInstance();

    @FXML
    private void initialize() {
        allNavButtons = new Button[]{
                btnHome, btnCommunity, btnInvestissement, btnMentorat,
                btnProjets, btnApprentissage, btnProfile, btnNetwork,
                btnPassword, btnHistory
        };
    }

    /**
     * Called after FXML load to set the current user and show home content.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionService.getInstance().setCurrentUser(user);

        if (currentUser != null && welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + currentUser.getFullName());
        }

        // Load notification count in background
        loadNotificationCount();

        // Apply theme
        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            themeService.applyTheme(welcomeLabel.getScene());
        }

        // Show home page by default
        handleHome();
    }

    // ─── Content Loading ────────────────────────────────────────

    private void loadFXMLContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Try to pass current user to loaded controller
            Object ctrl = loader.getController();
            if (ctrl != null) {
                try {
                    ctrl.getClass().getMethod("setCurrentUser", User.class).invoke(ctrl, currentUser);
                } catch (NoSuchMethodException ignored) {
                    // Controller doesn't have setCurrentUser
                }
            }

            // Update navigation history
            String pageName = PAGE_NAMES.getOrDefault(fxmlPath, fxmlPath);
            if (!pageName.equals(currentPage)) {
                navigationHistory.push(currentPage);
            }
            currentPage = pageName;
            updateNavBar();

            loadContent(content);
        } catch (Exception e) {
            System.err.println("FrontOfficeShell: Failed to load " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            showErrorContent("Impossible de charger la page: " + e.getMessage());
        }
    }

    private void loadContent(Node content) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    /** Update the module header bar: visibility, icon, title, breadcrumb, accent color. */
    private void updateNavBar() {
        boolean isHome = "Accueil".equals(currentPage);
        if (navBar != null) {
            navBar.setVisible(!isHome);
            navBar.setManaged(!isHome);
        }
        if (isHome) return;

        ModuleMeta meta = MODULE_META.getOrDefault(currentPage,
                new ModuleMeta("\uD83D\uDCC4", "", "#475569"));

        if (moduleIcon != null) moduleIcon.setText(meta.icon());
        if (moduleTitle != null) moduleTitle.setText(currentPage);
        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText("Accueil  \u203A  " + currentPage);
        }
        if (moduleDescription != null) moduleDescription.setText(meta.description());

        // Apply accent color as left border
        if (navBar != null) {
            navBar.setStyle("-fx-border-color: transparent transparent transparent " + meta.accentColor() + ";"
                    + " -fx-border-width: 0 0 0 4px;");
        }
    }

    private void showErrorContent(String message) {
        VBox errorBox = new VBox(12);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        Label icon = new Label("\u26a0");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #ef4444;");
        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-wrap-text: true; -fx-max-width: 400;");
        msg.setWrapText(true);
        Button retry = new Button("Retour à l'accueil");
        retry.getStyleClass().addAll("fo-sidebar-btn");
        retry.setOnAction(e -> handleHome());
        errorBox.getChildren().addAll(icon, msg, retry);
        loadContent(errorBox);
    }

    private void setActiveButton(Button active) {
        for (Button b : allNavButtons) {
            if (b != null) {
                b.getStyleClass().remove("fo-sidebar-btn-active");
            }
        }
        if (active != null) {
            active.getStyleClass().add("fo-sidebar-btn-active");
        }
    }

    // ─── Sidebar Navigation Handlers ────────────────────────────

    @FXML
    private void handleHome() {
        setActiveButton(btnHome);
        // Reset navigation history when going home
        navigationHistory.clear();
        currentPage = "Accueil";
        loadFXMLContent("/views/HomeContent.fxml");
    }

    /** Clicking the logo in the top bar navigates to home. */
    @FXML
    private void handleLogoClick() {
        handleHome();
    }

    /** Navigate back in history. */
    @FXML
    private void handleBack() {
        if (!navigationHistory.isEmpty()) {
            String previousPage = navigationHistory.pop();
            currentPage = previousPage;
            // Find the FXML path for this page name
            String fxmlPath = PAGE_NAMES.entrySet().stream()
                    .filter(e -> e.getValue().equals(previousPage))
                    .map(java.util.Map.Entry::getKey)
                    .findFirst().orElse("/views/HomeContent.fxml");

            // Highlight correct sidebar button
            highlightButtonForPage(previousPage);
            updateNavBar();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try {
                        ctrl.getClass().getMethod("setCurrentUser", User.class).invoke(ctrl, currentUser);
                    } catch (NoSuchMethodException ignored) {}
                }
                loadContent(content);
            } catch (Exception e) {
                e.printStackTrace();
                handleHome();
            }
        } else {
            handleHome();
        }
    }

    private void highlightButtonForPage(String pageName) {
        switch (pageName) {
            case "Accueil" -> setActiveButton(btnHome);
            case "Communaut\u00e9" -> setActiveButton(btnCommunity);
            case "Investissement" -> setActiveButton(btnInvestissement);
            case "Mentorat" -> setActiveButton(btnMentorat);
            case "Projets" -> setActiveButton(btnProjets);
            case "Apprentissage" -> setActiveButton(btnApprentissage);
            case "Mon Profil" -> setActiveButton(btnProfile);
            case "R\u00e9seau" -> setActiveButton(btnNetwork);
            case "Mot de passe" -> setActiveButton(btnPassword);
            default -> setActiveButton(btnHome);
        }
    }

    @FXML
    private void handleCommunity() {
        setActiveButton(btnCommunity);
        loadFXMLContent("/views/community/CommunityHomePage.fxml");
    }

    @FXML
    private void handleInvestissement() {
        setActiveButton(btnInvestissement);
        loadFXMLContent("/views/investissement/FrontOfficeView.fxml");
    }

    @FXML
    private void handleMentorat() {
        setActiveButton(btnMentorat);
        loadFXMLContent("/views/mentorat/FrontMentorat.fxml");
    }

    @FXML
    private void handleProjets() {
        setActiveButton(btnProjets);
        loadFXMLContent("/views/projets/ClientDashboard.fxml");
    }

    @FXML
    private void handleApprentissage() {
        setActiveButton(btnApprentissage);
        loadFXMLContent("/views/apprentissage/FrontOfficeView.fxml");
    }

    @FXML
    private void handleProfile() {
        setActiveButton(btnProfile);
        loadFXMLContent("/views/Profil.fxml");
    }

    @FXML
    private void handleNetwork() {
        setActiveButton(btnNetwork);
        loadFXMLContent("/views/Network.fxml");
    }

    @FXML
    private void handleChangePassword() {
        setActiveButton(btnPassword);
        loadFXMLContent("/views/ChangePassword.fxml");
    }

    @FXML
    private void handleLoginHistory() {
        setActiveButton(btnHistory);
        // Show login history in a popup dialog (same as HomeController)
        if (currentUser == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(welcomeLabel));
        dialog.setTitle("Historique de connexion");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("\uD83D\uDCCB Mon historique de connexion");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label loadingLabel = new Label("Chargement...");
        loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        layout.getChildren().addAll(title, loadingLabel);

        Scene scene = new Scene(layout, 700, 520);
        dialog.setScene(scene);
        dialog.show();

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                SuspiciousLoginService suspiciousService = SuspiciousLoginService.getInstance();
                int riskScore = suspiciousService.analyzeLogin(currentUser);
                String riskLevel = suspiciousService.getRiskLevel(riskScore);
                String riskColor = suspiciousService.getRiskColor(riskScore);
                List<LoginHistory> history = LoginHistoryService.getInstance().getLoginHistory(currentUser.getId(), 50);
                List<String> devices = LoginHistoryService.getInstance().getUniqueDevices(currentUser.getId());

                javafx.application.Platform.runLater(() -> {
                    layout.getChildren().remove(loadingLabel);

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

                    TableColumn<LoginHistory, String> methodCol = new TableColumn<>("Méthode");
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
                    historyTable.setItems(FXCollections.observableArrayList(history));

                    Label devicesLabel = new Label("\uD83D\uDCF1 Appareils connus: " + devices.size());
                    devicesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636E72;");

                    Button closeBtn = new Button("Fermer");
                    closeBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
                    closeBtn.setOnAction(e -> dialog.close());

                    HBox buttons = new HBox(10, closeBtn);
                    buttons.setAlignment(Pos.CENTER_RIGHT);

                    layout.getChildren().addAll(riskBox, historyTable, devicesLabel, buttons);
                });
                return null;
            }
        };
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    // ─── Top Bar Handlers ───────────────────────────────────────

    @FXML
    private void handleThemeToggle() {
        themeService.toggleTheme(welcomeLabel.getScene());
        if (currentUser != null) {
            UserService.getInstance().saveThemePreference(currentUser.getId(), themeService.getThemeName());
        }
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
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("\uD83D\uDD14 Notifications");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        List<Notification> notifs = notificationService.getNotifications(currentUser.getId());

        ScrollPane scrollPane = new ScrollPane();
        VBox notifList = new VBox(8);
        notifList.setPadding(new Insets(5));

        if (notifs.isEmpty()) {
            Label empty = new Label("Aucune notification");
            empty.setStyle("-fx-text-fill: #64748b;");
            notifList.getChildren().add(empty);
        } else {
            for (Notification n : notifs) {
                VBox item = new VBox(4);
                item.setPadding(new Insets(12));
                item.setStyle(n.isRead()
                        ? "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10;"
                        : "-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-border-color: #2563eb; -fx-border-radius: 10;");

                Label nTitle = new Label(n.getTypeIcon() + " " + n.getTitle());
                nTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
                Label nMsg = new Label(n.getMessage());
                nMsg.setWrapText(true);
                nMsg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                Label nTime = new Label(n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                nTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

                item.getChildren().addAll(nTitle, nMsg, nTime);
                notifList.getChildren().add(item);
            }
        }

        scrollPane.setContent(notifList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        Button markAllBtn = new Button("Tout marquer lu");
        markAllBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        markAllBtn.setOnAction(e -> {
            notificationService.markAllAsRead(currentUser.getId());
            loadNotificationCount();
            dialog.close();
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, markAllBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(title, scrollPane, buttons);

        Scene scene = new Scene(layout, 480, 520);
        dialog.setScene(scene);
        dialog.show();
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

    // ─── Helpers ────────────────────────────────────────────────

    private void loadNotificationCount() {
        if (currentUser == null) return;
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return notificationService.getUnreadCount(currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            int count = task.getValue();
            if (notificationBadge != null) {
                if (count > 0) {
                    notificationBadge.setText(String.valueOf(count));
                    notificationBadge.setVisible(true);
                } else {
                    notificationBadge.setVisible(false);
                }
            }
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
