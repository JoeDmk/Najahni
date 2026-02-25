package com.najahni.controllers;

import com.najahni.models.InvestmentOffer;
import com.najahni.services.InvestmentOfferService;
import com.najahni.services.SessionManager;
import com.najahni.utils.AnimationUtils;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contrôleur Front-Office — Portefeuille d'Investissements.
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

    public FrontPortfolioController() {
        this.offerService = new InvestmentOfferService();
    }

    @FXML
    public void initialize() {
        loadPortfolio();
    }

    @FXML
    public void refreshPortfolio() {
        loadPortfolio();
    }

    private void loadPortfolio() {
        int investorId = SessionManager.getInstance().getCurrentUserId();
        List<InvestmentOffer> paidOffers = offerService.findPaidByInvestor(investorId);

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

        card.getChildren().addAll(header, body);

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
}
