package com.najahni.controllers;

import com.najahni.models.EconomicData;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.services.EconomicApiService;
import com.najahni.services.EconomicRiskEngine;
import com.najahni.services.InvestmentOpportunityService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur de la vue Front-Office d'Analyse de Risque IA.
 *
 * <h3>Fonctionnalités interactives :</h3>
 * <ul>
 *   <li>Indicateurs économiques animés en temps réel</li>
 *   <li>Calculateur de risque interactif (montant + deadline → score)</li>
 *   <li>Jauge arc animée avec dégradé de couleurs</li>
 *   <li>Cartes de recommandation contextuelles</li>
 *   <li>Vue des opportunités avec leur score de risque</li>
 * </ul>
 */
public class FrontRiskAnalysisController {

    // ─── FXML — Indicateurs ──────────────────────────────────

    @FXML private Label lblFoEurUsd;
    @FXML private Label lblFoEurTnd;
    @FXML private Label lblFoGdp;
    @FXML private Label lblFoInflation;
    @FXML private Label lblFoCountry;
    @FXML private Label lblFoUpdateTime;
    @FXML private Label lblFoStatus;

    // ─── FXML — Calculateur Interactif ───────────────────────

    @FXML private TextField txtAmount;
    @FXML private DatePicker dpDeadline;
    @FXML private ComboBox<String> cmbFoCountry;
    @FXML private Button btnCalculate;

    // ─── FXML — Résultat & Jauge ─────────────────────────────

    @FXML private StackPane foGaugeContainer;
    @FXML private Label lblFoScore;
    @FXML private Label lblFoLevel;
    @FXML private Label lblFoRecommendation;
    @FXML private VBox resultPanel;
    @FXML private VBox detailsPanel;

    // ─── FXML — Opportunités ─────────────────────────────────

    @FXML private VBox opportunitiesContainer;
    @FXML private ProgressIndicator foLoadingIndicator;

    // ─── FXML — Breakdown ────────────────────────────────────

    @FXML private Label lblBreakdownAmount;
    @FXML private Label lblBreakdownDuration;
    @FXML private Label lblBreakdownEconomic;
    @FXML private ProgressBar pbAmount;
    @FXML private ProgressBar pbDuration;
    @FXML private ProgressBar pbEconomic;

    // ─── Services ────────────────────────────────────────────

    private final EconomicApiService apiService;
    private final EconomicRiskEngine riskEngine;
    private final InvestmentOpportunityService opportunityService;

    private EconomicData currentData;

    public FrontRiskAnalysisController() {
        this.apiService = new EconomicApiService();
        this.riskEngine = new EconomicRiskEngine();
        this.opportunityService = new InvestmentOpportunityService();
    }

    // ─── Initialisation ──────────────────────────────────────

    @FXML
    public void initialize() {
        setupControls();
        setupAnimations();

        // Hide result panel initially
        if (resultPanel != null) {
            resultPanel.setVisible(false);
            resultPanel.setManaged(false);
        }

        // Load economic data
        loadEconomicIndicators("TN");
        loadTopOpportunities();
    }

    private void setupControls() {
        // Country selector
        cmbFoCountry.getItems().addAll(
            "🇹🇳 Tunisie", "🇫🇷 France", "🇺🇸 États-Unis",
            "🇩🇪 Allemagne", "🇲🇦 Maroc"
        );
        cmbFoCountry.setValue("🇹🇳 Tunisie");
        cmbFoCountry.setOnAction(e -> {
            String code = extractCountryCode(cmbFoCountry.getValue());
            loadEconomicIndicators(code);
        });

        // Default deadline
        dpDeadline.setValue(LocalDate.now().plusMonths(6));

        // Amount validation
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtAmount.setText(oldVal);
            }
        });
    }

    private void setupAnimations() {
        // Pulse animation on calculate button
        if (btnCalculate != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(800), btnCalculate);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    // ─── Chargement des indicateurs ──────────────────────────

    private void loadEconomicIndicators(String countryCode) {
        setLoading(true);
        lblFoStatus.setText("⏳ Chargement des données...");
        lblFoStatus.setStyle("-fx-text-fill: #f39c12;");

        CompletableFuture.supplyAsync(() -> apiService.fetchAllEconomicData(countryCode))
            .thenAccept(data -> Platform.runLater(() -> {
                currentData = data;
                double factor = riskEngine.computeEconomicFactor(data);
                data.setEconomicRiskFactor(factor);

                updateIndicatorCards(data);
                setLoading(false);
                lblFoStatus.setText("✅ Données à jour");
                lblFoStatus.setStyle("-fx-text-fill: #27ae60;");

                animateIndicators();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    lblFoStatus.setText("⚠️ Mode hors-ligne");
                    lblFoStatus.setStyle("-fx-text-fill: #e74c3c;");
                });
                return null;
            });
    }

    private void updateIndicatorCards(EconomicData data) {
        animateLabel(lblFoEurUsd, String.format("%.4f", data.getExchangeRateEurUsd()));
        animateLabel(lblFoEurTnd, String.format("%.3f", data.getExchangeRateEurTnd()));
        animateLabel(lblFoGdp, data.getFormattedGdp());
        animateLabel(lblFoInflation, data.getFormattedInflation());
        lblFoCountry.setText(data.getCountryName());
        lblFoUpdateTime.setText(data.getFetchTimestamp()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Color-code inflation
        if (data.getInflationRate() > 7) {
            lblFoInflation.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        } else if (data.getInflationRate() > 4) {
            lblFoInflation.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        } else {
            lblFoInflation.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        }
    }

    // ─── Calculateur de risque interactif ────────────────────

    @FXML
    public void calculateRisk() {
        // Validate inputs
        String amountText = txtAmount.getText();
        if (amountText == null || amountText.isBlank()) {
            showAlert("Veuillez saisir un montant d'investissement.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Le montant doit être supérieur à 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Montant invalide.");
            return;
        }

        LocalDate deadline = dpDeadline.getValue();
        if (deadline == null) {
            showAlert("Veuillez sélectionner une date limite.");
            return;
        }

        if (currentData == null) {
            showAlert("Les données économiques ne sont pas encore disponibles. Veuillez patienter.");
            return;
        }

        // Show loading
        btnCalculate.setDisable(true);
        btnCalculate.setText("⏳ Calcul en cours...");

        CompletableFuture.supplyAsync(() -> {
            int score = riskEngine.calculateFullRisk(amount, deadline, currentData);
            return score;
        }).thenAccept(score -> Platform.runLater(() -> {
            displayResult(score, amount, deadline);
            btnCalculate.setDisable(false);
            btnCalculate.setText("🧠 Analyser le Risque");
        }));
    }

    private void displayResult(int score, BigDecimal amount, LocalDate deadline) {
        // Show result panel with animation
        resultPanel.setVisible(true);
        resultPanel.setManaged(true);
        resultPanel.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), resultPanel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Build animated gauge
        buildFrontGauge(score);

        // Update labels
        lblFoScore.setText(EconomicRiskEngine.getRiskEmoji(score) + " " + score + " / 100");
        lblFoScore.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: "
            + EconomicRiskEngine.getRiskColor(score) + ";");

        lblFoLevel.setText("Niveau de risque : " + EconomicRiskEngine.getRiskLevel(score));
        lblFoLevel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
            + EconomicRiskEngine.getRiskColor(score) + ";");

        lblFoRecommendation.setText(EconomicRiskEngine.getRecommendation(score));

        // Update breakdown bars
        double amountFactor = riskEngine.normalizeAmount(amount);
        double durationFactor = riskEngine.normalizeDuration(deadline);
        double economicFactor = currentData.getEconomicRiskFactor();

        animateProgressBar(pbAmount, amountFactor / 100.0);
        animateProgressBar(pbDuration, durationFactor / 100.0);
        animateProgressBar(pbEconomic, economicFactor / 100.0);

        lblBreakdownAmount.setText(String.format("Montant : %.0f/100 (× 0.3 = %.1f)",
            amountFactor, amountFactor * 0.3));
        lblBreakdownDuration.setText(String.format("Durée : %.0f/100 (× 0.2 = %.1f)",
            durationFactor, durationFactor * 0.2));
        lblBreakdownEconomic.setText(String.format("Économique : %.0f/100 (× 0.5 = %.1f)",
            economicFactor, economicFactor * 0.5));

        // Update detail colors
        setBarColor(pbAmount, amountFactor);
        setBarColor(pbDuration, durationFactor);
        setBarColor(pbEconomic, economicFactor);
    }

    private void buildFrontGauge(int score) {
        foGaugeContainer.getChildren().clear();

        // Gauge dimensions — arc center pinned to bottom-centre of the pane
        final double paneW  = 260;
        final double paneH  = 150;
        final double cx     = paneW / 2;      // 130
        final double cy     = paneH - 10;     // 140  (near bottom)
        final double radius = 110;
        final double stroke = 22;

        // Background arc: full 180° from left (180°) to right (0°)
        Arc bgArc = new Arc(cx, cy, radius, radius, 0, 180);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.web("#ecf0f1"));
        bgArc.setStrokeWidth(stroke);
        bgArc.setFill(Color.TRANSPARENT);

        // Score arc: starts at left (180°), sweeps clockwise (negative angle)
        double angle = (score / 100.0) * 180.0;
        Arc scoreArc = new Arc(cx, cy, radius, radius, 180, 0);
        scoreArc.setType(ArcType.OPEN);
        scoreArc.setStroke(Color.web(EconomicRiskEngine.getRiskColor(score)));
        scoreArc.setStrokeWidth(stroke);
        scoreArc.setFill(Color.TRANSPARENT);
        scoreArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        scoreArc.setEffect(new DropShadow(15, Color.web(EconomicRiskEngine.getRiskColor(score), 0.4)));

        // Animate arc from 0 → -angle (clockwise)
        Timeline arcAnim = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(scoreArc.lengthProperty(), 0)),
            new KeyFrame(Duration.millis(1500), new KeyValue(scoreArc.lengthProperty(), -angle, Interpolator.EASE_BOTH))
        );

        // Score counter text, centred inside the arc
        Text scoreText = new Text("0");
        scoreText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        scoreText.setFill(Color.web(EconomicRiskEngine.getRiskColor(score)));

        Text subText = new Text("/ 100");
        subText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subText.setFill(Color.web("#95a5a6"));

        // Animate counter 0 → score
        Timeline counterAnim = new Timeline();
        for (int i = 0; i <= score; i++) {
            final int val = i;
            counterAnim.getKeyFrames().add(
                new KeyFrame(Duration.millis(1500.0 * i / Math.max(score, 1)),
                    e -> scoreText.setText(String.valueOf(val)))
            );
        }

        // Absolute-positioned text — placed just above the arc centre
        VBox textBox = new VBox(-3, scoreText, subText);
        textBox.setAlignment(Pos.CENTER);
        // Centre the VBox horizontally; vertically place it just above arc centre
        textBox.setLayoutX(cx - 40);
        textBox.setLayoutY(cy - 70);

        javafx.scene.layout.Pane gaugePane = new javafx.scene.layout.Pane(bgArc, scoreArc, textBox);
        gaugePane.setPrefSize(paneW, paneH);
        gaugePane.setMaxSize(paneW, paneH);
        gaugePane.setMinSize(paneW, paneH);

        foGaugeContainer.getChildren().add(gaugePane);
        arcAnim.play();
        counterAnim.play();
    }

    // ─── Top Opportunités ────────────────────────────────────

    private void loadTopOpportunities() {
        try {
            List<InvestmentOpportunity> opportunities = opportunityService.findAll();
            opportunitiesContainer.getChildren().clear();

            int count = 0;
            for (InvestmentOpportunity opp : opportunities) {
                if (count >= 5) break; // Top 5

                HBox card = createOpportunityCard(opp);
                opportunitiesContainer.getChildren().add(card);

                // Staggered entrance animation
                card.setOpacity(0);
                card.setTranslateX(-30);
                PauseTransition delay = new PauseTransition(Duration.millis(count * 150));
                final HBox cardRef = card;
                delay.setOnFinished(e -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(400), cardRef);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    TranslateTransition tt = new TranslateTransition(Duration.millis(400), cardRef);
                    tt.setFromX(-30);
                    tt.setToX(0);
                    new ParallelTransition(ft, tt).play();
                });
                delay.play();

                count++;
            }

            if (count == 0) {
                Label emptyLabel = new Label("Aucune opportunité disponible");
                emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                opportunitiesContainer.getChildren().add(emptyLabel);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement opportunités: " + e.getMessage());
        }
    }

    private HBox createOpportunityCard(InvestmentOpportunity opp) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
            + "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER_LEFT);

        // Risk indicator circle
        Circle riskDot = new Circle(8);
        if (opp.getRiskScore() != null) {
            riskDot.setFill(Color.web(EconomicRiskEngine.getRiskColor((int) Math.round(opp.getRiskScore()))));
        } else {
            riskDot.setFill(Color.web("#bdc3c7"));
        }

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLabel = new Label(opp.getProjectTitle() != null ?
            opp.getProjectTitle() : "Opportunité #" + opp.getId());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label amountLabel = new Label(opp.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        Label deadlineLabel = new Label("📅 " + (opp.getDeadline() != null ?
            opp.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"));
        deadlineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        info.getChildren().addAll(titleLabel, amountLabel, deadlineLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Risk badge
        VBox riskBadge = new VBox(2);
        riskBadge.setAlignment(Pos.CENTER);
        if (opp.getRiskScore() != null) {
            int s = (int) Math.round(opp.getRiskScore());
            Label scoreLabel = new Label(EconomicRiskEngine.getRiskEmoji(s) + " " + s);
            scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                + EconomicRiskEngine.getRiskColor(s) + ";");
            Label levelLabel = new Label(EconomicRiskEngine.getRiskLevel(s));
            levelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            riskBadge.getChildren().addAll(scoreLabel, levelLabel);
        } else {
            Label na = new Label("⚪ N/A");
            na.setStyle("-fx-font-size: 14px; -fx-text-fill: #bdc3c7;");
            riskBadge.getChildren().add(na);
        }

        card.getChildren().addAll(riskDot, info, riskBadge);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #f8f9fa; -fx-background-radius: 12; "
            + "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(52,152,219,0.2), 15, 0, 0, 5); "
            + "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; "
            + "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);"));

        return card;
    }

    // ─── Animations ──────────────────────────────────────────

    private void animateIndicators() {
        // Flash indicators on update
        Label[] labels = {lblFoEurUsd, lblFoEurTnd, lblFoGdp, lblFoInflation};
        for (int i = 0; i < labels.length; i++) {
            Label lbl = labels[i];
            PauseTransition delay = new PauseTransition(Duration.millis(i * 200));
            delay.setOnFinished(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), lbl);
                st.setFromX(0.8);
                st.setFromY(0.8);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
            delay.play();
        }
    }

    private void animateLabel(Label label, String newValue) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            label.setText(newValue);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void animateProgressBar(ProgressBar pb, double targetValue) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(pb.progressProperty(), 0)),
            new KeyFrame(Duration.millis(1000),
                new KeyValue(pb.progressProperty(), targetValue, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void setBarColor(ProgressBar pb, double value) {
        String color;
        if (value <= 33) color = "#27ae60";
        else if (value <= 66) color = "#f39c12";
        else color = "#e74c3c";
        pb.setStyle("-fx-accent: " + color + ";");
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private String extractCountryCode(String display) {
        if (display == null) return "TN";
        if (display.contains("Tunisie")) return "TN";
        if (display.contains("France")) return "FR";
        if (display.contains("États-Unis")) return "US";
        if (display.contains("Allemagne")) return "DE";
        if (display.contains("Maroc")) return "MA";
        return "TN";
    }

    private void setLoading(boolean loading) {
        if (foLoadingIndicator != null) foLoadingIndicator.setVisible(loading);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
