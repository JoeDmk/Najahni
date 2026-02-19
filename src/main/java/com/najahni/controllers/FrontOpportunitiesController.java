package com.najahni.controllers;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Project;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.services.ProjectService;
import com.najahni.utils.AnimationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur Front-Office pour parcourir les Opportunités d'Investissement.
 * 
 * Affichage sous forme de cartes visuelles avec filtrage et recherche.
 * Pas de formulaire de création ici — c'est le côté "investisseur" (consultation).
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class FrontOpportunitiesController {

    // ─── Stats ───────────────────────────────────────────────
    @FXML private Label lblTotalOpps;
    @FXML private Label lblOpenOpps;
    @FXML private Label lblTotalTarget;
    @FXML private Label lblFundedOpps;

    // ─── Filter ──────────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cboStatusFilter;

    // ─── Cards container ─────────────────────────────────────
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;

    // ─── Services ────────────────────────────────────────────
    private final InvestmentOpportunityService opportunityService;
    private final ProjectService projectService;
    private List<InvestmentOpportunity> allOpportunities;

    public FrontOpportunitiesController() {
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
    }

    @FXML
    public void initialize() {
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

        // ── Deadline ──
        String deadlineText = "Pas de deadline";
        if (opp.getDeadline() != null) {
            deadlineText = "📅 " + opp.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            if (opp.getDeadline().isBefore(LocalDate.now())) {
                deadlineText += " (expirée)";
            }
        }
        Label lblDeadline = new Label(deadlineText);
        lblDeadline.getStyleClass().add("front-card-meta");

        // ── Separator ──
        Separator sep = new Separator();

        // ── Action button ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        if (opp.getStatus() == OpportunityStatus.OPEN) {
            Button btnInvest = new Button("💰 Investir");
            btnInvest.getStyleClass().add("front-btn-invest");
            btnInvest.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnInvest, Priority.ALWAYS);
            btnInvest.setOnAction(e -> navigateToOfferForm(opp));
            actions.getChildren().add(btnInvest);
        } else {
            Label lblClosed = new Label(opp.getStatus() == OpportunityStatus.FUNDED ? "✅ Objectif atteint" : "🔒 Fermée");
            lblClosed.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            actions.getChildren().add(lblClosed);
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
}
