package edu.najahni.services;

import edu.najahni.entities.Projet;
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

        String reqProjet =
                "INSERT INTO projet (user_id, titre, description, secteur, etape, statut, date_creation) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstProjet = conn.prepareStatement(reqProjet, Statement.RETURN_GENERATED_KEYS);

        pstProjet.setInt(1, projet.getUserId());
        pstProjet.setString(2, projet.getTitre());
        pstProjet.setString(3, projet.getDescription());
        pstProjet.setString(4, projet.getSecteur());
        pstProjet.setString(5, projet.getEtape());
        pstProjet.setString(6, projet.getStatut());
        pstProjet.setDate(7, Date.valueOf(projet.getDateCreation()));

        pstProjet.executeUpdate();

        ResultSet generatedKeys = pstProjet.getGeneratedKeys();

        if (generatedKeys.next()) {
            int projetId = generatedKeys.getInt(1);
            projet.setId(projetId);

            donneesBusiness db = projet.getDonneesBusiness();

            String reqDB =
                    "INSERT INTO donnees_business " +
                            "(taille_marche, modele_revenu, couts_estimes, revenus_attendus, niveau_risque, force_equipe, projet_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstDB = conn.prepareStatement(reqDB);

            pstDB.setString(1, db.getTailleMarche());
            pstDB.setString(2, db.getModeleRevenu());
            pstDB.setDouble(3, db.getCoutsEstimes());
            pstDB.setDouble(4, db.getRevenusAttendus());
            pstDB.setString(5, db.getNiveauRisque());
            pstDB.setInt(6, db.getForceEquipe());
            pstDB.setInt(7, projetId);

            pstDB.executeUpdate();
        }
    }

    // ================= AFFICHER (CORRIGÉ) =================

    public List<Projet> afficher() throws SQLException {
        String req = "SELECT " +
                "p.id AS projet_id, " +
                "p.user_id AS projet_user_id, " +
                "p.titre AS projet_titre, " +
                "p.description AS projet_description, " +
                "p.secteur AS projet_secteur, " +
                "p.etape AS projet_etape, " +
                "p.statut AS projet_statut, " +
                "p.date_creation AS projet_date_creation, " +
                "db.id AS db_id, " +
                "db.taille_marche AS db_taille_marche, " +
                "db.modele_revenu AS db_modele_revenu, " +
                "db.couts_estimes AS db_couts_estimes, " +
                "db.revenus_attendus AS db_revenus_attendus, " +
                "db.niveau_risque AS db_niveau_risque, " +
                "db.force_equipe AS db_force_equipe, " +
                "db.projet_id AS db_projet_id " +
                "FROM projet p " +
                "LEFT JOIN donnees_business db ON p.id = db.projet_id " +
                "ORDER BY p.date_creation DESC";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Projet> liste = new ArrayList<>();

        while (rs.next()) {
            Projet p = new Projet();

            p.setId(rs.getInt("projet_id"));
            p.setUserId(rs.getInt("projet_user_id"));
            p.setTitre(rs.getString("projet_titre"));
            p.setDescription(rs.getString("projet_description"));
            p.setSecteur(rs.getString("projet_secteur"));
            p.setEtape(rs.getString("projet_etape"));
            p.setStatut(rs.getString("projet_statut"));

            Date dateCreation = rs.getDate("projet_date_creation");
            if (dateCreation != null) {
                p.setDateCreation(dateCreation.toLocalDate());
            }

            if (rs.getObject("db_projet_id") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db_id"));
                db.setTailleMarche(rs.getString("db_taille_marche"));
                db.setModeleRevenu(rs.getString("db_modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("db_couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("db_revenus_attendus"));
                db.setNiveauRisque(rs.getString("db_niveau_risque"));
                db.setForceEquipe(rs.getInt("db_force_equipe"));
                db.setProjetId(p.getId());

                p.setDonneesBusiness(db);
            }

            liste.add(p);

            System.out.println("✅ Projet chargé : ID=" + p.getId() +
                    ", Titre=" + p.getTitre() +
                    ", UserID=" + p.getUserId());
        }

        System.out.println("📊 Total projets récupérés : " + liste.size());

        return liste;
    }

    // ================= MODIFIER =================

    public void modifier(Projet projet) throws SQLException {
        String req =
                "UPDATE projet SET titre=?, description=?, secteur=?, etape=?, statut=? " +
                        "WHERE id=? AND user_id=?";

        PreparedStatement pst = conn.prepareStatement(req);

        pst.setString(1, projet.getTitre());
        pst.setString(2, projet.getDescription());
        pst.setString(3, projet.getSecteur());
        pst.setString(4, projet.getEtape());
        pst.setString(5, projet.getStatut());
        pst.setInt(6, projet.getId());
        pst.setInt(7, projet.getUserId());

        pst.executeUpdate();

        if (projet.getDonneesBusiness() != null) {
            String reqDB =
                    "UPDATE donnees_business SET taille_marche=?, modele_revenu=?, " +
                            "couts_estimes=?, revenus_attendus=?, niveau_risque=?, force_equipe=? " +
                            "WHERE projet_id=?";

            PreparedStatement pstDB = conn.prepareStatement(reqDB);

            donneesBusiness db = projet.getDonneesBusiness();

            pstDB.setString(1, db.getTailleMarche());
            pstDB.setString(2, db.getModeleRevenu());
            pstDB.setDouble(3, db.getCoutsEstimes());
            pstDB.setDouble(4, db.getRevenusAttendus());
            pstDB.setString(5, db.getNiveauRisque());
            pstDB.setInt(6, db.getForceEquipe());
            pstDB.setInt(7, projet.getId());

            pstDB.executeUpdate();
        }
    }

    // ================= SUPPRIMER (Version Admin) =================
    public void supprimer(int id) throws SQLException {
        System.out.println("🗑️ Suppression du projet ID=" + id);

        // La suppression des données business se fait automatiquement avec CASCADE
        String reqProjet = "DELETE FROM projet WHERE id = ?";
        PreparedStatement pstProjet = conn.prepareStatement(reqProjet);
        pstProjet.setInt(1, id);
        pstProjet.executeUpdate();

        System.out.println("✅ Projet et ses données business supprimés automatiquement !");
    }

    // ================= SUPPRIMER (Version Client) =================
    public void supprimer(int projetId, int userId) throws SQLException {
        System.out.println("🗑️ Suppression du projet ID=" + projetId + " par user ID=" + userId);

        // La suppression des données business se fait automatiquement avec CASCADE
        String deleteProjet = "DELETE FROM projet WHERE id = ? AND user_id = ?";
        PreparedStatement pst = conn.prepareStatement(deleteProjet);
        pst.setInt(1, projetId);
        pst.setInt(2, userId);
        int rowsAffected = pst.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("Vous n'êtes pas autorisé à supprimer ce projet ou il n'existe pas.");
        }

        System.out.println("✅ Projet et ses données business supprimés automatiquement !");
    }

    // ================= AFFICHER PAR USER =================

    public List<Projet> afficherParUser(int userId) throws SQLException {
        String req =
                "SELECT p.*, " +
                        "db.id AS db_id, " +
                        "db.taille_marche, db.modele_revenu, db.couts_estimes, " +
                        "db.revenus_attendus, db.niveau_risque, db.force_equipe " +
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
            if (dateCreation != null) {
                p.setDateCreation(dateCreation.toLocalDate());
            }

            if (rs.getString("taille_marche") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db_id"));
                db.setTailleMarche(rs.getString("taille_marche"));
                db.setModeleRevenu(rs.getString("modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("revenus_attendus"));
                db.setNiveauRisque(rs.getString("niveau_risque"));
                db.setForceEquipe(rs.getInt("force_equipe"));
                db.setProjetId(p.getId());

                p.setDonneesBusiness(db);
            }

            liste.add(p);

            System.out.println("✅ Projet user chargé : ID=" + p.getId() +
                    ", Titre=" + p.getTitre());
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
}