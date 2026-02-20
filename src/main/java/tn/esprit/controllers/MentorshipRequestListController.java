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
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.models.MentorshipRequest;
import tn.esprit.services.ServiceMentorshipRequest;

import java.io.IOException;
import java.net.URL;

import java.util.ResourceBundle;

public class MentorshipRequestListController implements Initializable {

    @FXML
    private TableView<MentorshipRequest> tableView;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnSessions;
    @FXML
    private Button btnAvailability;
    @FXML
    private Button btnSort;

    @FXML
    private TextField searchField;

    private ServiceMentorshipRequest service;
    private ObservableList<MentorshipRequest> masterList;
    private int currentStatusIndex = 0;
    private final MentorshipRequest.RequestStatus[] statuses = MentorshipRequest.RequestStatus.values();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipRequest();
        loadData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
        });

        btnAdd.setOnAction(e -> navigateToForm(null));
        btnEdit.setOnAction(e -> {
            MentorshipRequest selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigateToForm(selected);
            } else {
                showAlert("Select a Request to edit");
            }
        });

        btnDelete.setOnAction(e -> {
            MentorshipRequest selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.delete(selected.getId());
                loadData();
            } else {
                showAlert("Select a Request to delete");
            }
        });

        btnSessions.setOnAction(e -> navigateTo("/FXML/MentorshipSessionList.fxml"));
        btnAvailability.setOnAction(e -> navigateTo("/FXML/MentorAvailabilityList.fxml"));
        btnSort.setOnAction(e -> sortByStatus());
    }

    private void loadData() {
        masterList = FXCollections.observableArrayList(service.getAll());
        tableView.setItems(masterList);
    }

    private void sortByStatus() {
        // currentStatusIndex: 0 = All, 1 = auto_accepted, 2 = pending_review, 3 = rejected, 4 = cancelled, 5 = completed
        currentStatusIndex++;
        if (currentStatusIndex > statuses.length) {
            currentStatusIndex = 0;
        }

        if (currentStatusIndex == 0) {
            // Show all requests
            tableView.setItems(masterList);
            btnSort.setText("Status: All");
        } else {
            MentorshipRequest.RequestStatus selectedStatus = statuses[currentStatusIndex - 1];
            ObservableList<MentorshipRequest> filteredList = FXCollections.observableArrayList();
            for (MentorshipRequest request : masterList) {
                if (request.getStatus() == selectedStatus) {
                    filteredList.add(request);
                }
            }
            tableView.setItems(filteredList);
            // Display a readable label for the status
            String label = selectedStatus.name().replace("_", " ");
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            btnSort.setText("Status: " + label);
        }
    }

    private void filterData(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tableView.setItems(masterList);
            return;
        }

        ObservableList<MentorshipRequest> filteredList = FXCollections.observableArrayList();
        for (MentorshipRequest request : masterList) {
            boolean matches = false;
            // Search by Mentor Name
            if (request.getMentorName() != null && request.getMentorName().toLowerCase().contains(keyword.toLowerCase())) {
                matches = true;
            }
            // Search by Entrepreneur Name
            if (request.getEntrepreneurName() != null && request.getEntrepreneurName().toLowerCase().contains(keyword.toLowerCase())) {
                matches = true;
            }
            
            if (matches) {
                filteredList.add(request);
            }
        }
        tableView.setItems(filteredList);
    }

    private void navigateToForm(MentorshipRequest request) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/MentorshipRequestForm.fxml"));
            Parent root = loader.load();

            if (request != null) {
                MentorshipRequestFormController controller = loader.getController();
                controller.setMentorshipRequest(request);
            }

            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }
}
