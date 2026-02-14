package com.najahni.controllers;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.InvestmentStatus;
import com.najahni.models.Project;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.services.ProjectService;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Investment management view.
 * Handles CRUD operations for investment opportunities with smooth animations.
 */
public class InvestmentController {

    // Summary Labels
    @FXML
    private Label lblPendingCount;

    @FXML
    private Label lblAcceptedCount;

    @FXML
    private Label lblCompletedCount;

    @FXML
    private Label lblTotalAmount;

    // Table components
    @FXML
    private TableView<InvestmentOpportunity> investmentsTable;

    @FXML
    private TableColumn<InvestmentOpportunity, Integer> colId;

    @FXML
    private TableColumn<InvestmentOpportunity, String> colAmount;

    @FXML
    private TableColumn<InvestmentOpportunity, String> colStatus;

    @FXML
    private TableColumn<InvestmentOpportunity, String> colProject;

    @FXML
    private TableColumn<InvestmentOpportunity, Void> colActions;

    // Filter components
    @FXML
    private ComboBox<String> cboStatusFilter;

    @FXML
    private ComboBox<Project> cboProjectFilter;

    // Form components
    @FXML
    private VBox formContainer;

    @FXML
    private Label formTitle;

    @FXML
    private TextField txtId;

    @FXML
    private ComboBox<Project> cboProject;

    @FXML
    private TextField txtAmount;

    @FXML
    private ComboBox<InvestmentStatus> cboStatus;

    @FXML
    private Label lblFormMessage;

    private final InvestmentOpportunityService investmentService;
    private final ProjectService projectService;
    private ObservableList<InvestmentOpportunity> investmentsList;
    private boolean isEditMode = false;

    public InvestmentController() {
        this.investmentService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
    }

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadInvestments();
        loadSummary();
        clearForm();
        
        // Animate components on load
        AnimationUtils.playFadeScaleIn(investmentsTable, 300, 150);
        AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
    }

    /**
     * Sets up table columns including action buttons.
     */
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Format amount with currency
        colAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));

        // Apply text wrapping to prevent truncation
        colAmount.setCellFactory(new WrappedTextCellFactory<>());
        colStatus.setCellFactory(new WrappedTextCellFactory<>());
        colProject.setCellFactory(new WrappedTextCellFactory<>());

        // Setup action buttons column
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button acceptBtn = new Button("✓");
            private final HBox pane = new HBox(5, editBtn, acceptBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-warning");
                editBtn.setStyle("-fx-padding: 5 8;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-padding: 5 8;");
                acceptBtn.getStyleClass().add("btn-success");
                acceptBtn.setStyle("-fx-padding: 5 8;");
                acceptBtn.setTooltip(new Tooltip("Accepter l'Investissement"));

                editBtn.setOnAction(event -> {
                    InvestmentOpportunity investment = getTableView().getItems().get(getIndex());
                    editInvestment(investment);
                });

                deleteBtn.setOnAction(event -> {
                    InvestmentOpportunity investment = getTableView().getItems().get(getIndex());
                    deleteInvestment(investment);
                });

                acceptBtn.setOnAction(event -> {
                    InvestmentOpportunity investment = getTableView().getItems().get(getIndex());
                    acceptInvestment(investment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    InvestmentOpportunity investment = getTableView().getItems().get(getIndex());
                    // Only show accept button for pending investments
                    acceptBtn.setVisible(investment.getStatus() == InvestmentStatus.PENDING);
                    acceptBtn.setManaged(investment.getStatus() == InvestmentStatus.PENDING);
                    setGraphic(pane);
                }
            }
        });
    }

    /**
     * Sets up combo boxes with data.
     */
    private void setupComboBoxes() {
        // Status filter combo box
        List<String> statusOptions = Arrays.stream(InvestmentStatus.values())
            .map(InvestmentStatus::getDisplayName)
            .collect(Collectors.toList());
        statusOptions.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statusOptions));
        cboStatusFilter.setValue("Tous");

        // Project filter combo box
        loadProjectFilters();

        // Status selection for form
        cboStatus.setItems(FXCollections.observableArrayList(InvestmentStatus.values()));

        // Project selection for form
        loadProjects();
    }

    /**
     * Loads projects into the filter combo box.
     */
    private void loadProjectFilters() {
        List<Project> projects = projectService.findAll();
        Project allOption = new Project();
        allOption.setId(0);
        allOption.setTitle("Tous les Projets");
        projects.add(0, allOption);
        
        cboProjectFilter.setItems(FXCollections.observableArrayList(projects));
        cboProjectFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Project project) {
                return project == null ? "" : project.getTitle();
            }

            @Override
            public Project fromString(String string) {
                return null;
            }
        });
        cboProjectFilter.setValue(allOption);
    }

    /**
     * Loads projects into the form combo box.
     */
    private void loadProjects() {
        List<Project> projects = projectService.findAll();
        cboProject.setItems(FXCollections.observableArrayList(projects));
        
        cboProject.setConverter(new StringConverter<>() {
            @Override
            public String toString(Project project) {
                return project == null ? "" : project.getTitle() + " [" + project.getSector() + "]";
            }

            @Override
            public Project fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Loads summary statistics.
     */
    private void loadSummary() {
        int pending = investmentService.countByStatus(InvestmentStatus.PENDING);
        int accepted = investmentService.countByStatus(InvestmentStatus.ACCEPTED);
        int completed = investmentService.countByStatus(InvestmentStatus.COMPLETED);
        BigDecimal totalAmount = investmentService.getTotalInvestmentAmount();

        lblPendingCount.setText(String.valueOf(pending));
        lblAcceptedCount.setText(String.valueOf(accepted));
        lblCompletedCount.setText(String.valueOf(completed));
        lblTotalAmount.setText(String.format("%,.2f €", totalAmount));
    }

    /**
     * Loads all investments into the table.
     */
    private void loadInvestments() {
        List<InvestmentOpportunity> investments = investmentService.findAll();
        investmentsList = FXCollections.observableArrayList(investments);
        investmentsTable.setItems(investmentsList);
    }

    /**
     * Refreshes the table data.
     */
    @FXML
    public void refreshTable() {
        loadInvestments();
        loadSummary();
        loadProjects();
        loadProjectFilters();
        clearForm();
        AlertUtils.showSuccess("Données actualisées avec succès !");
    }

    /**
     * Filters investments based on selected criteria.
     */
    @FXML
    public void filterInvestments() {
        String statusFilter = cboStatusFilter.getValue();
        Project projectFilter = cboProjectFilter.getValue();

        List<InvestmentOpportunity> filtered = investmentService.findAll().stream()
            .filter(investment -> {
                boolean matchesStatus = statusFilter == null || statusFilter.equals("Tous") ||
                    investment.getStatus().getDisplayName().equals(statusFilter);

                boolean matchesProject = projectFilter == null || projectFilter.getId() == 0 ||
                    investment.getProjectId() == projectFilter.getId();

                return matchesStatus && matchesProject;
            })
            .collect(Collectors.toList());

        investmentsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * Clears filter selections.
     */
    @FXML
    public void clearFilter() {
        cboStatusFilter.setValue("Tous");
        if (cboProjectFilter.getItems().size() > 0) {
            cboProjectFilter.setValue(cboProjectFilter.getItems().get(0));
        }
        loadInvestments();
    }

    /**
     * Shows the add investment form.
     */
    @FXML
    public void showAddForm() {
        clearForm();
        formTitle.setText("Ajouter un Investissement");
        isEditMode = false;
        cboStatus.setValue(InvestmentStatus.PENDING);
    }

    /**
     * Populates form for editing an investment.
     */
    private void editInvestment(InvestmentOpportunity investment) {
        isEditMode = true;
        formTitle.setText("Modifier l'Investissement");
        
        txtId.setText(String.valueOf(investment.getId()));
        txtAmount.setText(investment.getAmount().toString());
        cboStatus.setValue(investment.getStatus());
        
        // Find and select the project
        Project project = projectService.findById(investment.getProjectId()).orElse(null);
        cboProject.setValue(project);
        
        lblFormMessage.setText("");
    }

    /**
     * Accepts an investment.
     */
    private void acceptInvestment(InvestmentOpportunity investment) {
        if (AlertUtils.showConfirmation("Accepter l'Investissement", 
                "Accepter cet investissement de " + investment.getFormattedAmount() + " ?")) {
            boolean accepted = investmentService.acceptInvestment(investment.getId());
            if (accepted) {
                loadInvestments();
                loadSummary();
                AlertUtils.showSuccess("Investissement accepté avec succès !");
            } else {
                AlertUtils.showError("Échec d'Acceptation", "Impossible d'accepter l'investissement.");
            }
        }
    }

    /**
     * Deletes an investment after confirmation.
     */
    private void deleteInvestment(InvestmentOpportunity investment) {
        if (AlertUtils.confirmDelete("Investissement de " + investment.getFormattedAmount())) {
            boolean deleted = investmentService.deleteInvestment(investment.getId());
            if (deleted) {
                loadInvestments();
                loadSummary();
                clearForm();
                AlertUtils.showSuccess("Investissement supprimé avec succès !");
            } else {
                AlertUtils.showError("Échec de Suppression", "Impossible de supprimer l'investissement.");
            }
        }
    }

    /**
     * Saves the investment (create or update).
     */
    @FXML
    public void saveInvestment() {
        try {
            // Validate project selection
            if (cboProject.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un projet");
            }

            // Validate and parse amount
            String amountText = txtAmount.getText().trim();
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Le montant est requis");
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format de montant invalide. Veuillez entrer un nombre valide.");
            }

            // Create investment object
            InvestmentOpportunity investment = new InvestmentOpportunity();
            
            if (isEditMode && !txtId.getText().isEmpty()) {
                investment.setId(Integer.parseInt(txtId.getText()));
            }
            
            investment.setAmount(amount);
            investment.setStatus(cboStatus.getValue());
            investment.setProjectId(cboProject.getValue().getId());

            if (isEditMode) {
                // Update existing investment
                boolean updated = investmentService.updateInvestment(investment);
                if (updated) {
                    loadInvestments();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Investissement modifié avec succès !");
                } else {
                    AlertUtils.showError("Échec de Modification", "Impossible de modifier l'investissement.");
                }
            } else {
                // Create new investment
                InvestmentOpportunity created = investmentService.createInvestment(investment);
                if (created != null) {
                    loadInvestments();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Investissement créé avec succès !");
                } else {
                    AlertUtils.showError("Échec de Création", "Impossible de créer l'investissement.");
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
        txtAmount.clear();
        cboProject.setValue(null);
        cboStatus.setValue(null);
        lblFormMessage.setText("");
        formTitle.setText("Ajouter un Investissement");
        isEditMode = false;
    }
}
