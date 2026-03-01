package com.najahni.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.najahni.models.InvestmentContract;
import com.najahni.models.InvestmentOffer;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.Project;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Service de génération de documents PDF pour le module investissement.
 *
 * Génère :
 * - Reçu de paiement Stripe (après paiement réussi)
 * - Rapport portfolio (résumé de tous les investissements)
 * - Fiche opportunité (détails d'une opportunité)
 *
 * Utilise OpenPDF (fork iText LGPL) pour la création de PDFs professionnels.
 */
public class InvestmentPDFService {

    private static final Logger LOG = Logger.getLogger(InvestmentPDFService.class.getName());

    // ─── Couleurs NAJAHNI ────────────────────────────────────
    private static final Color PRIMARY = new Color(39, 174, 96);      // #27ae60
    private static final Color PRIMARY_DARK = new Color(30, 130, 76); // #1e824c
    private static final Color ACCENT = new Color(52, 152, 219);      // #3498db
    private static final Color DARK = new Color(44, 62, 80);          // #2c3e50
    private static final Color GRAY = new Color(127, 140, 141);       // #7f8c8d
    private static final Color LIGHT_BG = new Color(245, 247, 250);   // #f5f7fa
    private static final Color SUCCESS_BG = new Color(234, 250, 241); // #eafaf1
    private static final Color BORDER = new Color(220, 220, 220);

    // ─── Polices ─────────────────────────────────────────────
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, PRIMARY_DARK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, DARK);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, DARK);
    private static final Font BIG_AMOUNT_FONT = new Font(Font.HELVETICA, 28, Font.BOLD, PRIMARY);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, DARK);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    private static final NumberFormat CURRENCY_FMT;
    static {
        CURRENCY_FMT = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        CURRENCY_FMT.setCurrency(java.util.Currency.getInstance("EUR"));
    }

    // ═══════════════════════════════════════════════════════════
    //  1. REÇU DE PAIEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère un reçu de paiement PDF en mémoire.
     *
     * @param offer       L'offre payée
     * @param opportunity L'opportunité associée (peut être null)
     * @param project     Le projet associé (peut être null)
     * @return byte[] contenant le PDF, ou null en cas d'erreur
     */
    public byte[] generatePaymentReceipt(InvestmentOffer offer, InvestmentOpportunity opportunity, Project project) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Header NAJAHNI ──
            addHeader(doc, "REÇU DE PAIEMENT");

            // ── Reference & Date ──
            String ref = offer.getPaymentIntentId() != null
                    ? offer.getPaymentIntentId().replace("pi_", "REF-")
                    : "REF-" + offer.getId();
            PdfPTable refTable = new PdfPTable(2);
            refTable.setWidthPercentage(100);
            refTable.setSpacingBefore(12);
            addRefRow(refTable, "N° Référence :", ref);
            addRefRow(refTable, "Date :",
                    offer.getPaidAt() != null ? offer.getPaidAt().format(DATETIME_FMT) : LocalDate.now().format(DATE_FMT));
            doc.add(refTable);

            doc.add(new Paragraph(" "));

            // ── Grand montant centré ──
            Paragraph amountPara = new Paragraph(offer.getFormattedAmount(), BIG_AMOUNT_FONT);
            amountPara.setAlignment(Element.ALIGN_CENTER);
            doc.add(amountPara);
            Paragraph eurLabel = new Paragraph("Montant payé (EUR)", LABEL_FONT);
            eurLabel.setAlignment(Element.ALIGN_CENTER);
            eurLabel.setSpacingAfter(16);
            doc.add(eurLabel);

            // ── Status badge ──
            PdfPTable statusTable = new PdfPTable(1);
            statusTable.setWidthPercentage(40);
            statusTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell statusCell = new PdfPCell(new Phrase("✓ PAYÉ", new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE)));
            statusCell.setBackgroundColor(PRIMARY);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setPadding(8);
            statusCell.setBorderColor(PRIMARY);
            statusCell.setBorderWidth(0);
            statusTable.addCell(statusCell);
            doc.add(statusTable);

            doc.add(new Paragraph(" "));

            // ── Section : Investisseur ──
            addSectionTitle(doc, "INVESTISSEUR");
            PdfPTable investorTable = createDetailTable();
            addDetailRow(investorTable, "Nom", offer.getInvestorName() != null ? offer.getInvestorName() : "Investisseur #" + offer.getInvestorId());
            addDetailRow(investorTable, "ID", String.valueOf(offer.getInvestorId()));
            doc.add(investorTable);

            // ── Section : Détails de l'investissement ──
            addSectionTitle(doc, "DÉTAILS DE L'INVESTISSEMENT");
            PdfPTable detailTable = createDetailTable();
            addDetailRow(detailTable, "Montant proposé", offer.getFormattedAmount());
            if (opportunity != null) {
                addDetailRow(detailTable, "Opportunité", opportunity.getDescription() != null ? opportunity.getDescription() : "N/A");
                addDetailRow(detailTable, "Montant cible", opportunity.getFormattedAmount());
                addDetailRow(detailTable, "Deadline", opportunity.getDeadline() != null
                        ? opportunity.getDeadline().format(DATE_FMT) : "N/A");
            }
            if (project != null) {
                addDetailRow(detailTable, "Projet", project.getTitle() != null ? project.getTitle() : "N/A");
                addDetailRow(detailTable, "Secteur", project.getSector() != null ? project.getSector() : "N/A");
            }
            doc.add(detailTable);

            // ── Section : Paiement ──
            addSectionTitle(doc, "INFORMATIONS DE PAIEMENT");
            PdfPTable paymentTable = createDetailTable();
            addDetailRow(paymentTable, "Transaction Stripe", offer.getPaymentIntentId() != null ? offer.getPaymentIntentId() : "N/A");
            addDetailRow(paymentTable, "Méthode", "Carte bancaire via Stripe");
            addDetailRow(paymentTable, "Devise", "EUR (Euro)");
            addDetailRow(paymentTable, "Statut", "✓ Succès");
            addDetailRow(paymentTable, "Date du paiement", offer.getPaidAt() != null
                    ? offer.getPaidAt().format(DATETIME_FMT) : "N/A");
            doc.add(paymentTable);

            // ── Footer ──
            addFooter(doc, "Ce document fait office de reçu de paiement pour votre investissement.");

            doc.close();
            LOG.info("✓ PDF reçu de paiement généré (" + baos.size() + " octets)");
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.severe("✗ Erreur génération PDF reçu: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  2. RAPPORT PORTFOLIO
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère un rapport PDF du portfolio complet d'un investisseur.
     *
     * @param paidOffers    Liste des offres payées
     * @param opportunities Liste des opportunités associées
     * @param projects      Liste des projets associés
     * @param investorName  Nom de l'investisseur
     * @return byte[] contenant le PDF, ou null en cas d'erreur
     */
    public byte[] generatePortfolioReport(List<InvestmentOffer> paidOffers,
                                           List<InvestmentOpportunity> opportunities,
                                           List<Project> projects,
                                           String investorName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Header ──
            addHeader(doc, "RAPPORT PORTFOLIO");

            // ── Investor info ──
            PdfPTable refTable = new PdfPTable(2);
            refTable.setWidthPercentage(100);
            refTable.setSpacingBefore(12);
            addRefRow(refTable, "Investisseur :", investorName != null ? investorName : "Mon Portfolio");
            addRefRow(refTable, "Date du rapport :", LocalDate.now().format(DATE_FMT));
            addRefRow(refTable, "Nombre d'investissements :", String.valueOf(paidOffers.size()));
            doc.add(refTable);

            doc.add(new Paragraph(" "));

            // ── Résumé financier ──
            BigDecimal total = paidOffers.stream()
                    .map(InvestmentOffer::getProposedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = paidOffers.isEmpty() ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(paidOffers.size()), 2, java.math.RoundingMode.HALF_UP);

            addSectionTitle(doc, "RÉSUMÉ FINANCIER");
            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(8);
            summaryTable.setSpacingAfter(16);

            addSummaryCard(summaryTable, "Total Investi", CURRENCY_FMT.format(total), PRIMARY);
            addSummaryCard(summaryTable, "Nb. Investissements", String.valueOf(paidOffers.size()), ACCENT);
            addSummaryCard(summaryTable, "Montant Moyen", CURRENCY_FMT.format(avg), PRIMARY_DARK);
            doc.add(summaryTable);

            // ── Tableau détaillé ──
            addSectionTitle(doc, "DÉTAIL DES INVESTISSEMENTS");
            PdfPTable investTable = new PdfPTable(5);
            investTable.setWidthPercentage(100);
            investTable.setSpacingBefore(8);
            investTable.setWidths(new float[]{3f, 2f, 2f, 2f, 2f});

            // Table headers
            addTableHeader(investTable, "Projet");
            addTableHeader(investTable, "Secteur");
            addTableHeader(investTable, "Montant");
            addTableHeader(investTable, "Date");
            addTableHeader(investTable, "Transaction");

            // Table rows
            boolean alternate = false;
            for (InvestmentOffer offer : paidOffers) {
                Color rowBg = alternate ? LIGHT_BG : Color.WHITE;
                addTableCell(investTable, offer.getProjectTitle() != null ? offer.getProjectTitle() : "N/A", rowBg);
                addTableCell(investTable, offer.getProjectSector() != null ? offer.getProjectSector() : "N/A", rowBg);
                addTableCell(investTable, offer.getFormattedAmount(), rowBg);
                addTableCell(investTable, offer.getPaidAt() != null ? offer.getPaidAt().format(DATE_FMT) : "N/A", rowBg);
                String txShort = offer.getPaymentIntentId() != null
                        ? (offer.getPaymentIntentId().length() > 15
                            ? offer.getPaymentIntentId().substring(0, 12) + "…"
                            : offer.getPaymentIntentId())
                        : "N/A";
                addTableCell(investTable, txShort, rowBg);
                alternate = !alternate;
            }
            doc.add(investTable);

            // ── Footer ──
            addFooter(doc, "Ce rapport est généré automatiquement et reflète l'état de votre portfolio à la date du " + LocalDate.now().format(DATE_FMT) + ".");

            doc.close();
            LOG.info("✓ PDF rapport portfolio généré (" + baos.size() + " octets)");
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.severe("✗ Erreur génération PDF portfolio: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  3. FICHE OPPORTUNITÉ
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère une fiche PDF détaillée d'une opportunité d'investissement.
     *
     * @param opportunity L'opportunité
     * @param project     Le projet associé (peut être null)
     * @return byte[] contenant le PDF, ou null en cas d'erreur
     */
    public byte[] generateOpportunitySheet(InvestmentOpportunity opportunity, Project project) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Header ──
            addHeader(doc, "FICHE OPPORTUNITÉ");

            // ── Meta info ──
            PdfPTable refTable = new PdfPTable(2);
            refTable.setWidthPercentage(100);
            refTable.setSpacingBefore(12);
            addRefRow(refTable, "ID Opportunité :", "#" + opportunity.getId());
            addRefRow(refTable, "Statut :", opportunity.getStatus() != null ? opportunity.getStatus().name() : "N/A");
            addRefRow(refTable, "Date de génération :", LocalDate.now().format(DATE_FMT));
            doc.add(refTable);

            doc.add(new Paragraph(" "));

            // ── Grand montant ──
            Paragraph amountPara = new Paragraph(opportunity.getFormattedAmount(), BIG_AMOUNT_FONT);
            amountPara.setAlignment(Element.ALIGN_CENTER);
            doc.add(amountPara);
            Paragraph targetLabel = new Paragraph("Montant cible (EUR)", LABEL_FONT);
            targetLabel.setAlignment(Element.ALIGN_CENTER);
            targetLabel.setSpacingAfter(16);
            doc.add(targetLabel);

            // ── Détails de l'opportunité ──
            addSectionTitle(doc, "DÉTAILS");
            PdfPTable detailTable = createDetailTable();
            addDetailRow(detailTable, "Description", opportunity.getDescription() != null ? opportunity.getDescription() : "N/A");
            addDetailRow(detailTable, "Montant cible", opportunity.getFormattedAmount());
            addDetailRow(detailTable, "Deadline", opportunity.getDeadline() != null
                    ? opportunity.getDeadline().format(DATE_FMT) : "Non défini");
            addDetailRow(detailTable, "Statut", opportunity.getStatus() != null ? opportunity.getStatus().name() : "N/A");
            doc.add(detailTable);

            // ── Analyse de risque (si disponible) ──
            if (opportunity.getRiskScore() != null) {
                addSectionTitle(doc, "ANALYSE DE RISQUE IA");
                PdfPTable riskTable = createDetailTable();
                addDetailRow(riskTable, "Score de risque", String.format("%.0f / 100", opportunity.getRiskScore()));
                addDetailRow(riskTable, "Niveau", opportunity.getRiskLabel() != null ? opportunity.getRiskLabel() : "Non évalué");

                // Risk level interpretation
                String interpretation;
                double score = opportunity.getRiskScore();
                if (score <= 30) interpretation = "Risque faible — investissement relativement sûr";
                else if (score <= 60) interpretation = "Risque modéré — prudence recommandée";
                else interpretation = "Risque élevé — investissement à forte volatilité";
                addDetailRow(riskTable, "Interprétation", interpretation);
                doc.add(riskTable);

                // Risk bar (visual indicator)
                PdfPTable barTable = new PdfPTable(1);
                barTable.setWidthPercentage(80);
                barTable.setHorizontalAlignment(Element.ALIGN_CENTER);
                barTable.setSpacingBefore(6);
                barTable.setSpacingAfter(10);
                PdfPCell barCell = new PdfPCell();
                barCell.setPadding(0);
                barCell.setBorderWidth(0);

                PdfPTable innerBar = new PdfPTable(2);
                innerBar.setWidthPercentage(100);
                float pct = (float) (score / 100.0);
                innerBar.setWidths(new float[]{Math.max(pct, 0.01f), Math.max(1 - pct, 0.01f)});

                Color barColor = score <= 30 ? PRIMARY : (score <= 60 ? new Color(243, 156, 18) : new Color(231, 76, 60));
                PdfPCell filledCell = new PdfPCell(new Phrase(""));
                filledCell.setBackgroundColor(barColor);
                filledCell.setFixedHeight(12);
                filledCell.setBorderWidth(0);
                innerBar.addCell(filledCell);

                PdfPCell emptyCell = new PdfPCell(new Phrase(""));
                emptyCell.setBackgroundColor(new Color(236, 240, 241));
                emptyCell.setFixedHeight(12);
                emptyCell.setBorderWidth(0);
                innerBar.addCell(emptyCell);

                barCell.addElement(innerBar);
                barTable.addCell(barCell);
                doc.add(barTable);
            }

            // ── Projet associé ──
            if (project != null) {
                addSectionTitle(doc, "PROJET ASSOCIÉ");
                PdfPTable projectTable = createDetailTable();
                addDetailRow(projectTable, "Titre", project.getTitle() != null ? project.getTitle() : "N/A");
                addDetailRow(projectTable, "Secteur", project.getSector() != null ? project.getSector() : "N/A");
                addDetailRow(projectTable, "Description", project.getDescription() != null
                        ? (project.getDescription().length() > 300
                            ? project.getDescription().substring(0, 297) + "…"
                            : project.getDescription())
                        : "N/A");
                addDetailRow(projectTable, "Statut", project.getStatus() != null ? project.getStatus().name() : "N/A");
                doc.add(projectTable);
            }

            // ── Footer ──
            addFooter(doc, "Cette fiche est générée automatiquement et reflète les informations disponibles au " + LocalDate.now().format(DATE_FMT) + ".");

            doc.close();
            LOG.info("✓ PDF fiche opportunité généré (" + baos.size() + " octets)");
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.severe("✗ Erreur génération PDF opportunité: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  4. PDF CONTRAT NUMÉRIQUE
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère un PDF de contrat d'investissement numérique.
     *
     * @param contract    Le contrat
     * @param offer       L'offre associée (peut être null)
     * @param opportunity L'opportunité associée (peut être null)
     * @param project     Le projet associé (peut être null)
     * @return byte[] contenant le PDF, ou null en cas d'erreur
     */
    public byte[] generateContractPDF(InvestmentContract contract, InvestmentOffer offer,
                                       InvestmentOpportunity opportunity, Project project) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Header ──
            addHeader(doc, "CONTRAT D'INVESTISSEMENT");

            // ── Référence ──
            PdfPTable refTable = new PdfPTable(2);
            refTable.setWidthPercentage(100);
            refTable.setSpacingBefore(12);
            addRefRow(refTable, "N° Contrat :", contract.getContractNumber());
            addRefRow(refTable, "Statut :", contract.getStatusEmoji() + " " + contract.getStatus().getDisplayName());
            addRefRow(refTable, "Date :", contract.getCreatedAt() != null
                    ? contract.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "N/A");
            doc.add(refTable);

            doc.add(new Paragraph(" "));

            // ── Parties ──
            addSectionTitle(doc, "📋 Parties contractantes");
            PdfPTable partiesTable = createDetailTable();
            addDetailRow(partiesTable, "Investisseur",
                    contract.getInvestorName() != null ? contract.getInvestorName() : "Investisseur #" + contract.getInvestorId());
            addDetailRow(partiesTable, "Entrepreneur",
                    contract.getEntrepreneurName() != null ? contract.getEntrepreneurName() : "Entrepreneur #" + contract.getEntrepreneurId());
            if (project != null) {
                addDetailRow(partiesTable, "Projet", project.getTitle());
                if (project.getSector() != null) addDetailRow(partiesTable, "Secteur", project.getSector());
            }
            if (offer != null) {
                addDetailRow(partiesTable, "Montant investi", offer.getFormattedAmount());
            }
            if (opportunity != null) {
                addDetailRow(partiesTable, "Montant cible", opportunity.getFormattedAmount());
            }
            doc.add(partiesTable);

            // ── Termes du contrat ──
            addSectionTitle(doc, "📜 Termes et conditions");
            if (contract.getTermsText() != null) {
                Font termsFont = new Font(Font.HELVETICA, 9, Font.NORMAL, DARK);
                Paragraph terms = new Paragraph(contract.getTermsText(), termsFont);
                terms.setSpacingBefore(6);
                terms.setLeading(14f);
                doc.add(terms);
            }

            // ── Signatures ──
            addSectionTitle(doc, "✍️ Signatures numériques");
            PdfPTable sigTable = createDetailTable();
            addDetailRow(sigTable, "Signature Investisseur",
                    contract.getInvestorSignature() != null
                            ? "✓ Signé" + (contract.getInvestorSignedAt() != null
                            ? " le " + contract.getInvestorSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "")
                            : "⏳ En attente");
            addDetailRow(sigTable, "Signature Entrepreneur",
                    contract.getEntrepreneurSignature() != null
                            ? "✓ Signé" + (contract.getEntrepreneurSignedAt() != null
                            ? " le " + contract.getEntrepreneurSignedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "")
                            : "⏳ En attente");
            doc.add(sigTable);

            // ── Intégrité SHA-256 ──
            addSectionTitle(doc, "🔐 Intégrité du document");
            PdfPTable hashTable = createDetailTable();
            addDetailRow(hashTable, "Hash SHA-256", contract.getSha256Hash() != null ? contract.getSha256Hash() : "N/A");
            doc.add(hashTable);

            // ── Footer ──
            addFooter(doc, "Ce document est un contrat numérique émis par NAJAHNI. L'intégrité est vérifiable via le hash SHA-256.");

            doc.close();
            LOG.info("✓ PDF contrat généré: " + contract.getContractNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.severe("✗ Erreur génération PDF contrat: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS — Construction des éléments PDF
    // ═══════════════════════════════════════════════════════════

    /** En-tête NAJAHNI avec logo texte + séparateur. */
    private void addHeader(Document doc, String documentType) throws DocumentException {
        // Brand name
        Font brandFont = new Font(Font.HELVETICA, 28, Font.BOLD, PRIMARY);
        Paragraph brand = new Paragraph("NAJAHNI", brandFont);
        brand.setAlignment(Element.ALIGN_LEFT);
        doc.add(brand);

        // Subtitle
        Paragraph sub = new Paragraph("Plateforme d'Investissement & Entrepreneuriat", SUBTITLE_FONT);
        sub.setSpacingAfter(4);
        doc.add(sub);

        // Document type
        Font typeFont = new Font(Font.HELVETICA, 16, Font.BOLD, DARK);
        Paragraph typePara = new Paragraph(documentType, typeFont);
        typePara.setSpacingBefore(10);
        typePara.setSpacingAfter(4);
        doc.add(typePara);

        // Separator
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(PRIMARY);
        sep.setLineWidth(2);
        doc.add(new Chunk(sep));
    }

    /** Titre de section avec icône. */
    private void addSectionTitle(Document doc, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, SECTION_FONT);
        section.setSpacingBefore(16);
        section.setSpacingAfter(6);
        doc.add(section);

        LineSeparator sep = new LineSeparator();
        sep.setLineColor(BORDER);
        sep.setLineWidth(0.5f);
        sep.setPercentage(100);
        doc.add(new Chunk(sep));
    }

    /** Crée un tableau 2 colonnes (label / value) avec bordures légères. */
    private PdfPTable createDetailTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1.5f, 3f});
        return table;
    }

    /** Ajoute une ligne label/value dans un tableau de détails. */
    private void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorderColor(BORDER);
        labelCell.setBorderWidth(0.5f);
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(LIGHT_BG);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorderColor(BORDER);
        valueCell.setBorderWidth(0.5f);
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /** Ajoute une ligne label/value dans le tableau de référence (sans bordures). */
    private void addRefRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorderWidth(0);
        labelCell.setPaddingBottom(4);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorderWidth(0);
        valueCell.setPaddingBottom(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /** Ajoute une carte résumé (pour le portfolio). */
    private void addSummaryCard(PdfPTable table, String title, String value, Color bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(0);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font titleFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY);
        Font valueFont = new Font(Font.HELVETICA, 16, Font.BOLD, bgColor);

        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        Paragraph valuePara = new Paragraph(value, valueFont);
        valuePara.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(titlePara);
        cell.addElement(valuePara);
        cell.setBackgroundColor(LIGHT_BG);

        table.addCell(cell);
    }

    /** Ajoute un en-tête de colonne de tableau. */
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(PRIMARY_DARK);
        cell.setPadding(8);
        cell.setBorderWidth(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /** Ajoute une cellule de données dans le tableau. */
    private void addTableCell(PdfPTable table, String text, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_CELL_FONT));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(7);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.3f);
        table.addCell(cell);
    }

    /** Pied de page avec notice légale. */
    private void addFooter(Document doc, String notice) throws DocumentException {
        doc.add(new Paragraph(" "));

        LineSeparator sep = new LineSeparator();
        sep.setLineColor(BORDER);
        sep.setLineWidth(0.5f);
        doc.add(new Chunk(sep));

        Paragraph footerLine1 = new Paragraph(
                "Généré par NAJAHNI — " + LocalDateTime.now().format(DATETIME_FMT), FOOTER_FONT);
        footerLine1.setAlignment(Element.ALIGN_CENTER);
        footerLine1.setSpacingBefore(10);
        doc.add(footerLine1);

        Paragraph footerLine2 = new Paragraph(notice, FOOTER_FONT);
        footerLine2.setAlignment(Element.ALIGN_CENTER);
        footerLine2.setSpacingBefore(2);
        doc.add(footerLine2);

        Paragraph footerLine3 = new Paragraph(
                "NAJAHNI • Plateforme d'Investissement & Entrepreneuriat • www.najahni.com", FOOTER_FONT);
        footerLine3.setAlignment(Element.ALIGN_CENTER);
        footerLine3.setSpacingBefore(4);
        doc.add(footerLine3);
    }
}
