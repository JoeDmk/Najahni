package controllers.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.NiveauCours;
import models.apprentissage.TypeCours;
import services.apprentissage.CoursService;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller pour la gestion des Cours.
 * Gère les opérations CRUD pour les cours avec animations fluides.
 */
public class CoursController {

 // Labels de résumé
 @FXML
 private Label lblTotalCours;

 @FXML
 private Label lblCoursDebutant;

 @FXML
 private Label lblCoursCertifiants;

 @FXML
 private Label lblTotalXP;

 // Composants du tableau
 @FXML
 private TableView<Cours> coursTable;

 @FXML
 private TableColumn<Cours, Integer> colId;

 @FXML
 private TableColumn<Cours, String> colTitre;

 @FXML
 private TableColumn<Cours, String> colType;

 @FXML
 private TableColumn<Cours, String> colNiveau;

 @FXML
 private TableColumn<Cours, String> colPointsXP;

 @FXML
 private TableColumn<Cours, String> colCertification;

 @FXML
 private TableColumn<Cours, Void> colActions;

 // Composants de filtre
 @FXML
 private ComboBox<String> cboNiveauFilter;

 @FXML
 private ComboBox<String> cboTypeFilter;

 @FXML
 private TextField txtRecherche;

 // Composants du formulaire
 @FXML
 private VBox formContainer;

 @FXML
 private Label formTitle;

 @FXML
 private TextField txtId;

 @FXML
 private TextField txtTitre;

 @FXML
 private TextArea txtDescription;

 @FXML
 private ComboBox<TypeCours> cboType;

 @FXML
 private ComboBox<NiveauCours> cboNiveau;

 @FXML
 private TextField txtPointsXP;

 @FXML
 private TextField txtDuree;

 @FXML
 private CheckBox chkCertification;

 @FXML
 private Label lblFormMessage;

 @FXML
 private TextField txtDocumentPath;

 @FXML
 private Label lblDocumentName;
 @FXML private VBox vboxDocumentField;
 @FXML private VBox vboxVideoField;
 @FXML private TextField txtVideoUrl;

 private final CoursService coursService;
 private ObservableList<Cours> coursList;
 private boolean isEditMode = false;

 public CoursController() {
 this.coursService = new CoursService();
 }

 /**
 * Initialise le contrôleur.
 */
 @FXML
 public void initialize() {
 coursTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
 setupTableColumns();
 setupComboBoxes();
 loadCours();
 loadSummary();
 clearForm();

 // Animation des composants au chargement
 AnimationUtils.playFadeScaleIn(coursTable, 300, 150);
 AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
 cboType.valueProperty().addListener((obs, oldVal, newVal) -> updateMediaField(newVal));
 }

 private void updateMediaField(TypeCours type) {
 boolean isVideo = type == TypeCours.VIDEO;
 boolean isQuiz = type == TypeCours.QUIZ;
 boolean isMedia = isVideo || isQuiz;

 vboxDocumentField.setVisible(!isMedia);
 vboxDocumentField.setManaged(!isMedia);

 vboxVideoField.setVisible(isMedia);
 vboxVideoField.setManaged(isMedia);

 // Update the label depending on type
 if (isQuiz) {
 // Find the label inside vboxVideoField and update it
 ((javafx.scene.control.Label) vboxVideoField.getChildren().get(0))
 .setText("URL du Quiz");
 ((javafx.scene.control.TextField) vboxVideoField.getChildren().get(1))
 .setPromptText("https://forms.google.com/...");
 } else if (isVideo) {
 ((javafx.scene.control.Label) vboxVideoField.getChildren().get(0))
 .setText("URL de la video");
 ((javafx.scene.control.TextField) vboxVideoField.getChildren().get(1))
 .setPromptText("https://youtube.com/...");
 }
 }

 /**
 * Configure les colonnes du tableau.
 */
 private void setupTableColumns() {
 colId.setCellValueFactory(new PropertyValueFactory<>("id"));
 colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));

 colType.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getTypeIcon() + " " + 
 cellData.getValue().getType().getDisplayName()));

 colNiveau.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getNiveauIcon() + " " + 
 cellData.getValue().getNiveau().getDisplayName()));

 colPointsXP.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getPointsXP() + " XP"));

 colCertification.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().isCertification() ? " Oui" : "X Non"));

 // Appliquer le retour à la ligne
 colTitre.setCellFactory(new WrappedTextCellFactory<>());
 colType.setCellFactory(new WrappedTextCellFactory<>());
 colNiveau.setCellFactory(new WrappedTextCellFactory<>());

 // Configuration des boutons d'action
 colActions.setCellFactory(param -> new TableCell<>() {
 private final Button editBtn = new Button("Modifier");
 private final Button deleteBtn = new Button("Supprimer");
 private final HBox pane = new HBox(5, editBtn, deleteBtn);

 {
 editBtn.getStyleClass().add("btn-warning");
 editBtn.setStyle("-fx-padding: 5 8;");
 editBtn.setTooltip(new Tooltip("Modifier le cours"));

 deleteBtn.getStyleClass().add("btn-danger");
 deleteBtn.setStyle("-fx-padding: 5 8;");
 deleteBtn.setTooltip(new Tooltip("Supprimer le cours"));

 editBtn.setOnAction(event -> {
 Cours cours = getTableView().getItems().get(getIndex());
 editCours(cours);
 });

 deleteBtn.setOnAction(event -> {
 Cours cours = getTableView().getItems().get(getIndex());
 deleteCours(cours);
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
 // Filtre par niveau
 List<String> niveauOptions = Arrays.stream(NiveauCours.values())
 .map(NiveauCours::getDisplayName)
 .collect(Collectors.toList());
 niveauOptions.add(0, "Tous les niveaux");
 cboNiveauFilter.setItems(FXCollections.observableArrayList(niveauOptions));
 cboNiveauFilter.setValue("Tous les niveaux");

 // Filtre par type
 List<String> typeOptions = Arrays.stream(TypeCours.values())
 .map(TypeCours::getDisplayName)
 .collect(Collectors.toList());
 typeOptions.add(0, "Tous les types");
 cboTypeFilter.setItems(FXCollections.observableArrayList(typeOptions));
 cboTypeFilter.setValue("Tous les types");

 // ComboBox du formulaire
 cboType.setItems(FXCollections.observableArrayList(TypeCours.values()));
 cboNiveau.setItems(FXCollections.observableArrayList(NiveauCours.values()));
 }

 /**
 * Charge les cours dans le tableau.
 */
 private void loadCours() {
 List<Cours> cours = coursService.trouverTous();
 coursList = FXCollections.observableArrayList(cours);
 coursTable.setItems(coursList);
 }

 /**
 * Charge les statistiques de résumé.
 */
 private void loadSummary() {
 lblTotalCours.setText(String.valueOf(coursService.compterTous()));
 lblCoursDebutant.setText(String.valueOf(coursService.compterParNiveau(NiveauCours.DEBUTANT)));
 lblCoursCertifiants.setText(String.valueOf(coursService.compterCertifiants()));

 // Calculer le total des XP disponibles
 int totalXP = coursService.calculerTotalXP();
 lblTotalXP.setText(totalXP + " XP");
 }

 /**
 * Affiche le formulaire d'ajout.
 */
 @FXML
 public void showAddForm() {
 isEditMode = false;
 clearForm();
 formTitle.setText("+ Ajouter un Cours");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Édite un cours existant.
 */
 private void editCours(Cours cours) {
 isEditMode = true;
 formTitle.setText("Modifier le Cours");

 txtId.setText(String.valueOf(cours.getId()));
 txtTitre.setText(cours.getTitre());
 txtDescription.setText(cours.getDescription());

 cboType.setValue(cours.getType());
 cboNiveau.setValue(cours.getNiveau());
 updateMediaField(cours.getType());

 // Populate document path or video URL correctly from Optional
 txtDocumentPath.setText("");
 lblDocumentName.setText("");
 txtVideoUrl.setText("");
 cours.getDocumentPath().ifPresent(p -> {
 txtDocumentPath.setText(p);
 lblDocumentName.setText(" " + new java.io.File(p).getName());
 });
 cours.getVideoUrl().ifPresent(txtVideoUrl::setText);

 txtPointsXP.setText(String.valueOf(cours.getPointsXP()));
 txtDuree.setText(String.valueOf(cours.getDureeMinutes()));
 chkCertification.setSelected(cours.isCertification());

 lblFormMessage.setText("");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Supprime un cours.
 */
 private void deleteCours(Cours cours) {
 boolean confirm = AlertUtils.showConfirmation(
 "Suppression",
 "Voulez-vous vraiment supprimer le cours \"" + cours.getTitre() + "\" ?"
 );

 if (confirm) {
 boolean success = coursService.supprimerCours(cours.getId());
 if (success) {
 coursList.remove(cours);
 loadSummary();
 AlertUtils.showSuccess("Cours supprimé avec succès !");
 } else {
 AlertUtils.showError("Erreur", "Erreur lors de la suppression du cours.");
 }
 }
 }

 /**
 * Enregistre le cours (création ou modification).
 */
 @FXML
 public void saveCours() {
 try {
 Cours cours = buildCoursFromForm();

 if (isEditMode) {
 boolean success = coursService.modifierCours(cours);
 if (success) {
 loadCours();
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Cours modifié avec succès !");
 } else {
 showFormError("Erreur lors de la modification.");
 }
 } else {
 Cours created = coursService.creerCours(cours);
 if (created != null) {
 coursList.add(created);
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Cours créé avec succès !");
 } else {
 showFormError("Erreur lors de la création.");
 }
 }

 } catch (IllegalArgumentException e) {
 showFormError(e.getMessage());
 }
 }

 /**
 * Construit un objet Cours à partir du formulaire.
 */
 private Cours buildCoursFromForm() {
 Cours cours = new Cours();

 if (!txtId.getText().isEmpty()) {
 cours.setId(Integer.parseInt(txtId.getText()));
 }

 cours.setTitre(txtTitre.getText());
 cours.setDescription(txtDescription.getText());
 cours.setType(cboType.getValue());
 cours.setNiveau(cboNiveau.getValue());

 if (!txtPointsXP.getText().isEmpty()) {
 cours.setPointsXP(Integer.parseInt(txtPointsXP.getText()));
 }

 if (!txtDuree.getText().isEmpty()) {
 cours.setDureeMinutes(Integer.parseInt(txtDuree.getText()));
 }

 cours.setCertification(chkCertification.isSelected());
 // Replace the documentPath line with:
 if (cboType.getValue() == TypeCours.VIDEO) {
 cours.setVideoUrl(txtVideoUrl.getText().trim());
 cours.setDocumentPath(null);
 } else {
 cours.setDocumentPath(txtDocumentPath.getText().trim());
 cours.setVideoUrl(null);
 }
 cours.setCreateurId(1); // TODO: Utiliser l'utilisateur connecté

 return cours;
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
 formTitle.setText("+ Ajouter un Cours");
 txtId.setText("");
 txtTitre.setText("");
 txtDescription.setText("");
 cboType.setValue(TypeCours.TEXTE);
 cboNiveau.setValue(NiveauCours.DEBUTANT);
 txtPointsXP.setText("100");
 txtDuree.setText("30");
 chkCertification.setSelected(false);
 lblFormMessage.setText("");
 txtDocumentPath.setText("");
 lblDocumentName.setText("");
 txtVideoUrl.setText("");
 updateMediaField(TypeCours.TEXTE); // reset to default (document field shown)
 }

 /**
 * Filtre les cours.
 */
 @FXML
 public void filterCours() {
 String niveauFilter = cboNiveauFilter.getValue();
 String typeFilter = cboTypeFilter.getValue();
 String recherche = txtRecherche.getText().toLowerCase().trim();

 List<Cours> filtered = coursService.trouverTous().stream()
 .filter(c -> {
 boolean matchNiveau = niveauFilter.equals("Tous les niveaux") ||
 c.getNiveau().getDisplayName().equals(niveauFilter);
 boolean matchType = typeFilter.equals("Tous les types") ||
 c.getType().getDisplayName().equals(typeFilter);
 boolean matchRecherche = recherche.isEmpty() ||
 c.getTitre().toLowerCase().contains(recherche) ||
 (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));
 return matchNiveau && matchType && matchRecherche;
 })
 .collect(Collectors.toList());

 coursList = FXCollections.observableArrayList(filtered);
 coursTable.setItems(coursList);
 }

 /**
 * Efface les filtres.
 */
 @FXML
 public void clearFilter() {
 cboNiveauFilter.setValue("Tous les niveaux");
 cboTypeFilter.setValue("Tous les types");
 txtRecherche.setText("");
 loadCours();
 }

 /**
 * Actualise le tableau.
 */
 @FXML
 public void refreshTable() {
 loadCours();
 loadSummary();
 AnimationUtils.playFadeScaleIn(coursTable, 200, 0);
 }

 /**
 * Affiche un message d'erreur dans le formulaire.
 */
 private void showFormError(String message) {
 lblFormMessage.setText("X " + message);
 lblFormMessage.setStyle("-fx-text-fill: #e74c3c;");
 }

 /**
 * Opens a FileChooser to select a document (PDF, DOCX, etc.)
 */
 @FXML
 public void browseDocument() {
 javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
 fileChooser.setTitle("Sélectionner un document");
 fileChooser.getExtensionFilters().addAll(
 new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.doc", "*.pptx", "*.xlsx", "*.txt"),
 new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"),
 new javafx.stage.FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
 );

 // Use any visible window as the owner
 javafx.stage.Window window = coursTable.getScene().getWindow();
 java.io.File selectedFile = fileChooser.showOpenDialog(window);

 if (selectedFile != null) {
 txtDocumentPath.setText(selectedFile.getAbsolutePath());
 lblDocumentName.setText(" " + selectedFile.getName());
 }
 }

 /**
 * Clears the selected document path.
 */
 @FXML
 public void clearDocument() {
 txtDocumentPath.setText("");
 lblDocumentName.setText("");
 }
}
