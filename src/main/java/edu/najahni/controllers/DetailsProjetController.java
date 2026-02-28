package edu.najahni.controllers;

import edu.najahni.entities.Projet;
import edu.najahni.entities.StatutProjet;
import edu.najahni.entities.donneesBusiness;
import edu.najahni.services.IAScoringService;
import edu.najahni.services.NewsService;
import edu.najahni.services.PdfRapportService;
import edu.najahni.services.WorldBankService;
import edu.najahni.services.projetCRUD;

import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DetailsProjetController {

    // ── Labels ────────────────────────────────────────────────────
    @FXML private Label lblTitre;
    @FXML private Label lblStatut;
    @FXML private Label lblSecteur;
    @FXML private Label lblEtape;
    @FXML private Label lblDateCreation;
    @FXML private Label lblDescription;

    @FXML private Label lblTailleMarche;
    @FXML private Label lblModeleRevenu;
    @FXML private Label lblCouts;
    @FXML private Label lblRevenus;
    @FXML private Label lblMarge;
    @FXML private Label lblRatio;
    @FXML private Label lblForceEquipe;
    @FXML private Label lblNiveauRisque;

    @FXML private Label lblScoreMarche;
    @FXML private Label lblScoreFinancier;
    @FXML private Label lblScoreEquipe;
    @FXML private Label lblScoreRisque;

    @FXML private Label lblScoreGlobal;
    @FXML private ProgressBar progressScore;
    @FXML private Label lblCategorie;
    @FXML private Label lblMessage;

    @FXML private Button btnFermer;
    @FXML private Button btnTelechargerPdf;
    @FXML private VBox iaSection;
    @FXML private VBox rapportContainer;
    @FXML private VBox loadingBox;
    @FXML private Label lblLoading;
    @FXML private VBox verdictBox;
    @FXML private HBox verdictHeader;
    @FXML private Label lblVerdictText;
    @FXML private Label lblScoreVerdictFinal;

    // ── Attributs ─────────────────────────────────────────────────
    private Projet projet;
    private int connectedUserId;
    private projetCRUD projetService = new projetCRUD();
    private JsonObject derniereAnalyse = null;

    // ═══════════════════════════════════════════════════════════════
    //  setProjet
    // ═══════════════════════════════════════════════════════════════
    public void setProjet(Projet projet) {
        this.projet = projet;
        afficherDetails();

        if (projet.getStatutProjet() == StatutProjet.SOUMIS) {
            lancerEvaluationIA();
        } else if (projet.getStatutProjet() == StatutProjet.EVALUE) {
            Platform.runLater(() -> {
                JsonObject analyseReconstruite = reconstruireAnalyseDepuisDiagnostic(projet.getDiagnosticIA());
                if (analyseReconstruite != null) {
                    derniereAnalyse = analyseReconstruite;
                    afficherRapportStylee(analyseReconstruite);
                    String verdict = extraireVerdict(analyseReconstruite, projet.getScoreGlobal());
                    afficherVerdictFinal(verdict, projet.getScoreGlobal());
                    afficherBoutonPdf(true);
                } else {
                    lancerEvaluationIA();
                }
            });
        }
    }

    public void setConnectedUserId(int userId) {
        this.connectedUserId = userId;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Bouton PDF
    // ═══════════════════════════════════════════════════════════════
    @FXML
    private void telechargerPdf() {
        if (derniereAnalyse == null) {
            new Alert(Alert.AlertType.WARNING,
                    "L'analyse IA n'est pas encore terminée.\nVeuillez patienter.").showAndWait();
            return;
        }

        btnTelechargerPdf.setDisable(true);
        btnTelechargerPdf.setText("⏳  Génération en cours...");

        new Thread(() -> {
            try {
                JsonObject wrapper = derniereAnalyse.has("analyse")
                        ? derniereAnalyse : new JsonObject();
                if (!derniereAnalyse.has("analyse"))
                    wrapper.add("analyse", derniereAnalyse);

                String cheminPdf = PdfRapportService.genererPdfStatique(projet, wrapper);

                Platform.runLater(() -> {
                    btnTelechargerPdf.setDisable(false);
                    btnTelechargerPdf.setText("📄  Télécharger le Rapport PDF");

                    FileChooser fc = new FileChooser();
                    fc.setTitle("Enregistrer le rapport PDF");
                    fc.setInitialFileName("rapport_" + sanitiser(projet.getTitre()) + ".pdf");
                    fc.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

                    File dest = fc.showSaveDialog(
                            (Stage) btnTelechargerPdf.getScene().getWindow());

                    if (dest != null) {
                        try {
                            Files.copy(new File(cheminPdf).toPath(),
                                    dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            if (Desktop.isDesktopSupported())
                                Desktop.getDesktop().open(dest);
                            new Alert(Alert.AlertType.INFORMATION,
                                    "✅ PDF sauvegardé :\n" + dest.getAbsolutePath()).showAndWait();
                        } catch (Exception e) {
                            new Alert(Alert.AlertType.ERROR,
                                    "Erreur copie fichier :\n" + e.getMessage()).showAndWait();
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnTelechargerPdf.setDisable(false);
                    btnTelechargerPdf.setText("📄  Télécharger le Rapport PDF");
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur génération PDF :\n" + e.getMessage()).showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  lancerEvaluationIA
    // ═══════════════════════════════════════════════════════════════
    private void lancerEvaluationIA() {
        Thread thread = new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    lblMessage.setText("⏳ Analyse IA en cours...");
                    if (lblLoading != null) lblLoading.setText("🤖 Gemini AI analyse votre projet en détail...");
                });

                JsonObject resultat;
                boolean connexionOK = IAScoringService.testerConnexion();

                if (!connexionOK) {
                    Platform.runLater(() -> lblMessage.setText("⚠️ Mode analyse locale"));
                    resultat = IAScoringService.genererScoreSimple(projet);
                } else {
                    resultat = IAScoringService.genererScore(projet);
                }

                if (resultat != null && resultat.has("analyse")) {
                    JsonObject analyse = resultat.getAsJsonObject("analyse");

                    if (analyse.has("score")) {
                        final double score        = analyse.get("score").getAsDouble();
                        final String diagTexte    = buildDiagnosticTexte(analyse);
                        final JsonObject analyseFinal = analyse;

                        derniereAnalyse = analyseFinal;
                        projet.setScoreGlobal(score);
                        projet.setDiagnosticIA(diagTexte);
                        projetService.mettreAJourScore(projet.getId(), score, diagTexte);
                        projet.setStatutProjet(StatutProjet.EVALUE);

                        Platform.runLater(() -> {
                            afficherDetails();
                            afficherRapportStylee(analyseFinal);
                            String verdict = extraireVerdict(analyseFinal, score);
                            afficherVerdictFinal(verdict, score);
                            lblMessage.setText("✅ Évaluation terminée !");
                            afficherBoutonPdf(true);
                        });
                    } else {
                        Platform.runLater(() -> lblMessage.setText("❌ Score manquant dans la réponse"));
                    }
                } else {
                    Platform.runLater(() -> lblMessage.setText("❌ Réponse invalide — analyse impossible"));
                }

            } catch (Exception e) {
                Platform.runLater(() -> lblMessage.setText("❌ Erreur: " + e.getMessage()));
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════════════════
    //  buildDiagnosticTexte — CORRIGÉ : inclut analyse_concurrentielle
    //  et potentiel_croissance
    // ═══════════════════════════════════════════════════════════════
    private String buildDiagnosticTexte(JsonObject analyse) {
        StringBuilder sb = new StringBuilder();

        if (analyse.has("resume"))
            sb.append("RÉSUMÉ:\n").append(analyse.get("resume").getAsString()).append("\n\n");

        if (analyse.has("forces")) {
            sb.append("FORCES:\n");
            analyse.getAsJsonArray("forces").forEach(f -> sb.append("• ").append(f.getAsString()).append("\n"));
            sb.append("\n");
        }
        if (analyse.has("faiblesses")) {
            sb.append("FAIBLESSES:\n");
            analyse.getAsJsonArray("faiblesses").forEach(f -> sb.append("• ").append(f.getAsString()).append("\n"));
            sb.append("\n");
        }

        if (analyse.has("opportunites"))
            sb.append("OPPORTUNITÉS:\n").append(analyse.get("opportunites").getAsString()).append("\n\n");

        if (analyse.has("menaces"))
            sb.append("MENACES:\n").append(analyse.get("menaces").getAsString()).append("\n\n");

        // ✅ FIX : Analyse Concurrentielle sauvegardée
        if (analyse.has("analyse_concurrentielle") && !analyse.get("analyse_concurrentielle").getAsString().isEmpty())
            sb.append("ANALYSE CONCURRENTIELLE:\n")
                    .append(analyse.get("analyse_concurrentielle").getAsString()).append("\n\n");

        // ✅ FIX : Potentiel de Croissance sauvegardé
        if (analyse.has("potentiel_croissance") && !analyse.get("potentiel_croissance").getAsString().isEmpty())
            sb.append("POTENTIEL DE CROISSANCE:\n")
                    .append(analyse.get("potentiel_croissance").getAsString()).append("\n\n");

        if (analyse.has("recommandations_courtes")) {
            sb.append("RECOMMANDATIONS:\n");
            analyse.getAsJsonArray("recommandations_courtes").forEach(r -> sb.append("• ").append(r.getAsString()).append("\n"));
            sb.append("\n");
        }

        if (analyse.has("recommandations_long_terme")) {
            sb.append("STRATÉGIE LONG TERME:\n");
            analyse.getAsJsonArray("recommandations_long_terme").forEach(r -> sb.append("• ").append(r.getAsString()).append("\n"));
            sb.append("\n");
        }

        if (analyse.has("conseil_jury"))
            sb.append("CONSEIL JURY:\n").append(analyse.get("conseil_jury").getAsString()).append("\n\n");

        if (analyse.has("justification_score"))
            sb.append("JUSTIFICATION:\n").append(analyse.get("justification_score").getAsString()).append("\n\n");

        if (analyse.has("verdict")) {
            sb.append("VERDICT: ").append(analyse.get("verdict").getAsString());
            if (analyse.has("score"))
                sb.append(" | Score: ").append(String.format("%.1f/100", analyse.get("score").getAsDouble()));
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  reconstruireAnalyseDepuisDiagnostic — CORRIGÉ
    // ═══════════════════════════════════════════════════════════════
    private JsonObject reconstruireAnalyseDepuisDiagnostic(String texte) {
        if (texte == null || texte.trim().isEmpty()) return null;
        try {
            JsonObject analyse = new JsonObject();
            com.google.gson.Gson gson = new com.google.gson.Gson();

            analyse.addProperty("resume",             extraireSectionTexte(texte, "RÉSUMÉ:", "FORCES:"));
            analyse.addProperty("comprehension_idee", "");

            String forcesBloc = extraireSectionTexte(texte, "FORCES:", "FAIBLESSES:");
            analyse.add("forces", convertirBulletEnArray(forcesBloc, gson));

            String faiblessesBloc = extraireSectionTexte(texte, "FAIBLESSES:", "OPPORTUNITÉS:");
            if (faiblessesBloc.isEmpty())
                faiblessesBloc = extraireSectionTexte(texte, "FAIBLESSES:", "ANALYSE CONCURRENTIELLE:");
            if (faiblessesBloc.isEmpty())
                faiblessesBloc = extraireSectionTexte(texte, "FAIBLESSES:", "RECOMMANDATIONS:");
            analyse.add("faiblesses", convertirBulletEnArray(faiblessesBloc, gson));

            analyse.addProperty("opportunites", extraireSectionTexte(texte, "OPPORTUNITÉS:", "MENACES:"));

            String menaces = extraireSectionTexte(texte, "MENACES:", "ANALYSE CONCURRENTIELLE:");
            if (menaces.isEmpty()) menaces = extraireSectionTexte(texte, "MENACES:", "RECOMMANDATIONS:");
            analyse.addProperty("menaces", menaces);

            // ✅ FIX : Extraire Analyse Concurrentielle
            String analyseConcurrentielle = extraireSectionTexte(texte, "ANALYSE CONCURRENTIELLE:", "POTENTIEL DE CROISSANCE:");
            if (analyseConcurrentielle.isEmpty())
                analyseConcurrentielle = extraireSectionTexte(texte, "ANALYSE CONCURRENTIELLE:", "RECOMMANDATIONS:");
            analyse.addProperty("analyse_concurrentielle", analyseConcurrentielle);

            // ✅ FIX : Extraire Potentiel de Croissance
            String potentielCroissance = extraireSectionTexte(texte, "POTENTIEL DE CROISSANCE:", "RECOMMANDATIONS:");
            analyse.addProperty("potentiel_croissance", potentielCroissance);

            String recosBloc = extraireSectionTexte(texte, "RECOMMANDATIONS:", "STRATÉGIE LONG TERME:");
            if (recosBloc.isEmpty()) recosBloc = extraireSectionTexte(texte, "RECOMMANDATIONS:", "CONSEIL JURY:");
            analyse.add("recommandations_courtes", convertirBulletEnArray(recosBloc, gson));

            String ltBloc = extraireSectionTexte(texte, "STRATÉGIE LONG TERME:", "CONSEIL JURY:");
            analyse.add("recommandations_long_terme", convertirBulletEnArray(ltBloc, gson));

            analyse.addProperty("conseil_jury",
                    extraireSectionTexte(texte, "CONSEIL JURY:", "JUSTIFICATION:"));

            analyse.addProperty("justification_score",
                    extraireSectionTexte(texte, "JUSTIFICATION:", "VERDICT:"));

            double score   = projet.getScoreGlobal();
            String verdict = score >= 80 ? "EXCELLENT" : score >= 65 ? "TRES_BON"
                    : score >= 50 ? "BON" : score >= 35 ? "MOYEN" : score >= 20 ? "RISQUE" : "INCOMPLET";

            String verdictLigne = extraireSectionTexte(texte, "VERDICT:", null);
            if (!verdictLigne.isEmpty()) {
                String[] parts = verdictLigne.split("\\|");
                String v = parts[0].replace("VERDICT:", "").trim();
                if (!v.isEmpty()) verdict = v;
            }

            analyse.addProperty("score",   score);
            analyse.addProperty("verdict", verdict);
            analyse.add("donnees_manquantes", new com.google.gson.JsonArray());

            return analyse;
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de reconstruire l'analyse: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  afficherRapportStylee
    // ═══════════════════════════════════════════════════════════════
    private void afficherRapportStylee(JsonObject analyse) {
        if (rapportContainer == null) return;
        rapportContainer.getChildren().clear();
        if (loadingBox != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }

        if (analyse.has("resume") && !analyse.get("resume").getAsString().isEmpty())
            rapportContainer.getChildren().add(creerSectionResume(analyse.get("resume").getAsString()));

        String donneesMacro = WorldBankService.getContexteMacroEconomique(projet.getSecteur());
        if (donneesMacro != null && !donneesMacro.isEmpty())
            rapportContainer.getChildren().add(creerSectionWorldBank(donneesMacro));

        String repQuestions = analyse.has("reponses_questions") ? analyse.get("reponses_questions").getAsString() : "";
        if (!repQuestions.isEmpty())
            rapportContainer.getChildren().add(creerSectionTexte("💬  Réponses à vos Questions",
                    repQuestions, "#6c3483", "#f5eef8", "#d7bde2"));

        String ideeKey = analyse.has("comprehension_idee") ? "comprehension_idee" : "compréhension_idee";
        if (analyse.has(ideeKey) && !analyse.get(ideeKey).getAsString().isEmpty())
            rapportContainer.getChildren().add(creerSectionTexte("🔍  Analyse de l'Idée",
                    analyse.get(ideeKey).getAsString(), "#8e44ad", "#f9f4ff", "#d2b4de"));

        if (analyse.has("donnees_manquantes")) {
            var arr = analyse.getAsJsonArray("donnees_manquantes");
            if (arr.size() > 0) {
                java.util.List<String> items = new java.util.ArrayList<>();
                arr.forEach(e -> items.add(e.getAsString()));
                rapportContainer.getChildren().add(wrapWithPadding(
                        creerSectionListe("⛔  Données Manquantes (Critiques)", items, "#e74c3c", "#fff5f5", "#ffcccc"),
                        20, 24, 4, 24));
            }
        }

        HBox rowFF = creerHBoxAvecPadding(24, 4);
        if (analyse.has("forces") && analyse.getAsJsonArray("forces").size() > 0) {
            java.util.List<String> f = new java.util.ArrayList<>();
            analyse.getAsJsonArray("forces").forEach(e -> f.add(e.getAsString()));
            VBox box = creerSectionListe("✅  Forces du Projet", f, "#27ae60", "#f0fff4", "#a9dfbf");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowFF.getChildren().add(box);
        }
        if (analyse.has("faiblesses") && analyse.getAsJsonArray("faiblesses").size() > 0) {
            java.util.List<String> f = new java.util.ArrayList<>();
            analyse.getAsJsonArray("faiblesses").forEach(e -> f.add(e.getAsString()));
            VBox box = creerSectionListe("⚠️  Faiblesses à Corriger", f, "#e67e22", "#fffdf0", "#fde8a8");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowFF.getChildren().add(box);
        }
        if (!rowFF.getChildren().isEmpty()) rapportContainer.getChildren().add(rowFF);

        HBox rowOM = creerHBoxAvecPadding(24, 4);
        String opport = analyse.has("opportunites") ? analyse.get("opportunites").getAsString() : "";
        String menace = analyse.has("menaces")      ? analyse.get("menaces").getAsString()      : "";
        if (!opport.isEmpty()) {
            VBox box = creerSectionTexte("🌟  Opportunités de Marché", opport, "#16a085", "#eafaf1", "#a2d9ce");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowOM.getChildren().add(box);
        }
        if (!menace.isEmpty()) {
            VBox box = creerSectionTexte("⚡  Menaces & Risques", menace, "#c0392b", "#fdf2f2", "#f1948a");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowOM.getChildren().add(box);
        }
        if (!rowOM.getChildren().isEmpty()) rapportContainer.getChildren().add(rowOM);

        // ✅ Analyse Concurrentielle
        if (analyse.has("analyse_concurrentielle") && !analyse.get("analyse_concurrentielle").getAsString().isEmpty())
            rapportContainer.getChildren().add(creerSectionTexte("🏁  Analyse Concurrentielle",
                    analyse.get("analyse_concurrentielle").getAsString(), "#2980b9", "#eaf4fd", "#aed6f1"));

        // ✅ Potentiel de Croissance
        if (analyse.has("potentiel_croissance") && !analyse.get("potentiel_croissance").getAsString().isEmpty())
            rapportContainer.getChildren().add(creerSectionTexte("📈  Potentiel de Croissance",
                    analyse.get("potentiel_croissance").getAsString(), "#1abc9c", "#eafaf1", "#a2d9ce"));

        HBox rowRecos = creerHBoxAvecPadding(24, 4);
        if (analyse.has("recommandations_courtes") && analyse.getAsJsonArray("recommandations_courtes").size() > 0) {
            java.util.List<String> r = new java.util.ArrayList<>();
            analyse.getAsJsonArray("recommandations_courtes").forEach(e -> r.add(e.getAsString()));
            VBox box = creerSectionListe("🎯  Actions Immédiates (0–3 mois)", r, "#3498db", "#eaf4fd", "#aed6f1");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowRecos.getChildren().add(box);
        }
        if (analyse.has("recommandations_long_terme") && analyse.getAsJsonArray("recommandations_long_terme").size() > 0) {
            java.util.List<String> r = new java.util.ArrayList<>();
            analyse.getAsJsonArray("recommandations_long_terme").forEach(e -> r.add(e.getAsString()));
            VBox box = creerSectionListe("🚀  Stratégie Long Terme", r, "#9b59b6", "#f5eef8", "#d7bde2");
            HBox.setHgrow(box, Priority.ALWAYS);
            rowRecos.getChildren().add(box);
        }
        if (!rowRecos.getChildren().isEmpty()) rapportContainer.getChildren().add(rowRecos);

        List<NewsService.Article> news = NewsService.getActualitesSecteur(projet.getSecteur());
        if (!news.isEmpty())
            rapportContainer.getChildren().add(creerSectionActualites(news));

        if (analyse.has("justification_score") && !analyse.get("justification_score").getAsString().isEmpty())
            rapportContainer.getChildren().add(creerSectionJustification(analyse.get("justification_score").getAsString()));

        String conseilJury = analyse.has("conseil_jury") ? analyse.get("conseil_jury").getAsString() : "";
        if (!conseilJury.isEmpty())
            rapportContainer.getChildren().add(creerSectionConseilJury(conseilJury));

        String estInvest = analyse.has("estimation_investissement") ? analyse.get("estimation_investissement").getAsString() : "";
        if (!estInvest.isEmpty())
            rapportContainer.getChildren().add(creerSectionTexte("💰  Estimation Investissement Recommandée",
                    estInvest, "#16a085", "#e8f8f5", "#a2d9ce"));

        VBox spacer = new VBox();
        spacer.setPrefHeight(16);
        rapportContainer.getChildren().add(spacer);
    }

    // ═══════════════════════════════════════════════════════════════
    //  afficherDetails
    // ═══════════════════════════════════════════════════════════════
    private void afficherDetails() {
        if (projet == null) return;

        lblTitre.setText(projet.getTitre());
        lblStatut.setText(projet.getStatutProjet() != null ? projet.getStatutProjet().name() : "BROUILLON");
        lblSecteur.setText(projet.getSecteur()     != null ? projet.getSecteur()     : "—");
        lblEtape.setText(projet.getEtape()         != null ? projet.getEtape()       : "—");
        lblDateCreation.setText(projet.getDateCreation() != null ? projet.getDateCreation().toString() : "Non définie");
        lblDescription.setText(projet.getDescription()  != null ? projet.getDescription() : "Aucune description");

        donneesBusiness db = projet.getDonneesBusiness();
        if (db != null) {
            lblTailleMarche.setText(db.getTailleMarche()  != null ? db.getTailleMarche()  : "Non défini");
            lblModeleRevenu.setText(db.getModeleRevenu()  != null ? db.getModeleRevenu()  : "Non défini");
            lblNiveauRisque.setText(db.getNiveauRisque()  != null ? db.getNiveauRisque()  : "Non défini");

            if (db.getCoutsEstimes() > 0) {
                lblCouts.setText(String.format("%.0f DT", db.getCoutsEstimes()));
                lblCouts.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                lblCouts.setText("—");
                lblCouts.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }

            if (db.getRevenusAttendus() > 0) {
                lblRevenus.setText(String.format("%.0f DT", db.getRevenusAttendus()));
                lblRevenus.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            } else {
                lblRevenus.setText("—");
                lblRevenus.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            }

            if (db.getCoutsEstimes() > 0 && db.getRevenusAttendus() > 0) {
                lblMarge.setText(String.format("%.0f DT", db.getMargeEstimee()));
                lblRatio.setText(String.format("%.2fx", db.getRatioRentabilite()));
            } else {
                lblMarge.setText("—");
                lblRatio.setText("—");
            }

            lblForceEquipe.setText(db.getForceEquipe() > 0 ? db.getForceEquipe() + "/10" : "—");

            lblScoreMarche.setText(String.format("%.0f/30",    db.getScoreMarche()));
            lblScoreFinancier.setText(String.format("%.0f/40", db.getScoreFinancier()));
            lblScoreEquipe.setText(String.format("%.0f/30",    db.getScoreEquipeCalcule()));
            lblScoreRisque.setText(String.format("%.0f/30",    db.getScoreRisqueCalcule()));
        }

        if (projet.getScoreGlobal() > 0) {
            double s = projet.getScoreGlobal();
            lblScoreGlobal.setText(String.format("%.0f", s));
            progressScore.setProgress(s / 100);

            if (s >= 80) {
                lblCategorie.setText("🟢 Excellent");
                lblCategorie.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-background-color: #d5f5e3; -fx-text-fill: #27ae60;");
                lblScoreGlobal.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            } else if (s >= 65) {
                lblCategorie.setText("🟢 Très bon");
                lblCategorie.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-background-color: #d1f2eb; -fx-text-fill: #16a085;");
                lblScoreGlobal.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #16a085;");
            } else if (s >= 50) {
                lblCategorie.setText("🔵 BON — Potentiel réel");
                lblCategorie.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-background-color: #d6eaf8; -fx-text-fill: #2980b9;");
                lblScoreGlobal.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
            } else if (s >= 35) {
                lblCategorie.setText("🟠 Moyen — À améliorer");
                lblCategorie.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-background-color: #fdebd0; -fx-text-fill: #d35400;");
                lblScoreGlobal.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
            } else {
                lblCategorie.setText("🔴 Risque élevé");
                lblCategorie.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-background-color: #fadbd8; -fx-text-fill: #e74c3c;");
                lblScoreGlobal.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  afficherVerdictFinal
    // ═══════════════════════════════════════════════════════════════
    private void afficherVerdictFinal(String verdict, double score) {
        if (verdictBox == null) return;
        String gradient;
        if      (score >= 80) gradient = "linear-gradient(to right, #27ae60, #2ecc71)";
        else if (score >= 65) gradient = "linear-gradient(to right, #16a085, #1abc9c)";
        else if (score >= 50) gradient = "linear-gradient(to right, #2980b9, #3498db)";
        else if (score >= 35) gradient = "linear-gradient(to right, #d35400, #e67e22)";
        else                  gradient = "linear-gradient(to right, #c0392b, #e74c3c)";

        if (verdictHeader        != null) verdictHeader.setStyle(
                "-fx-padding: 24 30; -fx-background-radius: 12; -fx-background-color: " + gradient + ";");
        if (lblVerdictText       != null) lblVerdictText.setText(verdict);
        if (lblScoreVerdictFinal != null) lblScoreVerdictFinal.setText(String.format("%.1f/100", score));

        verdictBox.setVisible(true);
        verdictBox.setManaged(true);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    private void afficherBoutonPdf(boolean visible) {
        if (btnTelechargerPdf != null) {
            btnTelechargerPdf.setVisible(visible);
            btnTelechargerPdf.setManaged(visible);
        }
    }

    private String extraireVerdict(JsonObject analyse, double score) {
        if (analyse.has("verdict")) return analyse.get("verdict").getAsString();
        return score >= 80 ? "EXCELLENT" : score >= 65 ? "TRES_BON" : score >= 50 ? "BON"
                : score >= 35 ? "MOYEN" : score >= 20 ? "RISQUE" : "INCOMPLET";
    }

    private String extraireSectionTexte(String texte, String debut, String fin) {
        try {
            int idxDebut = texte.indexOf(debut);
            if (idxDebut < 0) return "";
            idxDebut += debut.length();
            int idxFin = fin != null ? texte.indexOf(fin, idxDebut) : -1;
            String section = idxFin > 0 ? texte.substring(idxDebut, idxFin) : texte.substring(idxDebut);
            return section.trim();
        } catch (Exception e) { return ""; }
    }

    private com.google.gson.JsonArray convertirBulletEnArray(String texte, com.google.gson.Gson gson) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        if (texte == null || texte.trim().isEmpty()) return arr;
        for (String ligne : texte.split("\n")) {
            String l = ligne.trim();
            if (l.startsWith("•")) l = l.substring(1).trim();
            if (!l.isEmpty()) arr.add(l);
        }
        return arr;
    }

    private String sanitiser(String nom) {
        if (nom == null || nom.trim().isEmpty()) return "projet";
        String s = nom.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
        return s.substring(0, Math.min(s.length(), 30));
    }

    private HBox creerHBoxAvecPadding(double horizontal, double vertical) {
        HBox hbox = new HBox(12);
        hbox.setPadding(new Insets(vertical, horizontal, vertical, horizontal));
        return hbox;
    }

    private VBox wrapWithPadding(VBox inner, double top, double right, double bottom, double left) {
        VBox outer = new VBox();
        outer.setPadding(new Insets(top, right, bottom, left));
        outer.getChildren().add(inner);
        return outer;
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMPOSANTS UI
    // ═══════════════════════════════════════════════════════════════
    private VBox creerSectionResume(String texte) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20, 24, 4, 24));
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label("📋"); icon.setStyle("-fx-font-size: 16px;");
        Label titre = new Label("Résumé Exécutif");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        header.getChildren().addAll(icon, titre);
        Label contenu = new Label(texte);
        contenu.setWrapText(true);
        contenu.setStyle("-fx-padding: 14 16; -fx-background-color: linear-gradient(to right, #eaf4fd, #f8fbff);" +
                "-fx-background-radius: 8; -fx-text-fill: #2c3e50; -fx-font-size: 13px;" +
                "-fx-border-color: #aed6f1; -fx-border-radius: 8; -fx-border-width: 0 0 0 4;");
        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 0, 0));
        section.getChildren().addAll(header, contenu, sep);
        return section;
    }

    private VBox creerSectionTexte(String titreStr, String texte,
                                   String couleur, String bgColor, String borderColor) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12, 24, 4, 24));
        Label titreLabel = new Label(titreStr);
        titreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        Label contenu = new Label(texte);
        contenu.setWrapText(true);
        contenu.setStyle("-fx-padding: 12 14; -fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 8; -fx-text-fill: #34495e; -fx-font-size: 12px;" +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 0 0 0 3;");
        Separator sep = new Separator();
        sep.setPadding(new Insets(6, 0, 0, 0));
        section.getChildren().addAll(titreLabel, contenu, sep);
        return section;
    }

    private VBox creerSectionListe(String titreStr, java.util.List<String> items,
                                   String couleur, String bgColor, String borderColor) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(14));
        section.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-border-radius: 10;");
        Label titreLabel = new Label(titreStr);
        titreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        VBox listeBox = new VBox(6);
        for (String item : items) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);
            Label puce = new Label("•");
            puce.setStyle("-fx-font-size: 14px; -fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
            Label texteItem = new Label(item);
            texteItem.setWrapText(true);
            texteItem.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
            HBox.setHgrow(texteItem, Priority.ALWAYS);
            row.getChildren().addAll(puce, texteItem);
            listeBox.getChildren().add(row);
        }
        section.getChildren().addAll(titreLabel, listeBox);
        return section;
    }

    private VBox creerSectionJustification(String texte) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(12, 24, 16, 24));
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;" +
                "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 10;");
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label("💡"); icon.setStyle("-fx-font-size: 15px;");
        Label titre = new Label("Justification du Score");
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        header.getChildren().addAll(icon, titre);
        VBox lignes = new VBox(4);
        for (String ligne : texte.split("\n")) {
            if (ligne.trim().isEmpty()) continue;
            Label l = new Label(ligne.trim());
            l.setWrapText(true);
            if (ligne.contains("Score final") || ligne.contains("score final") || ligne.contains("Score:")) {
                l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3498db;" +
                        " -fx-font-family: 'Consolas', monospace;");
            } else {
                l.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #34495e;");
            }
            lignes.getChildren().add(l);
        }
        card.getChildren().addAll(header, lignes);
        section.getChildren().add(card);
        return section;
    }

    private VBox creerSectionConseilJury(String texte) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(12, 24, 16, 24));
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: linear-gradient(to right, #fdf9f0, #fef9ed);" +
                "-fx-background-radius: 12; -fx-border-color: #f39c12; -fx-border-width: 0 0 0 5; -fx-border-radius: 12;");
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🏆"); icon.setStyle("-fx-font-size: 20px;");
        VBox titreBox = new VBox(2);
        Label titre = new Label("Message du Jury NAJAHNI");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        Label sous = new Label("Conseil personnalisé pour votre équipe");
        sous.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22;");
        titreBox.getChildren().addAll(titre, sous);
        header.getChildren().addAll(icon, titreBox);
        Label contenu = new Label("« " + texte + " »");
        contenu.setWrapText(true);
        contenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-font-style: italic; -fx-padding: 8 0 0 0;");
        card.getChildren().addAll(header, contenu);
        section.getChildren().add(card);
        return section;
    }

    private VBox creerSectionActualites(List<NewsService.Article> articles) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12, 24, 4, 24));
        Label titre = new Label("📰  Actualités Récentes du Secteur");
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        VBox cardsBox = new VBox(8);
        for (NewsService.Article a : articles) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;" +
                    "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8;");
            Label source = new Label("📌 " + a.source + " — " + (a.date != null ? a.date : "2025"));
            source.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");
            Label titreArticle = new Label(a.titre);
            titreArticle.setWrapText(true);
            titreArticle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            card.getChildren().addAll(source, titreArticle);
            if (a.description != null && !a.description.isEmpty()) {
                String desc = a.description.length() > 120 ? a.description.substring(0, 120) + "..." : a.description;
                Label descLabel = new Label(desc);
                descLabel.setWrapText(true);
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
                card.getChildren().add(descLabel);
            }
            cardsBox.getChildren().add(card);
        }
        Separator sep = new Separator();
        sep.setPadding(new Insets(6, 0, 0, 0));
        section.getChildren().addAll(titre, cardsBox, sep);
        return section;
    }

    private VBox creerSectionWorldBank(String texte) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12, 24, 4, 24));
        Label titre = new Label("🌍  Contexte Macro-Économique (World Bank)");
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a5276;");
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #eaf4fd; -fx-background-radius: 8;" +
                "-fx-border-color: #aed6f1; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");
        for (String ligne : texte.split("\n")) {
            if (ligne.trim().isEmpty() || ligne.startsWith("===")) continue;
            Label l = new Label(ligne.trim());
            l.setWrapText(true);
            if (ligne.startsWith("📊") || ligne.startsWith("🌾") || ligne.startsWith("💻")
                    || ligne.startsWith("🏥") || ligne.startsWith("💳")
                    || ligne.startsWith("📚") || ligne.startsWith("✈️")) {
                l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a5276; -fx-padding: 4 0 2 0;");
            } else {
                l.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");
            }
            card.getChildren().add(l);
        }
        Separator sep = new Separator();
        sep.setPadding(new Insets(6, 0, 0, 0));
        section.getChildren().addAll(titre, card, sep);
        return section;
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }
}