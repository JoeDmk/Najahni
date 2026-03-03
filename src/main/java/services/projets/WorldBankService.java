package services.projets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service World Bank API — Données macro-économiques Tunisie
 * Totalement gratuit, aucune clé requise
 */
public class WorldBankService {

    private static final String BASE_URL = "https://api.worldbank.org/v2/country/TN/indicator/";
    private static final HttpClient client = HttpClient.newHttpClient();

    // ========== INDICATEURS WORLD BANK ==========
    // PIB total Tunisie
    private static final String PIB_TOTAL         = "NY.GDP.MKTP.CD";
    // Croissance du PIB (%)
    private static final String CROISSANCE_PIB     = "NY.GDP.MKTP.KD.ZG";
    // PIB Agriculture (% du PIB)
    private static final String PIB_AGRI           = "NV.AGR.TOTL.ZS";
    // PIB Industrie (% du PIB)
    private static final String PIB_INDUSTRIE      = "NV.IND.TOTL.ZS";
    // PIB Services (% du PIB)
    private static final String PIB_SERVICES       = "NV.SRV.TOTL.ZS";
    // Taux de chômage
    private static final String CHOMAGE            = "SL.UEM.TOTL.ZS";
    // Individus utilisant internet (%)
    private static final String INTERNET           = "IT.NET.USER.ZS";
    // Population totale
    private static final String POPULATION         = "SP.POP.TOTL";
    // Inflation
    private static final String INFLATION          = "FP.CPI.TOTL.ZG";

    // ========== MÉTHODE PRINCIPALE ==========
    /**
     * Retourne un contexte macro-économique adapté au secteur du projet
     */
    public static String getContexteMacroEconomique(String secteur) {
        StringBuilder contexte = new StringBuilder();
        contexte.append("=== DONNÉES MACRO-ÉCONOMIQUES TUNISIE (World Bank) ===\n\n");

        try {
            // Données communes à tous les secteurs
            double pib         = getValeur(PIB_TOTAL);
            double croissance  = getValeur(CROISSANCE_PIB);
            double chomage     = getValeur(CHOMAGE);
            double internet    = getValeur(INTERNET);
            double population  = getValeur(POPULATION);
            double inflation   = getValeur(INFLATION);

            if (pib > 0) {
                contexte.append(String.format("📊 PIB Tunisie : %.1f Milliards USD\n", pib / 1_000_000_000));
            }
            if (croissance != Double.MIN_VALUE) {
                contexte.append(String.format("📈 Croissance PIB : %.1f%%\n", croissance));
            }
            if (chomage > 0) {
                contexte.append(String.format("👥 Taux de chômage : %.1f%%\n", chomage));
            }
            if (internet > 0) {
                contexte.append(String.format("🌐 Pénétration Internet : %.1f%%\n", internet));
            }
            if (population > 0) {
                contexte.append(String.format("🏙️ Population : %.1f millions\n", population / 1_000_000));
            }
            if (inflation != Double.MIN_VALUE) {
                contexte.append(String.format("💹 Inflation : %.1f%%\n", inflation));
            }

            contexte.append("\n");

            // Données spécifiques au secteur
            contexte.append(getDonneesSecteur(secteur));

        } catch (Exception e) {
            System.err.println("⚠️ World Bank API indisponible: " + e.getMessage());
            contexte.append("(Données World Bank temporairement indisponibles)\n");
        }

        return contexte.toString();
    }

    // ========== DONNÉES PAR SECTEUR ==========
    private static String getDonneesSecteur(String secteur) {
        if (secteur == null) return "";

        StringBuilder sb = new StringBuilder();
        String s = secteur.toLowerCase().trim();

        try {
            if (s.contains("agri")) {
                double partAgri = getValeur(PIB_AGRI);
                sb.append("🌾 SECTEUR AGRICULTURE (données réelles Tunisie):\n");
                if (partAgri > 0) {
                    sb.append(String.format("  • Part dans le PIB : %.1f%%\n", partAgri));
                    sb.append(String.format("  • Contribution estimée : %.2f Mds USD\n", (getValeur(PIB_TOTAL) * partAgri / 100) / 1e9));
                }
                sb.append("  • Tunisie = exportateur majeur d'huile d'olive et dattes\n");
                sb.append("  • Programme national d'agritech en développement\n");

            } else if (s.contains("tech") || s.contains("it") || s.contains("digit") || s.contains("logiciel")) {
                double partServices = getValeur(PIB_SERVICES);
                double net = getValeur(INTERNET);
                sb.append("💻 SECTEUR TECH / NUMÉRIQUE (données réelles Tunisie):\n");
                if (partServices > 0) {
                    sb.append(String.format("  • Part services dans le PIB : %.1f%%\n", partServices));
                }
                if (net > 0) {
                    sb.append(String.format("  • Pénétration internet : %.1f%% de la population\n", net));
                }
                sb.append("  • +1 400 entreprises IT recensées en Tunisie\n");
                sb.append("  • Hub tech régional : Tunis, Sfax, Sousse\n");

            } else if (s.contains("sante") || s.contains("santé") || s.contains("medic") || s.contains("pharma")) {
                sb.append("🏥 SECTEUR SANTÉ (données réelles Tunisie):\n");
                sb.append("  • Dépenses santé ≈ 7% du PIB\n");
                sb.append("  • 3 500+ médecins formés par an\n");
                sb.append("  • Marché télémédecine en forte croissance post-COVID\n");

            } else if (s.contains("finance") || s.contains("fintech") || s.contains("banque")) {
                sb.append("💳 SECTEUR FINANCE / FINTECH (données réelles Tunisie):\n");
                sb.append("  • 23 banques opérationnelles\n");
                sb.append("  • Taux de bancarisation ≈ 37% de la population\n");
                sb.append("  • Marché mobile money en pleine expansion\n");
                sb.append("  • Réglementation BCT encadrée (loi startup act 2019)\n");

            } else if (s.contains("educ") || s.contains("edtech") || s.contains("formation")) {
                double pop = getValeur(POPULATION);
                double net = getValeur(INTERNET);
                sb.append("📚 SECTEUR ÉDUCATION / EDTECH (données réelles Tunisie):\n");
                if (pop > 0) {
                    sb.append(String.format("  • Population étudiante estimée : %.0f K étudiants\n", pop * 0.08 / 1000));
                }
                if (net > 0) {
                    sb.append(String.format("  • Internet disponible pour %.1f%% de la population\n", net));
                }
                sb.append("  • 13 universités publiques, 40+ instituts supérieurs\n");
                sb.append("  • Startup Act 2019 : exonérations pour les EdTech\n");

            } else if (s.contains("tourism") || s.contains("tourisme") || s.contains("hotel")) {
                sb.append("✈️ SECTEUR TOURISME (données réelles Tunisie):\n");
                sb.append("  • 9 millions de touristes/an (pré-COVID)\n");
                sb.append("  • Contribution : ~7% du PIB\n");
                sb.append("  • Plan 2035 : doubler les recettes touristiques\n");

            } else {
                // Secteur générique
                double partServices = getValeur(PIB_SERVICES);
                double partIndustrie = getValeur(PIB_INDUSTRIE);
                sb.append("📊 DONNÉES SECTORIELLES TUNISIE:\n");
                if (partServices > 0) sb.append(String.format("  • Services : %.1f%% du PIB\n", partServices));
                if (partIndustrie > 0) sb.append(String.format("  • Industrie : %.1f%% du PIB\n", partIndustrie));
                sb.append("  • Startup Act 2019 : avantages fiscaux pour startups innovantes\n");
                sb.append("  • Accès aux marchés: UE (ALECA), Afrique (ZLECAF en discussion)\n");
            }

        } catch (Exception e) {
            sb.append("  (données sectorielles indisponibles)\n");
        }

        return sb.toString();
    }

    // ========== APPEL API ==========
    private static double getValeur(String indicateur) throws Exception {
        String url = BASE_URL + indicateur + "?format=json&mrv=1&per_page=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonArray root = JsonParser.parseString(response.body()).getAsJsonArray();
            if (root.size() >= 2) {
                JsonArray data = root.get(1).getAsJsonArray();
                if (data.size() > 0) {
                    JsonObject entry = data.get(0).getAsJsonObject();
                    if (!entry.get("value").isJsonNull()) {
                        return entry.get("value").getAsDouble();
                    }
                }
            }
        }
        return Double.MIN_VALUE;
    }
}