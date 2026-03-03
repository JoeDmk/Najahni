package controllers.apprentissage;

import models.apprentissage.Cours;
import models.apprentissage.NiveauCours;
import models.apprentissage.TypeCours;
import services.apprentissage.CoursService;
import util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import models.apprentissage.EtatProgression;
import models.apprentissage.Progression;
import services.apprentissage.PdfExportService;
import services.apprentissage.ProgressionService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Controller for the user-facing course catalogue.
 * Displays course cards in a grid; clicking a card opens CoursDetail.
 */
public class FrontOfficeCoursController {

 @FXML private TextField txtRecherche;
 @FXML private ComboBox<String> cboNiveauFilter;
 @FXML private ComboBox<String> cboTypeFilter;
 @FXML private FlowPane coursGrid;
 @FXML private VBox emptyState;
 @FXML private Label lblResultCount;

 // Level-pill buttons
 @FXML private Button btnTous;
 @FXML private Button btnDebutant;
 @FXML private Button btnInter;
 @FXML private Button btnAvance;
 @FXML private Button btnCertif;
 @FXML private Button btnExportPdf;

 private final CoursService coursService = new CoursService();
 private final ProgressionService progressionService = new ProgressionService();
 private final PdfExportService pdfExportService = new PdfExportService();
 private int currentUserId = 1; // replace with session user when available


 private List<Cours> allCours;


 /** Parent container used to swap views (set by the parent controller or main app). */
 private Pane viewContainer;

 // -----------------------------------------------------------------------
 // Initialisation
 // -----------------------------------------------------------------------

 @FXML
 public void initialize() {
 setupComboBoxes();
 loadCours();
 }

 /**
 * Injects the container that hosts this view so navigation can swap content.
 * Call this right after loading the FXML, e.g.:
 * <pre>
 * controller.setViewContainer(mainContentPane);
 * </pre>
 */
 public void setViewContainer(Pane viewContainer) {
 this.viewContainer = viewContainer;
 }

 // -----------------------------------------------------------------------
 // Setup helpers
 // -----------------------------------------------------------------------

 private void setupComboBoxes() {
 List<String> niveaux = Arrays.stream(NiveauCours.values())
 .map(NiveauCours::getDisplayName)
 .collect(Collectors.toList());
 niveaux.add(0, "Tous les niveaux");
 cboNiveauFilter.setItems(FXCollections.observableArrayList(niveaux));
 cboNiveauFilter.setValue("Tous les niveaux");

 List<String> types = Arrays.stream(TypeCours.values())
 .map(TypeCours::getDisplayName)
 .collect(Collectors.toList());
 types.add(0, "Tous les types");
 cboTypeFilter.setItems(FXCollections.observableArrayList(types));
 cboTypeFilter.setValue("Tous les types");
 }

 private void loadCours() {
 allCours = coursService.trouverTous();
 renderCards(allCours);
 }

 // -----------------------------------------------------------------------
 // Card rendering
 // -----------------------------------------------------------------------

 /**
 * Clears the grid and creates one card per course in the given list.
 */
 private void renderCards(List<Cours> list) {


 coursGrid.getChildren().clear();

 if (list == null || list.isEmpty()) {
 emptyState.setVisible(true);
 emptyState.setManaged(true);
 lblResultCount.setText("0 cours");
 return;
 }

 emptyState.setVisible(false);
 emptyState.setManaged(false);
 lblResultCount.setText(list.size() + " cours");

 for (Cours cours : list) {
 coursGrid.getChildren().add(buildCard(cours));
 }
 }

 /**
 * Builds a visual card for a single course.
 */
 private VBox buildCard(Cours cours) {
 VBox card = new VBox(10);
 card.setPrefWidth(260);
 card.setMaxWidth(260);
 card.setStyle(
 "-fx-background-color: white;" +
 "-fx-background-radius: 12;" +
 "-fx-padding: 18;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);" +
 "-fx-cursor: hand;"
 );

 // --- Top: type icon + niveau badge ---
 HBox topRow = new HBox(8);
 topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

 Label typeIcon = new Label(cours.getTypeIcon());
 typeIcon.setStyle("-fx-font-size: 22px;");

 Region spacer = new Region();
 HBox.setHgrow(spacer, Priority.ALWAYS);

 Label niveauBadge = new Label(cours.getNiveauIcon() + " " + cours.getNiveau().getDisplayName());
 niveauBadge.setStyle(
 "-fx-background-color: " + getNiveauColor(cours.getNiveau()) + ";" +
 "-fx-text-fill: white;" +
 "-fx-padding: 3 9;" +
 "-fx-background-radius: 20;" +
 "-fx-font-size: 11px;"
 );

 topRow.getChildren().addAll(typeIcon, spacer, niveauBadge);

 // --- Title ---
 Label titre = new Label(cours.getTitre());
 titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
 titre.setMaxWidth(224);
 titre.setWrapText(true);

 // --- Description preview ---
 String descText = cours.getDescription() != null && !cours.getDescription().isBlank()
 ? cours.getDescription()
 : "Aucune description disponible.";
 if (descText.length() > 90) descText = descText.substring(0, 90) + "…";

 Label desc = new Label(descText);
 desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-wrap-text: true;");
 desc.setWrapText(true);
 desc.setMaxWidth(224);

 // --- Footer: XP + certif badge ---
 HBox footer = new HBox(8);
 footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

 Label xp = new Label(cours.getPointsXP() + " XP");
 xp.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");

 Region footerSpacer = new Region();
 HBox.setHgrow(footerSpacer, Priority.ALWAYS);

 if (cours.isCertification()) {
 Label certif = new Label("Certifiant");
 certif.setStyle(
 "-fx-background-color: #9b59b6;" +
 "-fx-text-fill: white;" +
 "-fx-padding: 2 7;" +
 "-fx-background-radius: 20;" +
 "-fx-font-size: 10px;"
 );
 footer.getChildren().addAll(xp, footerSpacer, certif);
 } else {
 footer.getChildren().addAll(xp, footerSpacer);
 }

 // --- Delete button ---
 Button deleteBtn = new Button("Supprimer");
 deleteBtn.setStyle(
 "-fx-background-color: #fff0f0;" +
 "-fx-text-fill: #e74c3c;" +
 "-fx-font-size: 11px;" +
 "-fx-background-radius: 7;" +
 "-fx-padding: 5 10;" +
 "-fx-cursor: hand;"
 );
 deleteBtn.setMaxWidth(Double.MAX_VALUE);
 deleteBtn.setOnAction(e -> {
 e.consume(); // prevent card click
 handleDeleteCours(cours);
 });

 String docPath = cours.getDocumentPath().orElse(null);
 if (docPath != null && !docPath.isBlank()) {
 Label attachment = new Label("1 piece jointe");
 attachment.setStyle(
 "-fx-background-color: #eaf4fb;" +
 "-fx-text-fill: #2980b9;" +
 "-fx-font-size: 11px;" +
 "-fx-background-radius: 6;" +
 "-fx-padding: 3 8;"
 );
 card.getChildren().addAll(topRow, titre, desc, footer, attachment, deleteBtn);
 } else {
 card.getChildren().addAll(topRow, titre, desc, footer, deleteBtn);
 }

 // Hover effect
 card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);",
 "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.25), 16, 0, 0, 5);"
 )));
 card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
 "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.25), 16, 0, 0, 5);",
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);"
 )));

 // Click → detail view
 card.setOnMouseClicked(e -> {
 if (!e.isConsumed()) {
 openDetail(cours);
 }
 });

 return card;
 }

 /** Maps NiveauCours to a colour used for the badge. */
 private String getNiveauColor(NiveauCours niveau) {
 return switch (niveau) {
 case DEBUTANT -> "#27ae60";
 case INTERMEDIAIRE -> "#3498db";
 case AVANCE -> "#e67e22";
 default -> "#95a5a6";
 };
 }

 // -----------------------------------------------------------------------
 // Navigation
 // -----------------------------------------------------------------------

 /**
 * Opens the detail view for the selected course.
 * If viewContainer is set, swaps content in-place; otherwise opens a new Stage.
 */
 private void openDetail(Cours cours) {
 try {
 FXMLLoader loader = new FXMLLoader(
 getClass().getResource("/views/apprentissage/cours-details.fxml"));
 Parent detailView = loader.load();

 FrontOfficeCoursDetailsController detailCtrl = loader.getController();
 detailCtrl.setCours(cours);
 detailCtrl.setCurrentUserId(1);
 // Pass the container so the detail view can navigate back
 detailCtrl.setViewContainer(viewContainer, this::reloadView);

 if (viewContainer != null) {
 viewContainer.getChildren().setAll(detailView);
 } else {
 // Fallback: show in the same scene root
 card_getScene(detailView, cours);
 }
 } catch (IOException e) {
 e.printStackTrace();
 AlertUtils.showError("Erreur", "Impossible d'ouvrir le détail du cours.");
 }
 }

 /** Fallback navigation via scene root when no container was injected. */
 private void card_getScene(Parent detailView, Cours cours) {
 coursGrid.getScene().setRoot(detailView);
 }

 /** Called by the detail controller when the user clicks "back". */
 public void reloadView() {
 loadCours();
 }

 // -----------------------------------------------------------------------
 // Filter / Search actions
 // -----------------------------------------------------------------------

 @FXML
 public void filterCours() {
 applyFilters(cboNiveauFilter.getValue(), cboTypeFilter.getValue(), txtRecherche.getText());
 }

 @FXML
 public void clearFilter() {
 cboNiveauFilter.setValue("Tous les niveaux");
 cboTypeFilter.setValue("Tous les types");
 txtRecherche.setText("");
 resetPills();
 renderCards(allCours);
 }

 /**
 * Quick-filter by level pill buttons.
 * Each button carries its level code in the userData property.
 */
 @FXML
 public void filterByLevel(javafx.event.ActionEvent event) {
 Button clicked = (Button) event.getSource();
 String level = (String) clicked.getUserData();

 // Update pill styling
 List<Button> pills = List.of(btnTous, btnDebutant, btnInter, btnAvance, btnCertif);
 pills.forEach(b -> b.getStyleClass().setAll("pill-inactive"));
 clicked.getStyleClass().setAll("pill-active");

 List<Cours> filtered;
 if ("TOUS".equals(level)) {
 filtered = allCours;
 } else if ("CERTIFIANTS".equals(level)) {
 filtered = allCours.stream().filter(Cours::isCertification).collect(Collectors.toList());
 } else {
 filtered = allCours.stream()
 .filter(c -> c.getNiveau().name().equalsIgnoreCase(level))
 .collect(Collectors.toList());
 }
 renderCards(filtered);
 }

 private void applyFilters(String niveauFilter, String typeFilter, String recherche) {
 String r = recherche == null ? "" : recherche.toLowerCase().trim();
 List<Cours> filtered = allCours.stream()
 .filter(c -> {
 boolean matchN = "Tous les niveaux".equals(niveauFilter) ||
 c.getNiveau().getDisplayName().equals(niveauFilter);
 boolean matchT = "Tous les types".equals(typeFilter) ||
 c.getType().getDisplayName().equals(typeFilter);
 boolean matchR = r.isEmpty() ||
 c.getTitre().toLowerCase().contains(r) ||
 (c.getDescription() != null && c.getDescription().toLowerCase().contains(r));
 return matchN && matchT && matchR;
 })
 .collect(Collectors.toList());
 renderCards(filtered);
 }

 private void resetPills() {
 List<Button> pills = List.of(btnTous, btnDebutant, btnInter, btnAvance, btnCertif);
 pills.forEach(b -> b.getStyleClass().setAll("pill-inactive"));
 btnTous.getStyleClass().setAll("pill-active");
 }

 // -----------------------------------------------------------------------
 // Delete (from card)
 // -----------------------------------------------------------------------

 // -----------------------------------------------------------------------
 // PDF Export
 // -----------------------------------------------------------------------

 /**
 * Exports all completed courses for the current user to a temporary PDF
 * and opens it directly in the system's default PDF viewer.
 */
 @FXML
 public void exportCompletedCoursesPdf() {
 // 1. Retrieve completed progressions for this user
 List<Progression> allProgressions = progressionService.trouverParUtilisateur(currentUserId);
 List<Progression> completedProgressions = allProgressions.stream()
 .filter(p -> p.getEtat() == EtatProgression.COMPLETE || p.getEtat() == EtatProgression.CERTIFIE)
 .collect(Collectors.toList());

 if (completedProgressions.isEmpty()) {
 AlertUtils.showError("Export PDF", "Vous n'avez aucun cours complété à exporter.");
 return;
 }

 // 2. Resolve the matching Cours objects
 List<Cours> completedCourses = new ArrayList<>();
 List<Progression> matchedProgressions = new ArrayList<>();
 for (Progression prog : completedProgressions) {
 allCours.stream()
 .filter(c -> c.getId() == prog.getCoursId())
 .findFirst()
 .ifPresent(c -> {
 completedCourses.add(c);
 matchedProgressions.add(prog);
 });
 }

 if (completedCourses.isEmpty()) {
 AlertUtils.showError("Export PDF", "Aucun cours complété trouvé dans le catalogue actuel.");
 return;
 }

 // 3. Generate PDF bytes and show in-app preview
 try {
 byte[] pdfBytes = pdfExportService.exportCompletedCoursesToBytes(completedCourses, matchedProgressions);

 javafx.scene.Scene scene = coursGrid.getScene();
 if (scene != null) {
 util.PDFPreviewPopup.showInScene(pdfBytes, "Cours Complétés",
 "cours_completes.pdf", scene);
 } else {
 AlertUtils.showError("Erreur", "Impossible d'afficher l'aperçu PDF.");
 }
 } catch (IOException ex) {
 ex.printStackTrace();
 AlertUtils.showError("Erreur", "Impossible de générer le PDF : " + ex.getMessage());
 }
 }

 private void handleDeleteCours(Cours cours) {
 boolean confirm = AlertUtils.showConfirmation(
 "Suppression",
 "Supprimer le cours \"" + cours.getTitre() + "\" ?");
 if (confirm) {
 boolean ok = coursService.supprimerCours(cours.getId());
 if (ok) {
 allCours.remove(cours);
 renderCards(allCours);
 AlertUtils.showSuccess("Cours supprimé avec succès !");
 } else {
 AlertUtils.showError("Erreur", "Impossible de supprimer le cours.");
 }
 }
 }



}