package services.investissement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Calculateur de score de risque pour les opportunités d'investissement.
 *
 * <h3>Formule :</h3>
 * <pre>
 *   risk_score = (facteurMontant × 0.4) + (facteurDuree × 0.2) + (facteurAPI × 0.4)
 * </pre>
 *
 * <h3>Facteurs :</h3>
 * <ul>
 *   <li><b>facteurMontant</b> : Normalisé entre 0–100 selon le montant cible
 *       (0€ = 0, 500 000€+ = 100)</li>
 *   <li><b>facteurDuree</b> : Basé sur le nombre de jours restants avant la deadline
 *       (≤7j = 100, 365j+ = 10)</li>
 *   <li><b>facteurAPI</b> : Indice économique externe récupéré via API publique
 *       (0–100, valeur par défaut = 50)</li>
 * </ul>
 *
 * <h3>Niveaux de risque :</h3>
 * <ul>
 *   <li><b>Faible</b> : 0 – 33</li>
 *   <li><b>Moyen</b> : 34 – 66</li>
 *   <li><b>Élevé</b> : 67 – 100</li>
 * </ul>
 *
 * Diagramme de séquence simplifié :
 * <pre>
 * Controller ──→ RiskService.calculateRisk(opp)
 *                  │
 *                  ├──→ API Open-Meteo (HTTP GET)
 *                  │       └──→ JSON → facteurAPI
 *                  │
 *                  ├──→ RiskCalculator.computeScore(montant, duree, facteurAPI)
 *                  │       └──→ risk_score (0–100)
 *                  │
 *                  ├──→ InvestmentOpportunityService.updateRiskScore(id, score)
 *                  │       └──→ UPDATE SQL
 *                  │
 *                  └──→ return RiskResult(score, level)
 * </pre>
 *
 * @see RiskService
 */
public class RiskCalculator {

    private static final Logger LOGGER = Logger.getLogger(RiskCalculator.class.getName());

    /** Montant maximum pour la normalisation (500 000 €). */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500000");

    /** Seuil risque faible ≤ 33. */
    public static final int SEUIL_FAIBLE = 33;

    /** Seuil risque moyen ≤ 66. */
    public static final int SEUIL_MOYEN = 66;

    // Poids de la formule
    private static final double POIDS_MONTANT = 0.4;
    private static final double POIDS_DUREE = 0.2;
    private static final double POIDS_API = 0.4;

    /** Valeur par défaut du facteur API en cas d'erreur. */
    public static final double DEFAULT_API_FACTOR = 50.0;

    /**
     * Calcule le score de risque final entre 0 et 100.
     *
     * @param targetAmount Montant cible de l'opportunité (en €). Ne doit pas être null ni négatif.
     * @param deadline     Date limite de l'opportunité. Si null, on utilise un facteur durée moyen (50).
     * @param apiFactor    Facteur externe provenant de l'API (0–100).
     * @return Score de risque entier entre 0 et 100.
     * @throws IllegalArgumentException si targetAmount est null ou négatif
     */
    public int computeScore(BigDecimal targetAmount, LocalDate deadline, double apiFactor) {
        // ── Validation des entrées ──
        if (targetAmount == null) {
            throw new IllegalArgumentException("Le montant cible ne peut pas être null.");
        }
        if (targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant cible ne peut pas être négatif.");
        }

        // ── Facteur Montant (0–100) ──
        double facteurMontant = computeAmountFactor(targetAmount);

        // ── Facteur Durée (0–100) ──
        double facteurDuree = computeDurationFactor(deadline);

        // ── Facteur API (clampé entre 0–100) ──
        double facteurApiClamped = clamp(apiFactor, 0.0, 100.0);

        // ── Formule finale ──
        double rawScore = (facteurMontant * POIDS_MONTANT)
                        + (facteurDuree * POIDS_DUREE)
                        + (facteurApiClamped * POIDS_API);

        int score = (int) Math.round(clamp(rawScore, 0.0, 100.0));

        LOGGER.info(String.format(
            "[RiskCalculator] montant=%.2f→%.1f | durée→%.1f | api=%.1f | SCORE=%d (%s)",
            targetAmount, facteurMontant, facteurDuree, facteurApiClamped, score, getRiskLevel(score)
        ));

        return score;
    }

    /**
     * Normalise le montant entre 0 et 100.
     * Plus le montant est élevé, plus le risque est grand.
     *
     * @param amount Montant cible
     * @return Facteur entre 0 et 100
     */
    double computeAmountFactor(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        if (amount.compareTo(MAX_AMOUNT) >= 0) return 100.0;
        return amount.multiply(new BigDecimal("100"))
                     .divide(MAX_AMOUNT, 2, RoundingMode.HALF_UP)
                     .doubleValue();
    }

    /**
     * Calcule le facteur de durée (0–100) selon les jours restants.
     * Moins il reste de jours, plus le risque est élevé.
     *
     * @param deadline Date limite (peut être null → retourne 50)
     * @return Facteur entre 0 et 100
     */
    double computeDurationFactor(LocalDate deadline) {
        if (deadline == null) return 50.0;  // Valeur neutre si pas de deadline

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

        if (daysRemaining <= 0) return 100.0;   // Deadline passée → risque maximum
        if (daysRemaining <= 7) return 90.0;     // Moins d'une semaine
        if (daysRemaining <= 30) return 70.0;    // Moins d'un mois
        if (daysRemaining <= 90) return 50.0;    // 1 à 3 mois
        if (daysRemaining <= 180) return 30.0;   // 3 à 6 mois
        if (daysRemaining <= 365) return 15.0;   // 6 mois à 1 an
        return 10.0;                              // Plus d'un an → faible risque
    }

    /**
     * Retourne le niveau de risque textuel.
     *
     * @param score Score entre 0 et 100
     * @return "Faible", "Moyen" ou "Élevé"
     */
    public static String getRiskLevel(int score) {
        if (score <= SEUIL_FAIBLE) return "Faible";
        if (score <= SEUIL_MOYEN)  return "Moyen";
        return "Élevé";
    }

    /**
     * Retourne l'emoji correspondant au niveau de risque.
     *
     * @param score Score entre 0 et 100
     * @return Emoji 🟢 / 🟡 / 🔴
     */
    public static String getRiskEmoji(int score) {
        if (score <= SEUIL_FAIBLE) return "🟢";
        if (score <= SEUIL_MOYEN)  return "🟡";
        return "🔴";
    }

    /**
     * Retourne le style CSS JavaFX pour le badge de risque.
     *
     * @param score Score entre 0 et 100
     * @return Style CSS inline
     */
    public static String getRiskStyle(int score) {
        String color;
        if (score <= SEUIL_FAIBLE) color = "#27ae60";
        else if (score <= SEUIL_MOYEN) color = "#f39c12";
        else color = "#e74c3c";
        return "-fx-text-fill: " + color + "; -fx-font-weight: bold;";
    }

    /**
     * Limite une valeur entre min et max.
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
