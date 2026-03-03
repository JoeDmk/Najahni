package controllers.investissement;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import models.User;
import models.investissement.Contract;
import models.investissement.Contract.ContractStatus;
import services.SessionService;
import services.investissement.ContractService;
import util.AnimationUtils;
import util.PDFPreviewPopup;
import util.Type;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controller for the Contracts view.
 * Shows all contracts for the current user (investor or entrepreneur).
 * Allows signing contracts with SHA-256 and viewing/generating PDF contracts.
 */
public class FrontContractsController {

    private static final Logger LOG = Logger.getLogger(FrontContractsController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    @javafx.fxml.FXML private StackPane rootStack;
    @javafx.fxml.FXML private VBox contractsContainer;
    @javafx.fxml.FXML private Label lblTitle;
    @javafx.fxml.FXML private Label lblSubtitle;

    private final ContractService contractService = new ContractService();
    private User currentUser;
    private boolean isInvestor;

    @javafx.fxml.FXML
    public void initialize() {
        currentUser = SessionService.getInstance().getCurrentUser();
        if (currentUser == null) return;
        isInvestor = currentUser.getRole() == Type.INVESTISSEUR;

        // Ensure contracts exist for all paid offers
        contractService.ensureContractsForPaidOffers(currentUser.getId());

        loadContracts();
    }

    private void loadContracts() {
        contractsContainer.getChildren().clear();

        List<Contract> contracts = contractService.findByUser(currentUser.getId());

        if (contracts.isEmpty()) {
            VBox emptyState = buildEmptyState();
            contractsContainer.getChildren().add(emptyState);
            return;
        }

        lblSubtitle.setText(contracts.size() + " contrat" + (contracts.size() > 1 ? "s" : "") + " trouvé" + (contracts.size() > 1 ? "s" : ""));

        for (int i = 0; i < contracts.size(); i++) {
            VBox card = createContractCard(contracts.get(i));
            contractsContainer.getChildren().add(card);
            AnimationUtils.playFadeScaleIn(card, 350, i * 80);
        }
    }

    private VBox createContractCard(Contract contract) {
        VBox card = new VBox(0);
        card.getStyleClass().add("contract-card");
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 4); "
                + "-fx-border-color: #e9ecef; -fx-border-radius: 16; -fx-border-width: 1;");

        // ── Card Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 16 16 0 0;");

        Label icon = new Label("📜");
        icon.setFont(javafx.scene.text.Font.font(24));

        VBox headerInfo = new VBox(2);
        Label titleLbl = new Label(contract.getProjectTitle() != null ? contract.getProjectTitle() : "Contrat #" + contract.getId());
        titleLbl.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.WHITE);
        Label subtitleLbl = new Label("Contrat #" + contract.getId() + " — Offre #" + contract.getOfferId());
        subtitleLbl.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
        subtitleLbl.setTextFill(Color.web("#e0d4ff"));
        headerInfo.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(contract.getStatusDisplayName());
        statusBadge.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        String badgeColor = switch (contract.getStatus()) {
            case FULLY_SIGNED -> "#27ae60";
            case INVESTOR_SIGNED, ENTREPRENEUR_SIGNED -> "#f39c12";
            case PENDING -> "#95a5a6";
            case CANCELLED -> "#e74c3c";
        };
        statusBadge.setStyle("-fx-background-color: " + badgeColor + "22; -fx-text-fill: " + badgeColor + "; "
                + "-fx-background-radius: 10; -fx-padding: 4 12;");

        header.getChildren().addAll(icon, headerInfo, spacer, statusBadge);

        // ── Card Body ──
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 20, 12, 20));

        // Details grid
        HBox detailsRow1 = new HBox(20);
        detailsRow1.setAlignment(Pos.CENTER_LEFT);
        detailsRow1.getChildren().addAll(
                buildDetailItem("👤 Investisseur", contract.getInvestorName() != null ? contract.getInvestorName() : "ID #" + contract.getInvestorId()),
                buildDetailItem("🏢 Entrepreneur", contract.getEntrepreneurName() != null ? contract.getEntrepreneurName() : "ID #" + contract.getEntrepreneurId())
        );

        HBox detailsRow2 = new HBox(20);
        detailsRow2.setAlignment(Pos.CENTER_LEFT);
        detailsRow2.getChildren().addAll(
                buildDetailItem("💰 Montant", contract.getProposedAmount() != null ? contract.getProposedAmount() : "N/A"),
                buildDetailItem("🏷️ Secteur", contract.getProjectSector() != null ? contract.getProjectSector() : "N/A")
        );

        body.getChildren().addAll(detailsRow1, detailsRow2);

        // ── Signature Status ──
        HBox signaturesRow = new HBox(12);
        signaturesRow.setPadding(new Insets(8, 0, 0, 0));
        signaturesRow.setAlignment(Pos.CENTER_LEFT);

        VBox investorSig = buildSignatureStatus("Investisseur", contract.isInvestorSigned(),
                contract.getInvestorSignature(), contract.getInvestorSignedAt());
        VBox entrepreneurSig = buildSignatureStatus("Entrepreneur", contract.isEntrepreneurSigned(),
                contract.getEntrepreneurSignature(), contract.getEntrepreneurSignedAt());

        HBox.setHgrow(investorSig, Priority.ALWAYS);
        HBox.setHgrow(entrepreneurSig, Priority.ALWAYS);
        signaturesRow.getChildren().addAll(investorSig, entrepreneurSig);
        body.getChildren().add(signaturesRow);

        // ── Card Footer ──
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 1 0 0 0;");

        // Sign button (if not yet signed by current user)
        boolean canSign = canCurrentUserSign(contract);
        if (canSign) {
            Button btnSign = new Button("✍️ Signer le contrat");
            btnSign.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.BOLD, 13));
            btnSign.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white; "
                    + "-fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;");
            btnSign.setOnMouseEntered(e -> btnSign.setStyle("-fx-background-color: linear-gradient(to right, #5a6fd6, #6a4199); "
                    + "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;"));
            btnSign.setOnMouseExited(e -> btnSign.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); "
                    + "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10 24; -fx-cursor: hand;"));
            btnSign.setOnAction(e -> handleSign(contract, card));
            footer.getChildren().add(btnSign);
        }

        // View PDF button
        Button btnPDF = new Button("📄 Voir le contrat PDF");
        btnPDF.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        btnPDF.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #555; "
                + "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; "
                + "-fx-border-color: #ddd; -fx-border-radius: 10; -fx-border-width: 1;");
        btnPDF.setOnMouseEntered(e -> btnPDF.setStyle("-fx-background-color: #eef0ff; -fx-text-fill: #667eea; "
                + "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; "
                + "-fx-border-color: #667eea; -fx-border-radius: 10; -fx-border-width: 1;"));
        btnPDF.setOnMouseExited(e -> btnPDF.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #555; "
                + "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; "
                + "-fx-border-color: #ddd; -fx-border-radius: 10; -fx-border-width: 1;"));
        btnPDF.setOnAction(e -> showContractPDF(contract));
        footer.getChildren().add(btnPDF);

        card.getChildren().addAll(header, body, footer);

        // Hover animation
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.01); st.setToY(1.01); st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private VBox buildDetailItem(String label, String value) {
        VBox item = new VBox(2);
        item.setMinWidth(150);
        Label lbl = new Label(label);
        lbl.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web("#95a5a6"));
        Label val = new Label(value);
        val.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        val.setTextFill(Color.web("#2c3e50"));
        item.getChildren().addAll(lbl, val);
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    private VBox buildSignatureStatus(String role, boolean signed, String signature, LocalDateTime signedAt) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 14, 10, 14));
        String bgColor = signed ? "#f0fff4" : "#f8f9fa";
        String borderColor = signed ? "#27ae60" : "#e0e0e0";
        box.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; "
                + "-fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-border-width: 1;");

        Label roleLbl = new Label(role);
        roleLbl.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        roleLbl.setTextFill(Color.web(signed ? "#27ae60" : "#95a5a6"));

        Label statusLbl = new Label(signed ? "✅ Signé" : "⏳ En attente");
        statusLbl.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.BOLD, 11));
        statusLbl.setTextFill(Color.web(signed ? "#27ae60" : "#7f8c8d"));

        box.getChildren().addAll(roleLbl, statusLbl);

        if (signed && signature != null) {
            Label hashLbl = new Label("🔐 " + signature.substring(0, Math.min(20, signature.length())) + "...");
            hashLbl.setFont(javafx.scene.text.Font.font("Consolas", 9));
            hashLbl.setTextFill(Color.web("#95a5a6"));
            box.getChildren().add(hashLbl);
        }
        if (signed && signedAt != null) {
            Label dateLbl = new Label("📅 " + signedAt.format(DATE_FMT));
            dateLbl.setFont(javafx.scene.text.Font.font("Segoe UI", 9));
            dateLbl.setTextFill(Color.web("#b0b0b0"));
            box.getChildren().add(dateLbl);
        }

        return box;
    }

    private boolean canCurrentUserSign(Contract contract) {
        if (currentUser == null) return false;
        if (isInvestor) {
            return contract.getInvestorId() == currentUser.getId() && !contract.isInvestorSigned();
        } else {
            return contract.getEntrepreneurId() == currentUser.getId() && !contract.isEntrepreneurSigned();
        }
    }

    private void handleSign(Contract contract, VBox card) {
        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Signature du contrat");
        alert.setHeaderText("Signer le contrat #" + contract.getId() + " ?");
        alert.setContentText("En signant, vous acceptez les termes de cet investissement.\n"
                + "Une signature numérique SHA-256 sera générée.\n\n"
                + "Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = contractService.signContract(contract.getId(), currentUser.getId(), isInvestor);
                if (success) {
                    // Reload the contracts
                    loadContracts();

                    // Show success notification
                    showSuccessNotification("✅ Contrat signé avec succès!", "Votre signature numérique SHA-256 a été enregistrée.");
                } else {
                    showErrorNotification("Erreur", "Impossible de signer le contrat.");
                }
            }
        });
    }

    private void showContractPDF(Contract contract) {
        try {
            byte[] pdf = generateContractPDF(contract);
            if (pdf != null) {
                StackPane root = getSceneRootStack();
                if (root != null) {
                    PDFPreviewPopup.show(pdf, "Contrat d'Investissement",
                            "contrat_" + contract.getId() + ".pdf", root);
                }
            }
        } catch (Exception e) {
            LOG.warning("Error generating contract PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PDF GENERATION — Contract with SHA-256 signatures
    // ═══════════════════════════════════════════════════════════

    private byte[] generateContractPDF(Contract contract) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            java.awt.Color PRIMARY = new java.awt.Color(102, 126, 234);
            java.awt.Color DARK = new java.awt.Color(44, 62, 80);
            java.awt.Color GRAY = new java.awt.Color(127, 140, 141);
            java.awt.Color SUCCESS = new java.awt.Color(39, 174, 96);

            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, DARK);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY);
            Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, DARK);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, DARK);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY);
            Font hashFont = new Font(Font.COURIER, 7, Font.NORMAL, GRAY);

            // ── Header ──
            Paragraph title = new Paragraph("CONTRAT D'INVESTISSEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("NAJAHNI — Plateforme d'Investissement Intelligente", subtitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(5);
            doc.add(sub);

            Paragraph contractId = new Paragraph("Contrat N° " + contract.getId() + " — Offre N° " + contract.getOfferId(), subtitleFont);
            contractId.setAlignment(Element.ALIGN_CENTER);
            contractId.setSpacingAfter(10);
            doc.add(contractId);

            doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_CENTER, -5));
            doc.add(Chunk.NEWLINE);

            // ── Date ──
            String dateStr = contract.getCreatedAt() != null ? contract.getCreatedAt().format(DATE_FMT) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Paragraph datePara = new Paragraph("Date d'émission : " + dateStr, normalFont);
            datePara.setSpacingAfter(15);
            doc.add(datePara);

            // ── Parties ── 
            doc.add(new Paragraph("PARTIES AU CONTRAT", sectionFont));
            doc.add(new LineSeparator(0.5f, 100, GRAY, Element.ALIGN_LEFT, -3));
            doc.add(Chunk.NEWLINE);

            PdfPTable partiesTable = new PdfPTable(2);
            partiesTable.setWidthPercentage(100);
            partiesTable.setSpacingAfter(15);

            PdfPCell investorCell = new PdfPCell();
            investorCell.setBorder(Rectangle.BOX);
            investorCell.setBorderColor(new java.awt.Color(230, 230, 230));
            investorCell.setPadding(12);
            investorCell.setBackgroundColor(new java.awt.Color(240, 255, 244));
            investorCell.addElement(new Paragraph("INVESTISSEUR", new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS)));
            investorCell.addElement(new Paragraph(contract.getInvestorName() != null ? contract.getInvestorName() : "ID #" + contract.getInvestorId(), valueFont));

            PdfPCell entrepreneurCell = new PdfPCell();
            entrepreneurCell.setBorder(Rectangle.BOX);
            entrepreneurCell.setBorderColor(new java.awt.Color(230, 230, 230));
            entrepreneurCell.setPadding(12);
            entrepreneurCell.setBackgroundColor(new java.awt.Color(238, 240, 255));
            entrepreneurCell.addElement(new Paragraph("ENTREPRENEUR", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY)));
            entrepreneurCell.addElement(new Paragraph(contract.getEntrepreneurName() != null ? contract.getEntrepreneurName() : "ID #" + contract.getEntrepreneurId(), valueFont));

            partiesTable.addCell(investorCell);
            partiesTable.addCell(entrepreneurCell);
            doc.add(partiesTable);

            // ── Investment Details ──
            doc.add(new Paragraph("DÉTAILS DE L'INVESTISSEMENT", sectionFont));
            doc.add(new LineSeparator(0.5f, 100, GRAY, Element.ALIGN_LEFT, -3));
            doc.add(Chunk.NEWLINE);

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{35, 65});
            detailsTable.setSpacingAfter(15);

            addDetailRow(detailsTable, "Projet", contract.getProjectTitle() != null ? contract.getProjectTitle() : "N/A", labelFont, valueFont);
            addDetailRow(detailsTable, "Secteur", contract.getProjectSector() != null ? contract.getProjectSector() : "N/A", labelFont, valueFont);
            addDetailRow(detailsTable, "Montant investi", contract.getProposedAmount() != null ? contract.getProposedAmount() : "N/A", labelFont, valueFont);
            addDetailRow(detailsTable, "Transaction Stripe", contract.getPaymentIntentId() != null ? contract.getPaymentIntentId() : "N/A", labelFont, new Font(Font.COURIER, 9, Font.NORMAL, DARK));
            doc.add(detailsTable);

            // ── Terms ──
            doc.add(new Paragraph("TERMES ET CONDITIONS", sectionFont));
            doc.add(new LineSeparator(0.5f, 100, GRAY, Element.ALIGN_LEFT, -3));
            doc.add(Chunk.NEWLINE);

            String[] terms = {
                    "1. L'investisseur s'engage à fournir le montant spécifié ci-dessus pour le projet indiqué.",
                    "2. L'entrepreneur s'engage à utiliser les fonds exclusivement pour le développement du projet.",
                    "3. Les deux parties reconnaissent avoir pris connaissance des risques liés à l'investissement.",
                    "4. Ce contrat est régi par les lois tunisiennes en vigueur.",
                    "5. En cas de litige, les parties s'engagent à chercher une résolution amiable.",
                    "6. Ce contrat est signé numériquement via un hachage SHA-256 certifiant l'identité de chaque partie."
            };
            for (String term : terms) {
                Paragraph p = new Paragraph(term, normalFont);
                p.setSpacingAfter(4);
                p.setIndentationLeft(10);
                doc.add(p);
            }
            doc.add(Chunk.NEWLINE);

            // ── Signatures ──
            doc.add(new Paragraph("SIGNATURES NUMÉRIQUES (SHA-256)", sectionFont));
            doc.add(new LineSeparator(0.5f, 100, GRAY, Element.ALIGN_LEFT, -3));
            doc.add(Chunk.NEWLINE);

            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingAfter(15);

            // Investor signature
            PdfPCell invSigCell = new PdfPCell();
            invSigCell.setBorder(Rectangle.BOX);
            invSigCell.setBorderColor(new java.awt.Color(230, 230, 230));
            invSigCell.setPadding(12);
            invSigCell.addElement(new Paragraph("Investisseur", new Font(Font.HELVETICA, 10, Font.BOLD, DARK)));
            if (contract.isInvestorSigned()) {
                invSigCell.setBackgroundColor(new java.awt.Color(240, 255, 244));
                invSigCell.addElement(new Paragraph("✅ Signé", new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS)));
                invSigCell.addElement(new Paragraph(contract.getInvestorSignature(), hashFont));
                if (contract.getInvestorSignedAt() != null) {
                    invSigCell.addElement(new Paragraph("Le " + contract.getInvestorSignedAt().format(DATE_FMT), smallFont));
                }
            } else {
                invSigCell.setBackgroundColor(new java.awt.Color(248, 249, 250));
                invSigCell.addElement(new Paragraph("⏳ En attente de signature", new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY)));
            }

            // Entrepreneur signature
            PdfPCell entSigCell = new PdfPCell();
            entSigCell.setBorder(Rectangle.BOX);
            entSigCell.setBorderColor(new java.awt.Color(230, 230, 230));
            entSigCell.setPadding(12);
            entSigCell.addElement(new Paragraph("Entrepreneur", new Font(Font.HELVETICA, 10, Font.BOLD, DARK)));
            if (contract.isEntrepreneurSigned()) {
                entSigCell.setBackgroundColor(new java.awt.Color(240, 255, 244));
                entSigCell.addElement(new Paragraph("✅ Signé", new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS)));
                entSigCell.addElement(new Paragraph(contract.getEntrepreneurSignature(), hashFont));
                if (contract.getEntrepreneurSignedAt() != null) {
                    entSigCell.addElement(new Paragraph("Le " + contract.getEntrepreneurSignedAt().format(DATE_FMT), smallFont));
                }
            } else {
                entSigCell.setBackgroundColor(new java.awt.Color(248, 249, 250));
                entSigCell.addElement(new Paragraph("⏳ En attente de signature", new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY)));
            }

            sigTable.addCell(invSigCell);
            sigTable.addCell(entSigCell);
            doc.add(sigTable);

            // ── Footer ──
            doc.add(new LineSeparator(0.5f, 100, new java.awt.Color(200, 200, 200), Element.ALIGN_CENTER, 0));
            Paragraph footer = new Paragraph(
                    "Document généré automatiquement par NAJAHNI — Plateforme d'Investissement Intelligente\n"
                            + "© 2025 NAJAHNI. Tous droits réservés.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.warning("Error generating contract PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(new java.awt.Color(240, 240, 240));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(new java.awt.Color(248, 249, 251));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new java.awt.Color(240, 240, 240));
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ═══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════

    private VBox buildEmptyState() {
        VBox emptyBox = new VBox(12);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60, 20, 60, 20));

        Label emptyIcon = new Label("📜");
        emptyIcon.setFont(javafx.scene.text.Font.font(48));

        Label emptyTitle = new Label("Aucun contrat disponible");
        emptyTitle.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.BOLD, 18));
        emptyTitle.setTextFill(Color.web("#7f8c8d"));

        Label emptyDesc = new Label("Les contrats apparaîtront ici après le paiement d'une offre d'investissement.");
        emptyDesc.setFont(javafx.scene.text.Font.font("Segoe UI", 13));
        emptyDesc.setTextFill(Color.web("#95a5a6"));
        emptyDesc.setWrapText(true);
        emptyDesc.setMaxWidth(400);
        emptyDesc.setAlignment(Pos.CENTER);
        emptyDesc.setStyle("-fx-alignment: center;");

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
        return emptyBox;
    }

    private void showSuccessNotification(String title, String message) {
        HBox notification = new HBox(10);
        notification.setAlignment(Pos.CENTER);
        notification.setPadding(new Insets(14, 24, 14, 24));
        notification.setMaxWidth(500);
        notification.setStyle("-fx-background-color: #eafaf1; -fx-background-radius: 12; "
                + "-fx-border-color: #27ae60; -fx-border-radius: 12; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        Label icon = new Label("✅");
        icon.setFont(javafx.scene.text.Font.font(20));
        VBox textBox = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setFont(javafx.scene.text.Font.font("Segoe UI", FontWeight.BOLD, 13));
        titleLbl.setTextFill(Color.web("#27ae60"));
        Label msgLbl = new Label(message);
        msgLbl.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
        msgLbl.setTextFill(Color.web("#2c3e50"));
        textBox.getChildren().addAll(titleLbl, msgLbl);

        notification.getChildren().addAll(icon, textBox);
        StackPane.setAlignment(notification, Pos.TOP_CENTER);
        StackPane.setMargin(notification, new Insets(20, 0, 0, 0));

        StackPane root = getSceneRootStack();
        if (root == null) return;
        root.getChildren().add(notification);

        notification.setOpacity(0);
        notification.setTranslateY(-30);
        FadeTransition ft = new FadeTransition(Duration.millis(300), notification);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), notification);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), notification);
            out.setToValue(0);
            out.setOnFinished(ev -> root.getChildren().remove(notification));
            out.play();
        });
        pause.play();
    }

    private void showErrorNotification(String title, String message) {
        // Simplified — reuse same structure with red colors
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private StackPane getSceneRootStack() {
        try {
            javafx.scene.Scene scene = rootStack.getScene();
            if (scene == null) return rootStack;
            javafx.scene.Parent root = scene.getRoot();
            if (root instanceof StackPane sp) return sp;
            StackPane wrapper = new StackPane(root);
            if (root.getUserData() != null) wrapper.setUserData(root.getUserData());
            scene.setRoot(wrapper);
            return wrapper;
        } catch (Exception e) { return rootStack; }
    }
}
