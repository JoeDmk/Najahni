package com.najahni.models;

import java.time.LocalDateTime;

/**
 * Modèle représentant les données économiques récupérées via les APIs externes.
 *
 * <h3>Sources de données :</h3>
 * <ul>
 *   <li><b>Taux de change</b> : Open Exchange Rates API (gratuit, sans token)</li>
 *   <li><b>PIB (GDP)</b> : World Bank API (gratuit, sans token)</li>
 *   <li><b>Inflation</b> : World Bank API (gratuit, sans token)</li>
 * </ul>
 *
 * <h3>Utilisation :</h3>
 * <p>Ce DTO alimente le {@code EconomicRiskEngine} pour calculer un facteur
 * de risque économique composite, combiné ensuite avec les données
 * d'investissement pour produire un score de risque global.</p>
 *
 * @see com.najahni.services.EconomicApiService
 * @see com.najahni.services.EconomicRiskEngine
 */
public class EconomicData {

    // ─── Taux de change ──────────────────────────────────────
    /** Taux EUR → USD */
    private double exchangeRateEurUsd;
    /** Taux EUR → TND (Dinar Tunisien) */
    private double exchangeRateEurTnd;

    // ─── Indicateurs macroéconomiques ────────────────────────
    /** PIB en milliards USD */
    private double gdpBillions;
    /** Taux d'inflation annuel (%) */
    private double inflationRate;
    /** Année de la dernière donnée PIB/Inflation */
    private String dataYear;

    // ─── Métadonnées ─────────────────────────────────────────
    /** Code ISO du pays (ex: "TN", "FR") */
    private String countryCode;
    /** Nom du pays */
    private String countryName;
    /** Horodatage de la récupération */
    private LocalDateTime fetchTimestamp;
    /** Facteur de risque économique calculé (0–100) */
    private double economicRiskFactor;
    /** Indique si les données ont été récupérées avec succès */
    private boolean dataAvailable;
    /** Message d'erreur éventuel */
    private String errorMessage;

    // ─── Constructeurs ───────────────────────────────────────

    /** Constructeur par défaut. */
    public EconomicData() {
        this.fetchTimestamp = LocalDateTime.now();
        this.dataAvailable = false;
    }

    /** Constructeur complet. */
    public EconomicData(double exchangeRateEurUsd, double exchangeRateEurTnd,
                        double gdpBillions, double inflationRate,
                        String countryCode, String countryName) {
        this.exchangeRateEurUsd = exchangeRateEurUsd;
        this.exchangeRateEurTnd = exchangeRateEurTnd;
        this.gdpBillions = gdpBillions;
        this.inflationRate = inflationRate;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.fetchTimestamp = LocalDateTime.now();
        this.dataAvailable = true;
    }

    // ─── Getters & Setters ───────────────────────────────────

    public double getExchangeRateEurUsd() { return exchangeRateEurUsd; }
    public void setExchangeRateEurUsd(double exchangeRateEurUsd) { this.exchangeRateEurUsd = exchangeRateEurUsd; }

    public double getExchangeRateEurTnd() { return exchangeRateEurTnd; }
    public void setExchangeRateEurTnd(double exchangeRateEurTnd) { this.exchangeRateEurTnd = exchangeRateEurTnd; }

    public double getGdpBillions() { return gdpBillions; }
    public void setGdpBillions(double gdpBillions) { this.gdpBillions = gdpBillions; }

    public double getInflationRate() { return inflationRate; }
    public void setInflationRate(double inflationRate) { this.inflationRate = inflationRate; }

    public String getDataYear() { return dataYear; }
    public void setDataYear(String dataYear) { this.dataYear = dataYear; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }

    public LocalDateTime getFetchTimestamp() { return fetchTimestamp; }
    public void setFetchTimestamp(LocalDateTime fetchTimestamp) { this.fetchTimestamp = fetchTimestamp; }

    public double getEconomicRiskFactor() { return economicRiskFactor; }
    public void setEconomicRiskFactor(double economicRiskFactor) { this.economicRiskFactor = economicRiskFactor; }

    public boolean isDataAvailable() { return dataAvailable; }
    public void setDataAvailable(boolean dataAvailable) { this.dataAvailable = dataAvailable; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // ─── Méthodes utilitaires ────────────────────────────────

    /** Retourne le PIB formaté (ex: "46,7 Mrd $"). */
    public String getFormattedGdp() {
        if (gdpBillions <= 0) return "N/A";
        return String.format("%.1f Mrd $", gdpBillions);
    }

    /** Retourne l'inflation formatée (ex: "8,3%"). */
    public String getFormattedInflation() {
        return String.format("%.1f%%", inflationRate);
    }

    /** Retourne le taux EUR/USD formaté (ex: "1 EUR = 1.085 USD"). */
    public String getFormattedEurUsd() {
        if (exchangeRateEurUsd <= 0) return "N/A";
        return String.format("1 EUR = %.4f USD", exchangeRateEurUsd);
    }

    /** Retourne le taux EUR/TND formaté (ex: "1 EUR = 3.38 TND"). */
    public String getFormattedEurTnd() {
        if (exchangeRateEurTnd <= 0) return "N/A";
        return String.format("1 EUR = %.3f TND", exchangeRateEurTnd);
    }

    /** Retourne le facteur de risque économique formaté. */
    public String getFormattedRiskFactor() {
        return String.format("%.1f / 100", economicRiskFactor);
    }

    /** Retourne le niveau de risque textuel. */
    public String getRiskLevel() {
        if (economicRiskFactor <= 33) return "Faible";
        if (economicRiskFactor <= 66) return "Modéré";
        return "Élevé";
    }

    /** Retourne l'emoji correspondant au niveau de risque. */
    public String getRiskEmoji() {
        if (economicRiskFactor <= 33) return "🟢";
        if (economicRiskFactor <= 66) return "🟡";
        return "🔴";
    }

    @Override
    public String toString() {
        return "EconomicData{" +
               "country='" + countryName + " (" + countryCode + ")" +
               "', EUR/USD=" + String.format("%.4f", exchangeRateEurUsd) +
               ", EUR/TND=" + String.format("%.3f", exchangeRateEurTnd) +
               ", PIB=" + getFormattedGdp() +
               ", Inflation=" + getFormattedInflation() +
               ", RiskFactor=" + String.format("%.1f", economicRiskFactor) +
               ", available=" + dataAvailable +
               '}';
    }
}
