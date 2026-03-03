package controllers.mentorat;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.mentorat.MentorshipRequest;
import models.mentorat.MentorshipSession;
import services.mentorat.ServiceMentorshipRequest;
import services.mentorat.ServiceMentorshipSession;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class MentorshipSessionFormController implements Initializable {

    @FXML
    private ComboBox<MentorshipRequest> cbRequest;
    @FXML
    private DatePicker dpScheduledDate;
    @FXML
    private TextField tfScheduledTime;
    @FXML
    private TextField tfDuration;
    @FXML
    private ComboBox<MentorshipSession.SessionStatus> cbStatus;
    @FXML
    private TextField tfMeetingLink;
    @FXML
    private TextArea taMentorFeedback;
    @FXML
    private TextArea taEntrepreneurFeedback;
    @FXML
    private ComboBox<Integer> cbMentorRating;
    @FXML
    private ComboBox<Integer> cbEntrepreneurRating;

    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnAvailability;
    @FXML
    private Button btnChatbot;

    private ServiceMentorshipSession service;
    private ServiceMentorshipRequest serviceRequest;
    private MentorshipSession currentSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipSession();
        serviceRequest = new ServiceMentorshipRequest();

        cbRequest.setItems(FXCollections.observableArrayList(serviceRequest.getApprovedRequests()));
        cbStatus.setItems(FXCollections.observableArrayList(MentorshipSession.SessionStatus.values()));
        cbMentorRating.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        cbEntrepreneurRating.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        btnSave.setOnAction(e -> save());
        btnCancel.setOnAction(e -> navigateBack());
        if (btnRequests != null) btnRequests.setOnAction(e -> navigateTo("/views/mentorat/MentorshipRequestList.fxml"));
        if (btnAvailability != null) btnAvailability.setOnAction(e -> navigateTo("/views/mentorat/MentorAvailabilityList.fxml"));
        if (btnChatbot != null) btnChatbot.setOnAction(e -> handleChatbot());
    }

    public void setSession(MentorshipSession session) {
        this.currentSession = session;
        for (MentorshipRequest r : cbRequest.getItems())
            if (r.getId() == session.getRequestId())
                cbRequest.setValue(r);

        if (session.getScheduledAt() != null) {
            LocalDateTime dt = session.getScheduledAt().toLocalDateTime();
            dpScheduledDate.setValue(dt.toLocalDate());
            tfScheduledTime.setText(dt.toLocalTime().toString());
        }

        tfDuration.setText(String.valueOf(session.getDurationMinutes()));
        cbStatus.setValue(session.getStatus());
        tfMeetingLink.setText(session.getMeetingLink());
        taMentorFeedback.setText(session.getMentorFeedback());
        taEntrepreneurFeedback.setText(session.getEntrepreneurFeedback());
        cbMentorRating.setValue(session.getMentorRating());
        cbEntrepreneurRating.setValue(session.getEntrepreneurRating());
    }

    private void save() {
        if (cbRequest.getValue() == null || dpScheduledDate.getValue() == null || cbStatus.getValue() == null) {
            showAlert("Please fill in Request, Schedule Date, and Status.");
            return;
        }

        if (tfScheduledTime.getText().isEmpty()
                || !tfScheduledTime.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            showAlert("Time must be in HH:mm format.");
            return;
        }

        if (tfDuration.getText().isEmpty() || !tfDuration.getText().matches("\\d+")) {
            showAlert("Duration must be a number (minutes).");
            return;
        }

        if (tfMeetingLink.getText().isEmpty()) {
            showAlert("Meeting link is required.");
            return;
        }

        MentorshipSession s = currentSession != null ? currentSession : new MentorshipSession();
        s.setRequestId(cbRequest.getValue().getId());

        try {
            LocalTime time = LocalTime.parse(tfScheduledTime.getText());
            LocalDateTime dt = LocalDateTime.of(dpScheduledDate.getValue(), time);
            s.setScheduledAt(Timestamp.valueOf(dt));
        } catch (Exception e) {
            showAlert("Invalid Date/Time combination.");
            return;
        }

        s.setDurationMinutes(Integer.parseInt(tfDuration.getText()));
        s.setStatus(cbStatus.getValue());
        s.setMeetingLink(tfMeetingLink.getText());
        s.setMentorFeedback(taMentorFeedback.getText());
        s.setEntrepreneurFeedback(taEntrepreneurFeedback.getText());
        s.setMentorRating(cbMentorRating.getValue() != null ? cbMentorRating.getValue() : 0);
        s.setEntrepreneurRating(cbEntrepreneurRating.getValue() != null ? cbEntrepreneurRating.getValue() : 0);

        if (currentSession == null)
            service.add(s);
        else
            service.update(s);
        navigateBack();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void navigateBack() {
        navigateTo("/views/mentorat/MentorshipSessionList.fxml");
    }

    @FXML
    private void handleChatbot() {
        navigateTo("/views/mentorat/Chatbot.fxml");
    }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToHome() {
        tools.NavigationHelper.goHome(btnCancel);
    }

    @FXML
    private void handleGoBack() {
        navigateBack();
    }

    @FXML
    private void handleGoHome() {
        goToHome();
    }
}