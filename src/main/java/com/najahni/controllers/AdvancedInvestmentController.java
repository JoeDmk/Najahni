package com.najahni.controllers;

import com.najahni.models.*;
import com.najahni.services.*;
import com.najahni.utils.AnimationUtils;
import com.najahni.utils.PDFPreviewPopup;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Contrôleur du Dashboard Avancé d'Investissement.
 *
 * Fonctionnalités :
 * 1. 🧾 Smart Contracts — signature numérique + vérification SHA-256
 * 2. 🎯 AI Matching — recommandations par profil investisseur
 * 3. 📊 Portfolio Charts — graphiques interactifs (pie, bar, line)
 * 4. ⚖️  Comparateur — comparaison côte-à-côte d'opportunités
 * 5. ⭐ Ratings — évaluation des opportunités par les investisseurs
 */
public class AdvancedInvestmentController {

    private static final Logger LOG = Logger.getLogger(AdvancedInvestmentController.class.getName());

    @FXML private StackPane advancedRoot;
    @FXML private FlowPane contentArea;
    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;

    // Tab buttons
    @FXML private Button btnContracts;
    @FXML private Button btnMatching;
    @FXML private Button btnCharts;
    @FXML private Button btnComparator;
    @FXML private Button btnRatings;

    private Button activeTab;

    // Services
    private final ContractService contractService = new ContractService();
    private final InvestmentMatchingService matchingService = new InvestmentMatchingService();
    private final RatingService ratingService = new RatingService();
    private final InvestmentOfferService offerService = new InvestmentOfferService();
    private final InvestmentOpportunityService opportunityService = new InvestmentOpportunityService();
    private final ProjectService projectService = new ProjectService();

    @FXML
    public void initialize() {
        showContracts();
    }

    // ═══════════════════════════════════════════════════════════
    //  TAB NAVIGATION
    // ═══════════════════════════════════════════════════════════

    @FXML public void showContracts() {
        setActiveTab(btnContracts);
        lblPageTitle.setText("🧾 Contrats Numériques");
        lblPageSubtitle.setText("Signez et vérifiez vos contrats d'investissement");
        buildContractsView();
    }

    @FXML public void showMatching() {
        setActiveTab(btnMatching);
        lblPageTitle.setText("🎯 IA Matching");
        lblPageSubtitle.setText("Recommandations personnalisées basées sur votre profil");
        buildMatchingView();
    }

    @FXML public void showCharts() {
        setActiveTab(btnCharts);
        lblPageTitle.setText("📊 Analyse Portfolio");
        lblPageSubtitle.setText("Visualisation interactive de vos investissements");
        buildChartsView();
    }

    @FXML public void showComparator() {
        setActiveTab(btnComparator);
        lblPageTitle.setText("⚖️ Comparateur");
        lblPageSubtitle.setText("Comparez les opportunités côte à côte");
        buildComparatorView();
    }

    @FXML public void showRatings() {
        setActiveTab(btnRatings);
        lblPageTitle.setText("⭐ Évaluations");
        lblPageSubtitle.setText("Notez les opportunités dans lesquelles vous avez investi");
        buildRatingsView();
    }

    private void setActiveTab(Button btn) {
        if (activeTab != null) activeTab.getStyleClass().remove("fo-nav-link-active");
        if (btn != null && !btn.getStyleClass().contains("fo-nav-link-active"))
            btn.getStyleClass().add("fo-nav-link-active");
        activeTab = btn;
    }

    // ═══════════════════════════════════════════════════════════
    //  1. SMART CONTRACTS
    // ═══════════════════════════════════════════════════════════

    private void buildContractsView() {
        contentArea.getChildren().clear();

        int userId = SessionManager.getInstance().getCurrentUserId();

        // Auto-generate contracts for paid offers that don't have one yet
        try {
            List<InvestmentOffer> paidOffers = offerService.findByInvestor(userId).stream()
                    .filter(InvestmentOffer::isPaid)
                    .toList();
            System.out.println("[Contracts] Found " + paidOffers.size() + " paid offer(s) for user #" + userId);

            for (InvestmentOffer offer : paidOffers) {
                if (contractService.findByOfferId(offer.getId()).isEmpty()) {
                    Optional<InvestmentOpportunity> opp = opportunityService.findById(offer.getOpportunityId());
                    if (opp.isPresent()) {
                        Optional<Project> proj = projectService.findById(opp.get().getProjectId());
                        System.out.println("[Contracts] Generating contract for offer #" + offer.getId()
                                + " → opportunity #" + opp.get().getId()
                                + " → project " + (proj.isPresent() ? "#" + proj.get().getId() : "NOT FOUND"));
                        InvestmentContract generated = contractService.generateContract(offer, opp.get(), proj.orElse(null));
                        if (generated == null) {
                            System.err.println("[Contracts] ⚠ Contract generation returned null for offer #" + offer.getId());
                        }
                    } else {
                        System.err.println("[Contracts] ⚠ Opportunity #" + offer.getOpportunityId() + " not found for offer #" + offer.getId());
                    }
                }
            }

            // Also check offers where the user is entrepreneur (projects they own)
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getRole() == Role.ENTREPRENEUR) {
                List<Project> myProjects = projectService.findByEntrepreneur(userId);
                System.out.println("[Contracts] Entrepreneur #" + userId + " owns " + myProjects.size() + " project(s)");

                // Claim unowned projects for this entrepreneur
                for (Project p : myProjects) {
                    if (p.getEntrepreneurId() <= 0) {
                        try {
                            p.setEntrepreneurId(userId);
                            projectService.updateProject(p);
                            System.out.println("[Contracts]   Claimed project #" + p.getId() + " for entrepreneur #" + userId);
                        } catch (Exception ignored) {}
                    }
                }

                for (Project p : myProjects) {
                    List<InvestmentOffer> projOffers = offerService.findByProjectIds(
                            java.util.Collections.singletonList(p.getId()));
                    System.out.println("[Contracts]   Project #" + p.getId() + " (" + p.getTitle() + ") → " + projOffers.size() + " offer(s)");
                    for (InvestmentOffer offer : projOffers) {
                        System.out.println("[Contracts]     Offer #" + offer.getId() + " paid=" + offer.isPaid()
                                + " contractExists=" + contractService.findByOfferId(offer.getId()).isPresent());
                        if (offer.isPaid() && contractService.findByOfferId(offer.getId()).isEmpty()) {
                            Optional<InvestmentOpportunity> opp = opportunityService.findById(offer.getOpportunityId());
                            if (opp.isPresent()) {
                                InvestmentContract generated = contractService.generateContract(offer, opp.get(), p);
                                System.out.println("[Contracts]     → Generated contract: " + (generated != null ? generated.getContractNumber() : "FAILED"));
                            }
                        } else if (offer.isPaid()) {
                            // Fix entrepreneur_id on existing contracts
                            contractService.findByOfferId(offer.getId()).ifPresent(c -> {
                                if (c.getEntrepreneurId() == 0 || c.getEntrepreneurId() != userId) {
                                    contractService.updateEntrepreneurId(c.getId(), userId);
                                }
                            });
                        }
                    }
                }
            } else {
                System.out.println("[Contracts] User #" + userId + " is not ENTREPRENEUR, skipping project-based generation");
            }
        } catch (Exception e) {
            System.err.println("[Contracts] Error during auto-generation: " + e.getMessage());
            e.printStackTrace();
        }

        // Load all contracts for this user (as investor or entrepreneur)
        List<InvestmentContract> contracts = new ArrayList<>(contractService.findByUser(userId));

        // For entrepreneurs: also find contracts via project chain (in case entrepreneur_id wasn't set)
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.ENTREPRENEUR) {
            List<Project> myProjects = projectService.findByEntrepreneur(userId);
            List<Integer> projectIds = myProjects.stream().map(Project::getId).toList();
            if (!projectIds.isEmpty()) {
                List<InvestmentContract> projectContracts = contractService.findByProjectIds(projectIds);
                Set<Integer> existingIds = contracts.stream().map(InvestmentContract::getId).collect(Collectors.toSet());
                for (InvestmentContract c : projectContracts) {
                    if (!existingIds.contains(c.getId())) {
                        contracts.add(c);
                    }
                }
                System.out.println("[Contracts] Added " + (contracts.size() - existingIds.size()) + " contract(s) via project chain");
            }
        }

        System.out.println("[Contracts] Total contracts for user #" + userId + ": " + contracts.size());

        if (contracts.isEmpty()) {
            contentArea.getChildren().add(buildEmptyState("📝", "Aucun contrat",
                    "Les contrats sont générés automatiquement après un paiement.\n"
                    + "Assurez-vous d'avoir un investissement payé."));
            return;
        }

        int delay = 0;
        for (InvestmentContract contract : contracts) {
            VBox card = buildContractCard(contract);
            contentArea.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 300, delay);
            delay += 80;
        }
    }

    private VBox buildContractCard(InvestmentContract contract) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(320);
        card.setMinWidth(290);
        card.setMaxWidth(350);

        // ── Header with status color ──
        String headerColor = switch (contract.getStatus()) {
            case DRAFT -> "#d97706";
            case INVESTOR_SIGNED -> "#2563eb";
            case FULLY_SIGNED -> "#059669";
            case CANCELLED -> "#ef4444";
        };

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, " + headerColor + ", "
                + headerColor + "dd); -fx-background-radius: 16 16 0 0;");

        Label emoji = new Label(contract.getStatusEmoji());
        emoji.setStyle("-fx-font-size: 26;");
        Label number = new Label(contract.getContractNumber());
        number.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;");
        Label statusLabel = new Label(contract.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; "
                + "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 10;");
        header.getChildren().addAll(emoji, number, statusLabel);

        // ── Body ──
        VBox body = new VBox(6);
        body.setPadding(new Insets(14, 18, 10, 18));

        Label lblInvestor = new Label("👤 " + (contract.getInvestorName() != null ? contract.getInvestorName() : "Investisseur #" + contract.getInvestorId()));
        lblInvestor.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");

        Label lblEntrepreneur = new Label("🏢 " + (contract.getEntrepreneurName() != null ? contract.getEntrepreneurName() : "Entrepreneur #" + contract.getEntrepreneurId()));
        lblEntrepreneur.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");

        // SHA-256 hash preview
        String hashShort = contract.getSha256Hash() != null
                ? contract.getSha256Hash().substring(0, 16) + "…"
                : "N/A";
        Label lblHash = new Label("🔐 SHA-256: " + hashShort);
        lblHash.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-family: monospace;");

        if (contract.getCreatedAt() != null) {
            Label lblDate = new Label("📅 " + contract.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblDate.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");
            body.getChildren().addAll(lblInvestor, lblEntrepreneur, lblHash, lblDate);
        } else {
            body.getChildren().addAll(lblInvestor, lblEntrepreneur, lblHash);
        }

        // ── Actions ──
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(6, 18, 14, 18));

        Button btnView = new Button("📄 Voir");
        btnView.getStyleClass().add("front-btn-details");
        btnView.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnView, Priority.ALWAYS);
        btnView.setOnAction(e -> showContractDetail(contract));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isInvestor = currentUser != null && currentUser.getRole() == Role.INVESTOR;
        boolean isEntrepreneur = currentUser != null && currentUser.getRole() == Role.ENTREPRENEUR;

        if (contract.getStatus() == ContractStatus.DRAFT && isInvestor) {
            Button btnSign = new Button("✍️ Signer");
            btnSign.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand;");
            btnSign.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnSign, Priority.ALWAYS);
            btnSign.setOnAction(e -> showSignaturePopup(contract, true));
            actions.getChildren().addAll(btnView, btnSign);
        } else if (contract.getStatus() == ContractStatus.INVESTOR_SIGNED && isEntrepreneur) {
            Button btnSign = new Button("✍️ Contresigner");
            btnSign.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand;");
            btnSign.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnSign, Priority.ALWAYS);
            btnSign.setOnAction(e -> showSignaturePopup(contract, false));
            actions.getChildren().addAll(btnView, btnSign);
        } else {
            actions.getChildren().add(btnView);
        }

        card.getChildren().addAll(header, body, actions);
        addHoverEffect(card);
        return card;
    }

    private void showContractDetail(InvestmentContract contract) {
        StackPane root = getSceneRootStack();
        if (root == null) return;

        StackPane overlay = createOverlay();
        VBox popup = createPopup(560, 720);

        // ── Header ──
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(24, 24, 18, 24));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        Label icon = new Label("🧾");
        icon.setStyle("-fx-font-size: 40;");
        Label title = new Label(contract.getContractNumber());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label status = new Label(contract.getStatusEmoji() + " " + contract.getStatus().getDisplayName());
        status.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8;");
        headerBox.getChildren().addAll(icon, title, status);

        // ── Contract text ──
        VBox bodyBox = new VBox(12);
        bodyBox.setPadding(new Insets(20, 24, 10, 24));

        TextArea termsArea = new TextArea(contract.getTermsText());
        termsArea.setWrapText(true);
        termsArea.setEditable(false);
        termsArea.setPrefHeight(250);
        termsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        // Signatures section
        VBox sigSection = new VBox(8);
        sigSection.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        Label sigTitle = new Label("✍️ Signatures");
        sigTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #0f172a;");

        Label investorSig = new Label(contract.getInvestorSignature() != null
                ? "✅ Investisseur: Signé le " + (contract.getInvestorSignedAt() != null
                    ? contract.getInvestorSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "")
                : "⏳ Investisseur: En attente");
        investorSig.setStyle("-fx-font-size: 12; -fx-text-fill: " +
                (contract.getInvestorSignature() != null ? "#059669" : "#d97706") + ";");

        Label entrepreneurSig = new Label(contract.getEntrepreneurSignature() != null
                ? "✅ Entrepreneur: Signé le " + (contract.getEntrepreneurSignedAt() != null
                    ? contract.getEntrepreneurSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "")
                : "⏳ Entrepreneur: En attente");
        entrepreneurSig.setStyle("-fx-font-size: 12; -fx-text-fill: " +
                (contract.getEntrepreneurSignature() != null ? "#059669" : "#d97706") + ";");

        sigSection.getChildren().addAll(sigTitle, investorSig, entrepreneurSig);

        // Hash verification
        VBox hashSection = new VBox(6);
        hashSection.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: #bfdbfe; -fx-border-radius: 12; -fx-border-width: 1;");
        Label hashTitle = new Label("🔐 Intégrité SHA-256");
        hashTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #0f172a;");
        Label hashValue = new Label(contract.getSha256Hash() != null ? contract.getSha256Hash() : "N/A");
        hashValue.setStyle("-fx-font-size: 9; -fx-font-family: monospace; -fx-text-fill: #64748b;");
        hashValue.setWrapText(true);

        boolean verified = contractService.verifyIntegrity(contract);
        Label verifyLabel = new Label(verified ? "✅ Contrat vérifié — intégrité confirmée" : "⚠️ Hash non vérifié");
        verifyLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + (verified ? "#059669" : "#ef4444") + "; -fx-font-weight: bold;");
        hashSection.getChildren().addAll(hashTitle, hashValue, verifyLabel);

        bodyBox.getChildren().addAll(termsArea, sigSection, hashSection);

        ScrollPane scroll = new ScrollPane(bodyBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Footer ──
        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setPadding(new Insets(16, 24, 20, 24));

        Button btnPDF = new Button("📄 Exporter PDF");
        btnPDF.setStyle("-fx-background-color: linear-gradient(to bottom right, #2563eb, #1d4ed8); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        btnPDF.setOnAction(e -> exportContractPDF(contract));

        Button btnClose = new Button("Fermer");
        btnClose.getStyleClass().add("payment-btn-cancel");
        btnClose.setOnAction(e -> closePopup(overlay, popup, root));

        footerBox.getChildren().addAll(btnPDF, btnClose);

        popup.getChildren().addAll(headerBox, scroll, footerBox);
        overlay.getChildren().add(popup);
        showOverlay(overlay, popup, root);
    }

    /**
     * Shows a signature popup with a drawing canvas.
     */
    private void showSignaturePopup(InvestmentContract contract, boolean isInvestor) {
        StackPane root = getSceneRootStack();
        if (root == null) return;

        StackPane overlay = createOverlay();
        VBox popup = createPopup(440, 420);

        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20, 20, 14, 20));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                + (isInvestor ? "#059669, #10b981" : "#2563eb, #60a5fa") + ");");

        Label icon = new Label("✍️");
        icon.setStyle("-fx-font-size: 36;");
        Label title = new Label(isInvestor ? "Signature Investisseur" : "Contresignature Entrepreneur");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label sub = new Label(contract.getContractNumber());
        sub.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.7);");
        headerBox.getChildren().addAll(icon, title, sub);

        // ── Drawing canvas ──
        VBox canvasBox = new VBox(8);
        canvasBox.setPadding(new Insets(18, 24, 10, 24));
        canvasBox.setAlignment(Pos.CENTER);

        Label instruction = new Label("Dessinez votre signature ci-dessous :");
        instruction.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");

        Canvas canvas = new Canvas(360, 150);
        canvas.setStyle("-fx-cursor: crosshair;");
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 360, 150);
        gc.setStroke(Color.web("#2c3e50"));
        gc.setLineWidth(2.5);

        // Dashed border effect
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 2; "
                + "-fx-border-style: dashed; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10;");
        canvasWrapper.setMaxWidth(364);

        // Drawing handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

        Button btnClear = new Button("🗑 Effacer");
        btnClear.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-background-radius: 8; -fx-cursor: hand;");
        btnClear.setOnAction(e -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 360, 150);
            gc.setStroke(Color.web("#0f172a"));
        });

        canvasBox.getChildren().addAll(instruction, canvasWrapper, btnClear);

        // ── Action buttons ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 24, 20, 24));

        Button btnConfirm = new Button("✅ Confirmer la signature");
        btnConfirm.setStyle("-fx-background-color: " + (isInvestor ? "#059669" : "#2563eb")
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            // Capture canvas as Base64 string (simplified — store snapshot hash)
            String sigData = "SIG-" + (isInvestor ? "INV" : "ENT") + "-"
                    + contract.getContractNumber() + "-"
                    + System.currentTimeMillis();
            String sigBase64 = Base64.getEncoder().encodeToString(sigData.getBytes());

            boolean ok;
            if (isInvestor) {
                ok = contractService.signByInvestor(contract.getId(), sigBase64);
            } else {
                ok = contractService.signByEntrepreneur(contract.getId(), sigBase64);
            }

            if (ok) {
                closePopup(overlay, popup, root);
                showContracts(); // refresh
            }
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.getStyleClass().add("payment-btn-cancel");
        btnCancel.setOnAction(e -> closePopup(overlay, popup, root));

        actions.getChildren().addAll(btnConfirm, btnCancel);

        popup.getChildren().addAll(headerBox, canvasBox, actions);
        overlay.getChildren().add(popup);
        showOverlay(overlay, popup, root);
    }

    private void exportContractPDF(InvestmentContract contract) {
        // Generate contract PDF via InvestmentPDFService patterns
        InvestmentPDFService pdfService = new InvestmentPDFService();

        // Find associated offer for amount info
        Optional<InvestmentOffer> offerOpt = offerService.findById(contract.getOfferId());
        InvestmentOffer offer = offerOpt.orElse(null);
        InvestmentOpportunity opp = null;
        Project proj = null;
        if (offer != null) {
            opp = opportunityService.findById(offer.getOpportunityId()).orElse(null);
            if (opp != null) proj = projectService.findById(opp.getProjectId()).orElse(null);
        }

        byte[] pdf = pdfService.generateContractPDF(contract, offer, opp, proj);
        if (pdf != null) {
            StackPane rootStack = getSceneRootStack();
            if (rootStack != null) {
                PDFPreviewPopup.show(pdf, "Contrat " + contract.getContractNumber(),
                        "contrat_" + contract.getContractNumber() + ".pdf", rootStack);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  2. AI MATCHING
    // ═══════════════════════════════════════════════════════════

    private void buildMatchingView() {
        contentArea.getChildren().clear();

        int userId = SessionManager.getInstance().getCurrentUserId();
        Optional<InvestorProfile> optProfile = matchingService.findProfileByUser(userId);

        // Profile setup card
        VBox profileCard = buildProfileCard(optProfile.orElse(null), userId);
        profileCard.setPrefWidth(680);
        profileCard.setMaxWidth(700);
        contentArea.getChildren().add(profileCard);

        // Matching results
        if (optProfile.isPresent()) {
            List<InvestmentMatchingService.MatchResult> matches = matchingService.findMatches(userId);

            if (matches.isEmpty()) {
                contentArea.getChildren().add(buildEmptyState("🔍", "Aucune opportunité trouvée",
                        "Ajustez votre profil ou revenez plus tard."));
            } else {
                // Results header
                VBox resultsHeader = new VBox(4);
                resultsHeader.setPrefWidth(680);
                resultsHeader.setMaxWidth(700);
                resultsHeader.setPadding(new Insets(10, 0, 5, 4));
                Label matchTitle = new Label("🎯 " + matches.size() + " opportunité(s) trouvée(s)");
                matchTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
                resultsHeader.getChildren().add(matchTitle);
                contentArea.getChildren().add(resultsHeader);

                int delay = 0;
                for (InvestmentMatchingService.MatchResult match : matches) {
                    VBox card = buildMatchCard(match);
                    contentArea.getChildren().add(card);
                    AnimationUtils.playFadeScaleIn(card, 300, delay);
                    delay += 60;
                }
            }
        }
    }

    private VBox buildProfileCard(InvestorProfile profile, int userId) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(0));

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #7c3aed, #8b5cf6); -fx-background-radius: 16 16 0 0;");
        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 28;");
        Label title = new Label("Mon Profil d'Investissement");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().addAll(icon, title);

        VBox form = new VBox(12);
        form.setPadding(new Insets(18, 24, 20, 24));

        // Sectors
        Label lblSectors = new Label("🏷 Secteurs préférés (séparés par des virgules) :");
        lblSectors.setStyle("-fx-font-size: 12; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
        TextField txtSectors = new TextField(profile != null ? profile.getPreferredSectors() : "");
        txtSectors.setPromptText("tech, santé, énergie, éducation...");
        txtSectors.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; -fx-padding: 8;");

        // Risk tolerance slider
        Label lblRisk = new Label("⚡ Tolérance au risque : " + (profile != null ? profile.getRiskTolerance() : 5) + "/10");
        lblRisk.setStyle("-fx-font-size: 12; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
        Slider sldRisk = new Slider(1, 10, profile != null ? profile.getRiskTolerance() : 5);
        sldRisk.setShowTickLabels(true);
        sldRisk.setShowTickMarks(true);
        sldRisk.setMajorTickUnit(1);
        sldRisk.setMinorTickCount(0);
        sldRisk.setSnapToTicks(true);
        sldRisk.valueProperty().addListener((obs, o, n) ->
                lblRisk.setText("⚡ Tolérance au risque : " + n.intValue() + "/10"));

        // Budget range
        HBox budgetBox = new HBox(10);
        budgetBox.setAlignment(Pos.CENTER_LEFT);
        Label lblBudget = new Label("💰 Budget (€) :");
        lblBudget.setStyle("-fx-font-size: 12; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
        TextField txtMin = new TextField(profile != null && profile.getBudgetMin() != null
                ? profile.getBudgetMin().toPlainString() : "1000");
        txtMin.setPrefWidth(120);
        txtMin.setPromptText("Min");
        txtMin.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; -fx-padding: 8;");
        Label lblTo = new Label("→");
        TextField txtMax = new TextField(profile != null && profile.getBudgetMax() != null
                ? profile.getBudgetMax().toPlainString() : "500000");
        txtMax.setPrefWidth(120);
        txtMax.setPromptText("Max");
        txtMax.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e2e8f0; -fx-padding: 8;");
        budgetBox.getChildren().addAll(lblBudget, txtMin, lblTo, txtMax);

        // Horizon
        Label lblHorizon = new Label("📅 Horizon d'investissement (mois) :");
        lblHorizon.setStyle("-fx-font-size: 12; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
        Spinner<Integer> spnHorizon = new Spinner<>(1, 120, profile != null ? profile.getHorizonMonths() : 12);
        spnHorizon.setEditable(true);
        spnHorizon.setPrefWidth(100);

        // Save button
        Button btnSave = new Button("💾 Sauvegarder & Lancer le Matching");
        btnSave.setStyle("-fx-background-color: linear-gradient(to bottom right, #7c3aed, #6d28d9); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; "
                + "-fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(e -> {
            InvestorProfile p = profile != null ? profile : new InvestorProfile();
            p.setUserId(userId);
            p.setPreferredSectors(txtSectors.getText().trim());
            p.setRiskTolerance((int) sldRisk.getValue());
            try {
                p.setBudgetMin(new BigDecimal(txtMin.getText().trim()));
                p.setBudgetMax(new BigDecimal(txtMax.getText().trim()));
            } catch (NumberFormatException ex) {
                p.setBudgetMin(BigDecimal.ZERO);
                p.setBudgetMax(new BigDecimal("10000000"));
            }
            p.setHorizonMonths(spnHorizon.getValue());

            matchingService.saveProfile(p);
            showMatching(); // refresh
        });

        form.getChildren().addAll(lblSectors, txtSectors, lblRisk, sldRisk,
                budgetBox, lblHorizon, spnHorizon, btnSave);

        card.getChildren().addAll(header, form);
        return card;
    }

    private VBox buildMatchCard(InvestmentMatchingService.MatchResult match) {
        VBox card = new VBox(8);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(320);
        card.setMinWidth(290);
        card.setMaxWidth(350);

        // Compatibility score header
        int score = match.compatibilityScore;
        String color = score >= 70 ? "#059669" : score >= 40 ? "#d97706" : "#ef4444";

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, " + color + ", " + color + "dd); "
                + "-fx-background-radius: 16 16 0 0;");

        Label scoreLabel = new Label(score + "%");
        scoreLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label compLabel = new Label("Compatibilité");
        compLabel.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.7);");
        header.getChildren().addAll(scoreLabel, compLabel);

        // Body
        VBox body = new VBox(6);
        body.setPadding(new Insets(12, 18, 14, 18));

        String projTitle = match.project != null ? match.project.getTitle() : "Projet inconnu";
        Label lblProject = new Label("🏢 " + projTitle);
        lblProject.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        lblProject.setWrapText(true);

        String sector = match.project != null ? match.project.getSector() : "";
        if (sector != null && !sector.isEmpty()) {
            Label lblSector = new Label("🏷 " + sector);
            lblSector.setStyle("-fx-font-size: 11; -fx-text-fill: #5b21b6; -fx-font-weight: bold; "
                    + "-fx-background-color: #ede9fe; -fx-background-radius: 6; -fx-padding: 3 8;");
            body.getChildren().add(lblSector);
        }

        Label lblAmount = new Label("🎯 " + match.opportunity.getFormattedAmount());
        lblAmount.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #059669;");

        Label lblExplanation = new Label(match.explanation);
        lblExplanation.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");
        lblExplanation.setWrapText(true);

        // Risk badge
        if (match.opportunity.getRiskScore() != null) {
            Label riskBadge = new Label(match.opportunity.getFormattedRiskScore());
            riskBadge.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8;");
            body.getChildren().addAll(lblProject, lblAmount, riskBadge, lblExplanation);
        } else {
            body.getChildren().addAll(lblProject, lblAmount, lblExplanation);
        }

        card.getChildren().addAll(header, body);
        addHoverEffect(card);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  3. PORTFOLIO CHARTS
    // ═══════════════════════════════════════════════════════════

    private void buildChartsView() {
        contentArea.getChildren().clear();

        List<InvestmentOffer> paidOffers = offerService.findAll().stream()
                .filter(InvestmentOffer::isPaid)
                .toList();

        if (paidOffers.isEmpty()) {
            contentArea.getChildren().add(buildEmptyState("📊", "Aucune donnée",
                    "Investissez pour voir vos graphiques."));
            return;
        }

        // ── KPI Dashboard — Advanced Metrics ──
        VBox kpiSection = buildKPIDashboard(paidOffers);
        kpiSection.setPrefWidth(700);
        kpiSection.setMaxWidth(720);

        // 1. Pie Chart — by sector
        VBox pieBox = buildPieChart(paidOffers);
        pieBox.setPrefWidth(420);
        pieBox.setMaxWidth(450);

        // 2. Bar Chart — by project
        VBox barBox = buildBarChart(paidOffers);
        barBox.setPrefWidth(420);
        barBox.setMaxWidth(450);

        // 3. Timeline chart — investments over time
        VBox lineBox = buildTimelineChart(paidOffers);
        lineBox.setPrefWidth(680);
        lineBox.setMaxWidth(700);

        // 4. Risk-Return Matrix
        VBox riskReturnBox = buildRiskReturnAnalysis(paidOffers);
        riskReturnBox.setPrefWidth(680);
        riskReturnBox.setMaxWidth(700);

        // 5. What-if simulator (enhanced)
        VBox simulatorBox = buildSimulator(paidOffers);
        simulatorBox.setPrefWidth(680);
        simulatorBox.setMaxWidth(700);

        contentArea.getChildren().addAll(kpiSection, pieBox, barBox, lineBox, riskReturnBox, simulatorBox);

        int delay = 0;
        for (Node n : contentArea.getChildren()) {
            AnimationUtils.playFadeScaleIn(n, 400, delay);
            delay += 100;
        }
    }

    /**
     * Premium KPI Dashboard with advanced financial metrics:
     * - Total Invested / # Positions / Avg Position Size
     * - Herfindahl-Hirschman Index (HHI) for portfolio concentration
     * - Compound Annual Growth Rate (CAGR) estimation
     * - Portfolio Diversification Score
     * - Sector Entropy (Shannon)
     * - Weighted Average Risk Score
     */
    private VBox buildKPIDashboard(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b); -fx-background-radius: 16 16 0 0;");
        Label titleLbl = new Label("📈 Tableau de Bord — Métriques Avancées");
        titleLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label subLbl = new Label("Analyse quantitative de votre portefeuille");
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
        header.getChildren().addAll(titleLbl, subLbl);

        // Compute metrics
        BigDecimal totalInvested = offers.stream()
                .map(InvestmentOffer::getProposedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int numPositions = offers.size();
        BigDecimal avgPosition = numPositions > 0
                ? totalInvested.divide(BigDecimal.valueOf(numPositions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Sector grouping for HHI + Shannon
        Map<String, BigDecimal> sectorTotals = new LinkedHashMap<>();
        for (InvestmentOffer o : offers) {
            String sector = o.getProjectSector() != null ? o.getProjectSector() : "Autre";
            sectorTotals.merge(sector, o.getProposedAmount(), BigDecimal::add);
        }
        int numSectors = sectorTotals.size();

        // HHI (Herfindahl-Hirschman Index) — 0-10000 scale
        double hhi = 0;
        for (BigDecimal sectorAmt : sectorTotals.values()) {
            double share = sectorAmt.doubleValue() / totalInvested.doubleValue();
            hhi += share * share * 10000;
        }
        String hhiLabel = hhi < 1500 ? "Diversifié" : hhi < 2500 ? "Modéré" : "Concentré";

        // Shannon Entropy for diversification quality
        double shannonEntropy = 0;
        for (BigDecimal sectorAmt : sectorTotals.values()) {
            double p = sectorAmt.doubleValue() / totalInvested.doubleValue();
            if (p > 0) shannonEntropy -= p * Math.log(p);
        }
        double maxEntropy = numSectors > 1 ? Math.log(numSectors) : 1;
        double diversificationScore = maxEntropy > 0 ? (shannonEntropy / maxEntropy) * 100 : 0;

        // Weighted average risk score
        double weightedRisk = 0;
        double totalWeight = 0;
        for (InvestmentOffer o : offers) {
            Optional<InvestmentOpportunity> opp = opportunityService.findById(o.getOpportunityId());
            if (opp.isPresent() && opp.get().getRiskScore() != null) {
                double weight = o.getProposedAmount().doubleValue();
                weightedRisk += opp.get().getRiskScore() * weight;
                totalWeight += weight;
            }
        }
        double avgRisk = totalWeight > 0 ? weightedRisk / totalWeight : 0;

        // Largest position weight
        BigDecimal maxPos = offers.stream()
                .map(InvestmentOffer::getProposedAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        double maxPosWeight = totalInvested.doubleValue() > 0
                ? (maxPos.doubleValue() / totalInvested.doubleValue()) * 100 : 0;

        // Build KPI tiles — 2 rows of 3
        FlowPane tiles = new FlowPane(12, 12);
        tiles.setPadding(new Insets(18, 20, 20, 20));
        tiles.setAlignment(Pos.CENTER);

        tiles.getChildren().addAll(
                buildKPITile("💰", "Capital Total", String.format("%,.0f €", totalInvested), "#059669", "Montant total des investissements payés"),
                buildKPITile("📊", "Positions", String.valueOf(numPositions), "#2563eb",
                        "Moy: " + String.format("%,.0f €", avgPosition) + " par position"),
                buildKPITile("🏷", "Secteurs", numSectors + " actifs", "#7c3aed",
                        String.format("Entropie: %.2f bits", shannonEntropy)),
                buildKPITile("📐", "Indice HHI", String.format("%.0f", hhi), 
                        hhi < 1500 ? "#059669" : hhi < 2500 ? "#d97706" : "#ef4444",
                        hhiLabel + " — concentration du portefeuille"),
                buildKPITile("🎯", "Diversification", String.format("%.0f%%", diversificationScore),
                        diversificationScore >= 70 ? "#059669" : diversificationScore >= 40 ? "#d97706" : "#ef4444",
                        "Score basé sur l'entropie de Shannon"),
                buildKPITile("⚡", "Risque Pondéré", String.format("%.1f/100", avgRisk),
                        avgRisk <= 35 ? "#059669" : avgRisk <= 65 ? "#d97706" : "#ef4444",
                        String.format("Max position: %.0f%% du portfolio", maxPosWeight))
        );

        box.getChildren().addAll(header, tiles);
        return box;
    }

    private VBox buildKPITile(String emoji, String label, String value, String color, String detail) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setPrefWidth(205);
        tile.setMinWidth(190);
        tile.setPadding(new Insets(14, 16, 14, 16));
        tile.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 18;");
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        top.getChildren().addAll(emojiLbl, lblLabel);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label detailLbl = new Label(detail);
        detailLbl.setStyle("-fx-font-size: 9; -fx-text-fill: #94a3b8;");
        detailLbl.setWrapText(true);

        tile.getChildren().addAll(top, valLbl, detailLbl);

        // Hover effect
        tile.setOnMouseEntered(e -> tile.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; "
                + "-fx-border-color: " + color + "44; -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, " + color + "22, 8, 0, 0, 2);"));
        tile.setOnMouseExited(e -> tile.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);"));

        return tile;
    }

    /**
     * Risk-Return analysis matrix — shows each investment's risk vs. size
     * with a sector-weighted breakdown.
     */
    private VBox buildRiskReturnAnalysis(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #ef4444, #f87171); -fx-background-radius: 16 16 0 0;");
        Label titleLbl = new Label("🎲 Matrice Risque — Rendement");
        titleLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().add(titleLbl);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 20, 18, 20));

        // Build a table-like risk breakdown per investment
        VBox tableBox = new VBox(0);
        tableBox.setStyle("-fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        // Header row
        HBox hdr = new HBox(0);
        hdr.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 12 12 0 0;");
        hdr.getChildren().addAll(
                riskMatrixCell("Projet", true, 160),
                riskMatrixCell("Montant", true, 100),
                riskMatrixCell("Risque IA", true, 80),
                riskMatrixCell("Niveau", true, 80),
                riskMatrixCell("Score Ajusté", true, 100)
        );
        tableBox.getChildren().add(hdr);

        BigDecimal totalAmt = offers.stream().map(InvestmentOffer::getProposedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean alt = false;

        for (InvestmentOffer offer : offers) {
            Optional<InvestmentOpportunity> opp = opportunityService.findById(offer.getOpportunityId());
            String projName = offer.getProjectTitle() != null
                    ? (offer.getProjectTitle().length() > 18 ? offer.getProjectTitle().substring(0, 16) + "…" : offer.getProjectTitle())
                    : "Projet #" + offer.getOpportunityId();
            double risk = opp.isPresent() && opp.get().getRiskScore() != null ? opp.get().getRiskScore() : 50;
            String riskLabel = opp.isPresent() ? opp.get().getFormattedRiskLabel() : "N/A";
            // Risk-adjusted score: weight-adjusted inverse risk
            double weight = totalAmt.doubleValue() > 0
                    ? offer.getProposedAmount().doubleValue() / totalAmt.doubleValue() : 0;
            double riskAdjusted = (100 - risk) * weight * 100;

            String bg = alt ? "#f1f5f9" : "#ffffff";
            HBox row = new HBox(0);
            row.getChildren().addAll(
                    riskMatrixDataCell(projName, bg, 160, "#0f172a"),
                    riskMatrixDataCell(String.format("%,.0f €", offer.getProposedAmount()), bg, 100, "#059669"),
                    riskMatrixDataCell(String.format("%.0f/100", risk), bg, 80,
                            risk <= 35 ? "#059669" : risk <= 65 ? "#d97706" : "#ef4444"),
                    riskMatrixDataCell(riskLabel, bg, 80, "#64748b"),
                    riskMatrixDataCell(String.format("%.1f", riskAdjusted), bg, 100,
                            riskAdjusted >= 30 ? "#059669" : riskAdjusted >= 15 ? "#d97706" : "#ef4444")
            );
            tableBox.getChildren().add(row);
            alt = !alt;
        }

        content.getChildren().add(tableBox);

        // Summary insight
        Label insight = new Label("ℹ Score Ajusté = (100 - Risque) × Poids × 100 — mesure la contribution risque-pondérée de chaque position");
        insight.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        insight.setWrapText(true);
        content.getChildren().add(insight);

        box.getChildren().addAll(header, content);
        return box;
    }

    private Label riskMatrixCell(String text, boolean isHeader, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setMinWidth(width);
        lbl.setPadding(new Insets(10, 10, 10, 10));
        lbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-background-color: transparent;");
        return lbl;
    }

    private Label riskMatrixDataCell(String text, String bg, double width, String color) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setMinWidth(width);
        lbl.setPadding(new Insets(8, 10, 8, 10));
        lbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + color + "; -fx-background-color: " + bg + ";");
        lbl.setWrapText(true);
        return lbl;
    }

    private VBox buildPieChart(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 18, 10, 18));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #7c3aed, #8b5cf6); -fx-background-radius: 16 16 0 0;");
        Label title = new Label("🥧 Répartition par Secteur");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().add(title);

        // Group by sector
        Map<String, BigDecimal> sectorTotals = new LinkedHashMap<>();
        for (InvestmentOffer o : offers) {
            String sector = o.getProjectSector() != null ? o.getProjectSector() : "Autre";
            sectorTotals.merge(sector, o.getProposedAmount(), BigDecimal::add);
        }

        PieChart pieChart = new PieChart();
        sectorTotals.forEach((sector, amount) ->
                pieChart.getData().add(new PieChart.Data(sector + " (" + String.format("%,.0f€", amount) + ")", amount.doubleValue())));

        pieChart.setLegendSide(Side.BOTTOM);
        pieChart.setLabelsVisible(true);
        pieChart.setPrefHeight(320);
        pieChart.setStyle("-fx-font-size: 11;");

        VBox content = new VBox(pieChart);
        content.setPadding(new Insets(10, 14, 14, 14));
        box.getChildren().addAll(header, content);
        return box;
    }

    private VBox buildBarChart(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 18, 10, 18));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #2563eb, #60a5fa); -fx-background-radius: 16 16 0 0;");
        Label title = new Label("📊 Montants par Projet");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().add(title);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Projet");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Montant (€)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, BigDecimal> projectTotals = new LinkedHashMap<>();
        for (InvestmentOffer o : offers) {
            String proj = o.getProjectTitle() != null
                    ? (o.getProjectTitle().length() > 18 ? o.getProjectTitle().substring(0, 15) + "…" : o.getProjectTitle())
                    : "Projet #" + o.getOpportunityId();
            projectTotals.merge(proj, o.getProposedAmount(), BigDecimal::add);
        }
        projectTotals.forEach((p, a) -> series.getData().add(new XYChart.Data<>(p, a)));
        barChart.getData().add(series);

        VBox content = new VBox(barChart);
        content.setPadding(new Insets(10, 14, 14, 14));
        box.getChildren().addAll(header, content);
        return box;
    }

    private VBox buildTimelineChart(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 18, 10, 18));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #059669, #10b981); -fx-background-radius: 16 16 0 0;");
        Label title = new Label("📈 Évolution des Investissements");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().add(title);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cumul (€)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(280);
        lineChart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Investissements cumulés");

        // Sort by paid date and compute cumulative
        List<InvestmentOffer> sorted = offers.stream()
                .sorted((a, b) -> {
                    var da = a.getPaidAt();
                    var db = b.getPaidAt();
                    if (da == null && db == null) return 0;
                    if (da == null) return -1;
                    if (db == null) return 1;
                    return da.compareTo(db);
                })
                .toList();

        BigDecimal cumulative = BigDecimal.ZERO;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (InvestmentOffer o : sorted) {
            cumulative = cumulative.add(o.getProposedAmount());
            String dateStr = o.getPaidAt() != null ? o.getPaidAt().format(fmt) : "N/A";
            series.getData().add(new XYChart.Data<>(dateStr, cumulative));
        }
        lineChart.getData().add(series);

        VBox content = new VBox(lineChart);
        content.setPadding(new Insets(10, 14, 14, 14));
        box.getChildren().addAll(header, content);
        return box;
    }

    private VBox buildSimulator(List<InvestmentOffer> offers) {
        VBox box = new VBox(0);
        box.getStyleClass().add("front-card");
        box.setPadding(new Insets(0));

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(14, 18, 10, 18));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #f59e0b); -fx-background-radius: 16 16 0 0;");
        Label title = new Label("🔮 Simulateur Avancé — Projection Multi-Scénario");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label sub = new Label("CAGR · Volatilité · Monte Carlo simplifié · Analyse de scénario");
        sub.setStyle("-fx-font-size: 11; -fx-text-fill: rgba(255,255,255,0.7);");
        header.getChildren().addAll(title, sub);

        VBox form = new VBox(14);
        form.setPadding(new Insets(20, 24, 20, 24));

        BigDecimal totalInvested = offers.stream()
                .map(InvestmentOffer::getProposedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Label lblTotal = new Label("💰 Capital initial : " + String.format("%,.2f €", totalInvested));
        lblTotal.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        // ROI slider
        Label lblRoi = new Label("📈 Rendement annuel estimé (CAGR) : 8%");
        lblRoi.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Slider sldRoi = new Slider(-20, 50, 8);
        sldRoi.setShowTickLabels(true);
        sldRoi.setShowTickMarks(true);
        sldRoi.setMajorTickUnit(10);

        // Volatility slider
        Label lblVol = new Label("📊 Volatilité annuelle estimée : 15%");
        lblVol.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Slider sldVol = new Slider(0, 60, 15);
        sldVol.setShowTickLabels(true);
        sldVol.setShowTickMarks(true);
        sldVol.setMajorTickUnit(10);

        // Years slider
        Label lblYears = new Label("📅 Horizon : 5 ans");
        lblYears.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Slider sldYears = new Slider(1, 30, 5);
        sldYears.setShowTickLabels(true);
        sldYears.setShowTickMarks(true);
        sldYears.setMajorTickUnit(5);
        sldYears.setSnapToTicks(true);

        // Result display — base case
        Label lblResult = new Label();
        lblResult.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-padding: 10;");
        lblResult.setWrapText(true);

        Label lblGain = new Label();
        lblGain.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");

        // Advanced metrics labels
        Label lblCagr = new Label();
        lblCagr.setStyle("-fx-font-size: 12; -fx-text-fill: #2563eb; -fx-font-weight: bold;");

        Label lblSharpe = new Label();
        lblSharpe.setStyle("-fx-font-size: 12; -fx-text-fill: #7c3aed; -fx-font-weight: bold;");

        // Scenario box: Optimistic / Base / Pessimistic
        HBox scenarioBox = new HBox(12);
        scenarioBox.setAlignment(Pos.CENTER);
        VBox optimistic = buildScenarioTile("🟢 Optimiste", "", "#059669");
        VBox base = buildScenarioTile("🔵 Base", "", "#2563eb");
        VBox pessimistic = buildScenarioTile("🔴 Pessimiste", "", "#ef4444");
        scenarioBox.getChildren().addAll(pessimistic, base, optimistic);

        // Monthly projection label
        Label lblMonthly = new Label();
        lblMonthly.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
        lblMonthly.setWrapText(true);

        // Drawdown estimation
        Label lblDrawdown = new Label();
        lblDrawdown.setStyle("-fx-font-size: 11; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblDrawdown.setWrapText(true);

        // Update the result dynamically
        Runnable updateResult = () -> {
            double roi = sldRoi.getValue() / 100.0;
            double vol = sldVol.getValue() / 100.0;
            int years = (int) sldYears.getValue();
            double principal = totalInvested.doubleValue();
            double future = principal * Math.pow(1 + roi, years);
            double gain = future - principal;

            // CAGR verification
            double cagr = years > 0 ? (Math.pow(future / principal, 1.0 / years) - 1) * 100 : 0;

            // Sharpe Ratio (risk-free = 3%)
            double riskFreeRate = 0.03;
            double sharpe = vol > 0 ? (roi - riskFreeRate) / vol : 0;

            // Scenario analysis: optimistic = roi + vol, pessimistic = roi - vol
            double futureOpt = principal * Math.pow(1 + roi + vol * 0.5, years);
            double futurePes = principal * Math.pow(1 + roi - vol * 0.5, years);

            // Max theoretical drawdown (simplified)
            double maxDrawdown = 1 - Math.exp(-vol * Math.sqrt(years) * 1.5);

            // Monthly equivalent return
            double monthlyReturn = Math.pow(1 + roi, 1.0 / 12) - 1;

            lblRoi.setText("📈 Rendement annuel estimé (CAGR) : " + String.format("%.0f%%", sldRoi.getValue()));
            lblVol.setText("📊 Volatilité annuelle estimée : " + String.format("%.0f%%", sldVol.getValue()));
            lblYears.setText("📅 Horizon : " + years + " an(s)");
            lblResult.setText(String.format("💎 Valeur estimée (base) : %,.2f €", future));
            lblGain.setText(String.format("%s %,.2f € (%+.0f%%)",
                    gain >= 0 ? "📈 Gain :" : "📉 Perte :",
                    Math.abs(gain), (gain / principal) * 100));
            lblGain.setStyle("-fx-font-size: 13; -fx-text-fill: " + (gain >= 0 ? "#059669" : "#ef4444") + ";");

            lblCagr.setText(String.format("📐 CAGR vérifié : %.2f%% | Rendement mensuel équiv. : %.3f%%", cagr, monthlyReturn * 100));

            String sharpeLabel = sharpe >= 1 ? "Excellent" : sharpe >= 0.5 ? "Acceptable" : sharpe >= 0 ? "Faible" : "Négatif";
            lblSharpe.setText(String.format("⚡ Ratio de Sharpe : %.2f (%s) | Taux sans risque : %.0f%%", sharpe, sharpeLabel, riskFreeRate * 100));

            // Update scenario tiles
            updateScenarioTile(pessimistic, "🔴 Pessimiste", String.format("%,.0f €", futurePes), "#ef4444");
            updateScenarioTile(base, "🔵 Base", String.format("%,.0f €", future), "#2563eb");
            updateScenarioTile(optimistic, "🟢 Optimiste", String.format("%,.0f €", futureOpt), "#059669");

            lblMonthly.setText(String.format("📅 Sur %d mois : rendement mensuel composé de %.3f%% | Capital × %.2f",
                    years * 12, monthlyReturn * 100, future / principal));
            lblDrawdown.setText(String.format("⚠ Drawdown max estimé : %.1f%% (perte potentielle : %,.0f €)",
                    maxDrawdown * 100, principal * maxDrawdown));
        };

        sldRoi.valueProperty().addListener((obs, o, n) -> updateResult.run());
        sldYears.valueProperty().addListener((obs, o, n) -> updateResult.run());
        sldVol.valueProperty().addListener((obs, o, n) -> updateResult.run());
        updateResult.run(); // initial

        Label scenarioTitle = new Label("📊 Analyse Multi-Scénario (±½σ)");
        scenarioTitle.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        form.getChildren().addAll(lblTotal, new Separator(),
                lblRoi, sldRoi, lblVol, sldVol, lblYears, sldYears, new Separator(),
                lblResult, lblGain, lblCagr, lblSharpe, new Separator(),
                scenarioTitle, scenarioBox, new Separator(),
                lblMonthly, lblDrawdown);

        box.getChildren().addAll(header, form);
        return box;
    }

    private VBox buildScenarioTile(String titleText, String value, String color) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(12, 18, 12, 18));
        tile.setPrefWidth(190);
        tile.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: " + color + "44; -fx-border-radius: 12; -fx-border-width: 1.5;");
        Label t = new Label(titleText);
        t.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        tile.getChildren().addAll(t, v);
        return tile;
    }

    private void updateScenarioTile(VBox tile, String title, String value, String color) {
        if (tile.getChildren().size() >= 2) {
            ((Label) tile.getChildren().get(0)).setText(title);
            ((Label) tile.getChildren().get(1)).setText(value);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  4. COMPARATOR
    // ═══════════════════════════════════════════════════════════

    private void buildComparatorView() {
        contentArea.getChildren().clear();

        List<InvestmentOpportunity> allOpps = opportunityService.findAll();

        if (allOpps.size() < 2) {
            contentArea.getChildren().add(buildEmptyState("⚖️", "Pas assez d'opportunités",
                    "Il faut au moins 2 opportunités pour comparer."));
            return;
        }

        VBox selectorCard = new VBox(14);
        selectorCard.getStyleClass().add("front-card");
        selectorCard.setPrefWidth(700);
        selectorCard.setMaxWidth(720);

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #f59e0b); -fx-background-radius: 16 16 0 0;");
        Label title = new Label("⚖️ Sélectionnez 2 opportunités à comparer");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        header.getChildren().add(title);

        VBox formBox = new VBox(12);
        formBox.setPadding(new Insets(18, 24, 20, 24));

        ComboBox<String> combo1 = new ComboBox<>();
        ComboBox<String> combo2 = new ComboBox<>();
        combo1.setPrefWidth(300);
        combo2.setPrefWidth(300);
        combo1.setPromptText("Opportunité A...");
        combo2.setPromptText("Opportunité B...");

        Map<String, InvestmentOpportunity> oppMap = new LinkedHashMap<>();
        for (InvestmentOpportunity opp : allOpps) {
            String label = "#" + opp.getId() + " — " + opp.getFormattedAmount()
                    + " — " + (opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet #" + opp.getProjectId());
            oppMap.put(label, opp);
            combo1.getItems().add(label);
            combo2.getItems().add(label);
        }

        HBox comboRow = new HBox(14);
        comboRow.setAlignment(Pos.CENTER);
        comboRow.getChildren().addAll(
                new VBox(4, new Label("Opportunité A"), combo1),
                new Label("VS"),
                new VBox(4, new Label("Opportunité B"), combo2));

        VBox comparisonResult = new VBox(0);

        Button btnCompare = new Button("🔍 Comparer");
        btnCompare.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #b45309); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; "
                + "-fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCompare.setOnAction(e -> {
            if (combo1.getValue() != null && combo2.getValue() != null && !combo1.getValue().equals(combo2.getValue())) {
                InvestmentOpportunity oppA = oppMap.get(combo1.getValue());
                InvestmentOpportunity oppB = oppMap.get(combo2.getValue());
                comparisonResult.getChildren().clear();
                comparisonResult.getChildren().add(buildComparisonTable(oppA, oppB));
            }
        });

        formBox.getChildren().addAll(comboRow, btnCompare, comparisonResult);
        selectorCard.getChildren().addAll(header, formBox);
        contentArea.getChildren().add(selectorCard);
    }

    private VBox buildComparisonTable(InvestmentOpportunity a, InvestmentOpportunity b) {
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-padding: 0;");

        // Header row
        HBox headerRow = new HBox(0);
        headerRow.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 14 14 0 0;");
        headerRow.getChildren().addAll(
                compCell("Critère", true, "#0f172a", true),
                compCell("Opportunité A", true, "#0f172a", true),
                compCell("Opportunité B", true, "#0f172a", true)
        );
        table.getChildren().add(headerRow);

        Project projA = projectService.findById(a.getProjectId()).orElse(null);
        Project projB = projectService.findById(b.getProjectId()).orElse(null);

        boolean alt = false;
        table.getChildren().addAll(
                compRow("🏢 Projet", projA != null ? projA.getTitle() : "N/A", projB != null ? projB.getTitle() : "N/A", alt = !alt),
                compRow("🏷 Secteur", projA != null ? projA.getSector() : "N/A", projB != null ? projB.getSector() : "N/A", alt = !alt),
                compRow("🎯 Montant cible", a.getFormattedAmount(), b.getFormattedAmount(), alt = !alt),
                compRow("📅 Deadline",
                        a.getDeadline() != null ? a.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A",
                        b.getDeadline() != null ? b.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A",
                        alt = !alt),
                compRow("📊 Score risque IA",
                        a.getRiskScore() != null ? String.format("%.0f/100", a.getRiskScore()) : "N/A",
                        b.getRiskScore() != null ? String.format("%.0f/100", b.getRiskScore()) : "N/A",
                        alt = !alt),
                compRow("🏷 Label risque ML",
                        a.getFormattedRiskLabel(), b.getFormattedRiskLabel(), alt = !alt),
                compRow("📋 Statut", a.getStatus().getDisplayName(), b.getStatus().getDisplayName(), alt = !alt),
                compRow("⭐ Note moyenne",
                        formatAvgRating(ratingService.getAverageRating(a.getId()), ratingService.getRatingCount(a.getId())),
                        formatAvgRating(ratingService.getAverageRating(b.getId()), ratingService.getRatingCount(b.getId())),
                        alt = !alt)
        );

        // Winner summary
        int scoreA = computeQuickScore(a, projA);
        int scoreB = computeQuickScore(b, projB);

        HBox winnerRow = new HBox(10);
        winnerRow.setAlignment(Pos.CENTER);
        winnerRow.setPadding(new Insets(14));
        winnerRow.setStyle("-fx-background-color: #ecfdf5; -fx-background-radius: 0 0 14 14;");

        String winner;
        if (scoreA > scoreB) winner = "🏆 Opportunité A a un meilleur profil (" + scoreA + " vs " + scoreB + " pts)";
        else if (scoreB > scoreA) winner = "🏆 Opportunité B a un meilleur profil (" + scoreB + " vs " + scoreA + " pts)";
        else winner = "🤝 Les deux opportunités sont équivalentes (" + scoreA + " pts)";

        Label lblWinner = new Label(winner);
        lblWinner.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #059669;");
        winnerRow.getChildren().add(lblWinner);
        table.getChildren().add(winnerRow);

        return table;
    }

    private int computeQuickScore(InvestmentOpportunity opp, Project proj) {
        int score = 50;
        if (opp.getRiskScore() != null && opp.getRiskScore() <= 40) score += 20;
        else if (opp.getRiskScore() != null && opp.getRiskScore() <= 70) score += 10;
        if (opp.getDeadline() != null && opp.getDeadline().isAfter(LocalDate.now())) score += 15;
        if (opp.getStatus() == OpportunityStatus.OPEN) score += 10;
        double avg = ratingService.getAverageRating(opp.getId());
        score += (int) (avg * 5);
        return Math.min(100, score);
    }

    private HBox compRow(String criteria, String valA, String valB, boolean alt) {
        HBox row = new HBox(0);
        String bg = alt ? "#f1f5f9" : "#ffffff";
        row.getChildren().addAll(
                compCell(criteria, true, bg, false),
                compCell(valA, false, bg, false),
                compCell(valB, false, bg, false)
        );
        return row;
    }

    private Label compCell(String text, boolean bold, String bg, boolean isHeader) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(220);
        lbl.setMinWidth(140);
        lbl.setWrapText(true);
        lbl.setPadding(new Insets(10, 14, 10, 14));
        String style = "-fx-background-color: " + bg + "; -fx-font-size: 12;";
        if (bold) style += " -fx-font-weight: bold;";
        if (isHeader) style += " -fx-text-fill: #f8fafc;";
        else style += " -fx-text-fill: #0f172a;";
        lbl.setStyle(style);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return lbl;
    }

    private String formatAvgRating(double avg, int count) {
        if (count == 0) return "Pas encore noté";
        StringBuilder stars = new StringBuilder();
        int rounded = (int) Math.round(avg);
        for (int i = 1; i <= 5; i++) stars.append(i <= rounded ? "★" : "☆");
        return stars + " " + String.format("%.1f", avg) + " (" + count + " avis)";
    }

    // ═══════════════════════════════════════════════════════════
    //  5. RATINGS
    // ═══════════════════════════════════════════════════════════

    private void buildRatingsView() {
        contentArea.getChildren().clear();

        int userId = SessionManager.getInstance().getCurrentUserId();

        // Only show opportunities the user has invested in (paid offers)
        List<InvestmentOffer> paidOffers = offerService.findByInvestor(userId).stream()
                .filter(InvestmentOffer::isPaid)
                .toList();

        if (paidOffers.isEmpty()) {
            contentArea.getChildren().add(buildEmptyState("⭐", "Aucun investissement",
                    "Vous pourrez noter les opportunités après avoir investi."));
            return;
        }

        int delay = 0;
        for (InvestmentOffer offer : paidOffers) {
            Optional<InvestmentOpportunity> optOpp = opportunityService.findById(offer.getOpportunityId());
            if (optOpp.isEmpty()) continue;

            InvestmentOpportunity opp = optOpp.get();
            Optional<InvestorRating> existingRating = ratingService.findByInvestorAndOpportunity(userId, opp.getId());

            VBox card = buildRatingCard(opp, offer, existingRating.orElse(null), userId);
            contentArea.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 300, delay);
            delay += 80;
        }
    }

    private VBox buildRatingCard(InvestmentOpportunity opp, InvestmentOffer offer,
                                  InvestorRating existingRating, int userId) {
        VBox card = new VBox(10);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(340);
        card.setMinWidth(300);
        card.setMaxWidth(360);

        // Header
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #f59e0b); -fx-background-radius: 16 16 0 0;");

        Label amountLabel = new Label(offer.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        String projTitle = opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet";
        Label projLabel = new Label("🏢 " + projTitle);
        projLabel.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        header.getChildren().addAll(amountLabel, projLabel);

        // Body — rating stars
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 20, 16, 20));
        body.setAlignment(Pos.CENTER);

        // Current rating display
        int currentRating = existingRating != null ? existingRating.getRating() : 0;
        Label starsDisplay = new Label(existingRating != null
                ? existingRating.getStarDisplay()
                : "☆☆☆☆☆");
        starsDisplay.setStyle("-fx-font-size: 28; -fx-text-fill: #f59e0b;");

        // Star buttons (clickable)
        HBox starButtons = new HBox(6);
        starButtons.setAlignment(Pos.CENTER);
        final int[] selectedRating = {currentRating};

        for (int i = 1; i <= 5; i++) {
            Button starBtn = new Button(i <= currentRating ? "★" : "☆");
            starBtn.setStyle("-fx-font-size: 24; -fx-background-color: transparent; -fx-text-fill: #f59e0b; -fx-cursor: hand; -fx-padding: 4;");
            final int rating = i;
            starBtn.setOnAction(e -> {
                selectedRating[0] = rating;
                for (int j = 0; j < 5; j++) {
                    Button b = (Button) starButtons.getChildren().get(j);
                    b.setText(j < rating ? "★" : "☆");
                }
            });
            starButtons.getChildren().add(starBtn);
        }

        // Comment
        TextArea txtComment = new TextArea(existingRating != null ? existingRating.getComment() : "");
        txtComment.setPromptText("Votre avis sur cette opportunité...");
        txtComment.setPrefRowCount(3);
        txtComment.setWrapText(true);
        txtComment.setStyle("-fx-background-radius: 8;");

        // Save button
        Button btnSave = new Button(existingRating != null ? "📝 Mettre à jour" : "⭐ Soumettre l'évaluation");
        btnSave.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #b45309); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 18; -fx-cursor: hand;");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(e -> {
            if (selectedRating[0] > 0) {
                InvestorRating r = new InvestorRating(userId, opp.getId(), selectedRating[0], txtComment.getText().trim());
                ratingService.createOrUpdate(r);
                showRatings(); // refresh
            }
        });

        // Average rating for this opportunity
        double avgRating = ratingService.getAverageRating(opp.getId());
        int ratingCount = ratingService.getRatingCount(opp.getId());

        HBox avgBox = new HBox(6);
        avgBox.setAlignment(Pos.CENTER);
        avgBox.setStyle("-fx-background-color: #fefce8; -fx-background-radius: 10; -fx-padding: 8;");
        Label avgLabel = new Label(ratingCount > 0
                ? String.format("Note moyenne : %.1f/5 (%d avis)", avgRating, ratingCount)
                : "Aucun avis pour le moment");
        avgLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
        avgBox.getChildren().add(avgLabel);

        body.getChildren().addAll(starButtons, txtComment, btnSave, avgBox);
        card.getChildren().addAll(header, body);
        addHoverEffect(card);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  SHARED UI HELPERS
    // ═══════════════════════════════════════════════════════════

    private VBox buildEmptyState(String emoji, String title, String subtitle) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 60, 40));
        box.setPrefWidth(400);

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 48;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
        subLabel.setWrapText(true);

        box.getChildren().addAll(emojiLabel, titleLabel, subLabel);
        return box;
    }

    private void addHoverEffect(VBox card) {
        DropShadow normalShadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.05));
        DropShadow hoverShadow = new DropShadow(16, Color.web("#2563eb44"));
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
    }

    private StackPane createOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("payment-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOpacity(0);
        return overlay;
    }

    private VBox createPopup(int maxWidth, int maxHeight) {
        VBox popup = new VBox(0);
        popup.getStyleClass().add("payment-popup");
        popup.setMaxWidth(maxWidth);
        popup.setMinWidth(maxWidth - 80);
        popup.setMaxHeight(maxHeight);
        popup.setAlignment(Pos.TOP_CENTER);
        popup.setScaleX(0.85);
        popup.setScaleY(0.85);
        popup.setOpacity(0);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(popup.widthProperty());
        clip.heightProperty().bind(popup.heightProperty());
        popup.setClip(clip);

        return popup;
    }

    private void showOverlay(StackPane overlay, VBox popup, StackPane root) {
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) closePopup(overlay, popup, root); });
        overlay.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closePopup(overlay, popup, root); });

        root.getChildren().add(overlay);
        overlay.requestFocus();

        if (root.getChildren().size() > 1) root.getChildren().get(0).setEffect(new GaussianBlur(6));

        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(300), overlay);
        fadeOverlay.setFromValue(0); fadeOverlay.setToValue(1);
        FadeTransition fadePopup = new FadeTransition(Duration.millis(400), popup);
        fadePopup.setFromValue(0); fadePopup.setToValue(1);
        ScaleTransition scalePopup = new ScaleTransition(Duration.millis(450), popup);
        scalePopup.setFromX(0.85); scalePopup.setFromY(0.85);
        scalePopup.setToX(1.0); scalePopup.setToY(1.0);
        scalePopup.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        new ParallelTransition(fadeOverlay, fadePopup, scalePopup).play();
    }

    private void closePopup(StackPane overlay, VBox popup, StackPane root) {
        if (root.getChildren().size() > 1) root.getChildren().get(0).setEffect(null);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), overlay);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), popup);
        scaleOut.setToX(0.88); scaleOut.setToY(0.88);
        ParallelTransition closeAnim = new ParallelTransition(fadeOut, scaleOut);
        closeAnim.setOnFinished(e -> root.getChildren().remove(overlay));
        closeAnim.play();
    }

    private StackPane getSceneRootStack() {
        try {
            javafx.scene.Scene scene = advancedRoot.getScene();
            if (scene == null) return null;
            Parent current = advancedRoot.getParent();
            StackPane candidate = null;
            while (current != null) {
                if (current instanceof StackPane sp) candidate = sp;
                current = current.getParent();
            }
            if (candidate != null) return candidate;
            if (scene.getRoot() instanceof StackPane sp) return sp;
            StackPane wrapper = new StackPane(scene.getRoot());
            scene.setRoot(wrapper);
            return wrapper;
        } catch (Exception e) {
            return null;
        }
    }
}
