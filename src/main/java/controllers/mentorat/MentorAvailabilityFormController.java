package controllers.mentorat;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.mentorat.MentorAvailability;
import models.User;
import services.mentorat.ServiceMentorAvailability;
import services.mentorat.ServiceUser;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.util.ResourceBundle;

public class MentorAvailabilityFormController implements Initializable {

    @FXML
    private ComboBox<User> cbMentor;
    @FXML
    private DatePicker dpDate;
    @FXML
    private TextField tfStartTime;
    @FXML
    private TextField tfEndTime;

    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnChatbot;

    private ServiceMentorAvailability service;
    private ServiceUser serviceUser;
    private MentorAvailability currentAv;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorAvailability();
        serviceUser = new ServiceUser();

        cbMentor.setItems(FXCollections.observableArrayList(serviceUser.getByRole("MENTOR")));
        cbMentor.setConverter(new javafx.util.StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getFullName(); }
            @Override public User fromString(String s) { return null; }
        });

        btnSave.setOnAction(e -> save());
        btnCancel.setOnAction(e -> navigateBack());
        if (btnRequests != null) btnRequests.setOnAction(e -> navigateTo("/views/mentorat/MentorshipRequestList.fxml"));
        if (btnSessions != null) btnSessions.setOnAction(e -> navigateTo("/views/mentorat/MentorshipSessionList.fxml"));
        if (btnChatbot != null) btnChatbot.setOnAction(e -> handleChatbot());
    }

    public void setAvailability(MentorAvailability av) {
        this.currentAv = av;
        for (User u : cbMentor.getItems())
            if (u.getId() == av.getMentorId())
                cbMentor.setValue(u);

        if (av.getDate() != null)
            dpDate.setValue(av.getDate().toLocalDate());

        tfStartTime.setText(av.getStartTime().toString());
        tfEndTime.setText(av.getEndTime().toString());
    }

    private void save() {
        if (cbMentor.getValue() == null || dpDate.getValue() == null) {
            showAlert("Select Mentor and Date.");
            return;
        }

        if (!isValidTime(tfStartTime.getText()) || !isValidTime(tfEndTime.getText())) {
            showAlert("Times must be in HH:mm or HH:mm:ss format.");
            return;
        }

        MentorAvailability av = currentAv != null ? currentAv : new MentorAvailability();
        av.setMentorId(cbMentor.getValue().getId());
        av.setDate(java.sql.Date.valueOf(dpDate.getValue()));
        try {
            av.setStartTime(Time.valueOf(formatTime(tfStartTime.getText())));
            av.setEndTime(Time.valueOf(formatTime(tfEndTime.getText())));
        } catch (Exception e) {
            showAlert("Invalid Time Values.");
            return;
        }

        if (currentAv == null)
            service.add(av);
        else
            service.update(av);
        navigateBack();
    }

    private boolean isValidTime(String time) {
        return time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$");
    }

    private String formatTime(String time) {
        if (time.length() == 5)
            return time + ":00";
        return time;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void navigateBack() {
        navigateTo("/views/mentorat/MentorAvailabilityList.fxml");
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