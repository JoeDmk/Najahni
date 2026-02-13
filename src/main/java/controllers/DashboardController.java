package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.User;
import services.*;
import util.Type;

import java.util.List;

/**
 * Admin Dashboard Controller - manages all users (Entrepreneurs, Mentors, Investisseurs).
 */
public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label entrepreneursLabel;
    @FXML private Label mentorsLabel;
    @FXML private Label investisseursLabel;
    @FXML private Label bannedLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> firstnameColumn;
    @FXML private TableColumn<User, String> lastnameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> verifiedColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;

    private UserService userService = UserService.getInstance();
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getFullName());
        }
        loadStats();
        loadUsers(null);
    }

    @FXML
    public void initialize() {
        // Table columns setup
        if (idColumn != null) idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (firstnameColumn != null) firstnameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        if (lastnameColumn != null) lastnameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        if (emailColumn != null) emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (phoneColumn != null) phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if (roleColumn != null) roleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole() != null ? cellData.getValue().getRole().name() : ""));
        if (statusColumn != null) statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIsActiveDisplay()));
        if (verifiedColumn != null) verifiedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getVerifiedDisplay()));

        // Role filter
        if (roleFilterCombo != null) {
            roleFilterCombo.getItems().addAll("Tous", "Entrepreneur", "Mentor", "Investisseur", "Admin");
            roleFilterCombo.setValue("Tous");
            roleFilterCombo.setOnAction(e -> handleFilter());
        }

        // Search listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }
    }

    private void loadStats() {
        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(userService.countTotal()));
        if (entrepreneursLabel != null) entrepreneursLabel.setText(String.valueOf(userService.countByRole(Type.ENTREPRENEUR)));
        if (mentorsLabel != null) mentorsLabel.setText(String.valueOf(userService.countByRole(Type.MENTOR)));
        if (investisseursLabel != null) investisseursLabel.setText(String.valueOf(userService.countByRole(Type.INVESTISSEUR)));
        if (bannedLabel != null) bannedLabel.setText(String.valueOf(userService.countBanned()));
    }

    private void loadUsers(Type roleFilter) {
        List<User> users;
        if (roleFilter != null) {
            users = userService.getUsersByRole(roleFilter);
        } else {
            users = userService.getUsers();
        }
        usersList.setAll(users);
        if (usersTable != null) usersTable.setItems(usersList);
    }

    @FXML
    private void handleFilter() {
        String selected = roleFilterCombo.getValue();
        Type filter = switch (selected) {
            case "Entrepreneur" -> Type.ENTREPRENEUR;
            case "Mentor" -> Type.MENTOR;
            case "Investisseur" -> Type.INVESTISSEUR;
            case "Admin" -> Type.ADMIN;
            default -> null;
        };
        loadUsers(filter);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadUsers(null);
        } else {
            usersList.setAll(userService.searchUsers(keyword));
            usersTable.setItems(usersList);
        }
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterUser.fxml"));
            Parent root = loader.load();
            AjouterUserController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBanUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            if (selected.getIsBanned()) {
                userService.unbanUser(selected.getId());
            } else {
                userService.banUser(selected.getId());
            }
            loadUsers(null);
            loadStats();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet utilisateur ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.deleteUser(selected.getId());
                    loadUsers(null);
                    loadStats();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleViewProfile() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileDashboard.fxml"));
            Parent root = loader.load();
            ProfileDashboardController ctrl = loader.getController();
            ctrl.setUser(selected);
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/StatsView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        SessionService.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SignIn.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profil.fxml"));
            Parent root = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refreshData() {
        loadStats();
        loadUsers(null);
    }
}
