package controllers.apprentissage;

import services.apprentissage.BadgeService;
import services.apprentissage.ProgressionService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Set;

/**
 * Controller pour le Front Office.
 * Gère la navigation entre les vues front office (Cours, Badges, Progression, Apprentissage).
 */
public class FrontOfficeController {

 @FXML private StackPane foContentArea;
 @FXML private HBox navBar;
 @FXML private Button btnFOCours;
 @FXML private Button btnFOBadges;
 @FXML private Button btnFOProgression;
 @FXML private Button btnFOApprentissage;
 @FXML private Button btnBackOffice;

 private Button activeTab;

 // ── Services ──────────────────────────────────────────────────────────────
 private final ProgressionService progressionService = new ProgressionService();
 private final BadgeService badgeService = new BadgeService();

 // ── Current user from session ────────────────────────────────────────────
 private int currentUserId = 1;

 private int getCurrentUserId() {
 models.User u = services.SessionService.getInstance().getCurrentUser();
 return (u != null) ? u.getId() : 1;
 }

 // ── Init ──────────────────────────────────────────────────────────────────

 @FXML
 public void initialize() {
 showCours();
 }

 // ── Navigation ────────────────────────────────────────────────────────────

 @FXML
 public void showCours() {
 loadView("/views/apprentissage/front-office-cours-view.fxml");
 setActiveTab(btnFOCours);
 }

 @FXML
 public void showBadges() {
 try {
 FXMLLoader loader = new FXMLLoader(
 getClass().getResource("/views/apprentissage/front-office-badges-view.fxml"));
 Parent view = loader.load();

 FrontOfficeBadgesController ctrl = loader.getController();
 ctrl.setViewContainer(foContentArea);

 // Compute user data and inject BEFORE the view is shown
 badgeService.attribuerBonusXPBadges(currentUserId);
 int xp = progressionService.getTotalXP(currentUserId);
 Set<Integer> ids = badgeService.getUnlockedBadgeIds(currentUserId);
 ctrl.refreshWithUserData(xp, ids);

 animateAndShow(view);
 setActiveTab(btnFOBadges);

 } catch (IOException e) {
 System.err.println("x Erreur lors du chargement des badges");
 e.printStackTrace();
 }
 }

 @FXML
 public void showProgression() {
 try {
 FXMLLoader loader = new FXMLLoader(
 getClass().getResource("/views/apprentissage/front-office-progression-view.fxml"));
 Parent view = loader.load();

 FrontOfficeProgressionController ctrl = loader.getController();
 ctrl.setUserId(currentUserId);
 badgeService.attribuerBonusXPBadges(currentUserId);
 ctrl.setViewContainer(foContentArea);

 animateAndShow(view);
 setActiveTab(btnFOProgression);

 } catch (IOException e) {
 System.err.println("x Erreur lors du chargement de la progression");
 e.printStackTrace();
 }
 }

 @FXML
 public void showApprentissage() {
 loadView("/views/apprentissage/ApprentissageView.fxml");
 setActiveTab(btnFOApprentissage);
 }

 @FXML
 public void switchToBackOffice() {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/apprentissage/ApprentissageView.fxml"));
 Parent root = loader.load();
 foContentArea.getScene().setRoot(root);
 } catch (IOException e) {
 System.err.println("x Erreur lors du basculement vers le Back Office");
 e.printStackTrace();
 }
 }
 @FXML
 private void goToHome() {
 tools.NavigationHelper.goHome(foContentArea);
 }
 // ── Helpers ───────────────────────────────────────────────────────────────

 /**
 * Generic view loader for views that need no data injection.
 */
 private void loadView(String fxmlPath) {
 try {
 FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
 Parent view = loader.load();
 animateAndShow(view);
 } catch (IOException e) {
 System.err.println("x Erreur lors du chargement de la vue : " + fxmlPath);
 e.printStackTrace();
 }
 }

 /**
 * Applies the fade + slide-up animation and adds the view to the content area.
 */
 private void animateAndShow(Parent view) {
 view.setOpacity(0);
 view.setTranslateY(15);

 foContentArea.getChildren().clear();
 foContentArea.getChildren().add(view);

 FadeTransition fadeIn = new FadeTransition(Duration.millis(250), view);
 fadeIn.setFromValue(0);
 fadeIn.setToValue(1);

 TranslateTransition slideUp = new TranslateTransition(Duration.millis(250), view);
 slideUp.setFromY(15);
 slideUp.setToY(0);

 new ParallelTransition(fadeIn, slideUp).play();
 }

 private void setActiveTab(Button tab) {
 if (activeTab != null) {
 activeTab.getStyleClass().remove("fo-tab-active");
 }
 tab.getStyleClass().add("fo-tab-active");
 activeTab = tab;
 }
}