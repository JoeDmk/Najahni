package com.najahni.controllers;

import com.najahni.models.*;
import com.najahni.services.*;
import com.najahni.utils.AnimationUtils;
import com.najahni.utils.PDFPreviewPopup;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Contrôleur Front-Office — Portfolio d'Investissements.
 * Affiche les investissements payés et confirmés de l'investisseur.
 */
public class FrontPortfolioController {

    private static final Logger LOG = Logger.getLogger(FrontPortfolioController.class.getName());

    @FXML private Label lblTotalInvested;
    @FXML private Label lblTotalCount;
    @FXML private Label lblAvgAmount;
    @FXML private FlowPane portfolioContainer;
    @FXML private VBox emptyState;

    private final InvestmentOfferService offerService;
    private final InvestmentOpportunityService opportunityService;
    private final ProjectService projectService;

    public FrontPortfolioController() {
        this.offerService = new InvestmentOfferService();
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
    }

    @FXML
    public void initialize() {
        loadPortfolio();
    }

    @FXML
    public void refreshPortfolio() {
        loadPortfolio();
    }

    /**
     * Exporte le portfolio complet en PDF avec aperçu dans l'application.
     */
    @FXML
    public void exportPortfolioPDF() {
        List<InvestmentOffer> paidOffers = offerService.findAll().stream()
                .filter(InvestmentOffer::isPaid)
                .collect(Collectors.toList());

        if (paidOffers.isEmpty()) {
            LOG.info("Aucun investissement payé à exporter");
            return;
        }

        // Fetch associated data
        List<InvestmentOpportunity> opportunities = new ArrayList<>();
        List<Project> projects = new ArrayList<>();
        for (InvestmentOffer offer : paidOffers) {
            opportunityService.findById(offer.getOpportunityId()).ifPresent(opp -> {
                opportunities.add(opp);
                projectService.findById(opp.getProjectId()).ifPresent(projects::add);
            });
        }

        String investorName = paidOffers.stream()
                .filter(o -> o.getInvestorName() != null)
                .map(InvestmentOffer::getInvestorName)
                .findFirst().orElse("Mon Portfolio");

        InvestmentPDFService pdfService = new InvestmentPDFService();
        byte[] pdf = pdfService.generatePortfolioReport(paidOffers, opportunities, projects, investorName);
        if (pdf != null) {
            StackPane rootStack = getSceneRootStack();
            if (rootStack != null) {
                PDFPreviewPopup.show(pdf, "Rapport Portfolio",
                    "rapport_portfolio_" + java.time.LocalDate.now() + ".pdf", rootStack);
            }
        }
    }

    private void loadPortfolio() {
        // Show all paid offers (same scope as the offers page which shows all unpaid)
        List<InvestmentOffer> paidOffers = offerService.findAll().stream()
                .filter(InvestmentOffer::isPaid)
                .collect(Collectors.toList());
        System.out.println("[Portfolio] Found " + paidOffers.size() + " paid offers");

        updateStats(paidOffers);
        displayCards(paidOffers);
    }

    private void updateStats(List<InvestmentOffer> offers) {
        if (offers.isEmpty()) {
            lblTotalInvested.setText("0,00 €");
            lblTotalCount.setText("0");
            lblAvgAmount.setText("0,00 €");
            return;
        }

        BigDecimal total = offers.stream()
            .map(InvestmentOffer::getProposedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = total.divide(BigDecimal.valueOf(offers.size()), 2, RoundingMode.HALF_UP);

        lblTotalInvested.setText(String.format("%,.2f €", total));
        lblTotalCount.setText(String.valueOf(offers.size()));
        lblAvgAmount.setText(String.format("%,.2f €", avg));
    }

    private void displayCards(List<InvestmentOffer> offers) {
        portfolioContainer.getChildren().clear();

        if (offers.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        int delay = 0;
        for (InvestmentOffer offer : offers) {
            VBox card = createPortfolioCard(offer);
            portfolioContainer.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 300, delay);
            delay += 80;
        }
    }

    private VBox createPortfolioCard(InvestmentOffer offer) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(300);
        card.setMinWidth(270);
        card.setMaxWidth(330);
        card.setPadding(new Insets(0));

        // ── Green top header ──
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); "
            + "-fx-background-radius: 12 12 0 0;");

        Label iconLabel = new Label("✅");
        iconLabel.setStyle("-fx-font-size: 28;");

        Label amountLabel = new Label(offer.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");

        Label paidBadge = new Label("PAYÉ");
        paidBadge.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; "
            + "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 10;");

        header.getChildren().addAll(iconLabel, amountLabel, paidBadge);

        // ── Body ──
        VBox body = new VBox(8);
        body.setPadding(new Insets(16, 20, 18, 20));

        // Project info
        String projectTitle = offer.getProjectTitle() != null ? offer.getProjectTitle() : "Projet";
        Label lblProject = new Label("🏢 " + projectTitle);
        lblProject.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        lblProject.setWrapText(true);

        String sector = offer.getProjectSector() != null ? offer.getProjectSector() : "";
        Label lblSector = new Label();
        if (!sector.isEmpty()) {
            lblSector.setText("🏷 " + sector);
            lblSector.setStyle("-fx-font-size: 11; -fx-text-fill: #8e44ad; -fx-font-weight: bold; "
                + "-fx-background-color: #f4ecf7; -fx-background-radius: 6; -fx-padding: 3 8;");
        }

        // Opportunity
        String oppText = offer.getOpportunityDescription() != null
            ? (offer.getOpportunityDescription().length() > 70
                ? offer.getOpportunityDescription().substring(0, 67) + "…"
                : offer.getOpportunityDescription())
            : "Opportunité #" + offer.getOpportunityId();
        Label lblOpp = new Label("📋 " + oppText);
        lblOpp.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");
        lblOpp.setWrapText(true);
        lblOpp.setMaxHeight(40);

        Separator sep = new Separator();

        // Payment info
        VBox paymentInfo = new VBox(4);
        if (offer.getPaymentIntentId() != null) {
            Label txLabel = new Label("🧾 " + offer.getPaymentIntentId());
            txLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #bdc3c7;");
            txLabel.setWrapText(true);
            paymentInfo.getChildren().add(txLabel);
        }

        if (offer.getPaidAt() != null) {
            Label dateLabel = new Label("📅 Payé le " +
                offer.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
            paymentInfo.getChildren().add(dateLabel);
        }

        body.getChildren().add(lblProject);
        if (!sector.isEmpty()) body.getChildren().add(lblSector);
        body.getChildren().addAll(lblOpp, sep, paymentInfo);

        // ── Action buttons ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(6, 20, 14, 20));

        Button btnDetails = new Button("📄 Détails");
        btnDetails.getStyleClass().add("front-btn-details");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, Priority.ALWAYS);
        btnDetails.setOnAction(e -> showDetailPopup(offer));

        Button btnPDF = new Button("📑 PDF");
        btnPDF.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); "
            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12; "
            + "-fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand;");
        btnPDF.setOnMouseEntered(e -> btnPDF.setStyle(btnPDF.getStyle().replace("#3498db", "#5dade2")));
        btnPDF.setOnMouseExited(e -> btnPDF.setStyle(btnPDF.getStyle().replace("#5dade2", "#3498db")));
        btnPDF.setOnAction(e -> {
            Optional<InvestmentOpportunity> optOpp = opportunityService.findById(offer.getOpportunityId());
            InvestmentOpportunity opp = optOpp.orElse(null);
            Project proj = null;
            if (opp != null) proj = projectService.findById(opp.getProjectId()).orElse(null);
            InvestmentPDFService pdfService = new InvestmentPDFService();
            byte[] pdf = pdfService.generatePaymentReceipt(offer, opp, proj);
            if (pdf != null) {
                StackPane rootStack = getSceneRootStack();
                if (rootStack != null) {
                    PDFPreviewPopup.show(pdf, "Reçu de Paiement",
                        "recu_paiement_" + offer.getId() + ".pdf", rootStack);
                }
            }
        });

        actions.getChildren().addAll(btnDetails, btnPDF);

        card.getChildren().addAll(header, body, actions);

        // ── Hover effect ──
        DropShadow normalShadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.08));
        DropShadow hoverShadow = new DropShadow(18, Color.web("#27ae6055"));
        card.setEffect(normalShadow);
        card.setOnMouseEntered(e -> {
            card.setEffect(hoverShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.02); st.setToY(1.02); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(normalShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL POPUP — with secured payment info
    // ═══════════════════════════════════════════════════════════

    private void showDetailPopup(InvestmentOffer offer) {
        Optional<InvestmentOpportunity> optOpp = opportunityService.findById(offer.getOpportunityId());
        InvestmentOpportunity opportunity = optOpp.orElse(null);
        Project project = null;
        if (opportunity != null) {
            project = projectService.findById(opportunity.getProjectId()).orElse(null);
        }

        StackPane root = getSceneRootStack();
        if (root == null) return;

        // ── Overlay ──
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("payment-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);

        // ── Popup container ──
        VBox popup = new VBox(0);
        popup.getStyleClass().add("payment-popup");
        popup.setMaxWidth(520);
        popup.setMinWidth(440);
        popup.setMaxHeight(680);
        popup.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(popup, Pos.CENTER);
        popup.setScaleX(0.85);
        popup.setScaleY(0.85);
        popup.setOpacity(0);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(popup.widthProperty());
        clip.heightProperty().bind(popup.heightProperty());
        popup.setClip(clip);

        // ── Header (green gradient) ──
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(28, 28, 22, 28));
        header.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71, #27ae60);");

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 42;");
        Label title = new Label("Investissement Confirmé");
        title.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label(offer.getFormattedAmount());
        subtitle.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #FFD700;");
        header.getChildren().addAll(icon, title, subtitle);

        // ── Body ──
        VBox body = new VBox(18);
        body.setPadding(new Insets(24, 28, 10, 28));
        body.getStyleClass().add("payment-popup-body");

        // Offer details section
        body.getChildren().add(buildSection("💼", "DÉTAILS DE L'OFFRE",
            buildDetailRow("💰 Montant investi", offer.getFormattedAmount()),
            buildDetailRow("✅ Statut", "Payé & Confirmé"),
            buildDetailRow("👤 Investisseur",
                offer.getInvestorName() != null ? offer.getInvestorName() : "Moi")
        ));

        // Opportunity section
        if (opportunity != null) {
            VBox oppSection = buildSection("🎯", "OPPORTUNITÉ",
                buildDetailRow("📋 Description",
                    opportunity.getDescription() != null ? opportunity.getDescription() : "N/A"),
                buildDetailRow("🎯 Montant cible", opportunity.getFormattedAmount()),
                buildDetailRow("📅 Deadline", opportunity.getDeadline() != null
                    ? opportunity.getDeadline().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    : "Non défini")
            );
            body.getChildren().add(oppSection);
        }

        // Project section
        if (project != null) {
            body.getChildren().add(buildSection("🏢", "PROJET",
                buildDetailRow("📌 Titre", project.getTitle() != null ? project.getTitle() : "N/A"),
                buildDetailRow("🏷 Secteur", project.getSector() != null ? project.getSector() : "N/A"),
                buildDetailRow("📝 Description", project.getDescription() != null
                    ? (project.getDescription().length() > 150
                        ? project.getDescription().substring(0, 147) + "…"
                        : project.getDescription())
                    : "N/A")
            ));
        }

        // Payment details section (SECURED)
        VBox paymentSection = buildSection("🔐", "DÉTAILS DU PAIEMENT",
            buildDetailRow("🧾 Transaction ID",
                offer.getPaymentIntentId() != null ? offer.getPaymentIntentId() : "N/A"),
            buildDetailRow("📅 Date de paiement",
                offer.getPaidAt() != null
                    ? offer.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                    : "N/A"),
            buildDetailRow("💳 Carte bancaire", "**** **** **** ****"),
            buildDetailRow("🔒 Méthode", "Stripe (paiement sécurisé)"),
            buildDetailRow("💱 Devise", "EUR"),
            buildDetailRow("📊 Statut", "✅ Succès")
        );

        // Add a secure notice
        VBox secureNotice = new VBox(6);
        secureNotice.setAlignment(Pos.CENTER);
        secureNotice.setStyle("-fx-background-color: #eaf2f8; -fx-background-radius: 10; -fx-padding: 12;");
        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 20;");
        Label secureText = new Label("Les informations de la carte bancaire sont sécurisées\net traitées exclusivement par Stripe. Aucune donnée\nsensible n'est stockée sur nos serveurs.");
        secureText.setStyle("-fx-font-size: 11; -fx-text-fill: #5b7fa3; -fx-text-alignment: center;");
        secureText.setWrapText(true);
        secureNotice.getChildren().addAll(lockIcon, secureText);

        VBox paymentContent = (VBox) paymentSection.getChildren().get(1);
        paymentContent.getChildren().add(secureNotice);

        body.getChildren().add(paymentSection);

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollBody.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollBody.getStyleClass().add("payment-scroll");
        VBox.setVgrow(scrollBody, Priority.ALWAYS);

        // ── Footer ──
        VBox footer = new VBox(12);
        footer.setPadding(new Insets(20, 28, 24, 28));
        footer.setAlignment(Pos.CENTER);
        footer.getStyleClass().add("payment-popup-footer");

        HBox confirmedBox = new HBox(10);
        confirmedBox.setAlignment(Pos.CENTER);
        confirmedBox.setStyle("-fx-background-color: #eafaf1; -fx-background-radius: 12; -fx-padding: 14;");
        Label confirmedLabel = new Label("✅ Investissement confirmé et sécurisé");
        confirmedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13;");
        confirmedBox.getChildren().add(confirmedLabel);

        // PDF Receipt button
        final Project projectForPdf = project;
        final InvestmentOpportunity oppForPdf = opportunity;
        Button btnReceiptPDF = new Button("📄  Reçu PDF");
        btnReceiptPDF.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); "
            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; "
            + "-fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;");
        btnReceiptPDF.setMaxWidth(Double.MAX_VALUE);
        btnReceiptPDF.setOnMouseEntered(e -> btnReceiptPDF.setStyle(btnReceiptPDF.getStyle().replace("#3498db", "#5dade2")));
        btnReceiptPDF.setOnMouseExited(e -> btnReceiptPDF.setStyle(btnReceiptPDF.getStyle().replace("#5dade2", "#3498db")));
        btnReceiptPDF.setOnAction(e -> {
            InvestmentPDFService pdfService = new InvestmentPDFService();
            byte[] pdf = pdfService.generatePaymentReceipt(offer, oppForPdf, projectForPdf);
            if (pdf != null) {
                PDFPreviewPopup.show(pdf, "Reçu de Paiement",
                    "recu_paiement_" + offer.getId() + ".pdf", root);
            }
        });

        Button btnClose = new Button("✕  Fermer");
        btnClose.getStyleClass().add("payment-btn-cancel");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setOnAction(e -> closePopup(overlay, popup, root));

        footer.getChildren().addAll(confirmedBox, btnReceiptPDF, btnClose);

        popup.getChildren().addAll(header, scrollBody, footer);
        overlay.getChildren().add(popup);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) closePopup(overlay, popup, root);
        });
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closePopup(overlay, popup, root);
        });

        root.getChildren().add(overlay);
        overlay.requestFocus();

        if (root.getChildren().size() > 1) {
            root.getChildren().get(0).setEffect(new GaussianBlur(6));
        }

        // ── Animate in ──
        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(300), overlay);
        fadeOverlay.setFromValue(0); fadeOverlay.setToValue(1);
        FadeTransition fadePopup = new FadeTransition(Duration.millis(400), popup);
        fadePopup.setFromValue(0); fadePopup.setToValue(1);
        ScaleTransition scalePopup = new ScaleTransition(Duration.millis(450), popup);
        scalePopup.setFromX(0.85); scalePopup.setFromY(0.85);
        scalePopup.setToX(1.0); scalePopup.setToY(1.0);
        scalePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        TranslateTransition slidePopup = new TranslateTransition(Duration.millis(400), popup);
        slidePopup.setFromY(40); slidePopup.setToY(0);
        slidePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        new ParallelTransition(fadeOverlay, fadePopup, scalePopup, slidePopup).play();
    }

    // ─── Popup Helpers ──────────────────────────────────────

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

    private void closePopup(StackPane overlay, VBox popup, StackPane root) {
        if (root.getChildren().size() > 1) {
            root.getChildren().get(0).setEffect(null);
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), overlay);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), popup);
        scaleOut.setToX(0.88); scaleOut.setToY(0.88);
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), popup);
        slideOut.setToY(30);
        FadeTransition fadePopup = new FadeTransition(Duration.millis(200), popup);
        fadePopup.setToValue(0);
        ParallelTransition closeAnim = new ParallelTransition(fadeOut, scaleOut, slideOut, fadePopup);
        closeAnim.setOnFinished(e -> root.getChildren().remove(overlay));
        closeAnim.play();
    }

    private StackPane getSceneRootStack() {
        try {
            javafx.scene.Scene scene = portfolioContainer.getScene();
            if (scene == null) return null;
            Parent current = portfolioContainer.getParent();
            StackPane candidate = null;
            while (current != null) {
                if (current instanceof StackPane sp) {
                    candidate = sp;
                }
                current = current.getParent();
            }
            if (candidate != null) return candidate;
            if (scene.getRoot() instanceof StackPane sp) return sp;
            StackPane wrapper = new StackPane(scene.getRoot());
            scene.setRoot(wrapper);
            return wrapper;
        } catch (Exception e) {
            LOG.warning("Could not find root stack: " + e.getMessage());
            return null;
        }
    }
}
