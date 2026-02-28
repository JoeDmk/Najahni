package edu.najahni.services;

import edu.najahni.entities.Projet;
import edu.najahni.entities.StatutProjet;
import edu.najahni.entities.donneesBusiness;
import edu.najahni.tools.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class projetCRUD {

    Connection conn;

    public projetCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    // ================= LOGIN =================

    public boolean loginClient(int userId, String password) throws SQLException {
        String sql = "SELECT id FROM user WHERE id = ? AND password = ? AND is_active = 1 AND is_banned = 0";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    // ================= AJOUTER =================

    public void ajouter(Projet projet) throws SQLException {
        if (projet.getDonneesBusiness() == null)
            throw new IllegalArgumentException("Données business obligatoires");

        // 1. Insérer le projet
        String reqProjet = "INSERT INTO projet (user_id, titre, description, secteur, etape, statut, date_creation, statut_projet) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstProjet = conn.prepareStatement(reqProjet, Statement.RETURN_GENERATED_KEYS);
        pstProjet.setInt(1, projet.getUserId());
        pstProjet.setString(2, projet.getTitre());
        pstProjet.setString(3, projet.getDescription());
        pstProjet.setString(4, projet.getSecteur());
        pstProjet.setString(5, projet.getEtape());
        pstProjet.setString(6, projet.getStatut());
        pstProjet.setDate(7, Date.valueOf(projet.getDateCreation()));
        pstProjet.setString(8, projet.getStatutProjet().name());
        pstProjet.executeUpdate();

        ResultSet generatedKeys = pstProjet.getGeneratedKeys();

        if (generatedKeys.next()) {
            int projetId = generatedKeys.getInt(1);
            projet.setId(projetId);

            donneesBusiness db = projet.getDonneesBusiness();
            db.calculerIndicateurs();

            // ✅ INCLUT raw_couts, raw_revenus, raw_force_equipe
            String reqDB = "INSERT INTO donnees_business " +
                    "(taille_marche, modele_revenu, couts_estimes, revenus_attendus, " +
                    "niveau_risque, force_equipe, projet_id, " +
                    "marge_estimee, ratio_rentabilite, score_financier, " +
                    "score_marche, score_equipe_calcule, score_risque_calcule, " +
                    "raw_couts, raw_revenus, raw_force_equipe) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstDB = conn.prepareStatement(reqDB);
            pstDB.setString(1, db.getTailleMarche());
            pstDB.setString(2, db.getModeleRevenu());
            pstDB.setDouble(3, db.getCoutsEstimes());
            pstDB.setDouble(4, db.getRevenusAttendus());
            pstDB.setString(5, db.getNiveauRisque());
            pstDB.setInt(6, db.getForceEquipe());
            pstDB.setInt(7, projetId);
            pstDB.setDouble(8, db.getMargeEstimee());
            pstDB.setDouble(9, db.getRatioRentabilite());
            pstDB.setDouble(10, db.getScoreFinancier());
            pstDB.setDouble(11, db.getScoreMarche());
            pstDB.setDouble(12, db.getScoreEquipeCalcule());
            pstDB.setDouble(13, db.getScoreRisqueCalcule());
            // ✅ CHAMPS RAW — texte exact de l'utilisateur
            pstDB.setString(14, db.getRawCouts());
            pstDB.setString(15, db.getRawRevenus());
            pstDB.setString(16, db.getRawForceEquipe());
            pstDB.executeUpdate();

            System.out.println("✅ Projet ajouté — rawCouts=[" + db.getRawCouts()
                    + "] rawRevenus=[" + db.getRawRevenus()
                    + "] rawForce=[" + db.getRawForceEquipe() + "]");
        }
    }

    // ================= AFFICHER (Admin) =================

    public List<Projet> afficher() throws SQLException {
        String req = "SELECT " +
                "p.id AS projet_id, p.user_id, p.titre, p.description, " +
                "p.secteur, p.etape, p.statut, p.date_creation, p.statut_projet, " +
                "p.score_global, p.diagnostic_ia, p.date_soumission, p.date_evaluation, " +
                "db.id AS db_id, db.taille_marche, db.modele_revenu, " +
                "db.couts_estimes, db.revenus_attendus, db.niveau_risque, db.force_equipe, " +
                "db.marge_estimee, db.ratio_rentabilite, db.score_financier, " +
                "db.score_marche, db.score_equipe_calcule, db.score_risque_calcule, " +
                // ✅ CHAMPS RAW
                "db.raw_couts, db.raw_revenus, db.raw_force_equipe " +
                "FROM projet p " +
                "LEFT JOIN donnees_business db ON p.id = db.projet_id " +
                "ORDER BY p.date_creation DESC";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Projet> liste = new ArrayList<>();
        while (rs.next()) {
            Projet p = new Projet();
            p.setId(rs.getInt("projet_id"));
            p.setUserId(rs.getInt("user_id"));
            p.setTitre(rs.getString("titre"));
            p.setDescription(rs.getString("description"));
            p.setSecteur(rs.getString("secteur"));
            p.setEtape(rs.getString("etape"));
            p.setStatut(rs.getString("statut"));

            Date dateCreation = rs.getDate("date_creation");
            if (dateCreation != null) p.setDateCreation(dateCreation.toLocalDate());

            String statutProjetStr = rs.getString("statut_projet");
            if (statutProjetStr != null) p.setStatutProjet(StatutProjet.valueOf(statutProjetStr));

            p.setScoreGlobal(rs.getDouble("score_global"));
            p.setDiagnosticIA(rs.getString("diagnostic_ia"));

            Date dateSoumission = rs.getDate("date_soumission");
            if (dateSoumission != null) p.setDateSoumission(dateSoumission.toLocalDate());

            Date dateEvaluation = rs.getDate("date_evaluation");
            if (dateEvaluation != null) p.setDateEvaluation(dateEvaluation.toLocalDate());

            if (rs.getObject("db_id") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db_id"));
                db.setTailleMarche(rs.getString("taille_marche"));
                db.setModeleRevenu(rs.getString("modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("revenus_attendus"));
                db.setNiveauRisque(rs.getString("niveau_risque"));
                db.setForceEquipe(rs.getInt("force_equipe"));
                db.setProjetId(p.getId());
                db.setMargeEstimee(rs.getDouble("marge_estimee"));
                db.setRatioRentabilite(rs.getDouble("ratio_rentabilite"));
                db.setScoreFinancier(rs.getDouble("score_financier"));
                db.setScoreMarche(rs.getDouble("score_marche"));
                db.setScoreEquipeCalcule(rs.getDouble("score_equipe_calcule"));
                db.setScoreRisqueCalcule(rs.getDouble("score_risque_calcule"));
                // ✅ CHAMPS RAW
                db.setRawCouts(rs.getString("raw_couts"));
                db.setRawRevenus(rs.getString("raw_revenus"));
                db.setRawForceEquipe(rs.getString("raw_force_equipe"));
                p.setDonneesBusiness(db);
            }
            liste.add(p);
        }

        System.out.println("📊 Total projets récupérés : " + liste.size());
        return liste;
    }

    // ========== VALIDATION MÉTIER ==========

    public boolean estComplet(Projet projet) {
        if (projet == null) return false;
        if (projet.getDonneesBusiness() == null) return false;

        donneesBusiness db = projet.getDonneesBusiness();

        boolean tailleMarcheOk = db.getTailleMarche() != null && !db.getTailleMarche().trim().isEmpty();
        boolean modeleRevenuOk = db.getModeleRevenu() != null && !db.getModeleRevenu().trim().isEmpty();
        boolean coutsOk        = db.getCoutsEstimes() > 0;
        boolean revenusOk      = db.getRevenusAttendus() > 0;
        boolean forceOk        = db.getForceEquipe() >= 1 && db.getForceEquipe() <= 10;
        boolean risqueOk       = db.getNiveauRisque() != null && !db.getNiveauRisque().trim().isEmpty();
        boolean titreOk        = projet.getTitre() != null && !projet.getTitre().trim().isEmpty();
        boolean secteurOk      = projet.getSecteur() != null && !projet.getSecteur().trim().isEmpty();

        boolean complet = tailleMarcheOk && modeleRevenuOk && coutsOk && revenusOk
                && forceOk && risqueOk && titreOk && secteurOk;

        if (!complet) System.out.println("⚠️ Projet incomplet - vérifier les champs");
        return complet;
    }

    // ========== CHAMPS MANQUANTS ==========

    public List<String> getChampsManquants(Projet projet) {
        List<String> manquants = new ArrayList<>();
        if (projet == null) { manquants.add("Projet null"); return manquants; }
        if (projet.getTitre() == null || projet.getTitre().trim().isEmpty()) manquants.add("Titre");
        if (projet.getSecteur() == null || projet.getSecteur().trim().isEmpty()) manquants.add("Secteur");
        if (projet.getDonneesBusiness() == null) { manquants.add("Données business"); return manquants; }

        donneesBusiness db = projet.getDonneesBusiness();
        if (db.getTailleMarche() == null || db.getTailleMarche().trim().isEmpty()) manquants.add("Taille du marché");
        if (db.getModeleRevenu() == null || db.getModeleRevenu().trim().isEmpty()) manquants.add("Modèle de revenu");
        if (db.getCoutsEstimes() <= 0) manquants.add("Coûts estimés (doit être > 0)");
        if (db.getRevenusAttendus() <= 0) manquants.add("Revenus attendus (doit être > 0)");
        if (db.getForceEquipe() < 1 || db.getForceEquipe() > 10) manquants.add("Force équipe (doit être entre 1 et 10)");
        if (db.getNiveauRisque() == null || db.getNiveauRisque().trim().isEmpty()) manquants.add("Niveau de risque");
        return manquants;
    }

    // ========== SOUMETTRE ==========

    public void soumettreProjet(int projetId, int userId) throws SQLException {
        System.out.println("📤 Soumission du projet ID=" + projetId + " par user ID=" + userId);

        Projet projet = getProjetById(projetId);
        System.out.println("🔍 Projet trouvé: " + (projet != null ? projet.getTitre() : "null"));

        String sql = "UPDATE projet SET statut_projet = 'SOUMIS', date_soumission = CURDATE() " +
                "WHERE id = ? AND user_id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, projetId);
        pst.setInt(2, userId);
        int rowsAffected = pst.executeUpdate();

        if (rowsAffected == 0)
            throw new SQLException("Échec de la soumission - Projet non trouvé ou accès non autorisé");

        System.out.println("✅ Projet " + projetId + " soumis avec succès !");
    }

    // ========== GET PAR ID ==========

    public Projet getProjetById(int id) throws SQLException {
        // ✅ REQUÊTE EXPLICITE — pas de SELECT * pour éviter conflits de colonnes
        String req = "SELECT " +
                "p.id, p.user_id, p.titre, p.description, p.secteur, p.etape, " +
                "p.statut, p.date_creation, p.statut_projet, " +
                "p.score_global, p.diagnostic_ia, p.date_soumission, p.date_evaluation, " +
                "db.id AS db_id, db.taille_marche, db.modele_revenu, " +
                "db.couts_estimes, db.revenus_attendus, db.niveau_risque, db.force_equipe, " +
                "db.marge_estimee, db.ratio_rentabilite, db.score_financier, " +
                "db.score_marche, db.score_equipe_calcule, db.score_risque_calcule, " +
                // ✅ CHAMPS RAW
                "db.raw_couts, db.raw_revenus, db.raw_force_equipe " +
                "FROM projet p " +
                "LEFT JOIN donnees_business db ON p.id = db.projet_id " +
                "WHERE p.id = ?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Projet p = new Projet();
            p.setId(rs.getInt("id"));
            p.setUserId(rs.getInt("user_id"));
            p.setTitre(rs.getString("titre"));
            p.setDescription(rs.getString("description"));
            p.setSecteur(rs.getString("secteur"));
            p.setEtape(rs.getString("etape"));
            p.setStatut(rs.getString("statut"));

            Date dateCreation = rs.getDate("date_creation");
            if (dateCreation != null) p.setDateCreation(dateCreation.toLocalDate());

            String statutProjetStr = rs.getString("statut_projet");
            if (statutProjetStr != null) {
                try { p.setStatutProjet(StatutProjet.valueOf(statutProjetStr)); }
                catch (Exception e) { p.setStatutProjet(StatutProjet.BROUILLON); }
            }

            p.setScoreGlobal(rs.getDouble("score_global"));
            p.setDiagnosticIA(rs.getString("diagnostic_ia"));

            Date dateSoumission = rs.getDate("date_soumission");
            if (dateSoumission != null) p.setDateSoumission(dateSoumission.toLocalDate());

            Date dateEvaluation = rs.getDate("date_evaluation");
            if (dateEvaluation != null) p.setDateEvaluation(dateEvaluation.toLocalDate());

            if (rs.getObject("db_id") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db_id"));
                db.setTailleMarche(rs.getString("taille_marche"));
                db.setModeleRevenu(rs.getString("modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("revenus_attendus"));
                db.setNiveauRisque(rs.getString("niveau_risque"));
                db.setForceEquipe(rs.getInt("force_equipe"));
                db.setProjetId(p.getId());
                db.setMargeEstimee(rs.getDouble("marge_estimee"));
                db.setRatioRentabilite(rs.getDouble("ratio_rentabilite"));
                db.setScoreFinancier(rs.getDouble("score_financier"));
                db.setScoreMarche(rs.getDouble("score_marche"));
                db.setScoreEquipeCalcule(rs.getDouble("score_equipe_calcule"));
                db.setScoreRisqueCalcule(rs.getDouble("score_risque_calcule"));
                // ✅ CHAMPS RAW
                db.setRawCouts(rs.getString("raw_couts"));
                db.setRawRevenus(rs.getString("raw_revenus"));
                db.setRawForceEquipe(rs.getString("raw_force_equipe"));
                p.setDonneesBusiness(db);
            }

            return p;
        }

        return null;
    }

    // ================= MODIFIER =================

    public void modifier(Projet projet) throws SQLException {
        System.out.println("📝 Modification du projet ID=" + projet.getId());

        String req = "UPDATE projet SET titre=?, description=?, secteur=?, etape=?, statut=?, statut_projet=? " +
                "WHERE id=? AND user_id=?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, projet.getTitre());
        pst.setString(2, projet.getDescription());
        pst.setString(3, projet.getSecteur());
        pst.setString(4, projet.getEtape());
        pst.setString(5, projet.getStatut());
        pst.setString(6, projet.getStatutProjet() != null
                ? projet.getStatutProjet().name() : StatutProjet.BROUILLON.name());
        pst.setInt(7, projet.getId());
        pst.setInt(8, projet.getUserId());

        int rowsAffected = pst.executeUpdate();
        System.out.println("✅ " + rowsAffected + " ligne(s) mise(s) à jour dans projet");

        if (projet.getDonneesBusiness() != null) {
            donneesBusiness db = projet.getDonneesBusiness();
            db.calculerIndicateurs();

            // ✅ INCLUT raw_couts, raw_revenus, raw_force_equipe
            String reqDB = "UPDATE donnees_business SET taille_marche=?, modele_revenu=?, " +
                    "couts_estimes=?, revenus_attendus=?, niveau_risque=?, force_equipe=?, " +
                    "marge_estimee=?, ratio_rentabilite=?, score_financier=?, " +
                    "score_marche=?, score_equipe_calcule=?, score_risque_calcule=?, " +
                    "raw_couts=?, raw_revenus=?, raw_force_equipe=? " +
                    "WHERE projet_id=?";

            PreparedStatement pstDB = conn.prepareStatement(reqDB);
            pstDB.setString(1, db.getTailleMarche());
            pstDB.setString(2, db.getModeleRevenu());
            pstDB.setDouble(3, db.getCoutsEstimes());
            pstDB.setDouble(4, db.getRevenusAttendus());
            pstDB.setString(5, db.getNiveauRisque());
            pstDB.setInt(6, db.getForceEquipe());
            pstDB.setDouble(7, db.getMargeEstimee());
            pstDB.setDouble(8, db.getRatioRentabilite());
            pstDB.setDouble(9, db.getScoreFinancier());
            pstDB.setDouble(10, db.getScoreMarche());
            pstDB.setDouble(11, db.getScoreEquipeCalcule());
            pstDB.setDouble(12, db.getScoreRisqueCalcule());
            // ✅ CHAMPS RAW
            pstDB.setString(13, db.getRawCouts());
            pstDB.setString(14, db.getRawRevenus());
            pstDB.setString(15, db.getRawForceEquipe());
            pstDB.setInt(16, projet.getId());

            int dbRows = pstDB.executeUpdate();
            System.out.println("✅ " + dbRows + " ligne(s) dans donnees_business — rawCouts=["
                    + db.getRawCouts() + "] rawRevenus=[" + db.getRawRevenus()
                    + "] rawForce=[" + db.getRawForceEquipe() + "]");
        }
    }

    // ========== METTRE À JOUR LE SCORE IA ==========

    public void mettreAJourScore(int projetId, double score, String diagnostic) throws SQLException {
        String sql = "UPDATE projet SET score_global = ?, diagnostic_ia = ?, " +
                "statut_projet = 'EVALUE', date_evaluation = CURDATE() " +
                "WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setDouble(1, score);
        pst.setString(2, diagnostic);
        pst.setInt(3, projetId);
        int rows = pst.executeUpdate();
        System.out.println("✅ Score mis à jour pour projet " + projetId + " (" + rows + " ligne)");
    }

    // ================= SUPPRIMER (Admin) =================

    public void supprimer(int id) throws SQLException {
        System.out.println("🗑️ Suppression du projet ID=" + id);
        PreparedStatement pstProjet = conn.prepareStatement("DELETE FROM projet WHERE id = ?");
        pstProjet.setInt(1, id);
        pstProjet.executeUpdate();
        System.out.println("✅ Projet et ses données business supprimés automatiquement !");
    }

    // ================= SUPPRIMER (Client) =================

    public void supprimer(int projetId, int userId) throws SQLException {
        System.out.println("🗑️ Suppression du projet ID=" + projetId + " par user ID=" + userId);
        PreparedStatement pst = conn.prepareStatement("DELETE FROM projet WHERE id = ? AND user_id = ?");
        pst.setInt(1, projetId);
        pst.setInt(2, userId);
        int rowsAffected = pst.executeUpdate();
        if (rowsAffected == 0)
            throw new SQLException("Vous n'êtes pas autorisé à supprimer ce projet ou il n'existe pas.");
        System.out.println("✅ Projet et ses données business supprimés automatiquement !");
    }

    // ================= AFFICHER PAR USER =================

    public List<Projet> afficherParUser(int userId) throws SQLException {
        String req =
                "SELECT p.id, p.user_id, p.titre, p.description, p.secteur, p.etape, " +
                        "p.statut, p.date_creation, p.statut_projet, " +
                        "p.score_global, p.diagnostic_ia, p.date_soumission, p.date_evaluation, " +
                        "db.id AS db_id, " +
                        "db.taille_marche, db.modele_revenu, db.couts_estimes, " +
                        "db.revenus_attendus, db.niveau_risque, db.force_equipe, " +
                        "db.marge_estimee, db.ratio_rentabilite, db.score_financier, " +
                        "db.score_marche, db.score_equipe_calcule, db.score_risque_calcule, " +
                        // ✅ CHAMPS RAW
                        "db.raw_couts, db.raw_revenus, db.raw_force_equipe " +
                        "FROM projet p " +
                        "LEFT JOIN donnees_business db ON p.id = db.projet_id " +
                        "WHERE p.user_id = ? " +
                        "ORDER BY p.date_creation DESC";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();

        List<Projet> liste = new ArrayList<>();

        while (rs.next()) {
            Projet p = new Projet();
            p.setId(rs.getInt("id"));
            p.setUserId(rs.getInt("user_id"));
            p.setTitre(rs.getString("titre"));
            p.setDescription(rs.getString("description"));
            p.setSecteur(rs.getString("secteur"));
            p.setEtape(rs.getString("etape"));
            p.setStatut(rs.getString("statut"));

            Date dateCreation = rs.getDate("date_creation");
            if (dateCreation != null) p.setDateCreation(dateCreation.toLocalDate());

            String statutProjetStr = rs.getString("statut_projet");
            if (statutProjetStr != null) {
                try { p.setStatutProjet(StatutProjet.valueOf(statutProjetStr)); }
                catch (Exception e) { p.setStatutProjet(StatutProjet.BROUILLON); }
            }

            p.setScoreGlobal(rs.getDouble("score_global"));
            p.setDiagnosticIA(rs.getString("diagnostic_ia"));

            Date dateSoumission = rs.getDate("date_soumission");
            if (dateSoumission != null) p.setDateSoumission(dateSoumission.toLocalDate());

            Date dateEvaluation = rs.getDate("date_evaluation");
            if (dateEvaluation != null) p.setDateEvaluation(dateEvaluation.toLocalDate());

            if (rs.getObject("db_id") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db_id"));
                db.setTailleMarche(rs.getString("taille_marche"));
                db.setModeleRevenu(rs.getString("modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("revenus_attendus"));
                db.setNiveauRisque(rs.getString("niveau_risque"));
                db.setForceEquipe(rs.getInt("force_equipe"));
                db.setProjetId(p.getId());
                db.setMargeEstimee(rs.getDouble("marge_estimee"));
                db.setRatioRentabilite(rs.getDouble("ratio_rentabilite"));
                db.setScoreFinancier(rs.getDouble("score_financier"));
                db.setScoreMarche(rs.getDouble("score_marche"));
                db.setScoreEquipeCalcule(rs.getDouble("score_equipe_calcule"));
                db.setScoreRisqueCalcule(rs.getDouble("score_risque_calcule"));
                // ✅ CHAMPS RAW — la clef de tout
                db.setRawCouts(rs.getString("raw_couts"));
                db.setRawRevenus(rs.getString("raw_revenus"));
                db.setRawForceEquipe(rs.getString("raw_force_equipe"));
                p.setDonneesBusiness(db);
            }

            liste.add(p);
            System.out.println("✅ Projet chargé : ID=" + p.getId()
                    + ", Titre=" + p.getTitre()
                    + ", rawCouts=[" + (p.getDonneesBusiness() != null ? p.getDonneesBusiness().getRawCouts() : "null") + "]");
        }

        System.out.println("📊 Total projets pour user " + userId + " : " + liste.size());
        return liste;
    }

    // ================= USER EXISTS =================

    public boolean userExists(int userId) throws SQLException {
        String sql = "SELECT id FROM user WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public boolean userExiste(int userId) throws SQLException {
        String sql = "SELECT id FROM user WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }

    // ================= RESET SCORE =================

    public void resetScore(int projetId) throws SQLException {
        String sql = "UPDATE projet SET score_global = 0, diagnostic_ia = NULL, " +
                "statut_projet = 'SOUMIS', date_evaluation = NULL " +
                "WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, projetId);
        int rows = pst.executeUpdate();
        System.out.println("🔄 Score resetté pour projet " + projetId + " (" + rows + " ligne) — prêt pour réévaluation");
    }
}