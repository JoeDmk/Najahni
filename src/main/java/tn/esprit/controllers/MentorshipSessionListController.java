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
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import tn.esprit.models.MentorshipSession;
import tn.esprit.services.ServiceMentorshipSession;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MentorshipSessionListController implements Initializable {

    @FXML
    private TableView<MentorshipSession> tableView;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnAvailability;

    private ServiceMentorshipSession service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipSession();
        loadData();

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnDelete.setOnAction(e -> handleDelete());
        btnRequests.setOnAction(e -> handleRequests());
        btnAvailability.setOnAction(e -> handleAvailability());
    }

    private void loadData() {
        ObservableList<MentorshipSession> list = FXCollections.observableArrayList(service.getAll());
        tableView.setItems(list);
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

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.show();
    }
}
