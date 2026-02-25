package com.najahni.services;

import com.najahni.models.EconomicData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service d'appel aux APIs économiques externes gratuites (sans token / sans inscription).
 *
 * <h3>APIs utilisées :</h3>
 * <ol>
 *   <li><b>Open Exchange Rates API</b> (open.er-api.com) — Taux de change en temps réel
 *       <br>Endpoint : {@code https://open.er-api.com/v6/latest/EUR}</li>
 *   <li><b>World Bank API</b> — PIB et Inflation par pays
 *       <br>PIB : {@code https://api.worldbank.org/v2/country/{code}/indicator/NY.GDP.MKTP.CD?format=json}
 *       <br>Inflation : {@code https://api.worldbank.org/v2/country/{code}/indicator/FP.CPI.TOTL.ZG?format=json}</li>
 * </ol>
 *
 * <h3>Diagramme de séquence :</h3>
 * <pre>
 * EconomicDashboardController
 *   │
 *   ├─→ EconomicApiService.fetchAllEconomicData("TN")
 *   │       │
 *   │       ├─→ fetchExchangeRates()
 *   │       │      └─→ HTTP GET open.er-api.com → EUR/USD, EUR/TND
 *   │       │
 *   │       ├─→ fetchGdp("TN")
 *   │       │      └─→ HTTP GET api.worldbank.org → PIB en milliards
 *   │       │
 *   │       ├─→ fetchInflation("TN")
 *   │       │      └─→ HTTP GET api.worldbank.org → Taux d'inflation
 *   │       │
 *   │       └─→ return EconomicData (DTO complet)
 *   │
 *   └─→ EconomicRiskEngine.computeEconomicFactor(data)
 *           └─→ Facteur de risque économique (0–100)
 * </pre>
 *
 * @see EconomicRiskEngine
 * @see com.najahni.models.EconomicData
 */
public class EconomicApiService {

    private static final Logger LOGGER = Logger.getLogger(EconomicApiService.class.getName());

    /** URL de l'API Open Exchange Rates (gratuite, sans token). */
    private static final String EXCHANGE_RATE_URL = "https://open.er-api.com/v6/latest/EUR";

    /** URL template World Bank — PIB (GDP en USD courant). */
    private static final String GDP_URL_TEMPLATE =
        "https://api.worldbank.org/v2/country/%s/indicator/NY.GDP.MKTP.CD?format=json&per_page=5";

    /** URL template World Bank — Inflation (CPI annuel %). */
    private static final String INFLATION_URL_TEMPLATE =
        "https://api.worldbank.org/v2/country/%s/indicator/FP.CPI.TOTL.ZG?format=json&per_page=5";

    private static final int TIMEOUT_SECONDS = 15;

    private final HttpClient httpClient;

    /** Constructeur par défaut. */
    public EconomicApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /** Constructeur pour tests unitaires (injection du HttpClient). */
    public EconomicApiService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // ─── Méthode principale ──────────────────────────────────

    /**
     * Récupère toutes les données économiques pour un pays donné.
     *
     * @param countryCode Code ISO du pays (ex: "TN" pour Tunisie, "FR" pour France)
     * @return {@link EconomicData} contenant taux de change, PIB et inflation
     */
    public EconomicData fetchAllEconomicData(String countryCode) {
        EconomicData data = new EconomicData();
        data.setCountryCode(countryCode);
        data.setCountryName(getCountryName(countryCode));
        data.setFetchTimestamp(LocalDateTime.now());

        LOGGER.info("═══════════════════════════════════════════════════════");
        LOGGER.info("🌍 Récupération des données économiques pour : " + data.getCountryName() + " (" + countryCode + ")");
        LOGGER.info("═══════════════════════════════════════════════════════");

        boolean hasData = false;

        // ── 1. Taux de change ──
        try {
            fetchExchangeRates(data);
            hasData = true;
        } catch (Exception e) {
            LOGGER.warning("⚠️ Échec récupération taux de change : " + e.getMessage());
            data.setExchangeRateEurUsd(1.08); // Valeur par défaut
            data.setExchangeRateEurTnd(3.38); // Valeur par défaut
        }

        // ── 2. PIB ──
        try {
            fetchGdp(data, countryCode);
            hasData = true;
        } catch (Exception e) {
            LOGGER.warning("⚠️ Échec récupération PIB : " + e.getMessage());
            data.setGdpBillions(getDefaultGdp(countryCode));
        }

        // ── 3. Inflation ──
        try {
            fetchInflation(data, countryCode);
            hasData = true;
        } catch (Exception e) {
            LOGGER.warning("⚠️ Échec récupération inflation : " + e.getMessage());
            data.setInflationRate(getDefaultInflation(countryCode));
        }

        data.setDataAvailable(hasData);
        LOGGER.info("✅ Données économiques récupérées : " + data);
        return data;
    }

    // ─── Taux de change ──────────────────────────────────────

    /**
     * Récupère les taux de change EUR/USD et EUR/TND via Open Exchange Rates API.
     */
    void fetchExchangeRates(EconomicData data) throws Exception {
        LOGGER.info("📡 Appel API Exchange Rates : " + EXCHANGE_RATE_URL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EXCHANGE_RATE_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Exchange Rate retourne code " + response.statusCode());
        }

        String json = response.body();
        LOGGER.info("📥 Réponse Exchange Rate reçue (" + json.length() + " chars)");

        // Parse rates from JSON: {"rates": {"USD": 1.08, "TND": 3.38, ...}}
        double usdRate = extractJsonDouble(json, "USD");
        data.setExchangeRateEurUsd(usdRate);

        try {
            double tndRate = extractJsonDouble(json, "TND");
            data.setExchangeRateEurTnd(tndRate);
        } catch (Exception e) {
            data.setExchangeRateEurTnd(3.38); // Fallback
        }

        LOGGER.info("💱 EUR/USD = " + String.format("%.4f", usdRate)
                   + " | EUR/TND = " + String.format("%.3f", data.getExchangeRateEurTnd()));
    }

    // ─── PIB (GDP) ───────────────────────────────────────────

    /**
     * Récupère le PIB (GDP) du pays via World Bank API.
     */
    void fetchGdp(EconomicData data, String countryCode) throws Exception {
        String url = String.format(GDP_URL_TEMPLATE, countryCode);
        LOGGER.info("📡 Appel API World Bank GDP : " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API World Bank GDP retourne code " + response.statusCode());
        }

        String json = response.body();
        LOGGER.info("📥 Réponse GDP reçue (" + json.length() + " chars)");

        // World Bank returns: [{"page":1,...}, [{"value":46687214123,...,"date":"2023"}, ...]]
        // Find the first non-null "value" in the data array
        double gdpValue = extractWorldBankValue(json);
        String year = extractWorldBankDate(json);

        data.setGdpBillions(gdpValue / 1_000_000_000.0); // Convert to billions
        data.setDataYear(year);

        LOGGER.info("📊 PIB " + countryCode + " (" + year + ") = " + data.getFormattedGdp());
    }

    // ─── Inflation ───────────────────────────────────────────

    /**
     * Récupère le taux d'inflation du pays via World Bank API.
     */
    void fetchInflation(EconomicData data, String countryCode) throws Exception {
        String url = String.format(INFLATION_URL_TEMPLATE, countryCode);
        LOGGER.info("📡 Appel API World Bank Inflation : " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API World Bank Inflation retourne code " + response.statusCode());
        }

        String json = response.body();
        LOGGER.info("📥 Réponse Inflation reçue (" + json.length() + " chars)");

        double inflationValue = extractWorldBankValue(json);
        data.setInflationRate(inflationValue);

        LOGGER.info("📈 Inflation " + countryCode + " = " + data.getFormattedInflation());
    }

    // ─── Helpers JSON ────────────────────────────────────────

    /**
     * Extrait une valeur numérique d'un JSON simple (format clé: valeur).
     * Compatible avec le format {"rates": {"USD": 1.08, ...}}
     */
    static double extractJsonDouble(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            throw new RuntimeException("Clé JSON '" + key + "' non trouvée");
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            throw new RuntimeException("Format JSON invalide pour '" + key + "'");
        }

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
                if (c == 'n') return 0.0; // null
                break;
            }
        }

        if (numberStr.length() == 0) {
            throw new RuntimeException("Impossible de parser la valeur pour '" + key + "'");
        }

        return Double.parseDouble(numberStr.toString());
    }

    /**
     * Extrait la première valeur non-null du JSON World Bank.
     * Format : [{...}, [{"value": 46687214123, "date": "2023"}, {"value": null, ...}, ...]]
     */
    static double extractWorldBankValue(String json) {
        // Find the data array (second element of the outer array)
        // Look for "value" keys after the first ']' or after page metadata
        String searchStr = "\"value\":";
        int searchFrom = 0;

        // Skip past the metadata object (first element of array)
        int firstBracket = json.indexOf('[', 1);
        if (firstBracket > 0) {
            searchFrom = firstBracket;
        }

        // Find first non-null value
        while (searchFrom < json.length()) {
            int valueIndex = json.indexOf(searchStr, searchFrom);
            if (valueIndex == -1) break;

            int afterColon = valueIndex + searchStr.length();

            // Skip whitespace
            while (afterColon < json.length() && Character.isWhitespace(json.charAt(afterColon))) {
                afterColon++;
            }

            if (afterColon >= json.length()) break;

            char firstChar = json.charAt(afterColon);
            if (firstChar == 'n') {
                // null value, skip to next
                searchFrom = afterColon + 4;
                continue;
            }

            if (firstChar == '"') {
                // String value, skip (it's a label not a number)
                searchFrom = afterColon + 1;
                continue;
            }

            if (firstChar == '{') {
                // Object value (like indicator details), skip
                searchFrom = afterColon + 1;
                continue;
            }

            // Numeric value
            StringBuilder numberStr = new StringBuilder();
            for (int i = afterColon; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '-' || c == '.' || c == 'E' || c == 'e' || c == '+' || Character.isDigit(c)) {
                    numberStr.append(c);
                } else {
                    break;
                }
            }

            if (numberStr.length() > 0) {
                try {
                    return Double.parseDouble(numberStr.toString());
                } catch (NumberFormatException e) {
                    searchFrom = afterColon + numberStr.length();
                    continue;
                }
            }

            searchFrom = afterColon + 1;
        }

        throw new RuntimeException("Aucune valeur numérique trouvée dans la réponse World Bank");
    }

    /**
     * Extrait la première date (année) du JSON World Bank.
     */
    static String extractWorldBankDate(String json) {
        String searchStr = "\"date\":\"";
        // Start searching after the metadata
        int firstBracket = json.indexOf('[', 1);
        int searchFrom = firstBracket > 0 ? firstBracket : 0;

        int dateIndex = json.indexOf(searchStr, searchFrom);
        if (dateIndex == -1) return "N/A";

        int start = dateIndex + searchStr.length();
        int end = json.indexOf('"', start);
        if (end == -1) return "N/A";

        return json.substring(start, end);
    }

    // ─── Données par défaut (fallback) ───────────────────────

    /** Retourne le nom du pays à partir du code ISO. */
    static String getCountryName(String code) {
        return switch (code.toUpperCase()) {
            case "TN" -> "Tunisie";
            case "FR" -> "France";
            case "US" -> "États-Unis";
            case "DE" -> "Allemagne";
            case "GB" -> "Royaume-Uni";
            case "MA" -> "Maroc";
            case "DZ" -> "Algérie";
            case "EG" -> "Égypte";
            case "SA" -> "Arabie Saoudite";
            case "AE" -> "Émirats Arabes Unis";
            default -> code;
        };
    }

    /** PIB par défaut (en milliards USD) en cas d'échec API. */
    private static double getDefaultGdp(String code) {
        return switch (code.toUpperCase()) {
            case "TN" -> 46.7;
            case "FR" -> 2780.0;
            case "US" -> 25460.0;
            case "DE" -> 4070.0;
            case "MA" -> 130.0;
            default -> 100.0;
        };
    }

    /** Inflation par défaut (%) en cas d'échec API. */
    private static double getDefaultInflation(String code) {
        return switch (code.toUpperCase()) {
            case "TN" -> 8.3;
            case "FR" -> 4.9;
            case "US" -> 4.1;
            case "DE" -> 5.9;
            case "MA" -> 6.1;
            default -> 5.0;
        };
    }
}
