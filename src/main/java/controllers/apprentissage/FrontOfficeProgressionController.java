package controllers.apprentissage;

import models.apprentissage.Badge;
import models.apprentissage.EtatProgression;
import models.apprentissage.Progression;
import services.apprentissage.BadgeService;
import services.apprentissage.ProgressionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Front-office progression controller.
 *
 * Loads the current user's progressions, computes their total XP,
 * derives which badges are unlocked, and renders everything.
 *
 * Call {@link #setUserId(int)} right after loading the FXML.
 * Optionally call {@link #setViewContainer(Pane)} so "Voir tous les badges"
 * can navigate to the badges view.
 */
public class FrontOfficeProgressionController {

 // ── FXML ────────────────────────────────────────────────────────────────
 @FXML private Label lblNiveauActuel;
 @FXML private Label lblXPTotal;
 @FXML private ProgressBar progressNiveau;
 @FXML private Label lblXPProchain;
 @FXML private Label lblCoursTotal;
 @FXML private Label lblCoursCompletes;
 @FXML private Label lblBadgesDebloquesCount;
 @FXML private Label lblResultCount;

 @FXML private Button pillTous;
 @FXML private Button pillEnCours;
 @FXML private Button pillCompletes;
 @FXML private Button pillNonCommence;

 @FXML private FlowPane progressionGrid;
 @FXML private VBox emptyState;

 @FXML private FlowPane badgesRecentsGrid;
 @FXML private VBox noBadgesState;

 // ── State ────────────────────────────────────────────────────────────────
 private final ProgressionService progressionService = new ProgressionService();
 private final BadgeService badgeService = new BadgeService();

 private int userId = 1; // TODO: inject logged-in user
 private List<Progression> allProgressions;
 private List<Badge> allBadges;
 private int totalXP = 0;
 private Set<Integer> unlockedIds = Set.of();
 private String activeFilter = "TOUS";
 private Pane viewContainer;

 // ── Public API ────────────────────────────────────────────────────────────

 public void setUserId(int userId) {
 this.userId = userId;
 }

 public void setViewContainer(Pane viewContainer) {
 this.viewContainer = viewContainer;
 }

 // ── Init ──────────────────────────────────────────────────────────────────

 @FXML
 public void initialize() {
 allProgressions = progressionService.trouverParUtilisateur(userId);
 allBadges = badgeService.trouverTous();

 // Delegate XP + level computation to ProgressionService
 totalXP = progressionService.getTotalXP(userId);
 unlockedIds = badgeService.getUnlockedBadgeIds(userId); // uses BadgeService logic

 refreshHeader();
 renderProgressionGrid(allProgressions);
 renderBadgesRecents();
 }

 // ── Badge unlock logic ────────────────────────────────────────────────────

 /**
 * Determines which badges the user has unlocked based on:
 * 1. Total XP >= pointsRequis
 * 2. Number of completed courses >= coursRequis
 * 3. Special condition types (PREMIER_COURS, CERTIFICATION, etc.)
 */
 private Set<Integer> computeUnlockedBadges() {
 return badgeService.getUnlockedBadgeIds(userId);
 }

 private boolean isBadgeUnlocked(Badge badge, long completedCount, long certifiedCount,
 boolean hasStarted, boolean hasCompletedFirst) {
 // XP threshold
 if (badge.getPointsRequis() > 0 && totalXP < badge.getPointsRequis()) {
 return false;
 }

 // Course completion threshold
 if (badge.getCoursRequis() > 0 && completedCount < badge.getCoursRequis()) {
 return false;
 }

 // Condition-type based rules
 String condType = badge.getConditionType();
 if (condType != null) {
 return switch (condType) {
 case Badge.CONDITION_XP_TOTAL ->
 totalXP >= badge.getConditionValeur();
 case Badge.CONDITION_COURS_COMPLETE ->
 completedCount >= badge.getConditionValeur();
 case Badge.CONDITION_PREMIER_COURS ->
 hasCompletedFirst;
 case Badge.CONDITION_CERTIFICATION ->
 certifiedCount >= badge.getConditionValeur();
 case Badge.CONDITION_NIVEAU_ATTEINT ->
 getCurrentNiveau() >= badge.getConditionValeur();
 default -> true;
 };
 }

 return true;
 }

 // ── Header / stats ────────────────────────────────────────────────────────

 private void refreshHeader() {
 // Level from total XP
 int niveau = computeNiveauFromXP(totalXP);
 lblNiveauActuel.setText("Niveau " + niveau);
 lblXPTotal.setText(totalXP + " XP");

 // Progress toward next level
 double progressionNiveau = computeProgressionVersProchainNiveau(totalXP, niveau);
 progressNiveau.setProgress(progressionNiveau / 100.0);

 int xpProchain = computeXPProchainNiveau(totalXP, niveau);
 lblXPProchain.setText(xpProchain > 0
 ? xpProchain + " XP pour le niveau " + (niveau + 1)
 : " Niveau maximum atteint !");

 // Stats
 long completes = allProgressions.stream().filter(Progression::estComplete).count();
 lblCoursTotal.setText(String.valueOf(allProgressions.size()));
 lblCoursCompletes.setText(String.valueOf(completes));
 lblBadgesDebloquesCount.setText(String.valueOf(unlockedIds.size()));
 }

 private int getCurrentNiveau() {
 return computeNiveauFromXP(totalXP);
 }

 private int computeNiveauFromXP(int xp) {
 int[] seuils = Progression.SEUILS_NIVEAU;
 for (int i = seuils.length - 1; i >= 0; i--) {
 if (xp >= seuils[i]) return i + 1;
 }
 return 1;
 }

 private double computeProgressionVersProchainNiveau(int xp, int niveau) {
 int[] seuils = Progression.SEUILS_NIVEAU;
 if (niveau >= seuils.length) return 100.0;
 int seuilActuel = seuils[niveau - 1];
 int seuilProchain = seuils[niveau];
 return ((xp - seuilActuel) * 100.0) / (seuilProchain - seuilActuel);
 }

 private int computeXPProchainNiveau(int xp, int niveau) {
 int[] seuils = Progression.SEUILS_NIVEAU;
 if (niveau >= seuils.length) return 0;
 return seuils[niveau] - xp;
 }

 // ── Progression cards ─────────────────────────────────────────────────────

 @FXML
 public void filterProgressions(javafx.event.ActionEvent event) {
 Button clicked = (Button) event.getSource();
 activeFilter = (String) clicked.getUserData();

 for (Button b : List.of(pillTous, pillEnCours, pillCompletes, pillNonCommence)) {
 b.getStyleClass().setAll("pill-inactive");
 }
 clicked.getStyleClass().setAll("pill-active");

 List<Progression> filtered = allProgressions.stream()
 .filter(p -> switch (activeFilter) {
 case "EN_COURS" -> p.getEtat() == EtatProgression.EN_COURS;
 case "COMPLETE" -> p.getEtat() == EtatProgression.COMPLETE
 || p.getEtat() == EtatProgression.CERTIFIE;
 case "NON_COMMENCE" -> p.getEtat() == EtatProgression.NON_COMMENCE;
 default -> true;
 })
 .collect(Collectors.toList());

 renderProgressionGrid(filtered);
 }

 private void renderProgressionGrid(List<Progression> list) {
 progressionGrid.getChildren().clear();
 boolean isEmpty = list == null || list.isEmpty();
 emptyState.setVisible(isEmpty);
 emptyState.setManaged(isEmpty);

 if (!isEmpty) {
 lblResultCount.setText(list.size() + " cours");
 list.forEach(p -> progressionGrid.getChildren().add(buildProgressionCard(p)));
 } else {
 lblResultCount.setText("0 cours");
 }
 }

 private VBox buildProgressionCard(Progression prog) {
 boolean complete = prog.estComplete();

 String accentColor = switch (prog.getEtat()) {
 case COMPLETE, CERTIFIE -> "#27ae60";
 case EN_COURS -> "#3498db";
 default -> "#bdc3c7";
 };

 VBox card = new VBox(10);
 card.setPrefWidth(240);
 card.setMaxWidth(240);
 card.setStyle(
 "-fx-background-color: white;" +
 "-fx-background-radius: 12;" +
 "-fx-padding: 16 14;" +
 "-fx-border-color: " + accentColor + ";" +
 "-fx-border-width: 0 0 0 4;" +
 "-fx-border-radius: 12;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);" +
 "-fx-cursor: hand;"
 );

 // Title
 String titre = prog.getCoursTitre() != null
 ? prog.getCoursTitre() : "Cours #" + prog.getCoursId();
 Label lblTitre = new Label(titre);
 lblTitre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
 lblTitre.setWrapText(true);
 lblTitre.setMaxWidth(212);

 // Status badge
 String statusText = switch (prog.getEtat()) {
 case COMPLETE -> "Complete";
 case CERTIFIE -> "Certifie";
 case EN_COURS -> "En cours";
 case NON_COMMENCE -> "Non commence";
 };
 Label statusLabel = new Label(statusText);
 statusLabel.setStyle(
 "-fx-background-color: " + accentColor + "22;" +
 "-fx-text-fill: " + accentColor + ";" +
 "-fx-font-size: 10px; -fx-font-weight: bold;" +
 "-fx-padding: 2 8; -fx-background-radius: 20;"
 );

 // Progress bar
 ProgressBar bar = new ProgressBar(prog.getPourcentage() / 100.0);
 bar.setMaxWidth(Double.MAX_VALUE);
 bar.setStyle("-fx-accent: " + accentColor + "; -fx-pref-height: 8;");

 // Progress label
 Label pctLabel = new Label(prog.getPourcentageFormate());
 pctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

 // XP + Level row
 HBox footer = new HBox(8);
 footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
 Label xpLabel = new Label(prog.getPointsXP() + " XP");
 xpLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
 Region fSpacer = new Region();
 HBox.setHgrow(fSpacer, Priority.ALWAYS);
 Label niveauLabel = new Label("Niv. " + prog.getNiveau());
 niveauLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
 footer.getChildren().addAll(xpLabel, fSpacer, niveauLabel);

 card.getChildren().addAll(statusLabel, lblTitre, bar, pctLabel, footer);
 return card;
 }

 // ── Recent badges strip ───────────────────────────────────────────────────

 private void renderBadgesRecents() {
 badgesRecentsGrid.getChildren().clear();

 List<Badge> unlocked = allBadges.stream()
 .filter(b -> unlockedIds.contains(b.getId()))
 .limit(6)
 .collect(Collectors.toList());

 boolean none = unlocked.isEmpty();
 noBadgesState.setVisible(none);
 noBadgesState.setManaged(none);

 unlocked.forEach(b -> badgesRecentsGrid.getChildren().add(buildMiniCard(b)));
 }

 private HBox buildMiniCard(Badge badge) {
 String rarityColor = switch (badge.getRarete()) {
 case COMMUN -> "#6b7280";
 case RARE -> "#3498db";
 case EPIQUE -> "#9b59b6";
 case LEGENDAIRE -> "#f39c12";
 };

 HBox card = new HBox(10);
 card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
 card.setStyle(
 "-fx-background-color: white;" +
 "-fx-background-radius: 10;" +
 "-fx-padding: 10 14;" +
 "-fx-border-color: " + rarityColor + ";" +
 "-fx-border-width: 0 0 0 3;" +
 "-fx-border-radius: 10;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);"
 );

 String icon = badge.getIcone() != null && !badge.getIcone().isBlank()
 ? badge.getIcone() : "[B]";
 Label iconLabel = new Label(icon);
 iconLabel.setStyle("-fx-font-size: 22px;");

 VBox info = new VBox(2);
 Label nameLabel = new Label(badge.getNom());
 nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
 Label rarityLabel = new Label(badge.getRarete().name());
 rarityLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + rarityColor + "; -fx-font-weight: bold;");
 info.getChildren().addAll(nameLabel, rarityLabel);

 Label xpLabel = new Label("+" + badge.getPointsBonus() + " XP");
 xpLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");

 Region spacer = new Region();
 HBox.setHgrow(spacer, Priority.ALWAYS);

 card.getChildren().addAll(iconLabel, info, spacer, xpLabel);
 return card;
 }

 // ── Navigation ────────────────────────────────────────────────────────────

 @FXML
 public void goToBadges() {
 if (viewContainer == null) return;
 try {
 FXMLLoader loader = new FXMLLoader(
 getClass().getResource("/views/apprentissage/front-office-badges-view.fxml"));
 Parent badgesView = loader.load(); // initialize() runs here, but doesn't render

 FrontOfficeBadgesController ctrl = loader.getController();
 ctrl.setViewContainer(viewContainer);
 ctrl.refreshWithUserData(totalXP, unlockedIds); // NOW render with real data

 viewContainer.getChildren().setAll(badgesView);
 } catch (IOException e) {
 e.printStackTrace();
 }
 }
}