package controllers.mentorat;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import models.User;
import models.mentorat.*;
import services.SessionService;
import services.mentorat.*;
import util.Type;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Front-office controller for the Mentorat module.
 * Loaded inside FrontOfficeShellController.
 *
 * Entrepreneurs can:  explore mentors, submit requests, view requests & sessions.
 * Mentors can also:   manage their availability, see incoming requests.
 */
public class FrontMentoratController {

    // ═══════════════════════════════════════════════
    //  FXML
    // ═══════════════════════════════════════════════
    @FXML private BorderPane rootPane;
    @FXML private HBox       subTabBar;
    @FXML private StackPane  contentArea;

    @FXML private Button tabExploreMentors;
    @FXML private Button tabMyRequests;
    @FXML private Button tabMySessions;
    @FXML private Button tabMyAvailability;

    // ═══════════════════════════════════════════════
    //  State
    // ═══════════════════════════════════════════════
    private User    currentUser;
    private Button  activeTab;

    private final ServiceMentorAvailability  availabilityService = new ServiceMentorAvailability();
    private final ServiceMentorshipRequest   requestService      = new ServiceMentorshipRequest();
    private final ServiceMentorshipSession   sessionService      = new ServiceMentorshipSession();
    private final ServiceUser                userService         = new ServiceUser();
    private final ServiceProjet              projetService       = new ServiceProjet();
    private final MentoratNotificationService notificationService = new MentoratNotificationService();

    // ═══════════════════════════════════════════════
    //  Init
    // ═══════════════════════════════════════════════

    public void setCurrentUser(User user) {
        this.currentUser = user;
        configureForRole();
        showExploreMentors();
    }

    @FXML
    public void initialize() {
        try {
            // Fallback if setCurrentUser is not called (e.g. standalone test)
            if (currentUser == null) {
                currentUser = SessionService.getInstance().getCurrentUser();
            }
            if (currentUser != null) {
                configureForRole();
                showExploreMentors();
            }
        } catch (Exception e) {
            System.err.println("FrontMentoratController.initialize() FAILED: " + e.getMessage());
            e.printStackTrace();
            // Show error state instead of crashing the FXML load
            Label errorLabel = new Label("Erreur de chargement: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 40;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    private void configureForRole() {
        boolean isMentor = currentUser != null && currentUser.getRole() == Type.MENTOR;
        tabMyAvailability.setVisible(isMentor);
        tabMyAvailability.setManaged(isMentor);
    }

    // ═══════════════════════════════════════════════
    //  Tab Navigation
    // ═══════════════════════════════════════════════

    @FXML private void showExploreMentors() { switchTab(tabExploreMentors, buildExploreMentorsView()); }
    @FXML private void showMyRequests()     { switchTab(tabMyRequests,     buildMyRequestsView());     }
    @FXML private void showMySessions()     { switchTab(tabMySessions,     buildMySessionsView());     }
    @FXML private void showMyAvailability() { switchTab(tabMyAvailability, buildMyAvailabilityView()); }

    private void switchTab(Button tab, Node content) {
        if (activeTab != null) activeTab.getStyleClass().remove("fo-sub-tab-active");
        tab.getStyleClass().add("fo-sub-tab-active");
        activeTab = tab;

        FadeTransition fade = new FadeTransition(Duration.millis(200), content);
        fade.setFromValue(0); fade.setToValue(1);
        contentArea.getChildren().setAll(content);
        fade.play();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 1 — EXPLORE MENTORS
    // ═══════════════════════════════════════════════════════════════

    private Node buildExploreMentorsView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 24, 24, 40));
        root.getStyleClass().add("fo-content");

        // Title
        Label title = styledLabel("Trouver un Mentor", "section-title");
        Label subtitle = styledLabel("Parcourez les mentors disponibles et soumettez une demande de mentorat",
                "section-subtitle");

        // Filters
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        datePicker.setPromptText("Date souhaitée");
        datePicker.setPrefWidth(180);
        TextField timeField = new TextField();
        timeField.setPromptText("Heure (HH:mm)");
        timeField.setPrefWidth(120);
        Button btnSearch = new Button("Rechercher");
        btnSearch.getStyleClass().addAll("fo-sub-tab", "fo-sub-tab-active");

        filters.getChildren().addAll(new Label("Date:"), datePicker, new Label("Heure:"), timeField, btnSearch);

        // Mentor grid
        FlowPane mentorGrid = new FlowPane(16, 16);
        mentorGrid.setPadding(new Insets(8, 0, 0, 0));

        // Initial load — all available mentors
        Runnable loadMentors = () -> {
            mentorGrid.getChildren().clear();
            List<User> mentors;
            if (datePicker.getValue() != null && !timeField.getText().isBlank()) {
                Date sqlDate = Date.valueOf(datePicker.getValue());
                String time = timeField.getText().trim();
                if (!time.contains(":")) time += ":00";
                if (time.length() == 5) time += ":00";
                mentors = userService.getAvailableMentors(sqlDate, time);
            } else {
                mentors = userService.getAvailableMentors();
            }

            if (mentors.isEmpty()) {
                mentorGrid.getChildren().add(emptyState("Aucun mentor disponible",
                        "Essayez de modifier vos critères de recherche"));
            } else {
                for (User mentor : mentors) {
                    mentorGrid.getChildren().add(buildMentorCard(mentor, datePicker, timeField));
                }
            }
        };
        loadMentors.run();
        btnSearch.setOnAction(e -> loadMentors.run());

        root.getChildren().addAll(title, subtitle, filters, mentorGrid);
        return wrapInScrollPane(root);
    }

    private VBox buildMentorCard(User mentor, DatePicker datePicker, TextField timeField) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);
            """);
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "rgba(0,0,0,0.06)", "rgba(59,130,246,0.12)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "rgba(59,130,246,0.12)", "rgba(0,0,0,0.06)")));

        // Avatar circle
        String initial = (mentor.getFirstname() != null && !mentor.getFirstname().isEmpty())
                ? mentor.getFirstname().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initial);
        avatar.setStyle("""
            -fx-background-color: #10b981;
            -fx-background-radius: 50;
            -fx-text-fill: white;
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-min-width: 48; -fx-min-height: 48;
            -fx-max-width: 48; -fx-max-height: 48;
            """);

        Label name = styledLabel(mentor.getFullName(), "card-title");
        Label email = styledLabel(mentor.getEmail(), "card-detail");

        // Availability count
        List<MentorAvailability> slots = availabilityService.getByMentorId(mentor.getId());
        long futureSlots = slots.stream()
                .filter(s -> s.getDate() != null && s.getDate().toLocalDate().isAfter(LocalDate.now().minusDays(1)))
                .count();
        Label slotCount = new Label(futureSlots + " créneaux disponibles");
        slotCount.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button btnRequest = new Button("Demander un mentorat");
        btnRequest.setMaxWidth(Double.MAX_VALUE);
        btnRequest.setStyle("""
            -fx-background-color: #10b981;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-padding: 8 16;
            -fx-cursor: hand;
            """);
        btnRequest.setOnAction(e -> openRequestDialog(mentor, datePicker, timeField));

        card.getChildren().addAll(avatar, name, email, slotCount, btnRequest);
        return card;
    }

    private void openRequestDialog(User mentor, DatePicker datePicker, TextField timeField) {
        Dialog<MentorshipRequest> dialog = new Dialog<>();
        dialog.setTitle("Demander un mentorat");

        ButtonType submitType = new ButtonType("Envoyer la demande", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        // ── Style the DialogPane ──
        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(580);
        pane.setStyle("""
            -fx-background-color: #f8fafc;
            -fx-font-family: 'Segoe UI', Arial, sans-serif;
            """);
        pane.setHeaderText(null); // We build a custom header

        // ── Main container ──
        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(24, 28, 16, 28));
        mainBox.setStyle("-fx-background-color: #f8fafc;");

        // ── Mentor header card ──
        HBox mentorHeader = new HBox(14);
        mentorHeader.setAlignment(Pos.CENTER_LEFT);
        mentorHeader.setPadding(new Insets(16, 20, 16, 20));
        mentorHeader.setStyle("""
            -fx-background-color: linear-gradient(to right, #10b981, #059669);
            -fx-background-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.3), 10, 0, 0, 3);
            """);
        Label mentorAvatar = new Label(mentor.getFirstname().substring(0, 1).toUpperCase());
        mentorAvatar.setStyle("""
            -fx-background-color: rgba(255,255,255,0.25);
            -fx-background-radius: 50;
            -fx-text-fill: white;
            -fx-font-size: 20px; -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-min-width: 42; -fx-min-height: 42;
            -fx-max-width: 42; -fx-max-height: 42;
            """);
        VBox mentorInfo = new VBox(2);
        Label mentorName = new Label(mentor.getFullName());
        mentorName.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label mentorEmail = new Label(mentor.getEmail());
        mentorEmail.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12px;");
        mentorInfo.getChildren().addAll(mentorName, mentorEmail);
        mentorHeader.getChildren().addAll(mentorAvatar, mentorInfo);

        // ── Section: Planning ──
        Label planningSection = dialogSectionLabel("Planning");

        HBox dateTimeRow = new HBox(14);
        dateTimeRow.setAlignment(Pos.CENTER_LEFT);

        VBox dateBox = new VBox(6);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        Label dateLabel = dialogFieldLabel("Date");
        DatePicker dialogDate = new DatePicker(datePicker.getValue() != null ?
                datePicker.getValue() : LocalDate.now().plusDays(1));
        dialogDate.setMaxWidth(Double.MAX_VALUE);
        styleDialogField(dialogDate);
        dateBox.getChildren().addAll(dateLabel, dialogDate);

        VBox timeBox = new VBox(6);
        Label timeLabel = dialogFieldLabel("Heure");
        TextField dialogTime = new TextField(timeField.getText().isBlank() ? "10:00" : timeField.getText());
        dialogTime.setPromptText("HH:mm");
        dialogTime.setPrefWidth(140);
        styleDialogField(dialogTime);
        timeBox.getChildren().addAll(timeLabel, dialogTime);

        HBox.setHgrow(dateBox, Priority.ALWAYS);
        dateTimeRow.getChildren().addAll(dateBox, timeBox);

        // ── Section: Projet ──
        Label projectSection = dialogSectionLabel("Projet");
        ComboBox<Projet> projectCombo = new ComboBox<>();
        List<Projet> projects = projetService.getAll();
        projectCombo.getItems().addAll(projects);
        if (!projects.isEmpty()) projectCombo.getSelectionModel().selectFirst();
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        styleDialogField(projectCombo);

        // ── Section: Motivation & Objectifs ──
        Label detailsSection = dialogSectionLabel("Details de la demande");

        Label motivLabel = dialogFieldLabel("Motivation");
        TextArea motivationArea = new TextArea();
        motivationArea.setPromptText("Decrivez votre motivation pour cette demande de mentorat...");
        motivationArea.setPrefRowCount(3);
        motivationArea.setWrapText(true);
        styleDialogField(motivationArea);

        Label goalsLabel = dialogFieldLabel("Objectifs");
        TextArea goalsArea = new TextArea();
        goalsArea.setPromptText("Quels sont vos objectifs ?");
        goalsArea.setPrefRowCount(3);
        goalsArea.setWrapText(true);
        styleDialogField(goalsArea);

        // ── Assemble ──
        mainBox.getChildren().addAll(
                mentorHeader,
                dialogSeparator(),
                planningSection, dateTimeRow,
                projectSection, projectCombo,
                dialogSeparator(),
                detailsSection, motivLabel, motivationArea, goalsLabel, goalsArea
        );

        pane.setContent(mainBox);

        // ── Style submit button ──
        Node submitBtn = pane.lookupButton(submitType);
        submitBtn.setStyle("""
            -fx-background-color: #10b981;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 13px;
            -fx-background-radius: 8;
            -fx-padding: 8 24;
            -fx-cursor: hand;
            """);
        Node cancelBtn = pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("""
            -fx-background-color: #f1f5f9;
            -fx-text-fill: #64748b;
            -fx-font-weight: bold;
            -fx-font-size: 13px;
            -fx-background-radius: 8;
            -fx-padding: 8 20;
            -fx-cursor: hand;
            """);

        // ── Validation ──
        submitBtn.setDisable(true);
        Runnable validate = () -> {
            boolean valid = projectCombo.getValue() != null
                    && dialogDate.getValue() != null
                    && !dialogTime.getText().isBlank()
                    && !motivationArea.getText().isBlank()
                    && !goalsArea.getText().isBlank();
            submitBtn.setDisable(!valid);
        };
        projectCombo.valueProperty().addListener((o,a,b) -> validate.run());
        dialogDate.valueProperty().addListener((o,a,b) -> validate.run());
        dialogTime.textProperty().addListener((o,a,b) -> validate.run());
        motivationArea.textProperty().addListener((o,a,b) -> validate.run());
        goalsArea.textProperty().addListener((o,a,b) -> validate.run());

        dialog.setResultConverter(btn -> {
            if (btn == submitType) {
                MentorshipRequest req = new MentorshipRequest();
                req.setEntrepreneurId(currentUser.getId());
                req.setMentorId(mentor.getId());
                req.setProjectId(projectCombo.getValue().getId());
                req.setDate(Date.valueOf(dialogDate.getValue()));
                String time = dialogTime.getText().trim();
                if (!time.contains(":")) time += ":00";
                req.setTime(time);
                req.setMotivation(motivationArea.getText().trim());
                req.setGoals(goalsArea.getText().trim());
                req.setMatchScore(0);
                req.setAutoApproved(false);
                req.setStatus(MentorshipRequest.RequestStatus.pending_review);
                return req;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(req -> {
            requestService.add(req);
            // Send email + SMS notification to the mentor
            try {
                notificationService.notifyNewRequest(req);
            } catch (Exception notifEx) {
                System.out.println("Notification failed: " + notifEx.getMessage());
            }
            showToast("Demande envoyee avec succes !");
            showMyRequests();
        });
    }

    // ── Dialog helper methods ──

    private Label dialogSectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("""
            -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;
            -fx-padding: 6 0 2 0;
            """);
        return l;
    }

    private Label dialogFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        return l;
    }

    private void styleDialogField(Control field) {
        field.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-padding: 8;
            -fx-font-size: 13px;
            """);
        field.focusedProperty().addListener((o, a, focused) -> {
            if (focused) {
                field.setStyle("""
                    -fx-background-color: white;
                    -fx-border-color: #10b981;
                    -fx-border-radius: 8;
                    -fx-background-radius: 8;
                    -fx-padding: 8;
                    -fx-font-size: 13px;
                    -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.15), 6, 0, 0, 0);
                    """);
            } else {
                field.setStyle("""
                    -fx-background-color: white;
                    -fx-border-color: #e2e8f0;
                    -fx-border-radius: 8;
                    -fx-background-radius: 8;
                    -fx-padding: 8;
                    -fx-font-size: 13px;
                    """);
            }
        });
    }

    private Separator dialogSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 4 0;");
        return sep;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 2 — MY REQUESTS
    // ═══════════════════════════════════════════════════════════════

    private Node buildMyRequestsView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 24, 24, 40));
        root.getStyleClass().add("fo-content");

        Label title = styledLabel("Mes Demandes de Mentorat", "section-title");

        List<MentorshipRequest> allRequests = requestService.getAll();
        boolean isMentor = currentUser.getRole() == Type.MENTOR;

        // Filter: entrepreneur sees their own requests; mentor sees incoming requests
        List<MentorshipRequest> myRequests = allRequests.stream()
                .filter(r -> isMentor ? r.getMentorId() == currentUser.getId()
                                      : r.getEntrepreneurId() == currentUser.getId())
                .sorted(Comparator.comparing((MentorshipRequest r) -> r.getCreatedAt()).reversed())
                .collect(Collectors.toList());

        if (myRequests.isEmpty()) {
            root.getChildren().addAll(title,
                    emptyState("Aucune demande",
                            isMentor ? "Aucune demande de mentorat reçue pour le moment"
                                     : "Explorez les mentors pour soumettre votre première demande"));
        } else {
            // Stats bar
            long pending = myRequests.stream().filter(r -> r.getStatus() == MentorshipRequest.RequestStatus.pending_review).count();
            long accepted = myRequests.stream().filter(r ->
                    r.getStatus() == MentorshipRequest.RequestStatus.auto_accepted ||
                    r.getStatus() == MentorshipRequest.RequestStatus.completed).count();

            HBox statsBar = new HBox(24);
            statsBar.setAlignment(Pos.CENTER_LEFT);
            statsBar.getChildren().addAll(
                statBadge("Total", String.valueOf(myRequests.size()), "#3b82f6"),
                statBadge("En attente", String.valueOf(pending), "#f59e0b"),
                statBadge("Acceptees", String.valueOf(accepted), "#10b981")
            );

            VBox requestList = new VBox(12);
            for (MentorshipRequest req : myRequests) {
                requestList.getChildren().add(buildRequestCard(req, isMentor));
            }
            root.getChildren().addAll(title, statsBar, requestList);
        }

        return wrapInScrollPane(root);
    }

    private VBox buildRequestCard(MentorshipRequest req, boolean isMentor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 10;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);
            """);

        // Header row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label statusBadge = statusLabel(req.getStatus().toString());
        Label personLabel = styledLabel(
                isMentor ? "De: " + safe(req.getEntrepreneurName())
                         : "Mentor: " + safe(req.getMentorName()),
                "card-detail");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateLabel = styledLabel(req.getDate() + " à " + req.getTime(), "card-detail");
        header.getChildren().addAll(statusBadge, personLabel, spacer, dateLabel);

        Label projectLabel = styledLabel("Projet: " + safe(req.getProjectName()), "card-detail");
        Label motivationLabel = styledLabel(truncate(req.getMotivation(), 120), "card-detail");

        card.getChildren().addAll(header, projectLabel, motivationLabel);

        // Action buttons for mentor
        if (isMentor && req.getStatus() == MentorshipRequest.RequestStatus.pending_review) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));

            Button btnAccept = new Button("Accepter");
            btnAccept.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
            btnAccept.setOnAction(e -> {
                req.setStatus(MentorshipRequest.RequestStatus.auto_accepted);
                req.setAutoApproved(true);
                requestService.update(req);
                // Auto-create session
                createSessionForRequest(req);
                showToast("Demande acceptee !");
                showMyRequests();
            });

            Button btnReject = new Button("Refuser");
            btnReject.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
            btnReject.setOnAction(e -> {
                req.setStatus(MentorshipRequest.RequestStatus.rejected);
                requestService.update(req);
                showToast("Demande refusée");
                showMyRequests();
            });

            actions.getChildren().addAll(btnAccept, btnReject);
            card.getChildren().add(actions);
        }

        // Cancel button for entrepreneur (pending only)
        if (!isMentor && req.getStatus() == MentorshipRequest.RequestStatus.pending_review) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));

            Button btnCancel = new Button("Annuler");
            btnCancel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
            btnCancel.setOnAction(e -> {
                req.setStatus(MentorshipRequest.RequestStatus.cancelled);
                requestService.update(req);
                showToast("Demande annulée");
                showMyRequests();
            });
            actions.getChildren().add(btnCancel);
            card.getChildren().add(actions);
        }

        return card;
    }

    private void createSessionForRequest(MentorshipRequest req) {
        try {
            MentorshipSession session = new MentorshipSession();
            session.setRequestId(req.getId());
            String timeStr = req.getTime() != null ? req.getTime() : "10:00";
            if (timeStr.length() == 5) timeStr += ":00";
            String dtStr = req.getDate().toString() + " " + timeStr;
            session.setScheduledAt(Timestamp.valueOf(dtStr));
            session.setDurationMinutes(60);
            session.setStatus(MentorshipSession.SessionStatus.scheduled);
            sessionService.add(session);
        } catch (Exception ex) {
            System.err.println("Error creating session: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 3 — MY SESSIONS
    // ═══════════════════════════════════════════════════════════════

    private Node buildMySessionsView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 24, 24, 40));
        root.getStyleClass().add("fo-content");

        Label title = styledLabel("Mes Sessions de Mentorat", "section-title");

        // Get all requests for this user, then find sessions for each
        List<MentorshipRequest> allRequests = requestService.getAll();
        boolean isMentor = currentUser.getRole() == Type.MENTOR;
        List<MentorshipRequest> myRequests = allRequests.stream()
                .filter(r -> isMentor ? r.getMentorId() == currentUser.getId()
                                      : r.getEntrepreneurId() == currentUser.getId())
                .collect(Collectors.toList());

        Map<Integer, MentorshipRequest> reqMap = myRequests.stream()
                .collect(Collectors.toMap(MentorshipRequest::getId, r -> r, (a, b) -> a));

        // Get all sessions and filter by user's request IDs
        List<MentorshipSession> allSessions = sessionService.getAll();
        List<MentorshipSession> mySessions = allSessions.stream()
                .filter(s -> reqMap.containsKey(s.getRequestId()))
                .sorted(Comparator.comparing((MentorshipSession s) ->
                        s.getScheduledAt() != null ? s.getScheduledAt() : new Timestamp(0)).reversed())
                .collect(Collectors.toList());

        if (mySessions.isEmpty()) {
            root.getChildren().addAll(title,
                    emptyState("Aucune session",
                            "Les sessions apparaîtront ici une fois vos demandes acceptées"));
        } else {
            // Split into upcoming and past
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            List<MentorshipSession> upcoming = mySessions.stream()
                    .filter(s -> s.getScheduledAt() != null && s.getScheduledAt().after(now)
                            && s.getStatus() == MentorshipSession.SessionStatus.scheduled)
                    .collect(Collectors.toList());
            List<MentorshipSession> past = mySessions.stream()
                    .filter(s -> !upcoming.contains(s))
                    .collect(Collectors.toList());

            if (!upcoming.isEmpty()) {
                root.getChildren().add(styledLabel("Sessions a venir", "section-title"));
                for (MentorshipSession s : upcoming) {
                    root.getChildren().add(buildSessionCard(s, reqMap.get(s.getRequestId()), isMentor));
                }
            }
            if (!past.isEmpty()) {
                root.getChildren().add(styledLabel("Historique", "section-title"));
                for (MentorshipSession s : past) {
                    root.getChildren().add(buildSessionCard(s, reqMap.get(s.getRequestId()), isMentor));
                }
            }
        }

        return wrapInScrollPane(root);
    }

    private VBox buildSessionCard(MentorshipSession session, MentorshipRequest req, boolean isMentor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 10;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);
            """);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label statusBadge = sessionStatusLabel(session.getStatus().toString());
        String partner = req != null ? (isMentor ? safe(req.getEntrepreneurName()) : safe(req.getMentorName())) : "?";
        Label partnerLabel = styledLabel((isMentor ? " " : " ") + partner, "card-detail");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String dateStr = session.getScheduledAt() != null ?
                session.getScheduledAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        Label dateLabel = styledLabel(dateStr, "card-detail");
        header.getChildren().addAll(statusBadge, partnerLabel, spacer, dateLabel);

        Label durationLabel = styledLabel("⏱ " + session.getDurationMinutes() + " min", "card-detail");

        card.getChildren().addAll(header, durationLabel);

        // Meeting link
        if (session.getMeetingLink() != null && !session.getMeetingLink().isBlank()) {
            Label linkLabel = new Label(session.getMeetingLink());
            linkLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12px; -fx-cursor: hand;");
            card.getChildren().add(linkLabel);
        }

        // Feedback section for completed sessions
        if (session.getStatus() == MentorshipSession.SessionStatus.completed) {
            String myFeedback = isMentor ? session.getMentorFeedback() : session.getEntrepreneurFeedback();
            int myRating = isMentor ? session.getMentorRating() : session.getEntrepreneurRating();

            if (myFeedback != null && !myFeedback.isBlank()) {
                Label fbLabel = styledLabel(myFeedback, "card-detail");
                Label rtLabel = styledLabel(myRating + "/5", "card-detail");
                card.getChildren().addAll(fbLabel, rtLabel);
            } else {
                Button btnFeedback = new Button("Laisser un avis");
                btnFeedback.setStyle("""
                    -fx-background-color: #f59e0b; -fx-text-fill: white;
                    -fx-background-radius: 6; -fx-cursor: hand;
                    """);
                btnFeedback.setOnAction(e -> openFeedbackDialog(session, isMentor));
                card.getChildren().add(btnFeedback);
            }
        }

        // Cancel button for scheduled sessions
        if (session.getStatus() == MentorshipSession.SessionStatus.scheduled) {
            Button btnCancel = new Button("Annuler la session");
            btnCancel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
            btnCancel.setOnAction(e -> {
                session.setStatus(MentorshipSession.SessionStatus.cancelled);
                sessionService.update(session);
                showToast("Session annulée");
                showMySessions();
            });
            card.getChildren().add(btnCancel);
        }

        return card;
    }

    private void openFeedbackDialog(MentorshipSession session, boolean isMentor) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Évaluer la session");
        dialog.setHeaderText("Partagez votre retour sur cette session");

        ButtonType submitType = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Integer> ratingCombo = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingCombo.setPromptText("Note (1-5)");
        ratingCombo.getSelectionModel().select(4); // default 5

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Votre retour...");
        feedbackArea.setPrefRowCount(3);
        feedbackArea.setWrapText(true);

        grid.addRow(0, new Label("Note :"), ratingCombo);
        grid.addRow(1, new Label("Avis :"), feedbackArea);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == submitType) {
                int rating = ratingCombo.getValue() != null ? ratingCombo.getValue() : 5;
                String feedback = feedbackArea.getText().trim();
                if (isMentor) {
                    session.setMentorRating(rating);
                    session.setMentorFeedback(feedback);
                } else {
                    session.setEntrepreneurRating(rating);
                    session.setEntrepreneurFeedback(feedback);
                }
                sessionService.update(session);
                showToast("Avis enregistre !");
                showMySessions();
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  TAB 4 — MY AVAILABILITY (Mentors only)
    // ═══════════════════════════════════════════════════════════════

    private Node buildMyAvailabilityView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 24, 24, 40));
        root.getStyleClass().add("fo-content");

        Label title = styledLabel("Mes Disponibilités", "section-title");

        Button btnAdd = new Button("Ajouter un creneau");
        btnAdd.setStyle("""
            -fx-background-color: #10b981; -fx-text-fill: white;
            -fx-font-weight: bold; -fx-background-radius: 8;
            -fx-padding: 8 20; -fx-cursor: hand;
            """);

        VBox slotList = new VBox(10);
        Runnable loadSlots = () -> {
            slotList.getChildren().clear();
            List<MentorAvailability> slots = availabilityService.getByMentorId(currentUser.getId());
            slots.sort(Comparator.comparing(MentorAvailability::getDate));
            if (slots.isEmpty()) {
                slotList.getChildren().add(emptyState("Aucun créneau",
                        "Ajoutez des créneaux pour que les entrepreneurs puissent vous contacter"));
            } else {
                for (MentorAvailability slot : slots) {
                    slotList.getChildren().add(buildAvailabilityCard(slot, () -> {
                        // Refresh after delete
                        showMyAvailability();
                    }));
                }
            }
        };

        btnAdd.setOnAction(e -> openAddAvailabilityDialog(() -> showMyAvailability()));
        loadSlots.run();

        root.getChildren().addAll(title, btnAdd, slotList);
        return wrapInScrollPane(root);
    }

    private HBox buildAvailabilityCard(MentorAvailability slot, Runnable onDelete) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 8;
            -fx-border-color: #e2e8f0;
            -fx-border-radius: 8;
            """);

        Label dateLabel = styledLabel((slot.getDate() != null ? slot.getDate().toString() : "—"), "card-detail");
        Label timeLabel = styledLabel(fmtTime(slot.getStartTime()) + " - " + fmtTime(slot.getEndTime()), "card-detail");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            availabilityService.delete(slot.getId());
            onDelete.run();
        });

        card.getChildren().addAll(dateLabel, timeLabel, spacer, btnDelete);
        return card;
    }

    private void openAddAvailabilityDialog(Runnable onSuccess) {
        Dialog<MentorAvailability> dialog = new Dialog<>();
        dialog.setTitle("Nouveau créneau");
        dialog.setHeaderText("Ajouter une disponibilité");

        ButtonType addType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField startField = new TextField("09:00");
        startField.setPromptText("HH:mm");
        TextField endField = new TextField("10:00");
        endField.setPromptText("HH:mm");

        grid.addRow(0, new Label("Date :"), datePicker);
        grid.addRow(1, new Label("Début :"), startField);
        grid.addRow(2, new Label("Fin :"), endField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addType && datePicker.getValue() != null) {
                MentorAvailability av = new MentorAvailability();
                av.setMentorId(currentUser.getId());
                av.setDate(Date.valueOf(datePicker.getValue()));
                try {
                    String s = startField.getText().trim();
                    if (s.length() == 5) s += ":00";
                    av.setStartTime(java.sql.Time.valueOf(s));
                    String e2 = endField.getText().trim();
                    if (e2.length() == 5) e2 += ":00";
                    av.setEndTime(java.sql.Time.valueOf(e2));
                } catch (Exception ex) {
                    showToast("Format d'heure invalide (HH:mm)");
                    return null;
                }
                return av;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(av -> {
            availabilityService.add(av);
            showToast("Creneau ajoute !");
            onSuccess.run();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Label styledLabel(String text, String styleClass) {
        Label l = new Label(text);
        switch (styleClass) {
            case "section-title" -> l.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            case "section-subtitle" -> l.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            case "card-title" -> l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            case "card-detail" -> l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        }
        return l;
    }

    private Label statusLabel(String status) {
        String color = switch (status) {
            case "auto_accepted" -> "#10b981";
            case "pending_review" -> "#f59e0b";
            case "rejected" -> "#ef4444";
            case "cancelled" -> "#94a3b8";
            case "completed" -> "#3b82f6";
            default -> "#64748b";
        };
        String display = switch (status) {
            case "auto_accepted" -> "Acceptée";
            case "pending_review" -> "En attente";
            case "rejected" -> "Refusée";
            case "cancelled" -> "Annulée";
            case "completed" -> "Terminée";
            default -> status;
        };
        Label l = new Label(display);
        l.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private Label sessionStatusLabel(String status) {
        String color = switch (status) {
            case "scheduled" -> "#3b82f6";
            case "completed" -> "#10b981";
            case "cancelled" -> "#ef4444";
            case "no_show" -> "#f59e0b";
            default -> "#64748b";
        };
        String display = switch (status) {
            case "scheduled" -> "Planifiée";
            case "completed" -> "Terminée";
            case "cancelled" -> "Annulée";
            case "no_show" -> "Absent";
            default -> status;
        };
        Label l = new Label(display);
        l.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private HBox statBadge(String label, String value, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 20, 10, 20));
        box.setStyle("-fx-background-color: " + color + "11; -fx-background-radius: 10; -fx-border-color: " + color + "33; -fx-border-radius: 10;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        box.getChildren().addAll(val, lbl);
        HBox wrap = new HBox(box);
        return wrap;
    }

    private VBox emptyState(String titleText, String subtitleText) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("X");
        icon.setStyle("-fx-font-size: 48px;");
        Label t = new Label(titleText);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        Label s = new Label(subtitleText);
        s.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");
        box.getChildren().addAll(icon, t, s);
        return box;
    }

    private ScrollPane wrapInScrollPane(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setStyle("""
            -fx-background-color: #1e293b;
            -fx-text-fill: white;
            -fx-padding: 10 24;
            -fx-background-radius: 8;
            -fx-font-size: 13px;
            """);
        toast.setTranslateY(-20);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        contentArea.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
            fadeOut.setDelay(Duration.seconds(2));
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e2 -> contentArea.getChildren().remove(toast));
            fadeOut.play();
        });
        fadeIn.play();
    }

    private String safe(String s) { return s != null ? s : "-"; }
    private String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
    private String fmtTime(java.sql.Time t) { return t != null ? t.toString().substring(0, 5) : "-"; }
}
