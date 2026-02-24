package Controller;

import Entites.Event;
import Entites.EventParticipant;
import Services.AiEventService;
import Services.EmailAsync;
import Services.EventParticipantCRUD;
import Services.NotificationEmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class EventDetailsController {

    @FXML private javafx.scene.control.TextArea eventAiOutputLabel;
    private final AiEventService aiEventService = new AiEventService();
    @FXML
    private Button validateTicketBtn;

    @FXML
    private Label capacityLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Button joinBtn;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private VBox participantsContainer;

    private Event event;


    // TODO: Replace with SessionService.getInstance().getCurrentUser().getId()
    // Temporary logged user (until integration with User module)
    private final int currentUserId = 1;

    // Better practice: single CRUD instance
    private final EventParticipantCRUD crud = new EventParticipantCRUD();
    private String safe(String s){ return s == null ? "" : s; }

    private String baseEventContext() {
        return "EVENT CONTEXT:\n" +
                "Title: " + safe(event.getTitle()) + "\n" +
                "Description: " + safe(event.getDescription()) + "\n" +
                "Capacity: " + event.getCapacity() + "\n";
    }
    @FXML
    private void generateEventSummary() {
        eventAiOutputLabel.setText("⏳ Generating summary...");

        EmailAsync.run(() -> {
            try {
                String prompt =
                        "TASK: Write a neutral factual summary of the event.\n" +
                                " If details are unclear, keep it generic and safe.\n"+
                                "Rules:\n" +
                                "- Neutral tone.\n" +
                                "- No marketing language.\n" +
                                "- No invitation phrases like 'Join us'.\n" +
                                "- No persuasive tone.\n" +
                                "- 1-2 sentences only.\n\n" +
                                baseEventContext();

                String out = aiEventService.generate(prompt);

                Platform.runLater(() -> eventAiOutputLabel.setText(out));
            } catch (Exception ex) {
                Platform.runLater(() -> eventAiOutputLabel.setText("❌ AI failed: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void generateEventPromo() {
        eventAiOutputLabel.setText("⏳ Generating promo text...");

        EmailAsync.run(() -> {
            try {
                String prompt =
                        "TASK: Write a short promotional text for this event.\n" +
                                "Rules:\n" +
                                "- Friendly tone.\n" +
                                "- 2–3 lines max.\n" +
                                "- Do NOT invent details.\n" +
                                "- If details are missing, stay generic WITHOUT saying you lack info.\n" +
                                "- Do NOT include meta text like “I can’t…” or policy explanations.\n\n" +
                                baseEventContext();

                String out = aiEventService.generate(prompt);

                Platform.runLater(() -> eventAiOutputLabel.setText(out));
            } catch (Exception ex) {
                Platform.runLater(() -> eventAiOutputLabel.setText("❌ AI failed: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void generateEventChecklist() {
        eventAiOutputLabel.setText("⏳ Generating checklist...");

        EmailAsync.run(() -> {
            try {
                String prompt =
                        "Create a short preparation checklist for the event.\n" +
                                "\" If details are unclear, keep it generic and safe.\\n\"Rules: max 3 bullet points. No invented details. Practical.\n\n" +
                                baseEventContext();

                String out = aiEventService.generate(prompt);

                Platform.runLater(() -> eventAiOutputLabel.setText(out));
            } catch (Exception ex) {
                Platform.runLater(() -> eventAiOutputLabel.setText("❌ AI failed: " + ex.getMessage()));
            }
        });
    }
    public void setEvent(Event event) {
        this.event = event;
        boolean isCreator = isCurrentUserCreator();
        validateTicketBtn.setVisible(isCreator);
        validateTicketBtn.setManaged(isCreator);

        titleLabel.setText(event.getTitle());
        descriptionLabel.setText(event.getDescription());
        dateLabel.setText("Date: " + event.getEventDate().toLocalDateTime());

        updateJoinButton();
        loadParticipants();
    }
    @FXML
    private void onValidateTicket() {

        // extra safety (even if button hidden)
        if (!isCurrentUserCreator()) {
            showAlert("Only the event creator can validate tickets.");
            return;
        }

        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>("Paste payload", "Paste payload", "Upload QR image");

        dialog.setTitle("Ticket Validation");
        dialog.setHeaderText("Choose validation method");
        dialog.setContentText("Method:");

        dialog.showAndWait().ifPresent(choice -> {
            if (choice.equals("Paste payload")) {
                validateByPastingPayload();
            } else {
                validateByUploadingQrImage();
            }
        });
    }
    private void validateByPastingPayload() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Ticket Validation");
        dialog.setHeaderText("Paste QR payload text");
        dialog.setContentText("Payload:");

        dialog.showAndWait().ifPresent(payload -> {
            try {
                boolean ok = validatePayload(payload);
                showAlert(ok ? "✅ VALID TICKET" : "❌ INVALID / TAMPERED");
            } catch (Exception ex) {
                showAlert("❌ Error: " + ex.getMessage());
            }
        });
    }

    private void validateByUploadingQrImage() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Select QR image");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        java.io.File file = fc.showOpenDialog(participantsContainer.getScene().getWindow());
        if (file == null) return;

        try {
            String payload = Services.QrDecodeUtil.decodeFromImage(file);
            boolean ok = validatePayload(payload);
            showAlert((ok ? "✅ VALID TICKET\n\n" : "❌ INVALID / TAMPERED\n\n") + payload);
        } catch (Exception ex) {
            showAlert("❌ Could not decode QR: " + ex.getMessage());
        }
    }
    private boolean validatePayload(String payload) {

        int eId = getIntFromPayload(payload, "e");
        int uId = getIntFromPayload(payload, "u");
        String sigFromQr = getStringFromPayload(payload, "sig");

        // ✅ Special message ONLY for wrong event
        if (eId != event.getId()) {
            throw new IllegalArgumentException("❌ This ticket is NOT for this event.");
        }

        // read signed text fields (because you included them in signature)
        String eventTitleFromQr = getStringFromPayload(payload, "event");
        String userFromQr = getStringFromPayload(payload, "user");

        // MUST match generation format
        String eventDateIso = event.getEventDate().toLocalDateTime().toString();

        // ✅ recompute expected signature using SAME fields
        String expected = Services.TicketSigner.signature(
                eId, uId, eventDateIso, eventTitleFromQr, userFromQr
        );

        // Any other tampering -> invalid (no extra detail)
        return expected.equals(sigFromQr);
    }



    private int getIntFromPayload(String payload, String key) {
        return Integer.parseInt(getStringFromPayload(payload, key));
    }

    private String getStringFromPayload(String payload, String key) {
        for (String part : payload.split("\\|")) {
            if (part.startsWith(key + "=")) return part.substring((key + "=").length());
        }
        throw new IllegalArgumentException("Missing " + key);
    }



    private void updateJoinButton() {

        try {

            boolean isParticipant =
                    crud.isUserParticipant(event.getId(), currentUserId);

            int currentCount =
                    crud.countParticipants(event.getId());

            if (isParticipant) {

                joinBtn.setText("Leave");
                joinBtn.setDisable(false);
                joinBtn.setOnAction(e -> leaveEvent());

            } else {

                if (currentCount >= event.getCapacity()) {
                    joinBtn.setText("Event Full");
                    joinBtn.setDisable(true);
                } else {
                    joinBtn.setText("Join");
                    joinBtn.setDisable(false);
                    joinBtn.setOnAction(e -> joinEvent());
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void leaveEvent() {

        // 🔴 IF CURRENT USER IS CREATOR
        if (isCurrentUserCreator()) {

            if (!confirmCreatorLeave())
                return;

            try {

                // Delete entire event
                new Services.EventCRUD().supprimer(event.getId());

                goBack(); // return to events page

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            return;
        }

        // 🟢 NORMAL MEMBER LEAVING
        if (!confirmAction("Leave Event",
                "Are you sure you want to leave this event?"))
            return;

        try {

            crud.leaveEvent(event.getId(), currentUserId);

            updateJoinButton();
            loadParticipants();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    private void joinEvent() {
        try {
            int currentCount = crud.countParticipants(event.getId());

            if (currentCount >= event.getCapacity()) {
                showAlert("Event is full!");
                return;
            }

            crud.ajouter(new EventParticipant(event.getId(), currentUserId));

            // ✅ synchronous email to the JOINER
            Services.EmailAsync.run(() -> {
                new Services.NotificationEmailService()
                        .sendEventJoinMailToJoiner(event.getId(), currentUserId);
            });
            updateJoinButton();
            loadParticipants();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean isCurrentUserCreator() {
        return event.getUserId() == currentUserId;
    }

    private void loadParticipants() {

        participantsContainer.getChildren().clear();

        try {

            List<EventParticipant> participants =
                    crud.getParticipantsByEvent(event.getId());

            int currentCount = participants.size();
            capacityLabel.setText(
                    "Participants: " + currentCount + " / " + event.getCapacity()
            );

            boolean isCreator = isCurrentUserCreator();

            for (EventParticipant participant : participants) {

                HBox row = new HBox(10);

                Label userLabel = new Label(participant.getFirstname());

                // 🔥 ROLE LABEL
                Label roleLabel;

                if (participant.getUserId() == event.getUserId()) {
                    roleLabel = new Label("(Creator)");
                    roleLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    roleLabel = new Label("(Member)");
                }

                row.getChildren().addAll(userLabel, roleLabel);

                // 🔥 SHOW KICK ONLY IF CURRENT USER IS CREATOR
                if (isCreator && participant.getUserId() != event.getUserId()) {

                    Button kickBtn = new Button("Kick");
                    kickBtn.setStyle("-fx-background-color:red; -fx-text-fill:white;");

                    kickBtn.setOnAction(e -> {

                        if (!confirmAction("Kick Participant",
                                "Are you sure you want to remove this participant?"))
                            return;

                        try {
                            crud.kickUser(event.getId(), participant.getUserId());

                            updateJoinButton();   // 🔥 refresh join button
                            loadParticipants();   // 🔥 refresh participants list

                        } catch (SQLException ex) {
                            System.out.println(ex.getMessage());
                        }
                    });


                    row.getChildren().add(kickBtn);
                }

                participantsContainer.getChildren().add(row);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private boolean confirmCreatorLeave() {

        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING);

        alert.setTitle("Delete Event");
        alert.setHeaderText("You are the CREATOR of this event!");
        alert.setContentText(
                "If you leave, the entire event will be permanently deleted.\n\n" +
                        "All participants will be removed.\n\n" +
                        "This action CANNOT be undone.\n\n" +
                        "Do you want to continue?"
        );

        ButtonType deleteBtn = new ButtonType("Delete Event");
        ButtonType cancelBtn =
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(deleteBtn, cancelBtn);

        return alert.showAndWait().orElse(cancelBtn) == deleteBtn;
    }

    private boolean confirmAction(String title, String message) {

        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yesBtn = new ButtonType("Yes");
        ButtonType cancelBtn =
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yesBtn, cancelBtn);

        return alert.showAndWait().orElse(cancelBtn) == yesBtn;
    }


    private void showAlert(String message) {

        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);

        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/EventsPage.fxml")
            );
            participantsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
