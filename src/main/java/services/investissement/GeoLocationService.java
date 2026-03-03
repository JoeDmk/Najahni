package services.investissement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service de géolocalisation par IP.
 *
 * <p>Détecte le pays de l'utilisateur via son adresse IP publique,
 * puis mappe le pays vers la devise locale correspondante.</p>
 *
 * <h3>APIs utilisées (gratuites, sans clé) :</h3>
 * <ul>
 *   <li>{@code https://api.ipify.org} — IP publique</li>
 *   <li>{@code http://ip-api.com/json/{ip}} — Géolocalisation (45 req/min)</li>
 * </ul>
 *
 * <h3>Devises supportées :</h3>
 * Correspondance pays → devise pour EUR, USD, TND, GBP, MAD.
 * Les pays non reconnus utilisent EUR par défaut.
 */
public class GeoLocationService {

    private static final Logger LOGGER = Logger.getLogger(GeoLocationService.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    // ─── Cache (une seule détection par session) ─────────────

    private static String cachedCountryCode;
    private static String cachedCurrency;
    private static String cachedPublicIp;

    /**
     * Mapping code pays ISO 3166-1 alpha-2 → devise.
     * Couvre les pays de la zone euro, Maghreb, anglophone, etc.
     */
    private static final Map<String, String> COUNTRY_TO_CURRENCY = Map.ofEntries(
            // Zone Euro
            Map.entry("FR", "EUR"), Map.entry("DE", "EUR"), Map.entry("IT", "EUR"),
            Map.entry("ES", "EUR"), Map.entry("PT", "EUR"), Map.entry("NL", "EUR"),
            Map.entry("BE", "EUR"), Map.entry("AT", "EUR"), Map.entry("IE", "EUR"),
            Map.entry("FI", "EUR"), Map.entry("GR", "EUR"), Map.entry("LU", "EUR"),
            Map.entry("SK", "EUR"), Map.entry("SI", "EUR"), Map.entry("EE", "EUR"),
            Map.entry("LV", "EUR"), Map.entry("LT", "EUR"), Map.entry("CY", "EUR"),
            Map.entry("MT", "EUR"), Map.entry("HR", "EUR"),
            // Tunisie
            Map.entry("TN", "TND"),
            // Maroc
            Map.entry("MA", "MAD"),
            // Royaume-Uni
            Map.entry("GB", "GBP"),
            // États-Unis & dollarisés
            Map.entry("US", "USD"), Map.entry("PR", "USD"), Map.entry("EC", "USD"),
            Map.entry("SV", "USD"), Map.entry("PA", "USD"),
            // Canada → USD (closest supported)
            Map.entry("CA", "USD"),
            // Algérie, Libye → TND (closest supported Maghreb)
            Map.entry("DZ", "TND"), Map.entry("LY", "TND")
    );

    // ─── API publique ────────────────────────────────────────

    /**
     * Détecte la devise de l'utilisateur à partir de son IP publique.
     * Résultat mis en cache pour toute la session.
     *
     * @return Code devise ISO (EUR, USD, TND, GBP, MAD). Défaut : EUR.
     */
    public static String detectCurrency() {
        if (cachedCurrency != null) return cachedCurrency;

        try {
            String ip = getPublicIp();
            if (ip == null || ip.isBlank()) {
                LOGGER.warning("[GeoLocation] Impossible d'obtenir l'IP publique, devise par défaut : EUR");
                cachedCurrency = "EUR";
                return cachedCurrency;
            }

            String countryCode = getCountryFromIp(ip);
            cachedCountryCode = countryCode;
            cachedCurrency = COUNTRY_TO_CURRENCY.getOrDefault(countryCode, "EUR");

            LOGGER.info("[GeoLocation] IP=" + ip + " → Pays=" + countryCode + " → Devise=" + cachedCurrency);
        } catch (Exception e) {
            LOGGER.warning("[GeoLocation] Erreur de détection, devise par défaut : EUR — " + e.getMessage());
            cachedCurrency = "EUR";
        }

        return cachedCurrency;
    }

    /**
     * Retourne le code pays ISO détecté (ex: "TN", "FR", "US").
     * Appelle {@link #detectCurrency()} si pas encore fait.
     *
     * @return Code pays ISO 3166-1 alpha-2, ou null si indéterminé.
     */
    public static String getDetectedCountryCode() {
        if (cachedCountryCode == null) detectCurrency();
        return cachedCountryCode;
    }

    /**
     * Retourne l'IP publique détectée.
     */
    public static String getDetectedPublicIp() {
        if (cachedPublicIp == null) detectCurrency();
        return cachedPublicIp;
    }

    /**
     * Vérifie si une devise est supportée par le CurrencyService.
     */
    public static boolean isSupportedCurrency(String currency) {
        return currency != null && (
                currency.equals("EUR") || currency.equals("USD") ||
                currency.equals("TND") || currency.equals("GBP") ||
                currency.equals("MAD")
        );
    }

    /**
     * Réinitialise le cache (utile à la déconnexion).
     */
    public static void resetCache() {
        cachedCountryCode = null;
        cachedCurrency = null;
        cachedPublicIp = null;
    }

    // ─── Appels réseau internes ──────────────────────────────

    /**
     * Récupère l'IP publique via api.ipify.org.
     */
    private static String getPublicIp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String ip = response.body().trim();
                cachedPublicIp = ip;
                LOGGER.info("[GeoLocation] IP publique détectée : " + ip);
                return ip;
            }
        } catch (Exception e) {
            LOGGER.warning("[GeoLocation] Erreur api.ipify.org : " + e.getMessage());
        }
        return null;
    }

    /**
     * Appelle ip-api.com pour obtenir le code pays depuis une IP.
     * Réponse JSON simplifiée, parsing manuel (pas de dépendance externe).
     *
     * @param ip Adresse IP publique
     * @return Code pays ISO 3166-1 alpha-2 (ex: "TN"), ou null
     */
    private static String getCountryFromIp(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,countryCode"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                // Parse "countryCode":"XX" from JSON
                String countryCode = extractJsonString(body, "countryCode");
                if (countryCode != null && !countryCode.isBlank()) {
                    LOGGER.info("[GeoLocation] Pays détecté : " + countryCode);
                    return countryCode.toUpperCase();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[GeoLocation] Erreur ip-api.com : " + e.getMessage());
        }
        return null;
    }

    /**
     * Extrait une valeur string d'un JSON simple : "key":"value"
     */
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int colonIdx = json.indexOf(':', idx);
        if (colonIdx == -1) return null;

        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) return null;

        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd == -1) return null;

        return json.substring(quoteStart + 1, quoteEnd);
    }
}
