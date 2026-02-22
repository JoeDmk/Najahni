package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.models.MentorshipRequest;
import tn.esprit.models.Projet;
import tn.esprit.models.User;
import tn.esprit.services.ServiceMentorshipRequest;
import tn.esprit.services.ServiceProjet;
import tn.esprit.services.ServiceUser;
import tn.esprit.utils.AudioTranscriber;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MentorshipRequestFormController implements Initializable {

    @FXML
    private ComboBox<User> cbEntrepreneur;
    @FXML
    private DatePicker dpDate;
    @FXML
    private TextField tfTime;
    @FXML
    private ComboBox<User> cbMentor;
    @FXML
    private ComboBox<Projet> cbProject;
    @FXML
    private ComboBox<MentorshipRequest.RequestStatus> cbStatus;
    @FXML
    private CheckBox chkAutoApproved;
    @FXML
    private TextArea taMotivation;
    @FXML
    private TextArea taGoals;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnChatbot;
    @FXML
    private Button btnAvailability;
    @FXML
    private Button btnMicGoals;
    @FXML
    private Button btnMicMotivation;

    private ServiceMentorshipRequest service;
    private ServiceUser serviceUser;
    private ServiceProjet serviceProjet;
    private MentorshipRequest currentRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipRequest();
        serviceUser = new ServiceUser();
        serviceProjet = new ServiceProjet();

        loadComboBoxes();

        // Listener for Date and Time
        dpDate.valueProperty().addListener((obs, oldVal, newVal) -> filterMentors());
        tfTime.textProperty().addListener((obs, oldVal, newVal) -> filterMentors());

        btnSave.setOnAction(e -> save());
        btnCancel.setOnAction(e -> navigateBack());
        btnSessions.setOnAction(e -> navigateTo("/FXML/MentorshipSessionList.fxml"));
        btnAvailability.setOnAction(e -> navigateTo("/FXML/MentorAvailabilityList.fxml"));
        btnChatbot.setOnAction(e -> handleChatbot());
        if (btnMicMotivation != null) {
            btnMicMotivation.setOnAction(e -> AudioTranscriber.recordAndTranscribe(taMotivation, btnMicMotivation, 10));
        }
        if (btnMicGoals != null) {
            btnMicGoals.setOnAction(e -> AudioTranscriber.recordAndTranscribe(taGoals, btnMicGoals, 10));
        }
    }

    private void filterMentors() {
        if (dpDate.getValue() != null && tfTime.getText() != null && !tfTime.getText().isEmpty()) {
            java.sql.Date date = java.sql.Date.valueOf(dpDate.getValue());
            String time = tfTime.getText();
            // Validate time format? Simple check for now.
            try {
                cbMentor.setItems(FXCollections.observableArrayList(serviceUser.getAvailableMentors(date, time)));
            } catch (Exception e) {
                System.out.println("Error filtering mentors: " + e.getMessage());
                cbMentor.getItems().clear();
            }
        } else {
            cbMentor.getItems().clear();
        }
    }

    private void loadComboBoxes() {
        cbEntrepreneur.setItems(FXCollections.observableArrayList(serviceUser.getByRole("ENTREPRENEUR")));
        // Mentors loaded after date/time selection
        // cbMentor.setItems(FXCollections.observableArrayList(serviceUser.getAvailableMentors()));
        cbProject.setItems(FXCollections.observableArrayList(serviceProjet.getAll()));
        cbStatus.setItems(FXCollections.observableArrayList(MentorshipRequest.RequestStatus.values()));

        // Setup string converters for displaying nice names in ComboBox
        StringConverter<User> userConverter = new StringConverter<User>() {
            @Override
            public String toString(User object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        };
        cbEntrepreneur.setConverter(userConverter);
        cbMentor.setConverter(userConverter);

        cbProject.setConverter(new StringConverter<Projet>() {
            @Override
            public String toString(Projet object) {
                return object == null ? "" : object.getTitre();
            }

            @Override
            public Projet fromString(String string) {
                return null;
            }
        });
    }

    public void setMentorshipRequest(MentorshipRequest request) {
        this.currentRequest = request;

        // Select items in ComboBoxes
        for (User u : cbEntrepreneur.getItems())
            if (u.getId() == request.getEntrepreneurId())
                cbEntrepreneur.setValue(u);

        // Populate mentors based on request date/time, then select
        if (request.getDate() != null && request.getTime() != null) {
            dpDate.setValue(request.getDate().toLocalDate());
            tfTime.setText(request.getTime());
            filterMentors(); // Load mentors
        }

        for (User u : cbMentor.getItems())
            if (u.getId() == request.getMentorId())
                cbMentor.setValue(u);

        for (Projet p : cbProject.getItems())
            if (p.getId() == request.getProjectId())
                cbProject.setValue(p);

        cbStatus.setValue(request.getStatus());
        chkAutoApproved.setSelected(request.isAutoApproved());
        taMotivation.setText(request.getMotivation());
        taGoals.setText(request.getGoals());
    }

    private void save() {
        if (cbEntrepreneur.getValue() == null || cbMentor.getValue() == null || cbProject.getValue() == null
                || cbStatus.getValue() == null || dpDate.getValue() == null || tfTime.getText().isEmpty()) {
            showAlert("Please fill in all fields (including Date and Time).");
            return;
        }
        if (taMotivation.getText().isEmpty() || taGoals.getText().isEmpty()) {
            showAlert("Motivation and Goals cannot be empty.");
            return;
        }

        MentorshipRequest req = currentRequest != null ? currentRequest : new MentorshipRequest();
        req.setEntrepreneurId(cbEntrepreneur.getValue().getId());
        req.setMentorId(cbMentor.getValue().getId());
        req.setProjectId(cbProject.getValue().getId());
        req.setDate(java.sql.Date.valueOf(dpDate.getValue()));
        req.setTime(tfTime.getText());
        req.setStatus(cbStatus.getValue());
        req.setMatchScore(0.0f); // default to 0
        req.setAutoApproved(chkAutoApproved.isSelected());
        req.setMotivation(taMotivation.getText());
        req.setGoals(taGoals.getText());

        if (currentRequest == null) {
            service.add(req);
        } else {
            service.update(req);
        }
        navigateBack();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validation Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean validator(String str) {
        try {
            Float.parseFloat(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void navigateBack() {
        navigateTo("/FXML/MentorshipRequestList.fxml");
    }

    @FXML
    private void handleChatbot() {
        navigateTo("/FXML/Chatbot.fxml");
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
}
