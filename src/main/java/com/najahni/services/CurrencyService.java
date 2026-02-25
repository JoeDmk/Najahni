package com.najahni.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service de conversion de devises en temps réel.
 * Utilise l'API gratuite exchangerate.host (pas de clé requise).
 *
 * <h3>Devises supportées :</h3>
 * <ul>
 *   <li>EUR (Euro) — devise de base</li>
 *   <li>USD (Dollar américain)</li>
 *   <li>TND (Dinar tunisien)</li>
 *   <li>GBP (Livre sterling)</li>
 *   <li>MAD (Dirham marocain)</li>
 * </ul>
 *
 * <h3>Fallback :</h3>
 * En cas d'erreur réseau, des taux statiques sont utilisés.
 */
public class CurrencyService {

    private static final Logger LOGGER = Logger.getLogger(CurrencyService.class.getName());

    private static final String API_URL = "https://open.er-api.com/v6/latest/EUR";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private Map<String, Double> rates;
    private long lastFetch = 0;
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes

    /** Devises affichables avec drapeaux. */
    public static final String[] CURRENCIES = {"EUR", "USD", "TND", "GBP", "MAD"};
    public static final Map<String, String> CURRENCY_LABELS = Map.of(
        "EUR", "🇪🇺 Euro (EUR)",
        "USD", "🇺🇸 Dollar (USD)",
        "TND", "🇹🇳 Dinar (TND)",
        "GBP", "🇬🇧 Livre (GBP)",
        "MAD", "🇲🇦 Dirham (MAD)"
    );

    public CurrencyService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.rates = getDefaultRates();
    }

    /**
     * Charge les taux de change depuis l'API (asynchrone).
     * @return CompletableFuture qui se complète quand les taux sont chargés
     */
    public CompletableFuture<Map<String, Double>> fetchRates() {
        if (System.currentTimeMillis() - lastFetch < CACHE_DURATION_MS && rates != null) {
            return CompletableFuture.completedFuture(rates);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(TIMEOUT)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    Map<String, Double> parsed = parseRates(response.body());
                    if (!parsed.isEmpty()) {
                        rates = parsed;
                        lastFetch = System.currentTimeMillis();
                        LOGGER.info("[CurrencyService] Taux mis à jour depuis API");
                    }
                }
                return rates;
            })
            .exceptionally(ex -> {
                LOGGER.warning("[CurrencyService] Erreur API, utilisation des taux par défaut: " + ex.getMessage());
                return rates;
            });
    }

    /**
     * Convertit un montant d'une devise à une autre.
     *
     * @param amount Montant à convertir
     * @param from   Devise source (ex: "EUR")
     * @param to     Devise cible (ex: "USD")
     * @return Montant converti
     */
    public double convert(double amount, String from, String to) {
        if (from.equals(to)) return amount;

        double fromRate = rates.getOrDefault(from, 1.0);
        double toRate = rates.getOrDefault(to, 1.0);

        // Convertir via EUR comme pivot
        double amountInEur = amount / fromRate;
        return amountInEur * toRate;
    }

    /**
     * Retourne le taux de change entre deux devises.
     */
    public double getRate(String from, String to) {
        return convert(1.0, from, to);
    }

    /**
     * Formate un montant avec le symbole de la devise.
     */
    public static String format(double amount, String currency) {
        String symbol = switch (currency) {
            case "EUR" -> "€";
            case "USD" -> "$";
            case "TND" -> "DT";
            case "GBP" -> "£";
            case "MAD" -> "MAD";
            default -> currency;
        };
        return String.format("%,.2f %s", amount, symbol);
    }

    /**
     * Retourne les taux actuels.
     */
    public Map<String, Double> getRates() {
        return rates;
    }

    // ─── Parse JSON simple (pas de bibliothèque externe) ─────

    private Map<String, Double> parseRates(String json) {
        Map<String, Double> result = new HashMap<>();
        try {
            String ratesSection = extractJsonObject(json, "rates");
            if (ratesSection == null) return result;

            for (String currency : CURRENCIES) {
                double rate = extractJsonDoubleFromObject(ratesSection, currency);
                if (rate > 0) {
                    result.put(currency, rate);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[CurrencyService] Erreur de parsing JSON: " + e.getMessage());
        }
        return result;
    }

    private String extractJsonObject(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int braceStart = json.indexOf('{', idx);
        if (braceStart == -1) return null;

        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') depth--;
            if (depth == 0) return json.substring(braceStart, i + 1);
        }
        return null;
    }

    private double extractJsonDoubleFromObject(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return -1;

        int colonIdx = json.indexOf(':', idx);
        if (colonIdx == -1) return -1;

        StringBuilder num = new StringBuilder();
        boolean started = false;
        for (int i = colonIdx + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '-' || c == '.' || Character.isDigit(c)) {
                num.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }

        return num.length() > 0 ? Double.parseDouble(num.toString()) : -1;
    }

    // ─── Taux statiques en fallback ──────────────────────────

    private static Map<String, Double> getDefaultRates() {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("EUR", 1.0);
        defaults.put("USD", 1.08);
        defaults.put("TND", 3.35);
        defaults.put("GBP", 0.86);
        defaults.put("MAD", 10.85);
        return defaults;
    }
}
