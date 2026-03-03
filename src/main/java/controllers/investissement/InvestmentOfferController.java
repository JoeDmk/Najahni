package controllers.investissement;

import models.investissement.*;
import models.User;
import util.Type;
import services.investissement.InvestmentOfferService;
import services.investissement.InvestmentOpportunityService;
import services.investissement.PaymentService;
import services.investissement.CurrencyService;
import services.UserService;
import util.AlertUtils;
import util.AnimationUtils;
import util.WrappedTextCellFactory;
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
 * Contrôleur pour la gestion des Offres d'Investissement.
 * 
 * Ce contrôleur gère UNIQUEMENT la logique UI :
 * - Affichage des offres dans le TableView
 * - Gestion du formulaire (ajout / modification)
 * - Déclenchement des actions CRUD via le Service
 * - Affichage des messages d'erreur/succès
 * 
 * AUCUNE logique métier ici → tout est délégué à InvestmentOfferService.
 * 
 * Architecture : Controller → Service → DAO → Database
 */
public class InvestmentOfferController {

    // ─── Cartes de résumé ────────────────────────────────────
    @FXML private Label lblPendingCount;
    @FXML private Label lblAcceptedCount;
    @FXML private Label lblRejectedCount;
    @FXML private Label lblTotalOffers;

    // ─── Table ───────────────────────────────────────────────
    @FXML private TableView<InvestmentOffer> offersTable;
    @FXML private TableColumn<InvestmentOffer, Integer> colId;
    @FXML private TableColumn<InvestmentOffer, String> colProposedAmount;
    @FXML private TableColumn<InvestmentOffer, String> colStatus;
    @FXML private TableColumn<InvestmentOffer, String> colInvestor;
    @FXML private TableColumn<InvestmentOffer, String> colOpportunity;
    @FXML private TableColumn<InvestmentOffer, Void> colActions;

    // ─── Filtres ─────────────────────────────────────────────
    @FXML private ComboBox<String> cboStatusFilter;
    @FXML private ComboBox<InvestmentOpportunity> cboOpportunityFilter;

    // ─── Formulaire ──────────────────────────────────────────
    @FXML private VBox formContainer;
    @FXML private Label formTitle;
    @FXML private TextField txtId;
    @FXML private ComboBox<User> cboInvestor;
    @FXML private ComboBox<InvestmentOpportunity> cboOpportunity;
    @FXML private TextField txtProposedAmount;
    @FXML private ComboBox<OfferStatus> cboStatus;
    @FXML private Label lblFormMessage;

    // ─── Services ────────────────────────────────────────────
    private final InvestmentOfferService offerService;
    private final InvestmentOpportunityService opportunityService;
    private final UserService userService;
    private final CurrencyService currencyService;
    private ObservableList<InvestmentOffer> offersList;
    private boolean isEditMode = false;

    public InvestmentOfferController() {
        this.offerService = new InvestmentOfferService();
        this.opportunityService = new InvestmentOpportunityService();
        this.userService = UserService.getInstance();
        this.currencyService = new CurrencyService();
    }

    /** Retourne la devise préférée de l'utilisateur connecté. */
    private String getUserCurrency() {
        try {
            var user = services.SessionService.getInstance().getCurrentUser();
            return user != null ? user.getPreferredCurrency() : "EUR";
        } catch (Exception e) { return "EUR"; }
    }

    // ─── INITIALISATION ──────────────────────────────────────

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadOffers();
        loadSummary();
        clearForm();

        AnimationUtils.playFadeScaleIn(offersTable, 300, 150);
        AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
    }

    // ─── CONFIGURATION TABLE ─────────────────────────────────

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colProposedAmount.setCellValueFactory(cellData -> {
            String currency = getUserCurrency();
            return new SimpleStringProperty(cellData.getValue().getFormattedAmount(currency, currencyService));
        });

        colStatus.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));

        colInvestor.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getInvestorName() != null
                ? cellData.getValue().getInvestorName() : "Investisseur #" + cellData.getValue().getInvestorId()));

        colOpportunity.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getOpportunityDescription() != null
                ? cellData.getValue().getOpportunityDescription()
                : "Opportunité #" + cellData.getValue().getOpportunityId()));

        // Text wrapping
        colProposedAmount.setCellFactory(new WrappedTextCellFactory<>());
        colStatus.setCellFactory(new WrappedTextCellFactory<>());
        colInvestor.setCellFactory(new WrappedTextCellFactory<>());
        colOpportunity.setCellFactory(new WrappedTextCellFactory<>());

        // Colonne d'actions
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button acceptBtn = new Button("✓");
            private final Button rejectBtn = new Button("✗");
            private final HBox pane = new HBox(5, editBtn, acceptBtn, rejectBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-warning");
                editBtn.setStyle("-fx-padding: 5 8;");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-padding: 5 8;");
                acceptBtn.getStyleClass().add("btn-success");
                acceptBtn.setStyle("-fx-padding: 5 8;");
                acceptBtn.setTooltip(new Tooltip("Accepter l'offre"));
                rejectBtn.getStyleClass().add("btn-danger");
                rejectBtn.setStyle("-fx-padding: 5 8;");
                rejectBtn.setTooltip(new Tooltip("Rejeter l'offre"));

                editBtn.setOnAction(event -> {
                    InvestmentOffer offer = getTableView().getItems().get(getIndex());
                    editOffer(offer);
                });
                deleteBtn.setOnAction(event -> {
                    InvestmentOffer offer = getTableView().getItems().get(getIndex());
                    deleteOffer(offer);
                });
                acceptBtn.setOnAction(event -> {
                    InvestmentOffer offer = getTableView().getItems().get(getIndex());
                    acceptOffer(offer);
                });
                rejectBtn.setOnAction(event -> {
                    InvestmentOffer offer = getTableView().getItems().get(getIndex());
                    rejectOffer(offer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    InvestmentOffer offer = getTableView().getItems().get(getIndex());
                    boolean isPending = offer.getStatus() == OfferStatus.PENDING;
                    acceptBtn.setVisible(isPending);
                    acceptBtn.setManaged(isPending);
                    rejectBtn.setVisible(isPending);
                    rejectBtn.setManaged(isPending);
                    setGraphic(pane);
                }
            }
        });
    }

    // ─── CONFIGURATION COMBOBOX ──────────────────────────────

    private void setupComboBoxes() {
        // Filtre par statut
        List<String> statusOptions = Arrays.stream(OfferStatus.values())
            .map(OfferStatus::getDisplayName)
            .collect(Collectors.toList());
        statusOptions.add(0, "Tous");
        cboStatusFilter.setItems(FXCollections.observableArrayList(statusOptions));
        cboStatusFilter.setValue("Tous");

        // Filtre par opportunité
        loadOpportunityFilters();

        // Statut dans le formulaire
        cboStatus.setItems(FXCollections.observableArrayList(OfferStatus.values()));

        // Investisseurs dans le formulaire (seulement INVESTOR)
        loadInvestors();

        // Opportunités dans le formulaire
        loadOpportunities();
    }

    private void loadOpportunityFilters() {
        List<InvestmentOpportunity> opportunities = opportunityService.findAll();
        InvestmentOpportunity allOption = new InvestmentOpportunity();
        allOption.setId(0);
        allOption.setDescription("Toutes les Opportunités");
        opportunities.add(0, allOption);

        cboOpportunityFilter.setItems(FXCollections.observableArrayList(opportunities));
        cboOpportunityFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(InvestmentOpportunity o) {
                if (o == null) return "";
                if (o.getId() == 0) return o.getDescription();
                return "#" + o.getId() + " - " + o.getFormattedAmount(getUserCurrency(), currencyService)
                    + (o.getProjectTitle() != null ? " [" + o.getProjectTitle() + "]" : "");
            }
            @Override public InvestmentOpportunity fromString(String s) { return null; }
        });
        cboOpportunityFilter.setValue(allOption);
    }

    private void loadInvestors() {
        // Le rôle INVESTOR dans Java correspond à INVESTISSEUR dans la DB
        List<User> investors = userService.getUsersByRole(Type.INVESTISSEUR);
        cboInvestor.setItems(FXCollections.observableArrayList(investors));
        cboInvestor.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u == null ? "" : u.getFirstname() + " (" + u.getEmail() + ")"; }
            @Override public User fromString(String s) { return null; }
        });
    }

    private void loadOpportunities() {
        // Seulement les opportunités OPEN pour le formulaire de création
        List<InvestmentOpportunity> openOpportunities = opportunityService.findByStatus(OpportunityStatus.OPEN);
        cboOpportunity.setItems(FXCollections.observableArrayList(openOpportunities));
        cboOpportunity.setConverter(new StringConverter<>() {
            @Override
            public String toString(InvestmentOpportunity o) {
                if (o == null) return "";
                return "#" + o.getId() + " - " + o.getFormattedAmount(getUserCurrency(), currencyService)
                    + (o.getProjectTitle() != null ? " [" + o.getProjectTitle() + "]" : "");
            }
            @Override public InvestmentOpportunity fromString(String s) { return null; }
        });
    }

    // ─── CHARGEMENT DES DONNÉES ──────────────────────────────

    private void loadSummary() {
        int pending = offerService.countByStatus(OfferStatus.PENDING);
        int accepted = offerService.countByStatus(OfferStatus.ACCEPTED);
        int rejected = offerService.countByStatus(OfferStatus.REJECTED);
        int total = pending + accepted + rejected;

        lblPendingCount.setText(String.valueOf(pending));
        lblAcceptedCount.setText(String.valueOf(accepted));
        lblRejectedCount.setText(String.valueOf(rejected));
        lblTotalOffers.setText(String.valueOf(total));
    }

    private void loadOffers() {
        List<InvestmentOffer> list = offerService.findAll();
        offersList = FXCollections.observableArrayList(list);
        offersTable.setItems(offersList);
    }

    // ─── ACTIONS CRUD ────────────────────────────────────────

    @FXML
    public void refreshTable() {
        loadOffers();
        loadSummary();
        loadInvestors();
        loadOpportunities();
        loadOpportunityFilters();
        clearForm();
        AlertUtils.showSuccess("Données actualisées avec succès !");
    }

    @FXML
    public void filterOffers() {
        String statusFilter = cboStatusFilter.getValue();
        InvestmentOpportunity oppFilter = cboOpportunityFilter.getValue();

        List<InvestmentOffer> filtered = offerService.findAll().stream()
            .filter(offer -> {
                boolean matchesStatus = statusFilter == null || statusFilter.equals("Tous")
                    || offer.getStatus().getDisplayName().equals(statusFilter);
                boolean matchesOpp = oppFilter == null || oppFilter.getId() == 0
                    || offer.getOpportunityId() == oppFilter.getId();
                return matchesStatus && matchesOpp;
            })
            .collect(Collectors.toList());

        offersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void clearFilter() {
        cboStatusFilter.setValue("Tous");
        if (!cboOpportunityFilter.getItems().isEmpty()) {
            cboOpportunityFilter.setValue(cboOpportunityFilter.getItems().get(0));
        }
        loadOffers();
    }

    @FXML
    public void showAddForm() {
        clearForm();
        formTitle.setText("Nouvelle Offre d'Investissement");
        isEditMode = false;
        cboStatus.setValue(OfferStatus.PENDING);
    }

    private void editOffer(InvestmentOffer offer) {
        isEditMode = true;
        formTitle.setText("Modifier l'Offre");

        txtId.setText(String.valueOf(offer.getId()));
        txtProposedAmount.setText(offer.getProposedAmount().toPlainString());
        cboStatus.setValue(offer.getStatus());

        // Sélectionner l'investisseur
        for (User u : cboInvestor.getItems()) {
            if (u.getId() == offer.getInvestorId()) {
                cboInvestor.setValue(u);
                break;
            }
        }

        // Charger toutes les opportunités pour l'édition (pas seulement OPEN)
        List<InvestmentOpportunity> allOpps = opportunityService.findAll();
        cboOpportunity.setItems(FXCollections.observableArrayList(allOpps));
        for (InvestmentOpportunity o : cboOpportunity.getItems()) {
            if (o.getId() == offer.getOpportunityId()) {
                cboOpportunity.setValue(o);
                break;
            }
        }

        lblFormMessage.setText("");
    }

    private void acceptOffer(InvestmentOffer offer) {
        String confirmMsg = "Accepter cette offre de " + offer.getFormattedAmount(getUserCurrency(), currencyService) + " ?";
        if (PaymentService.isConfigured()) {
            confirmMsg += "\n\n💳 Un paiement Stripe (mode test) sera effectué.";
        }

        if (AlertUtils.showConfirmation("Accepter l'Offre", confirmMsg)) {
            // Si Stripe est configuré, effectuer le paiement avant d'accepter
            if (PaymentService.isConfigured()) {
                processPaymentAndAccept(offer);
            } else {
                // Acceptation sans paiement (Stripe non configuré)
                finalizeAcceptOffer(offer);
            }
        }
    }

    private void processPaymentAndAccept(InvestmentOffer offer) {
        PaymentService paymentService = new PaymentService();
        long amountCents = offer.getProposedAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String description = "Offre #" + offer.getId() + " — Investissement NAJAHNI";

        paymentService.createPaymentIntent(amountCents, getUserCurrency().toLowerCase(), description)
            .thenAccept(result -> {
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        finalizeAcceptOffer(offer);
                        AlertUtils.showInfo("💳 Paiement Stripe",
                            "✅ Paiement réussi !\n\n"
                            + "ID : " + result.getPaymentIntentId() + "\n"
                            + "Statut : " + result.getStatus() + "\n"
                            + "Montant : " + offer.getFormattedAmount(getUserCurrency(), currencyService));
                    } else {
                        AlertUtils.showError("💳 Échec du Paiement",
                            result.getErrorMessage() + "\n\nL'offre n'a pas été acceptée.");
                    }
                });
            });
    }

    private void finalizeAcceptOffer(InvestmentOffer offer) {
        boolean accepted = offerService.acceptOffer(offer.getId());
        if (accepted) {
            loadOffers();
            loadSummary();
            AlertUtils.showSuccess("Offre acceptée avec succès !");
        } else {
            AlertUtils.showError("Échec", "Impossible d'accepter l'offre.");
        }
    }

    private void rejectOffer(InvestmentOffer offer) {
        if (AlertUtils.showConfirmation("Rejeter l'Offre",
                "Rejeter cette offre de " + offer.getFormattedAmount(getUserCurrency(), currencyService) + " ?")) {
            boolean rejected = offerService.rejectOffer(offer.getId());
            if (rejected) {
                loadOffers();
                loadSummary();
                AlertUtils.showSuccess("Offre rejetée !");
            } else {
                AlertUtils.showError("Échec", "Impossible de rejeter l'offre.");
            }
        }
    }

    private void deleteOffer(InvestmentOffer offer) {
        if (AlertUtils.showConfirmation("Supprimer l'Offre",
                "Êtes-vous sûr de vouloir supprimer cette offre ?\n\n"
                + "Montant : " + offer.getFormattedAmount(getUserCurrency(), currencyService))) {
            boolean deleted = offerService.deleteOffer(offer.getId());
            if (deleted) {
                loadOffers();
                loadSummary();
                clearForm();
                AlertUtils.showSuccess("Offre supprimée avec succès !");
            } else {
                AlertUtils.showError("Échec de Suppression", "Impossible de supprimer l'offre.");
            }
        }
    }

    @FXML
    public void saveOffer() {
        try {
            // ── Validation UI : investisseur obligatoire ──
            if (cboInvestor.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un investisseur.");
            }

            // ── Validation UI : opportunité obligatoire ──
            if (cboOpportunity.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner une opportunité.");
            }

            // ── Validation UI : montant ──
            String amountText = txtProposedAmount.getText().trim();
            if (amountText.isEmpty()) {
                throw new IllegalArgumentException("Le montant proposé est obligatoire.");
            }
            BigDecimal proposedAmount;
            try {
                proposedAmount = new BigDecimal(amountText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format de montant invalide. Entrez un nombre valide.");
            }
            if (proposedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Le montant proposé doit être supérieur à zéro.");
            }
            if (proposedAmount.compareTo(new BigDecimal("10000000")) > 0) {
                throw new IllegalArgumentException("Le montant proposé ne peut pas dépasser " + CurrencyService.format(10_000_000, getUserCurrency()) + ".");
            }

            // ── Validation UI : statut ──
            if (cboStatus.getValue() == null) {
                throw new IllegalArgumentException("Veuillez sélectionner un statut.");
            }

            // ── Construction de l'objet ──
            InvestmentOffer offer = new InvestmentOffer();

            if (isEditMode && !txtId.getText().isEmpty()) {
                offer.setId(Integer.parseInt(txtId.getText()));
            }

            offer.setProposedAmount(proposedAmount);
            offer.setStatus(cboStatus.getValue());
            offer.setInvestorId(cboInvestor.getValue().getId());
            offer.setOpportunityId(cboOpportunity.getValue().getId());

            // ── Délégation au service (logique métier) ──
            if (isEditMode) {
                boolean updated = offerService.updateOffer(offer);
                if (updated) {
                    loadOffers();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Offre modifiée avec succès !");
                } else {
                    AlertUtils.showError("Échec", "Impossible de modifier l'offre.");
                }
            } else {
                InvestmentOffer created = offerService.createOffer(offer);
                if (created != null) {
                    loadOffers();
                    loadSummary();
                    clearForm();
                    AlertUtils.showSuccess("Offre créée avec succès !");
                } else {
                    AlertUtils.showError("Échec", "Impossible de créer l'offre.");
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
        txtProposedAmount.clear();
        cboInvestor.setValue(null);
        cboOpportunity.setValue(null);
        cboStatus.setValue(null);
        lblFormMessage.setText("");
        formTitle.setText("Nouvelle Offre d'Investissement");
        isEditMode = false;

        // Recharger uniquement les OPEN pour le formulaire
        loadOpportunities();
    }
}
