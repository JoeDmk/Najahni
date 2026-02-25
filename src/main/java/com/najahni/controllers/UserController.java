package com.najahni.controllers;

import com.najahni.models.Role;
import com.najahni.models.User;
import com.najahni.services.UserService;
import com.najahni.utils.AlertUtils;
import com.najahni.utils.AnimationUtils;
import com.najahni.utils.WrappedTextCellFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for User management view.
 * Handles CRUD operations for users with smooth animations.
 */
public class UserController {

    // Table components
    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colName;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private TableColumn<User, Void> colActions;

    // Search/Filter components
    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cboRoleFilter;

    // Form components
    @FXML
    private VBox formContainer;

    @FXML
    private Label formTitle;

    @FXML
    private TextField txtId;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<Role> cboRole;

    @FXML
    private Label lblFormMessage;

    private final UserService userService;
    private ObservableList<User> usersList;
    private boolean isEditMode = false;

    public UserController() {
        this.userService = new UserService();
    }

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadUsers();
        clearForm();
        
        // Animate components on load
        AnimationUtils.playFadeScaleIn(usersTable, 300, 100);
        AnimationUtils.playFadeScaleIn(formContainer, 300, 200);
    }

    /**
     * Sets up table columns including action buttons.
     */
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRole().getDisplayName()));

        // Apply text wrapping to prevent truncation
        colName.setCellFactory(new WrappedTextCellFactory<>());
        colEmail.setCellFactory(new WrappedTextCellFactory<>());
        colRole.setCellFactory(new WrappedTextCellFactory<>());

        // Setup action buttons column
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-warning");
                editBtn.setStyle("-fx-padding: 5 10;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-padding: 5 10;");

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * Sets up combo boxes with data.
     */
    private void setupComboBoxes() {
        // Role filter combo box
        cboRoleFilter.setItems(FXCollections.observableArrayList("Tous", "Entrepreneur", "Investisseur"));
        cboRoleFilter.setValue("Tous");

        // Role selection combo box for form
        cboRole.setItems(FXCollections.observableArrayList(Role.values()));
    }

    /**
     * Loads all users into the table.
     */
    private void loadUsers() {
        List<User> users = userService.findAll();
        usersList = FXCollections.observableArrayList(users);
        usersTable.setItems(usersList);
    }

    /**
     * Refreshes the table data.
     */
    @FXML
    public void refreshTable() {
        loadUsers();
        clearForm();
        AlertUtils.showSuccess("Données actualisées avec succès !");
    }

    /**
     * Searches users based on search criteria.
     */
    @FXML
    public void searchUsers() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String roleFilter = cboRoleFilter.getValue();

        List<User> filtered = userService.findAll().stream()
            .filter(user -> {
                boolean matchesSearch = searchText.isEmpty() ||
                    user.getName().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText);

                boolean matchesRole = roleFilter == null || roleFilter.equals("Tous") ||
                    user.getRole().getDisplayName().equals(roleFilter);

                return matchesSearch && matchesRole;
            })
            .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Clears search filters.
     */
    @FXML
    public void clearSearch() {
        txtSearch.clear();
        cboRoleFilter.setValue("Tous");
        loadUsers();
    }

    /**
     * Shows the add user form.
     */
    @FXML
    public void showAddForm() {
        clearForm();
        formTitle.setText("Ajouter un Utilisateur");
        isEditMode = false;
    }

    /**
     * Populates form for editing a user.
     */
    private void editUser(User user) {
        isEditMode = true;
        formTitle.setText("Modifier l'Utilisateur");
        
        txtId.setText(String.valueOf(user.getId()));
        txtName.setText(user.getName());
        txtEmail.setText(user.getEmail());
        txtPassword.setText(user.getPassword());
        cboRole.setValue(user.getRole());
        
        lblFormMessage.setText("");
    }

    /**
     * Deletes a user after confirmation.
     */
    private void deleteUser(User user) {
        if (AlertUtils.confirmDelete(user.getName())) {
            boolean deleted = userService.deleteUser(user.getId());
            if (deleted) {
                loadUsers();
                clearForm();
                AlertUtils.showSuccess("Utilisateur supprimé avec succès !");
            } else {
                AlertUtils.showError("Échec de la Suppression", "Impossible de supprimer l'utilisateur. Il peut avoir des projets associés.");
            }
        }
    }

    /**
     * Saves the user (create or update).
     */
    @FXML
    public void saveUser() {
        try {
            // ── Validation UI ──
            String name = txtName.getText() != null ? txtName.getText().trim() : "";
            String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
            String password = txtPassword.getText() != null ? txtPassword.getText() : "";

            if (name.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire.");
            if (name.length() < 2) throw new IllegalArgumentException("Le nom doit contenir au moins 2 caractères.");
            if (email.isEmpty()) throw new IllegalArgumentException("L'email est obligatoire.");
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                throw new IllegalArgumentException("Format d'email invalide.");
            if (password.isEmpty()) throw new IllegalArgumentException("Le mot de passe est obligatoire.");
            if (password.length() < 6) throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères.");
            if (cboRole.getValue() == null) throw new IllegalArgumentException("Veuillez sélectionner un rôle.");

            // Validate and create user object
            User user = new User();
            
            if (isEditMode && !txtId.getText().isEmpty()) {
                user.setId(Integer.parseInt(txtId.getText()));
            }
            
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(cboRole.getValue());

            if (isEditMode) {
                // Update existing user
                boolean updated = userService.updateUser(user);
                if (updated) {
                    loadUsers();
                    clearForm();
                    AlertUtils.showSuccess("Utilisateur modifié avec succès !");
                } else {
                    AlertUtils.showError("Échec de la Modification", "Impossible de modifier l'utilisateur.");
                }
            } else {
                // Create new user
                User created = userService.createUser(user);
                if (created != null) {
                    loadUsers();
                    clearForm();
                    AlertUtils.showSuccess("Utilisateur créé avec succès !");
                } else {
                    AlertUtils.showError("Échec de la Création", "Impossible de créer l'utilisateur.");
                }
            }

        } catch (IllegalArgumentException e) {
            lblFormMessage.setText(e.getMessage());
            AlertUtils.showValidationError(e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Une erreur inattendue s'est produite : " + e.getMessage());
        }
    }

    /**
     * Cancels the form and clears input.
     */
    @FXML
    public void cancelForm() {
        clearForm();
    }

    /**
     * Clears all form fields.
     */
    private void clearForm() {
        txtId.clear();
        txtName.clear();
        txtEmail.clear();
        txtPassword.clear();
        cboRole.setValue(null);
        lblFormMessage.setText("");
        formTitle.setText("Ajouter un Utilisateur");
        isEditMode = false;
    }
}
