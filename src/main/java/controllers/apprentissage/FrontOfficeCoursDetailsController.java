package controllers.apprentissage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import models.apprentissage.Comment;
import models.apprentissage.Cours;
import models.apprentissage.EtatProgression;
import models.apprentissage.Progression;
import models.apprentissage.TypeCours;
import services.apprentissage.CommentService;
import services.apprentissage.CoursService;
import services.apprentissage.ProgressionService;
import util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FrontOfficeCoursDetailsController {

 // Hero banner labels
 @FXML private Label lblTypeIcon;
 @FXML private Label lblTitre;
 @FXML private Label lblNiveau;
 @FXML private Label lblType;
 @FXML private Label lblCertifBadge;

 // Right-column stats
 @FXML private Label lblXP;
 @FXML private Label lblDuree;
 @FXML private Label lblCertification;
 @FXML private Button btnDelete;

 // Description body
 @FXML private Label lblDescription;
 @FXML private Label lblDocument;
 @FXML private Button btnOpenDocument;
 @FXML private Label lblDocumentIcon;
 @FXML private Label lblDocumentLabel;
 @FXML private VBox vboxQRCode; // container in the detail FXML
 @FXML private javafx.scene.image.ImageView imgQRCode;
 @FXML private Button btnCompleterCours;
 @FXML private Label lblCompletionStatus;

 // Comment UI elements
 @FXML private TextArea txtCommentInput;
 @FXML private VBox vboxCommentsList;

 private final ProgressionService progressionService = new ProgressionService();
 private final CommentService commentService = new CommentService();
 private int currentUserId = 1; // replace with session user when available
 private static final String STATIC_USER_NAME = "Utilisateur"; // static user name until auth is integrated

 public void setCurrentUserId(int userId) {
 this.currentUserId = userId;
 }

 private final CoursService coursService = new CoursService();

 /** The course being displayed. */
 private Cours cours;

 /**
 * Container used to navigate back (swap views).
 * Inject via {@link #setViewContainer(Pane, Runnable)}.
 */
 private Pane viewContainer;

 /**
 * Callback executed when the user navigates back to the list.
 * Typically {@code CoursListeUtilisateurController::reloadView}.
 */
 private Runnable onBack;

 // -----------------------------------------------------------------------
 // Public API (called by the list controller after loading this FXML)
 // -----------------------------------------------------------------------

 /**
 * Populates the view with the selected course data.
 * Must be called before the view is shown.
 */
 public void setCours(Cours cours) {
 this.cours = cours;
 populateView();
 loadComments();
 }

 /**
 * Injects navigation context so the "back" button can restore the previous view.
 *
 * @param viewContainer the parent pane whose children will be swapped
 * @param onBack callback to refresh the list after a deletion
 */
 public void setViewContainer(Pane viewContainer, Runnable onBack) {
 this.viewContainer = viewContainer;
 this.onBack = onBack;
 }

 // -----------------------------------------------------------------------
 // View population
 // -----------------------------------------------------------------------

 private void populateView() {
 if (cours == null) return;

 // Hero
 lblTypeIcon.setText(cours.getTypeIcon());
 lblTitre.setText(cours.getTitre());
 lblNiveau.setText(cours.getNiveauIcon() + " " + cours.getNiveau().getDisplayName());
 lblType.setText(cours.getTypeIcon() + " " + cours.getType().getDisplayName());

 if (cours.isCertification()) {
 lblCertifBadge.setVisible(true);
 lblCertifBadge.setManaged(true);
 }

 // Description
 String desc = cours.getDescription() != null && !cours.getDescription().isBlank()
 ? cours.getDescription()
 : "Aucune description disponible pour ce cours.";
 lblDescription.setText(desc);

 // Right stats column
 lblXP.setText(cours.getPointsXP() + " XP");

 int duree = cours.getDureeMinutes();
 lblDuree.setText(duree > 0 ? duree + " min" : "— min");

 if (cours.isCertification()) {
 lblCertification.setText(" Certifiant");
 lblCertification.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
 } else {
 lblCertification.setText("Non certifiant");
 lblCertification.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
 }

 // Document attachment
 String docPath = cours.getDocumentPath().orElse(null);
 String videoUrl = cours.getVideoUrl().orElse(null);

 if (videoUrl != null && !videoUrl.isBlank()) {
 lblDocumentIcon.setText("");
 lblDocumentLabel.setText("Vidéo liée");
 lblDocument.setText(cours.getVideoUrl().orElse(""));
 lblDocument.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
 btnOpenDocument.setVisible(true);
 btnOpenDocument.setManaged(true);
 btnOpenDocument.setText("Ouvrir la video");
 } else if (docPath != null && !docPath.isBlank()) {
 lblDocumentIcon.setText("");
 lblDocumentLabel.setText("Pièce jointe");
 lblDocument.setText(new java.io.File(docPath).getName());
 lblDocument.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
 btnOpenDocument.setVisible(true);
 btnOpenDocument.setManaged(true);
 btnOpenDocument.setText(" Ouvrir le document");
 } else {
 lblDocumentIcon.setText("");
 lblDocumentLabel.setText("Pièce jointe");
 lblDocument.setText("Aucun fichier joint");
 lblDocument.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
 }

 // Show QR code if this is a quiz type
 if (cours.getType() == TypeCours.QUIZ) {
 String quizUrl = cours.getVideoUrl().orElse(null);
 if (quizUrl != null && !quizUrl.isBlank()) {
 showQRCode(quizUrl);
 }
 } else {
 vboxQRCode.setVisible(false);
 vboxQRCode.setManaged(false);
 }
 }

 // -----------------------------------------------------------------------
 // Actions
 // -----------------------------------------------------------------------

 /**
 * Navigates back to the course catalogue.
 */
 @FXML
 public void goBack() {
 if (viewContainer != null && onBack != null) {
 try {
 javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
 getClass().getResource("/views/apprentissage/FrontOfficeView.fxml"));
 javafx.scene.Parent listView = loader.load();

 FrontOfficeCoursController listCtrl = loader.getController();
 listCtrl.setViewContainer(viewContainer);

 viewContainer.getChildren().setAll(listView);
 onBack.run();
 } catch (java.io.IOException e) {
 e.printStackTrace();
 }
 } else {
 // Fallback: navigate via scene root
 try {
 javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
 getClass().getResource("/views/apprentissage/FrontOfficeView.fxml"));
 javafx.scene.Parent listView = loader.load();
 lblTitre.getScene().setRoot(listView);
 } catch (java.io.IOException e) {
 e.printStackTrace();
 }
 }
 }

 @FXML
 public void openDocument() {
 try {
 String videoUrl = cours.getVideoUrl()
 .filter(s -> !s.isBlank())
 .orElse(null);
 String docPath = cours.getDocumentPath()
 .filter(s -> !s.isBlank())
 .orElse(null);

 javafx.stage.Stage owner = (javafx.stage.Stage) lblTitre.getScene().getWindow();

 if (videoUrl != null) {
 // Strip any accidental "Optional[...]" wrapping just in case
 if (videoUrl.startsWith("Optional[")) {
 videoUrl = videoUrl.substring(9, videoUrl.length() - 1);
 }
 // Open video/URL inside the application
 DocumentViewerController.openUrl(videoUrl, cours.getTitre(), owner);
 revealCompletionButton();
 } else if (docPath != null) {
 java.io.File file = new java.io.File(docPath);
 if (!file.exists()) {
 AlertUtils.showError("Fichier introuvable", "Le fichier n'existe plus à :\n" + docPath);
 return;
 }
 // Open document inside the application
 DocumentViewerController.openFile(file, cours.getTitre(), owner);
 revealCompletionButton();
 }
 } catch (Exception e) {
 AlertUtils.showError("Erreur", "Impossible d'ouvrir : " + e.getMessage());
 }
 }

 /**
 * Placeholder action when the user starts a course.
 * Wire this up to your lesson/quiz flow when ready.
 */
 @FXML
 public void startCours() {
 AlertUtils.showSuccess("Lancement du cours : " + cours.getTitre());
 // TODO: navigate to the course content / first lesson
 }

 /**
 * Deletes the current course after confirmation, then goes back to the list.
 */
 @FXML
 public void deleteCours() {
 boolean confirm = AlertUtils.showConfirmation(
 "Suppression",
 "Voulez-vous vraiment supprimer le cours \"" + cours.getTitre() + "\" ?"
 );

 if (confirm) {
 boolean ok = coursService.supprimerCours(cours.getId());
 if (ok) {
 AlertUtils.showSuccess("Cours supprimé avec succès !");
 goBack(); // return to catalogue after deletion
 } else {
 AlertUtils.showError("Erreur", "Impossible de supprimer le cours.");
 }
 }
 }

 /**
 * Generates a QR code from the given URL and displays it in the detail view.
 */
 private void showQRCode(String url) {
 try {
 QRCodeWriter writer = new QRCodeWriter();
 BitMatrix bitMatrix =
 writer.encode(url, BarcodeFormat.QR_CODE, 180, 180);

 int width = bitMatrix.getWidth();
 int height = bitMatrix.getHeight();
 javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
 javafx.scene.image.PixelWriter pw = image.getPixelWriter();

 for (int x = 0; x < width; x++) {
 for (int y = 0; y < height; y++) {
 pw.setColor(x, y, bitMatrix.get(x, y)
 ? javafx.scene.paint.Color.BLACK
 : javafx.scene.paint.Color.WHITE);
 }
 }

 imgQRCode.setImage(image);
 vboxQRCode.setVisible(true);
 vboxQRCode.setManaged(true);

 } catch (WriterException e) {
 e.printStackTrace();
 }
 }

 private void revealCompletionButton() {
 // Check if already completed — don't show button if so
 progressionService.trouverParUtilisateurEtCours(currentUserId, cours.getId())
 .ifPresentOrElse(prog -> {
 if (prog.estComplete()) {
 lblCompletionStatus.setText("Vous avez deja complete ce cours !");
 lblCompletionStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
 lblCompletionStatus.setVisible(true);
 lblCompletionStatus.setManaged(true);
 } else {
 btnCompleterCours.setVisible(true);
 btnCompleterCours.setManaged(true);
 }
 }, () -> {
 // No progression exists yet — show the button anyway
 btnCompleterCours.setVisible(true);
 btnCompleterCours.setManaged(true);
 });
 }

 // -----------------------------------------------------------------------
 // Comment Actions
 // -----------------------------------------------------------------------

 /**
 * Adds a new comment to the current course.
 */
 @FXML
 public void ajouterCommentaire() {
 String contenu = txtCommentInput.getText();
 if (contenu == null || contenu.trim().isEmpty()) {
 AlertUtils.showWarning("Commentaire vide", "Veuillez écrire un commentaire avant d'envoyer.");
 return;
 }

 try {
 Comment comment = new Comment(cours.getId(), currentUserId, contenu.trim());

 // Get AI rating from Python script
 double rating = getAIRating(contenu.trim());
 comment.setRating(rating);

 commentService.ajouterCommentaire(comment);
 txtCommentInput.clear();
 loadComments();
 } catch (IllegalArgumentException e) {
 AlertUtils.showValidationError(e.getMessage());
 } catch (Exception e) {
 AlertUtils.showError("Erreur", "Impossible d'ajouter le commentaire : " + e.getMessage());
 }
 }

 /**
 * Loads and displays all comments for the current course.
 */
 private void loadComments() {
 if (cours == null || vboxCommentsList == null) return;

 vboxCommentsList.getChildren().clear();
 List<Comment> comments = commentService.trouverParCours(cours.getId());

 if (comments.isEmpty()) {
 Label noComments = new Label("Aucun commentaire pour ce cours. Soyez le premier à commenter !");
 noComments.setStyle("-fx-font-size: 12px; -fx-text-fill: #999; -fx-font-style: italic;");
 vboxCommentsList.getChildren().add(noComments);
 return;
 }

 for (Comment comment : comments) {
 vboxCommentsList.getChildren().add(createCommentCard(comment));
 }
 }

 /**
 * Creates a styled card for a single comment with edit/delete buttons.
 */
 private VBox createCommentCard(Comment comment) {
 VBox card = new VBox(6);
 card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12; -fx-background-radius: 8;"
 + "-fx-border-color: #e9ecef; -fx-border-radius: 8;");

 // Header: user name + date
 HBox header = new HBox(10);
 header.setAlignment(Pos.CENTER_LEFT);

 String displayName = comment.getUserName() != null ? comment.getUserName() : STATIC_USER_NAME;
 Label lblUser = new Label(displayName);
 lblUser.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

 Label lblDate = new Label("");
 if (comment.getCreatedAt() != null) {
 lblDate.setText(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
 }
 lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

 Region spacer = new Region();
 HBox.setHgrow(spacer, Priority.ALWAYS);

 header.getChildren().addAll(lblUser, lblDate, spacer);

 // Only show edit/delete if this comment belongs to the current user
 if (comment.getUserId() == currentUserId) {
 Button btnEdit = new Button("Modifier");
 btnEdit.setStyle("-fx-background-color: #eaf4fb; -fx-text-fill: #2980b9; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-size: 11px; -fx-background-radius: 6;");
 btnEdit.setOnAction(e -> editComment(comment));

 Button btnDelete = new Button("Supprimer");
 btnDelete.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-size: 11px; -fx-background-radius: 6;");
 btnDelete.setOnAction(e -> deleteComment(comment));

 header.getChildren().addAll(btnEdit, btnDelete);
 }

 // Content
 Label lblContent = new Label(comment.getContenu());
 lblContent.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-wrap-text: true;");
 lblContent.setWrapText(true);

 // Star rating display
 HBox starsBox = buildStarsDisplay(comment.getRating());

 card.getChildren().addAll(header, lblContent, starsBox);
 return card;
 }

 /**
 * Edit the given comment using a dialog.
 */
 private void editComment(Comment comment) {
 TextInputDialog dialog = new TextInputDialog(comment.getContenu());
 dialog.setTitle("Modifier le commentaire");
 dialog.setHeaderText(null);
 dialog.setContentText("Commentaire :");

 // Make the text field bigger
 dialog.getEditor().setPrefWidth(400);

 Optional<String> result = dialog.showAndWait();
 result.ifPresent(newContent -> {
 if (newContent.trim().isEmpty()) {
 AlertUtils.showWarning("Commentaire vide", "Le commentaire ne peut pas être vide.");
 return;
 }
 try {
 comment.setContenu(newContent.trim());
 // Re-rate the modified comment
 double newRating = getAIRating(newContent.trim());
 comment.setRating(newRating);
 boolean updated = commentService.modifierCommentaire(comment);
 if (updated) {
 loadComments();
 } else {
 AlertUtils.showError("Erreur", "Impossible de modifier le commentaire.");
 }
 } catch (IllegalArgumentException e) {
 AlertUtils.showValidationError(e.getMessage());
 }
 });
 }

 /**
 * Delete the given comment after confirmation.
 */
 private void deleteComment(Comment comment) {
 boolean confirm = AlertUtils.showConfirmation(
 "Supprimer le commentaire",
 "Voulez-vous vraiment supprimer ce commentaire ?"
 );
 if (confirm) {
 boolean deleted = commentService.supprimerCommentaire(comment.getId());
 if (deleted) {
 loadComments();
 } else {
 AlertUtils.showError("Erreur", "Impossible de supprimer le commentaire.");
 }
 }
 }

 // -----------------------------------------------------------------------
 // AI Rating Integration
 // -----------------------------------------------------------------------

 /**
 * Calls the Python AI rating script and returns the star rating.
 * Falls back to 3.0 if the script fails.
 */
 private double getAIRating(String commentText) {
 try {
 // Resolve the Python script path relative to the project
 // The scripts folder is at the project root (alongside src/ and pom.xml)
 Path projectDir = Paths.get(System.getProperty("user.dir"));
 Path ratingScript = projectDir.resolve("scripts/rate_comment.py").normalize();

 // If not found relative, try absolute fallback
 if (!ratingScript.toFile().exists()) {
 ratingScript = Paths.get("C:/Users/HP/OneDrive/Bureau/pidev_israa/pidev_israa/scripts/rate_comment.py");
 }

 ProcessBuilder pb = new ProcessBuilder("python", ratingScript.toString(), commentText);
 pb.redirectErrorStream(true);
 Process process = pb.start();

 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
 String line = reader.readLine();
 int exitCode = process.waitFor();

 if (exitCode == 0 && line != null && !line.isBlank()) {
 double rating = Double.parseDouble(line.trim());
 return Math.max(0.5, Math.min(5.0, rating));
 }
 } catch (Exception e) {
 System.err.println("AI Rating failed: " + e.getMessage());
 }
 return 3.0; // default fallback
 }

 /**
 * Builds a star display HBox for a given rating (supports half stars).
 * Uses: * (full), ½ (half), * (empty)
 */
 private HBox buildStarsDisplay(double rating) {
 HBox starsBox = new HBox(2);
 starsBox.setAlignment(Pos.CENTER_LEFT);

 if (rating <= 0) {
 Label noRating = new Label("Pas encore note");
 noRating.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb;");
 starsBox.getChildren().add(noRating);
 return starsBox;
 }

 // Build visual rating bar
 int fullStars = (int) Math.floor(rating);
 int emptyStars = 5 - fullStars - ((rating - fullStars >= 0.5) ? 1 : 0);

 for (int i = 0; i < fullStars; i++) {
 Label s = new Label("*");
 s.setStyle("-fx-font-size: 14px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
 starsBox.getChildren().add(s);
 }
 if (rating - fullStars >= 0.5) {
 Label h = new Label("*");
 h.setStyle("-fx-font-size: 14px; -fx-text-fill: #f5c842;");
 starsBox.getChildren().add(h);
 }
 for (int i = 0; i < emptyStars; i++) {
 Label e = new Label("*");
 e.setStyle("-fx-font-size: 14px; -fx-text-fill: #ddd;");
 starsBox.getChildren().add(e);
 }

 Label ratingValue = new Label(String.format(" %.1f/5", rating));
 ratingValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-font-weight: bold;");

 starsBox.getChildren().add(ratingValue);
 return starsBox;
 }

 @FXML
 public void completerCours() {
 // Complete the course — service handles percentage, etat, XP, and badges
 Progression prog = progressionService.completerCours(currentUserId, cours.getId());

 // Update button to reflect completion
 btnCompleterCours.setVisible(false);
 btnCompleterCours.setManaged(false);

 String message = prog.getEtat() == EtatProgression.CERTIFIE
 ? "Cours certifie ! +" + cours.getPointsXP() + " XP gagnes"
 : "Cours complete ! +" + cours.getPointsXP() + " XP gagnes";

 lblCompletionStatus.setText(message);
 lblCompletionStatus.setStyle(
 prog.getEtat() == EtatProgression.CERTIFIE
 ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #9b59b6;"
 : "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27ae60;"
 );
 lblCompletionStatus.setVisible(true);
 lblCompletionStatus.setManaged(true);

 // Update the hero banner certification badge if newly certified
 if (prog.getEtat() == EtatProgression.CERTIFIE) {
 lblCertifBadge.setVisible(true);
 lblCertifBadge.setManaged(true);
 }
 }
}
