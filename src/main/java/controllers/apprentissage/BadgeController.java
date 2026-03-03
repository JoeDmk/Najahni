package controllers.apprentissage;

import models.apprentissage.Badge;
import models.apprentissage.Badge.Rarete;
import services.apprentissage.BadgeService;
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
 * Controller pour la gestion des Badges.
 * Gère les opérations CRUD pour les badges avec animations fluides.
 */
public class BadgeController {

 // Labels de résumé
 @FXML private Label lblTotalBadges;
 @FXML private Label lblBadgesCommun;
 @FXML private Label lblBadgesRare;
 @FXML private Label lblBadgesLegendaire;

 // Composants du tableau
 @FXML private TableView<Badge> badgeTable;
 @FXML private TableColumn<Badge, Integer> colId;
 @FXML private TableColumn<Badge, String> colIcone;
 @FXML private TableColumn<Badge, String> colNom;
 @FXML private TableColumn<Badge, String> colRarete;
 @FXML private TableColumn<Badge, String> colCategorie;
 @FXML private TableColumn<Badge, String> colCondition;
 @FXML private TableColumn<Badge, String> colActif;
 @FXML private TableColumn<Badge, Void> colActions;

 // Composants de filtre
 @FXML private ComboBox<String> cboRareteFilter;
 @FXML private ComboBox<String> cboCategorieFilter;
 @FXML private TextField txtRecherche;

 // Composants du formulaire
 @FXML private VBox formContainer;
 @FXML private Label formTitle;
 @FXML private TextField txtId;
 @FXML private TextField txtNom;
 @FXML private TextArea txtDescription;
 @FXML private TextField txtIcone;
 @FXML private ComboBox<String> cboCategorie;
 @FXML private ComboBox<Rarete> cboRarete;
 @FXML private TextArea txtCondition;
 @FXML private TextField txtPointsRequis;
 @FXML private TextField txtCoursRequis;
 @FXML private TextField txtNiveauRequis;
 @FXML private CheckBox chkActif;
 @FXML private Label lblFormMessage;

 private final BadgeService badgeService;
 private ObservableList<Badge> badgeList;
 private boolean isEditMode = false;

 private static final List<String> CATEGORIES = List.of(
 "Général", "Apprentissage", "Social", "Compétition", "Spécial"
 );

 public BadgeController() {
 this.badgeService = new BadgeService();
 }

 /**
 * Initialise le contrôleur.
 */
 @FXML
 public void initialize() {
 badgeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
 setupTableColumns();
 setupComboBoxes();
 loadBadges();
 loadSummary();
 clearForm();

 // Animation des composants au chargement
 AnimationUtils.playFadeScaleIn(badgeTable, 300, 150);
 AnimationUtils.playFadeScaleIn(formContainer, 300, 250);
 }

 /**
 * Configure les colonnes du tableau.
 */
 private void setupTableColumns() {
 colId.setCellValueFactory(new PropertyValueFactory<>("id"));

 colIcone.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getIcone() != null ? cellData.getValue().getIcone() : "[B]"));

 colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

 colRarete.setCellValueFactory(cellData -> {
 Rarete rarete = cellData.getValue().getRarete();
 String display = switch (rarete) {
 case COMMUN -> "o Commun";
 case RARE -> "Rare";
 case EPIQUE -> "Epique";
 case LEGENDAIRE -> "Legendaire";
 };
 return new SimpleStringProperty(display);
 });

 colCategorie.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getCategorie() != null ? cellData.getValue().getCategorie() : "Général"));

 colCondition.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().getConditionObtention()));

 colActif.setCellValueFactory(cellData ->
 new SimpleStringProperty(cellData.getValue().isActif() ? " Oui" : "X Non"));

 // Appliquer le retour à la ligne
 colNom.setCellFactory(new WrappedTextCellFactory<>());
 colCondition.setCellFactory(new WrappedTextCellFactory<>());
 colCategorie.setCellFactory(new WrappedTextCellFactory<>());

 // Configuration des boutons d'action
 colActions.setCellFactory(param -> new TableCell<>() {
 private final Button editBtn = new Button("Modifier");
 private final Button deleteBtn = new Button("Supprimer");
 private final HBox pane = new HBox(5, editBtn, deleteBtn);

 {
 editBtn.getStyleClass().add("btn-warning");
 editBtn.setStyle("-fx-padding: 5 8;");
 editBtn.setTooltip(new Tooltip("Modifier le badge"));

 deleteBtn.getStyleClass().add("btn-danger");
 deleteBtn.setStyle("-fx-padding: 5 8;");
 deleteBtn.setTooltip(new Tooltip("Supprimer le badge"));

 editBtn.setOnAction(event -> {
 Badge badge = getTableView().getItems().get(getIndex());
 editBadge(badge);
 });

 deleteBtn.setOnAction(event -> {
 Badge badge = getTableView().getItems().get(getIndex());
 deleteBadge(badge);
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
 // Filtre par rareté
 List<String> rareteOptions = Arrays.stream(Rarete.values())
 .map(Rarete::name)
 .collect(Collectors.toList());
 rareteOptions.add(0, "Toutes les raretés");
 cboRareteFilter.setItems(FXCollections.observableArrayList(rareteOptions));
 cboRareteFilter.setValue("Toutes les raretés");

 // Filtre par catégorie
 List<String> categorieOptions = new java.util.ArrayList<>(CATEGORIES);
 categorieOptions.add(0, "Toutes les catégories");
 cboCategorieFilter.setItems(FXCollections.observableArrayList(categorieOptions));
 cboCategorieFilter.setValue("Toutes les catégories");

 // ComboBox du formulaire
 cboRarete.setItems(FXCollections.observableArrayList(Rarete.values()));
 cboCategorie.setItems(FXCollections.observableArrayList(CATEGORIES));
 }

 /**
 * Charge les badges dans le tableau.
 */
 private void loadBadges() {
 List<Badge> badges = badgeService.trouverTous();
 badgeList = FXCollections.observableArrayList(badges);
 badgeTable.setItems(badgeList);
 }

 /**
 * Charge les statistiques de résumé.
 */
 private void loadSummary() {
 List<Badge> all = badgeService.trouverTous();
 lblTotalBadges.setText(String.valueOf(all.size()));

 long commun = all.stream().filter(b -> b.getRarete() == Rarete.COMMUN).count();
 long rare = all.stream().filter(b -> b.getRarete() == Rarete.RARE).count();
 long epiqueLegendaire = all.stream().filter(b -> b.getRarete() == Rarete.EPIQUE || b.getRarete() == Rarete.LEGENDAIRE).count();

 lblBadgesCommun.setText(String.valueOf(commun));
 lblBadgesRare.setText(String.valueOf(rare));
 lblBadgesLegendaire.setText(String.valueOf(epiqueLegendaire));
 }

 /**
 * Affiche le formulaire d'ajout.
 */
 @FXML
 public void showAddForm() {
 isEditMode = false;
 clearForm();
 formTitle.setText("+ Ajouter un Badge");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Édite un badge existant.
 */
 private void editBadge(Badge badge) {
 isEditMode = true;
 formTitle.setText("Modifier le Badge");

 txtId.setText(String.valueOf(badge.getId()));
 txtNom.setText(badge.getNom());
 txtDescription.setText(badge.getDescription());
 txtIcone.setText(badge.getIcone());
 cboCategorie.setValue(badge.getCategorie());
 cboRarete.setValue(badge.getRarete());
 txtCondition.setText(badge.getConditionObtention());
 txtPointsRequis.setText(String.valueOf(badge.getPointsRequis()));
 txtCoursRequis.setText(String.valueOf(badge.getCoursRequis()));
 txtNiveauRequis.setText(String.valueOf(badge.getNiveauRequis()));
 chkActif.setSelected(badge.isActif());

 lblFormMessage.setText("");
 AnimationUtils.playFadeScaleIn(formContainer, 200, 0);
 }

 /**
 * Supprime un badge.
 */
 private void deleteBadge(Badge badge) {
 boolean confirm = AlertUtils.showConfirmation(
 "Suppression",
 "Voulez-vous vraiment supprimer le badge \"" + badge.getNom() + "\" ?"
 );

 if (confirm) {
 boolean success = badgeService.supprimerBadge(badge.getId());
 if (success) {
 badgeList.remove(badge);
 loadSummary();
 AlertUtils.showSuccess("Badge supprimé avec succès !");
 } else {
 AlertUtils.showError("Erreur", "Erreur lors de la suppression du badge.");
 }
 }
 }

 /**
 * Enregistre le badge (création ou modification).
 */
 @FXML
 public void saveBadge() {
 try {
 Badge badge = buildBadgeFromForm();

 if (isEditMode) {
 boolean success = badgeService.modifierBadge(badge);
 if (success) {
 loadBadges();
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Badge modifié avec succès !");
 } else {
 showFormError("Erreur lors de la modification.");
 }
 } else {
 Badge created = badgeService.creerBadge(badge);
 if (created != null) {
 badgeList.add(created);
 loadSummary();
 clearForm();
 AlertUtils.showSuccess("Badge créé avec succès !");
 } else {
 showFormError("Erreur lors de la création.");
 }
 }

 } catch (IllegalArgumentException e) {
 showFormError(e.getMessage());
 }
 }

 /**
 * Construit un objet Badge à partir du formulaire.
 */
 private Badge buildBadgeFromForm() {
 Badge badge = new Badge();

 if (!txtId.getText().isEmpty()) {
 badge.setId(Integer.parseInt(txtId.getText()));
 }

 badge.setNom(txtNom.getText());
 badge.setDescription(txtDescription.getText());
 badge.setIcone(txtIcone.getText().isEmpty() ? "[B]" : txtIcone.getText());
 badge.setCategorie(cboCategorie.getValue() != null ? cboCategorie.getValue() : "Général");
 badge.setRarete(cboRarete.getValue() != null ? cboRarete.getValue() : Rarete.COMMUN);
 badge.setConditionObtention(txtCondition.getText());

 badge.setPointsRequis(parseIntOrDefault(txtPointsRequis.getText(), 0));
 badge.setCoursRequis(parseIntOrDefault(txtCoursRequis.getText(), 0));
 badge.setNiveauRequis(parseIntOrDefault(txtNiveauRequis.getText(), 0));
 badge.setActif(chkActif.isSelected());

 return badge;
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
 formTitle.setText("+ Ajouter un Badge");
 txtId.setText("");
 txtNom.setText("");
 txtDescription.setText("");
 txtIcone.setText("[B]");
 cboCategorie.setValue("Général");
 cboRarete.setValue(Rarete.COMMUN);
 txtCondition.setText("");
 txtPointsRequis.setText("0");
 txtCoursRequis.setText("0");
 txtNiveauRequis.setText("0");
 chkActif.setSelected(true);
 lblFormMessage.setText("");
 }

 /**
 * Filtre les badges.
 */
 @FXML
 public void filterBadges() {
 String rareteFilter = cboRareteFilter.getValue();
 String categorieFilter = cboCategorieFilter.getValue();
 String recherche = txtRecherche.getText().toLowerCase().trim();

 List<Badge> filtered = badgeService.trouverTous().stream()
 .filter(b -> {
 boolean matchRarete = rareteFilter.equals("Toutes les raretés") ||
 b.getRarete().name().equals(rareteFilter);
 boolean matchCategorie = categorieFilter.equals("Toutes les catégories") ||
 (b.getCategorie() != null && b.getCategorie().equals(categorieFilter));
 boolean matchRecherche = recherche.isEmpty() ||
 b.getNom().toLowerCase().contains(recherche) ||
 (b.getDescription() != null && b.getDescription().toLowerCase().contains(recherche));
 return matchRarete && matchCategorie && matchRecherche;
 })
 .collect(Collectors.toList());

 badgeList = FXCollections.observableArrayList(filtered);
 badgeTable.setItems(badgeList);
 }

 /**
 * Efface les filtres.
 */
 @FXML
 public void clearFilter() {
 cboRareteFilter.setValue("Toutes les raretés");
 cboCategorieFilter.setValue("Toutes les catégories");
 txtRecherche.setText("");
 loadBadges();
 }

 /**
 * Actualise le tableau.
 */
 @FXML
 public void refreshTable() {
 loadBadges();
 loadSummary();
 AnimationUtils.playFadeScaleIn(badgeTable, 200, 0);
 }

 /**
 * Affiche un message d'erreur dans le formulaire.
 */
 private void showFormError(String message) {
 lblFormMessage.setText("X " + message);
 lblFormMessage.setStyle("-fx-text-fill: #e74c3c;");
 }
}
