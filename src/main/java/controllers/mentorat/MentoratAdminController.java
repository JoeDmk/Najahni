package controllers.mentorat;

import javafx.animation.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import models.mentorat.MentorAvailability;
import models.mentorat.MentorshipRequest;
import models.mentorat.MentorshipSession;
import services.SessionService;
import services.mentorat.MentoratNotificationService;
import services.mentorat.ServiceMentorAvailability;
import services.mentorat.ServiceMentorshipRequest;
import services.mentorat.ServiceMentorshipSession;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Admin back-office controller for the Mentorat module.
 * Full CRUD with input validation and uniqueness checks.
 */
public class MentoratAdminController {

    private static final Logger LOG = Logger.getLogger(MentoratAdminController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private StackPane rootStack;
    @FXML private Button tabDashboard, tabRequests, tabSessions, tabAvailability;
    @FXML private StackPane contentArea;

    private final ServiceMentorshipRequest requestService = new ServiceMentorshipRequest();
    private final ServiceMentorshipSession sessionService = new ServiceMentorshipSession();
    private final ServiceMentorAvailability availabilityService = new ServiceMentorAvailability();
    private final MentoratNotificationService notificationService = new MentoratNotificationService();

    private Button activeTab;
    private int currentUserId;

    @FXML
    public void initialize() {
        try { currentUserId = SessionService.getInstance().getCurrentUser().getId(); }
        catch (Exception e) { currentUserId = 0; }
        showDashboard();
    }

    // ====== TAB NAVIGATION ======

    @FXML private void showDashboard()    { setActiveTab(tabDashboard);    loadDashboard(); }
    @FXML private void showRequests()     { setActiveTab(tabRequests);     loadRequestsTable(); }
    @FXML private void showSessions()     { setActiveTab(tabSessions);     loadSessionsTable(); }
    @FXML private void showAvailability() { setActiveTab(tabAvailability); loadAvailabilityTable(); }

    private void setActiveTab(Button tab) {
        if (activeTab != null) activeTab.getStyleClass().remove("mentorat-admin-tab-active");
        activeTab = tab;
        if (activeTab != null && !activeTab.getStyleClass().contains("mentorat-admin-tab-active")) {
            activeTab.getStyleClass().add("mentorat-admin-tab-active");
        }
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setToValue(1);
        ft.play();
    }

    // ====== DASHBOARD ======

    private void loadDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Tableau de Bord");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#2c3e50"));

        Label subtitle = new Label("Vue d'ensemble du module mentorat");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#7f8c8d"));

        HBox statsRow = new HBox(14);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        List<MentorshipRequest> allRequests = requestService.getAll();
        List<MentorshipSession> allSessions = sessionService.getAll();
        List<MentorAvailability> allAvail = availabilityService.getAll();

        long pendingCount = allRequests.stream()
                .filter(r -> r.getStatus() == MentorshipRequest.RequestStatus.pending_review)
                .count();
        long completedSessions = allSessions.stream()
                .filter(s -> s.getStatus() == MentorshipSession.SessionStatus.completed)
                .count();

        statsRow.getChildren().addAll(
                buildStatCard("Demandes", allRequests.size(), "#667eea"),
                buildStatCard("En attente", (int) pendingCount, "#e67e22"),
                buildStatCard("Sessions", allSessions.size(), "#27ae60"),
                buildStatCard("Completees", (int) completedSessions, "#8e44ad"),
                buildStatCard("Disponibilites", allAvail.size(), "#e74c3c")
        );

        for (int i = 0; i < statsRow.getChildren().size(); i++) {
            javafx.scene.Node card = statsRow.getChildren().get(i);
            card.setOpacity(0);
            card.setTranslateY(15);
            PauseTransition pause = new PauseTransition(Duration.millis(i * 60));
            pause.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(250), card);
                fade.setToValue(1);
                TranslateTransition slide = new TranslateTransition(Duration.millis(250), card);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(fade, slide).play();
            });
            pause.play();
        }

        VBox recentSection = new VBox(10);
        recentSection.setPadding(new Insets(12, 0, 0, 0));

        Label recentTitle = new Label("Demandes Recentes");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        recentTitle.setTextFill(Color.web("#2c3e50"));

        VBox activityList = new VBox(6);
        int limit = Math.min(5, allRequests.size());
        for (int i = allRequests.size() - 1; i >= Math.max(0, allRequests.size() - limit); i--) {
            MentorshipRequest r = allRequests.get(i);
            String entName = r.getEntrepreneurName() != null ? r.getEntrepreneurName() : "Entrepreneur #" + r.getEntrepreneurId();
            String menName = r.getMentorName() != null ? r.getMentorName() : "Mentor #" + r.getMentorId();
            String date = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDateTime().format(DATE_FMT) : "-";

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label msgLbl = new Label(entName + " -> " + menName);
            msgLbl.setFont(Font.font("Segoe UI", 12));
            msgLbl.setTextFill(Color.web("#2c3e50"));

            Label statusBadge = new Label(r.getStatus() != null ? r.getStatus().toString() : "unknown");
            statusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            String badgeColor = getStatusColor(r.getStatus());
            statusBadge.setStyle("-fx-background-color: " + badgeColor + "20; -fx-text-fill: " + badgeColor + "; "
                    + "-fx-background-radius: 4; -fx-padding: 2 8;");

            Label metaLbl = new Label(date + (r.getProjectName() != null ? " | Projet: " + r.getProjectName() : ""));
            metaLbl.setFont(Font.font("Segoe UI", 10));
            metaLbl.setTextFill(Color.web("#95a5a6"));
            info.getChildren().addAll(msgLbl, metaLbl);

            row.getChildren().addAll(info, statusBadge);
            activityList.getChildren().add(row);
        }

        recentSection.getChildren().addAll(recentTitle, activityList);
        dashboard.getChildren().addAll(title, subtitle, statsRow, buildChartsSection(allRequests, allSessions), recentSection);
        scroll.setContent(dashboard);
        setContent(scroll);
    }

    private VBox buildStatCard(String label, int value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(130);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); "
                + "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1.5;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLbl = new Label(String.valueOf(value));
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        valueLbl.setTextFill(Color.web(color));
        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        labelLbl.setTextFill(Color.web("#7f8c8d"));

        card.getChildren().addAll(valueLbl, labelLbl);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + color + "08; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, " + color + "22, 12, 0, 0, 3); "
                + "-fx-border-color: " + color + "55; -fx-border-radius: 12; -fx-border-width: 1.5;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); "
                + "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1.5;"));
        return card;
    }

    // ====== CHARTS SECTION (PieChart + BarChart) ======

    private HBox buildChartsSection(List<MentorshipRequest> allRequests, List<MentorshipSession> allSessions) {
        HBox chartsRow = new HBox(16);
        chartsRow.setAlignment(Pos.CENTER_LEFT);

        // --- PieChart: Request Status Distribution ---
        Map<String, Long> statusCounts = allRequests.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(r -> getStatusLabel(r.getStatus()), Collectors.counting()));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        String[] pieColors = {"#27ae60", "#e67e22", "#e74c3c", "#95a5a6", "#8e44ad"};
        int colorIdx = 0;
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            colorIdx++;
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setTitle("Repartition des demandes");
        pieChart.setLegendSide(Side.BOTTOM);
        pieChart.setLabelsVisible(false);
        pieChart.setPrefSize(340, 280);
        pieChart.setMaxSize(340, 280);
        pieChart.setStyle("-fx-font-family: 'Segoe UI';");

        // Apply colors
        for (int i = 0; i < pieData.size(); i++) {
            final int ci = i % pieColors.length;
            PieChart.Data slice = pieData.get(i);
            slice.getNode().setStyle("-fx-pie-color: " + pieColors[ci] + ";");
        }

        VBox pieBox = new VBox(4, pieChart);
        pieBox.setAlignment(Pos.TOP_CENTER);
        pieBox.setPadding(new Insets(12));
        pieBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        HBox.setHgrow(pieBox, Priority.ALWAYS);

        // --- BarChart: Sessions per Month (last 6 months) ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Mois");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Sessions");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Sessions par mois");
        barChart.setLegendVisible(false);
        barChart.setPrefSize(400, 280);
        barChart.setMaxSize(400, 280);
        barChart.setStyle("-fx-font-family: 'Segoe UI';");
        barChart.setCategoryGap(8);
        barChart.setBarGap(2);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sessions");

        // Count sessions per month for last 6 months
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = now.minusMonths(i);
            String monthLabel = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                    + " " + month.getYear();
            long count = allSessions.stream()
                    .filter(s -> s.getScheduledAt() != null)
                    .filter(s -> {
                        LocalDate d = s.getScheduledAt().toLocalDateTime().toLocalDate();
                        return YearMonth.from(d).equals(month);
                    })
                    .count();
            series.getData().add(new XYChart.Data<>(monthLabel, count));
        }
        barChart.getData().add(series);

        // Style bar colors
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: #667eea;");
            }
        }
        barChart.setAnimated(true);

        VBox barBox = new VBox(4, barChart);
        barBox.setAlignment(Pos.TOP_CENTER);
        barBox.setPadding(new Insets(12));
        barBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        HBox.setHgrow(barBox, Priority.ALWAYS);

        chartsRow.getChildren().addAll(pieBox, barBox);
        return chartsRow;
    }

    private String getStatusLabel(MentorshipRequest.RequestStatus status) {
        if (status == null) return "Inconnu";
        return switch (status) {
            case auto_accepted -> "Auto-acceptee";
            case pending_review -> "En attente";
            case rejected -> "Rejetee";
            case cancelled -> "Annulee";
            case completed -> "Completee";
        };
    }

    // ====== REQUESTS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadRequestsTable() {
        VBox container = buildTableContainer("Gestion des Demandes", "Gerer toutes les demandes de mentorat");

        List<MentorshipRequest> requests = requestService.getAll();
        ObservableList<MentorshipRequest> data = FXCollections.observableArrayList(requests);
        FilteredList<MentorshipRequest> filtered = new FilteredList<>(data, p -> true);

        // No Add button for requests — admin only edits status / deletes
        TextField search = createSearchField("Rechercher par nom, projet, statut...");
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase().trim();
            filtered.setPredicate(r -> q.isEmpty()
                    || (r.getEntrepreneurName() != null && r.getEntrepreneurName().toLowerCase().contains(q))
                    || (r.getMentorName() != null && r.getMentorName().toLowerCase().contains(q))
                    || (r.getProjectName() != null && r.getProjectName().toLowerCase().contains(q))
                    || (r.getStatus() != null && r.getStatus().toString().toLowerCase().contains(q)));
        });

        TableView<MentorshipRequest> table = createStyledTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<MentorshipRequest, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); colId.setMinWidth(50);

        TableColumn<MentorshipRequest, String> colEnt = new TableColumn<>("Entrepreneur");
        colEnt.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getEntrepreneurName() != null ? cd.getValue().getEntrepreneurName() : "#" + cd.getValue().getEntrepreneurId()));

        TableColumn<MentorshipRequest, String> colMentor = new TableColumn<>("Mentor");
        colMentor.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMentorName() != null ? cd.getValue().getMentorName() : "#" + cd.getValue().getMentorId()));

        TableColumn<MentorshipRequest, String> colProject = new TableColumn<>("Projet");
        colProject.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getProjectName() != null ? cd.getValue().getProjectName() : "-"));

        TableColumn<MentorshipRequest, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDate() != null ? cd.getValue().getDate().toString() : "-"));
        colDate.setMaxWidth(100); colDate.setMinWidth(100);

        TableColumn<MentorshipRequest, String> colScore = new TableColumn<>("Score");
        colScore.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%.0f%%", cd.getValue().getMatchScore() * 100)));
        colScore.setMaxWidth(70); colScore.setMinWidth(70);

        TableColumn<MentorshipRequest, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus() != null ? cd.getValue().getStatus().toString() : "-"));
        colStatus.setMaxWidth(120); colStatus.setMinWidth(120);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                MentorshipRequest.RequestStatus st = null;
                try { st = MentorshipRequest.RequestStatus.valueOf(item); } catch (Exception ignored) {}
                String c = getStatusColor(st);
                setStyle("-fx-text-fill: " + c + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<MentorshipRequest, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(170); colActions.setMinWidth(170);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);
            private final Button editBtn = createEditButton();
            private final Button delBtn = createDeleteButton();
            {
                editBtn.setOnAction(e -> {
                    MentorshipRequest r = getTableView().getItems().get(getIndex());
                    openRequestForm(r);
                });
                delBtn.setOnAction(e -> {
                    MentorshipRequest r = getTableView().getItems().get(getIndex());
                    confirmAndDelete("la demande #" + r.getId(), () -> {
                        requestService.delete(r.getId());
                        data.remove(r);
                        showToast("Demande supprimee");
                    });
                });
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(editBtn, delBtn);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(colId, colEnt, colMentor, colProject, colDate, colScore, colStatus, colActions);
        table.setItems(filtered);

        Label countLbl = createCountLabel(requests.size() + " demandes au total");
        container.getChildren().addAll(search, countLbl, table);
        setContent(container);
    }

    private void openRequestForm(MentorshipRequest existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Demande #" + existing.getId());
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(460);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        // Status dropdown
        ComboBox<String> statusCombo = new ComboBox<>();
        for (MentorshipRequest.RequestStatus s : MentorshipRequest.RequestStatus.values()) {
            statusCombo.getItems().add(s.toString());
        }
        if (existing.getStatus() != null) {
            statusCombo.setValue(existing.getStatus().toString());
        }
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setStyle(fieldStyle());
        Label statusErr = createErrLabel();

        // Motivation
        TextArea motivField = new TextArea(existing.getMotivation() != null ? existing.getMotivation() : "");
        motivField.setPromptText("Motivation");
        motivField.setPrefRowCount(3);
        motivField.setWrapText(true);
        motivField.setStyle(fieldStyle());
        Label motivErr = createErrLabel();

        // Goals
        TextArea goalsField = new TextArea(existing.getGoals() != null ? existing.getGoals() : "");
        goalsField.setPromptText("Objectifs");
        goalsField.setPrefRowCount(3);
        goalsField.setWrapText(true);
        goalsField.setStyle(fieldStyle());

        // Date
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (existing.getDate() != null) {
            datePicker.setValue(existing.getDate().toLocalDate());
        }

        // Time
        TextField timeField = createFormField(
                existing.getTime() != null ? existing.getTime() : "", "Heure (HH:mm)");

        // Info display
        Label infoLbl = new Label("Entrepreneur: "
                + (existing.getEntrepreneurName() != null ? existing.getEntrepreneurName() : "#" + existing.getEntrepreneurId())
                + " | Mentor: "
                + (existing.getMentorName() != null ? existing.getMentorName() : "#" + existing.getMentorId()));
        infoLbl.setFont(Font.font("Segoe UI", 11));
        infoLbl.setTextFill(Color.web("#7f8c8d"));
        infoLbl.setWrapText(true);

        form.getChildren().addAll(
                infoLbl, new Separator(),
                formLabel("Statut *"), statusCombo, statusErr,
                formLabel("Date"), datePicker,
                formLabel("Heure"), timeField,
                formLabel("Motivation"), motivField, motivErr,
                formLabel("Objectifs"), goalsField);
        dp.setContent(form);

        final boolean[] saved = {false};
        final MentorshipRequest.RequestStatus oldStatus = existing.getStatus();
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            String statusStr = statusCombo.getValue();
            statusErr.setText(""); motivErr.setText("");
            statusCombo.setStyle(fieldStyle()); motivField.setStyle(fieldStyle());

            boolean valid = true;
            if (statusStr == null || statusStr.isEmpty()) {
                statusErr.setText("Le statut est obligatoire");
                statusCombo.setStyle(errFieldStyle()); valid = false;
            }
            String motiv = motivField.getText().trim();
            if (!motiv.isEmpty() && motiv.length() < 5) {
                motivErr.setText("Minimum 5 caracteres si renseignee");
                motivField.setStyle(errFieldStyle()); valid = false;
            }
            if (!valid) { evt.consume(); return; }

            try {
                existing.setStatus(MentorshipRequest.RequestStatus.valueOf(statusStr));
                existing.setMotivation(motiv.isEmpty() ? existing.getMotivation() : motiv);
                String goals = goalsField.getText().trim();
                if (!goals.isEmpty()) existing.setGoals(goals);
                if (datePicker.getValue() != null) {
                    existing.setDate(java.sql.Date.valueOf(datePicker.getValue()));
                }
                String timeStr = timeField.getText().trim();
                if (!timeStr.isEmpty()) existing.setTime(timeStr);
                requestService.update(existing);
                // Send email + SMS notifications on status change
                MentorshipRequest.RequestStatus newStatus = existing.getStatus();
                if (oldStatus != newStatus) {
                    try {
                        notificationService.notifyRequestStatusChange(existing, oldStatus, newStatus);
                        LOG.info("Notification sent for request #" + existing.getId() + ": " + oldStatus + " -> " + newStatus);
                    } catch (Exception notifEx) {
                        LOG.warning("Notification failed: " + notifEx.getMessage());
                    }
                }
                saved[0] = true;
            } catch (Exception ex) {
                statusErr.setText("Erreur: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast("Demande modifiee"); showRequests(); }
    }

    // ====== SESSIONS TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadSessionsTable() {
        VBox container = buildTableContainer("Gestion des Sessions", "Gerer toutes les sessions de mentorat");

        List<MentorshipSession> sessions = sessionService.getAll();
        ObservableList<MentorshipSession> data = FXCollections.observableArrayList(sessions);
        FilteredList<MentorshipSession> filtered = new FilteredList<>(data, p -> true);

        Button addBtn = createAddButton("+ Ajouter une Session");
        addBtn.setOnAction(e -> openSessionForm(null));

        TextField search = createSearchField("Rechercher par statut, lien...");
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase().trim();
            filtered.setPredicate(s -> q.isEmpty()
                    || (s.getStatus() != null && s.getStatus().toString().toLowerCase().contains(q))
                    || (s.getMeetingLink() != null && s.getMeetingLink().toLowerCase().contains(q))
                    || String.valueOf(s.getRequestId()).contains(q));
        });

        HBox topBar = createTopBar(addBtn, search);

        TableView<MentorshipSession> table = createStyledTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<MentorshipSession, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); colId.setMinWidth(50);

        TableColumn<MentorshipSession, Integer> colReq = new TableColumn<>("Demande");
        colReq.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colReq.setMaxWidth(80); colReq.setMinWidth(80);

        TableColumn<MentorshipSession, String> colScheduled = new TableColumn<>("Planifiee le");
        colScheduled.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getScheduledAt() != null
                        ? cd.getValue().getScheduledAt().toLocalDateTime().format(DATE_FMT) : "-"));

        TableColumn<MentorshipSession, Integer> colDuration = new TableColumn<>("Duree (min)");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colDuration.setMaxWidth(90); colDuration.setMinWidth(90);

        TableColumn<MentorshipSession, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStatus() != null ? cd.getValue().getStatus().toString() : "-"));
        colStatus.setMaxWidth(100); colStatus.setMinWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String c = switch (item) {
                    case "scheduled" -> "#3498db";
                    case "completed" -> "#27ae60";
                    case "cancelled" -> "#e74c3c";
                    case "no_show"   -> "#e67e22";
                    default          -> "#7f8c8d";
                };
                setStyle("-fx-text-fill: " + c + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<MentorshipSession, String> colLink = new TableColumn<>("Lien");
        colLink.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMeetingLink() != null && !cd.getValue().getMeetingLink().isBlank()
                        ? truncate(cd.getValue().getMeetingLink(), 30) : "-"));

        TableColumn<MentorshipSession, Integer> colMRating = new TableColumn<>("Note M.");
        colMRating.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getMentorRating()).asObject());
        colMRating.setMaxWidth(70); colMRating.setMinWidth(70);

        TableColumn<MentorshipSession, Integer> colERating = new TableColumn<>("Note E.");
        colERating.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getEntrepreneurRating()).asObject());
        colERating.setMaxWidth(70); colERating.setMinWidth(70);

        TableColumn<MentorshipSession, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(170); colActions.setMinWidth(170);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);
            private final Button editBtn = createEditButton();
            private final Button delBtn = createDeleteButton();
            {
                editBtn.setOnAction(e -> {
                    MentorshipSession s = getTableView().getItems().get(getIndex());
                    openSessionForm(s);
                });
                delBtn.setOnAction(e -> {
                    MentorshipSession s = getTableView().getItems().get(getIndex());
                    confirmAndDelete("la session #" + s.getId(), () -> {
                        sessionService.delete(s.getId());
                        data.remove(s);
                        showToast("Session supprimee");
                    });
                });
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(editBtn, delBtn);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(colId, colReq, colScheduled, colDuration, colStatus, colLink, colMRating, colERating, colActions);
        table.setItems(filtered);

        Label countLbl = createCountLabel(sessions.size() + " sessions au total");
        container.getChildren().addAll(topBar, countLbl, table);
        setContent(container);
    }

    private void openSessionForm(MentorshipSession existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la Session #" + existing.getId() : "Nouvelle Session");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(460);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        // Request ID (required for new, read-only for edit)
        TextField reqIdField = createFormField(
                isEdit ? String.valueOf(existing.getRequestId()) : "", "ID de la demande associee");
        if (isEdit) reqIdField.setEditable(false);
        Label reqIdErr = createErrLabel();

        // Date + Time
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        TextField timeField = createFormField("", "Heure (HH:mm)");
        Label dateErr = createErrLabel();
        if (isEdit && existing.getScheduledAt() != null) {
            LocalDateTime ldt = existing.getScheduledAt().toLocalDateTime();
            datePicker.setValue(ldt.toLocalDate());
            timeField.setText(ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        // Duration
        TextField durationField = createFormField(
                isEdit ? String.valueOf(existing.getDurationMinutes()) : "", "Duree en minutes");
        Label durationErr = createErrLabel();

        // Status
        ComboBox<String> statusCombo = new ComboBox<>();
        for (MentorshipSession.SessionStatus s : MentorshipSession.SessionStatus.values()) {
            statusCombo.getItems().add(s.toString());
        }
        if (isEdit && existing.getStatus() != null) {
            statusCombo.setValue(existing.getStatus().toString());
        } else {
            statusCombo.setValue("scheduled");
        }
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setStyle(fieldStyle());

        // Meeting link
        TextField linkField = createFormField(
                isEdit && existing.getMeetingLink() != null ? existing.getMeetingLink() : "",
                "Lien de la reunion (optionnel)");
        Label linkErr = createErrLabel();

        form.getChildren().addAll(
                formLabel("Demande ID *"), reqIdField, reqIdErr,
                formLabel("Date *"), datePicker,
                formLabel("Heure *"), timeField, dateErr,
                formLabel("Duree (min) *"), durationField, durationErr,
                formLabel("Statut"), statusCombo,
                formLabel("Lien reunion"), linkField, linkErr);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            reqIdErr.setText(""); dateErr.setText(""); durationErr.setText(""); linkErr.setText("");
            reqIdField.setStyle(fieldStyle()); durationField.setStyle(fieldStyle());
            timeField.setStyle(fieldStyle()); linkField.setStyle(fieldStyle());

            boolean valid = true;

            // Validate request ID
            int reqId = 0;
            if (!isEdit) {
                String reqStr = reqIdField.getText().trim();
                if (reqStr.isEmpty()) {
                    reqIdErr.setText("L'ID de la demande est obligatoire");
                    reqIdField.setStyle(errFieldStyle()); valid = false;
                } else {
                    try {
                        reqId = Integer.parseInt(reqStr);
                        if (reqId <= 0) { reqIdErr.setText("ID invalide"); reqIdField.setStyle(errFieldStyle()); valid = false; }
                        else {
                            // Check request exists
                            MentorshipRequest req = requestService.getOne(reqId);
                            if (req == null || req.getId() == 0) {
                                reqIdErr.setText("Demande #" + reqId + " introuvable");
                                reqIdField.setStyle(errFieldStyle()); valid = false;
                            }
                        }
                    } catch (NumberFormatException e) {
                        reqIdErr.setText("Nombre entier requis"); reqIdField.setStyle(errFieldStyle()); valid = false;
                    }
                }
            }

            // Validate date + time
            LocalDate date = datePicker.getValue();
            LocalTime time = null;
            String timeStr = timeField.getText().trim();
            if (date == null) {
                dateErr.setText("La date est obligatoire"); valid = false;
            }
            if (timeStr.isEmpty()) {
                dateErr.setText(dateErr.getText().isEmpty() ? "L'heure est obligatoire" : dateErr.getText() + " et l'heure aussi");
                timeField.setStyle(errFieldStyle()); valid = false;
            } else {
                try { time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm")); }
                catch (Exception e) { dateErr.setText("Format heure invalide (HH:mm)"); timeField.setStyle(errFieldStyle()); valid = false; }
            }

            // Validate duration
            int duration = 0;
            String durStr = durationField.getText().trim();
            if (durStr.isEmpty()) {
                durationErr.setText("La duree est obligatoire");
                durationField.setStyle(errFieldStyle()); valid = false;
            } else {
                try {
                    duration = Integer.parseInt(durStr);
                    if (duration <= 0) { durationErr.setText("Doit etre > 0"); durationField.setStyle(errFieldStyle()); valid = false; }
                    if (duration > 480) { durationErr.setText("Maximum 480 minutes (8h)"); durationField.setStyle(errFieldStyle()); valid = false; }
                } catch (NumberFormatException e) {
                    durationErr.setText("Nombre entier requis"); durationField.setStyle(errFieldStyle()); valid = false;
                }
            }

            // Validate link format
            String link = linkField.getText().trim();
            if (!link.isEmpty() && !link.startsWith("http://") && !link.startsWith("https://")) {
                linkErr.setText("L'URL doit commencer par http:// ou https://");
                linkField.setStyle(errFieldStyle()); valid = false;
            }

            if (!valid) { evt.consume(); return; }

            try {
                Timestamp scheduledTs = Timestamp.valueOf(LocalDateTime.of(date, time));
                MentorshipSession.SessionStatus status = MentorshipSession.SessionStatus.valueOf(
                        statusCombo.getValue() != null ? statusCombo.getValue() : "scheduled");

                if (isEdit) {
                    existing.setScheduledAt(scheduledTs);
                    existing.setDurationMinutes(duration);
                    existing.setStatus(status);
                    existing.setMeetingLink(link.isEmpty() ? null : link);
                    sessionService.update(existing);
                } else {
                    MentorshipSession s = new MentorshipSession();
                    s.setRequestId(reqId);
                    s.setScheduledAt(scheduledTs);
                    s.setDurationMinutes(duration);
                    s.setStatus(status);
                    s.setMeetingLink(link.isEmpty() ? null : link);
                    sessionService.add(s);
                }
                saved[0] = true;
            } catch (Exception ex) {
                durationErr.setText("Erreur: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Session modifiee" : "Session ajoutee"); showSessions(); }
    }

    // ====== AVAILABILITY TABLE + CRUD ======

    @SuppressWarnings("unchecked")
    private void loadAvailabilityTable() {
        VBox container = buildTableContainer("Gestion des Disponibilites", "Gerer les creneaux de disponibilite des mentors");

        List<MentorAvailability> avails = availabilityService.getAll();
        ObservableList<MentorAvailability> data = FXCollections.observableArrayList(avails);
        FilteredList<MentorAvailability> filtered = new FilteredList<>(data, p -> true);

        Button addBtn = createAddButton("+ Ajouter une Disponibilite");
        addBtn.setOnAction(e -> openAvailabilityForm(null));

        TextField search = createSearchField("Rechercher par mentor...");
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase().trim();
            filtered.setPredicate(a -> q.isEmpty()
                    || (a.getMentorName() != null && a.getMentorName().toLowerCase().contains(q))
                    || String.valueOf(a.getMentorId()).contains(q));
        });

        HBox topBar = createTopBar(addBtn, search);

        TableView<MentorAvailability> table = createStyledTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<MentorAvailability, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50); colId.setMinWidth(50);

        TableColumn<MentorAvailability, String> colMentor = new TableColumn<>("Mentor");
        colMentor.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMentorName() != null ? cd.getValue().getMentorName() : "#" + cd.getValue().getMentorId()));

        TableColumn<MentorAvailability, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDate() != null ? cd.getValue().getDate().toString() : "-"));
        colDate.setMaxWidth(110); colDate.setMinWidth(110);

        TableColumn<MentorAvailability, String> colStart = new TableColumn<>("Debut");
        colStart.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getStartTime() != null ? cd.getValue().getStartTime().toString() : "-"));
        colStart.setMaxWidth(90); colStart.setMinWidth(90);

        TableColumn<MentorAvailability, String> colEnd = new TableColumn<>("Fin");
        colEnd.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getEndTime() != null ? cd.getValue().getEndTime().toString() : "-"));
        colEnd.setMaxWidth(90); colEnd.setMinWidth(90);

        TableColumn<MentorAvailability, String> colCreated = new TableColumn<>("Cree le");
        colCreated.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCreatedAt() != null
                        ? cd.getValue().getCreatedAt().toLocalDateTime().format(DATE_FMT) : "-"));
        colCreated.setMaxWidth(140); colCreated.setMinWidth(140);

        TableColumn<MentorAvailability, Void> colActions = new TableColumn<>("Actions");
        colActions.setMaxWidth(170); colActions.setMinWidth(170);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);
            private final Button editBtn = createEditButton();
            private final Button delBtn = createDeleteButton();
            {
                editBtn.setOnAction(e -> {
                    MentorAvailability a = getTableView().getItems().get(getIndex());
                    openAvailabilityForm(a);
                });
                delBtn.setOnAction(e -> {
                    MentorAvailability a = getTableView().getItems().get(getIndex());
                    confirmAndDelete("la disponibilite #" + a.getId(), () -> {
                        availabilityService.delete(a.getId());
                        data.remove(a);
                        showToast("Disponibilite supprimee");
                    });
                });
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(editBtn, delBtn);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(colId, colMentor, colDate, colStart, colEnd, colCreated, colActions);
        table.setItems(filtered);

        Label countLbl = createCountLabel(avails.size() + " disponibilites au total");
        container.getChildren().addAll(topBar, countLbl, table);
        setContent(container);
    }

    private void openAvailabilityForm(MentorAvailability existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la Disponibilite" : "Nouvelle Disponibilite");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.setPrefWidth(440);
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialogPane(dp);

        VBox form = new VBox(8);
        form.setPadding(new Insets(16));

        // Mentor ID
        TextField mentorIdField = createFormField(
                isEdit ? String.valueOf(existing.getMentorId()) : String.valueOf(currentUserId),
                "ID du mentor");
        Label mentorErr = createErrLabel();

        // Date
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (isEdit && existing.getDate() != null) {
            datePicker.setValue(existing.getDate().toLocalDate());
        }
        Label dateErr = createErrLabel();

        // Start time
        TextField startField = createFormField(
                isEdit && existing.getStartTime() != null ? existing.getStartTime().toString().substring(0, 5) : "",
                "Debut (HH:mm)");
        Label startErr = createErrLabel();

        // End time
        TextField endField = createFormField(
                isEdit && existing.getEndTime() != null ? existing.getEndTime().toString().substring(0, 5) : "",
                "Fin (HH:mm)");
        Label endErr = createErrLabel();

        form.getChildren().addAll(
                formLabel("Mentor ID *"), mentorIdField, mentorErr,
                formLabel("Date *"), datePicker, dateErr,
                formLabel("Heure debut *"), startField, startErr,
                formLabel("Heure fin *"), endField, endErr);
        dp.setContent(form);

        final boolean[] saved = {false};
        dp.lookupButton(saveType).addEventFilter(ActionEvent.ACTION, evt -> {
            mentorErr.setText(""); dateErr.setText(""); startErr.setText(""); endErr.setText("");
            mentorIdField.setStyle(fieldStyle()); startField.setStyle(fieldStyle()); endField.setStyle(fieldStyle());

            boolean valid = true;

            // Mentor ID
            int mentorId = 0;
            String mStr = mentorIdField.getText().trim();
            if (mStr.isEmpty()) {
                mentorErr.setText("L'ID du mentor est obligatoire");
                mentorIdField.setStyle(errFieldStyle()); valid = false;
            } else {
                try {
                    mentorId = Integer.parseInt(mStr);
                    if (mentorId <= 0) { mentorErr.setText("ID invalide"); mentorIdField.setStyle(errFieldStyle()); valid = false; }
                } catch (NumberFormatException e) {
                    mentorErr.setText("Nombre entier requis"); mentorIdField.setStyle(errFieldStyle()); valid = false;
                }
            }

            // Date
            LocalDate date = datePicker.getValue();
            if (date == null) {
                dateErr.setText("La date est obligatoire"); valid = false;
            }

            // Start time
            LocalTime startTime = null;
            String sStr = startField.getText().trim();
            if (sStr.isEmpty()) {
                startErr.setText("L'heure de debut est obligatoire");
                startField.setStyle(errFieldStyle()); valid = false;
            } else {
                try { startTime = LocalTime.parse(sStr, DateTimeFormatter.ofPattern("HH:mm")); }
                catch (Exception e) { startErr.setText("Format invalide (HH:mm)"); startField.setStyle(errFieldStyle()); valid = false; }
            }

            // End time
            LocalTime endTime = null;
            String eStr = endField.getText().trim();
            if (eStr.isEmpty()) {
                endErr.setText("L'heure de fin est obligatoire");
                endField.setStyle(errFieldStyle()); valid = false;
            } else {
                try { endTime = LocalTime.parse(eStr, DateTimeFormatter.ofPattern("HH:mm")); }
                catch (Exception e) { endErr.setText("Format invalide (HH:mm)"); endField.setStyle(errFieldStyle()); valid = false; }
            }

            // End must be after start
            if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
                endErr.setText("L'heure de fin doit etre apres le debut");
                endField.setStyle(errFieldStyle()); valid = false;
            }

            // Uniqueness: check overlapping availability for same mentor on same date
            if (valid && date != null && startTime != null && endTime != null) {
                final LocalTime st = startTime;
                final LocalTime et = endTime;
                final int mId = mentorId;
                List<MentorAvailability> allAvails = availabilityService.getAll();
                boolean overlap = allAvails.stream().anyMatch(a -> {
                    if (a.getMentorId() != mId) return false;
                    if (a.getDate() == null || !a.getDate().toLocalDate().equals(date)) return false;
                    if (isEdit && a.getId() == existing.getId()) return false;
                    LocalTime aStart = a.getStartTime().toLocalTime();
                    LocalTime aEnd = a.getEndTime().toLocalTime();
                    return st.isBefore(aEnd) && et.isAfter(aStart);
                });
                if (overlap) {
                    startErr.setText("Chevauchement avec un creneau existant");
                    startField.setStyle(errFieldStyle());
                    endField.setStyle(errFieldStyle()); valid = false;
                }
            }

            if (!valid) { evt.consume(); return; }

            try {
                if (isEdit) {
                    existing.setMentorId(mentorId);
                    existing.setDate(java.sql.Date.valueOf(date));
                    existing.setStartTime(java.sql.Time.valueOf(startTime));
                    existing.setEndTime(java.sql.Time.valueOf(endTime));
                    availabilityService.update(existing);
                } else {
                    MentorAvailability a = new MentorAvailability();
                    a.setMentorId(mentorId);
                    a.setDate(java.sql.Date.valueOf(date));
                    a.setStartTime(java.sql.Time.valueOf(startTime));
                    a.setEndTime(java.sql.Time.valueOf(endTime));
                    availabilityService.add(a);
                }
                saved[0] = true;
            } catch (Exception ex) {
                mentorErr.setText("Erreur: " + ex.getMessage());
                evt.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) { showToast(isEdit ? "Disponibilite modifiee" : "Disponibilite ajoutee"); showAvailability(); }
    }

    // ====== SHARED UTILITY METHODS ======

    private String getStatusColor(MentorshipRequest.RequestStatus status) {
        if (status == null) return "#7f8c8d";
        return switch (status) {
            case auto_accepted -> "#27ae60";
            case pending_review -> "#e67e22";
            case rejected -> "#e74c3c";
            case cancelled -> "#95a5a6";
            case completed -> "#8e44ad";
        };
    }

    private VBox buildTableContainer(String title, String subtitle) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20, 24, 20, 24));
        VBox.setVgrow(container, Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLbl.setTextFill(Color.web("#2c3e50"));

        Label subLbl = new Label(subtitle);
        subLbl.setFont(Font.font("Segoe UI", 12));
        subLbl.setTextFill(Color.web("#7f8c8d"));

        container.getChildren().addAll(titleLbl, subLbl);
        return container;
    }

    private <T> TableView<T> createStyledTable() {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #e8e8e8; -fx-border-radius: 8; -fx-border-width: 1;");
        table.setFixedCellSize(40);
        table.setPlaceholder(new Label("Aucune donnee"));
        return table;
    }

    private TextField createSearchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(380);
        field.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1; "
                + "-fx-padding: 8 14; -fx-font-size: 13;");
        field.focusedProperty().addListener((obs, o, n) -> {
            field.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                    + "-fx-border-color: " + (n ? "#e67e22" : "#ddd") + "; -fx-border-radius: 8; "
                    + "-fx-border-width: " + (n ? "1.5" : "1") + "; -fx-padding: 8 14; -fx-font-size: 13;");
        });
        return field;
    }

    private HBox createTopBar(Button addBtn, TextField search) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, addBtn, spacer, search);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private Button createAddButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #d35400; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-font-size: 12; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 8 18;"));
        return btn;
    }

    private Button createEditButton() {
        Button btn = new Button("Modifier");
        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        return btn;
    }

    private Button createDeleteButton() {
        Button btn = new Button("Supprimer");
        btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 10;"));
        return btn;
    }

    private Label createCountLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web("#95a5a6"));
        return lbl;
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) return "-";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    // ====== FORM UTILITY METHODS ======

    private void styleDialogPane(DialogPane dp) {
        dp.setStyle("-fx-background-color: #f8f9fa; -fx-font-family: 'Segoe UI';");
    }

    private Label formLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web("#2c3e50"));
        return lbl;
    }

    private TextField createFormField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setStyle(fieldStyle());
        return field;
    }

    private Label createErrLabel() {
        Label lbl = new Label();
        lbl.setTextFill(Color.web("#e74c3c"));
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setWrapText(true);
        return lbl;
    }

    private String fieldStyle() {
        return "-fx-background-color: white; -fx-background-radius: 6; "
                + "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-border-width: 1; "
                + "-fx-padding: 6 10; -fx-font-size: 13;";
    }

    private String errFieldStyle() {
        return "-fx-background-color: #fff5f5; -fx-background-radius: 6; "
                + "-fx-border-color: #e74c3c; -fx-border-radius: 6; -fx-border-width: 1.5; "
                + "-fx-padding: 6 10; -fx-font-size: 13;";
    }

    // ====== TOAST / DIALOGS ======

    private void confirmAndDelete(String item, Runnable action) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + item + " ?");
        alert.setContentText("Cette action est irreversible.");
        alert.getButtonTypes().setAll(
                new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            action.run();
        }
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        toast.setTextFill(Color.WHITE);
        toast.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 8; "
                + "-fx-padding: 10 24; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        StackPane.setMargin(toast, new Insets(10, 0, 0, 0));

        rootStack.getChildren().add(toast);
        toast.setOpacity(0);
        toast.setTranslateY(-15);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), toast);
        slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();

        PauseTransition hold = new PauseTransition(Duration.seconds(2));
        hold.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> rootStack.getChildren().remove(toast));
            fadeOut.play();
        });
        hold.play();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
