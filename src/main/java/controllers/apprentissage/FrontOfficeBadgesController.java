package controllers.apprentissage;

import models.apprentissage.Badge;
import services.apprentissage.BadgeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Front-office controller for the badges view.
 * Unlocked badges → full colour with coloured border.
 * Locked badges → greyscale white card with lock icon.
 *
 * Call {@link #setUserXP(int)} and {@link #setUnlockedBadgeIds(Set)}
 * after loading the FXML to supply the current user context.
 */
public class FrontOfficeBadgesController {

 // ── FXML ────────────────────────────────────────────────────────────────
 @FXML private Label lblXPValue;
 @FXML private ProgressBar progressXP;
 @FXML private Label lblNextBadge;
 @FXML private Label lblUnlockedCount;
 @FXML private Label lblLockedCount;
 @FXML private Label lblBonusXP;
 @FXML private Label lblResultCount;

 @FXML private Button pillTous;
 @FXML private Button pillDebloque;
 @FXML private Button pillVerrouille;

 @FXML private ComboBox<String> cboRarete;
 @FXML private ComboBox<String> cboCategorie;

 @FXML private FlowPane badgeGrid;
 @FXML private VBox emptyState;
 private Pane viewContainer;


 // ── State ────────────────────────────────────────────────────────────────
 private final BadgeService badgeService = new BadgeService();
 private List<Badge> allBadges;
 private Set<Integer> unlockedIds = Set.of();
 private int userXP = 0;
 private String activeFilter = "TOUS";

 // ── Rarity accent colours ────────────────────────────────────────────────
 private static final String COLOR_COMMUN = "#6b7280";
 private static final String COLOR_RARE = "#3498db";
 private static final String COLOR_EPIQUE = "#9b59b6";
 private static final String COLOR_LEGENDAIRE = "#f39c12";

 // ── Public API ────────────────────────────────────────────────────────────

 public void setUserXP(int xp) {
 this.userXP = xp;
 System.out.println("DEBUG setUserXP called with: " + xp);
 }

 public void setUnlockedBadgeIds(Set<Integer> ids) {
 this.unlockedIds = ids != null ? ids : Set.of();
 }

 // ── Initialisation ────────────────────────────────────────────────────────

 @FXML
 public void initialize() {
 setupComboBoxes();
 allBadges = badgeService.trouverTous();
 }

 private void setupComboBoxes() {
 cboRarete.setItems(FXCollections.observableArrayList(
 "Toutes", "COMMUN", "RARE", "EPIQUE", "LEGENDAIRE"));
 cboRarete.setValue("Toutes");
 cboRarete.setOnAction(e -> applyFilters());

 List<String> cats = badgeService.trouverTous().stream()
 .map(Badge::getCategorie)
 .filter(c -> c != null && !c.isBlank())
 .distinct().sorted()
 .collect(Collectors.toList());
 cats.add(0, "Toutes");
 cboCategorie.setItems(FXCollections.observableArrayList(cats));
 cboCategorie.setValue("Toutes");
 cboCategorie.setOnAction(e -> applyFilters());
 }

 // ── Filters ───────────────────────────────────────────────────────────────

 @FXML
 public void filterBadges(javafx.event.ActionEvent event) {
 Button clicked = (Button) event.getSource();
 activeFilter = (String) clicked.getUserData();

 for (Button b : List.of(pillTous, pillDebloque, pillVerrouille)) {
 b.getStyleClass().setAll("pill-inactive");
 }
 clicked.getStyleClass().setAll("pill-active");

 applyFilters();
 }

 private void applyFilters() {
 String rareteFilter = cboRarete.getValue();
 String categorieFilter = cboCategorie.getValue();

 List<Badge> filtered = allBadges.stream()
 .filter(b -> {
 if ("DEBLOQUE".equals(activeFilter) && !isUnlocked(b)) return false;
 if ("VERROUILLE".equals(activeFilter) && isUnlocked(b)) return false;
 if (!"Toutes".equals(rareteFilter) &&
 !b.getRarete().name().equals(rareteFilter)) return false;
 if (!"Toutes".equals(categorieFilter) &&
 !categorieFilter.equals(b.getCategorie())) return false;
 return true;
 })
 .collect(Collectors.toList());

 renderGrid(filtered);
 }

 // ── Stats ─────────────────────────────────────────────────────────────────

 private void refreshStats() {
 long unlocked = allBadges.stream().filter(this::isUnlocked).count();
 long locked = allBadges.size() - unlocked;
 int bonusTotal = allBadges.stream()
 .filter(this::isUnlocked)
 .mapToInt(Badge::getPointsBonus)
 .sum();

 lblUnlockedCount.setText(String.valueOf(unlocked));
 lblLockedCount.setText(String.valueOf(locked));
 lblBonusXP.setText(bonusTotal + " XP");
 lblXPValue.setText(userXP + " XP");
 lblResultCount.setText(allBadges.size() + " badges");

 // Progress bar toward next locked badge
 allBadges.stream()
 .filter(b -> !isUnlocked(b) && b.getPointsRequis() > 0)
 .sorted((a, b) -> Integer.compare(a.getPointsRequis(), b.getPointsRequis()))
 .findFirst()
 .ifPresentOrElse(next -> {
 double progress = Math.min(1.0, (double) userXP / next.getPointsRequis());
 progressXP.setProgress(progress);
 lblNextBadge.setText("Prochain : " + next.getNom()
 + " — " + userXP + " / " + next.getPointsRequis() + " XP");
 }, () -> {
 progressXP.setProgress(1.0);
 lblNextBadge.setText(" Tous les badges sont débloqués !");
 });
 }

 // ── Grid rendering ────────────────────────────────────────────────────────

 private void renderGrid(List<Badge> list) {
 badgeGrid.getChildren().clear();

 boolean isEmpty = list == null || list.isEmpty();
 emptyState.setVisible(isEmpty);
 emptyState.setManaged(isEmpty);

 if (!isEmpty) {
 lblResultCount.setText(list.size() + " badge" + (list.size() > 1 ? "s" : ""));
 // Unlocked first, then locked
 list.stream()
 .sorted((a, b) -> Boolean.compare(!isUnlocked(a), !isUnlocked(b)))
 .forEach(b -> badgeGrid.getChildren().add(buildCard(b)));
 } else {
 lblResultCount.setText("0 badge");
 }
 }

 /**
 * Builds a badge card matching the courses light theme.
 *
 * Unlocked → white card, coloured left border + accent, full-colour icon.
 * Locked → white card, grey border, greyscale icon + , muted text.
 */
 private VBox buildCard(Badge badge) {
 boolean unlocked = isUnlocked(badge);
 String rarityColor = getRarityColor(badge.getRarete());

 VBox card = new VBox(10);
 card.setPrefWidth(200);
 card.setMaxWidth(200);
 card.setAlignment(javafx.geometry.Pos.CENTER);

 String baseStyle =
 "-fx-background-color: white;" +
 "-fx-background-radius: 12;" +
 "-fx-padding: 18 14;" +
 "-fx-cursor: hand;";

 if (unlocked) {
 card.setStyle(
 baseStyle +
 "-fx-border-color: " + rarityColor + ";" +
 "-fx-border-width: 0 0 0 4;" + // left accent border only
 "-fx-border-radius: 12;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 10, 0, 0, 3);"
 );
 } else {
 card.setStyle(
 baseStyle +
 "-fx-border-color: #e0e0e0;" +
 "-fx-border-width: 1;" +
 "-fx-border-radius: 12;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);" +
 "-fx-opacity: 0.72;"
 );
 }

 // ── Icon ──────────────────────────────────────────────────────────────
 // Use badge name initial as icon — DB icons are emojis that JavaFX can't render
 String iconText;
 if (badge.getNom() != null && !badge.getNom().isBlank()) {
 iconText = badge.getNom().substring(0, 1).toUpperCase();
 } else {
 iconText = "B";
 }
 Label icon = new Label(unlocked ? iconText : "?");
 icon.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: " + (unlocked ? getRarityColor(badge.getRarete()) : "#ccc") + ";");

 // ── Rarity pill ───────────────────────────────────────────────────────
 Label rarityLabel = new Label(badge.getRarete().name());
 rarityLabel.setStyle(
 "-fx-background-color: " + (unlocked ? rarityColor + "22" : "#f0f0f0") + ";" +
 "-fx-text-fill: " + (unlocked ? rarityColor : "#aaaaaa") + ";" +
 "-fx-font-size: 10px; -fx-font-weight: bold;" +
 "-fx-padding: 2 8; -fx-background-radius: 20;"
 );

 // ── Name ──────────────────────────────────────────────────────────────
 Label name = new Label(badge.getNom());
 name.setStyle(
 "-fx-font-size: 13px; -fx-font-weight: bold; -fx-wrap-text: true;" +
 "-fx-text-fill: " + (unlocked ? "#2c3e50" : "#aaaaaa") + ";" +
 "-fx-text-alignment: center;"
 );
 name.setWrapText(true);
 name.setMaxWidth(172);

 // ── Description ───────────────────────────────────────────────────────
 String descText = badge.getDescription() != null ? badge.getDescription() : "";
 if (descText.length() > 60) descText = descText.substring(0, 60) + "…";
 Label desc = new Label(descText);
 desc.setStyle(
 "-fx-font-size: 11px; -fx-wrap-text: true; -fx-text-alignment: center;" +
 "-fx-text-fill: " + (unlocked ? "#7f8c8d" : "#bbbbbb") + ";"
 );
 desc.setWrapText(true);
 desc.setMaxWidth(172);

 // ── Footer ────────────────────────────────────────────────────────────
 Label footer;
 if (unlocked) {
 footer = new Label("+ " + badge.getPointsBonus() + " XP");
 footer.setStyle(
 "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f39c12;" +
 "-fx-background-color: #fff8e1; -fx-padding: 3 10; -fx-background-radius: 20;"
 );
 } else {
 String cond = badge.getPointsRequis() > 0
 ? badge.getPointsRequis() + " XP requis"
 : (badge.getCondition() != null ? badge.getCondition() : "---");
 footer = new Label(cond);
 footer.setStyle(
 "-fx-font-size: 10px; -fx-text-fill: #bbbbbb; -fx-wrap-text: true;" +
 "-fx-text-alignment: center;"
 );
 footer.setWrapText(true);
 footer.setMaxWidth(172);
 }

 card.getChildren().addAll(icon, rarityLabel, name, desc, footer);

 // ── Hover (unlocked only) ─────────────────────────────────────────────
 if (unlocked) {
 String hoverStyle =
 "-fx-background-color: white;" +
 "-fx-background-radius: 12;" +
 "-fx-padding: 18 14;" +
 "-fx-cursor: hand;" +
 "-fx-border-color: " + rarityColor + ";" +
 "-fx-border-width: 0 0 0 4;" +
 "-fx-border-radius: 12;" +
 "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 14, 0, 0, 5);";
 String normalStyle = card.getStyle();
 card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
 card.setOnMouseExited(e -> card.setStyle(normalStyle));
 }

 return card;
 }

 // ── Helpers ───────────────────────────────────────────────────────────────

 private boolean isUnlocked(Badge badge) {
 return unlockedIds.contains(badge.getId()) ||
 (badge.getPointsRequis() > 0 && userXP >= badge.getPointsRequis());
 }

 private String getRarityColor(Badge.Rarete rarete) {
 return switch (rarete) {
 case COMMUN -> COLOR_COMMUN;
 case RARE -> COLOR_RARE;
 case EPIQUE -> COLOR_EPIQUE;
 case LEGENDAIRE -> COLOR_LEGENDAIRE;
 };
 }

 public void setViewContainer(Pane viewContainer) {
 this.viewContainer = viewContainer;
 }

 /**
 * Called after setUserXP() and setUnlockedBadgeIds() to re-render
 * with the actual user data. Needed because initialize() runs before
 * the data is injected.
 */
 public void refreshWithUserData(int xp, Set<Integer> ids) {
 this.userXP = xp;
 this.unlockedIds = ids != null ? ids : Set.of();
 System.out.println("DEBUG refreshWithUserData xp="+ xp + " ids="+ ids);
 refreshStats();
 renderGrid(allBadges);
 }
}