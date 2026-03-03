package services.projets;

import models.projets.Projet;
import models.projets.donneesBusiness;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class IAScoringService {

    private static final String GROQ_KEY;
    private static final String API_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    static {
        String key = "";
        try (InputStream is = IAScoringService.class.getResourceAsStream("/secrets.properties")) {
            Properties p = new Properties();
            p.load(is);
            key = p.getProperty("groq.api_key", "");
        } catch (Exception e) { e.printStackTrace(); }
        GROQ_KEY = key;
    }

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Gson gson = new Gson();

    public static JsonObject genererScore(Projet projet) throws Exception {
        if (projet == null || projet.getDonneesBusiness() == null)
            throw new IllegalArgumentException("Projet ou données business manquantes");

        System.out.println("🧠 Analyse Groq IA pour: " + projet.getTitre());

        if (GROQ_KEY == null || GROQ_KEY.trim().isEmpty()) {
            System.err.println("⚠️ Variable d'environnement GROQ_API_KEY non définie !");
            System.out.println("🔄 Basculement vers analyse locale...");
            return fallbackIntelligent(projet);
        }

        String prompt = construirePromptUltraIntelligent(projet);

        try {
            JsonObject resultat = appelGroq(prompt);
            if (resultat != null && resultat.has("analyse")) {
                System.out.println("✅ Analyse Groq réussie");
                return resultat;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur Groq API: " + e.getMessage());
        }

        System.out.println("🔄 Basculement vers analyse locale...");
        return fallbackIntelligent(projet);
    }

    private static JsonObject appelGroq(String prompt) throws Exception {

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model",       MODEL);
        requestBody.addProperty("temperature", 0.35);
        requestBody.addProperty("max_tokens",  5000);
        requestBody.addProperty("top_p",       0.9);
        requestBody.add("messages",            messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + GROQ_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("🌐 Groq status: " + response.statusCode());

        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String text = json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            System.out.println("📝 Réponse Groq: " + text.length() + " chars");
            return extraireJson(text);
        }

        System.err.println("❌ Groq " + response.statusCode() + " : " + response.body());
        return null;
    }

    public static boolean testerConnexion() {
        try {
            if (GROQ_KEY == null || GROQ_KEY.trim().isEmpty()) {
                System.out.println("🔌 Groq: ❌ Clé API non définie (variable GROQ_API_KEY manquante)");
                return false;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/models"))
                    .header("Authorization", "Bearer " + GROQ_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() == 200;
            System.out.println("🔌 Groq: " + (ok ? "✅ OK — modèle " + MODEL : "❌ " + response.statusCode()));
            return ok;
        } catch (Exception e) {
            System.out.println("🔌 Groq: ❌ " + e.getMessage());
            return false;
        }
    }

    private static String construirePromptUltraIntelligent(Projet projet) {
        donneesBusiness db = projet.getDonneesBusiness();

        String titre       = safe(projet.getTitre(),       "Sans titre");
        String description = safe(projet.getDescription(), "(non fournie)");
        String secteur     = safe(projet.getSecteur(),     "(non fourni)");
        String etape       = safe(projet.getEtape(),       "(non fourni)");

        String marche  = safe(db.getTailleMarche(),  "(non renseigné)");
        String revenu  = safe(db.getModeleRevenu(),  "(non renseigné)");
        String risque  = safe(db.getNiveauRisque(),  "(non renseigné)");

        String coutsTexte   = safe(db.getRawCouts(),        null);
        String revenusTexte = safe(db.getRawRevenus(),      null);
        String equipeTexte  = safe(db.getRawForceEquipe(),  null);

        if (coutsTexte == null)
            coutsTexte = db.getCoutsEstimes() > 0 ? String.format("%.0f DT", db.getCoutsEstimes()) : "(non renseigné)";
        if (revenusTexte == null)
            revenusTexte = db.getRevenusAttendus() > 0 ? String.format("%.0f DT", db.getRevenusAttendus()) : "(non renseigné)";
        if (equipeTexte == null)
            equipeTexte = db.getForceEquipe() > 0 ? db.getForceEquipe() + "/10" : "(non renseigné)";

        String macro      = WorldBankService.getContexteMacroEconomique(secteur);
        String actualites = NewsService.getResumePourGemini(secteur);

        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert startup de niveau YCombinator / McKinsey, conseiller principal du programme NAJAHNI en Tunisie.\n");
        sb.append("Tu vas analyser un projet entrepreneurial. L'entrepreneur a rempli un formulaire LIBREMENT.\n\n");

        sb.append("════ RÈGLES ABSOLUES ════\n");
        sb.append("1. CHAQUE champ peut contenir : un nombre, du texte, une question, une hésitation, ou du langage naturel.\n");
        sb.append("   → Tu LIS tout, tu COMPRENDS tout, tu RÉPONDS à tout.\n");
        sb.append("2. Si un champ dit \"je sais pas\", \"estimez\", \"je ne sais pas encore\", \"unknown\" ou similaire :\n");
        sb.append("   → Tu ESTIMES toi-même une valeur réaliste basée sur le secteur et le stade, et tu l'utilises dans l'analyse.\n");
        sb.append("   → Tu expliques ton estimation dans la section 'reponses_questions'.\n");
        sb.append("3. Si un champ contient une QUESTION (\"combien dois-je prévoir ?\", \"quel modèle me conseilles-tu ?\") :\n");
        sb.append("   → Tu réponds avec une recommandation chiffrée et précise.\n");
        sb.append("4. JAMAIS de \"données manquantes\" pour des champs où l'utilisateur a exprimé une intention ou une question.\n");
        sb.append("   Seuls les champs VIDES (non renseignés du tout) peuvent aller dans donnees_manquantes.\n");
        sb.append("5. Sois aussi intelligent et utile que possible. Cite des chiffres, des noms, des alternatives.\n");
        sb.append("6. Parle à l'entrepreneur comme son mentor — direct, bienveillant, actionnable.\n\n");

        sb.append("════ SAISIE EXACTE DE L'ENTREPRENEUR ════\n");
        sb.append("(Ce qui suit est exactement ce qu'il a tapé — ne filtre rien)\n\n");
        sb.append("  Titre du projet   : ").append(titre).append("\n");
        sb.append("  Description       : ").append(description).append("\n");
        sb.append("  Secteur           : ").append(secteur).append("\n");
        sb.append("  Stade / Étape     : ").append(etape).append("\n");
        sb.append("  Taille du marché  : ").append(marche).append("\n");
        sb.append("  Modèle de revenu  : ").append(revenu).append("\n");
        sb.append("  Coûts estimés     : ").append(coutsTexte).append("\n");
        sb.append("  Revenus attendus  : ").append(revenusTexte).append("\n");
        sb.append("  Niveau de risque  : ").append(risque).append("\n");
        sb.append("  Force de l'équipe : ").append(equipeTexte).append("\n\n");

        if (db.getCoutsEstimes() > 0 && db.getRevenusAttendus() > 0) {
            sb.append("  [Indicateurs calculés automatiquement]\n");
            sb.append("  Marge brute       : ").append(String.format("%.0f DT", db.getMargeEstimee())).append("\n");
            sb.append("  Ratio R/C         : ").append(String.format("%.2fx", db.getRatioRentabilite())).append("\n\n");
        }

        sb.append("════ CONTEXTE MACRO TUNISIE ════\n").append(macro).append("\n");
        sb.append("════ ACTUALITÉS SECTEUR ════\n").append(actualites.isEmpty() ? "(Non disponible)\n" : actualites).append("\n");

        sb.append("════ TON ANALYSE ════\n");
        sb.append("Réponds UNIQUEMENT avec ce JSON valide et complet. Pas de texte avant, pas de markdown.\n\n");
        sb.append("{\n  \"analyse\": {\n");

        sb.append("    \"resume\": \"5-6 phrases. Résume le projet, cite le titre, mentionne les valeurs estimées (même si tu les as estimées toi-même), donne le positionnement tunisien. Si l'entrepreneur a posé des questions, confirme que tu y réponds dans l'analyse.\",\n\n");
        sb.append("    \"reponses_questions\": \"IMPORTANT : si l'entrepreneur a écrit une hésitation ou une question dans n'importe quel champ (coûts, revenus, modèle, équipe...), réponds ici de façon détaillée et chiffrée. Si aucune question, laisse une chaîne vide.\",\n\n");
        sb.append("    \"comprehension_idee\": \"Analyse en 8 points numérotés : (1) Problème résolu, (2) Solution, (3) Unicité, (4) Cible client, (5) Adéquation marché-produit, (6) Faisabilité, (7) Scalabilité Maghreb/Afrique, (8) Risques principaux.\",\n\n");
        sb.append("    \"forces\": [\"Force 1 avec données spécifiques\", \"Force 2\", \"Force 3\", \"Force 4 (minimum 4)\"],\n\n");
        sb.append("    \"faiblesses\": [\"Faiblesse 1 avec impact estimé\", \"Faiblesse 2\", \"Faiblesse 3 (minimum 3)\"],\n\n");
        sb.append("    \"donnees_manquantes\": [\"Uniquement les champs laissés VIDES par l'entrepreneur.\"],\n\n");
        sb.append("    \"analyse_concurrentielle\": \"4-5 concurrents réels en Tunisie/Maghreb dans ").append(secteur).append(". Nom, positionnement, forces/faiblesses, et comment ").append(titre).append(" se différencie.\",\n\n");
        sb.append("    \"potentiel_croissance\": \"3 scénarios chiffrés sur 3 ans. Inclus les jalons clés et le potentiel d'expansion Maghreb.\",\n\n");
        sb.append("    \"recommandations_courtes\": [\"Action 0-30j avec critère mesurable\", \"Action 30-60j\", \"Action 60-90j\", \"Action 90j+ (minimum 4)\"],\n\n");
        sb.append("    \"recommandations_long_terme\": [\"Stratégie 6-12 mois\", \"Stratégie 1-2 ans\", \"Stratégie 3-5 ans\"],\n\n");
        sb.append("    \"opportunites\": \"3-4 opportunités concrètes basées sur les actualités sectorielles, Startup Act, Smart Capital, gaps marché tunisien.\",\n\n");
        sb.append("    \"menaces\": \"3-4 menaces réelles (réglementaires, concurrentielles, macro) avec stratégie de mitigation pour chacune.\",\n\n");
        sb.append("    \"estimation_investissement\": \"Budget de lancement recommandé avec répartition (Tech/Produit X%, Marketing Y%, Ops Z%, Équipe W%) et sources de financement adaptées en Tunisie.\",\n\n");
        sb.append("    \"conseil_jury\": \"4-5 phrases directes à l'équipe de ").append(titre).append(". Cite le projet par son nom. Identifie le 1 truc qui peut tout changer.\",\n\n");
        sb.append("    \"score\": <0-100. Finance 35pts + Marché 20pts + Équipe 20pts + Risque 15pts + Dossier 10pts.>,\n\n");
        sb.append("    \"justification_score\": \"Finance X/35 + Marché X/20 + Équipe X/20 + Risque X/15 + Dossier X/10 = Total/100. Explique chaque note.\",\n\n");
        sb.append("    \"verdict\": \"INCOMPLET | RISQUE | MOYEN | BON | TRES_BON | EXCELLENT\"\n");
        sb.append("  }\n}\n");

        return sb.toString();
    }

    private static JsonObject extraireJson(String texte) {
        try {
            String s = texte.trim();
            if (s.startsWith("```json")) s = s.substring(7);
            else if (s.startsWith("```"))  s = s.substring(3);
            if (s.endsWith("```"))         s = s.substring(0, s.length() - 3);
            s = s.trim();
            int debut = s.indexOf('{');
            int fin   = s.lastIndexOf('}') + 1;
            if (debut >= 0 && fin > debut)
                return JsonParser.parseString(s.substring(debut, fin)).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("⚠️ Erreur extraction JSON: " + e.getMessage());
        }
        return null;
    }

    private static JsonObject fallbackIntelligent(Projet projet) {
        donneesBusiness db = projet.getDonneesBusiness();
        JsonObject result  = new JsonObject();
        JsonObject analyse = new JsonObject();

        String titre   = safe(projet.getTitre(),   "ce projet");
        String secteur = safe(projet.getSecteur(), "votre secteur");
        String etape   = safe(projet.getEtape(),   "l'étape actuelle");

        String coutsRaw   = safe(db.getRawCouts(),       null);
        String revenusRaw = safe(db.getRawRevenus(),     null);
        String equipeRaw  = safe(db.getRawForceEquipe(), null);
        String marche     = safe(db.getTailleMarche(),   null);
        String revenu     = safe(db.getModeleRevenu(),   null);
        String risque     = safe(db.getNiveauRisque(),   null);

        boolean hasNumericFinancials       = db.getCoutsEstimes() > 0 && db.getRevenusAttendus() > 0;
        boolean userAskedToEstimateCouts   = isAskingForEstimate(coutsRaw);
        boolean userAskedToEstimateRevenus = isAskingForEstimate(revenusRaw);
        // ✅ FIX: on considère que si L'UN OU L'AUTRE champ financier est une demande d'estimation,
        //         il ne faut PAS afficher "Données financières non chiffrées"
        boolean userAskedAnyFinancialEstimate = userAskedToEstimateCouts || userAskedToEstimateRevenus;

        double ratio  = db.getRatioRentabilite();
        double marge  = db.getMargeEstimee();
        int    equipe = db.getForceEquipe();
        double score  = calculerScore(db, coutsRaw, revenusRaw);

        // ── Résumé ──
        String resume;
        if (hasNumericFinancials) {
            resume = String.format(
                    "\"%s\" est un projet au stade %s dans le secteur %s. " +
                            "Financièrement : ratio R/C de %.2fx (%.0f DT de revenus pour %.0f DT de coûts, marge %.0f DT). " +
                            "Modèle : %s. Marché : %s. Équipe : %s. Score NAJAHNI : %.0f/100.",
                    titre, etape, secteur, ratio, db.getRevenusAttendus(), db.getCoutsEstimes(), marge,
                    revenu != null ? revenu : "à définir",
                    marche != null ? marche : "à quantifier",
                    equipe > 0 ? equipe + "/10" : equipeRaw != null ? equipeRaw : "à évaluer",
                    score);
        } else {
            String estimCouts   = estimerBudgetSecteur(secteur, etape, "couts");
            String estimRevenus = estimerBudgetSecteur(secteur, etape, "revenus");
            resume = String.format(
                    "\"%s\" est un projet au stade %s dans le secteur %s. " +
                            "Les données financières ont été saisies de façon ouverte (%s / %s) — " +
                            "estimation pour ce type de projet en Tunisie : budget lancement %s, revenus cibles %s. " +
                            "Modèle : %s. Marché : %s. Score NAJAHNI : %.0f/100.",
                    titre, etape, secteur,
                    coutsRaw != null ? "\"" + coutsRaw + "\"" : "non renseigné",
                    revenusRaw != null ? "\"" + revenusRaw + "\"" : "non renseigné",
                    estimCouts, estimRevenus,
                    revenu != null ? revenu : "à définir",
                    marche != null ? marche : "à quantifier",
                    score);
        }

        // ── Réponses aux questions ──
        StringBuilder repQ = new StringBuilder();
        if (userAskedToEstimateCouts || userAskedToEstimateRevenus || isAskingForEstimate(equipeRaw)) {
            repQ.append("L'entrepreneur a demandé des estimations — voici mes recommandations :\n\n");
            if (userAskedToEstimateCouts) {
                repQ.append("💰 BUDGET DE LANCEMENT (\"").append(coutsRaw).append("\") :\n");
                repQ.append(detaillerBudgetLancement(secteur, etape)).append("\n\n");
            }
            if (userAskedToEstimateRevenus) {
                repQ.append("📈 REVENUS ATTENDUS (\"").append(revenusRaw).append("\") :\n");
                repQ.append(detaillerRevenusEstimes(secteur, etape)).append("\n\n");
            }
            if (isAskingForEstimate(equipeRaw)) {
                repQ.append("👥 ÉQUIPE RECOMMANDÉE (\"").append(equipeRaw).append("\") :\n");
                repQ.append(String.format(
                        "Pour un projet %s au stade %s : équipe idéale de 2-4 personnes. " +
                                "Profils clés : (1) CEO/Business — réseau et ventes, (2) CTO/Tech — produit, " +
                                "(3) Marketing/Growth optionnel. Force équipe estimée : 6/10 si vous avez les 2 premiers profils.",
                        secteur, etape));
            }
        }

        // ── Forces ──
        java.util.List<String> forces = new java.util.ArrayList<>();
        if (hasNumericFinancials && ratio >= 2)
            forces.add(String.format("Excellent ratio R/C de %.2fx — modèle économique solide", ratio));
        else if (hasNumericFinancials && ratio >= 1)
            forces.add(String.format("Équilibre financier atteint (ratio %.2f) — modèle viable", ratio));
        if (userAskedAnyFinancialEstimate)
            forces.add("Démarche ouverte et honnête : l'entrepreneur cherche des estimations réalistes");
        if (equipe >= 7)
            forces.add(String.format("Équipe forte (%d/10) — facteur #1 que regardent les investisseurs NAJAHNI", equipe));
        else if (equipe >= 5)
            forces.add(String.format("Équipe compétente (%d/10) pour le stade %s", equipe, etape));
        if (marche != null)
            forces.add(String.format("Vision marché identifiée (%s) dans %s", marche, secteur));
        if (revenu != null)
            forces.add(String.format("Modèle de revenu articulé (%s) — crédibilité business", revenu));
        String desc = safe(projet.getDescription(), "");
        if (desc.length() > 150)
            forces.add(String.format("Description détaillée (%d chars) — maturité de réflexion visible", desc.length()));
        if (forces.isEmpty())
            forces.add(String.format("Initiative dans %s — secteur à fort potentiel en Tunisie", secteur));

        // ── Faiblesses ──
        java.util.List<String> faiblesses = new java.util.ArrayList<>();

        // ✅ FIX PRINCIPAL : ne PAS afficher cette faiblesse si l'utilisateur a demandé une estimation
        //    dans n'importe quel champ financier (coûts OU revenus)
        if (!hasNumericFinancials && !userAskedAnyFinancialEstimate)
            faiblesses.add("Données financières non chiffrées — le jury NAJAHNI exige des projections numériques");

        if (hasNumericFinancials && ratio < 1)
            faiblesses.add(String.format("Modèle déficitaire (ratio %.2f) — les coûts dépassent les revenus projetés", ratio));
        if (desc.length() < 80)
            faiblesses.add("Description trop courte — étoffer avec : problème, solution, cible, différenciation");
        if (equipe < 5 && equipe > 0)
            faiblesses.add(String.format("Équipe sous-dimensionnée (%d/10) pour lancer dans %s", equipe, secteur));
        if (revenu == null)
            faiblesses.add("Modèle de revenu non défini — comment les DT entrent dans la caisse ?");
        if (faiblesses.isEmpty())
            faiblesses.add(String.format("Dossier solide pour le stade %s — ajustements mineurs avant jury", etape));

        // ── Données manquantes (SEULEMENT les vrais vides) ──
        java.util.List<String> manquants = new java.util.ArrayList<>();
        if (!hasNumericFinancials && !userAskedToEstimateCouts && (coutsRaw == null || coutsRaw.isEmpty()))
            manquants.add("Coûts estimés (laissé vide — une estimation ou une question suffit)");
        if (!hasNumericFinancials && !userAskedToEstimateRevenus && (revenusRaw == null || revenusRaw.isEmpty()))
            manquants.add("Revenus attendus (laissé vide)");
        if (marche == null) manquants.add("Taille de marché (laissé vide)");
        if (revenu == null) manquants.add("Modèle de revenu (laissé vide)");
        if (risque == null) manquants.add("Niveau de risque (laissé vide)");
        if (equipe <= 0 && (equipeRaw == null || equipeRaw.isEmpty()))
            manquants.add("Force équipe (laissé vide)");

        // ── Concurrentielle ──
        String concurrentielle = buildConcurrentielle(secteur, titre);

        // ── Potentiel ──
        String estimCouts   = hasNumericFinancials ? String.format("%.0f DT de coûts", db.getCoutsEstimes())      : estimerBudgetSecteur(secteur, etape, "couts");
        String estimRevenus = hasNumericFinancials ? String.format("%.0f DT de revenus", db.getRevenusAttendus())  : estimerBudgetSecteur(secteur, etape, "revenus");
        String potentiel = String.format(
                "Projections pour \"%s\" (%s, %s) basées sur %s → %s :\n\n" +
                        "• Scénario conservateur (An 1) : 30-40%% des revenus cibles, validation marché\n" +
                        "• Scénario de base     (An 2) : 100%% des revenus cibles, modèle validé\n" +
                        "• Scénario optimiste   (An 3) : 150-200%%, expansion Maghreb (Maroc, Algérie)\n\n" +
                        "Jalons : MVP → 5 clients pilotes → 1er revenu récurrent → lever 2-3x les coûts",
                titre, secteur, etape, estimCouts, estimRevenus);

        // ── Recommandations courtes ──
        java.util.List<String> recosCourt = new java.util.ArrayList<>();
        if (userAskedAnyFinancialEstimate)
            recosCourt.add("URGENT (0-30j) : Valider les estimations budgétaires avec 5 devis réels");
        if (desc.length() < 150)
            recosCourt.add(String.format("(0-30j) Enrichir la description de \"%s\" : Problème → Solution → Cible → Différenciation → Revenus", titre));
        recosCourt.add(String.format("(30-60j) 10 entretiens clients dans %s pour valider les hypothèses", secteur));
        recosCourt.add(String.format("(60-90j) Prototype ou maquette Figma de \"%s\"", titre));
        recosCourt.add("(90j+) Déposer dossier Startup Act sur startup.gov.tn + contacter Smart Capital");

        String[] recosLong = {
                String.format("6-12 mois : Rejoindre Flat6Labs Tunis ou Cogite — accélération %s. Viser 10 clients payants.", secteur),
                "1-2 ans : Lever 2-3x les coûts opérationnels une fois le modèle validé. Recruter commercial et CTO si manquant.",
                "3-5 ans : Expansion Maghreb (Maroc, Algérie) puis Afrique francophone."
        };

        String opportunites = String.format(
                "• Startup Act 2019 : 4 ans d'exonération IS — s'inscrire sur startup.gov.tn dès maintenant\n" +
                        "• Smart Capital : fonds 50K-500K DT pour startups tunisiennes\n" +
                        "• Flat6Labs Tunis : programme d'accélération avec ticket 25K$ dans %s\n" +
                        "• Marché maghrébin : 100M+ habitants, Maroc et Algérie peu couverts dans %s",
                secteur, secteur);

        String menaces = String.format(
                "• Copie rapide : déposer la marque à l'INNORPI maintenant pour protéger \"%s\"\n" +
                        "• Réglementation %s : anticiper les agréments (BCT/Ministère, 6-18 mois)\n" +
                        "• Inflation tunisienne ~7%% : impact sur coûts opérationnels → marge de sécurité 25%%",
                titre, secteur);

        String conseilJury = String.format(
                "À l'équipe de \"%s\" : vous avez une idée solide pour le secteur %s en Tunisie. " +
                        "%s" +
                        "Le jury NAJAHNI cherche des fondateurs qui connaissent leurs chiffres ET leur marché — préparez les deux. " +
                        "Le truc qui peut tout changer : %s. On y croit !",
                titre, secteur,
                userAskedAnyFinancialEstimate
                        ? "Vous avez eu le réflexe de demander des estimations — bonne démarche. Utilisez ce rapport pour préparer votre pitch. "
                        : (hasNumericFinancials && ratio >= 2 ? String.format("Votre ratio de %.2fx est votre meilleur argument. ", ratio) : "Chiffrez vos projections avant le jury. "),
                manquants.isEmpty()
                        ? "aller valider le marché avec de vrais clients payants cette semaine"
                        : "compléter les informations manquantes et préparer un pitch de 3 minutes");

        String estInvest = hasNumericFinancials
                ? String.format(
                "Budget confirmé : %.0f DT. Répartition : Produit/Tech 40%% (%.0f DT), " +
                        "Marketing 25%% (%.0f DT), Opérations 20%% (%.0f DT), Équipe 15%% (%.0f DT).",
                db.getCoutsEstimes(),
                db.getCoutsEstimes() * 0.40,
                db.getCoutsEstimes() * 0.25,
                db.getCoutsEstimes() * 0.20,
                db.getCoutsEstimes() * 0.15)
                : detaillerBudgetLancement(secteur, etape);

        String justification = String.format(
                "Score \"%s\" :\n• Finance (35pts) : %s\n• Marché (20pts) : %s\n• Équipe (20pts) : %s\n• Risque (15pts) : %s\n• Dossier (10pts) : %s\nTotal : %.0f/100",
                titre,
                hasNumericFinancials ? String.format("ratio %.2f → %dpts", ratio, ratio >= 3 ? 32 : ratio >= 2 ? 25 : ratio >= 1.5 ? 18 : 10) : (userAskedAnyFinancialEstimate ? "estimation demandée → 8pts" : "non renseigné → 3pts"),
                marche != null ? "marché identifié → 13pts" : "non défini → 3pts",
                equipe > 0 ? String.format("%d/10 → %dpts", equipe, Math.min(20, equipe * 2)) : "estimé → 8pts",
                risque != null ? (risque.toLowerCase().contains("faible") ? "faible → 15pts" : risque.toLowerCase().contains("moyen") ? "moyen → 10pts" : "élevé → 4pts") : "estimé → 7pts",
                desc.length() > 200 ? "riche → 9pts" : desc.length() > 80 ? "correct → 6pts" : "court → 2pts",
                score);

        String verdict = manquants.size() >= 4 ? "INCOMPLET" :
                score >= 80 ? "EXCELLENT" : score >= 65 ? "TRES_BON" : score >= 50 ? "BON" :
                        score >= 35 ? "MOYEN" : "RISQUE";

        analyse.addProperty("resume",                    resume);
        analyse.addProperty("reponses_questions",        repQ.toString());
        analyse.addProperty("comprehension_idee",        buildComprehensionIdee(projet, db));
        analyse.add("forces",                            gson.toJsonTree(forces.toArray(new String[0])));
        analyse.add("faiblesses",                        gson.toJsonTree(faiblesses.toArray(new String[0])));
        analyse.add("donnees_manquantes",                gson.toJsonTree(manquants.toArray(new String[0])));
        analyse.addProperty("analyse_concurrentielle",   concurrentielle);
        analyse.addProperty("potentiel_croissance",      potentiel);
        analyse.add("recommandations_courtes",           gson.toJsonTree(recosCourt.toArray(new String[0])));
        analyse.add("recommandations_long_terme",        gson.toJsonTree(recosLong));
        analyse.addProperty("opportunites",              opportunites);
        analyse.addProperty("menaces",                   menaces);
        analyse.addProperty("estimation_investissement", estInvest);
        analyse.addProperty("conseil_jury",              conseilJury);
        analyse.addProperty("score",                     Math.round(score * 10.0) / 10.0);
        analyse.addProperty("justification_score",       justification);
        analyse.addProperty("verdict",                   verdict);
        result.add("analyse", analyse);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static boolean isAskingForEstimate(String val) {
        if (val == null || val.trim().isEmpty()) return false;
        String v = val.toLowerCase().trim();
        return v.contains("sais pas") || v.contains("know")       || v.contains("estim")
                || v.contains("combien")  || v.contains("how much")   || v.contains("?")
                || v.contains("pas encore") || v.contains("not yet")  || v.contains("tbd")
                || v.contains("à définir") || v.contains("inconnu")   || v.contains("unknown")
                || v.contains("aide")     || v.contains("help")       || v.contains("conseil")
                || v.contains("suggère")  || v.contains("recommande") || v.contains("calculez")
                || v.contains("dites")    || v.contains("expliquez")  || v.equals("?");
    }

    private static String estimerBudgetSecteur(String secteur, String etape, String type) {
        String s = secteur != null ? secteur.toLowerCase() : "";
        boolean isCouts = "couts".equals(type);
        if (s.contains("tech") || s.contains("digit") || s.contains("logiciel") || s.contains("ia"))
            return isCouts ? "30 000 - 80 000 DT"  : "60 000 - 200 000 DT (An 1)";
        if (s.contains("sante") || s.contains("santé") || s.contains("medic"))
            return isCouts ? "50 000 - 150 000 DT" : "80 000 - 300 000 DT (An 1)";
        if (s.contains("agri"))
            return isCouts ? "20 000 - 60 000 DT"  : "40 000 - 120 000 DT (An 1)";
        if (s.contains("finance") || s.contains("fintech"))
            return isCouts ? "80 000 - 200 000 DT" : "150 000 - 500 000 DT (An 1)";
        if (s.contains("educ") || s.contains("formation"))
            return isCouts ? "15 000 - 50 000 DT"  : "30 000 - 100 000 DT (An 1)";
        return isCouts ? "20 000 - 100 000 DT" : "40 000 - 200 000 DT (An 1)";
    }

    private static String detaillerBudgetLancement(String secteur, String etape) {
        String fourchette = estimerBudgetSecteur(secteur, etape, "couts");
        return String.format(
                "Pour une startup %s au stade %s en Tunisie, budget recommandé : %s.\n" +
                        "Répartition suggérée :\n" +
                        "  • Développement produit/tech   : 40%%\n" +
                        "  • Marketing & acquisition      : 25%%\n" +
                        "  • Opérations & infrastructure  : 20%%\n" +
                        "  • Salaires fondateurs (6 mois) : 15%%\n\n" +
                        "Sources de financement Tunisie : Smart Capital (50K-200K DT), Flat6Labs (grant 25K$), " +
                        "Startup Act (avantages fiscaux), BFPME (prêt PME), love money.",
                secteur, etape, fourchette);
    }

    private static String detaillerRevenusEstimes(String secteur, String etape) {
        String fourchette = estimerBudgetSecteur(secteur, etape, "revenus");
        return String.format(
                "Pour une startup %s au stade %s en Tunisie, revenus estimés An 1 : %s.\n" +
                        "Scénario réaliste : 5-15 clients la première année à 500-3000 DT/mois selon le modèle.\n" +
                        "Conseil : viser la rentabilité opérationnelle dès le mois 18 maximum.",
                secteur, etape, fourchette);
    }

    private static String buildComprehensionIdee(Projet projet, donneesBusiness db) {
        String desc    = safe(projet.getDescription(), "");
        String descL   = desc.toLowerCase();
        String secteur = safe(projet.getSecteur(), "votre secteur");
        String titre   = safe(projet.getTitre(),   "ce projet");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Analyse de \"%s\" (%s) :\n\n", titre, secteur));
        sb.append("1. PROBLÈME    : ").append(descL.contains("problème") || descL.contains("besoin")
                ? "Identifié dans la description — bien articuler pour le jury."
                : "À expliciter — quel problème précis résolvez-vous ?").append("\n");
        sb.append("2. SOLUTION    : ").append(descL.contains("solution") || descL.contains("plateforme") || descL.contains("app")
                ? "Solution décrite — différencier des alternatives existantes."
                : "À préciser — comment votre solution est-elle concrètement différente ?").append("\n");
        sb.append("3. CIBLE       : ").append(descL.contains("client") || descL.contains("utilisateur") || descL.contains("entreprise")
                ? "Cible mentionnée — la quantifier (combien de clients potentiels en Tunisie ?)."
                : "Cible non définie — critique pour le jury NAJAHNI.").append("\n");
        sb.append("4. MODÈLE      : ").append(db.getModeleRevenu() != null
                ? db.getModeleRevenu() + " — modèle articulé."
                : "À définir — comment les DT entrent dans la caisse ?").append("\n");
        sb.append("5. MARCHÉ      : ").append(db.getTailleMarche() != null
                ? db.getTailleMarche() + " — potentiel tunisien à chiffrer."
                : "Non quantifié — estimer en DT ou % de marché.").append("\n");
        sb.append("6. FAISABILITÉ : ").append(descL.contains("tech") || descL.contains("dev") || descL.contains("équipe")
                ? "Compétences techniques évoquées."
                : "À démontrer — qui va construire le produit ?").append("\n");
        sb.append("7. SCALABILITÉ : ").append(secteur.toLowerCase().contains("tech") || secteur.toLowerCase().contains("digit")
                ? "Forte scalabilité digitale — potentiel Maghreb/Afrique réel."
                : "Scalabilité à démontrer selon le modèle opérationnel choisi.").append("\n");
        sb.append("8. RISQUES     : ").append(db.getNiveauRisque() != null
                ? "Risque " + db.getNiveauRisque() + " identifié — stratégie de mitigation à préparer."
                : "Risques non évalués — le jury posera cette question. Anticiper.");
        return sb.toString();
    }

    private static String buildConcurrentielle(String secteur, String titre) {
        String s = secteur != null ? secteur.toLowerCase() : "";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Paysage concurrentiel pour \"%s\" dans %s (Tunisie) :\n\n", titre, secteur));
        if (s.contains("tech") || s.contains("digit") || s.contains("logiciel") || s.contains("ia")) {
            sb.append("• SaaS internationaux (Monday, Notion, HubSpot) : présents mais non localisés\n");
            sb.append("• Freelances tunisiens : moins chers, sans scalabilité\n");
            sb.append("• Startups El Ghazala / IntilaQ : concurrence directe locale\n");
            sb.append("• Solution DIY (Excel, WhatsApp) : l'ennemi invisible\n\n");
            sb.append("Avantage de \"").append(titre).append("\" : pricing DT, support arabe/français, conformité droit tunisien.");
        } else if (s.contains("sante") || s.contains("santé")) {
            sb.append("• Portails CHU publics : gratuits mais obsolètes\n");
            sb.append("• Doctolib (France) : non déployé en Tunisie — gap stratégique\n");
            sb.append("• DabaDoc : présent au Maroc, absent en Tunisie\n");
            sb.append("• Médecins via Facebook/WhatsApp : pratique informelle dominante\n\n");
            sb.append("⚠️ Agrément Ministère Santé obligatoire (6-12 mois). Marché : 12M Tunisiens, 3500+ médecins.");
        } else if (s.contains("agri")) {
            sb.append("• Coopératives traditionnelles : faible digital\n");
            sb.append("• AgriMaroc : présent au Maroc, absent en Tunisie\n");
            sb.append("• Apps météo génériques : partiellement utilisées\n");
            sb.append("• Grossistes/intermédiaires : à disrupter\n\n");
            sb.append("Opportunité : marché non numérisé — avantage au premier entrant.");
        } else if (s.contains("finance") || s.contains("fintech")) {
            sb.append("• BIAT, Attijari, STB : apps bancaires fonctionnelles mais rigides\n");
            sb.append("• Orange Money / eDinar : mobile money basique\n");
            sb.append("• Flouci : startup tunisienne paiement mobile émergente\n\n");
            sb.append("⚠️ Agrément BCT obligatoire (12-18 mois). Bac à sable réglementaire disponible.");
        } else if (s.contains("educ") || s.contains("formation")) {
            sb.append("• GoMyCode : leader EdTech tunisien (coding), 15 pays\n");
            sb.append("• Coursera / Udemy : en anglais, non localisés\n");
            sb.append("• Cours particuliers : marché informel ~200M DT/an\n\n");
            sb.append("Différenciation : contenu en arabe tunisien + alignement programme national.");
        } else {
            sb.append(String.format("• Opérateurs traditionnels dans %s : peu digitalisés\n", secteur));
            sb.append("• Startups régionales (Maroc, Algérie) pourraient s'étendre\n");
            sb.append("• Solutions importées non adaptées au contexte tunisien\n\n");
            sb.append(String.format("Avantage de \"%s\" : ancrage local, pricing DT, réseau tunisien.", titre));
        }
        return sb.toString();
    }

    private static double calculerScore(donneesBusiness db, String coutsRaw, String revenusRaw) {
        double score = 0;
        if (db.getCoutsEstimes() > 0 && db.getRevenusAttendus() > 0) {
            double r = db.getRatioRentabilite();
            score += r >= 3 ? 32 : r >= 2 ? 25 : r >= 1.5 ? 18 : r >= 1 ? 10 : 3;
            double m = db.getMargeEstimee();
            score += m > 100000 ? 3 : m > 50000 ? 2 : m > 0 ? 1 : 0;
        } else if (isAskingForEstimate(coutsRaw) || isAskingForEstimate(revenusRaw)) {
            score += 8;
        } else {
            score += 3;
        }
        String tm = db.getTailleMarche();
        if (tm != null) {
            String t = tm.toLowerCase();
            score += t.contains("grand") || t.contains("national") || t.contains("large") ? 20 :
                    t.contains("moyen") || t.contains("medium")   || t.contains("régional") ? 13 :
                            t.contains("petit") || t.contains("niche") ? 8 : 7;
        }
        score += Math.min(20, db.getForceEquipe() * 2);
        if (isAskingForEstimate(db.getRawForceEquipe())) score += 8;
        String nr = db.getNiveauRisque();
        if (nr != null) {
            String r = nr.toLowerCase();
            score += r.contains("faible") || r.contains("low") ? 15 :
                    r.contains("moyen")  ? 10 :
                            r.contains("elev")   || r.contains("high") ? 4 : 7;
        } else {
            score += 5;
        }
        return Math.max(10, Math.min(100, score));
    }

    private static String safe(String val, String defaut) {
        if (val == null || val.trim().isEmpty()) return defaut;
        return val.trim();
    }

    public static JsonObject genererScoreSimple(Projet projet) {
        return fallbackIntelligent(projet);
    }

    public static JsonObject diagnostiquerFaiblesses(Projet projet) {
        JsonObject d = new JsonObject();
        double score = calculerScore(projet.getDonneesBusiness(), null, null);
        d.addProperty("diagnostic",    "Score: " + score);
        d.addProperty("score_global",  score);
        d.addProperty("niveau_risque", score > 70 ? "FAIBLE" : score > 50 ? "MOYEN" : "ÉLEVÉ");
        return d;
    }
}