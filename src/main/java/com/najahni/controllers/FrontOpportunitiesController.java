package com.najahni.controllers;

import com.najahni.models.*;
import com.najahni.services.DeadlineService;
import com.najahni.services.GeminiService;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.services.ProjectService;
import com.najahni.services.SessionManager;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.AnimationUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur Front-Office pour parcourir les Opportunités d'Investissement.
 * 
 * Role-aware:
 * - INVESTOR: browse opportunities, click "Investir"
 * - ENTREPRENEUR: browse opportunities, create new ones on their projects
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class FrontOpportunitiesController {

    // ─── Stats ───────────────────────────────────────────────
    @FXML private Label lblTotalOpps;
    @FXML private Label lblOpenOpps;
    @FXML private Label lblTotalTarget;
    @FXML private Label lblFundedOpps;
    @FXML private Label lblHeroSubtitle;

    // ─── Filter ──────────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cboStatusFilter;

    // ─── Cards container ─────────────────────────────────────
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;

    // ─── Create opportunity form (entrepreneur only) ─────────
    @FXML private TitledPane newOppPane;
    @FXML private ComboBox<Project> cboProject;
    @FXML private TextField txtTargetAmount;
    @FXML private TextArea txtOppDescription;
    @FXML private DatePicker dpDeadline;
    @FXML private Label lblOppFormMsg;

    // ─── Services ────────────────────────────────────────────
    private final InvestmentOpportunityService opportunityService;
    private final ProjectService projectService;
    private final GeminiService geminiService;
    private List<InvestmentOpportunity> allOpportunities;
    private boolean isEntrepreneur;

    public FrontOpportunitiesController() {
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
        this.geminiService = new GeminiService();
    }

    @FXML
    public void initialize() {
        // Detect role
        User currentUser = SessionManager.getInstance().getCurrentUser();
        isEntrepreneur = currentUser != null && currentUser.getRole() == Role.ENTREPRENEUR;

        if (isEntrepreneur) {
            lblHeroSubtitle.setText("Gérez vos opportunités de financement et suivez les investissements sur vos projets.");
            // Show the create form
            newOppPane.setVisible(true);
            newOppPane.setManaged(true);
            setupOppForm();
        }

        setupFilters();
        loadData();
    }

    // ─── SETUP ───────────────────────────────────────────────

    private void setupFilters() {
        List<String> statuses = Arrays.stream(OpportunityStatus.values())
            .map(OpportunityStatus::getDisplayName)
            .collect(Collectors.toList());
        statuses.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statuses));
        cboStatusFilter.setValue("Tous");
    }

    private void setupOppForm() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        List<Project> projects = projectService.findByEntrepreneur(userId);
        cboProject.setItems(FXCollections.observableArrayList(projects));
        cboProject.setConverter(new StringConverter<>() {
            @Override
            public String toString(Project p) {
                if (p == null) return "";
                return "#" + p.getId() + " — " + p.getTitle()
                    + (p.getSector() != null ? " [" + p.getSector() + "]" : "");
            }
            @Override
            public Project fromString(String s) { return null; }
        });
    }

    // ─── DATA LOADING ────────────────────────────────────────

    private void loadData() {
        allOpportunities = opportunityService.findAll();
        updateStats();
        displayCards(allOpportunities);
    }

    private void updateStats() {
        int total = allOpportunities.size();
        long open = allOpportunities.stream().filter(o -> o.getStatus() == OpportunityStatus.OPEN).count();
        long funded = allOpportunities.stream().filter(o -> o.getStatus() == OpportunityStatus.FUNDED).count();
        BigDecimal totalTarget = allOpportunities.stream()
            .map(InvestmentOpportunity::getTargetAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalOpps.setText(String.valueOf(total));
        lblOpenOpps.setText(String.valueOf(open));
        lblFundedOpps.setText(String.valueOf(funded));
        lblTotalTarget.setText(String.format("%,.0f €", totalTarget));
    }

    // ─── CARD DISPLAY ────────────────────────────────────────

    private void displayCards(List<InvestmentOpportunity> opportunities) {
        cardsContainer.getChildren().clear();

        if (opportunities.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        int delay = 0;
        for (InvestmentOpportunity opp : opportunities) {
            VBox card = createOpportunityCard(opp);
            cardsContainer.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 250, delay);
            delay += 60;
        }
    }

    private VBox createOpportunityCard(InvestmentOpportunity opp) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(280);
        card.setMinWidth(260);
        card.setMaxWidth(300);
        card.setPadding(new Insets(20));

        // ── Status badge ──
        Label statusBadge = new Label(opp.getStatus().getDisplayName());
        statusBadge.getStyleClass().add("front-badge");
        switch (opp.getStatus()) {
            case OPEN:   statusBadge.getStyleClass().add("front-badge-green"); break;
            case CLOSED: statusBadge.getStyleClass().add("front-badge-red"); break;
            case FUNDED: statusBadge.getStyleClass().add("front-badge-blue"); break;
        }

        HBox topBar = new HBox(statusBadge);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // ── Project name ──
        String projectName = opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet #" + opp.getProjectId();
        Label lblProject = new Label("📁 " + projectName);
        lblProject.getStyleClass().add("front-card-project");
        lblProject.setWrapText(true);

        // ── Amount (big) ──
        Label lblAmount = new Label(opp.getFormattedAmount());
        lblAmount.getStyleClass().add("front-card-amount");

        // ── Description ──
        String desc = opp.getDescription() != null ? opp.getDescription() : "Pas de description";
        if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
        Label lblDesc = new Label(desc);
        lblDesc.getStyleClass().add("front-card-desc");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(60);

        // ── Deadline badge coloré ──
        Label lblDeadline = new Label(DeadlineService.getDeadlineBadge(opp.getDeadline()));
        lblDeadline.setStyle(DeadlineService.getDeadlineStyle(opp.getDeadline()));
        lblDeadline.getStyleClass().add("front-card-meta");

        // ── Separator ──
        Separator sep = new Separator();

        // ── Action buttons ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        // AI Insights button — always available
        Button btnAI = new Button("🧠 Insights IA");
        btnAI.getStyleClass().add("front-btn-ai");
        btnAI.setOnAction(e -> showAIInsights(opp, card));

        if (isEntrepreneur) {
            // Entrepreneurs don't see "Investir", they see a status label
            if (opp.getStatus() == OpportunityStatus.OPEN) {
                Label lblOpen = new Label("🟢 En attente d'offres");
                lblOpen.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 12px;");
                actions.getChildren().addAll(lblOpen, btnAI);
            } else {
                Label lblClosed = new Label(opp.getStatus() == OpportunityStatus.FUNDED ? "✅ Objectif atteint" : "🔒 Fermée");
                lblClosed.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                actions.getChildren().addAll(lblClosed, btnAI);
            }
        } else {
            // Investor mode — show "Investir" button on OPEN opportunities
            if (opp.getStatus() == OpportunityStatus.OPEN) {
                Button btnInvest = new Button("💰 Investir");
                btnInvest.getStyleClass().add("front-btn-invest");
                HBox.setHgrow(btnInvest, Priority.ALWAYS);
                btnInvest.setOnAction(e -> navigateToOfferForm(opp));
                actions.getChildren().addAll(btnInvest, btnAI);
            } else {
                Label lblClosed = new Label(opp.getStatus() == OpportunityStatus.FUNDED ? "✅ Objectif atteint" : "🔒 Fermée");
                lblClosed.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                actions.getChildren().addAll(lblClosed, btnAI);
            }
        }

        card.getChildren().addAll(topBar, lblProject, lblAmount, lblDesc, lblDeadline, sep, actions);
        return card;
    }

    // ─── FILTER ACTIONS ──────────────────────────────────────

    @FXML
    public void applyFilter() {
        String search = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String statusFilter = cboStatusFilter.getValue();

        List<InvestmentOpportunity> filtered = allOpportunities.stream()
            .filter(opp -> {
                // Status filter
                boolean matchesStatus = statusFilter == null || statusFilter.equals("Tous")
                    || opp.getStatus().getDisplayName().equals(statusFilter);

                // Text search
                boolean matchesSearch = search.isEmpty()
                    || (opp.getProjectTitle() != null && opp.getProjectTitle().toLowerCase().contains(search))
                    || (opp.getDescription() != null && opp.getDescription().toLowerCase().contains(search));

                return matchesStatus && matchesSearch;
            })
            .collect(Collectors.toList());

        displayCards(filtered);
    }

    @FXML
    public void clearFilter() {
        txtSearch.clear();
        cboStatusFilter.setValue("Tous");
        displayCards(allOpportunities);
    }

    // ─── NAVIGATION ──────────────────────────────────────────

    private void navigateToOfferForm(InvestmentOpportunity opp) {
        try {
            FrontOfficeController foController = getFrontOfficeController();
            if (foController != null) {
                foController.showOffers(opp.getId());
            }
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private FrontOfficeController getFrontOfficeController() {
        try {
            javafx.scene.Parent root = cardsContainer.getScene().getRoot();
            Object userData = root.getUserData();
            if (userData instanceof FrontOfficeController) {
                return (FrontOfficeController) userData;
            }
        } catch (Exception e) {
            // Fallback - ignore
        }
        return null;
    }

    // ─── CREATE OPPORTUNITY (Entrepreneur only) ──────────────

    @FXML
    public void submitOpportunity() {
        if (lblOppFormMsg != null) lblOppFormMsg.setText("");

        try {
            if (cboProject.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un projet.");
            }

            String amountText = txtTargetAmount.getText() != null ? txtTargetAmount.getText().trim() : "";
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Le montant cible est obligatoire.");
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format de montant invalide.");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Le montant doit être supérieur à zéro.");
            }
            if (amount.compareTo(new BigDecimal("10000000")) > 0) {
                throw new IllegalArgumentException("Le montant ne peut pas dépasser 10 000 000 €.");
            }

            String description = txtOppDescription.getText() != null ? txtOppDescription.getText().trim() : "";
            if (description.isEmpty()) {
                throw new IllegalArgumentException("La description est obligatoire.");
            }
            if (description.length() < 10) {
                throw new IllegalArgumentException("La description doit contenir au moins 10 caractères.");
            }
            if (description.length() > 2000) {
                throw new IllegalArgumentException("La description ne peut pas dépasser 2000 caractères.");
            }

            LocalDate deadline = dpDeadline.getValue();
            if (deadline == null) {
                throw new IllegalArgumentException("La deadline est obligatoire.");
            }
            if (deadline.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La deadline ne peut pas être dans le passé.");
            }

            InvestmentOpportunity opp = new InvestmentOpportunity();
            opp.setTargetAmount(amount);
            opp.setDescription(description);
            opp.setDeadline(deadline);
            opp.setStatus(OpportunityStatus.OPEN);
            opp.setProjectId(cboProject.getValue().getId());

            InvestmentOpportunity created = opportunityService.createOpportunity(opp);
            if (created != null) {
                AlertUtils.showSuccess("Opportunité créée avec succès !");
                clearOppForm();
                newOppPane.setExpanded(false);
                loadData();
            } else {
                AlertUtils.showError("Erreur", "Impossible de créer l'opportunité.");
            }

        } catch (IllegalArgumentException e) {
            if (lblOppFormMsg != null) lblOppFormMsg.setText(e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    public void clearOppForm() {
        if (cboProject != null) cboProject.setValue(null);
        if (txtTargetAmount != null) txtTargetAmount.clear();
        if (txtOppDescription != null) txtOppDescription.clear();
        if (dpDeadline != null) dpDeadline.setValue(null);
        if (lblOppFormMsg != null) lblOppFormMsg.setText("");
    }

    // ═══════════════════════════════════════════════════════════
    //  AI INSIGHTS — Gemini-powered project analysis
    // ═══════════════════════════════════════════════════════════

    private void showAIInsights(InvestmentOpportunity opp, VBox card) {
        // Get the scene root StackPane for overlay
        StackPane rootStack = getSceneRootStack();
        if (rootStack == null) return;

        // Blur ALL existing children
        for (javafx.scene.Node child : rootStack.getChildren()) {
            child.setEffect(new GaussianBlur(8));
        }

        // Overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlay.setOnMouseClicked(e -> closeInsightsPopup(rootStack, overlay));

        // ── Main popup container ──
        VBox popup = new VBox(0);
        popup.setPrefWidth(600);
        popup.setMaxWidth(640);
        popup.setMaxHeight(720);
        popup.setMinWidth(480);
        popup.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 40, 0, 0, 10);");
        popup.setOnMouseClicked(e -> e.consume());
        StackPane.setAlignment(popup, Pos.CENTER);

        // ── HEADER — premium gradient ──
        String projectName = opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet #" + opp.getProjectId();
        VBox header = buildInsightsHeader(projectName);

        // ── ANALYTICS CARDS ROW — real data ──
        HBox analyticsRow = buildAnalyticsRow(opp);

        // ── CONTENT AREA — will show loading then AI response ──
        VBox contentArea = new VBox(12);
        contentArea.setPadding(new Insets(16, 24, 10, 24));
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Loading state
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(44, 44);
        spinner.setStyle("-fx-progress-color: #667eea;");
        Label loadingLabel = new Label("Analyse IA en cours...");
        loadingLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        loadingLabel.setTextFill(Color.web("#7f8c8d"));
        Label loadingSub = new Label("Notre IA analyse les données du projet,\nle marché et les facteurs de risque.");
        loadingSub.setFont(Font.font("Segoe UI", 12));
        loadingSub.setTextFill(Color.web("#95a5a6"));
        loadingSub.setWrapText(true);
        loadingSub.setAlignment(Pos.CENTER);
        loadingSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox loadingBox = new VBox(10, spinner, loadingLabel, loadingSub);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(40, 0, 40, 0));
        contentArea.getChildren().add(loadingBox);

        // ── FOOTER ──
        HBox footer = buildInsightsFooter(rootStack, overlay);

        // Scrollable wrapper for analytics + content
        VBox scrollContent = new VBox(0, analyticsRow, contentArea);
        ScrollPane scroll = new ScrollPane(scrollContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefHeight(500);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        popup.getChildren().addAll(header, scroll, footer);
        overlay.getChildren().add(popup);
        rootStack.getChildren().add(overlay);

        // Animate popup in
        popup.setScaleX(0.85); popup.setScaleY(0.85); popup.setOpacity(0);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(300), popup);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(javafx.animation.Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(250), popup);
        ft.setToValue(1);
        new javafx.animation.ParallelTransition(st, ft).play();

        // ── Call AI ──
        String desc = opp.getDescription() != null ? opp.getDescription() : "Pas de description";
        String deadline = opp.getDeadline() != null
                ? opp.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Non définie";
        double amount = opp.getTargetAmount() != null ? opp.getTargetAmount().doubleValue() : 0;

        // Try to get sector from project
        String sector = "N/A";
        try {
            var projectOpt = projectService.findById(opp.getProjectId());
            if (projectOpt.isPresent() && projectOpt.get().getSector() != null) {
                sector = projectOpt.get().getSector();
            }
        } catch (Exception ignored) {}

        geminiService.generateProjectInsights(projectName, sector, desc, amount, deadline)
                .thenAccept(response -> Platform.runLater(() -> {
                    contentArea.getChildren().clear();
                    VBox insightsContent = buildInsightsContent(response, opp);
                    contentArea.getChildren().add(insightsContent);
                    AnimationUtils.playFadeScaleIn(insightsContent, 350, 0);
                }));
    }

    // ═══════════════════════════════════════════════════════════
    //  INSIGHTS — UI COMPONENTS
    // ═══════════════════════════════════════════════════════════

    private VBox buildInsightsHeader(String projectName) {
        VBox header = new VBox(6);
        header.setPadding(new Insets(22, 28, 16, 28));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); "
                + "-fx-background-radius: 20 20 0 0;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label aiIcon = new Label("🧠");
        aiIcon.setFont(Font.font(24));
        Label popupTitle = new Label("Insights IA");
        popupTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        popupTitle.setTextFill(Color.WHITE);

        Label badgeLabel = new Label(geminiService.getBackendName());
        badgeLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 10));
        badgeLabel.setTextFill(Color.web("#667eea"));
        badgeLabel.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10; "
                + "-fx-padding: 3 8 3 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(aiIcon, popupTitle, spacer, badgeLabel);

        Label popupSub = new Label("📌 " + projectName);
        popupSub.setFont(Font.font("Segoe UI", 13));
        popupSub.setTextFill(Color.web("#e0d4ff"));

        header.getChildren().addAll(titleRow, popupSub);
        return header;
    }

    private HBox buildAnalyticsRow(InvestmentOpportunity opp) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(16, 24, 8, 24));
        row.setAlignment(Pos.CENTER);

        // Card 1: Amount
        String amountText = opp.getFormattedAmount();
        VBox amountCard = buildStatCard("💰", "Montant Cible", amountText, "#667eea", "#eef0ff");

        // Card 2: Deadline countdown
        String deadlineText;
        String deadlineColor;
        if (opp.getDeadline() != null) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), opp.getDeadline());
            if (daysLeft < 0) { deadlineText = "Expiré"; deadlineColor = "#e74c3c"; }
            else if (daysLeft <= 30) { deadlineText = daysLeft + " jours"; deadlineColor = "#e67e22"; }
            else if (daysLeft <= 90) { deadlineText = daysLeft + " jours"; deadlineColor = "#f39c12"; }
            else { deadlineText = daysLeft + " jours"; deadlineColor = "#27ae60"; }
        } else { deadlineText = "—"; deadlineColor = "#95a5a6"; }
        VBox deadlineCard = buildStatCard("📅", "Délai Restant", deadlineText, deadlineColor, "#fff8f0");

        // Card 3: Risk Score
        String riskText;
        String riskColor;
        if (opp.getRiskScore() != null) {
            int score = (int) Math.round(opp.getRiskScore());
            riskText = score + "/100";
            if (score <= 33) riskColor = "#27ae60";
            else if (score <= 66) riskColor = "#f39c12";
            else riskColor = "#e74c3c";
        } else { riskText = "—"; riskColor = "#95a5a6"; }
        VBox riskCard = buildStatCard("📊", "Score Risque", riskText, riskColor, "#f0fff4");

        // Card 4: Status
        String statusText = opp.getStatus().getDisplayName();
        String statusColor = switch (opp.getStatus()) {
            case OPEN -> "#27ae60";
            case FUNDED -> "#3498db";
            case CLOSED -> "#e74c3c";
        };
        VBox statusCard = buildStatCard("🔖", "Statut", statusText, statusColor, "#f0f8ff");

        row.getChildren().addAll(amountCard, deadlineCard, riskCard, statusCard);
        HBox.setHgrow(amountCard, Priority.ALWAYS);
        HBox.setHgrow(deadlineCard, Priority.ALWAYS);
        HBox.setHgrow(riskCard, Priority.ALWAYS);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        return row;
    }

    private VBox buildStatCard(String icon, String label, String value, String valueColor, String bgColor) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 8, 10, 8));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; "
                + "-fx-border-color: " + valueColor + "22; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setMinWidth(100);

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(18));
        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", 10));
        labelLbl.setTextFill(Color.web("#7f8c8d"));
        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        valueLbl.setTextFill(Color.web(valueColor));
        valueLbl.setWrapText(true);
        valueLbl.setAlignment(Pos.CENTER);
        valueLbl.setMaxWidth(110);

        card.getChildren().addAll(iconLbl, labelLbl, valueLbl);
        return card;
    }

    private HBox buildInsightsFooter(StackPane rootStack, StackPane overlay) {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 20 20; "
                + "-fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        Label powered = new Label("⚡ Powered by " + geminiService.getBackendName());
        powered.setFont(Font.font("Segoe UI", 10));
        powered.setTextFill(Color.web("#b0b0b0"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Fermer");
        closeBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); "
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 28 8 28; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #5a6fd6, #6a4199); "
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 28 8 28; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); "
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 28 8 28; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> closeInsightsPopup(rootStack, overlay));

        footer.getChildren().addAll(powered, spacer, closeBtn);
        return footer;
    }

    // ═══════════════════════════════════════════════════════════
    //  INSIGHTS — CONTENT BUILDER (parses AI structured response)
    // ═══════════════════════════════════════════════════════════

    private VBox buildInsightsContent(String response, InvestmentOpportunity opp) {
        VBox content = new VBox(12);

        // Try to parse structured tags
        String score = extractTag(response, "SCORE");
        String riskLevel = extractTag(response, "RISKLEVEL");
        String confidence = extractTag(response, "CONFIDENCE");
        String summary = extractTag(response, "SUMMARY");
        java.util.List<String> strengths = extractMultipleTags(response, "STRENGTH");
        java.util.List<String> risks = extractMultipleTags(response, "RISK");
        String market = extractTag(response, "MARKET");
        String recommendation = extractTag(response, "RECOMMENDATION");

        boolean hasStructured = summary != null && !strengths.isEmpty();

        if (hasStructured) {
            // ── AI Scores Row ──
            if (score != null || riskLevel != null || confidence != null) {
                HBox scoresRow = new HBox(10);
                scoresRow.setAlignment(Pos.CENTER);

                if (score != null) {
                    scoresRow.getChildren().add(buildScoreBadge("🎯 Attractivité", score + "/10",
                            parseScoreColor(score)));
                }
                if (riskLevel != null) {
                    String rlColor = switch (riskLevel.toLowerCase()) {
                        case "faible" -> "#27ae60";
                        case "modéré" -> "#f39c12";
                        case "élevé" -> "#e74c3c";
                        default -> "#95a5a6";
                    };
                    scoresRow.getChildren().add(buildScoreBadge("⚡ Risque IA", riskLevel, rlColor));
                }
                if (confidence != null) {
                    int conf = 50;
                    try { conf = Integer.parseInt(confidence.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
                    String confColor = conf >= 70 ? "#27ae60" : conf >= 40 ? "#f39c12" : "#e74c3c";
                    scoresRow.getChildren().add(buildScoreBadge("📈 Confiance", confidence + "%", confColor));
                }
                content.getChildren().add(scoresRow);
            }

            // ── Summary Section ──
            if (summary != null) {
                content.getChildren().add(buildSection("📋 Résumé Exécutif", summary, "#667eea", "#f0f0ff"));
            }

            // ── Strengths Section ──
            if (!strengths.isEmpty()) {
                VBox strengthsBox = new VBox(6);
                Label strengthTitle = new Label("✅ Points Forts");
                strengthTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                strengthTitle.setTextFill(Color.web("#27ae60"));
                strengthsBox.getChildren().add(strengthTitle);
                for (String s : strengths) {
                    strengthsBox.getChildren().add(buildBulletItem(s, "#27ae60", "#f0fff4"));
                }
                content.getChildren().add(strengthsBox);
            }

            // ── Risks Section ──
            if (!risks.isEmpty()) {
                VBox risksBox = new VBox(6);
                Label riskTitle = new Label("⚠️ Risques Potentiels");
                riskTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                riskTitle.setTextFill(Color.web("#e67e22"));
                risksBox.getChildren().add(riskTitle);
                for (String r : risks) {
                    risksBox.getChildren().add(buildBulletItem(r, "#e67e22", "#fff8f0"));
                }
                content.getChildren().add(risksBox);
            }

            // ── Market Section ──
            if (market != null) {
                content.getChildren().add(buildSection("🇹🇳 Analyse Marché", market, "#3498db", "#f0f8ff"));
            }

            // ── Recommendation Section ──
            if (recommendation != null) {
                VBox recBox = new VBox(6);
                recBox.setPadding(new Insets(12, 16, 12, 16));
                recBox.setStyle("-fx-background-color: linear-gradient(to right, #667eea15, #764ba215); "
                        + "-fx-background-radius: 12; -fx-border-color: #667eea44; -fx-border-radius: 12; -fx-border-width: 1;");
                Label recTitle = new Label("💡 Recommandation");
                recTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                recTitle.setTextFill(Color.web("#764ba2"));
                Label recText = new Label(recommendation);
                recText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                recText.setTextFill(Color.web("#2c3e50"));
                recText.setWrapText(true);
                recBox.getChildren().addAll(recTitle, recText);
                content.getChildren().add(recBox);
            }

        } else {
            // Fallback — display the full response in a styled TextFlow
            content.getChildren().add(buildSection("🧠 Analyse IA", response, "#667eea", "#f8f9fa"));
        }

        // ── Risk progress bar (if risk score available) ──
        if (opp.getRiskScore() != null) {
            content.getChildren().add(buildRiskBar(opp.getRiskScore()));
        }

        return content;
    }

    private VBox buildScoreBadge(String label, String value, String color) {
        VBox badge = new VBox(2);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(10, 16, 10, 16));
        badge.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 14; "
                + "-fx-border-color: " + color + "44; -fx-border-radius: 14; -fx-border-width: 1.5;");
        badge.setMinWidth(110);

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        valueLbl.setTextFill(Color.web(color));
        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", 11));
        labelLbl.setTextFill(Color.web("#555"));

        badge.getChildren().addAll(valueLbl, labelLbl);
        HBox.setHgrow(badge, Priority.ALWAYS);
        return badge;
    }

    private VBox buildSection(String title, String text, String accentColor, String bgColor) {
        VBox section = new VBox(6);
        section.setPadding(new Insets(12, 16, 12, 16));
        section.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web(accentColor));
        Label textLbl = new Label(text);
        textLbl.setFont(Font.font("Segoe UI", 12.5));
        textLbl.setTextFill(Color.web("#2c3e50"));
        textLbl.setWrapText(true);
        textLbl.setLineSpacing(2);

        section.getChildren().addAll(titleLbl, textLbl);
        return section;
    }

    private HBox buildBulletItem(String text, String color, String bgColor) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12));
        item.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");

        Label dot = new Label("●");
        dot.setFont(Font.font(8));
        dot.setTextFill(Color.web(color));
        Label textLbl = new Label(text);
        textLbl.setFont(Font.font("Segoe UI", 12));
        textLbl.setTextFill(Color.web("#2c3e50"));
        textLbl.setWrapText(true);
        HBox.setHgrow(textLbl, Priority.ALWAYS);

        item.getChildren().addAll(dot, textLbl);
        return item;
    }

    private VBox buildRiskBar(double riskScore) {
        VBox riskSection = new VBox(6);
        riskSection.setPadding(new Insets(12, 16, 12, 16));
        riskSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; "
                + "-fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 1;");

        int score = (int) Math.round(riskScore);
        String barColor;
        if (score <= 33) barColor = "#27ae60";
        else if (score <= 50) barColor = "#f39c12";
        else if (score <= 66) barColor = "#e67e22";
        else barColor = "#e74c3c";

        Label title = new Label("📊 Indice de Risque Algorithmique");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#555"));

        // Progress bar container
        StackPane barContainer = new StackPane();
        barContainer.setPrefHeight(22);
        barContainer.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 11;");

        Region barFill = new Region();
        barFill.setMaxWidth(0);
        barFill.setPrefHeight(22);
        barFill.setStyle("-fx-background-color: linear-gradient(to right, " + barColor + ", " + barColor + "cc); "
                + "-fx-background-radius: 11;");
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

        Label scoreLbl = new Label(score + "/100");
        scoreLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        scoreLbl.setTextFill(Color.WHITE);

        barContainer.getChildren().addAll(barFill, scoreLbl);

        riskSection.getChildren().addAll(title, barContainer);

        // Animate bar width
        Platform.runLater(() -> {
            double maxWidth = barContainer.getWidth();
            if (maxWidth <= 0) maxWidth = 400;
            double targetWidth = Math.max(30, (score / 100.0) * maxWidth);
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                            new javafx.animation.KeyValue(barFill.maxWidthProperty(), 0)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(800),
                            new javafx.animation.KeyValue(barFill.maxWidthProperty(), targetWidth,
                                    javafx.animation.Interpolator.EASE_OUT))
            );
            timeline.setDelay(javafx.util.Duration.millis(200));
            timeline.play();
        });

        return riskSection;
    }

    private String parseScoreColor(String scoreStr) {
        try {
            int s = Integer.parseInt(scoreStr.replaceAll("[^0-9]", ""));
            if (s >= 7) return "#27ae60";
            if (s >= 5) return "#f39c12";
            return "#e74c3c";
        } catch (Exception e) { return "#667eea"; }
    }

    // ═══════════════════════════════════════════════════════════
    //  TAG PARSING — extract structured AI response
    // ═══════════════════════════════════════════════════════════

    private String extractTag(String text, String tag) {
        String open = "[" + tag + "]";
        String close = "[/" + tag + "]";
        int start = text.indexOf(open);
        if (start == -1) return null;
        start += open.length();
        int end = text.indexOf(close, start);
        if (end == -1) return null;
        return text.substring(start, end).trim();
    }

    private java.util.List<String> extractMultipleTags(String text, String tag) {
        java.util.List<String> results = new java.util.ArrayList<>();
        String open = "[" + tag + "]";
        String close = "[/" + tag + "]";
        int searchFrom = 0;
        while (true) {
            int start = text.indexOf(open, searchFrom);
            if (start == -1) break;
            start += open.length();
            int end = text.indexOf(close, start);
            if (end == -1) break;
            String val = text.substring(start, end).trim();
            if (!val.isEmpty()) results.add(val);
            searchFrom = end + close.length();
        }
        return results;
    }

    private void closeInsightsPopup(StackPane rootStack, StackPane overlay) {
        // Remove blur from ALL children (except the overlay itself)
        for (javafx.scene.Node child : rootStack.getChildren()) {
            if (child != overlay) child.setEffect(null);
        }
        rootStack.getChildren().remove(overlay);
    }

    private StackPane getSceneRootStack() {
        try {
            javafx.scene.Scene scene = cardsContainer.getScene();
            if (scene == null) return null;
            javafx.scene.Parent root = scene.getRoot();
            // If already a StackPane, use it directly
            if (root instanceof StackPane sp) return sp;
            // Wrap the existing scene root in a StackPane (one-time)
            // so the overlay covers the ENTIRE window (including navbar)
            StackPane wrapper = new StackPane(root);
            scene.setRoot(wrapper);
            return wrapper;
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

}
