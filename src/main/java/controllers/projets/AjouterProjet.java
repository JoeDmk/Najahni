package controllers.projets;

import models.projets.Projet;
import models.projets.StatutProjet;
import models.projets.donneesBusiness;
import services.projets.EmailNotificationService;
import services.projets.UserService;
import services.projets.projetCRUD;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AjouterProjet implements Initializable {

    @FXML private TextField titre;
    @FXML private TextArea  description;
    @FXML private TextField secteur;
    @FXML private TextField etape;
    @FXML private TextField userIdField;

    @FXML private TextField tailleMarche;
    @FXML private TextField modeleRevenu;
    @FXML private TextField couts;
    @FXML private TextField revenus;
    @FXML private TextField risque;
    @FXML private TextField forceEquipe;

    private projetCRUD projetService = new projetCRUD();
    private UserService userService  = new UserService();
    private Stage stage;
    private boolean isClientMode = false;
    private int connectedUserId;
    private boolean isModification = false;
    private Projet projetAModifier;
    private Runnable onModificationComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setOnModificationComplete(Runnable callback) {
        this.onModificationComplete = callback;
    }

    public void setProjet(Projet projet) {
        this.projetAModifier = projet;
        this.isModification  = (projet != null);

        if (isModification) {
            titre.setText(projet.getTitre());
            description.setText(projet.getDescription());
            secteur.setText(projet.getSecteur());
            etape.setText(projet.getEtape());

            if (projet.getDonneesBusiness() != null) {
                donneesBusiness db = projet.getDonneesBusiness();
                tailleMarche.setText(db.getTailleMarche() != null ? db.getTailleMarche() : "");
                modeleRevenu.setText(db.getModeleRevenu() != null ? db.getModeleRevenu() : "");
                couts.setText(db.getRawCouts() != null && !db.getRawCouts().isEmpty()
                        ? db.getRawCouts()
                        : (db.getCoutsEstimes() > 0 ? String.valueOf((int) db.getCoutsEstimes()) : ""));
                revenus.setText(db.getRawRevenus() != null && !db.getRawRevenus().isEmpty()
                        ? db.getRawRevenus()
                        : (db.getRevenusAttendus() > 0 ? String.valueOf((int) db.getRevenusAttendus()) : ""));
                risque.setText(db.getNiveauRisque() != null ? db.getNiveauRisque() : "");
                forceEquipe.setText(db.getRawForceEquipe() != null && !db.getRawForceEquipe().isEmpty()
                        ? db.getRawForceEquipe()
                        : (db.getForceEquipe() > 0 ? String.valueOf(db.getForceEquipe()) : ""));
            }
        }
    }

    @FXML
    private void AjouterProjet() {

        // Validation : aucun champ vide
        if (titre.getText().trim().isEmpty()       ||
                secteur.getText().trim().isEmpty()      ||
                etape.getText().trim().isEmpty()        ||
                tailleMarche.getText().trim().isEmpty() ||
                modeleRevenu.getText().trim().isEmpty() ||
                couts.getText().trim().isEmpty()        ||
                revenus.getText().trim().isEmpty()      ||
                risque.getText().trim().isEmpty()       ||
                forceEquipe.getText().trim().isEmpty()) {

            showAlert(Alert.AlertType.WARNING, "Champs obligatoires",
                    "Tous les champs doivent être remplis.\n\n" +
                            "✍️  Écris ce que tu veux — l'IA interprète tout :\n" +
                            "  • \"je sais pas encore\"\n" +
                            "  • \"estimez pour moi\"\n" +
                            "  • \"8/10 — équipe de 3 ingénieurs expérimentés\"\n" +
                            "  • \"environ 50 000 DT\"\n" +
                            "  • \"entre 30K et 50K\"\n" +
                            "  • \"1.5 million de dinars\"\n\n" +
                            "Seule règle : ne pas laisser vide.");
            return;
        }

        // Validation userId (mode admin seulement)
        int userIdValue = 0;
        if (!isClientMode) {
            if (userIdField == null || userIdField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez saisir le User ID.");
                return;
            }
            try { userIdValue = Integer.parseInt(userIdField.getText().trim()); }
            catch (NumberFormatException e) { showAlert(Alert.AlertType.ERROR, "Erreur", "User ID invalide."); return; }
            try {
                if (!projetService.userExiste(userIdValue)) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Cet utilisateur n'existe pas."); return;
                }
            } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur BDD", e.getMessage()); return; }
        }

        Projet projet = isModification ? projetAModifier : new Projet();
        int userId = isClientMode ? connectedUserId : userIdValue;
        projet.setUserId(userId);
        projet.setTitre(titre.getText().trim());
        projet.setDescription(description.getText().trim());
        projet.setSecteur(secteur.getText().trim());
        projet.setEtape(etape.getText().trim());

        if (!isModification) {
            projet.setDateCreation(LocalDate.now());
            projet.setStatutProjet(StatutProjet.BROUILLON);
        }

        donneesBusiness db = isModification && projet.getDonneesBusiness() != null
                ? projet.getDonneesBusiness() : new donneesBusiness();

        db.setTailleMarche(tailleMarche.getText().trim());
        db.setModeleRevenu(modeleRevenu.getText().trim());
        db.setNiveauRisque(risque.getText().trim());

        // Stocker le texte brut EXACT — l'IA reçoit exactement ce que l'utilisateur a écrit
        String coutsTexte   = couts.getText().trim();
        String revenusTexte = revenus.getText().trim();
        String equipeTexte  = forceEquipe.getText().trim();

        db.setRawCouts(coutsTexte);
        db.setRawRevenus(revenusTexte);
        db.setRawForceEquipe(equipeTexte);

        // Parser intelligent — extrait la valeur numérique si possible, sinon 0 (l'IA gère)
        db.setCoutsEstimes(parseFlexible(coutsTexte));
        db.setRevenusAttendus(parseFlexible(revenusTexte));
        db.setForceEquipe((int) parseFlexible(equipeTexte));

        System.out.println("📝 Parsing saisie libre :");
        System.out.println("   Coûts   : \"" + coutsTexte   + "\" → " + db.getCoutsEstimes());
        System.out.println("   Revenus : \"" + revenusTexte + "\" → " + db.getRevenusAttendus());
        System.out.println("   Équipe  : \"" + equipeTexte  + "\" → " + db.getForceEquipe() + "/10");

        projet.setDonneesBusiness(db);

        try {
            if (isModification) {
                projet.setScoreGlobal(0);
                projet.setDiagnosticIA(null);
                projetService.modifier(projet);
                projetService.resetScore(projet.getId());
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Projet modifié !\n🤖 Nouvelle évaluation IA en cours...");
                if (onModificationComplete != null) onModificationComplete.run();
            } else {
                projetService.ajouter(projet);
                envoyerEmailAjout(userId, projet);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Projet ajouté !\n📧 Confirmation envoyée par email.");
                clearAllFields();
            }
            if (stage != null) stage.close();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de données", e.getMessage());
        }
    }

    // =========================================================
    //  PARSER INTELLIGENT — saisie libre en langage naturel
    //
    //  Priorité des cas (ordre important) :
    //  1. X/10 ou X/Y        "8/10 — équipe de 3 dev"      → 8
    //  2. X sur Y            "9 sur 10"                     → 9
    //  3. Plage entre/à/-    "entre 30K et 50K"             → 40 000
    //  4. Texte million/mille "1.5 million"                 → 1 500 000
    //  5. Suffixe K/M/B      "50K", "2M"                   → 50 000
    //  6. Nombre avec espaces "50 000 DT"                   → 50 000
    //  → Si hésitation/question détectée               → 0 (IA estime)
    // =========================================================
    private double parseFlexible(String texte) {
        if (texte == null || texte.trim().isEmpty()) return 0.0;

        String t = texte.toLowerCase().trim();

        // Demande d'estimation explicite → laisser l'IA gérer
        if (t.contains("sais pas") || t.contains("estimez") || t.contains("pas encore")
                || t.contains("unknown") || t.contains("tbd") || t.equals("?")
                || t.contains("combien") || t.contains("aide") || t.contains("conseil")
                || t.contains("recommand")) {
            return 0.0;
        }

        try {
            // CAS 1 — X/10 ou X/Y (note sur N)
            // PRIORITÉ : évite de lire "810" dans "8/10 — équipe de 3 dev"
            Matcher slashM = Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s*/\\s*\\d+").matcher(t);
            if (slashM.find())
                return Double.parseDouble(slashM.group(1).replace(",", "."));

            // CAS 2 — "X sur Y"
            Matcher surM = Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s+sur\\s+\\d+").matcher(t);
            if (surM.find())
                return Double.parseDouble(surM.group(1).replace(",", "."));

            // CAS 3 — plage "entre X[K] et Y[K]" ou "X[K]-Y[K]" ou "X[K] à Y[K]"
            Matcher rangeM = Pattern.compile(
                    "(?:entre\\s+)?(\\d+(?:[.,]\\d+)?)(\\s*[kmb]?)\\s*(?:et|-|à)\\s*(\\d+(?:[.,]\\d+)?)(\\s*[kmb]?)"
            ).matcher(t);
            if (rangeM.find()) {
                double v1 = Double.parseDouble(rangeM.group(1).replace(",", "."));
                double v2 = Double.parseDouble(rangeM.group(3).replace(",", "."));
                v1 = appliquerSuffixe(v1, rangeM.group(2).trim());
                v2 = appliquerSuffixe(v2, rangeM.group(4).trim());
                return (v1 + v2) / 2.0;
            }

            // CAS 4 — multiplicateurs texte
            Matcher multM = Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s*(milliard|million|mille)").matcher(t);
            if (multM.find()) {
                double val = Double.parseDouble(multM.group(1).replace(",", "."));
                switch (multM.group(2)) {
                    case "mille":    return val * 1_000;
                    case "million":  return val * 1_000_000;
                    case "milliard": return val * 1_000_000_000;
                }
            }

            // CAS 5 — suffixe K/M/B collé ("50K", "1.5M")
            Matcher kmM = Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s*([kmb])\\b").matcher(t);
            if (kmM.find())
                return appliquerSuffixe(
                        Double.parseDouble(kmM.group(1).replace(",", ".")),
                        kmM.group(2));

            // CAS 6 — nombre avec espaces, puis premier nombre trouvé
            String compact = t.replaceAll("(\\d)\\s+(\\d)", "$1$2");
            Matcher numM = Pattern.compile("(\\d+(?:[.,]\\d+)?)").matcher(compact);
            if (numM.find()) {
                double val = Double.parseDouble(numM.group(1).replace(",", "."));
                if (t.matches(".*\\d\\s*k(?:[^a-z]|$).*") && val < 10_000) val *= 1_000;
                return val;
            }

        } catch (Exception ignored) {}

        return 0.0;
    }

    private double appliquerSuffixe(double val, String suffixe) {
        if (suffixe == null || suffixe.isEmpty()) return val;
        switch (suffixe.trim().toLowerCase()) {
            case "k": return val * 1_000;
            case "m": return val * 1_000_000;
            case "b": return val * 1_000_000_000;
            default:  return val;
        }
    }

    private void envoyerEmailAjout(int userId, Projet projet) {
        try {
            String email = userService.getEmailById(userId);
            String nom   = userService.getNomById(userId);
            if (email != null && !email.trim().isEmpty()) {
                EmailNotificationService.envoyerConfirmationAjout(
                        email, nom, projet.getTitre(),
                        projet.getSecteur() != null ? projet.getSecteur() : "—",
                        projet.getEtape()   != null ? projet.getEtape()   : "—");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Email non envoyé : " + e.getMessage());
        }
    }

    private void clearAllFields() {
        titre.clear(); description.clear(); secteur.clear(); etape.clear();
        tailleMarche.clear(); modeleRevenu.clear();
        couts.clear(); revenus.clear(); risque.clear(); forceEquipe.clear();
        if (userIdField != null) userIdField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }

    public void annuler(javafx.event.ActionEvent e) { if (stage != null) stage.close(); }

    public void setClientMode(int userId) {
        this.isClientMode = true; this.connectedUserId = userId;
        if (userIdField != null) { userIdField.setVisible(false); userIdField.setManaged(false); }
    }

    public void setStage(Stage stage) { this.stage = stage; }
}