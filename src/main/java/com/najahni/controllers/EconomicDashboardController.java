package com.najahni.controllers;

import com.najahni.models.EconomicData;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.services.EconomicApiService;
import com.najahni.services.EconomicRiskEngine;
import com.najahni.services.InvestmentOpportunityService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur du Dashboard Économique IA (Back-Office).
 *
 * <h3>Fonctionnalités :</h3>
 * <ul>
 *   <li>Indicateurs économiques en temps réel (taux de change, PIB, inflation)</li>
 *   <li>Jauge animée de risque économique composite</li>
 *   <li>Graphiques interactifs (distribution des risques, comparaison pays)</li>
 *   <li>Analyse IA des opportunités d'investissement</li>
 *   <li>Recommandations basées sur les données économiques</li>
 * </ul>
 */
public class EconomicDashboardController {

    // ─── FXML Components ─────────────────────────────────────

    @FXML private Label lblEurUsd;
    @FXML private Label lblEurTnd;
    @FXML private Label lblGdp;
    @FXML private Label lblInflation;
    @FXML private Label lblDataYear;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblCountryName;

    @FXML private Label lblEconomicScore;
    @FXML private Label lblEconomicLevel;
    @FXML private Label lblRecommendation;

    @FXML private StackPane gaugeContainer;
    @FXML private PieChart riskDistributionChart;
    @FXML private BarChart<String, Number> economicComparisonChart;
    @FXML private CategoryAxis xAxisComparison;
    @FXML private NumberAxis yAxisComparison;

    @FXML private ComboBox<String> cmbCountry;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox indicatorsPanel;
    @FXML private VBox analysisPanel;

    @FXML private Label lblTotalOpportunities;
    @FXML private Label lblAvgRiskScore;
    @FXML private Label lblHighRiskCount;

    @FXML private VBox formulaBox;

    // ─── Services ────────────────────────────────────────────

    private final EconomicApiService apiService;
    private final EconomicRiskEngine riskEngine;
    private final InvestmentOpportunityService opportunityService;

    private EconomicData currentData;

    public EconomicDashboardController() {
        this.apiService = new EconomicApiService();
        this.riskEngine = new EconomicRiskEngine();
        this.opportunityService = new InvestmentOpportunityService();
    }

    // ─── Initialisation ──────────────────────────────────────

    @FXML
    public void initialize() {
        setupCountrySelector();
        setupCharts();
        animateEntrance();

        // Load data for default country (Tunisia)
        loadEconomicData("TN");
    }

    private void setupCountrySelector() {
        cmbCountry.setItems(FXCollections.observableArrayList(
            "🇹🇳 Tunisie", "🇫🇷 France", "🇺🇸 États-Unis",
            "🇩🇪 Allemagne", "🇲🇦 Maroc", "🇪🇬 Égypte"
        ));
        cmbCountry.setValue("🇹🇳 Tunisie");
        cmbCountry.setOnAction(e -> {
            String selected = cmbCountry.getValue();
            String code = extractCountryCode(selected);
            loadEconomicData(code);
        });
    }

    private void setupCharts() {
        // Risk Distribution PieChart
        riskDistributionChart.setTitle("Distribution des Risques");
        riskDistributionChart.setLabelsVisible(true);
        riskDistributionChart.setAnimated(true);
        riskDistributionChart.setLegendVisible(true);

        // Economic Comparison BarChart
        xAxisComparison.setLabel("Indicateur");
        yAxisComparison.setLabel("Valeur (normalisée)");
        economicComparisonChart.setTitle("Facteurs de Risque Économique");
        economicComparisonChart.setAnimated(true);
        economicComparisonChart.setLegendVisible(false);
    }

    // ─── Chargement des données ──────────────────────────────

    /**
     * Charge les données économiques pour un pays (appel asynchrone).
     */
    @FXML
    public void refreshData() {
        String code = extractCountryCode(cmbCountry.getValue());
        loadEconomicData(code);
    }

    private void loadEconomicData(String countryCode) {
        setLoading(true);

        CompletableFuture.supplyAsync(() -> apiService.fetchAllEconomicData(countryCode))
            .thenAccept(data -> Platform.runLater(() -> {
                currentData = data;

                // Compute economic factor
                double factor = riskEngine.computeEconomicFactor(data);
                data.setEconomicRiskFactor(factor);

                // Update UI
                updateIndicators(data);
                updateGauge(factor);
                updateCharts(data);
                updateAnalysis(data);
                updateOpportunityStats();

                setLoading(false);
                animateDataRefresh();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Erreur lors du chargement des données: " + ex.getMessage());
                });
                return null;
            });
    }

    // ─── Mise à jour des indicateurs ─────────────────────────

    private void updateIndicators(EconomicData data) {
        lblEurUsd.setText(data.getFormattedEurUsd());
        lblEurTnd.setText(data.getFormattedEurTnd());
        lblGdp.setText(data.getFormattedGdp());
        lblInflation.setText(data.getFormattedInflation());
        lblCountryName.setText(data.getCountryName() + " (" + data.getCountryCode() + ")");
        lblDataYear.setText(data.getDataYear() != null ? "Données " + data.getDataYear() : "");
        lblLastUpdate.setText("Mis à jour : " + data.getFetchTimestamp()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        // Color code inflation
        if (data.getInflationRate() > 7) {
            lblInflation.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if (data.getInflationRate() > 4) {
            lblInflation.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        } else {
            lblInflation.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    // ─── Jauge animée ────────────────────────────────────────

    private void updateGauge(double score) {
        gaugeContainer.getChildren().clear();

        // Gauge dimensions — arc centre pinned to bottom-centre of the pane
        final double paneW  = 230;
        final double paneH  = 135;
        final double cx     = paneW / 2;      // 115
        final double cy     = paneH - 10;     // 125
        final double radius = 98;
        final double stroke = 18;

        // Background arc: full 180°
        Arc bgArc = new Arc(cx, cy, radius, radius, 0, 180);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.web("#ecf0f1"));
        bgArc.setStrokeWidth(stroke);
        bgArc.setFill(Color.TRANSPARENT);

        // Score arc: starts at left (180°), sweeps clockwise
        double angle = (score / 100.0) * 180.0;
        Arc scoreArc = new Arc(cx, cy, radius, radius, 180, 0);
        scoreArc.setType(ArcType.OPEN);
        scoreArc.setStroke(Color.web(EconomicRiskEngine.getRiskColor((int) score)));
        scoreArc.setStrokeWidth(stroke);
        scoreArc.setFill(Color.TRANSPARENT);
        scoreArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Animate the score arc
        Timeline arcAnimation = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(scoreArc.lengthProperty(), 0)),
            new KeyFrame(Duration.millis(1200), new KeyValue(scoreArc.lengthProperty(), -angle, Interpolator.EASE_BOTH))
        );

        // Score text, placed inside the arc
        Text scoreText = new Text("0");
        scoreText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
        scoreText.setFill(Color.web(EconomicRiskEngine.getRiskColor((int) score)));

        // Animate score counter
        Timeline counterAnimation = new Timeline();
        int intScore = (int) Math.round(score);
        for (int i = 0; i <= intScore; i++) {
            final int val = i;
            counterAnimation.getKeyFrames().add(
                new KeyFrame(Duration.millis(1200.0 * i / Math.max(intScore, 1)),
                    e -> scoreText.setText(String.valueOf(val)))
            );
        }

        Text labelText = new Text("/100");
        labelText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        labelText.setFill(Color.web("#7f8c8d"));

        // Absolute-positioned text — centred horizontally, just above arc centre
        VBox textBox = new VBox(-3, scoreText, labelText);
        textBox.setAlignment(Pos.CENTER);
        textBox.setLayoutX(cx - 35);
        textBox.setLayoutY(cy - 62);

        javafx.scene.layout.Pane gaugePane = new javafx.scene.layout.Pane(bgArc, scoreArc, textBox);
        gaugePane.setPrefSize(paneW, paneH);
        gaugePane.setMaxSize(paneW, paneH);
        gaugePane.setMinSize(paneW, paneH);

        gaugeContainer.getChildren().add(gaugePane);

        // Level and recommendation
        int scoreInt = (int) Math.round(score);
        lblEconomicScore.setText(EconomicRiskEngine.getRiskEmoji(scoreInt) + " " + scoreInt + "/100");
        lblEconomicLevel.setText("Niveau : " + EconomicRiskEngine.getRiskLevel(scoreInt));
        lblEconomicLevel.setStyle("-fx-text-fill: " + EconomicRiskEngine.getRiskColor(scoreInt) + ";");
        lblRecommendation.setText(EconomicRiskEngine.getRecommendation(scoreInt));

        // Play animations
        arcAnimation.play();
        counterAnimation.play();
    }

    // ─── Graphiques ──────────────────────────────────────────

    private void updateCharts(EconomicData data) {
        // Risk Distribution PieChart
        updateRiskDistributionChart();

        // Economic Factors BarChart
        updateEconomicFactorsChart(data);
    }

    private void updateRiskDistributionChart() {
        try {
            List<InvestmentOpportunity> opportunities = opportunityService.findAll();
            int faible = 0, moyen = 0, eleve = 0, nonCalcule = 0;

            for (InvestmentOpportunity opp : opportunities) {
                if (opp.getRiskScore() == null) {
                    nonCalcule++;
                } else {
                    int score = (int) Math.round(opp.getRiskScore());
                    if (score <= 33) faible++;
                    else if (score <= 66) moyen++;
                    else eleve++;
                }
            }

            riskDistributionChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("🟢 Faible (" + faible + ")", faible),
                new PieChart.Data("🟡 Moyen (" + moyen + ")", moyen),
                new PieChart.Data("🔴 Élevé (" + eleve + ")", eleve),
                new PieChart.Data("⚪ Non calculé (" + nonCalcule + ")", nonCalcule)
            ));

            // Apply colors after data is set
            Platform.runLater(() -> {
                int i = 0;
                String[] colors = {"#27ae60", "#f39c12", "#e74c3c", "#95a5a6"};
                for (PieChart.Data d : riskDistributionChart.getData()) {
                    if (d.getNode() != null && i < colors.length) {
                        d.getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                    }
                    i++;
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur chargement distribution risques: " + e.getMessage());
        }
    }

    private void updateEconomicFactorsChart(EconomicData data) {
        economicComparisonChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Facteurs");

        double facteurChange = riskEngine.normalizeExchangeRate(data.getExchangeRateEurUsd());
        double facteurPib = riskEngine.normalizeGdp(data.getGdpBillions());
        double facteurInflation = riskEngine.normalizeInflation(data.getInflationRate());

        series.getData().add(new XYChart.Data<>("💱 Change", facteurChange));
        series.getData().add(new XYChart.Data<>("📊 PIB", facteurPib));
        series.getData().add(new XYChart.Data<>("📈 Inflation", facteurInflation));
        series.getData().add(new XYChart.Data<>("🧮 Composite", data.getEconomicRiskFactor()));

        economicComparisonChart.getData().add(series);

        // Color bars based on value
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    double val = d.getYValue().doubleValue();
                    String color = EconomicRiskEngine.getRiskColor((int) val);
                    d.getNode().setStyle("-fx-bar-fill: " + color + ";");
                }
            }
        });
    }

    // ─── Analyse des opportunités ────────────────────────────

    private void updateAnalysis(EconomicData data) {
        // Update formula display
        formulaBox.getChildren().clear();

        Label formulaTitle = new Label("🧠 Formule de Scoring IA");
        formulaTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label formulaMain = new Label(
            "Risk = (Montant × 0.3) + (Durée × 0.2) + (Éco × 0.5)");
        formulaMain.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e; -fx-font-family: 'Consolas';");

        Label formulaEco = new Label(
            "Éco = (Change × 0.3) + (PIB × 0.3) + (Inflation × 0.4)");
        formulaEco.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-font-family: 'Consolas';");

        Label formulaValues = new Label(String.format(
            "Éco = (%.1f × 0.3) + (%.1f × 0.3) + (%.1f × 0.4) = %.1f",
            riskEngine.normalizeExchangeRate(data.getExchangeRateEurUsd()),
            riskEngine.normalizeGdp(data.getGdpBillions()),
            riskEngine.normalizeInflation(data.getInflationRate()),
            data.getEconomicRiskFactor()
        ));
        formulaValues.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
            EconomicRiskEngine.getRiskColor((int) data.getEconomicRiskFactor()) +
            "; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        formulaBox.getChildren().addAll(formulaTitle, formulaMain, formulaEco, formulaValues);
        formulaBox.setSpacing(8);
    }

    private void updateOpportunityStats() {
        try {
            List<InvestmentOpportunity> opportunities = opportunityService.findAll();
            int total = opportunities.size();
            double avgScore = opportunities.stream()
                .filter(o -> o.getRiskScore() != null)
                .mapToDouble(o -> o.getRiskScore())
                .average()
                .orElse(0);
            long highRisk = opportunities.stream()
                .filter(o -> o.getRiskScore() != null && o.getRiskScore() > 66)
                .count();

            lblTotalOpportunities.setText(String.valueOf(total));
            lblAvgRiskScore.setText(String.format("%.1f", avgScore));
            lblHighRiskCount.setText(String.valueOf(highRisk));
        } catch (Exception e) {
            lblTotalOpportunities.setText("—");
            lblAvgRiskScore.setText("—");
            lblHighRiskCount.setText("—");
        }
    }

    // ─── Analyse IA sur toutes les opportunités ──────────────

    @FXML
    public void analyzeAllOpportunities() {
        if (currentData == null) {
            showError("Veuillez d'abord charger les données économiques.");
            return;
        }

        setLoading(true);

        CompletableFuture.runAsync(() -> {
            try {
                List<InvestmentOpportunity> opportunities = opportunityService.findAll();
                int updated = 0;

                for (InvestmentOpportunity opp : opportunities) {
                    int score = riskEngine.calculateRiskForOpportunity(opp, currentData);
                    opportunityService.updateRiskScore(opp.getId(), score);
                    updated++;
                }

                final int count = updated;
                Platform.runLater(() -> {
                    setLoading(false);
                    updateRiskDistributionChart();
                    updateOpportunityStats();
                    showSuccess("✅ " + count + " opportunités analysées avec les données économiques de "
                        + currentData.getCountryName() + " !");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Erreur lors de l'analyse : " + e.getMessage());
                });
            }
        });
    }

    // ─── Animations ──────────────────────────────────────────

    private void animateEntrance() {
        if (indicatorsPanel != null) {
            indicatorsPanel.setOpacity(0);
            indicatorsPanel.setTranslateY(30);
            FadeTransition fade = new FadeTransition(Duration.millis(600), indicatorsPanel);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(600), indicatorsPanel);
            slide.setFromY(30);
            slide.setToY(0);
            new ParallelTransition(fade, slide).play();
        }

        if (analysisPanel != null) {
            analysisPanel.setOpacity(0);
            analysisPanel.setTranslateY(30);
            PauseTransition delay = new PauseTransition(Duration.millis(300));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(600), analysisPanel);
                fade.setFromValue(0);
                fade.setToValue(1);
                TranslateTransition slide = new TranslateTransition(Duration.millis(600), analysisPanel);
                slide.setFromY(30);
                slide.setToY(0);
                new ParallelTransition(fade, slide).play();
            });
            delay.play();
        }
    }

    private void animateDataRefresh() {
        if (indicatorsPanel != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(200), indicatorsPanel);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.02);
            pulse.setToY(1.02);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private String extractCountryCode(String display) {
        if (display == null) return "TN";
        if (display.contains("Tunisie")) return "TN";
        if (display.contains("France")) return "FR";
        if (display.contains("États-Unis")) return "US";
        if (display.contains("Allemagne")) return "DE";
        if (display.contains("Maroc")) return "MA";
        if (display.contains("Égypte")) return "EG";
        return "TN";
    }

    private void setLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
