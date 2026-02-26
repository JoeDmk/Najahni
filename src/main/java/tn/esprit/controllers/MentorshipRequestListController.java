package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.models.MentorshipRequest;
import tn.esprit.services.ServiceMentorshipRequest;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MentorshipRequestListController implements Initializable {

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnChatbot;
    @FXML
    private Button btnAvailability;
    @FXML
    private Button btnSort;
    @FXML
    private TextField searchField;

    private ServiceMentorshipRequest service;
    private ObservableList<MentorshipRequest> masterList;
    private MentorshipRequest selectedRequest = null;
    private VBox selectedCard = null;
    private int currentStatusIndex = 0;
    private final MentorshipRequest.RequestStatus[] statuses = MentorshipRequest.RequestStatus.values();

    // Status colors
    private static final String COLOR_AUTO_ACCEPTED = "#22c55e"; // green
    private static final String COLOR_PENDING_REVIEW = "#eab308"; // yellow
    private static final String COLOR_REJECTED = "#ef4444";       // red
    private static final String COLOR_CANCELLED = "#f97316";      // orange
    private static final String COLOR_COMPLETED = "#3b82f6";      // blue

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipRequest();

        // Style the scroll pane
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        loadData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
        });

        btnAdd.setOnAction(e -> navigateToForm(null));
        btnEdit.setOnAction(e -> {
            if (selectedRequest != null) {
                navigateToForm(selectedRequest);
            } else {
                showAlert("Select a Request card to edit");
            }
        });

        btnDelete.setOnAction(e -> {
            if (selectedRequest != null) {
                service.delete(selectedRequest.getId());
                selectedRequest = null;
                selectedCard = null;
                loadData();
            } else {
                showAlert("Select a Request card to delete");
            }
        });

        btnSessions.setOnAction(e -> navigateTo("/FXML/MentorshipSessionList.fxml"));
        btnAvailability.setOnAction(e -> navigateTo("/FXML/MentorAvailabilityList.fxml"));
        btnChatbot.setOnAction(e -> handleChatbot());
        btnSort.setOnAction(e -> sortByStatus());
    }

    private void loadData() {
        masterList = FXCollections.observableArrayList(service.getAll());
        buildCards(masterList);
    }

    private void buildCards(ObservableList<MentorshipRequest> list) {
        cardsContainer.getChildren().clear();
        selectedRequest = null;
        selectedCard = null;

        for (MentorshipRequest request : list) {
            VBox card = createCard(request);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createCard(MentorshipRequest request) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.setPrefHeight(180);
        card.setPadding(new Insets(0));
        card.setAlignment(Pos.TOP_LEFT);

        // Get status color
        String statusColor = getStatusColor(request.getStatus());
        Color color = Color.web(statusColor);

        // Create the gradient background: status color on left fading to white
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1, 0.35)),
                new Stop(0.3, color.deriveColor(0, 0.5, 1, 0.12)),
                new Stop(1, Color.WHITE)
        );

        BackgroundFill bgFill = new BackgroundFill(gradient, new CornerRadii(14), Insets.EMPTY);
        card.setBackground(new Background(bgFill));

        // Border with status color accent on the left
        card.setBorder(new Border(
                new BorderStroke(color.deriveColor(0, 1, 1, 0.5),
                        BorderStrokeStyle.SOLID, new CornerRadii(14),
                        new BorderWidths(1, 1, 1, 4))
        ));

        // Drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setOffsetY(4);
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        card.setEffect(shadow);

        // Content padding wrapper
        VBox content = new VBox(6);
        content.setPadding(new Insets(16, 18, 16, 18));

        // Status badge with trig function wave decoration
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        // Trig wave SVG decoration for the status
        SVGPath wave = createTrigWave();
        wave.setFill(color.deriveColor(0, 1, 1, 0.3));
        wave.setScaleX(0.6);
        wave.setScaleY(0.6);

        Label statusBadge = new Label(formatStatus(request.getStatus()));
        statusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        statusBadge.setTextFill(Color.WHITE);
        statusBadge.setPadding(new Insets(3, 10, 3, 10));
        statusBadge.setStyle("-fx-background-color: " + statusColor + ";"
                + "-fx-background-radius: 12;");

        statusRow.getChildren().addAll(wave, statusBadge);

        // Title: Entrepreneur → Mentor
        Label titleLabel = new Label(
                (request.getEntrepreneurName() != null ? request.getEntrepreneurName() : "Entrepreneur #" + request.getEntrepreneurId())
                        + "  →  "
                        + (request.getMentorName() != null ? request.getMentorName() : "Mentor #" + request.getMentorId()));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#1f2937"));
        titleLabel.setWrapText(true);

        // Project
        Label projectLabel = new Label("📁 " + (request.getProjectName() != null ? request.getProjectName() : "Project #" + request.getProjectId()));
        projectLabel.setFont(Font.font("Segoe UI", 12));
        projectLabel.setTextFill(Color.web("#6b7280"));

        // Date & Time row
        HBox dateTimeRow = new HBox(15);
        dateTimeRow.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label("📅 " + (request.getDate() != null ? request.getDate().toString() : "N/A"));
        dateLabel.setFont(Font.font("Segoe UI", 12));
        dateLabel.setTextFill(Color.web("#6b7280"));

        Label timeLabel = new Label("🕐 " + (request.getTime() != null ? request.getTime() : "N/A"));
        timeLabel.setFont(Font.font("Segoe UI", 12));
        timeLabel.setTextFill(Color.web("#6b7280"));

        dateTimeRow.getChildren().addAll(dateLabel, timeLabel);

        // Match score bar
        HBox scoreRow = new HBox(8);
        scoreRow.setAlignment(Pos.CENTER_LEFT);

        Label scoreLabel = new Label("Match: " + String.format("%.0f%%", request.getMatchScore()));
        scoreLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        scoreLabel.setTextFill(color);

        ProgressBar scoreBar = new ProgressBar(request.getMatchScore() / 100.0);
        scoreBar.setPrefWidth(100);
        scoreBar.setPrefHeight(6);
        scoreBar.setStyle("-fx-accent: " + statusColor + ";");

        scoreRow.getChildren().addAll(scoreLabel, scoreBar);

        content.getChildren().addAll(statusRow, titleLabel, projectLabel, dateTimeRow, scoreRow);
        card.getChildren().add(content);

        // Hover effect
        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(18);
            hoverShadow.setOffsetY(6);
            hoverShadow.setColor(color.deriveColor(0, 1, 1, 0.25));
            card.setEffect(hoverShadow);
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });
        card.setOnMouseExited(e -> {
            card.setEffect(shadow);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Click to select
        card.setOnMouseClicked(e -> {
            // Deselect previous
            if (selectedCard != null) {
                MentorshipRequest prev = selectedRequest;
                String prevColor = getStatusColor(prev.getStatus());
                Color pColor = Color.web(prevColor);
                selectedCard.setBorder(new Border(
                        new BorderStroke(pColor.deriveColor(0, 1, 1, 0.5),
                                BorderStrokeStyle.SOLID, new CornerRadii(14),
                                new BorderWidths(1, 1, 1, 4))
                ));
            }

            // Select this card
            selectedRequest = request;
            selectedCard = card;
            card.setBorder(new Border(
                    new BorderStroke(color,
                            BorderStrokeStyle.SOLID, new CornerRadii(14),
                            new BorderWidths(2, 2, 2, 5))
            ));

            // Double-click for popup details
            if (e.getClickCount() == 2) {
                showDetailsPopup(request);
            }
        });

        card.setCursor(javafx.scene.Cursor.HAND);

        return card;
    }

    /**
     * Creates a sine-wave SVG path that decorates the status badge area.
     */
    private SVGPath createTrigWave() {
        SVGPath path = new SVGPath();
        // A small sine wave path
        StringBuilder sb = new StringBuilder();
        sb.append("M 0 10 ");
        for (int x = 0; x <= 40; x++) {
            double y = 10 + 6 * Math.sin(Math.toRadians(x * 18)); // sin wave
            sb.append("L ").append(x).append(" ").append(String.format("%.1f", y)).append(" ");
        }
        path.setContent(sb.toString());
        path.setStrokeWidth(2);
        path.setStroke(path.getFill());
        return path;
    }

    private void showDetailsPopup(MentorshipRequest request) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        String statusColor = getStatusColor(request.getStatus());
        Color color = Color.web(statusColor);

        VBox popupRoot = new VBox(0);
        popupRoot.setStyle("-fx-background-color: transparent;");
        popupRoot.setPadding(new Insets(20));
        popupRoot.setMaxWidth(500);

        // Main content card
        VBox card = new VBox(14);
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.TOP_LEFT);

        LinearGradient popupGradient = new LinearGradient(
                0, 0, 1, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1, 0.2)),
                new Stop(0.4, Color.WHITE),
                new Stop(1, Color.WHITE)
        );
        card.setBackground(new Background(new BackgroundFill(popupGradient, new CornerRadii(18), Insets.EMPTY)));
        card.setBorder(new Border(
                new BorderStroke(color.deriveColor(0, 1, 1, 0.4),
                        BorderStrokeStyle.SOLID, new CornerRadii(18),
                        new BorderWidths(1, 1, 1, 5))
        ));

        DropShadow popupShadow = new DropShadow();
        popupShadow.setRadius(30);
        popupShadow.setOffsetY(10);
        popupShadow.setColor(Color.rgb(0, 0, 0, 0.2));
        card.setEffect(popupShadow);

        // Header with status and close button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label(formatStatus(request.getStatus()));
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setPadding(new Insets(5, 14, 5, 14));
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 14;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 18; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popup.close());

        header.getChildren().addAll(statusLabel, spacer, closeBtn);

        // Trig wave as a decorative separator
        SVGPath decorWave = createTrigWave();
        decorWave.setFill(color.deriveColor(0, 1, 1, 0.15));
        decorWave.setStroke(color.deriveColor(0, 1, 1, 0.3));
        decorWave.setScaleX(3);
        decorWave.setScaleY(1.2);

        HBox waveContainer = new HBox(decorWave);
        waveContainer.setAlignment(Pos.CENTER);
        waveContainer.setPadding(new Insets(2, 0, 2, 0));

        // Title
        Label titleLabel = new Label("Mentorship Request #" + request.getId());
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#1f2937"));

        // Details grid
        VBox details = new VBox(10);

        details.getChildren().addAll(
                createDetailRow("👤 Entrepreneur",
                        request.getEntrepreneurName() != null ? request.getEntrepreneurName() : "ID: " + request.getEntrepreneurId()),
                createDetailRow("🎓 Mentor",
                        request.getMentorName() != null ? request.getMentorName() : "ID: " + request.getMentorId()),
                createDetailRow("📁 Project",
                        request.getProjectName() != null ? request.getProjectName() : "ID: " + request.getProjectId()),
                createDetailRow("📅 Date",
                        request.getDate() != null ? request.getDate().toString() : "N/A"),
                createDetailRow("🕐 Time",
                        request.getTime() != null ? request.getTime() : "N/A"),
                createDetailRow("📊 Match Score",
                        String.format("%.1f%%", request.getMatchScore())),
                createDetailRow("✅ Auto Approved",
                        request.isAutoApproved() ? "Yes" : "No"),
                createDetailRow("💡 Motivation",
                        request.getMotivation() != null ? request.getMotivation() : "N/A"),
                createDetailRow("🎯 Goals",
                        request.getGoals() != null ? request.getGoals() : "N/A"),
                createDetailRow("🕓 Created At",
                        request.getCreatedAt() != null ? request.getCreatedAt().toString() : "N/A")
        );

        // Action buttons row
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button editBtn = new Button("✏ Edit");
        editBtn.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            popup.close();
            navigateToForm(request);
        });

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; "
                + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #fca5a5; -fx-border-radius: 8;");
        deleteBtn.setOnAction(e -> {
            popup.close();
            service.delete(request.getId());
            selectedRequest = null;
            selectedCard = null;
            loadData();
        });

        actions.getChildren().addAll(deleteBtn, editBtn);

        card.getChildren().addAll(header, waveContainer, titleLabel, details, actions);
        popupRoot.getChildren().add(card);

        Scene scene = new Scene(popupRoot, 500, 560);
        scene.setFill(Color.TRANSPARENT);

        popup.setScene(scene);
        popup.setTitle("Request Details");
        popup.showAndWait();
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label keyLabel = new Label(label);
        keyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        keyLabel.setTextFill(Color.web("#6b7280"));
        keyLabel.setMinWidth(140);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", 13));
        valueLabel.setTextFill(Color.web("#1f2937"));
        valueLabel.setWrapText(true);

        row.getChildren().addAll(keyLabel, valueLabel);
        return row;
    }

    private String getStatusColor(MentorshipRequest.RequestStatus status) {
        if (status == null) return "#9ca3af"; // gray fallback
        switch (status) {
            case auto_accepted:
                return COLOR_AUTO_ACCEPTED;
            case pending_review:
                return COLOR_PENDING_REVIEW;
            case rejected:
                return COLOR_REJECTED;
            case cancelled:
                return COLOR_CANCELLED;
            case completed:
                return COLOR_COMPLETED;
            default:
                return "#9ca3af";
        }
    }

    private String formatStatus(MentorshipRequest.RequestStatus status) {
        if (status == null) return "Unknown";
        String label = status.name().replace("_", " ");
        // Capitalize each word
        String[] words = label.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return sb.toString();
    }

    private void sortByStatus() {
        currentStatusIndex++;
        if (currentStatusIndex > statuses.length) {
            currentStatusIndex = 0;
        }

        if (currentStatusIndex == 0) {
            buildCards(masterList);
            btnSort.setText("Status: All");
        } else {
            MentorshipRequest.RequestStatus selectedStatus = statuses[currentStatusIndex - 1];
            ObservableList<MentorshipRequest> filteredList = FXCollections.observableArrayList();
            for (MentorshipRequest request : masterList) {
                if (request.getStatus() == selectedStatus) {
                    filteredList.add(request);
                }
            }
            buildCards(filteredList);
            String label = formatStatus(selectedStatus);
            btnSort.setText("Status: " + label);
        }
    }

    private void filterData(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            buildCards(masterList);
            return;
        }

        ObservableList<MentorshipRequest> filteredList = FXCollections.observableArrayList();
        for (MentorshipRequest request : masterList) {
            boolean matches = false;
            if (request.getMentorName() != null
                    && request.getMentorName().toLowerCase().contains(keyword.toLowerCase())) {
                matches = true;
            }
            if (request.getEntrepreneurName() != null
                    && request.getEntrepreneurName().toLowerCase().contains(keyword.toLowerCase())) {
                matches = true;
            }
            if (matches) {
                filteredList.add(request);
            }
        }
        buildCards(filteredList);
    }

    private void navigateToForm(MentorshipRequest request) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MentorshipRequestForm.fxml"));
            Parent root = loader.load();

            if (request != null) {
                MentorshipRequestFormController controller = loader.getController();
                controller.setMentorshipRequest(request);
            }

            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChatbot() {
        navigateTo("/FXML/Chatbot.fxml");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }
}
