package util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utility class for displaying alerts and dialogs.
 */
public class AlertUtils {

    /**
     * Shows an information alert.
     * @param title Alert title
     * @param message Alert message
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche une alerte de succès.
     * @param message Message de succès
     */
    public static void showSuccess(String message) {
        showInfo("Succès", message);
    }

    /**
     * Shows an error alert.
     * @param title Alert title
     * @param message Error message
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'erreur de validation.
     * @param message Message d'erreur de validation
     */
    public static void showValidationError(String message) {
        showError("Erreur de Validation", message);
    }

    /**
     * Shows a warning alert.
     * @param title Alert title
     * @param message Warning message
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog.
     * @param title Dialog title
     * @param message Confirmation message
     * @return true if user confirmed
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Affiche une boîte de dialogue de confirmation de suppression.
     * @param itemName Nom de l'élément à supprimer
     * @return true si l'utilisateur a confirmé la suppression
     */
    public static boolean confirmDelete(String itemName) {
        return showConfirmation(
            "Confirmer la Suppression",
            "Êtes-vous sûr de vouloir supprimer : " + itemName + " ?\n\nCette action est irréversible."
        );
    }
}
