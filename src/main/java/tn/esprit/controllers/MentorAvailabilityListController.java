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
import tn.esprit.models.MentorAvailability;
import tn.esprit.services.ServiceMentorAvailability;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MentorAvailabilityListController implements Initializable {

    @FXML
    private TableView<MentorAvailability> tableView;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnSessions;

    private ServiceMentorAvailability service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorAvailability();
        loadData();

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnDelete.setOnAction(e -> handleDelete());
        btnRequests.setOnAction(e -> handleRequests());
        btnSessions.setOnAction(e -> handleSessions());
    }

    private void loadData() {
        ObservableList<MentorAvailability> list = FXCollections.observableArrayList(service.getAll());
        tableView.setItems(list);
    }

    @FXML
    private void handleAdd() {
        navigateToForm(null);
    }

    @FXML
    private void handleEdit() {
        MentorAvailability selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null)
            navigateToForm(selected);
        else
            showAlert("Select an item");
    }

    @FXML
    private void handleDelete() {
        MentorAvailability selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.delete(selected.getId());
            loadData();
        } else
            showAlert("Select an item");
    }

    @FXML
    private void handleRequests() {
        navigateTo("/FXML/MentorshipRequestList.fxml");
    }

    @FXML
    private void handleSessions() {
        navigateTo("/FXML/MentorshipSessionList.fxml");
    }

    private void navigateToForm(MentorAvailability obj) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MentorAvailabilityForm.fxml"));
            Parent root = loader.load();
            if (obj != null) {
                MentorAvailabilityFormController c = loader.getController();
                c.setAvailability(obj);
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
