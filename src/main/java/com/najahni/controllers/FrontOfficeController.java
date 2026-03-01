package com.najahni.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.najahni.models.*;
import com.najahni.services.InvestmentOfferService;
import com.najahni.services.ProjectService;
import com.najahni.services.SessionManager;
import com.najahni.utils.AlertUtils;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la page Front Office séparée.
 * Gère la navigation entre les vues front-office (Opportunités / Offres)
 * et le retour vers le Back Office.
 * 
 * Role-aware:
 * - INVESTOR: sees Opportunités, Mes Offres, Analyse IA
 * - ENTREPRENEUR: sees Opportunités (with create form), Analyse IA, Inbox (offers on their projects)
 */
public class FrontOfficeController {

    private static final Logger LOG = Logger.getLogger(FrontOfficeController.class.getName());

    @FXML
    private StackPane foContentArea;

    @FXML
    private Label lblNavSubtitle;

    @FXML
    private Button btnFoOpportunities;

    @FXML
    private Button btnFoOffers;

    @FXML
    private Button btnFoRiskAnalysis;

    @FXML
    private Button btnFoPortfolio;

    @FXML
    private Button btnInbox;

    @FXML
    private Button btnAdvanced;

    private Button activeNavLink;

    // ─── Inbox state ─────────────────────────────────────────
    private VBox inboxPanel;
    private boolean inboxOpen = false;
    private final InvestmentOfferService offerService = new InvestmentOfferService();
    private final ProjectService projectService = new ProjectService();

    @FXML
    public void initialize() {
        // Store reference so child controllers can navigate via FrontOfficeController
        foContentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().setUserData(this);
            }
        });

        // ── Role-aware setup ──
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isEntrepreneur = currentUser != null && currentUser.getRole() == Role.ENTREPRENEUR;

        if (isEntrepreneur) {
            lblNavSubtitle.setText("Espace Entrepreneur");
            // Hide "Mes Offres" and "Portefeuille" tabs for entrepreneurs
            btnFoOffers.setVisible(false);
            btnFoOffers.setManaged(false);
            btnFoPortfolio.setVisible(false);
            btnFoPortfolio.setManaged(false);
            // Show inbox button
            btnInbox.setVisible(true);
            btnInbox.setManaged(true);
        } else {
            // Investor mode — hide inbox
            btnInbox.setVisible(false);
            btnInbox.setManaged(false);
        }

        // Show opportunities by default
        showOpportunities();

        // Attach AI chatbot widget
        AIChatWidget.attachTo(foContentArea);
    }

    // ─── ROLE CHECK ──────────────────────────────────────────

    /** Returns true if logged-in user is ENTREPRENEUR. */
    public boolean isEntrepreneur() {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && u.getRole() == Role.ENTREPRENEUR;
    }

    /**
     * Shows the front-office Opportunities view.
     */
    @FXML
    public void showOpportunities() {
        closeInboxIfOpen();
        loadView("/fxml/FrontOpportunitiesView.fxml");
        setActiveNavLink(btnFoOpportunities);
    }

    /**
     * Shows the front-office Offers view.
     */
    @FXML
    public void showOffers() {
        closeInboxIfOpen();
        loadView("/fxml/FrontOffersView.fxml");
        setActiveNavLink(btnFoOffers);
    }

    /**
     * Shows the front-office AI Risk Analysis view.
     */
    @FXML
    public void showRiskAnalysis() {
        closeInboxIfOpen();
        loadView("/fxml/FrontRiskAnalysisView.fxml");
        setActiveNavLink(btnFoRiskAnalysis);
    }

    /**
     * Shows the front-office Portfolio view (paid investments).
     */
    @FXML
    public void showPortfolio() {
        closeInboxIfOpen();
        loadView("/fxml/FrontPortfolioView.fxml");
        setActiveNavLink(btnFoPortfolio);
    }

    /**
     * Shows the Advanced Investment Dashboard (contracts, matching, charts, comparator, ratings).
     */
    @FXML
    public void showAdvanced() {
        closeInboxIfOpen();
        loadView("/fxml/AdvancedInvestmentView.fxml");
        setActiveNavLink(btnAdvanced);
    }

    /**
     * Shows the front-office Offers view with a pre-selected opportunity.
     * Called when user clicks "Investir" on an opportunity card.
     * @param opportunityId The ID of the opportunity to pre-select
     */
    public void showOffers(int opportunityId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FrontOffersView.fxml"));
            Parent view = loader.load();

            // Pre-select the opportunity in the form
            FrontOffersController controller = loader.getController();
            controller.preselectOpportunity(opportunityId);

            animateIn(view);
            setActiveNavLink(btnFoOffers);
        } catch (IOException e) {
            LOG.warning("Erreur chargement FrontOffersView: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INBOX — sliding panel for entrepreneur to review offers
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void toggleInbox() {
        if (inboxOpen) {
            closeInbox();
        } else {
            openInbox();
        }
    }

    private void openInbox() {
        // Build/refresh the inbox panel
        inboxPanel = buildInboxPanel();
        inboxPanel.setTranslateX(380); // start off-screen to the right
        inboxPanel.setOpacity(0);

        // Place it as overlay on top of content area
        if (!foContentArea.getChildren().contains(inboxPanel)) {
            foContentArea.getChildren().add(inboxPanel);
        }
        StackPane.setAlignment(inboxPanel, Pos.CENTER_RIGHT);

        // Slide in from right
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), inboxPanel);
        slide.setToX(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), inboxPanel);
        fade.setToValue(1);
        new ParallelTransition(slide, fade).play();

        inboxOpen = true;
        // Highlight inbox button
        if (!btnInbox.getStyleClass().contains("fo-nav-link-active")) {
            btnInbox.getStyleClass().add("fo-nav-link-active");
        }
    }

    private void closeInbox() {
        if (inboxPanel == null || !inboxOpen) return;

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), inboxPanel);
        slide.setToX(380);
        FadeTransition fade = new FadeTransition(Duration.millis(250), inboxPanel);
        fade.setToValue(0);
        ParallelTransition anim = new ParallelTransition(slide, fade);
        anim.setOnFinished(e -> foContentArea.getChildren().remove(inboxPanel));
        anim.play();

        inboxOpen = false;
        btnInbox.getStyleClass().remove("fo-nav-link-active");
    }

    private void closeInboxIfOpen() {
        if (inboxOpen) closeInbox();
    }

    /**
     * Builds the entire inbox panel with offer cards.
     */
    private VBox buildInboxPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("inbox-panel");
        panel.setPrefWidth(370);
        panel.setMaxWidth(370);
        panel.setMinWidth(370);

        // ── Header ──
        HBox header = new HBox(10);
        header.getStyleClass().add("inbox-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 16, 18));

        Label title = new Label("📬 Boîte de réception");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("✕");
        btnClose.getStyleClass().add("inbox-close-btn");
        btnClose.setOnAction(e -> closeInbox());
        header.getChildren().addAll(title, spacer, btnClose);

        // ── Offer cards ──
        VBox cardList = new VBox(10);
        cardList.setPadding(new Insets(10, 12, 15, 12));

        List<InvestmentOffer> offers = loadEntrepreneurOffers();

        if (offers.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40, 20, 40, 20));
            Label emptyIcon = new Label("📭");
            emptyIcon.setStyle("-fx-font-size: 40px;");
            Label emptyText = new Label("Aucune offre reçue");
            emptyText.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            Label emptyHint = new Label("Les investisseurs verront vos opportunités\net pourront y soumettre des offres.");
            emptyHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
            emptyHint.setWrapText(true);
            empty.getChildren().addAll(emptyIcon, emptyText, emptyHint);
            cardList.getChildren().add(empty);
        } else {
            // Pending count badge
            long pendingCount = offers.stream().filter(o -> o.getStatus() == OfferStatus.PENDING).count();
            if (pendingCount > 0) {
                Label badge = new Label("🔔 " + pendingCount + " offre(s) en attente");
                badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 8 14; "
                    + "-fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: bold;");
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setAlignment(Pos.CENTER);
                cardList.getChildren().add(badge);
            }

            for (InvestmentOffer offer : offers) {
                VBox card = buildInboxOfferCard(offer, cardList);
                cardList.getChildren().add(card);
            }
        }

        ScrollPane scroll = new ScrollPane(cardList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, new Separator(), scroll);
        return panel;
    }

    /**
     * Builds a single offer card for the inbox panel.
     */
    private VBox buildInboxOfferCard(InvestmentOffer offer, VBox cardList) {
        VBox card = new VBox(6);
        card.getStyleClass().add("inbox-card");
        card.setPadding(new Insets(14, 14, 14, 14));

        // Status badge
        Label statusBadge = new Label(offer.getStatus().getDisplayName());
        statusBadge.getStyleClass().add("front-badge");
        switch (offer.getStatus()) {
            case PENDING:  statusBadge.getStyleClass().add("front-badge-orange"); break;
            case ACCEPTED: statusBadge.getStyleClass().add("front-badge-green"); break;
            case REJECTED: statusBadge.getStyleClass().add("front-badge-red"); break;
        }
        HBox topBar = new HBox(statusBadge);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // Amount
        Label lblAmount = new Label(offer.getFormattedAmount());
        lblAmount.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        // Investor
        String investorText = offer.getInvestorName() != null ? offer.getInvestorName() : "Investisseur #" + offer.getInvestorId();
        Label lblInvestor = new Label("👤 " + investorText);
        lblInvestor.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Opportunity
        String oppText = offer.getOpportunityDescription() != null
            ? (offer.getOpportunityDescription().length() > 60
                ? offer.getOpportunityDescription().substring(0, 57) + "..."
                : offer.getOpportunityDescription())
            : "Opportunité #" + offer.getOpportunityId();
        Label lblOpp = new Label("💰 " + oppText);
        lblOpp.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        lblOpp.setWrapText(true);

        card.getChildren().addAll(topBar, lblAmount, lblInvestor, lblOpp);

        // Action buttons — only for PENDING
        if (offer.getStatus() == OfferStatus.PENDING) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER);
            actions.setPadding(new Insets(6, 0, 0, 0));

            Button btnAccept = new Button("✅ Accepter");
            btnAccept.getStyleClass().add("inbox-btn-accept");
            btnAccept.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnAccept, Priority.ALWAYS);
            btnAccept.setOnAction(e -> handleAcceptOffer(offer, card, cardList));

            Button btnDecline = new Button("❌ Refuser");
            btnDecline.getStyleClass().add("inbox-btn-decline");
            btnDecline.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnDecline, Priority.ALWAYS);
            btnDecline.setOnAction(e -> handleDeclineOffer(offer, card, cardList));

            actions.getChildren().addAll(btnAccept, btnDecline);
            card.getChildren().add(actions);
        } else if (offer.getStatus() == OfferStatus.ACCEPTED) {
            Label lbl = new Label("✅ Offre acceptée");
            lbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");
            card.getChildren().add(lbl);
        } else {
            Label lbl = new Label("❌ Offre refusée");
            lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-style: italic; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");
            card.getChildren().add(lbl);
        }

        return card;
    }

    private void handleAcceptOffer(InvestmentOffer offer, VBox card, VBox cardList) {
        if (AlertUtils.showConfirmation("Accepter l'offre",
                "Accepter l'offre de " + offer.getFormattedAmount() + " ?")) {
            boolean ok = offerService.acceptOffer(offer.getId());
            if (ok) {
                AlertUtils.showSuccess("Offre acceptée !");
                refreshInbox();
            } else {
                AlertUtils.showError("Erreur", "Impossible d'accepter l'offre.");
            }
        }
    }

    private void handleDeclineOffer(InvestmentOffer offer, VBox card, VBox cardList) {
        if (AlertUtils.showConfirmation("Refuser l'offre",
                "Refuser l'offre de " + offer.getFormattedAmount() + " ?")) {
            boolean ok = offerService.rejectOffer(offer.getId());
            if (ok) {
                AlertUtils.showSuccess("Offre refusée.");
                refreshInbox();
            } else {
                AlertUtils.showError("Erreur", "Impossible de refuser l'offre.");
            }
        }
    }

    /**
     * Refreshes the inbox panel in-place.
     */
    private void refreshInbox() {
        if (inboxOpen) {
            foContentArea.getChildren().remove(inboxPanel);
            inboxPanel = buildInboxPanel();
            foContentArea.getChildren().add(inboxPanel);
            StackPane.setAlignment(inboxPanel, Pos.CENTER_RIGHT);
        }
    }

    /**
     * Loads offers for the entrepreneur's projects' opportunities.
     */
    private List<InvestmentOffer> loadEntrepreneurOffers() {
        try {
            int userId = SessionManager.getInstance().getCurrentUserId();
            List<Project> projects = projectService.findByEntrepreneur(userId);
            List<Integer> projectIds = projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
            return offerService.findByProjectIds(projectIds);
        } catch (Exception e) {
            LOG.warning("Error loading entrepreneur offers: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Navigates back to the Back Office (MainView).
     */
    @FXML
    public void goBackOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) foContentArea.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("NAJAHNI — Back-Office");
        } catch (IOException e) {
            LOG.warning("Erreur retour Back Office: " + e.getMessage());
        }
    }

    /**
     * Déconnexion — retour à LoginView.
     */
    @FXML
    public void handleLogout() {
        if (AlertUtils.showConfirmation("Déconnexion", "Êtes-vous sûr de vouloir vous déconnecter ?")) {
            SessionManager.getInstance().logout();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) foContentArea.getScene().getWindow();
                Scene scene = new Scene(root, 900, 600);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                stage.setTitle("NAJAHNI — Connexion");
                stage.setScene(scene);
            } catch (IOException e) {
                LOG.warning("Erreur déconnexion: " + e.getMessage());
            }
        }
    }

    /**
     * Loads a view into the front-office content area with smooth animation.
     * @param fxmlPath Path to the FXML file
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            animateIn(view);
        } catch (IOException e) {
            LOG.warning("Erreur chargement vue: " + fxmlPath + " - " + e.getMessage());
        }
    }

    /**
     * Animates a view into the content area with fade + slide.
     */
    private void animateIn(Parent view) {
        view.setOpacity(0);
        view.setTranslateY(15);

        // Remove all children except the inbox panel (if open)
        foContentArea.getChildren().removeIf(n -> n != inboxPanel);
        foContentArea.getChildren().add(0, view);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(250), view);
        slideUp.setFromY(15);
        slideUp.setToY(0);

        new ParallelTransition(fadeIn, slideUp).play();
    }

    /**
     * Sets the active navigation link style.
     */
    private void setActiveNavLink(Button button) {
        if (activeNavLink != null) {
            activeNavLink.getStyleClass().remove("fo-nav-link-active");
        }
        if (!button.getStyleClass().contains("fo-nav-link-active")) {
            button.getStyleClass().add("fo-nav-link-active");
        }
        activeNavLink = button;
    }
}
