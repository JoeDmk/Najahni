package services;

import models.User;
import tools.MyConnection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting user data to CSV and PDF.
 */
public class ExportService {

    private static ExportService instance;

    private ExportService() {}

    public static ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    /**
     * Export users to CSV file.
     */
    public File exportToCSV(List<User> users, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // BOM for UTF-8 Excel compatibility
            writer.write('\ufeff');

            // Header
            writer.write("ID,Prénom,Nom,Email,Téléphone,Rôle,Statut,Vérifié,Entreprise,Adresse,Date de naissance,Créé le\n");

            // Data rows
            for (User user : users) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        user.getId(),
                        escapeCSV(user.getFirstname()),
                        escapeCSV(user.getLastname()),
                        escapeCSV(user.getEmail()),
                        escapeCSV(user.getPhone() != null ? user.getPhone() : ""),
                        user.getRole().name(),
                        user.getIsActive() ? "Actif" : "Inactif",
                        user.isVerified() ? "Oui" : "Non",
                        escapeCSV(user.getCompanyName() != null ? user.getCompanyName() : ""),
                        escapeCSV(user.getAddress() != null ? user.getAddress() : ""),
                        user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "",
                        user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
                ));
            }
        }
        return file;
    }

    /**
     * Export users to PDF file using iTextPDF.
     */
    public File exportToPDF(List<User> users, File file) throws Exception {
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(file);
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc,
                com.itextpdf.kernel.geom.PageSize.A4.rotate());

        // Title
        document.add(new com.itextpdf.layout.element.Paragraph("Najahni - Liste des Utilisateurs")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5));

        // Date
        document.add(new com.itextpdf.layout.element.Paragraph(
                "Exporté le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                .setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(20));

        // Table
        float[] columnWidths = {40, 80, 80, 150, 90, 80, 60, 60};
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(columnWidths);
        table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Header row
        String[] headers = {"ID", "Prénom", "Nom", "Email", "Téléphone", "Rôle", "Statut", "Vérifié"};
        for (String header : headers) {
            com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(header).setBold().setFontSize(9))
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(108, 99, 255))
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                    .setPadding(6);
            table.addHeaderCell(cell);
        }

        // Data rows
        boolean alternate = false;
        for (User user : users) {
            com.itextpdf.kernel.colors.Color bgColor = alternate
                    ? new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 250)
                    : com.itextpdf.kernel.colors.ColorConstants.WHITE;

            addCell(table, String.valueOf(user.getId()), bgColor);
            addCell(table, user.getFirstname(), bgColor);
            addCell(table, user.getLastname(), bgColor);
            addCell(table, user.getEmail(), bgColor);
            addCell(table, user.getPhone() != null ? user.getPhone() : "-", bgColor);
            addCell(table, user.getRole().name(), bgColor);
            addCell(table, user.getIsActive() ? "Actif" : "Inactif", bgColor);
            addCell(table, user.isVerified() ? "Oui" : "Non", bgColor);

            alternate = !alternate;
        }

        document.add(table);

        // Footer
        document.add(new com.itextpdf.layout.element.Paragraph("Total: " + users.size() + " utilisateurs")
                .setFontSize(10)
                .setMarginTop(15)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));

        document.close();
        return file;
    }

    // ==================== Helper Methods ====================

    private void addCell(com.itextpdf.layout.element.Table table, String text, com.itextpdf.kernel.colors.Color bgColor) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(text != null ? text : "-").setFontSize(8))
                .setBackgroundColor(bgColor)
                .setPadding(4);
        table.addCell(cell);
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
