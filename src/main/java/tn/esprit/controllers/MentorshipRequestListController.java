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

    private ServiceMentorshipRequest service;
    private ObservableList<MentorshipRequest> requestList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorshipRequest();
        loadData();

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
    }

    private void loadData() {
        requestList = FXCollections.observableArrayList(service.getAll());
        tableView.setItems(requestList);
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
