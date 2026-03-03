package services.projets;

import models.projets.Projet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfRapportService — Génère un rapport PDF NAJAHNI en Java pur.
 * Aucun Python requis. Utilise iText7 (dépendance Maven).
 *
 * Ajoute dans pom.xml :
 *
 * <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itext7-core</artifactId>
 *     <version>7.2.5</version>
 *     <type>pom</type>
 * </dependency>
 */
public class PdfRapportService {

    // ── Dossier de sortie ─────────────────────────────────────────
    private static final String PDF_OUTPUT_DIR = "rapports_pdf/";

    // ── Palette NAJAHNI ──────────────────────────────────────────
    private static final DeviceRgb NAVY   = new DeviceRgb(26,  46,  90);
    private static final DeviceRgb GOLD   = new DeviceRgb(212, 160,  23);
    private static final DeviceRgb GREEN  = new DeviceRgb(39,  174,  96);
    private static final DeviceRgb RED    = new DeviceRgb(231,  76,  60);
    private static final DeviceRgb ORANGE = new DeviceRgb(230, 126,  34);
    private static final DeviceRgb BLUE   = new DeviceRgb(41,  128, 185);
    private static final DeviceRgb PURP   = new DeviceRgb(142,  68, 173);
    private static final DeviceRgb TEAL   = new DeviceRgb(22,  160, 133);
    private static final DeviceRgb LIGHT  = new DeviceRgb(244, 246, 250);
    private static final DeviceRgb GREY   = new DeviceRgb(127, 140, 141);
    private static final DeviceRgb DARK   = new DeviceRgb(44,   62,  80);
    private static final DeviceRgb WHITE  = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb CREAM  = new DeviceRgb(255, 251, 240);

    // ── Fonts (initialisés par instance) ─────────────────────────
    private PdfFont NORMAL;
    private PdfFont BOLD;
    private PdfFont ITALIC;
    private Document document;

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODES PUBLIQUES STATIQUES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère le score IA puis le PDF en un seul appel.
     */
    public static String scorerEtGenererPdf(Projet projet) throws Exception {
        JsonObject analyse = IAScoringService.genererScore(projet);
        return new PdfRapportService().genererPdf(projet, analyse);
    }

    /**
     * Génère le PDF à partir d'un JsonObject déjà calculé.
     */
    public static String genererPdfStatique(Projet projet, JsonObject analyseJson) throws Exception {
        return new PdfRapportService().genererPdf(projet, analyseJson);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GÉNÉRATION PRINCIPALE
    // ═══════════════════════════════════════════════════════════════

    private String genererPdf(Projet projet, JsonObject analyseJson) throws Exception {
        Files.createDirectories(Paths.get(PDF_OUTPUT_DIR));
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomFichier = PDF_OUTPUT_DIR + "rapport_"
                + sanitiser(projet.getTitre()) + "_" + timestamp + ".pdf";

        PdfWriter  writer = new PdfWriter(nomFichier);
        PdfDocument pdf   = new PdfDocument(writer);
        document          = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        NORMAL = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        BOLD   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        ITALIC = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        JsonObject a = analyseJson.has("analyse")
                ? analyseJson.getAsJsonObject("analyse")
                : analyseJson;

        float  score   = a.has("score")   ? a.get("score").getAsFloat()   : 0f;
        String verdict = a.has("verdict") ? a.get("verdict").getAsString() : "MOYEN";

        // ── PAGE 1 ──────────────────────────────────────────────
        ajouterEnTete(projet.getTitre());
        ajouterScoreVerdict(score, verdict);

        ajouterSection("Resume Executif", NAVY);
        ajouterTexte(getString(a, "resume"));

        String repq = getString(a, "reponses_questions");
        if (!repq.isEmpty()) {
            ajouterSection("Reponses a vos Questions", BLUE);
            ajouterTexteMultiligne(repq);
        }

        ajouterSection("Comprehension de l'Idee", PURP);
        ajouterTexteMultiligne(getString(a, "comprehension_idee"));

        ajouterDeuxColonnes(
                "Forces",     getStringList(a, "forces"),     GREEN,
                "Faiblesses", getStringList(a, "faiblesses"), RED);

        ajouterSection("Analyse Concurrentielle", TEAL);
        ajouterTexteMultiligne(getString(a, "analyse_concurrentielle"));

        ajouterSection("Potentiel de Croissance", BLUE);
        ajouterTexteMultiligne(getString(a, "potentiel_croissance"));

        // ── PAGE 2 ──────────────────────────────────────────────
        document.add(new AreaBreak());

        ajouterSection("Recommandations Court Terme (0-90 jours)", NAVY);
        ajouterBullets(getStringList(a, "recommandations_courtes"));

        ajouterSection("Strategie Long Terme", PURP);
        ajouterBullets(getStringList(a, "recommandations_long_terme"));

        ajouterDeuxColonnes(
                "Opportunites", splitLines(getString(a, "opportunites")), GREEN,
                "Menaces",      splitLines(getString(a, "menaces")),      ORANGE);

        ajouterSection("Estimation Investissement", GOLD);
        ajouterTexteMultiligne(getString(a, "estimation_investissement"));

        List<String> manq = getStringList(a, "donnees_manquantes");
        if (!manq.isEmpty()) {
            ajouterSection("Donnees Manquantes", RED);
            ajouterBullets(manq);
        }

        ajouterSection("Justification du Score", NAVY);
        ajouterTexteMultiligne(getString(a, "justification_score"));

        ajouterSection("Message du Jury NAJAHNI", GOLD);
        ajouterConseilJury(getString(a, "conseil_jury"));

        ajouterPiedDePage();

        document.close();
        System.out.println("PDF genere : " + nomFichier);
        return new File(nomFichier).getAbsolutePath();
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMPOSANTS VISUELS
    // ═══════════════════════════════════════════════════════════════

    private void ajouterEnTete(String titreProjet) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(NAVY).setPadding(18).setBorder(null);
        cell.add(new Paragraph("NAJAHNI")
                .setFont(BOLD).setFontSize(26).setFontColor(WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        cell.add(new Paragraph("Rapport d'Evaluation Startup")
                .setFont(NORMAL).setFontSize(12).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
        cell.add(new Paragraph(titreProjet)
                .setFont(BOLD).setFontSize(15).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(cell);
        document.add(table);
        document.add(new Paragraph().setMarginBottom(12));
    }

    private void ajouterScoreVerdict(float score, String verdict) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .useAllAvailableWidth();

        Cell scoreCell = new Cell().setBackgroundColor(LIGHT).setPadding(14).setBorder(null)
                .setBorderRight(new SolidBorder(new DeviceRgb(221, 226, 238), 1));
        scoreCell.add(new Paragraph("SCORE NAJAHNI")
                .setFont(NORMAL).setFontSize(9).setFontColor(GREY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        scoreCell.add(new Paragraph(String.format("%.0f/100", score))
                .setFont(BOLD).setFontSize(36).setFontColor(couleurScore(score))
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(scoreCell);

        Cell verdictCell = new Cell().setBackgroundColor(LIGHT).setPadding(14).setBorder(null);
        verdictCell.add(new Paragraph("VERDICT")
                .setFont(NORMAL).setFontSize(9).setFontColor(GREY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        verdictCell.add(new Paragraph(verdict)
                .setFont(BOLD).setFontSize(20).setFontColor(couleurVerdict(verdict))
                .setTextAlignment(TextAlignment.CENTER));
        table.addCell(verdictCell);

        document.add(table);
        document.add(new Paragraph().setMarginBottom(14));
    }

    private void ajouterSection(String titre, DeviceRgb couleur) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth();
        Cell c = new Cell().setBackgroundColor(couleur)
                .setPaddingLeft(12).setPaddingTop(7).setPaddingBottom(7).setBorder(null);
        c.add(new Paragraph(titre).setFont(BOLD).setFontSize(11).setFontColor(WHITE));
        t.addCell(c);
        document.add(t);
        document.add(new Paragraph().setMarginBottom(5));
    }

    private void ajouterTexte(String texte) {
        if (texte == null || texte.isEmpty()) return;
        document.add(new Paragraph(texte)
                .setFont(NORMAL).setFontSize(9.5f).setFontColor(DARK)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMultipliedLeading(1.4f).setMarginBottom(8));
    }

    private void ajouterTexteMultiligne(String texte) {
        if (texte == null || texte.isEmpty()) return;
        for (String ligne : texte.split("\n")) {
            String l = ligne.trim();
            if (!l.isEmpty()) {
                document.add(new Paragraph(l)
                        .setFont(NORMAL).setFontSize(9.5f).setFontColor(DARK)
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                        .setMultipliedLeading(1.4f).setMarginBottom(4));
            }
        }
        document.add(new Paragraph().setMarginBottom(4));
    }

    private void ajouterBullets(List<String> items) {
        for (String item : items) {
            document.add(new Paragraph("• " + item)
                    .setFont(NORMAL).setFontSize(9.5f).setFontColor(DARK)
                    .setMarginLeft(16).setMarginBottom(4).setMultipliedLeading(1.4f));
        }
        document.add(new Paragraph().setMarginBottom(4));
    }

    private void ajouterDeuxColonnes(String titre1, List<String> items1, DeviceRgb bg1,
                                     String titre2, List<String> items2, DeviceRgb bg2) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();
        table.addCell(buildColonne(titre1, items1, bg1));
        table.addCell(buildColonne(titre2, items2, bg2));
        document.add(table);
        document.add(new Paragraph().setMarginBottom(10));
    }

    private Cell buildColonne(String titre, List<String> items, DeviceRgb bg) {
        Cell outer = new Cell().setBorder(null).setPadding(0);
        Table inner = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth();

        Cell titreCell = new Cell().setBackgroundColor(bg).setPadding(7).setBorder(null);
        titreCell.add(new Paragraph(titre).setFont(BOLD).setFontSize(10).setFontColor(WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        inner.addCell(titreCell);

        for (String item : items) {
            Cell ic = new Cell().setBackgroundColor(LIGHT)
                    .setPaddingLeft(10).setPaddingTop(5).setPaddingBottom(5).setPaddingRight(8)
                    .setBorder(null)
                    .setBorderBottom(new SolidBorder(new DeviceRgb(220, 224, 232), 0.5f));
            ic.add(new Paragraph("• " + item)
                    .setFont(NORMAL).setFontSize(9).setFontColor(DARK).setMultipliedLeading(1.3f));
            inner.addCell(ic);
        }

        outer.add(inner);
        return outer;
    }

    private void ajouterConseilJury(String texte) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(CREAM).setPadding(16)
                .setBorder(new SolidBorder(GOLD, 1.5f));
        cell.add(new Paragraph(texte).setFont(ITALIC).setFontSize(10).setFontColor(DARK)
                .setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.5f));
        table.addCell(cell);
        document.add(table);
        document.add(new Paragraph().setMarginBottom(16));
    }

    private void ajouterPiedDePage() {
        document.add(new LineSeparator(new SolidLine(1f))
                .setStrokeColor(NAVY).setMarginTop(8));
        document.add(new Paragraph("Rapport genere par NAJAHNI AI Scoring System - Confidentiel")
                .setFont(NORMAL).setFontSize(8).setFontColor(GREY)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
    }

    // ═══════════════════════════════════════════════════════════════
    //  COULEURS DYNAMIQUES
    // ═══════════════════════════════════════════════════════════════

    private DeviceRgb couleurScore(float s) {
        if (s >= 80) return GREEN;
        if (s >= 65) return BLUE;
        if (s >= 50) return PURP;
        if (s >= 35) return ORANGE;
        return RED;
    }

    private DeviceRgb couleurVerdict(String v) {
        switch (v) {
            case "EXCELLENT": return GREEN;
            case "TRES_BON":  return BLUE;
            case "BON":       return PURP;
            case "MOYEN":     return ORANGE;
            case "RISQUE":    return RED;
            default:          return GREY;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS JSON
    // ═══════════════════════════════════════════════════════════════

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }

    private List<String> getStringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj == null || !obj.has(key)) return list;
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray())
                if (!item.isJsonNull()) list.add(item.getAsString());
        } else if (!el.isJsonNull()) {
            list.add(el.getAsString());
        }
        return list;
    }

    private List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        for (String l : text.split("\n")) {
            String t = l.trim();
            if (!t.isEmpty()) lines.add(t);
        }
        return lines;
    }

    private String sanitiser(String nom) {
        if (nom == null || nom.trim().isEmpty()) return "projet";
        String s = nom.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
        return s.substring(0, Math.min(s.length(), 40));
    }
}