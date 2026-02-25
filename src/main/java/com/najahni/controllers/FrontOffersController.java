package com.najahni.controllers;

import com.najahni.models.*;
import com.najahni.services.*;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.AnimationUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Contrôleur Front-Office — Offres d'Investissement (version enrichie).
 *
 * Fonctionnalités :
 * - Cartes d'offres avec popup détaillé pour TOUT statut
 * - ACCEPTED → popup avec paiement Stripe
 * - PENDING  → popup détaillé + statut "En attente"
 * - REJECTED → popup détaillé + statut "Rejetée"
 * - Statistiques avancées avec indicateurs visuels
 * - Animations fluides et effets de survol
 */
public class FrontOffersController {

    private static final Logger LOG = Logger.getLogger(FrontOffersController.class.getName());

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
    private final ProjectService projectService;
    private final PaymentService paymentService;
    private List<InvestmentOffer> allOffers;

    private int preselectedOpportunityId = -1;

    public FrontOffersController() {
        this.offerService = new InvestmentOfferService();
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
        this.paymentService = new PaymentService();
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupOfferForm();
        loadData();
    }

    public void preselectOpportunity(int opportunityId) {
        this.preselectedOpportunityId = opportunityId;
        if (cboOpportunity != null && !cboOpportunity.getItems().isEmpty()) {
            selectOpportunityById(opportunityId);
            newOfferPane.setExpanded(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════

    private void setupFilters() {
        List<String> statuses = Arrays.stream(OfferStatus.values())
            .map(OfferStatus::getDisplayName)
            .collect(Collectors.toList());
        statuses.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statuses));
        cboStatusFilter.setValue("Tous");
    }

    private void setupOfferForm() {
        List<InvestmentOpportunity> openOpps = opportunityService.findByStatus(OpportunityStatus.OPEN);
        cboOpportunity.setItems(FXCollections.observableArrayList(openOpps));
        cboOpportunity.setConverter(new StringConverter<>() {
            @Override
            public String toString(InvestmentOpportunity o) {
                if (o == null) return "";
                return "#" + o.getId() + " — " + o.getFormattedAmount()
                    + (o.getProjectTitle() != null ? " [" + o.getProjectTitle() + "]" : "");
            }
            @Override
            public InvestmentOpportunity fromString(String s) { return null; }
        });

        if (preselectedOpportunityId > 0) {
            selectOpportunityById(preselectedOpportunityId);
            newOfferPane.setExpanded(true);
        }
    }

    private void selectOpportunityById(int id) {
        for (InvestmentOpportunity o : cboOpportunity.getItems()) {
            if (o.getId() == id) { cboOpportunity.setValue(o); break; }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DATA LOADING & STATS
    // ═══════════════════════════════════════════════════════════

    private void loadData() {
        allOffers = offerService.findAll();
        updateStats();
        displayCards(allOffers);
    }

    private void updateStats() {
        long total   = allOffers.size();
        long pending  = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.PENDING).count();
        long accepted = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();
        long rejected = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.REJECTED).count();

        // Animated counter values
        animateStatLabel(lblTotalOffers, (int) total);
        animateStatLabel(lblPendingOffers, (int) pending);
        animateStatLabel(lblAcceptedOffers, (int) accepted);
        animateStatLabel(lblRejectedOffers, (int) rejected);
    }

    /** Animate a stat label counting up from 0 to target */
    private void animateStatLabel(Label label, int target) {
        if (target == 0) { label.setText("0"); return; }
        Timeline timeline = new Timeline();
        int frames = Math.min(target, 20);
        for (int i = 0; i <= frames; i++) {
            int value = (int) Math.round((double) target * i / frames);
            KeyFrame kf = new KeyFrame(Duration.millis(40.0 * i),
                e -> label.setText(String.valueOf(value)));
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    // ═══════════════════════════════════════════════════════════
    //  CARD DISPLAY — Enhanced with hover effects & click-to-detail
    // ═══════════════════════════════════════════════════════════

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
            AnimationUtils.playFadeScaleIn(card, 300, delay);
            delay += 70;
        }
    }

    private VBox createOfferCard(InvestmentOffer offer) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(290);
        card.setMinWidth(260);
        card.setMaxWidth(310);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-cursor: hand;");

        // ── Colored top stripe based on status ──
        String stripeColor = switch (offer.getStatus()) {
            case PENDING  -> "#f39c12";
            case ACCEPTED -> "#27ae60";
            case REJECTED -> "#e74c3c";
        };
        Rectangle stripe = new Rectangle();
        stripe.setHeight(4);
        stripe.setArcWidth(4);
        stripe.setArcHeight(4);
        stripe.setFill(Color.web(stripeColor));
        stripe.widthProperty().bind(card.widthProperty().subtract(44));

        // ── Status badge ──
        Label statusBadge = new Label(getStatusIcon(offer.getStatus()) + " " + offer.getStatus().getDisplayName());
        statusBadge.getStyleClass().add("front-badge");
        switch (offer.getStatus()) {
            case PENDING:  statusBadge.getStyleClass().add("front-badge-orange"); break;
            case ACCEPTED: statusBadge.getStyleClass().add("front-badge-green");  break;
            case REJECTED: statusBadge.getStyleClass().add("front-badge-red");    break;
        }

        HBox topBar = new HBox(statusBadge);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        // ── Amount (big, with currency icon) ──
        Label lblAmount = new Label(offer.getFormattedAmount());
        lblAmount.getStyleClass().add("front-card-amount");

        // ── Investor ──
        String investorText = offer.getInvestorName() != null
            ? offer.getInvestorName()
            : "Investisseur #" + offer.getInvestorId();
        Label lblInvestor = new Label("👤 " + investorText);
        lblInvestor.getStyleClass().add("front-card-project");
        lblInvestor.setWrapText(true);

        // ── Opportunity snippet ──
        String oppText = offer.getOpportunityDescription() != null
            ? offer.getOpportunityDescription()
            : "Opportunité #" + offer.getOpportunityId();
        if (oppText.length() > 80) oppText = oppText.substring(0, 77) + "…";
        Label lblOpp = new Label("📋 " + oppText);
        lblOpp.getStyleClass().add("front-card-desc");
        lblOpp.setWrapText(true);
        lblOpp.setMaxHeight(50);

        Separator sep = new Separator();

        // ── Actions row ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        switch (offer.getStatus()) {
            case PENDING -> {
                Button btnDetails = new Button("📄 Détails");
                btnDetails.getStyleClass().add("front-btn-details");
                btnDetails.setOnAction(e -> { e.consume(); showDetailPopup(offer); });
                Button btnCancel = new Button("❌ Annuler");
                btnCancel.getStyleClass().add("front-btn-danger");
                btnCancel.setOnAction(e -> { e.consume(); deleteOffer(offer); });
                actions.getChildren().addAll(btnDetails, btnCancel);
            }
            case ACCEPTED -> {
                if (offer.isPaid()) {
                    Label paidBadge = new Label("✅ Payé");
                    paidBadge.setStyle("-fx-background-color: #eafaf1; -fx-text-fill: #27ae60; "
                        + "-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 6 16; "
                        + "-fx-background-radius: 8;");
                    Button btnDetails = new Button("📋 Détails");
                    btnDetails.getStyleClass().add("front-btn-details");
                    btnDetails.setOnAction(e -> { e.consume(); showDetailPopup(offer); });
                    actions.getChildren().addAll(paidBadge, btnDetails);
                } else {
                    Button btnPay = new Button("💳 Payer & Détails");
                    btnPay.getStyleClass().add("front-btn-pay");
                    btnPay.setOnAction(e -> { e.consume(); showDetailPopup(offer); });
                    actions.getChildren().add(btnPay);
                }
            }
            case REJECTED -> {
                Button btnDetails = new Button("📄 Voir Détails");
                btnDetails.getStyleClass().add("front-btn-details");
                btnDetails.setOnAction(e -> { e.consume(); showDetailPopup(offer); });
                actions.getChildren().add(btnDetails);
            }
        }

        card.getChildren().addAll(stripe, topBar, lblAmount, lblInvestor, lblOpp, sep, actions);

        // ── Card click → popup ──
        card.setOnMouseClicked(e -> showDetailPopup(offer));

        // ── Hover glow effect ──
        DropShadow normalShadow = new DropShadow(12, Color.rgb(0, 0, 0, 0.08));
        DropShadow hoverShadow = new DropShadow(20, Color.web(stripeColor + "55"));
        card.setEffect(normalShadow);
        card.setOnMouseEntered(e -> {
            card.setEffect(hoverShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(normalShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private String getStatusIcon(OfferStatus status) {
        return switch (status) {
            case PENDING  -> "⏳";
            case ACCEPTED -> "✅";
            case REJECTED -> "❌";
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL POPUP — Centered overlay, all statuses
    // ═══════════════════════════════════════════════════════════

    private void showDetailPopup(InvestmentOffer offer) {
        Optional<InvestmentOpportunity> optOpp = opportunityService.findById(offer.getOpportunityId());
        if (optOpp.isEmpty()) {
            AlertUtils.showError("Erreur", "Opportunité introuvable.");
            return;
        }
        InvestmentOpportunity opportunity = optOpp.get();
        Optional<Project> optProject = projectService.findById(opportunity.getProjectId());
        Project project = optProject.orElse(null);

        // ── Use Scene root for guaranteed full-screen overlay ──
        StackPane root = getSceneRootStack();
        if (root == null) return;

        // ── Blurred backdrop ──
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("payment-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);

        // ── Popup container — force centered ──
        VBox popup = new VBox(0);
        popup.getStyleClass().add("payment-popup");
        popup.setMaxWidth(540);
        popup.setMinWidth(460);
        popup.setMaxHeight(720);
        popup.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(popup, Pos.CENTER);

        popup.setScaleX(0.85);
        popup.setScaleY(0.85);
        popup.setOpacity(0);

        // Clip rounded corners
        Rectangle clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(popup.widthProperty());
        clip.heightProperty().bind(popup.heightProperty());
        popup.setClip(clip);

        // ── Header ──
        VBox header = buildPopupHeader(offer);

        // ── Body ──
        VBox body = new VBox(18);
        body.setPadding(new Insets(24, 28, 10, 28));
        body.getStyleClass().add("payment-popup-body");

        // Offer section
        body.getChildren().add(buildSection(
            "💼", "DÉTAILS DE L'OFFRE",
            buildDetailRow("💰 Montant proposé", offer.getFormattedAmount()),
            buildDetailRow(getStatusIcon(offer.getStatus()) + " Statut", offer.getStatus().getDisplayName()),
            buildDetailRow("👤 Investisseur",
                offer.getInvestorName() != null ? offer.getInvestorName() : "Investisseur #" + offer.getInvestorId())
        ));

        // Opportunity section
        VBox oppSection = buildSection(
            "🎯", "OPPORTUNITÉ D'INVESTISSEMENT",
            buildDetailRow("📋 Description",
                opportunity.getDescription() != null ? opportunity.getDescription() : "N/A"),
            buildDetailRow("🎯 Montant cible", opportunity.getFormattedAmount()),
            buildDetailRow("📅 Deadline", opportunity.getDeadline() != null
                ? opportunity.getDeadline().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "Non défini")
        );

        VBox oppContent = (VBox) oppSection.getChildren().get(1);

        // Risk indicator with colored bar
        if (opportunity.getRiskScore() != null) {
            String riskLabel = opportunity.getFormattedRiskLabel() != null ? opportunity.getFormattedRiskLabel() : "";
            String riskLevel = opportunity.getRiskLevel();
            String riskColor = switch (riskLevel) {
                case "LOW"    -> "#27ae60";
                case "MEDIUM" -> "#f39c12";
                case "HIGH"   -> "#e74c3c";
                default       -> "#95a5a6";
            };

            HBox riskRow = new HBox(10);
            riskRow.setAlignment(Pos.CENTER_LEFT);
            Label riskKey = new Label("📊 Score de risque");
            riskKey.getStyleClass().add("payment-detail-key");
            riskKey.setMinWidth(140);

            VBox riskValue = new VBox(4);
            Label riskText = new Label(opportunity.getFormattedRiskScore() + " " + riskLabel);
            riskText.setStyle("-fx-text-fill: " + riskColor + "; -fx-font-weight: bold; -fx-font-size: 13;");

            ProgressBar riskBar = new ProgressBar(opportunity.getRiskScore() / 100.0);
            riskBar.setPrefWidth(180);
            riskBar.setPrefHeight(8);
            riskBar.setStyle("-fx-accent: " + riskColor + ";");

            riskValue.getChildren().addAll(riskText, riskBar);
            riskRow.getChildren().addAll(riskKey, riskValue);
            oppContent.getChildren().add(riskRow);
        }

        // Deadline badge
        if (opportunity.getDeadline() != null) {
            String badge = DeadlineService.getDeadlineBadge(opportunity.getDeadline());
            String badgeStyle = DeadlineService.getDeadlineStyle(opportunity.getDeadline());
            Label deadlineBadge = new Label(badge);
            deadlineBadge.setStyle(badgeStyle
                + "-fx-padding: 5 14; -fx-background-radius: 14; -fx-font-size: 11; -fx-font-weight: bold;");
            oppContent.getChildren().add(deadlineBadge);
        }

        body.getChildren().add(oppSection);

        // Project section
        if (project != null) {
            body.getChildren().add(buildSection(
                "🏢", "PROJET",
                buildDetailRow("📌 Titre", project.getTitle() != null ? project.getTitle() : "N/A"),
                buildDetailRow("🏷 Secteur", project.getSector() != null ? project.getSector() : "N/A"),
                buildDetailRow("📝 Description", project.getDescription() != null
                    ? (project.getDescription().length() > 150
                        ? project.getDescription().substring(0, 147) + "…"
                        : project.getDescription())
                    : "N/A")
            ));
        }

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollBody.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollBody.getStyleClass().add("payment-scroll");
        VBox.setVgrow(scrollBody, Priority.ALWAYS);

        // ── Footer (status-dependent) ──
        VBox footer = buildPopupFooter(offer, opportunity, project, overlay, popup, root);

        popup.getChildren().addAll(header, scrollBody, footer);
        overlay.getChildren().add(popup);

        // Close on backdrop click
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) closePopup(overlay, popup, root);
        });

        // ESC key to close
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closePopup(overlay, popup, root);
        });

        root.getChildren().add(overlay);
        overlay.requestFocus();

        // Blur background content
        if (root.getChildren().size() > 1) {
            root.getChildren().get(0).setEffect(new GaussianBlur(6));
        }

        animatePopupIn(overlay, popup);
    }

    // ═══════════════════════════════════════════════════════════
    //  POPUP BUILDERS
    // ═══════════════════════════════════════════════════════════

    private VBox buildPopupHeader(InvestmentOffer offer) {
        String headerGradient = switch (offer.getStatus()) {
            case PENDING  -> "linear-gradient(to right, #e67e22, #f39c12, #f1c40f)";
            case ACCEPTED -> "linear-gradient(to right, #0f3460, #16213e, #1a1a2e)";
            case REJECTED -> "linear-gradient(to right, #c0392b, #e74c3c, #c0392b)";
        };

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(28, 28, 22, 28));
        header.setStyle("-fx-background-color: " + headerGradient + ";");

        // Status icon — large
        String bigIcon = switch (offer.getStatus()) {
            case PENDING  -> "⏳";
            case ACCEPTED -> "💳";
            case REJECTED -> "📋";
        };
        Label icon = new Label(bigIcon);
        icon.setStyle("-fx-font-size: 42;");

        // Pulse animation on icon
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), icon);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.15); pulse.setToY(1.15);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        String titleText = switch (offer.getStatus()) {
            case PENDING  -> "Offre en attente de validation";
            case ACCEPTED -> "Paiement de l'investissement";
            case REJECTED -> "Détails de l'offre rejetée";
        };
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label(offer.getFormattedAmount());
        subtitle.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

        // Shimmer animation on amount
        FadeTransition shimmer = new FadeTransition(Duration.millis(2000), subtitle);
        shimmer.setFromValue(0.7); shimmer.setToValue(1.0);
        shimmer.setCycleCount(Animation.INDEFINITE);
        shimmer.setAutoReverse(true);
        shimmer.play();

        header.getChildren().addAll(icon, title, subtitle);

        // Close button overlaid top-right
        Button btnClose = new Button("✕");
        btnClose.getStyleClass().add("payment-close-btn");
        // Will be wired by caller via lookup

        return header;
    }

    private VBox buildSection(String emoji, String sectionTitle, HBox... rows) {
        VBox section = new VBox(4);
        section.getStyleClass().add("payment-section");

        HBox titleBar = new HBox(8);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 16;");
        Label lblTitle = new Label(sectionTitle);
        lblTitle.getStyleClass().add("payment-section-title");
        titleBar.getChildren().addAll(emojiLabel, lblTitle);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 0, 0, 0));
        content.getChildren().addAll(rows);

        section.getChildren().addAll(titleBar, content);
        return section;
    }

    private HBox buildDetailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblKey = new Label(label);
        lblKey.getStyleClass().add("payment-detail-key");
        lblKey.setMinWidth(140);

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("payment-detail-value");
        lblValue.setWrapText(true);
        HBox.setHgrow(lblValue, Priority.ALWAYS);

        row.getChildren().addAll(lblKey, lblValue);
        return row;
    }

    private VBox buildPopupFooter(InvestmentOffer offer, InvestmentOpportunity opportunity,
                                   Project project, StackPane overlay, VBox popup, StackPane root) {
        VBox footer = new VBox(12);
        footer.setPadding(new Insets(20, 28, 24, 28));
        footer.setAlignment(Pos.CENTER);
        footer.getStyleClass().add("payment-popup-footer");

        switch (offer.getStatus()) {
            case ACCEPTED -> buildStripePaymentFooter(footer, offer, opportunity, project, overlay, popup, root);

            case PENDING -> {
                // Waiting badge — animated pulse
                HBox waitingBox = new HBox(10);
                waitingBox.setAlignment(Pos.CENTER);
                waitingBox.setStyle("-fx-background-color: #fef9e7; -fx-background-radius: 12; -fx-padding: 16;");

                ProgressIndicator waitSpinner = new ProgressIndicator();
                waitSpinner.setPrefSize(24, 24);
                waitSpinner.setStyle("-fx-progress-color: #f39c12;");

                Label waitLabel = new Label("⏳ En attente de validation par l'entrepreneur…");
                waitLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 14;");
                waitLabel.setWrapText(true);

                // Pulse animation
                FadeTransition waitPulse = new FadeTransition(Duration.millis(1500), waitLabel);
                waitPulse.setFromValue(0.6); waitPulse.setToValue(1.0);
                waitPulse.setCycleCount(Animation.INDEFINITE);
                waitPulse.setAutoReverse(true);
                waitPulse.play();

                waitingBox.getChildren().addAll(waitSpinner, waitLabel);
                footer.getChildren().add(waitingBox);
            }

            case REJECTED -> {
                HBox rejectedBox = new HBox(10);
                rejectedBox.setAlignment(Pos.CENTER);
                rejectedBox.setStyle("-fx-background-color: #fdedec; -fx-background-radius: 12; -fx-padding: 16;");

                Label rejLabel = new Label("❌ Cette offre a été déclinée par l'entrepreneur.");
                rejLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-font-size: 14;");
                rejLabel.setWrapText(true);

                rejectedBox.getChildren().add(rejLabel);
                footer.getChildren().add(rejectedBox);
            }
        }

        // Close button — always present
        Button btnClose = new Button("✕  Fermer");
        btnClose.getStyleClass().add("payment-btn-cancel");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setOnAction(e -> closePopup(overlay, popup, root));
        footer.getChildren().add(btnClose);

        return footer;
    }

    private void buildStripePaymentFooter(VBox footer, InvestmentOffer offer,
                                           InvestmentOpportunity opportunity, Project project,
                                           StackPane overlay, VBox popup, StackPane root) {
        // Check if already paid
        if (offer.isPaid() || offerService.isOfferPaid(offer.getId())) {
            HBox paidBox = new HBox(10);
            paidBox.setAlignment(Pos.CENTER);
            paidBox.setStyle("-fx-background-color: #eafaf1; -fx-background-radius: 12; -fx-padding: 16;");
            Label paidLabel = new Label("✅ Paiement déjà effectué");
            paidLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 15;");
            paidBox.getChildren().add(paidLabel);
            if (offer.getPaymentIntentId() != null) {
                Label txId = new Label("🧾 " + offer.getPaymentIntentId());
                txId.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                VBox paidInfo = new VBox(6, paidBox, txId);
                paidInfo.setAlignment(Pos.CENTER);
                footer.getChildren().add(paidInfo);
            } else {
                footer.getChildren().add(paidBox);
            }
            return;
        }

        // "Payer" button opens the card form
        Button btnOpenCardForm = new Button("💳  Payer via Stripe");
        btnOpenCardForm.getStyleClass().add("payment-btn-stripe");
        btnOpenCardForm.setMaxWidth(Double.MAX_VALUE);

        DropShadow btnGlow = new DropShadow(15, Color.web("#6772e5", 0.5));
        btnOpenCardForm.setEffect(btnGlow);
        Timeline glowAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(btnGlow.radiusProperty(), 10)),
            new KeyFrame(Duration.millis(1500), new KeyValue(btnGlow.radiusProperty(), 22))
        );
        glowAnim.setCycleCount(Animation.INDEFINITE);
        glowAnim.setAutoReverse(true);
        glowAnim.play();

        Label lblSummary = new Label("Montant : " + offer.getFormattedAmount() + " EUR");
        lblSummary.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12; -fx-alignment: center;");
        lblSummary.setMaxWidth(Double.MAX_VALUE);

        btnOpenCardForm.setOnAction(e -> {
            glowAnim.stop();
            showCardEntryForm(offer, opportunity, project, overlay, popup, root, footer);
        });

        footer.getChildren().addAll(lblSummary, btnOpenCardForm);
    }

    // ═══════════════════════════════════════════════════════════
    //  CARD ENTRY FORM with validation
    // ═══════════════════════════════════════════════════════════

    private void showCardEntryForm(InvestmentOffer offer, InvestmentOpportunity opportunity,
                                    Project project, StackPane overlay, VBox popup,
                                    StackPane root, VBox footer) {
        footer.getChildren().clear();
        footer.setSpacing(14);

        // ── Title ──
        Label formTitle = new Label("💳 Détails de la carte bancaire");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label secureLabel = new Label("🔒 Paiement sécurisé via Stripe");
        secureLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6;");

        // ── Card Number ──
        Label lblCardNum = new Label("Numéro de carte");
        lblCardNum.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555;");
        TextField txtCardNum = new TextField();
        txtCardNum.setPromptText("1234 5678 9012 3456");
        txtCardNum.setStyle("-fx-font-size: 15; -fx-background-radius: 10; -fx-border-radius: 10; "
            + "-fx-border-color: #dcdde1; -fx-border-width: 1; -fx-padding: 10 14;");
        txtCardNum.setMaxWidth(Double.MAX_VALUE);
        Label errCardNum = new Label();
        errCardNum.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        errCardNum.setVisible(false);

        // Auto-format card number with spaces every 4 digits
        txtCardNum.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(' ');
                formatted.append(digits.charAt(i));
            }
            String result = formatted.toString();
            if (!result.equals(newVal)) {
                txtCardNum.setText(result);
                txtCardNum.positionCaret(result.length());
            }
        });

        VBox cardNumBox = new VBox(4, lblCardNum, txtCardNum, errCardNum);

        // ── Expiry + CVV row ──
        Label lblExpiry = new Label("Date d'expiration");
        lblExpiry.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555;");
        TextField txtExpiry = new TextField();
        txtExpiry.setPromptText("MM/AA");
        txtExpiry.setStyle("-fx-font-size: 15; -fx-background-radius: 10; -fx-border-radius: 10; "
            + "-fx-border-color: #dcdde1; -fx-border-width: 1; -fx-padding: 10 14;");
        txtExpiry.setPrefWidth(140);
        Label errExpiry = new Label();
        errExpiry.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        errExpiry.setVisible(false);

        // Auto-format MM/YY
        txtExpiry.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2) formatted.append('/');
                formatted.append(digits.charAt(i));
            }
            String result = formatted.toString();
            if (!result.equals(newVal)) {
                txtExpiry.setText(result);
                txtExpiry.positionCaret(result.length());
            }
        });

        VBox expiryBox = new VBox(4, lblExpiry, txtExpiry, errExpiry);
        HBox.setHgrow(expiryBox, Priority.ALWAYS);

        Label lblCvv = new Label("CVV");
        lblCvv.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555;");
        TextField txtCvv = new TextField();
        txtCvv.setPromptText("123");
        txtCvv.setStyle("-fx-font-size: 15; -fx-background-radius: 10; -fx-border-radius: 10; "
            + "-fx-border-color: #dcdde1; -fx-border-width: 1; -fx-padding: 10 14;");
        txtCvv.setPrefWidth(100);
        Label errCvv = new Label();
        errCvv.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        errCvv.setVisible(false);

        // CVV: max 4 digits
        txtCvv.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            if (!digits.equals(newVal)) {
                txtCvv.setText(digits);
                txtCvv.positionCaret(digits.length());
            }
        });

        VBox cvvBox = new VBox(4, lblCvv, txtCvv, errCvv);

        HBox expiryRow = new HBox(15, expiryBox, cvvBox);
        expiryRow.setAlignment(Pos.CENTER_LEFT);

        // ── Cardholder Name ──
        Label lblName = new Label("Nom du titulaire");
        lblName.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #555;");
        TextField txtName = new TextField();
        txtName.setPromptText("Jean Dupont");
        txtName.setStyle("-fx-font-size: 15; -fx-background-radius: 10; -fx-border-radius: 10; "
            + "-fx-border-color: #dcdde1; -fx-border-width: 1; -fx-padding: 10 14;");
        txtName.setMaxWidth(Double.MAX_VALUE);
        Label errName = new Label();
        errName.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        errName.setVisible(false);

        VBox nameBox = new VBox(4, lblName, txtName, errName);

        // ── Amount summary ──
        Label amountLabel = new Label("💰 Montant à payer : " + offer.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50; "
            + "-fx-background-color: #f0f4ff; -fx-background-radius: 10; -fx-padding: 12 18;");
        amountLabel.setMaxWidth(Double.MAX_VALUE);
        amountLabel.setAlignment(Pos.CENTER);

        // ── Error & status labels ──
        Label lblPayStatus = new Label();
        lblPayStatus.setWrapText(true);
        lblPayStatus.setMaxWidth(Double.MAX_VALUE);
        lblPayStatus.setStyle("-fx-alignment: center; -fx-text-alignment: center;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(28, 28);
        spinner.setVisible(false);

        HBox spinnerBox = new HBox(10, spinner, lblPayStatus);
        spinnerBox.setAlignment(Pos.CENTER);

        // ── Pay button ──
        Button btnPay = new Button("🔐  Confirmer le paiement");
        btnPay.getStyleClass().add("payment-btn-stripe");
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setStyle("-fx-background-color: linear-gradient(to right, #6772e5, #7b68ee); "
            + "-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; "
            + "-fx-padding: 14 32; -fx-background-radius: 12; -fx-cursor: hand;");

        // ── Back button ──
        Button btnBack = new Button("← Retour");
        btnBack.getStyleClass().add("payment-btn-cancel");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> {
            footer.getChildren().clear();
            buildStripePaymentFooter(footer, offer, opportunity, project, overlay, popup, root);
            // Re-add close button
            Button btnClose2 = new Button("✕  Fermer");
            btnClose2.getStyleClass().add("payment-btn-cancel");
            btnClose2.setMaxWidth(Double.MAX_VALUE);
            btnClose2.setOnAction(ev -> closePopup(overlay, popup, root));
            footer.getChildren().add(btnClose2);
        });

        btnPay.setOnAction(e -> {
            // ── VALIDATION ──
            boolean valid = true;

            // Card number: must be 16 digits, Luhn check
            String cardDigits = txtCardNum.getText().replaceAll("[^0-9]", "");
            if (cardDigits.length() < 13 || cardDigits.length() > 19) {
                errCardNum.setText("Numéro de carte invalide (13-19 chiffres)");
                errCardNum.setVisible(true);
                txtCardNum.setStyle(txtCardNum.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else if (!luhnCheck(cardDigits)) {
                errCardNum.setText("Numéro de carte invalide (vérification Luhn)");
                errCardNum.setVisible(true);
                txtCardNum.setStyle(txtCardNum.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else {
                errCardNum.setVisible(false);
                txtCardNum.setStyle(txtCardNum.getStyle().replace("-fx-border-color: #e74c3c", "-fx-border-color: #27ae60"));
            }

            // Expiry: MM/YY, future date
            String expiryText = txtExpiry.getText().trim();
            if (!expiryText.matches("\\d{2}/\\d{2}")) {
                errExpiry.setText("Format invalide (MM/AA)");
                errExpiry.setVisible(true);
                txtExpiry.setStyle(txtExpiry.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else {
                int month = Integer.parseInt(expiryText.substring(0, 2));
                int year = 2000 + Integer.parseInt(expiryText.substring(3, 5));
                if (month < 1 || month > 12) {
                    errExpiry.setText("Mois invalide (01-12)");
                    errExpiry.setVisible(true);
                    txtExpiry.setStyle(txtExpiry.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                    valid = false;
                } else {
                    java.time.YearMonth expiry = java.time.YearMonth.of(year, month);
                    if (expiry.isBefore(java.time.YearMonth.now())) {
                        errExpiry.setText("Carte expirée");
                        errExpiry.setVisible(true);
                        txtExpiry.setStyle(txtExpiry.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                        valid = false;
                    } else {
                        errExpiry.setVisible(false);
                        txtExpiry.setStyle(txtExpiry.getStyle().replace("-fx-border-color: #e74c3c", "-fx-border-color: #27ae60"));
                    }
                }
            }

            // CVV: 3 or 4 digits
            String cvv = txtCvv.getText().trim();
            if (cvv.length() < 3 || cvv.length() > 4) {
                errCvv.setText("CVV invalide (3-4 chiffres)");
                errCvv.setVisible(true);
                txtCvv.setStyle(txtCvv.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else {
                errCvv.setVisible(false);
                txtCvv.setStyle(txtCvv.getStyle().replace("-fx-border-color: #e74c3c", "-fx-border-color: #27ae60"));
            }

            // Cardholder name
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                errName.setText("Le nom du titulaire est obligatoire");
                errName.setVisible(true);
                txtName.setStyle(txtName.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else if (name.length() < 2) {
                errName.setText("Nom trop court");
                errName.setVisible(true);
                txtName.setStyle(txtName.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #e74c3c"));
                valid = false;
            } else {
                errName.setVisible(false);
                txtName.setStyle(txtName.getStyle().replace("-fx-border-color: #e74c3c", "-fx-border-color: #27ae60"));
            }

            if (!valid) return;

            // ── PROCESS PAYMENT ──
            if (!PaymentService.isConfigured()) {
                lblPayStatus.setText("⚠ Stripe non configuré — ajoutez votre clé API.");
                lblPayStatus.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-alignment: center;");
                return;
            }

            btnPay.setDisable(true);
            btnPay.setText("⏳  Traitement en cours…");
            btnBack.setDisable(true);
            spinner.setVisible(true);
            lblPayStatus.setText("Connexion sécurisée à Stripe…");
            lblPayStatus.setStyle("-fx-text-fill: #666; -fx-alignment: center;");

            long amountCents = offer.getProposedAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

            String description = "Investissement NAJAHNI — "
                + (project != null ? project.getTitle() : "Projet")
                + " | Opportunité #" + opportunity.getId()
                + " | Offre #" + offer.getId()
                + " | Carte: ****" + cardDigits.substring(cardDigits.length() - 4);

            paymentService.createPaymentIntent(amountCents, "eur", description)
                .thenAccept(result -> Platform.runLater(() -> {
                    spinner.setVisible(false);
                    if (result.isSuccess()) {
                        // Mark as paid in DB
                        offerService.markAsPaid(offer.getId(), result.getPaymentIntentId());
                        offer.setPaid(true);
                        offer.setPaymentIntentId(result.getPaymentIntentId());

                        // Success UI
                        footer.getChildren().clear();
                        footer.setSpacing(12);

                        VBox successBox = new VBox(10);
                        successBox.setAlignment(Pos.CENTER);
                        successBox.setStyle("-fx-background-color: #eafaf1; -fx-background-radius: 14; -fx-padding: 20;");

                        Label confetti = new Label("🎊");
                        confetti.setStyle("-fx-font-size: 48;");
                        confetti.setOpacity(0);
                        FadeTransition confettiFade = new FadeTransition(Duration.millis(600), confetti);
                        confettiFade.setFromValue(0); confettiFade.setToValue(1);
                        ScaleTransition confettiScale = new ScaleTransition(Duration.millis(600), confetti);
                        confettiScale.setFromX(0.3); confettiScale.setFromY(0.3);
                        confettiScale.setToX(1.2); confettiScale.setToY(1.2);
                        new ParallelTransition(confettiFade, confettiScale).play();

                        Label successLabel = new Label("✅ Paiement réussi !");
                        successLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

                        Label txLabel = new Label("🧾 " + result.getPaymentIntentId());
                        txLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");

                        Label redirectLabel = new Label("Redirection vers le portefeuille…");
                        redirectLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60; -fx-font-style: italic;");

                        successBox.getChildren().addAll(confetti, successLabel, txLabel, redirectLabel);
                        footer.getChildren().add(successBox);

                        // Auto-close and navigate to portfolio after 2.5s
                        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
                        pause.setOnFinished(ev -> {
                            closePopup(overlay, popup, root);
                            // Remove the paid offer from the local list
                            allOffers.removeIf(o -> o.getId() == offer.getId());
                            updateStats();
                            displayCards(allOffers);
                            // Navigate to Portfolio
                            navigateToPortfolio();
                        });
                        pause.play();
                    } else {
                        btnPay.setText("🔐  Réessayer le paiement");
                        btnPay.setDisable(false);
                        btnBack.setDisable(false);
                        lblPayStatus.setText("❌ " + result.getErrorMessage());
                        lblPayStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: center;");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        spinner.setVisible(false);
                        btnPay.setText("🔐  Réessayer");
                        btnPay.setDisable(false);
                        btnBack.setDisable(false);
                        lblPayStatus.setText("❌ Erreur : " + ex.getMessage());
                        lblPayStatus.setStyle("-fx-text-fill: #e74c3c; -fx-alignment: center;");
                    });
                    return null;
                });
        });

        // Focus animation on fields
        for (TextField tf : new TextField[]{txtCardNum, txtExpiry, txtCvv, txtName}) {
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    tf.setStyle(tf.getStyle().replace("-fx-border-color: #dcdde1", "-fx-border-color: #6772e5"));
                } else {
                    tf.setStyle(tf.getStyle().replace("-fx-border-color: #6772e5", "-fx-border-color: #dcdde1"));
                }
            });
        }

        // Animate form appearance
        VBox formContainer = new VBox(12, formTitle, secureLabel, cardNumBox, expiryRow, nameBox,
            amountLabel, btnPay, spinnerBox, btnBack);
        formContainer.setOpacity(0);

        footer.getChildren().add(formContainer);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), formContainer);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), formContainer);
        slideIn.setFromY(15); slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();
    }

    /** Luhn algorithm to validate card numbers. */
    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /** Navigate to the Portfolio tab in FrontOfficeController. */
    private void navigateToPortfolio() {
        try {
            javafx.scene.Scene scene = offersContainer.getScene();
            if (scene != null && scene.getRoot().getUserData() instanceof FrontOfficeController foc) {
                foc.showPortfolio();
            }
        } catch (Exception e) {
            LOG.warning("Could not navigate to portfolio: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════════

    private void animatePopupIn(StackPane overlay, VBox popup) {
        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(300), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);

        FadeTransition fadePopup = new FadeTransition(Duration.millis(400), popup);
        fadePopup.setFromValue(0);
        fadePopup.setToValue(1);

        ScaleTransition scalePopup = new ScaleTransition(Duration.millis(450), popup);
        scalePopup.setFromX(0.85);
        scalePopup.setFromY(0.85);
        scalePopup.setToX(1.0);
        scalePopup.setToY(1.0);
        scalePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0)); // iOS-like spring

        TranslateTransition slidePopup = new TranslateTransition(Duration.millis(400), popup);
        slidePopup.setFromY(40);
        slidePopup.setToY(0);
        slidePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));

        ParallelTransition openAnim = new ParallelTransition(
            fadeOverlay, fadePopup, scalePopup, slidePopup
        );
        openAnim.play();
    }

    private void closePopup(StackPane overlay, VBox popup, StackPane root) {
        // Remove blur from background
        if (root.getChildren().size() > 1) {
            root.getChildren().get(0).setEffect(null);
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), overlay);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), popup);
        scaleOut.setToX(0.88);
        scaleOut.setToY(0.88);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), popup);
        slideOut.setToY(30);

        FadeTransition fadePopup = new FadeTransition(Duration.millis(200), popup);
        fadePopup.setToValue(0);

        ParallelTransition closeAnim = new ParallelTransition(fadeOut, scaleOut, slideOut, fadePopup);
        closeAnim.setOnFinished(e -> root.getChildren().remove(overlay));
        closeAnim.play();
    }

    /**
     * Get guaranteed full-screen StackPane from the scene graph.
     * Strategy: use the Scene root. If it's not a StackPane, wrap it.
     */
    private StackPane getSceneRootStack() {
        try {
            javafx.scene.Scene scene = offersContainer.getScene();
            if (scene == null) return null;
            javafx.scene.Parent sceneRoot = scene.getRoot();

            // Walk up to find a StackPane (e.g. foContentArea)
            javafx.scene.Parent current = offersContainer.getParent();
            StackPane candidate = null;
            while (current != null) {
                if (current instanceof StackPane sp) {
                    candidate = sp; // keep the outermost one
                }
                current = current.getParent();
            }
            if (candidate != null) return candidate;

            // Fallback: wrap scene root
            if (sceneRoot instanceof StackPane sp) return sp;
            StackPane wrapper = new StackPane(sceneRoot);
            scene.setRoot(wrapper);
            return wrapper;
        } catch (Exception e) {
            LOG.warning("Could not find root stack: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SUBMIT / DELETE / FILTER
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void submitOffer() {
        lblFormMsg.setText("");
        try {
            if (cboOpportunity.getValue() == null)
                throw new IllegalArgumentException("Veuillez sélectionner une opportunité.");

            String amountText = txtAmount.getText() != null ? txtAmount.getText().trim() : "";
            if (amountText.isEmpty())
                throw new IllegalArgumentException("Le montant est obligatoire.");

            BigDecimal amount;
            try { amount = new BigDecimal(amountText); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Format de montant invalide."); }

            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Le montant doit être supérieur à zéro.");

            InvestmentOffer offer = new InvestmentOffer();
            offer.setProposedAmount(amount);
            offer.setStatus(OfferStatus.PENDING);
            offer.setOpportunityId(cboOpportunity.getValue().getId());
            int investorId = SessionManager.getInstance().getCurrentUserId();
            if (investorId <= 0) throw new IllegalArgumentException("Vous devez être connecté.");
            offer.setInvestorId(investorId);

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

    private void deleteOffer(InvestmentOffer offer) {
        if (AlertUtils.showConfirmation("Annuler l'offre",
                "Voulez-vous annuler cette offre de " + offer.getFormattedAmount() + " ?")) {
            boolean deleted = offerService.deleteOffer(offer.getId());
            if (deleted) { AlertUtils.showSuccess("Offre annulée !"); loadData(); }
            else AlertUtils.showError("Échec", "Impossible d'annuler l'offre.");
        }
    }

    @FXML
    public void applyFilter() {
        String statusFilter = cboStatusFilter.getValue();
        List<InvestmentOffer> filtered = allOffers.stream()
            .filter(o -> statusFilter == null || statusFilter.equals("Tous")
                || o.getStatus().getDisplayName().equals(statusFilter))
            .collect(Collectors.toList());
        displayCards(filtered);
    }

    @FXML
    public void clearFilter() {
        cboStatusFilter.setValue("Tous");
        displayCards(allOffers);
    }
}
