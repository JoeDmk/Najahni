package services.investissement;

import models.investissement.InvestmentOpportunity;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service IA de scoring de risque pour les opportunités d'investissement.
 *
 * <h3>Responsabilités :</h3>
 * <ol>
 *   <li>Appeler une API publique gratuite (Open-Meteo) pour obtenir un facteur externe</li>
 *   <li>Déléguer le calcul au {@link RiskCalculator}</li>
 *   <li>Retourner un {@link RiskResult} contenant le score et le niveau</li>
 * </ol>
 *
 * <h3>API utilisée :</h3>
 * <p>Open-Meteo (gratuite, sans token) — on utilise la température comme proxy
 * économique : des conditions météo extrêmes augmentent le risque d'investissement
 * (analogie : sécheresse/gel → impact sur l'économie réelle).</p>
 *
 * <h3>Diagramme de séquence :</h3>
 * <pre>
 * Controller
 *   │
 *   ├─→ RiskService.calculateRisk(opportunity)
 *   │       │
 *   │       ├─→ fetchApiFactor()
 *   │       │      ├─→ HTTP GET https://api.open-meteo.com/v1/forecast?...
 *   │       │      └─→ Parse JSON → température → normalize → facteurAPI
 *   │       │
 *   │       ├─→ RiskCalculator.computeScore(montant, deadline, facteurAPI)
 *   │       │      └─→ score (0–100)
 *   │       │
 *   │       └─→ return RiskResult(score, level, emoji)
 *   │
 *   ├─→ InvestmentOpportunityService.updateRiskScore(id, score)
 *   │       └─→ UPDATE investment_opportunity SET risk_score = ? WHERE id = ?
 *   │
 *   └─→ Afficher score dans UI (TableView + popup)
 * </pre>
 *
 * @see RiskCalculator
 * @see RiskResult
 */
public class RiskService {

    private static final Logger LOGGER = Logger.getLogger(RiskService.class.getName());

    /**
     * URL de l'API Open-Meteo (Tunis, Tunisie — latitude=36.80, longitude=10.18).
     * Paramètres : température actuelle + vent.
     * Gratuit, sans token, sans inscription.
     */
    private static final String API_URL =
        "https://api.open-meteo.com/v1/forecast?latitude=36.80&longitude=10.18"
        + "&current=temperature_2m,wind_speed_10m&timezone=auto";

    private static final int TIMEOUT_SECONDS = 10;

    private final RiskCalculator calculator;
    private final HttpClient httpClient;

    /** Constructeur par défaut. */
    public RiskService() {
        this.calculator = new RiskCalculator();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /** Constructeur pour tests unitaires (injection de dépendances). */
    public RiskService(RiskCalculator calculator, HttpClient httpClient) {
        this.calculator = calculator;
        this.httpClient = httpClient;
    }

    /**
     * Calcule le score de risque pour une opportunité d'investissement.
     *
     * @param opportunity L'opportunité à évaluer. Ne doit pas être null.
     * @return {@link RiskResult} contenant le score (0–100) et le niveau de risque.
     * @throws IllegalArgumentException si l'opportunité est null ou invalide.
     */
    public RiskResult calculateRisk(InvestmentOpportunity opportunity) {
        if (opportunity == null) {
            throw new IllegalArgumentException("L'opportunité ne peut pas être null.");
        }
        if (opportunity.getTargetAmount() == null || opportunity.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être supérieur à zéro.");
        }

        LOGGER.info("═══════════════════════════════════════════════");
        LOGGER.info("🎯 Calcul du Risk Score pour Opportunité #" + opportunity.getId());
        LOGGER.info("   Montant: " + opportunity.getFormattedAmount());
        LOGGER.info("   Deadline: " + opportunity.getDeadline());
        LOGGER.info("═══════════════════════════════════════════════");

        // ── Étape 1 : Appel API externe ──
        double apiFactor = fetchApiFactor();

        // ── Étape 2 : Calcul du score ──
        int score = calculator.computeScore(
            opportunity.getTargetAmount(),
            opportunity.getDeadline(),
            apiFactor
        );

        // ── Étape 3 : Construire le résultat ──
        String level = RiskCalculator.getRiskLevel(score);
        String emoji = RiskCalculator.getRiskEmoji(score);

        LOGGER.info("✅ Résultat : " + emoji + " Score=" + score + " (" + level + ")");
        LOGGER.info("═══════════════════════════════════════════════");

        return new RiskResult(score, level, emoji);
    }

    /**
     * Appelle l'API Open-Meteo et transforme les données météo en facteur de risque (0–100).
     *
     * <p>Logique de conversion :</p>
     * <ul>
     *   <li>Température extrême (< 5°C ou > 40°C) → facteur élevé (80–100)</li>
     *   <li>Température modérée (15–25°C) → facteur faible (20–30)</li>
     *   <li>Vent fort (> 50 km/h) ajoute un bonus de risque</li>
     * </ul>
     *
     * @return Facteur API entre 0 et 100, ou {@link RiskCalculator#DEFAULT_API_FACTOR} en cas d'erreur.
     */
    double fetchApiFactor() {
        try {
            LOGGER.info("📡 Appel API Open-Meteo : " + API_URL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warning("⚠️ API retourne code " + response.statusCode() + " → utilisation valeur par défaut");
                return RiskCalculator.DEFAULT_API_FACTOR;
            }

            String json = response.body();
            LOGGER.info("📥 Réponse API reçue (" + json.length() + " caractères)");

            // ── Parse JSON manuellement (pas de dépendance externe) ──
            double temperature = extractJsonDouble(json, "temperature_2m");
            double windSpeed = extractJsonDouble(json, "wind_speed_10m");

            LOGGER.info("🌡️  Température: " + temperature + "°C | 💨 Vent: " + windSpeed + " km/h");

            // ── Conversion en facteur de risque ──
            double tempFactor = computeTemperatureFactor(temperature);
            double windFactor = computeWindFactor(windSpeed);

            // Pondération : 70% température + 30% vent
            double apiFactor = (tempFactor * 0.7) + (windFactor * 0.3);

            LOGGER.info("📊 Facteur API calculé : " + String.format("%.1f", apiFactor)
                + " (temp=" + String.format("%.1f", tempFactor)
                + ", vent=" + String.format("%.1f", windFactor) + ")");

            return apiFactor;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Erreur API : " + e.getMessage()
                + " → utilisation valeur par défaut (" + RiskCalculator.DEFAULT_API_FACTOR + ")", e);
            return RiskCalculator.DEFAULT_API_FACTOR;
        }
    }

    /**
     * Convertit la température en facteur de risque (0–100).
     * Les extrêmes augmentent le risque.
     */
    double computeTemperatureFactor(double temperature) {
        // Zone optimale : 15–25°C → risque faible
        if (temperature >= 15 && temperature <= 25) return 20.0;
        // Températures modérées : 5–15°C ou 25–35°C
        if (temperature >= 5 && temperature < 15) return 40.0;
        if (temperature > 25 && temperature <= 35) return 40.0;
        // Températures froides : < 5°C
        if (temperature < 5 && temperature >= -10) return 70.0;
        // Températures chaudes : > 35°C
        if (temperature > 35 && temperature <= 45) return 75.0;
        // Extrêmes
        return 95.0;
    }

    /**
     * Convertit la vitesse du vent en facteur de risque (0–100).
     */
    double computeWindFactor(double windSpeed) {
        if (windSpeed <= 10) return 10.0;
        if (windSpeed <= 25) return 30.0;
        if (windSpeed <= 40) return 50.0;
        if (windSpeed <= 60) return 75.0;
        return 95.0;
    }

    /**
     * Extrait une valeur numérique d'un JSON simple.
     * Parse manuel sans bibliothèque externe (pas de Jackson/Gson).
     *
     * @param json Le corps JSON complet
     * @param key  La clé à chercher (ex: "temperature_2m")
     * @return La valeur numérique trouvée
     * @throws RuntimeException si la clé n'est pas trouvée
     */
    static double extractJsonDouble(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            throw new RuntimeException("Clé JSON '" + key + "' non trouvée dans la réponse API");
        }

        // Chercher le ':' après la clé
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            throw new RuntimeException("Format JSON invalide pour la clé '" + key + "'");
        }

        // Extraire la valeur numérique après le ':'
        StringBuilder numberStr = new StringBuilder();
        boolean started = false;
        for (int i = colonIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '-' || c == '.' || Character.isDigit(c)) {
                numberStr.append(c);
                started = true;
            } else if (started) {
                break;
            } else if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                // Non-whitespace, non-numeric → erreur
                if (c == '"') break; // C'est une string, pas un nombre
                break;
            }
        }

        if (numberStr.length() == 0) {
            throw new RuntimeException("Impossible de parser la valeur numérique pour '" + key + "'");
        }

        return Double.parseDouble(numberStr.toString());
    }
}
