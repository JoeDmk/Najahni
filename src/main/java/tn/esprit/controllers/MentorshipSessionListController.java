package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import tn.esprit.models.MentorshipSession;
import tn.esprit.services.ServiceMentorshipSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import javafx.stage.FileChooser;
import java.io.File;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class MentorshipSessionListController implements Initializable {

    @FXML
    private TableView<MentorshipSession> tableView;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnExportPDF;
    @FXML
    private Button btnExportExcel;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnAvailability;
    @FXML
    private Button btnChatbot;

    @FXML
    private DatePicker searchDate;

    private ServiceMentorshipSession service;
    private ObservableList<MentorshipSession> masterList;

    // ===== PASTE YOUR FREE GROQ API KEY HERE =====
    // Get it from: https://console.groq.com/keys (free signup)
    private static final String GROQ_API_KEY = "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";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipSession();
        loadData();

        // Listen for changes in DatePicker
        searchDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
        });

        btnAdd.setOnAction(e -> handleAdd());
        btnExportPDF.setOnAction(e -> handleExportPDF());
        btnExportExcel.setOnAction(e -> handleExportExcel());
        btnEdit.setOnAction(e -> handleEdit());
        btnDelete.setOnAction(e -> handleDelete());
        btnRequests.setOnAction(e -> handleRequests());
        btnAvailability.setOnAction(e -> handleAvailability());
        btnChatbot.setOnAction(e -> handleChatbot());
    }

    private void loadData() {
        masterList = FXCollections.observableArrayList(service.getAll());
        tableView.setItems(masterList);
    }

    private void filterData(java.time.LocalDate date) {
        if (date == null) {
            tableView.setItems(masterList);
            return;
        }

        ObservableList<MentorshipSession> filteredList = FXCollections.observableArrayList();
        for (MentorshipSession session : masterList) {
            if (session.getScheduledAt() != null) {
                java.time.LocalDate sessionDate = session.getScheduledAt().toLocalDateTime().toLocalDate();
                if (sessionDate.isEqual(date)) {
                    filteredList.add(session);
                }
            }
        }
        tableView.setItems(filteredList);
    }

    // ... Implement button actions (Add, Edit, Delete, Navigate) similar to
    // RequestListController
    // For brevity, skipping full implementation here, assuming user can copy
    // pattern or I can add if asked.
    // Wait, the user asked for full CRUD. I MUST implement them.

    @FXML
    private void handleAdd() {
        navigateToForm(null);
    }

    @FXML
    private void handleEdit() {
        MentorshipSession selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null)
            navigateToForm(selected);
        else
            showAlert("Select a session");
    }

    @FXML
    private void handleDelete() {
        MentorshipSession selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.delete(selected.getId());
            loadData();
        } else
            showAlert("Select a session");
    }

    @FXML
    private void handleRequests() {
        navigateTo("/FXML/MentorshipRequestList.fxml");
    }

    @FXML
    private void handleAvailability() {
        navigateTo("/FXML/MentorAvailabilityList.fxml");
    }

    private void navigateToForm(MentorshipSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MentorshipSessionForm.fxml"));
            Parent root = loader.load();
            if (session != null) {
                MentorshipSessionFormController controller = loader.getController();
                controller.setSession(session);
            }
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChatbot() {
        navigateTo("/FXML/Chatbot.fxml");
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("mentorship_sessions.xlsx");
        File file = fileChooser.showSaveDialog(tableView.getScene().getWindow());
        if (file == null) return;

        // Disable button and show progress
        btnExportExcel.setDisable(true);
        btnExportExcel.setText("Exporting...");

        // Run in background thread to avoid freezing UI during AI call
        new Thread(() -> {
            try (Workbook workbook = new XSSFWorkbook()) {

                // ========== SHEET 1: Session Data ==========
                Sheet dataSheet = workbook.createSheet("Sessions");

                // Header style
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 12);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                // Data style
                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);
                dataStyle.setWrapText(true);

                // Title row
                Row titleRow = dataSheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Mentorship Sessions Report");
                CellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 16);
                titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
                titleStyle.setFont(titleFont);
                titleCell.setCellStyle(titleStyle);
                dataSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

                // Date row
                Row dateRow = dataSheet.createRow(1);
                Cell dateCell = dateRow.createCell(0);
                dateCell.setCellValue("Generated on: " + java.time.LocalDate.now());

                // Headers (row 3)
                String[] headers = {"ID", "Request ID", "Scheduled At", "Duration (min)",
                        "Status", "Meeting Link", "Mentor Feedback", "Entrepreneur Feedback",
                        "Mentor Rating", "Entrepreneur Rating", "Created At"};
                Row headerRow = dataSheet.createRow(3);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Data rows
                ObservableList<MentorshipSession> items = tableView.getItems();
                int rowNum = 4;
                for (MentorshipSession s : items) {
                    Row row = dataSheet.createRow(rowNum++);
                    createStyledCell(row, 0, String.valueOf(s.getId()), dataStyle);
                    createStyledCell(row, 1, String.valueOf(s.getRequestId()), dataStyle);
                    createStyledCell(row, 2, s.getScheduledAt() != null ? s.getScheduledAt().toString() : "", dataStyle);
                    createStyledCell(row, 3, String.valueOf(s.getDurationMinutes()), dataStyle);
                    createStyledCell(row, 4, s.getStatus() != null ? s.getStatus().toString() : "", dataStyle);
                    createStyledCell(row, 5, s.getMeetingLink() != null ? s.getMeetingLink() : "", dataStyle);
                    createStyledCell(row, 6, s.getMentorFeedback() != null ? s.getMentorFeedback() : "", dataStyle);
                    createStyledCell(row, 7, s.getEntrepreneurFeedback() != null ? s.getEntrepreneurFeedback() : "", dataStyle);
                    createStyledCell(row, 8, String.valueOf(s.getMentorRating()), dataStyle);
                    createStyledCell(row, 9, String.valueOf(s.getEntrepreneurRating()), dataStyle);
                    createStyledCell(row, 10, s.getCreatedAt() != null ? s.getCreatedAt().toString() : "", dataStyle);
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    dataSheet.autoSizeColumn(i);
                }

                // ========== SHEET 2: AI Insights ==========
                Sheet insightsSheet = workbook.createSheet("AI Insights");

                // Build a data summary to send to the AI
                StringBuilder dataSummary = new StringBuilder();
                dataSummary.append("Here is the mentorship session data. Analyze it and provide insights:\n");
                dataSummary.append("Total sessions: ").append(items.size()).append("\n");

                int scheduled = 0, completed = 0, cancelled = 0, noShow = 0;
                int totalMentorRating = 0, totalEntrepreneurRating = 0, ratedCount = 0;
                int totalDuration = 0;
                StringBuilder feedbackList = new StringBuilder();

                for (MentorshipSession s : items) {
                    if (s.getStatus() != null) {
                        switch (s.getStatus()) {
                            case scheduled -> scheduled++;
                            case completed -> completed++;
                            case cancelled -> cancelled++;
                            case no_show -> noShow++;
                        }
                    }
                    totalDuration += s.getDurationMinutes();
                    if (s.getMentorRating() > 0 || s.getEntrepreneurRating() > 0) {
                        totalMentorRating += s.getMentorRating();
                        totalEntrepreneurRating += s.getEntrepreneurRating();
                        ratedCount++;
                    }
                    if (s.getMentorFeedback() != null && !s.getMentorFeedback().isEmpty()) {
                        feedbackList.append("Mentor feedback: ").append(s.getMentorFeedback()).append("\n");
                    }
                    if (s.getEntrepreneurFeedback() != null && !s.getEntrepreneurFeedback().isEmpty()) {
                        feedbackList.append("Entrepreneur feedback: ").append(s.getEntrepreneurFeedback()).append("\n");
                    }
                }

                dataSummary.append("Status breakdown - Scheduled: ").append(scheduled)
                        .append(", Completed: ").append(completed)
                        .append(", Cancelled: ").append(cancelled)
                        .append(", No-show: ").append(noShow).append("\n");
                dataSummary.append("Total duration: ").append(totalDuration).append(" minutes\n");
                if (ratedCount > 0) {
                    dataSummary.append("Avg mentor rating: ").append(String.format("%.1f", (double) totalMentorRating / ratedCount)).append("/5\n");
                    dataSummary.append("Avg entrepreneur rating: ").append(String.format("%.1f", (double) totalEntrepreneurRating / ratedCount)).append("/5\n");
                }
                if (feedbackList.length() > 0) {
                    dataSummary.append("\nFeedback entries:\n").append(feedbackList);
                }
                dataSummary.append("\nPlease provide: 1) A summary of the data, 2) Key observations, 3) Recommendations for improvement, 4) Any concerning patterns.");

                // Call AI
                String aiInsights = callAIForInsights(dataSummary.toString());

                // Insights sheet title
                Row insightsTitleRow = insightsSheet.createRow(0);
                Cell insightsTitleCell = insightsTitleRow.createCell(0);
                insightsTitleCell.setCellValue("AI-Generated Insights");
                insightsTitleCell.setCellStyle(titleStyle);
                insightsSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

                Row insightsDateRow = insightsSheet.createRow(1);
                insightsDateRow.createCell(0).setCellValue("Generated on: " + java.time.LocalDate.now());

                // Stats summary section
                Row statsHeaderRow = insightsSheet.createRow(3);
                Cell statsHeaderCell = statsHeaderRow.createCell(0);
                statsHeaderCell.setCellValue("Quick Statistics");
                CellStyle subHeaderStyle = workbook.createCellStyle();
                Font subHeaderFont = workbook.createFont();
                subHeaderFont.setBold(true);
                subHeaderFont.setFontHeightInPoints((short) 13);
                subHeaderFont.setColor(IndexedColors.DARK_BLUE.getIndex());
                subHeaderStyle.setFont(subHeaderFont);
                statsHeaderCell.setCellStyle(subHeaderStyle);

                insightsSheet.createRow(4).createCell(0).setCellValue("Total Sessions: " + items.size());
                insightsSheet.createRow(5).createCell(0).setCellValue("Scheduled: " + scheduled + " | Completed: " + completed + " | Cancelled: " + cancelled + " | No-show: " + noShow);
                insightsSheet.createRow(6).createCell(0).setCellValue("Total Duration: " + totalDuration + " minutes");
                if (ratedCount > 0) {
                    insightsSheet.createRow(7).createCell(0).setCellValue(
                            "Avg Mentor Rating: " + String.format("%.1f", (double) totalMentorRating / ratedCount)
                            + "/5 | Avg Entrepreneur Rating: " + String.format("%.1f", (double) totalEntrepreneurRating / ratedCount) + "/5");
                }

                // AI Analysis section
                Row aiHeaderRow = insightsSheet.createRow(9);
                Cell aiHeaderCell = aiHeaderRow.createCell(0);
                aiHeaderCell.setCellValue("AI Analysis");
                aiHeaderCell.setCellStyle(subHeaderStyle);

                // Write AI insights line by line
                String[] insightsLines = aiInsights.split("\n");
                int insightRowNum = 10;
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                for (String line : insightsLines) {
                    Row insightRow = insightsSheet.createRow(insightRowNum++);
                    Cell insightCell = insightRow.createCell(0);
                    insightCell.setCellValue(line);
                    insightCell.setCellStyle(wrapStyle);
                }

                // Set column widths for insights sheet
                insightsSheet.setColumnWidth(0, 20000);

                // Write to file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }

                javafx.application.Platform.runLater(() -> {
                    showAlert("Excel exported successfully with AI insights!");
                    btnExportExcel.setDisable(false);
                    btnExportExcel.setText("Export Excel");
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showAlert("Error exporting Excel: " + e.getMessage());
                    btnExportExcel.setDisable(false);
                    btnExportExcel.setText("Export Excel");
                });
            }
        }).start();
    }

    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String callAIForInsights(String dataPrompt) {
        try {
            // Groq API endpoint (OpenAI-compatible, free tier)
            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            // Escape the prompt for JSON
            String safePrompt = dataPrompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                    .replace("\t", "\\t");

            // Build Groq API request body (OpenAI-compatible format)
            String jsonBody = "{\"model\": \"llama-3.3-70b-versatile\", \"messages\": [{\"role\": \"user\", \"content\": \"" + safePrompt + "\"}]}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (Scanner scanner = new Scanner(conn.getInputStream(), "utf-8")) {
                    String response = scanner.useDelimiter("\\A").next();

                    // Parse Groq response - extract content from:
                    // {"choices":[{"message":{"content":"..."}}]}
                    int contentStart = response.indexOf("\"content\":\"");
                    if (contentStart != -1) {
                        contentStart += 11; // skip past "content":"
                        // Find the closing quote, accounting for escaped quotes
                        StringBuilder extracted = new StringBuilder();
                        boolean escaped = false;
                        for (int i = contentStart; i < response.length(); i++) {
                            char c = response.charAt(i);
                            if (escaped) {
                                if (c == 'n') extracted.append('\n');
                                else if (c == 't') extracted.append('\t');
                                else if (c == '"') extracted.append('"');
                                else if (c == '\\') extracted.append('\\');
                                else extracted.append(c);
                                escaped = false;
                            } else if (c == '\\') {
                                escaped = true;
                            } else if (c == '"') {
                                break; // end of content value
                            } else {
                                extracted.append(c);
                            }
                        }
                        return extracted.toString();
                    }
                    return "Could not parse AI response.";
                }
            } else {
                // Read error response for more details
                try (Scanner scanner = new Scanner(conn.getErrorStream(), "utf-8")) {
                    String errorResponse = scanner.useDelimiter("\\A").next();
                    System.err.println("Groq API error: " + errorResponse);
                }
                if (code == 401) return "Groq API error (401): Invalid API key.\nGet a free key at: https://console.groq.com/keys";
                if (code == 429) return "Groq API error (429): Rate limit exceeded. Try again in a moment.";
                return "Groq API returned error code: " + code;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not connect to Groq API.\n\nError: " + e.getMessage()
                    + "\n\nMake sure you have a valid API key and internet connection.";
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.show();
    }

    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(tableView.getScene().getWindow());
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                document.add(new Paragraph("Mentorship Sessions List"));
                document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()));
                document.add(new Paragraph(" ")); // Empty line

                // Create table with 11 columns
                PdfPTable pdfTable = new PdfPTable(11);
                pdfTable.setWidthPercentage(100);

                // Add Headers
                pdfTable.addCell("ID");
                pdfTable.addCell("Req ID");
                pdfTable.addCell("Scheduled");
                pdfTable.addCell("Dur");
                pdfTable.addCell("Status");
                pdfTable.addCell("Link");
                pdfTable.addCell("M. Feed");
                pdfTable.addCell("E. Feed");
                pdfTable.addCell("M. Rate");
                pdfTable.addCell("E. Rate");
                pdfTable.addCell("Created");

                // Add Data
                for (MentorshipSession s : tableView.getItems()) {
                    pdfTable.addCell(String.valueOf(s.getId()));
                    pdfTable.addCell(String.valueOf(s.getRequestId()));
                    pdfTable.addCell(s.getScheduledAt() != null ? s.getScheduledAt().toString() : "");
                    pdfTable.addCell(String.valueOf(s.getDurationMinutes()));
                    pdfTable.addCell(s.getStatus() != null ? s.getStatus().toString() : "");
                    pdfTable.addCell(s.getMeetingLink() != null ? s.getMeetingLink() : "");
                    pdfTable.addCell(s.getMentorFeedback() != null ? s.getMentorFeedback() : "");
                    pdfTable.addCell(s.getEntrepreneurFeedback() != null ? s.getEntrepreneurFeedback() : "");
                    pdfTable.addCell(String.valueOf(s.getMentorRating()));
                    pdfTable.addCell(String.valueOf(s.getEntrepreneurRating()));
                    pdfTable.addCell(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
                }

                document.add(pdfTable);
                document.close();

                showAlert("PDF exported successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error exporting PDF: " + e.getMessage());
            }
        }
    }
}
