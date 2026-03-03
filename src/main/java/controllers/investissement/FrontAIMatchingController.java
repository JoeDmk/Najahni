package controllers.investissement;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import models.User;
import models.investissement.Project;
import services.SessionService;
import services.investissement.GeminiService;
import services.investissement.ProjectService;
import util.AnimationUtils;
import util.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for AI Matching view.
 * Uses HuggingFace Llama AI to match investors with entrepreneur projects.
 */
public class FrontAIMatchingController {

    private static final Logger LOG = Logger.getLogger(FrontAIMatchingController.class.getName());

    @javafx.fxml.FXML private StackPane rootStack;
    @javafx.fxml.FXML private VBox matchingContainer;
    @javafx.fxml.FXML private Label lblStatus;
    @javafx.fxml.FXML private VBox loadingBox;
    @javafx.fxml.FXML private VBox resultsBox;

    private final GeminiService geminiService = new GeminiService();
    private final ProjectService projectService = new ProjectService();
    private User currentUser;

    @javafx.fxml.FXML
    public void initialize() {
        currentUser = SessionService.getInstance().getCurrentUser();
        if (currentUser == null) return;

        startMatching();
    }

    @javafx.fxml.FXML
    public void refreshMatching() {
        startMatching();
    }

    private void startMatching() {
        resultsBox.getChildren().clear();
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        lblStatus.setText("🔄 Analyse en cours...");

        // Get projects (for investors: all projects; for entrepreneurs: show investor demand)
        List<Project> projects = projectService.findAll();
        if (projects.isEmpty()) {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            showEmptyState();
            return;
        }

        // Take up to 6 diverse projects for AI analysis
        List<Project> topProjects = projects.stream()
                .filter(p -> p.getStatus() != null)
                .limit(6)
                .collect(Collectors.toList());

        // Format projects for AI prompt
        StringBuilder projectsDesc = new StringBuilder();
        for (int i = 0; i < topProjects.size(); i++) {
            Project p = topProjects.get(i);
            projectsDesc.append(String.format("""
                    Projet %d : "%s"
                    - Secteur : %s
                    - Description : %s
                    - Entrepreneur : %s
                    - Statut : %s
                    
                    """,
                    i + 1,
                    p.getTitle() != null ? p.getTitle() : "Sans titre",
                    p.getSector() != null ? p.getSector() : "Non défini",
                    p.getDescription() != null ? p.getDescription() : "Pas de description",
                    p.getEntrepreneurName() != null ? p.getEntrepreneurName() : "Anonyme",
                    p.getStatus() != null ? p.getStatus().name() : "N/A"
            ));
        }

        String investorName = (currentUser.getFirstname() + " " + currentUser.getLastname()).trim();
        String investorBio = currentUser.getBio();
        if (investorBio == null || investorBio.isBlank()) {
            investorBio = "Investisseur sur NAJAHNI";
            if (currentUser.getCompanyName() != null && !currentUser.getCompanyName().isBlank()) {
                investorBio += " — " + currentUser.getCompanyName();
            }
        }

        geminiService.generateMatchingAnalysis(investorName, investorBio, projectsDesc.toString())
                .thenAccept(response -> Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    loadingBox.setManaged(false);
                    lblStatus.setText("✅ Analyse terminée");
                    displayMatchingResults(response, topProjects);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loadingBox.setVisible(false);
                        loadingBox.setManaged(false);
                        lblStatus.setText("⚠️ Erreur d'analyse");
                        showErrorState(ex.getMessage());
                    });
                    return null;
                });
    }

    private void displayMatchingResults(String response, List<Project> projects) {
        resultsBox.getChildren().clear();

        // Parse the response by splitting on "---"
        String[] projectAnalyses = response.split("---");
        int projectIndex = 0;

        for (String analysis : projectAnalyses) {
            if (analysis.trim().isEmpty()) continue;

            String projectName = extractTag(analysis, "PROJECT_NAME");
            String matchScore = extractTag(analysis, "MATCH_SCORE");
            String compatibility = extractTag(analysis, "COMPATIBILITY");
            String summary = extractTag(analysis, "MATCH_SUMMARY");
            List<String> strengths = extractMultipleTags(analysis, "MATCH_STRENGTH");
            List<String> concerns = extractMultipleTags(analysis, "MATCH_CONCERN");
            String recommendation = extractTag(analysis, "MATCH_RECOMMENDATION");

            // Use project data if available
            Project project = projectIndex < projects.size() ? projects.get(projectIndex) : null;
            if (projectName == null && project != null) projectName = project.getTitle();

            VBox card = buildMatchingCard(projectName, matchScore, compatibility, summary,
                    strengths, concerns, recommendation, project);
            resultsBox.getChildren().add(card);

            AnimationUtils.playFadeScaleIn(card, 400, projectIndex * 120);
            projectIndex++;
        }

        // If no structured results found, show fallback
        if (resultsBox.getChildren().isEmpty()) {
            VBox fallbackCard = buildFallbackCard(response);
            resultsBox.getChildren().add(fallbackCard);
            AnimationUtils.playFadeScaleIn(fallbackCard, 400, 0);
        }
    }

    private VBox buildMatchingCard(String projectName, String matchScore, String compatibility,
                                    String summary, List<String> strengths, List<String> concerns,
                                    String recommendation, Project project) {
        VBox card = new VBox(0);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 4); "
                + "-fx-border-color: #e9ecef; -fx-border-radius: 16; -fx-border-width: 1;");

        // ── Card Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));

        // Determine header gradient based on score
        int score = parseScore(matchScore);
        String gradient;
        if (score >= 7) gradient = "linear-gradient(to right, #27ae60, #2ecc71)";
        else if (score >= 5) gradient = "linear-gradient(to right, #f39c12, #f1c40f)";
        else gradient = "linear-gradient(to right, #e74c3c, #c0392b)";

        header.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 16 16 0 0;");

        Label icon = new Label("🤝");
        icon.setFont(Font.font(24));

        VBox headerInfo = new VBox(2);
        Label titleLbl = new Label(projectName != null ? projectName : "Projet");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setWrapText(true);
        
        String sectorText = "";
        if (project != null && project.getSector() != null) sectorText = " — " + project.getSector();
        Label subtitleLbl = new Label("🏷️ " + (project != null && project.getEntrepreneurName() != null ? project.getEntrepreneurName() : "Entrepreneur") + sectorText);
        subtitleLbl.setFont(Font.font("Segoe UI", 11));
        subtitleLbl.setTextFill(Color.web("#ffffffcc"));
        headerInfo.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Score badge
        VBox scoreBadge = new VBox(0);
        scoreBadge.setAlignment(Pos.CENTER);
        scoreBadge.setPadding(new Insets(6, 14, 6, 14));
        scoreBadge.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 12;");
        Label scoreVal = new Label(matchScore != null ? matchScore + "/10" : "?/10");
        scoreVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        scoreVal.setTextFill(Color.web(score >= 7 ? "#27ae60" : score >= 5 ? "#f39c12" : "#e74c3c"));
        Label scoreLbl = new Label("Match");
        scoreLbl.setFont(Font.font("Segoe UI", 9));
        scoreLbl.setTextFill(Color.web("#7f8c8d"));
        scoreBadge.getChildren().addAll(scoreVal, scoreLbl);

        header.getChildren().addAll(icon, headerInfo, spacer, scoreBadge);

        // ── Card Body ──
        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 20, 16, 20));

        // Compatibility + Score row
        HBox metricsRow = new HBox(12);
        metricsRow.setAlignment(Pos.CENTER);

        if (compatibility != null) {
            String compColor = switch (compatibility.toLowerCase()) {
                case "excellente" -> "#27ae60";
                case "bonne" -> "#2ecc71";
                case "moyenne" -> "#f39c12";
                default -> "#e74c3c";
            };
            VBox compBox = buildMetricBadge("🎯 Compatibilité", compatibility, compColor);
            metricsRow.getChildren().add(compBox);
        }

        // Match progress bar
        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressBox, Priority.ALWAYS);
        Label progLabel = new Label("Score de correspondance");
        progLabel.setFont(Font.font("Segoe UI", 10));
        progLabel.setTextFill(Color.web("#95a5a6"));
        StackPane barContainer = new StackPane();
        barContainer.setPrefHeight(10);
        barContainer.setMaxWidth(Double.MAX_VALUE);
        barContainer.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 5;");
        Region barFill = new Region();
        barFill.setPrefHeight(10);
        barFill.setMaxWidth(0);
        String barColor = score >= 7 ? "#27ae60" : score >= 5 ? "#f39c12" : "#e74c3c";
        barFill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 5;");
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        barContainer.getChildren().add(barFill);
        progressBox.getChildren().addAll(progLabel, barContainer);
        metricsRow.getChildren().add(progressBox);

        // Animate bar
        final int finalScore = score;
        Platform.runLater(() -> {
            double maxW = barContainer.getWidth() > 0 ? barContainer.getWidth() : 300;
            double target = Math.max(10, (finalScore / 10.0) * maxW);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(barFill.maxWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(800), new KeyValue(barFill.maxWidthProperty(), target, Interpolator.EASE_OUT))
            );
            tl.setDelay(Duration.millis(400));
            tl.play();
        });

        body.getChildren().add(metricsRow);

        // Summary
        if (summary != null) {
            VBox summaryBox = new VBox(4);
            summaryBox.setPadding(new Insets(10, 14, 10, 14));
            summaryBox.setStyle("-fx-background-color: #f0f0ff; -fx-background-radius: 10;");
            Label sumTitle = new Label("📋 Résumé");
            sumTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            sumTitle.setTextFill(Color.web("#667eea"));
            Label sumText = new Label(summary);
            sumText.setFont(Font.font("Segoe UI", 12));
            sumText.setTextFill(Color.web("#2c3e50"));
            sumText.setWrapText(true);
            sumText.setLineSpacing(2);
            summaryBox.getChildren().addAll(sumTitle, sumText);
            body.getChildren().add(summaryBox);
        }

        // Strengths
        if (!strengths.isEmpty()) {
            VBox strengthsBox = new VBox(6);
            Label strTitle = new Label("✅ Points forts du match");
            strTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            strTitle.setTextFill(Color.web("#27ae60"));
            strengthsBox.getChildren().add(strTitle);
            for (String s : strengths) {
                HBox bullet = buildBulletItem(s, "#27ae60", "#f0fff4");
                strengthsBox.getChildren().add(bullet);
            }
            body.getChildren().add(strengthsBox);
        }

        // Concerns
        if (!concerns.isEmpty()) {
            VBox concernsBox = new VBox(6);
            Label conTitle = new Label("⚠️ Points de vigilance");
            conTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            conTitle.setTextFill(Color.web("#e67e22"));
            concernsBox.getChildren().add(conTitle);
            for (String c : concerns) {
                HBox bullet = buildBulletItem(c, "#e67e22", "#fff8f0");
                concernsBox.getChildren().add(bullet);
            }
            body.getChildren().add(concernsBox);
        }

        // Recommendation
        if (recommendation != null) {
            VBox recBox = new VBox(4);
            recBox.setPadding(new Insets(10, 14, 10, 14));
            recBox.setStyle("-fx-background-color: linear-gradient(to right, #667eea15, #764ba215); "
                    + "-fx-background-radius: 10; -fx-border-color: #667eea44; -fx-border-radius: 10; -fx-border-width: 1;");
            Label recTitle = new Label("💡 Recommandation");
            recTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            recTitle.setTextFill(Color.web("#764ba2"));
            Label recText = new Label(recommendation);
            recText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            recText.setTextFill(Color.web("#2c3e50"));
            recText.setWrapText(true);
            recBox.getChildren().addAll(recTitle, recText);
            body.getChildren().add(recBox);
        }

        card.getChildren().addAll(header, body);

        // Hover animation
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.012); st.setToY(1.012); st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private VBox buildMetricBadge(String label, String value, String color) {
        VBox badge = new VBox(2);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(10, 16, 10, 16));
        badge.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 12; "
                + "-fx-border-color: " + color + "44; -fx-border-radius: 12; -fx-border-width: 1;");
        badge.setMinWidth(120);

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        valueLbl.setTextFill(Color.web(color));
        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", 10));
        labelLbl.setTextFill(Color.web("#555"));

        badge.getChildren().addAll(valueLbl, labelLbl);
        return badge;
    }

    private HBox buildBulletItem(String text, String color, String bgColor) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12));
        item.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        Label dot = new Label("●");
        dot.setFont(Font.font(8));
        dot.setTextFill(Color.web(color));
        Label textLbl = new Label(text);
        textLbl.setFont(Font.font("Segoe UI", 11));
        textLbl.setTextFill(Color.web("#2c3e50"));
        textLbl.setWrapText(true);
        HBox.setHgrow(textLbl, Priority.ALWAYS);
        item.getChildren().addAll(dot, textLbl);
        return item;
    }

    private VBox buildFallbackCard(String response) {
        VBox card = new VBox(10);
        card.setMaxWidth(700);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 3); "
                + "-fx-border-color: #e9ecef; -fx-border-radius: 16; -fx-border-width: 1;");

        Label title = new Label("🤖 Analyse IA");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#667eea"));

        Label text = new Label(response);
        text.setFont(Font.font("Segoe UI", 13));
        text.setTextFill(Color.web("#2c3e50"));
        text.setWrapText(true);
        text.setLineSpacing(3);

        card.getChildren().addAll(title, text);
        return card;
    }

    private void showEmptyState() {
        VBox emptyBox = new VBox(12);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60, 20, 60, 20));
        Label icon = new Label("🤝");
        icon.setFont(Font.font(48));
        Label title = new Label("Aucun projet disponible");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#7f8c8d"));
        Label desc = new Label("Il n'y a pas encore de projets à analyser pour le matching.");
        desc.setFont(Font.font("Segoe UI", 13));
        desc.setTextFill(Color.web("#95a5a6"));
        desc.setWrapText(true);
        desc.setMaxWidth(400);
        desc.setAlignment(Pos.CENTER);
        emptyBox.getChildren().addAll(icon, title, desc);
        resultsBox.getChildren().add(emptyBox);
    }

    private void showErrorState(String message) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40, 20, 40, 20));
        Label icon = new Label("⚠️");
        icon.setFont(Font.font(36));
        Label title = new Label("Erreur d'analyse");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#e74c3c"));
        Label desc = new Label(message != null ? message : "Une erreur est survenue lors de l'analyse IA.");
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Color.web("#7f8c8d"));
        desc.setWrapText(true);
        desc.setMaxWidth(400);
        errorBox.getChildren().addAll(icon, title, desc);
        resultsBox.getChildren().add(errorBox);
    }

    // ── Tag parsing (same logic as FrontOpportunitiesController) ──

    private String extractTag(String text, String tag) {
        String open = "[" + tag + "]";
        String close = "[/" + tag + "]";
        int start = text.indexOf(open);
        if (start == -1) return null;
        start += open.length();
        int end = text.indexOf(close, start);
        if (end == -1) return null;
        return text.substring(start, end).trim();
    }

    private List<String> extractMultipleTags(String text, String tag) {
        List<String> results = new ArrayList<>();
        String open = "[" + tag + "]";
        String close = "[/" + tag + "]";
        int searchFrom = 0;
        while (true) {
            int start = text.indexOf(open, searchFrom);
            if (start == -1) break;
            start += open.length();
            int end = text.indexOf(close, start);
            if (end == -1) break;
            String val = text.substring(start, end).trim();
            if (!val.isEmpty()) results.add(val);
            searchFrom = end + close.length();
        }
        return results;
    }

    private int parseScore(String scoreStr) {
        if (scoreStr == null) return 5;
        try {
            return Integer.parseInt(scoreStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return 5; }
    }
}
