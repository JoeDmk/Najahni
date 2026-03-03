package controllers.mentorat;

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
import models.mentorat.MentorAvailability;
import services.SessionService;
import services.mentorat.ServiceMentorAvailability;

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
    private TextField searchField;

    private ServiceMentorAvailability service;
    private ObservableList<MentorAvailability> masterList;
    private int currentUserId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceMentorAvailability();
        try { currentUserId = SessionService.getInstance().getCurrentUser().getId(); }
        catch (Exception ex) { currentUserId = 0; }
        loadData();

        // Listen for changes in search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
        });

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnDelete.setOnAction(e -> handleDelete());
    }

    private void loadData() {
        ObservableList<MentorAvailability> all = FXCollections.observableArrayList(service.getAll());
        masterList = FXCollections.observableArrayList();
        for (MentorAvailability a : all) {
            if (a.getMentorId() == currentUserId) {
                masterList.add(a);
            }
        }
        tableView.setItems(masterList);
    }

    private void filterData(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tableView.setItems(masterList);
            return;
        }

        ObservableList<MentorAvailability> filteredList = FXCollections.observableArrayList();
        for (MentorAvailability item : masterList) {
            if (item.getMentorName() != null && item.getMentorName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(item);
            }
        }
        tableView.setItems(filteredList);
    }

    @FXML
    private void handleAdd() {
        navigateToForm(null);
    }

    @FXML
    private void handleEdit() {
        MentorAvailability selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getMentorId() != currentUserId) {
                showAlert("Vous ne pouvez modifier que vos propres disponibilites");
                return;
            }
            navigateToForm(selected);
        } else
            showAlert("Select an item");
    }

    @FXML
    private void handleDelete() {
        MentorAvailability selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getMentorId() != currentUserId) {
                showAlert("Vous ne pouvez supprimer que vos propres disponibilites");
                return;
            }
            service.delete(selected.getId());
            loadData();
        } else
            showAlert("Select an item");
    }

    @FXML
    private void handleRequests() {
        navigateTo("/views/mentorat/MentorshipRequestList.fxml");
    }

    @FXML
    private void handleSessions() {
        navigateTo("/views/mentorat/MentorshipSessionList.fxml");
    }

    private void navigateToForm(MentorAvailability obj) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mentorat/MentorAvailabilityForm.fxml"));
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

    @FXML
    private void handleChatbot() {
        navigateTo("/views/mentorat/Chatbot.fxml");
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.show();
    }

    @FXML
    private void goToHome() {
        tools.NavigationHelper.goHome(tableView);
    }

    @FXML
    private void handleGoBack() {
        goToHome();
    }

    @FXML
    private void handleGoHome() {
        goToHome();
    }
}