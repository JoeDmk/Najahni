package services.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.Progression;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service that exports a list of completed courses to a styled PDF document.
 */
public class PdfExportService {

    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 22;
    private static final float HEADER_ROW_HEIGHT = 26;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generates a PDF listing all completed courses and saves it to the given file.
     *
     * @param completedCourses the courses that have been completed
     * @param progressions     matching progressions (same order) for date/XP info
     * @param outputFile       destination PDF file
     * @throws IOException if writing fails
     */
    public void exportCompletedCourses(List<Cours> completedCourses,
                                       List<Progression> progressions,
                                       File outputFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float tableWidth = pageWidth - 2 * MARGIN;

            // Column widths (5 columns)
            float[] colWidths = {30, tableWidth * 0.30f, tableWidth * 0.25f, 60, tableWidth * 0.20f};

            int rowIndex = 0;
            int totalRows = completedCourses.size();
            boolean firstPage = true;

            while (rowIndex < totalRows || firstPage) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = pageHeight - MARGIN;

                    if (firstPage) {
                        // --- Title ---
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                        cs.setNonStrokingColor(new Color(44, 62, 80));
                        cs.beginText();
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText("NAJAHNI - Cours Complétés");
                        cs.endText();
                        y -= 28;

                        // --- Subtitle / date ---
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.setNonStrokingColor(new Color(127, 140, 141));
                        cs.beginText();
                        cs.newLineAtOffset(MARGIN, y);
                        cs.showText("Généré le " + LocalDateTime.now().format(DATE_FMT)
                                + "  •  " + totalRows + " cours complété(s)");
                        cs.endText();
                        y -= 10;

                        // --- Separator line ---
                        y -= 10;
                        cs.setStrokingColor(new Color(52, 152, 219));
                        cs.setLineWidth(2);
                        cs.moveTo(MARGIN, y);
                        cs.lineTo(pageWidth - MARGIN, y);
                        cs.stroke();
                        y -= 20;

                        firstPage = false;
                    }

                    // --- Table header ---
                    drawTableHeader(cs, MARGIN, y, colWidths);
                    y -= HEADER_ROW_HEIGHT;

                    // --- Table rows ---
                    while (rowIndex < totalRows && y - ROW_HEIGHT > MARGIN + 30) {
                        Cours cours = completedCourses.get(rowIndex);
                        Progression prog = (rowIndex < progressions.size()) ? progressions.get(rowIndex) : null;

                        Color rowBg = (rowIndex % 2 == 0)
                                ? new Color(245, 246, 250)
                                : Color.WHITE;
                        drawRow(cs, MARGIN, y, colWidths, rowIndex + 1, cours, prog, rowBg);
                        y -= ROW_HEIGHT;
                        rowIndex++;
                    }

                    // --- Footer ---
                    cs.setFont(PDType1Font.HELVETICA, 8);
                    cs.setNonStrokingColor(new Color(180, 180, 180));
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, MARGIN - 10);
                    cs.showText("NAJAHNI © " + LocalDateTime.now().getYear());
                    cs.endText();
                }
            }

            doc.save(outputFile);
        }
    }

    /**
     * Generates a PDF listing all completed courses and returns it as a byte array.
     * Same styled output as the file version, but for in-app preview.
     */
    public byte[] exportCompletedCoursesToBytes(List<Cours> completedCourses,
                                                 List<Progression> progressions) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float tableWidth = pageWidth - 2 * MARGIN;
            float[] colWidths = {30, tableWidth * 0.30f, tableWidth * 0.25f, 60, tableWidth * 0.20f};
            int rowIndex = 0;
            int totalRows = completedCourses.size();
            boolean firstPage = true;
            while (rowIndex < totalRows || firstPage) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = pageHeight - MARGIN;
                    if (firstPage) {
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                        cs.setNonStrokingColor(new Color(44, 62, 80));
                        cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText("NAJAHNI - Cours Complétés"); cs.endText();
                        y -= 28;
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.setNonStrokingColor(new Color(127, 140, 141));
                        cs.beginText(); cs.newLineAtOffset(MARGIN, y);
                        cs.showText("Généré le " + LocalDateTime.now().format(DATE_FMT) + "  •  " + totalRows + " cours complété(s)");
                        cs.endText(); y -= 10; y -= 10;
                        cs.setStrokingColor(new Color(52, 152, 219)); cs.setLineWidth(2);
                        cs.moveTo(MARGIN, y); cs.lineTo(pageWidth - MARGIN, y); cs.stroke();
                        y -= 20; firstPage = false;
                    }
                    drawTableHeader(cs, MARGIN, y, colWidths); y -= HEADER_ROW_HEIGHT;
                    while (rowIndex < totalRows && y - ROW_HEIGHT > MARGIN + 30) {
                        Cours cours = completedCourses.get(rowIndex);
                        Progression prog = (rowIndex < progressions.size()) ? progressions.get(rowIndex) : null;
                        Color rowBg = (rowIndex % 2 == 0) ? new Color(245, 246, 250) : Color.WHITE;
                        drawRow(cs, MARGIN, y, colWidths, rowIndex + 1, cours, prog, rowBg);
                        y -= ROW_HEIGHT; rowIndex++;
                    }
                    cs.setFont(PDType1Font.HELVETICA, 8); cs.setNonStrokingColor(new Color(180, 180, 180));
                    cs.beginText(); cs.newLineAtOffset(MARGIN, MARGIN - 10);
                    cs.showText("NAJAHNI © " + LocalDateTime.now().getYear()); cs.endText();
                }
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Drawing helpers
    // -----------------------------------------------------------------------

    private void drawTableHeader(PDPageContentStream cs, float x, float y, float[] colWidths) throws IOException {
        // Background
        float totalWidth = 0;
        for (float w : colWidths) totalWidth += w;
        cs.setNonStrokingColor(new Color(52, 152, 219));
        cs.addRect(x, y - HEADER_ROW_HEIGHT, totalWidth, HEADER_ROW_HEIGHT);
        cs.fill();

        // Text
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.setNonStrokingColor(Color.WHITE);

        String[] headers = {"#", "Titre", "Niveau", "XP", "Complété le"};
        float cx = x + 5;
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(cx, y - HEADER_ROW_HEIGHT + 8);
            cs.showText(headers[i]);
            cs.endText();
            cx += colWidths[i];
        }
    }

    private void drawRow(PDPageContentStream cs, float x, float y, float[] colWidths,
                          int num, Cours cours, Progression prog, Color bg) throws IOException {
        float totalWidth = 0;
        for (float w : colWidths) totalWidth += w;

        // Row background
        cs.setNonStrokingColor(bg);
        cs.addRect(x, y - ROW_HEIGHT, totalWidth, ROW_HEIGHT);
        cs.fill();

        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.setNonStrokingColor(new Color(44, 62, 80));

        float cx = x + 5;

        // #
        drawCell(cs, cx, y, String.valueOf(num));
        cx += colWidths[0];

        // Titre (truncate if too long)
        String titre = cours.getTitre();
        if (titre != null && titre.length() > 40) titre = titre.substring(0, 37) + "...";
        drawCell(cs, cx, y, titre != null ? titre : "");
        cx += colWidths[1];

        // Niveau
        String niveau = cours.getNiveau() != null ? cours.getNiveau().getDisplayName() : "-";
        drawCell(cs, cx, y, niveau);
        cx += colWidths[2];

        // XP
        drawCell(cs, cx, y, String.valueOf(cours.getPointsXP()));
        cx += colWidths[3];

        // Date complété
        String dateStr = "-";
        if (prog != null && prog.getDateObtention() != null) {
            dateStr = prog.getDateObtention().format(DATE_FMT);
        }
        drawCell(cs, cx, y, dateStr);
    }

    private void drawCell(PDPageContentStream cs, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y - ROW_HEIGHT + 7);
        cs.showText(text);
        cs.endText();
    }
}
