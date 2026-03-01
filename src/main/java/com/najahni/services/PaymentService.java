package com.najahni.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service de paiement Stripe (mode TEST).
 * Utilise directement l'API REST Stripe via java.net.http — pas de SDK externe.
 *
 * <h3>Configuration :</h3>
 * Clé secrète de test Stripe définie dans {@link #TEST_SECRET_KEY}.
 * En prod, utiliser une variable d'environnement ou un fichier de config.
 *
 * <h3>Flow simplifié :</h3>
 * 1. Créer un PaymentIntent côté serveur
 * 2. Confirmer le paiement avec une carte de test pm_card_visa
 * 3. Vérifier le statut
 */
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class.getName());

    /**
     * Clé secrète de test Stripe.
     * IMPORTANT : remplacer par votre propre clé sk_test_... depuis https://dashboard.stripe.com/test/apikeys
     */
    private static final String TEST_SECRET_KEY = System.getenv("STRIPE_SECRET_KEY") != null
            ? System.getenv("STRIPE_SECRET_KEY")
            : "VOTRE_CLE_STRIPE";

    private static final String STRIPE_API = "https://api.stripe.com/v1";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Crée un PaymentIntent Stripe.
     *
     * @param amountCents Montant en centimes (ex : 5000 = 50.00 EUR)
     * @param currency    Code devise (eur, usd, tnd...)
     * @param description Description du paiement
     * @return Résultat contenant l'ID du PaymentIntent et le statut
     */
    public CompletableFuture<PaymentResult> createPaymentIntent(long amountCents, String currency, String description) {
        String body = "amount=" + amountCents
                + "&currency=" + URLEncoder.encode(currency.toLowerCase(), StandardCharsets.UTF_8)
                + "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                + "&payment_method=pm_card_visa"
                + "&confirm=true"
                + "&automatic_payment_methods[enabled]=true"
                + "&automatic_payment_methods[allow_redirects]=never";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_API + "/payment_intents"))
                .header("Authorization", "Basic " + encodeKey())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String json = response.body();
                    LOG.info("Stripe response (" + response.statusCode() + "): " + json.substring(0, Math.min(json.length(), 200)));

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        String id = extractJsonField(json, "id");
                        String status = extractJsonField(json, "status");
                        return new PaymentResult(true, id, status, null);
                    } else {
                        String errorMsg = extractJsonField(json, "message");
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = "Stripe error (HTTP " + response.statusCode() + ")";
                        }
                        return new PaymentResult(false, null, "failed", errorMsg);
                    }
                })
                .exceptionally(ex -> {
                    LOG.warning("Stripe request failed: " + ex.getMessage());
                    return new PaymentResult(false, null, "error",
                            "Erreur réseau : " + ex.getMessage());
                });
    }

    /**
     * Vérifie si la clé Stripe est configurée (non-placeholder).
     */
    public static boolean isConfigured() {
        return !TEST_SECRET_KEY.contains("VOTRE_CLE_ICI") && TEST_SECRET_KEY.startsWith("sk_test_");
    }

    private String encodeKey() {
        return Base64.getEncoder().encodeToString((TEST_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extraction simple d'un champ JSON (sans bibliothèque externe).
     */
    static String extractJsonField(String json, String field) {
        String searchKey = "\"" + field + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        // Skip whitespace
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            // String value
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else if (json.charAt(start) == 'n') {
            return null; // null
        } else {
            // Number or boolean
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') end++;
            return json.substring(start, end).trim();
        }
    }

    /**
     * Résultat d'un paiement Stripe.
     */
    public static class PaymentResult {
        private final boolean success;
        private final String paymentIntentId;
        private final String status;
        private final String errorMessage;

        public PaymentResult(boolean success, String paymentIntentId, String status, String errorMessage) {
            this.success = success;
            this.paymentIntentId = paymentIntentId;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getPaymentIntentId() { return paymentIntentId; }
        public String getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }

        @Override
        public String toString() {
            return success
                    ? "✅ Paiement réussi [" + paymentIntentId + "] status=" + status
                    : "❌ Échec : " + errorMessage;
        }
    }
}
