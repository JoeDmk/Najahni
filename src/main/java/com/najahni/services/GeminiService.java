package com.najahni.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service IA hybride : Hugging Face Inference API (primary) + Gemini (fallback).
 * <p>HF = free, reliable, no rate-limit issues, great French support via Mistral.
 * <p>Appels REST via java.net.http — aucune dépendance externe.
 */
public class GeminiService {

    // ── Hugging Face (primary) ──
    private static final String HF_TOKEN = System.getenv("HF_TOKEN") != null
            ? System.getenv("HF_TOKEN") : "VOTRE_CLE_HF";
    private static final String HF_MODEL = "meta-llama/Llama-3.2-3B-Instruct";
    private static final String HF_URL =
            "https://router.huggingface.co/v1/chat/completions";

    // ── Gemini (cloud fallback) ──
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") != null
            ? System.getenv("GEMINI_API_KEY") : "VOTRE_CLE_GEMINI";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    private static final int MAX_HISTORY = 20;
    private final HttpClient httpClient;
    private final List<ChatTurn> conversationHistory = new ArrayList<>();

    /** true once we've confirmed HF is reachable */
    private boolean hfAvailable = false;
    private boolean detectionDone = false;

    private static final String SYSTEM_INSTRUCTION = """
            Tu es NAJAHNI AI, l'assistant intelligent intégré dans la plateforme fintech NAJAHNI — une application desktop JavaFX \
            tunisienne qui connecte entrepreneurs et investisseurs. Tu connais TOUT sur cette application.

            === PLATEFORME NAJAHNI ===
            - App desktop JavaFX (Java 17), MySQL, IA intégrée (HuggingFace Llama 3.2 + Gemini fallback)
            - 2 rôles : INVESTOR (investir, portfolio, paiements Stripe) et ENTREPRENEUR (créer projets, opportunités, gérer offres)
            - Navigation : BackOffice (admin CRUD) + FrontOffice (expérience utilisateur riche avec cartes animées)

            === MODULES FONCTIONNELS ===
            1. **Projets** : DRAFT → APPROVED → COMPLETED/CANCELLED. Entrepreneurs créent des projets avec titre, description, secteur.
            2. **Opportunités d'Investissement** : Liées à un projet. Statuts : OPEN → FUNDED/CLOSED. Montant cible, deadline, description.
            3. **Offres d'Investissement** : Un investisseur propose un montant sur une opportunité. PENDING → ACCEPTED/REJECTED. \
               Offres acceptées → paiement Stripe → Portfolio.
            4. **Portfolio** : Affiche uniquement les offres payées avec détails de transaction (PaymentIntent ID, date).
            5. **Analyse de Risque IA** : Score 0-100 basé sur montant (30%), durée (20%), facteurs économiques (50%). \
               Données économiques en temps réel : taux de change EUR/USD, PIB, inflation via APIs World Bank. \
               Niveaux : Très Faible (0-15), Faible (16-33), Modéré (34-50), Significatif (51-66), Élevé (67-85), Critique (86-100).
            6. **Formation (Cours)** : Types : VIDEO, ARTICLE, QUIZ, TUTORIEL, WORKSHOP. Niveaux : DEBUTANT, INTERMEDIAIRE, AVANCE, EXPERT. \
               Système de gamification avec XP, niveaux, badges, progression en pourcentage.
            7. **Paiements** : Intégration Stripe — validation Luhn, format MM/YY, CVV 3-4 chiffres, confirmation avec confetti animation.

            === DONNÉES ÉCONOMIQUES ===
            - Taux de change via Open Exchange Rates (EUR base)
            - PIB et Inflation via World Bank API
            - Météo Tunis via Open-Meteo (facteur de risque secondaire)
            - 5 pays suivis : Tunisie (TUN), France (FRA), Allemagne (DEU), USA, Chine (CHN)

            === SECTEURS PORTEURS EN TUNISIE ===
            Technologie, Agriculture, Tourisme, Santé, Énergie renouvelable, Industrie textile, Agroalimentaire, Services financiers.

            === RÈGLES ===
            - Réponds TOUJOURS en français
            - Sois concis mais précis (max 4-5 phrases sauf demande de détails)
            - Utilise des émojis pertinents : 📊💰🏦📈⚠️✅❌🎯🇹🇳
            - Pour les analyses d'investissement : fournis niveau de risque, avantages, inconvénients, recommandation
            - Tu connais le contexte tunisien (TND, dinar tunisien, réglementation locale, secteurs porteurs)
            - Ne donne jamais de conseil juridique formel — rappelle de consulter un professionnel
            - Si la question n'est pas liée à NAJAHNI ou la finance, réponds que tu es spécialisé dans la plateforme NAJAHNI
            - Tu peux expliquer comment utiliser chaque fonctionnalité de l'application
            - Tu connais les formules de risque, les statuts des entités, les flux de paiement
            """;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        // Probe HF API asynchronously at startup
        CompletableFuture.runAsync(this::detectHuggingFace);
    }

    // ═══════════════════════════════════════════════════════════
    //  HF DETECTION — quick health check
    // ═══════════════════════════════════════════════════════════

    private void detectHuggingFace() {
        try {
            // Quick probe — send a tiny request to verify token + model work
            String probe = "{\"model\":\"" + HF_MODEL + "\",\"messages\":[{\"role\":\"user\",\"content\":\"ok\"}],\"max_tokens\":1}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HF_URL))
                    .header("Authorization", "Bearer " + HF_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(probe))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                hfAvailable = true;
                System.out.println("[AI] ✅ Hugging Face détecté — modèle : " + HF_MODEL);
            } else {
                System.out.println("[AI] HF status " + resp.statusCode() + ". Fallback → Gemini.");
            }
        } catch (Exception e) {
            System.out.println("[AI] Hugging Face inaccessible (" + e.getMessage() + "). Fallback → Gemini.");
        }
        detectionDone = true;
    }

    private void waitForDetection() {
        int maxWait = 100; // 10 seconds max
        while (!detectionDone && maxWait-- > 0) {
            try { Thread.sleep(100); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
    }

    /** Returns true if using Hugging Face. */
    public boolean isUsingHuggingFace() {
        waitForDetection();
        return hfAvailable;
    }

    /** Kept for backward compatibility with AIChatWidget / FrontOpportunities. */
    public boolean isUsingOllama() { return false; }

    /** Returns which AI backend is in use. */
    public String getBackendName() {
        return isUsingHuggingFace() ? "Llama 3.2 (HuggingFace)" : "Google Gemini";
    }

    // ═══════════════════════════════════════════════════════════
    //  PUBLIC API
    // ═══════════════════════════════════════════════════════════

    public CompletableFuture<String> chat(String userMessage) {
        conversationHistory.add(new ChatTurn("user", userMessage));
        trimHistory();

        return sendRequest(buildMessages()).thenApply(response -> {
            conversationHistory.add(new ChatTurn("assistant", response));
            return response;
        });
    }

    public CompletableFuture<String> analyzeRisk(String projectTitle, String sector,
                                                  double amount, String deadline,
                                                  String description, double currentRiskScore) {
        String prompt = String.format("""
                Analyse de risque IA pour cet investissement :
                
                📌 Projet : %s
                🏷️ Secteur : %s
                💰 Montant : %.2f €
                📅 Deadline : %s
                📝 Description : %s
                📊 Score de risque algorithmique actuel : %.0f/100
                
                Fournis une analyse structurée avec :
                1. 🎯 Ton évaluation du risque (Faible/Moyen/Élevé) et pourquoi
                2. ✅ Points forts de cet investissement (2-3 points)
                3. ⚠️ Points de vigilance (2-3 points)
                4. 💡 Recommandation finale (investir / attendre / éviter)
                5. 📊 Ton score de confiance dans cette analyse (0-100%%)
                
                Contexte : Plateforme d'investissement tunisienne NAJAHNI.
                """, projectTitle, sector, amount, deadline, description, currentRiskScore);

        return sendOneShot(prompt);
    }

    public CompletableFuture<String> generateProjectInsights(String projectTitle, String sector,
                                                              String description, double targetAmount,
                                                              String deadline) {
        String prompt = String.format("""
                Analyse cette opportunité d'investissement sur la plateforme NAJAHNI et fournis des insights professionnels.
                
                📌 Projet : %s
                🏷️ Secteur : %s
                💰 Montant cible : %.2f €
                📅 Deadline : %s
                📝 Description : %s
                
                Réponds EXACTEMENT avec ce format structuré (respecte les balises) :
                
                [SCORE]X[/SCORE]
                (un chiffre de 1 à 10, score d'attractivité)
                
                [RISKLEVEL]Faible|Modéré|Élevé[/RISKLEVEL]
                (un seul mot parmi ces trois)
                
                [CONFIDENCE]X[/CONFIDENCE]
                (un pourcentage de 0 à 100)
                
                [SUMMARY]
                Résumé exécutif en 2-3 phrases.
                [/SUMMARY]
                
                [STRENGTH]Premier avantage clé[/STRENGTH]
                [STRENGTH]Deuxième avantage clé[/STRENGTH]
                [STRENGTH]Troisième avantage clé[/STRENGTH]
                
                [RISK]Premier risque potentiel[/RISK]
                [RISK]Deuxième risque potentiel[/RISK]
                [RISK]Troisième risque potentiel[/RISK]
                
                [MARKET]
                Analyse du marché tunisien pour ce secteur en 1-2 phrases.
                [/MARKET]
                
                [RECOMMENDATION]
                Recommandation finale claire en 1-2 phrases.
                [/RECOMMENDATION]
                
                IMPORTANT : Utilise EXACTEMENT les balises [TAG]...[/TAG]. Pas de texte en dehors des balises.
                """, projectTitle, sector, targetAmount, deadline, description);

        return sendOneShotExtended(prompt, 800);
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    // ═══════════════════════════════════════════════════════════
    //  INTERNAL — ROUTER (HuggingFace vs Gemini)
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<String> sendOneShot(String prompt) {
        return sendOneShotExtended(prompt, 512);
    }

    private CompletableFuture<String> sendOneShotExtended(String prompt, int maxTokens) {
        List<ChatTurn> messages = new ArrayList<>();
        messages.add(new ChatTurn("system", SYSTEM_INSTRUCTION));
        messages.add(new ChatTurn("user", prompt));
        return sendRequestWithTokens(messages, maxTokens);
    }

    private List<ChatTurn> buildMessages() {
        List<ChatTurn> messages = new ArrayList<>();
        messages.add(new ChatTurn("system", SYSTEM_INSTRUCTION));
        for (ChatTurn turn : conversationHistory) {
            messages.add(turn);
        }
        return messages;
    }

    private CompletableFuture<String> sendRequest(List<ChatTurn> messages) {
        return sendRequestWithTokens(messages, 512);
    }

    private CompletableFuture<String> sendRequestWithTokens(List<ChatTurn> messages, int maxTokens) {
        waitForDetection();
        if (hfAvailable) {
            return callHuggingFace(messages, maxTokens);
        } else {
            return callGemini(messages);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HUGGING FACE INFERENCE API — OpenAI-compatible chat
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<String> callHuggingFace(List<ChatTurn> messages, int maxTokens) {
        String payload = buildHFPayload(messages, maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + HF_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(60))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("[HF] Status: " + response.statusCode());
                    if (response.statusCode() == 503) {
                        // Model loading — HF returns 503 while cold-starting
                        System.out.println("[HF] Model loading, retrying via Gemini...");
                        return callGemini(messages).join();
                    }
                    if (response.statusCode() != 200) {
                        System.err.println("[HF] Error " + response.statusCode() + ": "
                                + response.body().substring(0, Math.min(300, response.body().length())));
                        // Fallback to Gemini
                        return callGemini(messages).join();
                    }
                    return extractHFContent(response.body());
                })
                .exceptionally(ex -> {
                    System.err.println("[HF] Request failed: " + ex.getMessage());
                    System.out.println("[AI] HF inaccessible, tentative Gemini...");
                    try {
                        return callGemini(messages).join();
                    } catch (Exception e2) {
                        return "⚠️ IA indisponible. Vérifiez votre connexion internet.";
                    }
                });
    }

    /**
     * Build OpenAI-compatible chat payload for HF Inference API:
     * {"model":"...","messages":[{"role":"system","content":"..."},...],"max_tokens":512,"temperature":0.7}
     */
    private String buildHFPayload(List<ChatTurn> messages, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(HF_MODEL)
          .append("\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            ChatTurn m = messages.get(i);
            sb.append("{\"role\":\"").append(m.role).append("\",\"content\":\"")
              .append(escapeJson(m.text)).append("\"}");
        }
        sb.append("],\"max_tokens\":").append(maxTokens).append(",\"temperature\":0.7,\"stream\":false}");
        return sb.toString();
    }

    /**
     * Extract content from HF OpenAI-compatible response:
     * {"choices":[{"message":{"role":"assistant","content":"..."}}]}
     */
    private String extractHFContent(String json) {
        try {
            // Find "content" inside "choices" → "message"
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx == -1) {
                System.err.println("[HF] No 'choices' in response: " + json.substring(0, Math.min(200, json.length())));
                return "⚠️ Réponse inattendue de l'IA.";
            }
            // Find the "content" field after "choices"
            int contentIdx = json.indexOf("\"content\"", choicesIdx);
            if (contentIdx == -1) return "⚠️ Réponse vide de l'IA.";
            return extractJsonStringValue(json, contentIdx);
        } catch (Exception e) {
            System.err.println("[HF] Parse error: " + e.getMessage());
            return "⚠️ Erreur de traitement de la réponse IA.";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GEMINI API (Fallback) — POST /generateContent
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<String> callGemini(List<ChatTurn> messages) {
        String payload = buildGeminiPayload(messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("[Gemini] API error " + response.statusCode());
                        if (response.statusCode() == 429) {
                            return "⚠️ Tous les services IA sont temporairement surchargés. Réessayez dans quelques minutes.";
                        }
                        return "⚠️ Erreur IA (code " + response.statusCode() + ").";
                    }
                    return extractGeminiText(response.body());
                })
                .exceptionally(ex -> "⚠️ IA cloud inaccessible. Vérifiez votre connexion.");
    }

    private String buildGeminiPayload(List<ChatTurn> messages) {
        StringBuilder contents = new StringBuilder();
        contents.append("[");
        boolean first = true;
        for (ChatTurn m : messages) {
            if ("system".equals(m.role)) continue; // system goes in system_instruction
            if (!first) contents.append(",");
            first = false;
            String role = "user".equals(m.role) ? "user" : "model";
            contents.append("{\"role\":\"").append(role)
                    .append("\",\"parts\":[{\"text\":\"").append(escapeJson(m.text)).append("\"}]}");
        }
        contents.append("]");

        // Extract system message
        String systemMsg = SYSTEM_INSTRUCTION;
        for (ChatTurn m : messages) {
            if ("system".equals(m.role)) { systemMsg = m.text; break; }
        }

        return """
                {"system_instruction":{"parts":[{"text":"%s"}]},"contents":%s,"generationConfig":{"temperature":0.7,"maxOutputTokens":1024}}
                """.formatted(escapeJson(systemMsg), contents.toString()).trim();
    }

    private String extractGeminiText(String json) {
        try {
            int idx = json.indexOf("\"text\"");
            if (idx == -1) {
                if (json.contains("\"error\"")) {
                    int msgIdx = json.indexOf("\"message\"");
                    if (msgIdx != -1) return "⚠️ Erreur Gemini : " + extractJsonStringValue(json, msgIdx);
                }
                return "⚠️ Réponse inattendue de Gemini.";
            }
            return extractJsonStringValue(json, idx);
        } catch (Exception e) {
            return "⚠️ Erreur de traitement de la réponse.";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  JSON UTILITIES
    // ═══════════════════════════════════════════════════════════

    private String extractJsonStringValue(String json, int keyIndex) {
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex == -1) return "";
        int openQuote = json.indexOf('"', colonIndex + 1);
        if (openQuote == -1) return "";

        StringBuilder value = new StringBuilder();
        int i = openQuote + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    default -> { value.append('\\'); value.append(next); }
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
                i++;
            }
        }
        return value.toString().trim();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void trimHistory() {
        while (conversationHistory.size() > MAX_HISTORY) {
            conversationHistory.remove(0);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    private static class ChatTurn {
        final String role; // "system", "user", "assistant"
        final String text;
        ChatTurn(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }
}
