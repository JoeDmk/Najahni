package com.najahni.controllers;

import com.najahni.models.Project;
import com.najahni.models.ProjectStatus;
import com.najahni.models.User;
import com.najahni.services.ProjectService;
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
import javafx.util.StringConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Project management view.
 * Handles CRUD operations for projects with smooth animations.
 */
public class ProjectController {

    // Table components
    @FXML
    private TableView<Project> projectsTable;

    @FXML
    private TableColumn<Project, Integer> colId;

    @FXML
    private TableColumn<Project, String> colTitle;

    @FXML
    private TableColumn<Project, String> colSector;

    @FXML
    private TableColumn<Project, String> colStatus;

    @FXML
    private TableColumn<Project, String> colEntrepreneur;

    @FXML
    private TableColumn<Project, Void> colActions;

    // Search/Filter components
    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cboStatusFilter;

    @FXML
    private ComboBox<String> cboSectorFilter;

    // Form components
    @FXML
    private VBox formContainer;

    @FXML
    private Label formTitle;

    @FXML
    private TextField txtId;

    @FXML
    private TextField txtTitle;

    @FXML
    private ComboBox<String> cboSector;

    @FXML
    private ComboBox<ProjectStatus> cboStatus;

    @FXML
    private ComboBox<User> cboEntrepreneur;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Label lblFormMessage;

    private final ProjectService projectService;
    private final UserService userService;
    private ObservableList<Project> projectsList;
    private boolean isEditMode = false;

    public ProjectController() {
        this.projectService = new ProjectService();
        this.userService = new UserService();
    }

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadProjects();
        clearForm();
        
        // Animate components on load
        AnimationUtils.playFadeScaleIn(projectsTable, 300, 100);
        AnimationUtils.playFadeScaleIn(formContainer, 300, 200);
    }

    /**
     * Sets up table columns including action buttons.
     */
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colSector.setCellValueFactory(new PropertyValueFactory<>("sector"));
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        colEntrepreneur.setCellValueFactory(new PropertyValueFactory<>("entrepreneurName"));

        // Apply text wrapping to prevent truncation
        colTitle.setCellFactory(new WrappedTextCellFactory<>());
        colSector.setCellFactory(new WrappedTextCellFactory<>());
        colStatus.setCellFactory(new WrappedTextCellFactory<>());
        colEntrepreneur.setCellFactory(new WrappedTextCellFactory<>());

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
                    Project project = getTableView().getItems().get(getIndex());
                    editProject(project);
                });

                deleteBtn.setOnAction(event -> {
                    Project project = getTableView().getItems().get(getIndex());
                    deleteProject(project);
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
        // Status filter combo box
        List<String> statusOptions = Arrays.stream(ProjectStatus.values())
            .map(ProjectStatus::getDisplayName)
            .collect(Collectors.toList());
        statusOptions.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statusOptions));
        cboStatusFilter.setValue("Tous");

        // Sector filter combo box
        List<String> sectors = projectService.getAllSectors();
        sectors.add(0, "Tous");
        cboSectorFilter.setItems(FXCollections.observableArrayList(sectors));
        cboSectorFilter.setValue("Tous");

        // Status selection for form
        cboStatus.setItems(FXCollections.observableArrayList(ProjectStatus.values()));

        // Sector selection for form (editable combo box with predefined options)
        List<String> sectorOptions = Arrays.asList(
            "Technologie", "Santé", "Finance", "Éducation", 
            "Énergie", "Agriculture", "Industrie", "Commerce", 
            "Immobilier", "Transport", "Divertissement", "Autre"
        );
        cboSector.setItems(FXCollections.observableArrayList(sectorOptions));

        // Entrepreneur selection for form
        loadEntrepreneurs();
    }

    /**
     * Loads entrepreneurs into the combo box.
     */
    private void loadEntrepreneurs() {
        List<User> entrepreneurs = userService.getAllEntrepreneurs();
        cboEntrepreneur.setItems(FXCollections.observableArrayList(entrepreneurs));
        
        // Set custom string converter to display user names
        cboEntrepreneur.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getName() + " (" + user.getEmail() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Loads all projects into the table.
     */
    private void loadProjects() {
        List<Project> projects = projectService.findAll();
        projectsList = FXCollections.observableArrayList(projects);
        projectsTable.setItems(projectsList);
    }

    /**
     * Refreshes the table data.
     */
    @FXML
    public void refreshTable() {
        loadProjects();
        loadEntrepreneurs();
        clearForm();
        AlertUtils.showSuccess("Données actualisées avec succès !");
    }

    /**
     * Searches projects based on search criteria.
     */
    @FXML
    public void searchProjects() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String statusFilter = cboStatusFilter.getValue();
        String sectorFilter = cboSectorFilter.getValue();

        List<Project> filtered = projectService.findAll().stream()
            .filter(project -> {
                boolean matchesSearch = searchText.isEmpty() ||
                    project.getTitle().toLowerCase().contains(searchText);

                boolean matchesStatus = statusFilter == null || statusFilter.equals("Tous") ||
                    project.getStatus().getDisplayName().equals(statusFilter);

                boolean matchesSector = sectorFilter == null || sectorFilter.equals("Tous") ||
                    project.getSector().equals(sectorFilter);

                return matchesSearch && matchesStatus && matchesSector;
            })
            .collect(Collectors.toList());

        projectsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Clears search filters.
     */
    @FXML
    public void clearSearch() {
        txtSearch.clear();
        cboStatusFilter.setValue("Tous");
        cboSectorFilter.setValue("Tous");
        loadProjects();
    }

    /**
     * Shows the add project form.
     */
    @FXML
    public void showAddForm() {
        clearForm();
        formTitle.setText("Ajouter un Projet");
        isEditMode = false;
        cboStatus.setValue(ProjectStatus.DRAFT);
    }

    /**
     * Populates form for editing a project.
     */
    private void editProject(Project project) {
        isEditMode = true;
        formTitle.setText("Modifier le Projet");
        
        txtId.setText(String.valueOf(project.getId()));
        txtTitle.setText(project.getTitle());
        cboSector.setValue(project.getSector());
        cboStatus.setValue(project.getStatus());
        txtDescription.setText(project.getDescription());
        
        // Find and select the entrepreneur
        User entrepreneur = userService.findById(project.getEntrepreneurId()).orElse(null);
        cboEntrepreneur.setValue(entrepreneur);
        
        lblFormMessage.setText("");
    }

    /**
     * Deletes a project after confirmation.
     */
    private void deleteProject(Project project) {
        if (AlertUtils.confirmDelete(project.getTitle())) {
            boolean deleted = projectService.deleteProject(project.getId());
            if (deleted) {
                loadProjects();
                clearForm();
                AlertUtils.showSuccess("Projet supprimé avec succès !");
            } else {
                AlertUtils.showError("Échec de Suppression", "Impossible de supprimer le projet. Il peut avoir des investissements associés.");
            }
        }
    }

    /**
     * Saves the project (create or update).
     */
    @FXML
    public void saveProject() {
        try {
            // Validate entrepreneur selection
            if (cboEntrepreneur.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un entrepreneur");
            }

            // Validate and create project object
            Project project = new Project();
            
            if (isEditMode && !txtId.getText().isEmpty()) {
                project.setId(Integer.parseInt(txtId.getText()));
            }
            
            project.setTitle(txtTitle.getText().trim());
            
            // Get sector from editable combo box
            String sector = cboSector.getValue();
            if (sector == null || sector.trim().isEmpty()) {
                sector = cboSector.getEditor().getText();
            }
            project.setSector(sector != null ? sector.trim() : "");
            
            project.setStatus(cboStatus.getValue());
            project.setDescription(txtDescription.getText());
            project.setEntrepreneurId(cboEntrepreneur.getValue().getId());

            if (isEditMode) {
                // Update existing project
                boolean updated = projectService.updateProject(project);
                if (updated) {
                    loadProjects();
                    clearForm();
                    AlertUtils.showSuccess("Projet modifié avec succès !");
                } else {
                    AlertUtils.showError("Échec de Modification", "Impossible de modifier le projet.");
                }
            } else {
                // Create new project
                Project created = projectService.createProject(project);
                if (created != null) {
                    loadProjects();
                    clearForm();
                    AlertUtils.showSuccess("Projet créé avec succès !");
                } else {
                    AlertUtils.showError("Échec de Création", "Impossible de créer le projet.");
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
        txtTitle.clear();
        cboSector.setValue(null);
        cboSector.getEditor().clear();
        cboStatus.setValue(null);
        cboEntrepreneur.setValue(null);
        txtDescription.clear();
        lblFormMessage.setText("");
        formTitle.setText("Ajouter un Projet");
        isEditMode = false;
    }
}
