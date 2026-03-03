package controllers.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.EtatProgression;
import models.apprentissage.Progression;
import services.apprentissage.CoursService;
import services.apprentissage.ProgressionService;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller pour la gestion des Progressions (CRUD).
 * Gère les opérations CRUD pour les progressions avec animations fluides.
 */
public class ProgressionController {

 // Labels de résumé
 @FXML private Label lblTotalProgressions;
 @FXML private Label lblEnCours;
 @FXML private Label lblCompletes;
 @FXML private Label lblTotalXP;

 // Composants du tableau
 @FXML private TableView<Progression> progressionTable;
 @FXML private TableColumn<Progression, Integer> colId;
 @FXML private TableColumn<Progression, String> colUtilisateur;
 @FXML private TableColumn<Progression, String> colCours;
 @FXML private TableColumn<Progression, String> colPourcentage;
 @FXML private TableColumn<Progression, String> colXP;
 @FXML private TableColumn<Progression, String> colNiveau;
 @FXML private TableColumn<Progression, String> colEtat;
 @FXML private TableColumn<Progression, Void> colActions;

 // Composants de filtre
 @FXML private ComboBox<String> cboEtatFilter;
 @FXML private TextField txtRecherche;

 // Composants du formulaire
 @FXML private VBox formContainer;
 @FXML private Label formTitle;
 @FXML private TextField txtId;
 @FXML private TextField txtUserId;
 @FXML private ComboBox<String> cboCours;
 @FXML private TextField txtPourcentage;
 @FXML private TextField txtPointsXP;
 @FXML private TextField txtNiveau;
 @FXML private ComboBox<EtatProgression> cboEtat;
 @FXML private Label lblFormMessage;

 private final ProgressionService progressionService;
 private final CoursService coursService;
 private ObservableList<Progression> progressionList;
 private List<Cours> allCours;
 private boolean isEditMode = false;

 public ProgressionController() {
 this.progressionService = new ProgressionService();
 this.coursService = new CoursService();
 }

 /**
 * Initialise le contrôleur.
 */
 @FXML
 public void initialize() {
 setupTableColumns();
 setupComboBoxes();
 loadProgressions();
 loadSummary();
 clearForm();

 // Animation des composants au chargement
 AnimationUtils.playFadeScaleIn(progressionTable, 300, 150);
 AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
 }

 /**
 * Configure les colonnes du tableau.
 */
 private void setupTableColumns() {
 colId.setCellValueFactory(new PropertyValueFactory<>("id"));

 colUtilisateur.setCellValueFactory(cellData -> {
 String nom = cellData.getValue().getUserNom();
 return new SimpleStringProperty(nom != null ? nom : "Utilisateur #" + cellData.getValue().getUserId());
 });

 colCours.setCellValueFactory(cellData -> {
 String titre = cellData.getValue().getCoursTitre();
 return new SimpleStringProperty(titre != null ? titre : "Cours #" + cellData.getValue().getCoursId());
 });

 colPourcentage.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getPourcentageFormate()));

 // Ajouter une barre de progression visuelle
 colPourcentage.setCellFactory(column -> new TableCell<Progression, String>() {
 private final ProgressBar progressBar = new ProgressBar();
 private final Label label = new Label();
 private final HBox container = new HBox(5, progressBar, label);

 {
 progressBar.setPrefWidth(70);
 progressBar.setPrefHeight(14);
 }

 @Override
 protected void updateItem(String item, boolean empty) {
 super.updateItem(item, empty);
 if (empty || item == null) {
 setGraphic(null);
 } else {
 Progression prog = getTableView().getItems().get(getIndex());
 progressBar.setProgress(prog.getPourcentage() / 100.0);
 label.setText(item);

 String color = prog.getPourcentage() >= 100 ? "#27ae60" :
 prog.getPourcentage() >= 50 ? "#f39c12" : "#3498db";
 progressBar.setStyle("-fx-accent: " + color + ";");

 setGraphic(container);
 }
 }
 });

 colXP.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getPointsXP() + " XP"));

 colNiveau.setCellValueFactory(cellData ->
 new SimpleStringProperty("Niv. " + cellData.getValue().getNiveau()));

 colEtat.setCellValueFactory(cellData -> {
 EtatProgression etat = cellData.getValue().getEtat();
 return new SimpleStringProperty(etat.getDisplayName());
 });

 // Appliquer le retour à la ligne
 colUtilisateur.setCellFactory(new WrappedTextCellFactory<>());
 colCours.setCellFactory(new WrappedTextCellFactory<>());

 // Configuration des boutons d'action
 colActions.setCellFactory(param -> new TableCell<>() {
 private final Button editBtn = new Button("Modifier");
 private final Button deleteBtn = new Button("Supprimer");
 private final HBox pane = new HBox(5, editBtn, deleteBtn);

 {
 editBtn.getStyleClass().add("btn-warning");
 editBtn.setStyle("-fx-padding: 5 8;");
 editBtn.setTooltip(new Tooltip("Modifier la progression"));

 deleteBtn.getStyleClass().add("btn-danger");
 deleteBtn.setStyle("-fx-padding: 5 8;");
 deleteBtn.setTooltip(new Tooltip("Supprimer la progression"));

 editBtn.setOnAction(event -> {
 Progression prog = getTableView().getItems().get(getIndex());
 editProgression(prog);
 });

 deleteBtn.setOnAction(event -> {
 Progression prog = getTableView().getItems().get(getIndex());
 deleteProgression(prog);
 });
 }

 @Override
 protected void updateItem(Void item, boolean empty) {
 super.updateItem(item, empty);
 if (empty) {
 setGraphic(null);
 } else {
 setGraphic(pane);
 }
 }
 });
 }

 /**
 * Configure les ComboBox.
 */
 private void setupComboBoxes() {
 // Filtre par état
 List<String> etatOptions = Arrays.stream(EtatProgression.values())
 .map(EtatProgression::getDisplayName)
 .collect(Collectors.toList());
 etatOptions.add(0, "Tous les états");
 cboEtatFilter.setItems(FXCollections.observableArrayList(etatOptions));
 cboEtatFilter.setValue("Tous les états");

 // ComboBox état du formulaire
 cboEtat.setItems(FXCollections.observableArrayList(EtatProgression.values()));

 // ComboBox cours du formulaire
 allCours = coursService.trouverTous();
 List<String> coursOptions = allCours.stream()
 .map(c -> c.getId() + " - " + c.getTitre())
 .collect(Collectors.toList());
 cboCours.setItems(FXCollections.observableArrayList(coursOptions));
 }

 /**
 * Charge les progressions dans le tableau.
 */
 private void loadProgressions() {
 List<Progression> progressions = progressionService.trouverToutes();
 progressionList = FXCollections.observableArrayList(progressions);
 progressionTable.setItems(progressionList);
 }

 /**
 * Charge les statistiques de résumé.
 */
 private void loadSummary() {
 List<Progression> all = progressionService.trouverToutes();
 lblTotalProgressions.setText(String.valueOf(all.size()));

 long enCours = all.stream().filter(p -> p.getEtat() == EtatProgression.EN_COURS).count();
 long completes = all.stream().filter(p -> p.getEtat() == EtatProgression.COMPLETE || p.getEtat() == EtatProgression.CERTIFIE).count();
 int totalXP = all.stream().mapToInt(Progression::getPointsXP).sum();

 lblEnCours.setText(String.valueOf(enCours));
 lblCompletes.setText(String.valueOf(completes));
 lblTotalXP.setText(totalXP + " XP");
 }

 /**
 * Affiche le formulaire d'ajout.
 */
 @FXML
 public void showAddForm() {
 isEditMode = false;
 clearForm();
 formTitle.setText("+ Nouvelle Progression");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Édite une progression existante.
 */
 private void editProgression(Progression prog) {
 isEditMode = true;
 formTitle.setText("Modifier la Progression");

 txtId.setText(String.valueOf(prog.getId()));
 txtUserId.setText(String.valueOf(prog.getUserId()));

 // Sélectionner le cours dans le ComboBox
 String coursDisplay = prog.getCoursId() + " - " + 
 (prog.getCoursTitre() != null ? prog.getCoursTitre() : "Cours #" + prog.getCoursId());
 cboCours.setValue(coursDisplay);

 txtPourcentage.setText(String.valueOf(prog.getPourcentage()));
 txtPointsXP.setText(String.valueOf(prog.getPointsXP()));
 txtNiveau.setText(String.valueOf(prog.getNiveau()));
 cboEtat.setValue(prog.getEtat());

 lblFormMessage.setText("");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Supprime une progression.
 */
 private void deleteProgression(Progression prog) {
 String coursInfo = prog.getCoursTitre() != null ? prog.getCoursTitre() : "Cours #" + prog.getCoursId();
 boolean confirm = AlertUtils.showConfirmation(
 "Suppression",
 "Voulez-vous vraiment supprimer la progression pour \"" + coursInfo + "\" ?"
 );

 if (confirm) {
 boolean success = progressionService.supprimer(prog.getId());
 if (success) {
 progressionList.remove(prog);
 loadSummary();
 AlertUtils.showSuccess("Progression supprimée avec succès !");
 } else {
 AlertUtils.showError("Erreur", "Erreur lors de la suppression de la progression.");
 }
 }
 }

 /**
 * Enregistre la progression (création ou modification).
 */
 @FXML
 public void saveProgression() {
 try {
 Progression prog = buildProgressionFromForm();

 if (isEditMode) {
 boolean success = progressionService.mettreAJour(prog);
 if (success) {
 loadProgressions();
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Progression modifiée avec succès !");
 } else {
 showFormError("Erreur lors de la modification.");
 }
 } else {
 Progression created = progressionService.creer(prog);
 if (created != null) {
 progressionList.add(created);
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Progression créée avec succès !");
 } else {
 showFormError("Erreur lors de la création.");
 }
 }

 } catch (IllegalArgumentException e) {
 showFormError(e.getMessage());
 }
 }

 /**
 * Construit un objet Progression à partir du formulaire.
 */
 private Progression buildProgressionFromForm() {
 Progression prog = new Progression();

 if (!txtId.getText().isEmpty()) {
 prog.setId(Integer.parseInt(txtId.getText()));
 }

 // User ID
 if (txtUserId.getText().isEmpty()) {
 throw new IllegalArgumentException("L'ID utilisateur est requis.");
 }
 prog.setUserId(Integer.parseInt(txtUserId.getText()));

 // Cours ID (extrait du format "ID - Titre")
 if (cboCours.getValue() == null || cboCours.getValue().isEmpty()) {
 throw new IllegalArgumentException("Le cours est requis.");
 }
 String coursSelection = cboCours.getValue();
 int coursId = Integer.parseInt(coursSelection.split(" - ")[0].trim());
 prog.setCoursId(coursId);

 // Pourcentage
 double pourcentage = parseDoubleOrDefault(txtPourcentage.getText(), 0.0);
 prog.setPourcentage(pourcentage);

 // Points XP
 prog.setPointsXP(parseIntOrDefault(txtPointsXP.getText(), 0));

 // Niveau
 prog.setNiveau(parseIntOrDefault(txtNiveau.getText(), 1));

 // État
 if (cboEtat.getValue() != null) {
 prog.setEtat(cboEtat.getValue());
 } else {
 prog.setEtat(EtatProgression.NON_COMMENCE);
 }

 return prog;
 }

 /**
 * Parse un entier ou retourne la valeur par défaut.
 */
 private int parseIntOrDefault(String text, int defaultValue) {
 if (text == null || text.trim().isEmpty()) return defaultValue;
 try {
 return Integer.parseInt(text.trim());
 } catch (NumberFormatException e) {
 return defaultValue;
 }
 }

 /**
 * Parse un double ou retourne la valeur par défaut.
 */
 private double parseDoubleOrDefault(String text, double defaultValue) {
 if (text == null || text.trim().isEmpty()) return defaultValue;
 try {
 return Double.parseDouble(text.trim());
 } catch (NumberFormatException e) {
 return defaultValue;
 }
 }

 /**
 * Annule le formulaire.
 */
 @FXML
 public void cancelForm() {
 clearForm();
 }

 /**
 * Vide le formulaire.
 */
 private void clearForm() {
 isEditMode = false;
 formTitle.setText("+ Nouvelle Progression");
 txtId.setText("");
 txtUserId.setText("1");
 cboCours.setValue(null);
 txtPourcentage.setText("0.0");
 txtPointsXP.setText("0");
 txtNiveau.setText("1");
 cboEtat.setValue(EtatProgression.NON_COMMENCE);
 lblFormMessage.setText("");
 }

 /**
 * Filtre les progressions.
 */
 @FXML
 public void filterProgressions() {
 String etatFilter = cboEtatFilter.getValue();
 String recherche = txtRecherche.getText().toLowerCase().trim();

 List<Progression> filtered = progressionService.trouverToutes().stream()
 .filter(p -> {
 boolean matchEtat = etatFilter.equals("Tous les états") ||
 p.getEtat().getDisplayName().equals(etatFilter);
 boolean matchRecherche = recherche.isEmpty() ||
 (p.getCoursTitre() != null && p.getCoursTitre().toLowerCase().contains(recherche)) ||
 (p.getUserNom() != null && p.getUserNom().toLowerCase().contains(recherche));
 return matchEtat && matchRecherche;
 })
 .collect(Collectors.toList());

 progressionList = FXCollections.observableArrayList(filtered);
 progressionTable.setItems(progressionList);
 }

 /**
 * Efface les filtres.
 */
 @FXML
 public void clearFilter() {
 cboEtatFilter.setValue("Tous les états");
 txtRecherche.setText("");
 loadProgressions();
 }

 /**
 * Actualise le tableau.
 */
 @FXML
 public void refreshTable() {
 loadProgressions();
 loadSummary();
 AnimationUtils.playFadeScaleIn(progressionTable, 200, 0);
 }

 /**
 * Affiche un message d'erreur dans le formulaire.
 */
 private void showFormError(String message) {
 lblFormMessage.setText("X " + message);
 lblFormMessage.setStyle("-fx-text-fill: #e74c3c;");
 }
}
