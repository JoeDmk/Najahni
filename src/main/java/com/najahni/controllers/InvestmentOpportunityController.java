package com.najahni.controllers;

import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Project;
import com.najahni.services.InvestmentOpportunityService;
import com.najahni.services.ProjectService;
import com.najahni.services.RiskCalculator;
import com.najahni.services.RiskResult;
import com.najahni.services.RiskService;
import com.najahni.services.ml.RiskAIService;
import com.najahni.services.ml.RiskPrediction;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des Opportunités d'Investissement.
 * 
 * Ce contrôleur gère UNIQUEMENT la logique UI :
 * - Affichage des données dans le TableView
 * - Gestion du formulaire (ajout / modification)
 * - Déclenchement des actions CRUD via le Service
 * - Affichage des messages d'erreur/succès
 * 
 * AUCUNE logique métier ici → tout est délégué à InvestmentOpportunityService.
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class InvestmentOpportunityController {

    // ─── Cartes de résumé ────────────────────────────────────
    @FXML private Label lblOpenCount;
    @FXML private Label lblClosedCount;
    @FXML private Label lblFundedCount;
    @FXML private Label lblTotalAmount;

    // ─── Table ───────────────────────────────────────────────
    @FXML private TableView<InvestmentOpportunity> opportunitiesTable;
    @FXML private TableColumn<InvestmentOpportunity, Integer> colId;
    @FXML private TableColumn<InvestmentOpportunity, String> colTargetAmount;
    @FXML private TableColumn<InvestmentOpportunity, String> colDescription;
    @FXML private TableColumn<InvestmentOpportunity, String> colDeadline;
    @FXML private TableColumn<InvestmentOpportunity, String> colStatus;
    @FXML private TableColumn<InvestmentOpportunity, String> colProject;
    @FXML private TableColumn<InvestmentOpportunity, String> colRiskScore;
    @FXML private TableColumn<InvestmentOpportunity, String> colRiskLabel;
    @FXML private TableColumn<InvestmentOpportunity, Void> colActions;

    // ─── Filtres ─────────────────────────────────────────────
    @FXML private ComboBox<String> cboStatusFilter;
    @FXML private ComboBox<Project> cboProjectFilter;

    // ─── Formulaire ──────────────────────────────────────────
    @FXML private VBox formContainer;
    @FXML private Label formTitle;
    @FXML private TextField txtId;
    @FXML private ComboBox<Project> cboProject;
    @FXML private TextField txtTargetAmount;
    @FXML private TextField txtDescription;
    @FXML private DatePicker dpDeadline;
    @FXML private ComboBox<OpportunityStatus> cboStatus;
    @FXML private Label lblFormMessage;

    // ─── Services ────────────────────────────────────────────
    private final InvestmentOpportunityService opportunityService;
    private final ProjectService projectService;
    private final RiskService riskService;
    private final RiskAIService riskAIService;
    private ObservableList<InvestmentOpportunity> opportunitiesList;
    private boolean isEditMode = false;

    public InvestmentOpportunityController() {
        this.opportunityService = new InvestmentOpportunityService();
        this.projectService = new ProjectService();
        this.riskService = new RiskService();
        this.riskAIService = new RiskAIService();
    }

    // ─── INITIALISATION ──────────────────────────────────────

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadOpportunities();
        loadSummary();
        clearForm();

        AnimationUtils.playFadeScaleIn(opportunitiesTable, 300, 150);
        AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
    }

    // ─── CONFIGURATION TABLE ─────────────────────────────────

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colTargetAmount.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));

        colDescription.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDescription() != null
                ? cellData.getValue().getDescription() : "—"));

        colDeadline.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDeadline() != null
                ? cellData.getValue().getDeadline().toString() : "—"));

        colStatus.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));

        // ── Colonne Risk Score IA ──
        colRiskScore.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedRiskScore()));

        // Style coloré pour le risque
        colRiskScore.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    if (opp.getRiskScore() != null) {
                        setStyle(RiskCalculator.getRiskStyle(opp.getRiskScore().intValue()));
                    } else {
                        setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                    }
                }
            }
        });

        // ── Colonne Risk Label ML ──
        if (colRiskLabel != null) {
            colRiskLabel.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedRiskLabel()));
            colRiskLabel.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                        String label = opp.getRiskLabel();
                        if (label != null) {
                            String bgColor = switch (label.toLowerCase()) {
                                case "faible" -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                                case "moyen" -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
                                case "eleve" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                                default -> "-fx-text-fill: #999;";
                            };
                            setStyle(bgColor);
                        } else {
                            setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                        }
                    }
                }
            });
        }

        // Application du text wrapping
        colTargetAmount.setCellFactory(new WrappedTextCellFactory<>());
        colDescription.setCellFactory(new WrappedTextCellFactory<>());
        colStatus.setCellFactory(new WrappedTextCellFactory<>());
        colProject.setCellFactory(new WrappedTextCellFactory<>());

        // Colonne d'actions
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button closeBtn = new Button("🔒");
            private final Button riskBtn = new Button("🎯");
            private final Button mlBtn = new Button("🤖");
            private final HBox pane = new HBox(5, editBtn, riskBtn, mlBtn, closeBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-warning");
                editBtn.setStyle("-fx-padding: 5 8;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-padding: 5 8;");
                closeBtn.getStyleClass().add("btn-secondary");
                closeBtn.setStyle("-fx-padding: 5 8;");
                closeBtn.setTooltip(new Tooltip("Fermer l'opportunité"));
                riskBtn.getStyleClass().add("btn-primary");
                riskBtn.setStyle("-fx-padding: 5 8;");
                riskBtn.setTooltip(new Tooltip("Calculer le Risk Score IA"));
                mlBtn.getStyleClass().add("btn-primary");
                mlBtn.setStyle("-fx-padding: 5 8; -fx-background-color: #8e44ad; -fx-text-fill: white;");
                mlBtn.setTooltip(new Tooltip("Prédire Risque IA (Machine Learning)"));

                editBtn.setOnAction(event -> {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    editOpportunity(opp);
                });
                deleteBtn.setOnAction(event -> {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    deleteOpportunity(opp);
                });
                closeBtn.setOnAction(event -> {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    closeOpportunity(opp);
                });
                riskBtn.setOnAction(event -> {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    calculateRisk(opp);
                });
                mlBtn.setOnAction(event -> {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    predictRiskML(opp);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    InvestmentOpportunity opp = getTableView().getItems().get(getIndex());
                    closeBtn.setVisible(opp.getStatus() == OpportunityStatus.OPEN);
                    closeBtn.setManaged(opp.getStatus() == OpportunityStatus.OPEN);
                    setGraphic(pane);
                }
            }
        });
    }

    // ─── CONFIGURATION COMBOBOX ──────────────────────────────

    private void setupComboBoxes() {
        // Filtre par statut
        List<String> statusOptions = Arrays.stream(OpportunityStatus.values())
            .map(OpportunityStatus::getDisplayName)
            .collect(Collectors.toList());
        statusOptions.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statusOptions));
        cboStatusFilter.setValue("Tous");

        // Filtre par projet
        loadProjectFilters();

        // Statut dans le formulaire
        cboStatus.setItems(FXCollections.observableArrayList(OpportunityStatus.values()));

        // Projet dans le formulaire
        loadProjects();
    }

    private void loadProjectFilters() {
        List<Project> projects = projectService.findAll();
        Project allOption = new Project();
        allOption.setId(0);
        allOption.setTitle("Tous les Projets");
        projects.add(0, allOption);

        cboProjectFilter.setItems(FXCollections.observableArrayList(projects));
        cboProjectFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Project p) { return p == null ? "" : p.getTitle(); }
            @Override public Project fromString(String s) { return null; }
        });
        cboProjectFilter.setValue(allOption);
    }

    private void loadProjects() {
        List<Project> projects = projectService.findAll();
        cboProject.setItems(FXCollections.observableArrayList(projects));
        cboProject.setConverter(new StringConverter<>() {
            @Override
            public String toString(Project p) {
                return p == null ? "" : p.getTitle() + " [" + p.getSector() + "]";
            }
            @Override public Project fromString(String s) { return null; }
        });
    }

    // ─── CHARGEMENT DES DONNÉES ──────────────────────────────

    private void loadSummary() {
        int open = opportunityService.countByStatus(OpportunityStatus.OPEN);
        int closed = opportunityService.countByStatus(OpportunityStatus.CLOSED);
        int funded = opportunityService.countByStatus(OpportunityStatus.FUNDED);
        BigDecimal totalAmount = opportunityService.getTotalTargetAmount();

        lblOpenCount.setText(String.valueOf(open));
        lblClosedCount.setText(String.valueOf(closed));
        lblFundedCount.setText(String.valueOf(funded));
        lblTotalAmount.setText(String.format("%,.2f €", totalAmount));
    }

    private void loadOpportunities() {
        List<InvestmentOpportunity> list = opportunityService.findAll();
        opportunitiesList = FXCollections.observableArrayList(list);
        opportunitiesTable.setItems(opportunitiesList);
    }

    // ─── ACTIONS CRUD ────────────────────────────────────────

    @FXML
    public void refreshTable() {
        loadOpportunities();
        loadSummary();
        loadProjects();
        loadProjectFilters();
        clearForm();
        AlertUtils.showSuccess("Données actualisées avec succès !");
    }

    @FXML
    public void filterOpportunities() {
        String statusFilter = cboStatusFilter.getValue();
        Project projectFilter = cboProjectFilter.getValue();

        List<InvestmentOpportunity> filtered = opportunityService.findAll().stream()
            .filter(opp -> {
                boolean matchesStatus = statusFilter == null || statusFilter.equals("Tous")
                    || opp.getStatus().getDisplayName().equals(statusFilter);
                boolean matchesProject = projectFilter == null || projectFilter.getId() == 0
                    || opp.getProjectId() == projectFilter.getId();
                return matchesStatus && matchesProject;
            })
            .collect(Collectors.toList());

        opportunitiesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void clearFilter() {
        cboStatusFilter.setValue("Tous");
        if (!cboProjectFilter.getItems().isEmpty()) {
            cboProjectFilter.setValue(cboProjectFilter.getItems().get(0));
        }
        loadOpportunities();
    }

    @FXML
    public void showAddForm() {
        clearForm();
        formTitle.setText("Ajouter une Opportunité");
        isEditMode = false;
        cboStatus.setValue(OpportunityStatus.OPEN);
    }

    private void editOpportunity(InvestmentOpportunity opp) {
        isEditMode = true;
        formTitle.setText("Modifier l'Opportunité");

        txtId.setText(String.valueOf(opp.getId()));
        txtTargetAmount.setText(opp.getTargetAmount().toPlainString());
        txtDescription.setText(opp.getDescription());
        dpDeadline.setValue(opp.getDeadline());
        cboStatus.setValue(opp.getStatus());

        Project project = projectService.findById(opp.getProjectId()).orElse(null);
        cboProject.setValue(project);

        lblFormMessage.setText("");
    }

    private void closeOpportunity(InvestmentOpportunity opp) {
        if (AlertUtils.showConfirmation("Fermer l'Opportunité",
                "Fermer cette opportunité de " + opp.getFormattedAmount() + " ?\nAucune nouvelle offre ne pourra être soumise.")) {
            boolean closed = opportunityService.closeOpportunity(opp.getId());
            if (closed) {
                loadOpportunities();
                loadSummary();
                AlertUtils.showSuccess("Opportunité fermée avec succès !");
            } else {
                AlertUtils.showError("Échec", "Impossible de fermer l'opportunité.");
            }
        }
    }

    // ─── RISK SCORING IA ─────────────────────────────────────

    /**
     * Calcule le Risk Score IA pour une opportunité.
     * Affiche un popup de confirmation avant de lancer le calcul.
     * Le calcul appelle une API externe (Open-Meteo) et applique la formule :
     * risk_score = (montant × 0.4) + (durée × 0.2) + (facteur API × 0.4)
     */
    private void calculateRisk(InvestmentOpportunity opp) {
        // ── Popup de confirmation ──
        String confirmMsg = "Calculer le Risk Score IA pour cette opportunité ?\n\n"
            + "📊 Montant : " + opp.getFormattedAmount() + "\n"
            + "📅 Deadline : " + (opp.getDeadline() != null ? opp.getDeadline().toString() : "Aucune") + "\n"
            + "🏷️ Projet : " + (opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet #" + opp.getProjectId()) + "\n\n"
            + "⚡ Cette action appelle une API externe (Open-Meteo)\n"
            + "   pour obtenir un facteur économique en temps réel.";

        if (!AlertUtils.showConfirmation("🎯 Calcul Risk Score IA", confirmMsg)) {
            return; // L'utilisateur a annulé
        }

        try {
            // ── Calcul via RiskService ──
            RiskResult result = riskService.calculateRisk(opp);

            // ── Sauvegarde en BDD ──
            boolean saved = opportunityService.updateRiskScore(opp.getId(), result.getScore());

            if (saved) {
                // Mettre à jour l'objet local
                opp.setRiskScore((double) result.getScore());
                opportunitiesTable.refresh();

                // ── Popup de résultat ──
                AlertUtils.showInfo("🎯 Risk Score IA — Résultat",
                    result.getEmoji() + " Score : " + result.getScore() + "/100\n"
                    + "📊 Niveau : " + result.getLevel() + "\n\n"
                    + "Formule : (montant × 0.4) + (durée × 0.2) + (facteur API × 0.4)\n\n"
                    + "Le score a été sauvegardé en base de données.");
            } else {
                AlertUtils.showError("Erreur", "Le score a été calculé ("
                    + result.getDisplay() + ") mais n'a pas pu être sauvegardé en BDD.");
            }

        } catch (IllegalArgumentException e) {
            AlertUtils.showValidationError("Données invalides : " + e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur Risk Score",
                "Erreur lors du calcul du Risk Score :\n" + e.getMessage()
                + "\n\nVérifiez votre connexion internet (API Open-Meteo).");
        }
    }

    // ─── PREDICTION ML (WEKA) ─────────────────────────────

    /**
     * Prédit le risque d'une opportunité via Machine Learning (Weka RandomForest).
     * Affiche un popup de confirmation, lance la prédiction, affiche le résultat
     * détaillé (label + probabilités) et sauvegarde le risk_label en BDD.
     */
    private void predictRiskML(InvestmentOpportunity opp) {
        // ── Récupérer le secteur du projet associé ──
        String projectSector = "tech"; // Défaut
        try {
            var project = projectService.findById(opp.getProjectId());
            if (project.isPresent() && project.get().getSector() != null) {
                projectSector = project.get().getSector();
            }
        } catch (Exception ignored) {}

        // ── Popup de confirmation ──
        String confirmMsg = "Prédire le risque ML pour cette opportunité ?\n\n"
            + "📊 Montant : " + opp.getFormattedAmount() + "\n"
            + "📅 Deadline : " + (opp.getDeadline() != null ? opp.getDeadline().toString() : "Aucune") + "\n"
            + "🏷️ Projet : " + (opp.getProjectTitle() != null ? opp.getProjectTitle() : "Projet #" + opp.getProjectId()) + "\n"
            + "🏭 Secteur : " + projectSector + "\n\n"
            + "🤖 Modèle : RandomForest (Weka) — 100 arbres\n"
            + "   Entraîné localement, aucune API externe.";

        if (!AlertUtils.showConfirmation("🤖 Prédiction Risque ML", confirmMsg)) {
            return;
        }

        try {
            // ── Initialisation du service ML ──
            riskAIService.initialize();

            // ── Prédiction ──
            RiskPrediction prediction = riskAIService.predictRisk(opp, projectSector);

            // ── Sauvegarde du label en BDD ──
            boolean saved = opportunityService.updateRiskLabel(opp.getId(), prediction.getLabel());

            if (saved) {
                opp.setRiskLabel(prediction.getLabel());
                opportunitiesTable.refresh();

                // ── Popup de résultat détaillé ──
                AlertUtils.showInfo("🤖 Prédiction ML — Résultat",
                    prediction.getDisplay() + "\n\n"
                    + prediction.getDetailedDisplay() + "\n\n"
                    + "Algorithme : RandomForest (100 arbres, seed=42)\n"
                    + "Le label a été sauvegardé en base de données.");
            } else {
                AlertUtils.showError("Erreur",
                    "Prédiction réussie (" + prediction.getDisplay() + ")\n"
                    + "mais échec de sauvegarde en BDD.");
            }

        } catch (Exception e) {
            AlertUtils.showError("Erreur Prédiction ML",
                "Erreur lors de la prédiction ML :\n" + e.getMessage()
                + "\n\nVérifiez que le dataset ARFF et le modèle sont accessibles.");
            e.printStackTrace();
        }
    }

    private void deleteOpportunity(InvestmentOpportunity opp) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer ?");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cet élément ?\n\n"
            + "Opportunité : " + opp.getFormattedAmount() + "\n"
            + "Toutes les offres liées seront aussi supprimées (CASCADE).");
        java.util.Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = opportunityService.deleteOpportunity(opp.getId());
            if (deleted) {
                loadOpportunities();
                loadSummary();
                clearForm();
                AlertUtils.showSuccess("Opportunité supprimée avec succès !");
            } else {
                AlertUtils.showError("Échec de Suppression", "Impossible de supprimer l'opportunité.");
            }
        }
    }

    @FXML
    public void saveOpportunity() {
        try {
            // ── Validation UI : projet obligatoire ──
            if (cboProject.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un projet.");
            }

            // ── Validation UI : montant ──
            String amountText = txtTargetAmount.getText().trim();
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Le montant cible est obligatoire.");
            }
            BigDecimal targetAmount;
            try {
                targetAmount = new BigDecimal(amountText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format de montant invalide. Entrez un nombre valide.");
            }
            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Le montant cible doit être supérieur à zéro.");
            }

            // ── Validation UI : statut ──
            if (cboStatus.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un statut.");
            }

            // ── Construction de l'objet ──
            InvestmentOpportunity opp = new InvestmentOpportunity();

            if (isEditMode && !txtId.getText().isEmpty()) {
                opp.setId(Integer.parseInt(txtId.getText()));
            }

            opp.setTargetAmount(targetAmount);
            opp.setDescription(txtDescription.getText().trim().isEmpty() ? null : txtDescription.getText().trim());
            opp.setDeadline(dpDeadline.getValue());
            opp.setStatus(cboStatus.getValue());
            opp.setProjectId(cboProject.getValue().getId());

            // ── Délégation au service (logique métier) ──
            if (isEditMode) {
                boolean updated = opportunityService.updateOpportunity(opp);
                if (updated) {
                    loadOpportunities();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Opportunité modifiée avec succès !");
                } else {
                    AlertUtils.showError("Échec", "Impossible de modifier l'opportunité.");
                }
            } else {
                InvestmentOpportunity created = opportunityService.createOpportunity(opp);
                if (created != null) {
                    loadOpportunities();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Opportunité créée avec succès !");
                } else {
                    AlertUtils.showError("Échec", "Impossible de créer l'opportunité.");
                }
            }

        } catch (IllegalArgumentException e) {
            lblFormMessage.setText(e.getMessage());
            AlertUtils.showValidationError(e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    public void cancelForm() {
        clearForm();
    }

    private void clearForm() {
        txtId.clear();
        txtTargetAmount.clear();
        txtDescription.clear();
        dpDeadline.setValue(null);
        cboProject.setValue(null);
        cboStatus.setValue(null);
        lblFormMessage.setText("");
        formTitle.setText("Ajouter une Opportunité");
        isEditMode = false;
    }
}
