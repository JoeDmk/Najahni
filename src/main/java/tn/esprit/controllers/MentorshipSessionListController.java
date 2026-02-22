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
import java.net.URL;
import java.util.ResourceBundle;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import javafx.stage.FileChooser;
import java.io.File;

public class MentorshipSessionListController implements Initializable {

    @FXML
    private TableView<MentorshipSession> tableView;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnExportPDF;
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
