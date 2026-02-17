package tools;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Optional;
import java.util.Random;

/**
 * CaptchaGenerator implements a "I'm not a robot" checkbox-style CAPTCHA.
 *
 * HOW IT WORKS:
 * 1. The login form shows a styled panel with a checkbox and "Je ne suis pas un robot" text.
 * 2. When the user clicks the checkbox, a popup dialog appears with a simple math challenge
 *    (e.g. "What is 7 + 4 ?"). This is easy for humans but blocks simple automated scripts.
 * 3. If the user answers correctly, the panel turns green with a checkmark (✓ Vérifié),
 *    and the `verified` flag is set to true.
 * 4. If wrong, the panel shows a red X and the user can try again.
 * 5. The controller checks `isVerified()` before allowing login.
 *
 * The challenge rotates between addition, subtraction, and multiplication to add variety.
 */
public class CaptchaGenerator {

    private static final Random random = new Random();
    private boolean verified = false;

    // The current challenge question and expected answer
    private String challengeQuestion;
    private int challengeAnswer;

    /**
     * Generates a new random math challenge.
     * Types: addition (a + b), subtraction (a - b, always positive), multiplication (a × b).
     */
    public void generateChallenge() {
        int type = random.nextInt(3); // 0 = add, 1 = subtract, 2 = multiply
        int a, b;

        switch (type) {
            case 0: // Addition: two numbers 1-20
                a = 1 + random.nextInt(20);
                b = 1 + random.nextInt(20);
                challengeQuestion = "Combien font  " + a + " + " + b + "  ?";
                challengeAnswer = a + b;
                break;
            case 1: // Subtraction: ensure positive result
                a = 5 + random.nextInt(20);
                b = 1 + random.nextInt(a - 1); // b < a, so result > 0
                challengeQuestion = "Combien font  " + a + " − " + b + "  ?";
                challengeAnswer = a - b;
                break;
            case 2: // Multiplication: small numbers
                a = 2 + random.nextInt(9);
                b = 2 + random.nextInt(9);
                challengeQuestion = "Combien font  " + a + " × " + b + "  ?";
                challengeAnswer = a * b;
                break;
        }
    }

    /**
     * Shows the challenge dialog. Returns true if the user answers correctly.
     * The dialog is a styled JavaFX Alert with a text input field.
     */
    public boolean showChallengeDialog() {
        generateChallenge();

        // Build a custom dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Vérification de sécurité");
        dialog.setHeaderText(null);

        // --- Build dialog content ---
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 30, 16, 30));
        content.setStyle("-fx-background-color: #F8F9FA;");

        // Shield icon
        Label iconLabel = new Label("\uD83D\uDEE1"); // 🛡 shield emoji
        iconLabel.setFont(Font.font(36));

        // Title
        Label titleLabel = new Label("Prouvez que vous êtes humain");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#1A1A2E"));

        // Question
        Label questionLabel = new Label(challengeQuestion);
        questionLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
        questionLabel.setTextFill(Color.web("#6C63FF"));
        questionLabel.setStyle("-fx-background-color: white; -fx-padding: 12 24; " +
                "-fx-border-color: #E1E8ED; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Answer input
        TextField answerField = new TextField();
        answerField.setPromptText("Votre réponse");
        answerField.setMaxWidth(160);
        answerField.setAlignment(Pos.CENTER);
        answerField.setFont(Font.font("Segoe UI", 16));
        answerField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #E1E8ED; -fx-padding: 8 12;");

        // Error label (hidden by default)
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#E17055"));
        errorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        errorLabel.setVisible(false);

        content.getChildren().addAll(iconLabel, titleLabel, questionLabel, answerField, errorLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8F9FA;");

        // Buttons
        ButtonType verifyButton = new ButtonType("Vérifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(verifyButton, cancelButton);

        // Style the Verify button
        dialog.getDialogPane().lookupButton(verifyButton).setStyle(
                "-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 8 24; -fx-cursor: hand;");

        dialog.setResultConverter(btn -> {
            if (btn == verifyButton) {
                return answerField.getText();
            }
            return null;
        });

        // Focus the answer field when dialog opens
        dialog.setOnShowing(e ->
            javafx.application.Platform.runLater(answerField::requestFocus)
        );

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && result.get() != null) {
            try {
                int userAnswer = Integer.parseInt(result.get().trim());
                if (userAnswer == challengeAnswer) {
                    verified = true;
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric input — fail
            }
        }
        verified = false;
        return false;
    }

    /**
     * @return true if the user has successfully completed the challenge.
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Resets verification state (e.g. after a failed login or page reload).
     */
    public void reset() {
        verified = false;
    }
}
