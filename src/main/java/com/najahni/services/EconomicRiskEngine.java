package com.najahni.services;

import com.najahni.models.EconomicData;
import com.najahni.models.InvestmentOpportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Moteur IA de scoring de risque économique.
 *
 * <h3>Formule de scoring composite :</h3>
 * <pre>
 *   risk_score = (facteurMontant × 0.3) + (facteurDuree × 0.2) + (facteurÉconomique × 0.5)
 * </pre>
 *
 * <h3>Le facteur économique est lui-même une combinaison :</h3>
 * <pre>
 *   facteurÉconomique = (facteurChange × 0.3) + (facteurPIB × 0.3) + (facteurInflation × 0.4)
 * </pre>
 *
 * <h3>Logique IA :</h3>
 * <ul>
 *   <li><b>Taux de change</b> : Un EUR faible par rapport à l'USD augmente le risque
 *       (difficulté d'import, dette en dollars plus coûteuse)</li>
 *   <li><b>PIB</b> : Un PIB élevé réduit le risque (économie robuste)</li>
 *   <li><b>Inflation</b> : Une inflation élevée augmente le risque
 *       (érosion du capital, incertitude économique)</li>
 * </ul>
 *
 * @see EconomicApiService
 * @see com.najahni.models.EconomicData
 */
public class EconomicRiskEngine {

    private static final Logger LOGGER = Logger.getLogger(EconomicRiskEngine.class.getName());

    // ─── Constantes de normalisation ─────────────────────────

    /** Montant maximum pour normalisation (500 000 €). */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500000");

    /** PIB de référence (en milliards USD) — Moyenne OCDE ~2000 Mrd. */
    private static final double REFERENCE_GDP = 2000.0;

    /** Inflation "idéale" cible par les banques centrales (2%). */
    private static final double TARGET_INFLATION = 2.0;

    /** Seuil d'inflation considéré comme critique (15%). */
    private static final double CRITICAL_INFLATION = 15.0;

    /** Taux EUR/USD de référence (parité historique moyenne). */
    private static final double REFERENCE_EUR_USD = 1.10;

    // ─── Poids du score final ────────────────────────────────
    private static final double POIDS_MONTANT = 0.3;
    private static final double POIDS_DUREE = 0.2;
    private static final double POIDS_ECONOMIQUE = 0.5;

    // ─── Poids du facteur économique ─────────────────────────
    private static final double POIDS_CHANGE = 0.3;
    private static final double POIDS_PIB = 0.3;
    private static final double POIDS_INFLATION = 0.4;

    // ─── Seuils de risque ────────────────────────────────────
    public static final int SEUIL_FAIBLE = 33;
    public static final int SEUIL_MOYEN = 66;

    // ─── Calcul du facteur économique ────────────────────────

    /**
     * Calcule le facteur de risque économique composite (0–100)
     * à partir des données macroéconomiques.
     *
     * @param data Données économiques récupérées via les APIs
     * @return Facteur de risque économique entre 0 et 100
     */
    public double computeEconomicFactor(EconomicData data) {
        if (data == null || !data.isDataAvailable()) {
            LOGGER.warning("⚠️ Données économiques indisponibles → facteur par défaut (50)");
            return 50.0;
        }

        double facteurChange = normalizeExchangeRate(data.getExchangeRateEurUsd());
        double facteurPib = normalizeGdp(data.getGdpBillions());
        double facteurInflation = normalizeInflation(data.getInflationRate());

        double facteurEconomique = (facteurChange * POIDS_CHANGE)
                                 + (facteurPib * POIDS_PIB)
                                 + (facteurInflation * POIDS_INFLATION);

        facteurEconomique = clamp(facteurEconomique, 0.0, 100.0);

        LOGGER.info("📊 Facteur économique calculé :");
        LOGGER.info("   Change  : " + String.format("%.1f", facteurChange)
                   + " (EUR/USD=" + String.format("%.4f", data.getExchangeRateEurUsd()) + ")");
        LOGGER.info("   PIB     : " + String.format("%.1f", facteurPib)
                   + " (" + data.getFormattedGdp() + ")");
        LOGGER.info("   Inflation: " + String.format("%.1f", facteurInflation)
                   + " (" + data.getFormattedInflation() + ")");
        LOGGER.info("   → Composite : " + String.format("%.1f", facteurEconomique) + " / 100");

        return facteurEconomique;
    }

    // ─── Calcul du score de risque global ────────────────────

    /**
     * Calcule le score de risque global en combinant les données d'investissement
     * et les données économiques.
     *
     * @param targetAmount Montant cible de l'investissement (€)
     * @param deadline     Date limite
     * @param data         Données économiques
     * @return Score de risque global entre 0 et 100
     * @throws IllegalArgumentException si le montant est null ou négatif
     */
    public int calculateFullRisk(BigDecimal targetAmount, LocalDate deadline, EconomicData data) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être supérieur à zéro.");
        }

        double facteurMontant = normalizeAmount(targetAmount);
        double facteurDuree = normalizeDuration(deadline);
        double facteurEconomique = computeEconomicFactor(data);

        // Stocker le facteur dans les données
        if (data != null) {
            data.setEconomicRiskFactor(facteurEconomique);
        }

        double score = (facteurMontant * POIDS_MONTANT)
                      + (facteurDuree * POIDS_DUREE)
                      + (facteurEconomique * POIDS_ECONOMIQUE);

        int finalScore = (int) Math.round(clamp(score, 0.0, 100.0));

        LOGGER.info("═══════════════════════════════════════════════");
        LOGGER.info("🧠 Score de risque IA économique :");
        LOGGER.info("   Montant     : " + String.format("%.1f", facteurMontant) + " × " + POIDS_MONTANT);
        LOGGER.info("   Durée       : " + String.format("%.1f", facteurDuree) + " × " + POIDS_DUREE);
        LOGGER.info("   Économique  : " + String.format("%.1f", facteurEconomique) + " × " + POIDS_ECONOMIQUE);
        LOGGER.info("   → SCORE FINAL : " + finalScore + " / 100 (" + getRiskLevel(finalScore) + ")");
        LOGGER.info("═══════════════════════════════════════════════");

        return finalScore;
    }

    /**
     * Calcule le score de risque pour une opportunité d'investissement existante.
     *
     * @param opportunity L'opportunité à évaluer
     * @param data        Données économiques
     * @return Score de risque global entre 0 et 100
     */
    public int calculateRiskForOpportunity(InvestmentOpportunity opportunity, EconomicData data) {
        if (opportunity == null) {
            throw new IllegalArgumentException("L'opportunité ne peut pas être null.");
        }
        return calculateFullRisk(opportunity.getTargetAmount(), opportunity.getDeadline(), data);
    }

    // ─── Normalisations ──────────────────────────────────────

    /**
     * Normalise le taux de change EUR/USD en facteur de risque (0–100).
     * Un EUR plus faible = risque plus élevé.
     *
     * Logique : Si 1 EUR = 1.20 USD → très fort → risque faible (10)
     *           Si 1 EUR = 1.00 USD → parité → risque moyen (50)
     *           Si 1 EUR = 0.85 USD → EUR faible → risque élevé (85)
     */
    public double normalizeExchangeRate(double eurUsdRate) {
        if (eurUsdRate <= 0) return 50.0;

        // Plus l'EUR est fort (>1.10), plus le risque est faible
        // Plus l'EUR est faible (<1.00), plus le risque est élevé
        double deviation = (REFERENCE_EUR_USD - eurUsdRate) / REFERENCE_EUR_USD;
        double factor = 50.0 + (deviation * 200.0); // ±50 autour de 50

        return clamp(factor, 0.0, 100.0);
    }

    /**
     * Normalise le PIB en facteur de risque (0–100).
     * Un PIB plus élevé = risque plus faible (économie robuste).
     *
     * Logique : PIB > 2000 Mrd → risque faible (10-30)
     *           PIB ~500 Mrd → risque moyen (50)
     *           PIB < 50 Mrd → risque élevé (80-100)
     */
    public double normalizeGdp(double gdpBillions) {
        if (gdpBillions <= 0) return 80.0;

        // Logarithmique : log(PIB/REF) pour compresser l'échelle
        double ratio = gdpBillions / REFERENCE_GDP;

        if (ratio >= 1.0) return clamp(20.0 - (ratio - 1.0) * 10.0, 5.0, 30.0);
        if (ratio >= 0.5) return clamp(40.0 - (ratio - 0.5) * 40.0, 20.0, 50.0);
        if (ratio >= 0.1) return clamp(70.0 - (ratio - 0.1) * 75.0, 40.0, 75.0);
        return clamp(90.0 - ratio * 100.0, 75.0, 95.0);
    }

    /**
     * Normalise le taux d'inflation en facteur de risque (0–100).
     * Une inflation plus élevée = risque plus élevé.
     *
     * Logique : Inflation ~2% → risque faible (15)
     *           Inflation ~5% → risque moyen (40)
     *           Inflation >10% → risque élevé (75+)
     *           Inflation >15% → risque critique (90+)
     */
    public double normalizeInflation(double inflationRate) {
        // Inflation négative (déflation) est aussi risquée
        double absInflation = Math.abs(inflationRate);

        if (absInflation <= TARGET_INFLATION) return 15.0;
        if (absInflation <= 4.0) return 15.0 + (absInflation - TARGET_INFLATION) * 12.5;
        if (absInflation <= 7.0) return 40.0 + (absInflation - 4.0) * 10.0;
        if (absInflation <= 10.0) return 70.0 + (absInflation - 7.0) * 5.0;
        if (absInflation <= CRITICAL_INFLATION) return 85.0 + (absInflation - 10.0) * 2.0;
        return 95.0;
    }

    /**
     * Normalise le montant d'investissement (0–100).
     * Plus le montant est élevé, plus le risque est grand.
     */
    public double normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        double ratio = amount.doubleValue() / MAX_AMOUNT.doubleValue();
        return clamp(ratio * 100.0, 0.0, 100.0);
    }

    /**
     * Normalise la durée restante avant deadline (0–100).
     * Moins de temps restant = risque plus élevé.
     */
    public double normalizeDuration(LocalDate deadline) {
        if (deadline == null) return 50.0;
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        if (daysRemaining <= 0) return 100.0;  // Deadline dépassée
        if (daysRemaining <= 7) return 90.0;
        if (daysRemaining <= 30) return 70.0;
        if (daysRemaining <= 90) return 50.0;
        if (daysRemaining <= 180) return 35.0;
        if (daysRemaining <= 365) return 20.0;
        return 10.0;
    }

    // ─── Utilitaires ─────────────────────────────────────────

    /** Clamp une valeur entre min et max. */
    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Retourne le niveau de risque textuel. */
    public static String getRiskLevel(int score) {
        if (score <= SEUIL_FAIBLE) return "Faible";
        if (score <= SEUIL_MOYEN) return "Modéré";
        return "Élevé";
    }

    /** Retourne l'emoji du risque. */
    public static String getRiskEmoji(int score) {
        if (score <= SEUIL_FAIBLE) return "🟢";
        if (score <= SEUIL_MOYEN) return "🟡";
        return "🔴";
    }

    /** Retourne la couleur CSS du risque. */
    public static String getRiskColor(int score) {
        if (score <= SEUIL_FAIBLE) return "#27ae60";
        if (score <= SEUIL_MOYEN) return "#f39c12";
        return "#e74c3c";
    }

    /** Retourne un conseil d'investissement basé sur le score. */
    public static String getRecommendation(int score) {
        if (score <= 20) return "✅ Excellent climat économique. Investissement fortement recommandé.";
        if (score <= SEUIL_FAIBLE) return "✅ Conditions favorables. Bon moment pour investir.";
        if (score <= 50) return "⚠️ Risque modéré. Investissement viable avec précautions.";
        if (score <= SEUIL_MOYEN) return "⚠️ Prudence recommandée. Diversifiez vos investissements.";
        if (score <= 80) return "🔴 Risque élevé. Investissement déconseillé sauf profil agressif.";
        return "🔴 Risque critique. Report de l'investissement fortement recommandé.";
    }
}
