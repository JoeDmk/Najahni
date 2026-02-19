package com.najahni.controllers;

import com.najahni.models.*;
import com.najahni.services.InvestmentOfferService;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.AnimationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur Front-Office pour gérer les Offres d'Investissement.
 * 
 * Permet de :
 * - Voir ses offres sous forme de cartes
 * - Soumettre une nouvelle offre (formulaire intégré)
 * - Filtrer par statut
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class FrontOffersController {

    // ─── Stats ───────────────────────────────────────────────
    @FXML private Label lblTotalOffers;
    @FXML private Label lblPendingOffers;
    @FXML private Label lblAcceptedOffers;
    @FXML private Label lblRejectedOffers;

    // ─── New offer form ──────────────────────────────────────
    @FXML private TitledPane newOfferPane;
    @FXML private ComboBox<InvestmentOpportunity> cboOpportunity;
    @FXML private TextField txtAmount;
    @FXML private Label lblFormMsg;

    // ─── Filter ──────────────────────────────────────────────
    @FXML private ComboBox<String> cboStatusFilter;

    // ─── Cards container ─────────────────────────────────────
    @FXML private FlowPane offersContainer;
    @FXML private VBox emptyState;

    // ─── Services ────────────────────────────────────────────
    private final InvestmentOfferService offerService;
    private final InvestmentOpportunityService opportunityService;
    private List<InvestmentOffer> allOffers;

    // Pre-selected opportunity (when navigating from "Investir" button)
    private int preselectedOpportunityId = -1;

    public FrontOffersController() {
        this.offerService = new InvestmentOfferService();
        this.opportunityService = new InvestmentOpportunityService();
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupOfferForm();
        loadData();
    }

    /**
     * Called by MainController to pre-select an opportunity in the form.
     */
    public void preselectOpportunity(int opportunityId) {
        this.preselectedOpportunityId = opportunityId;
        // If already initialized, apply the selection
        if (cboOpportunity != null && !cboOpportunity.getItems().isEmpty()) {
            selectOpportunityById(opportunityId);
            newOfferPane.setExpanded(true);
        }
    }

    // ─── SETUP ───────────────────────────────────────────────

    private void setupFilters() {
        List<String> statuses = Arrays.stream(OfferStatus.values())
            .map(OfferStatus::getDisplayName)
            .collect(Collectors.toList());
        statuses.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statuses));
        cboStatusFilter.setValue("Tous");
    }

    private void setupOfferForm() {
        // Load only OPEN opportunities
        List<InvestmentOpportunity> openOpps = opportunityService.findByStatus(OpportunityStatus.OPEN);
        cboOpportunity.setItems(FXCollections.observableArrayList(openOpps));
        cboOpportunity.setConverter(new StringConverter<>() {
            @Override
            public String toString(InvestmentOpportunity o) {
                if (o == null) return "";
                return "#" + o.getId() + " - " + o.getFormattedAmount()
                    + (o.getProjectTitle() != null ? " [" + o.getProjectTitle() + "]" : "");
            }
            @Override
            public InvestmentOpportunity fromString(String s) { return null; }
        });

        // Pre-select if navigated from opportunities page
        if (preselectedOpportunityId > 0) {
            selectOpportunityById(preselectedOpportunityId);
            newOfferPane.setExpanded(true);
        }
    }

    private void selectOpportunityById(int id) {
        for (InvestmentOpportunity o : cboOpportunity.getItems()) {
            if (o.getId() == id) {
                cboOpportunity.setValue(o);
                break;
            }
        }
    }

    // ─── DATA LOADING ────────────────────────────────────────

    private void loadData() {
        allOffers = offerService.findAll();
        updateStats();
        displayCards(allOffers);
    }

    private void updateStats() {
        long pending = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.PENDING).count();
        long accepted = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();
        long rejected = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.REJECTED).count();

        lblTotalOffers.setText(String.valueOf(allOffers.size()));
        lblPendingOffers.setText(String.valueOf(pending));
        lblAcceptedOffers.setText(String.valueOf(accepted));
        lblRejectedOffers.setText(String.valueOf(rejected));
    }

    // ─── CARD DISPLAY ────────────────────────────────────────

    private void displayCards(List<InvestmentOffer> offers) {
        offersContainer.getChildren().clear();

        if (offers.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        int delay = 0;
        for (InvestmentOffer offer : offers) {
            VBox card = createOfferCard(offer);
            offersContainer.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 250, delay);
            delay += 60;
        }
    }

    private VBox createOfferCard(InvestmentOffer offer) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(280);
        card.setMinWidth(260);
        card.setMaxWidth(300);
        card.setPadding(new Insets(20));

        // ── Status badge ──
        Label statusBadge = new Label(offer.getStatus().getDisplayName());
        statusBadge.getStyleClass().add("front-badge");
        switch (offer.getStatus()) {
            case PENDING:  statusBadge.getStyleClass().add("front-badge-orange"); break;
            case ACCEPTED: statusBadge.getStyleClass().add("front-badge-green"); break;
            case REJECTED: statusBadge.getStyleClass().add("front-badge-red"); break;
        }

        HBox topBar = new HBox(statusBadge);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // ── Amount (big) ──
        Label lblAmount = new Label(offer.getFormattedAmount());
        lblAmount.getStyleClass().add("front-card-amount");

        // ── Investor ──
        String investorText = offer.getInvestorName() != null ? offer.getInvestorName() : "Investisseur #" + offer.getInvestorId();
        Label lblInvestor = new Label("👤 " + investorText);
        lblInvestor.getStyleClass().add("front-card-project");
        lblInvestor.setWrapText(true);

        // ── Opportunity info ──
        String oppText = offer.getOpportunityDescription() != null
            ? offer.getOpportunityDescription()
            : "Opportunité #" + offer.getOpportunityId();
        if (oppText.length() > 80) oppText = oppText.substring(0, 77) + "...";
        Label lblOpp = new Label("💰 " + oppText);
        lblOpp.getStyleClass().add("front-card-desc");
        lblOpp.setWrapText(true);
        lblOpp.setMaxHeight(50);

        // ── Separator ──
        Separator sep = new Separator();

        // ── Actions ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        if (offer.getStatus() == OfferStatus.PENDING) {
            Button btnCancel = new Button("❌ Annuler");
            btnCancel.getStyleClass().add("front-btn-danger");
            btnCancel.setOnAction(e -> deleteOffer(offer));
            actions.getChildren().add(btnCancel);
        } else if (offer.getStatus() == OfferStatus.ACCEPTED) {
            Label lbl = new Label("✅ Félicitations !");
            lbl.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            actions.getChildren().add(lbl);
        } else {
            Label lbl = new Label("❌ Offre rejetée");
            lbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
            actions.getChildren().add(lbl);
        }

        card.getChildren().addAll(topBar, lblAmount, lblInvestor, lblOpp, sep, actions);
        return card;
    }

    // ─── SUBMIT NEW OFFER ────────────────────────────────────

    @FXML
    public void submitOffer() {
        lblFormMsg.setText("");

        try {
            if (cboOpportunity.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner une opportunité.");
            }

            String amountText = txtAmount.getText() != null ? txtAmount.getText().trim() : "";
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Le montant est obligatoire.");
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

            InvestmentOffer offer = new InvestmentOffer();
            offer.setProposedAmount(amount);
            offer.setStatus(OfferStatus.PENDING);
            offer.setOpportunityId(cboOpportunity.getValue().getId());
            // For front-office, we use investor_id = 1 as placeholder (no auth system yet)
            // In a real app, this would come from the logged-in user
            offer.setInvestorId(1);

            InvestmentOffer created = offerService.createOffer(offer);
            if (created != null) {
                AlertUtils.showSuccess("Offre soumise avec succès !");
                clearOfferForm();
                newOfferPane.setExpanded(false);
                loadData();
            } else {
                AlertUtils.showError("Échec", "Impossible de soumettre l'offre.");
            }

        } catch (IllegalArgumentException e) {
            lblFormMsg.setText(e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    public void clearOfferForm() {
        cboOpportunity.setValue(null);
        txtAmount.clear();
        lblFormMsg.setText("");
    }

    // ─── DELETE OFFER ────────────────────────────────────────

    private void deleteOffer(InvestmentOffer offer) {
        if (AlertUtils.showConfirmation("Annuler l'offre",
                "Voulez-vous annuler cette offre de " + offer.getFormattedAmount() + " ?")) {
            boolean deleted = offerService.deleteOffer(offer.getId());
            if (deleted) {
                AlertUtils.showSuccess("Offre annulée !");
                loadData();
            } else {
                AlertUtils.showError("Échec", "Impossible d'annuler l'offre.");
            }
        }
    }

    // ─── FILTER ACTIONS ──────────────────────────────────────

    @FXML
    public void applyFilter() {
        String statusFilter = cboStatusFilter.getValue();

        List<InvestmentOffer> filtered = allOffers.stream()
            .filter(offer -> {
                return statusFilter == null || statusFilter.equals("Tous")
                    || offer.getStatus().getDisplayName().equals(statusFilter);
            })
            .collect(Collectors.toList());

        displayCards(filtered);
    }

    @FXML
    public void clearFilter() {
        cboStatusFilter.setValue("Tous");
        displayCards(allOffers);
    }
}
