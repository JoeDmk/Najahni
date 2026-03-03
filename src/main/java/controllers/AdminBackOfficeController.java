package controllers;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;
import models.User;
import services.*;
import tools.SceneHelper;
import util.Type;

import java.util.List;

/**
 * Admin Back-Office Hub Controller with left sidebar navigation.
 * Content is loaded dynamically into the center contentArea StackPane.
 */
public class AdminBackOfficeController {

    // --- FXML Bindings ---
    @FXML private Label welcomeLabel;
    @FXML private StackPane contentArea;

    // Sidebar parent module buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnCommunity;
    @FXML private Button btnInvestment;
    @FXML private Button btnMentorat;
    @FXML private Button btnProjets;
    @FXML private Button btnApprentissage;

    // Collapsible sub-menu containers
    @FXML private VBox subMenuCommunity;
    @FXML private VBox subMenuInvestment;
    @FXML private VBox subMenuMentorat;
    @FXML private VBox subMenuProjets;
    @FXML private VBox subMenuApprentissage;

    // Community sub-items
    @FXML private Button btnCommunityGroups;
    @FXML private Button btnCommunityPosts;
    @FXML private Button btnCommunityEvents;

    // Investment sub-items
    @FXML private Button btnInvestDashboard;
    @FXML private Button btnInvestOpportunities;
    @FXML private Button btnInvestOffers;
    @FXML private Button btnInvestProjects;
    @FXML private Button btnInvestEconomic;

    // Mentorat sub-items
    @FXML private Button btnMentoratRequests;
    @FXML private Button btnMentoratSessions;
    @FXML private Button btnMentoratAvailability;
    @FXML private Button btnMentoratChatbot;

    // Projets sub-items
    @FXML private Button btnProjetsDashboard;
    @FXML private Button btnProjetsClient;

    // Apprentissage sub-items
    @FXML private Button btnAppDashboard;
    @FXML private Button btnAppCours;
    @FXML private Button btnAppBadges;
    @FXML private Button btnAppProgress;

    // Tool buttons
    @FXML private Button btnStats;
    @FXML private Button btnBroadcast;
    @FXML private Button btnHistory;
    @FXML private Button btnProfile;

    // --- Services ---
    private User currentUser;
    private final UserService userService = UserService.getInstance();
    private final ThemeService themeService = ThemeService.getInstance();

    // All sidebar nav buttons collected for active-state toggling
    private Button[] allNavButtons;

    @FXML
    private void initialize() {
        allNavButtons = new Button[]{
                btnDashboard, btnUsers,
                btnCommunityGroups, btnCommunityPosts, btnCommunityEvents,
                btnInvestDashboard, btnInvestOpportunities, btnInvestOffers, btnInvestProjects, btnInvestEconomic,
                btnMentoratRequests, btnMentoratSessions, btnMentoratAvailability, btnMentoratChatbot,
                btnProjetsDashboard, btnProjetsClient,
                btnAppDashboard, btnAppCours, btnAppBadges, btnAppProgress,
                btnStats, btnBroadcast, btnHistory, btnProfile
        };
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionService.getInstance().setCurrentUser(user);

        if (user != null && welcomeLabel != null) {
            welcomeLabel.setText("Admin: " + user.getFullName());
        }

        // Show the dashboard by default
        showDashboard();

        if (welcomeLabel != null && welcomeLabel.getScene() != null) {
            themeService.applyTheme(welcomeLabel.getScene());
        }
    }

    // ========== Active-state helper ==========

    private void setActiveButton(Button active) {
        if (allNavButtons == null) return;
        for (Button btn : allNavButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }
        if (active != null) {
            active.getStyleClass().add("nav-button-active");
        }
    }

    // ========== Collapsible Sub-Menu Animation ==========

    private void toggleSubMenu(VBox subMenu, Button parentBtn) {
        boolean expanding = !subMenu.isVisible();

        // Accordion: collapse all other sub-menus
        VBox[] allSubs = {subMenuCommunity, subMenuInvestment, subMenuMentorat, subMenuProjets, subMenuApprentissage};
        Button[] allParents = {btnCommunity, btnInvestment, btnMentorat, btnProjets, btnApprentissage};
        for (int i = 0; i < allSubs.length; i++) {
            if (allSubs[i] != subMenu && allSubs[i] != null && allSubs[i].isVisible()) {
                collapseSubMenu(allSubs[i], allParents[i]);
            }
        }

        if (expanding) {
            expandSubMenu(subMenu, parentBtn);
        } else {
            collapseSubMenu(subMenu, parentBtn);
        }
    }

    private void expandSubMenu(VBox subMenu, Button parentBtn) {
        subMenu.setManaged(true);
        subMenu.setVisible(true);
        subMenu.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), subMenu);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        parentBtn.setText(parentBtn.getText().replace("\u25B8", "\u25BE"));
    }

    private void collapseSubMenu(VBox subMenu, Button parentBtn) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), subMenu);
        ft.setFromValue(subMenu.getOpacity());
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            subMenu.setVisible(false);
            subMenu.setManaged(false);
        });
        ft.play();
        parentBtn.setText(parentBtn.getText().replace("\u25BE", "\u25B8"));
    }

    private void loadContent(Node content) {
        if (contentArea != null) {
            contentArea.getChildren().setAll(content);
        }
    }

    // ========== Dashboard (default view) ==========

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(24, 30, 30, 30));
        dashboard.setStyle("-fx-background-color: transparent;");

        // Welcome panel
        VBox welcomePanel = new VBox(10);
        welcomePanel.getStyleClass().add("welcome-panel");
        Label welcomeText = new Label("Bienvenue sur NAJAHNI");
        welcomeText.getStyleClass().add("welcome-text");
        Label welcomeSub = new Label("Panneau d'Administration — G\u00e9rez tous les modules de la plateforme");
        welcomeSub.getStyleClass().add("welcome-subtext");
        welcomePanel.getChildren().addAll(welcomeText, welcomeSub);

        // Stats section title
        Label statsTitle = new Label("\uD83D\uDCCA Statistiques de la Plateforme");
        statsTitle.getStyleClass().add("section-title");

        // Stat cards
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        VBox totalCard = createStatCard("0", "Total Utilisateurs", "#3498db");
        VBox entCard = createStatCard("0", "Entrepreneurs", "#27ae60");
        VBox mentCard = createStatCard("0", "Mentors", "#f39c12");
        VBox invCard = createStatCard("0", "Investisseurs", "#9b59b6");
        VBox banCard = createStatCard("0", "Bannis", "#e74c3c");
        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(entCard, Priority.ALWAYS);
        HBox.setHgrow(mentCard, Priority.ALWAYS);
        HBox.setHgrow(invCard, Priority.ALWAYS);
        HBox.setHgrow(banCard, Priority.ALWAYS);
        statsRow.getChildren().addAll(totalCard, entCard, mentCard, invCard, banCard);

        // Load stats async
        Task<int[]> statsTask = new Task<>() {
            @Override
            protected int[] call() {
                return new int[]{
                        userService.countTotal(),
                        userService.countByRole(Type.ENTREPRENEUR),
                        userService.countByRole(Type.MENTOR),
                        userService.countByRole(Type.INVESTISSEUR),
                        userService.countBanned()
                };
            }
        };
        statsTask.setOnSucceeded(e -> {
            int[] s = statsTask.getValue();
            ((Label) totalCard.getChildren().get(0)).setText(String.valueOf(s[0]));
            ((Label) entCard.getChildren().get(0)).setText(String.valueOf(s[1]));
            ((Label) mentCard.getChildren().get(0)).setText(String.valueOf(s[2]));
            ((Label) invCard.getChildren().get(0)).setText(String.valueOf(s[3]));
            ((Label) banCard.getChildren().get(0)).setText(String.valueOf(s[4]));
        });
        Thread t = new Thread(statsTask);
        t.setDaemon(true);
        t.start();

        // Quick actions
        Label actionsTitle = new Label("\u26A1 Actions Rapides");
        actionsTitle.getStyleClass().add("section-title");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnRefresh = new Button("\uD83D\uDD04 Actualiser");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> showDashboard());

        Button btnAddUser = new Button("➕ Ajouter Utilisateur");
        btnAddUser.getStyleClass().add("btn-primary");
        btnAddUser.setOnAction(e -> handleUsersBO());

        Button btnExport = new Button("\uD83D\uDCC4 Export CSV");
        btnExport.getStyleClass().add("btn-success");
        btnExport.setOnAction(e -> handleUsersBO());

        actions.getChildren().addAll(btnRefresh, btnAddUser, btnExport);

        // Module cards section
        Label modulesTitle = new Label("\uD83D\uDCC2 Modules Back-Office");
        modulesTitle.getStyleClass().add("section-title");

        HBox moduleRow1 = new HBox(20);
        moduleRow1.setAlignment(Pos.CENTER);
        moduleRow1.getChildren().addAll(
                createModuleCard("\uD83D\uDC65", "Utilisateurs", "CRUD, ban, export CSV/PDF", e -> handleUsersBO()),
                createModuleCard("\uD83C\uDFD8", "Communaut\u00e9", "Publications, mod\u00e9ration", e -> handleCommunityBO()),
                createModuleCard("\uD83D\uDCB0", "Investissement", "Opportunit\u00e9s, offres, IA", e -> handleInvestmentBO())
        );

        HBox moduleRow2 = new HBox(20);
        moduleRow2.setAlignment(Pos.CENTER);
        moduleRow2.getChildren().addAll(
                createModuleCard("\uD83C\uDF93", "Mentorat", "Disponibilit\u00e9s, sessions", e -> handleMentoratBO()),
                createModuleCard("\uD83D\uDCC1", "Projets", "T\u00e2ches, deadlines", e -> handleProjetsBO()),
                createModuleCard("\uD83D\uDCDA", "Apprentissage", "Cours, quiz, gamification", e -> handleApprentissageBO())
        );

        dashboard.getChildren().addAll(
                welcomePanel, statsTitle, statsRow, actionsTitle, actions,
                modulesTitle, moduleRow1, moduleRow2
        );

        scroll.setContent(dashboard);
        loadContent(scroll);
    }

    private VBox createStatCard(String value, String label, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("stat-card");
        card.setStyle("-fx-border-color: " + color + ";");

        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-value");
        valLabel.setStyle("-fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valLabel, nameLabel);
        return card;
    }

    private VBox createModuleCard(String icon, String title, String desc,
                                  javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("admin-module-card");
        card.setCursor(javafx.scene.Cursor.HAND);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0f172a;");

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        Button goBtn = new Button("Ouvrir \u25B6");
        goBtn.getStyleClass().add("btn-primary");
        goBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 18;");
        goBtn.setOnAction(action);

        card.getChildren().addAll(iconLabel, titleLabel, descLabel, goBtn);
        card.setOnMouseClicked(e -> goBtn.fire());
        return card;
    }

    // ========== Module Navigation (loads into contentArea) ==========

    @FXML
    private void handleUsersBO() {
        setActiveButton(btnUsers);
        loadFXMLContent("/views/Dashboard.fxml");
    }

    // ========== Module Toggle Handlers (sidebar parent buttons) ==========

    @FXML
    private void toggleCommunity() {
        toggleSubMenu(subMenuCommunity, btnCommunity);
    }

    @FXML
    private void toggleInvestment() {
        toggleSubMenu(subMenuInvestment, btnInvestment);
    }

    @FXML
    private void toggleMentorat() {
        toggleSubMenu(subMenuMentorat, btnMentorat);
    }

    @FXML
    private void toggleProjets() {
        toggleSubMenu(subMenuProjets, btnProjets);
    }

    @FXML
    private void toggleApprentissage() {
        toggleSubMenu(subMenuApprentissage, btnApprentissage);
    }

    // ========== Module BO Handlers (dashboard card compatibility) ==========

    private void handleCommunityBO() {
        if (!subMenuCommunity.isVisible()) expandSubMenu(subMenuCommunity, btnCommunity);
        setActiveButton(btnCommunity);
        loadFXMLContent("/views/community/CommunityAdmin.fxml");
    }

    private void handleInvestmentBO() {
        if (!subMenuInvestment.isVisible()) expandSubMenu(subMenuInvestment, btnInvestment);
        handleInvestDashboard();
    }

    private void handleMentoratBO() {
        if (!subMenuMentorat.isVisible()) expandSubMenu(subMenuMentorat, btnMentorat);
        setActiveButton(btnMentorat);
        loadFXMLContent("/views/mentorat/MentoratAdmin.fxml");
    }

    private void handleProjetsBO() {
        if (!subMenuProjets.isVisible()) expandSubMenu(subMenuProjets, btnProjets);
        handleProjetsDashboard();
    }

    private void handleApprentissageBO() {
        if (!subMenuApprentissage.isVisible()) expandSubMenu(subMenuApprentissage, btnApprentissage);
        handleAppDashboard();
    }

    // ========== Community Sub-Item Handlers ==========

    @FXML
    private void handleCommunityGroups() {
        setActiveButton(btnCommunityGroups);
        loadFXMLContent("/views/community/CommunityAdmin.fxml");
    }

    @FXML
    private void handleCommunityPosts() {
        setActiveButton(btnCommunityPosts);
        loadFXMLContent("/views/community/CommunityAdmin.fxml");
    }

    @FXML
    private void handleCommunityEvents() {
        setActiveButton(btnCommunityEvents);
        loadFXMLContent("/views/community/CommunityAdmin.fxml");
    }

    // ========== Investment Sub-Item Handlers ==========

    @FXML
    private void handleInvestDashboard() {
        setActiveButton(btnInvestDashboard);
        loadFXMLContent("/views/investissement/InvestDashboardView.fxml");
    }

    @FXML
    private void handleInvestOpportunities() {
        setActiveButton(btnInvestOpportunities);
        loadFXMLContent("/views/investissement/InvestmentView.fxml");
    }

    @FXML
    private void handleInvestOffers() {
        setActiveButton(btnInvestOffers);
        loadFXMLContent("/views/investissement/InvestmentOfferView.fxml");
    }

    @FXML
    private void handleInvestProjects() {
        setActiveButton(btnInvestProjects);
        loadFXMLContent("/views/investissement/ProjectView.fxml");
    }

    @FXML
    private void handleInvestEconomic() {
        setActiveButton(btnInvestEconomic);
        loadFXMLContent("/views/investissement/EconomicDashboardView.fxml");
    }

    // ========== Mentorat Sub-Item Handlers ==========

    @FXML
    private void handleMentoratRequests() {
        setActiveButton(btnMentoratRequests);
        loadFXMLContent("/views/mentorat/MentoratAdmin.fxml");
    }

    @FXML
    private void handleMentoratSessions() {
        setActiveButton(btnMentoratSessions);
        loadFXMLContent("/views/mentorat/MentoratAdmin.fxml");
    }

    @FXML
    private void handleMentoratAvailability() {
        setActiveButton(btnMentoratAvailability);
        loadFXMLContent("/views/mentorat/MentoratAdmin.fxml");
    }

    @FXML
    private void handleMentoratChatbot() {
        setActiveButton(btnMentoratChatbot);
        loadFXMLContent("/views/mentorat/Chatbot.fxml");
    }

    // ========== Projets Sub-Item Handlers ==========

    @FXML
    private void handleProjetsDashboard() {
        setActiveButton(btnProjetsDashboard);
        loadFXMLContent("/views/projets/DashboardProjets.fxml");
    }

    @FXML
    private void handleProjetsClient() {
        setActiveButton(btnProjetsClient);
        loadFXMLContent("/views/projets/ClientDashboard.fxml");
    }

    // ========== Apprentissage Sub-Item Handlers ==========

    @FXML
    private void handleAppDashboard() {
        setActiveButton(btnAppDashboard);
        loadFXMLContent("/views/apprentissage/ApprentissageView.fxml");
    }

    @FXML
    private void handleAppCours() {
        setActiveButton(btnAppCours);
        loadFXMLContent("/views/apprentissage/CoursView.fxml");
    }

    @FXML
    private void handleAppBadges() {
        setActiveButton(btnAppBadges);
        loadFXMLContent("/views/apprentissage/BadgeView.fxml");
    }

    @FXML
    private void handleAppProgress() {
        setActiveButton(btnAppProgress);
        loadFXMLContent("/views/apprentissage/ProgressionView.fxml");
    }

    private void loadFXMLContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // If the loaded controller has setCurrentUser, call it
            Object ctrl = loader.getController();
            if (ctrl != null) {
                try {
                    ctrl.getClass().getMethod("setCurrentUser", User.class).invoke(ctrl, currentUser);
                } catch (NoSuchMethodException ignored) {
                    // Controller doesn't have setCurrentUser — that's fine
                }
            }

            loadContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            // Show full error chain for debugging
            StringBuilder msg = new StringBuilder("\u26A0 Erreur de chargement: " + fxmlPath);
            Throwable cause = e;
            while (cause != null) {
                if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                    msg.append("\n").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
                }
                cause = cause.getCause();
            }
            Label errorLabel = new Label(msg.toString());
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-padding: 40;");
            errorLabel.setWrapText(true);
            loadContent(errorLabel);
        }
    }

    // ========== Tool Actions ==========

    @FXML
    private void handleMyProfile() {
        setActiveButton(btnProfile);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent content = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            loadContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStats() {
        setActiveButton(btnStats);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/StatsView.fxml"));
            Parent content = loader.load();
            StatsController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            loadContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBroadcast() {
        setActiveButton(btnBroadcast);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(welcomeLabel));
        dialog.setTitle("Email Broadcast");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("\uD83D\uDCE7 Diffusion Email");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        TextField subjectField = new TextField();
        subjectField.setPromptText("Sujet de l'email...");
        subjectField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Corps du message...");
        messageArea.setPrefRowCount(8);
        messageArea.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        List<String> emails = userService.getAllEmails();
        Label countLabel = new Label("Destinataires: " + emails.size() + " utilisateurs");
        countLabel.setStyle("-fx-text-fill: #64748b;");

        Button sendBtn = new Button("Envoyer \u00e0 tous");
        sendBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563eb, #4f46e5); " +
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 10 30; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> {
            if (subjectField.getText().trim().isEmpty() || messageArea.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir le sujet et le message.").showAndWait();
                return;
            }
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
                new Alert(Alert.AlertType.INFORMATION, "Emails envoy\u00e9s avec succ\u00e8s !").showAndWait();
                dialog.close();
            });
            sendTask.setOnFailed(ev -> {
                sendBtn.setDisable(false);
                sendBtn.setText("Envoyer \u00e0 tous");
                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'envoi.").showAndWait();
            });
            Thread thread = new Thread(sendTask);
            thread.setDaemon(true);
            thread.start();
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; " +
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
    private void handleLoginHistory() {
        setActiveButton(btnHistory);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(SceneHelper.stageOf(welcomeLabel));
        dialog.setTitle("Historique de connexion");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("\uD83D\uDCCB Historique de connexion (Admin)");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        TableView<models.LoginHistory> historyTable = new TableView<>();
        historyTable.setPrefHeight(400);

        TableColumn<models.LoginHistory, String> userCol = new TableColumn<>("Utilisateur");
        userCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUserFullName() != null ? c.getValue().getUserFullName() : "ID: " + c.getValue().getUserId()));
        userCol.setPrefWidth(150);

        TableColumn<models.LoginHistory, String> methodCol = new TableColumn<>("M\u00e9thode");
        methodCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLoginMethod()));
        methodCol.setPrefWidth(90);

        TableColumn<models.LoginHistory, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getIpAddress()));
        ipCol.setPrefWidth(120);

        TableColumn<models.LoginHistory, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSuccessDisplay()));
        statusCol.setPrefWidth(70);

        TableColumn<models.LoginHistory, String> timeCol = new TableColumn<>("Date");
        timeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getLoginTime() != null
                        ? c.getValue().getLoginTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        : ""));
        timeCol.setPrefWidth(150);

        historyTable.getColumns().addAll(userCol, methodCol, ipCol, statusCol, timeCol);

        List<models.LoginHistory> history = LoginHistoryService.getInstance().getAllLoginHistory(100);
        historyTable.setItems(FXCollections.observableArrayList(history));

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(title, historyTable, closeBtn);

        Scene scene = new Scene(layout, 700, 520);
        dialog.setScene(scene);
        dialog.show();
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
}