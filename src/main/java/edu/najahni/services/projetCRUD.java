package edu.najahni.services;

import edu.najahni.entities.Projet;
import edu.najahni.entities.donneesBusiness;
import edu.najahni.tools.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class projetCRUD implements intrefaceCRUD<Projet> {

    Connection conn;

    public projetCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(Projet projet) throws SQLException {

        // RÈGLE MÉTIER OBLIGATOIRE : les données business sont requises pour tout projet
        if (projet.getDonneesBusiness() == null) {
            throw new IllegalArgumentException(
                    "Erreur métier : Impossible de créer un projet sans ses données business.\n" +
                            "Veuillez obligatoirement renseigner :\n" +
                            "- Taille du marché\n" +
                            "- Modèle de revenu\n" +
                            "- Coûts estimés\n" +
                            "- Revenus attendus\n" +
                            "- Niveau de risque\n" +
                            "- Force de l'équipe"
            );
        }

        // 1. Insertion du projet principal
        String reqProjet = "INSERT INTO projet (titre, description, secteur, etape, statut, date_creation) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement pstProjet = conn.prepareStatement(reqProjet, Statement.RETURN_GENERATED_KEYS);
        pstProjet.setString(1, projet.getTitre());
        pstProjet.setString(2, projet.getDescription());
        pstProjet.setString(3, projet.getSecteur());
        pstProjet.setString(4, projet.getEtape());
        pstProjet.setString(5, projet.getStatut());
        pstProjet.setDate(6, Date.valueOf(projet.getDateCreation()));

        pstProjet.executeUpdate();

        // 2. Récupération de l'ID généré
        ResultSet generatedKeys = pstProjet.getGeneratedKeys();
        if (generatedKeys.next()) {
            int projetId = generatedKeys.getInt(1);
            projet.setId(projetId);

            // 3. Insertion obligatoire des données business
            donneesBusiness db = projet.getDonneesBusiness();
            String reqDB = "INSERT INTO donnees_business (taille_marche, modele_revenu, couts_estimes, revenus_attendus, niveau_risque, force_equipe, projet_id) " +
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
            System.out.println("Données business ajoutées (obligatoires) !");
        }

        System.out.println("Projet ajouté avec succès !");
    }

    @Override
    public void modifier(Projet projet) throws SQLException {
        String reqProjet = "UPDATE projet SET titre=?, description=?, secteur=?, etape=?, statut=?, date_creation=? WHERE id=?";

        PreparedStatement pstProjet = conn.prepareStatement(reqProjet);
        pstProjet.setString(1, projet.getTitre());
        pstProjet.setString(2, projet.getDescription());
        pstProjet.setString(3, projet.getSecteur());
        pstProjet.setString(4, projet.getEtape());
        pstProjet.setString(5, projet.getStatut());
        pstProjet.setDate(6, Date.valueOf(projet.getDateCreation()));
        pstProjet.setInt(7, projet.getId());

        pstProjet.executeUpdate();

        donneesBusiness db = projet.getDonneesBusiness();
        if (db != null) {
            // Vérifier si existe déjà
            String checkReq = "SELECT id FROM donnees_business WHERE projet_id = ?";
            PreparedStatement checkPst = conn.prepareStatement(checkReq);
            checkPst.setInt(1, projet.getId());
            ResultSet rsCheck = checkPst.executeQuery();

            if (rsCheck.next()) {
                // Update
                String reqDB = "UPDATE donnees_business SET taille_marche=?, modele_revenu=?, couts_estimes=?, revenus_attendus=?, niveau_risque=?, force_equipe=? WHERE projet_id=?";

                PreparedStatement pstDB = conn.prepareStatement(reqDB);
                pstDB.setString(1, db.getTailleMarche());
                pstDB.setString(2, db.getModeleRevenu());
                pstDB.setDouble(3, db.getCoutsEstimes());
                pstDB.setDouble(4, db.getRevenusAttendus());
                pstDB.setString(5, db.getNiveauRisque());
                pstDB.setInt(6, db.getForceEquipe());
                pstDB.setInt(7, projet.getId());

                pstDB.executeUpdate();
                System.out.println("Données business modifiées !");
            } else {
                // Insert si pas existant
                String reqInsert = "INSERT INTO donnees_business (taille_marche, modele_revenu, couts_estimes, revenus_attendus, niveau_risque, force_equipe, projet_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstInsert = conn.prepareStatement(reqInsert);
                pstInsert.setString(1, db.getTailleMarche());
                pstInsert.setString(2, db.getModeleRevenu());
                pstInsert.setDouble(3, db.getCoutsEstimes());
                pstInsert.setDouble(4, db.getRevenusAttendus());
                pstInsert.setString(5, db.getNiveauRisque());
                pstInsert.setInt(6, db.getForceEquipe());
                pstInsert.setInt(7, projet.getId());

                pstInsert.executeUpdate();
                System.out.println("Données business ajoutées (lors de la modification) !");
            }
        }
        System.out.println("Projet modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Supprimer d'abord les données business (clé étrangère)
        String reqDB = "DELETE FROM donnees_business WHERE projet_id = ?";
        PreparedStatement pstDB = conn.prepareStatement(reqDB);
        pstDB.setInt(1, id);
        pstDB.executeUpdate();

        // Puis supprimer le projet
        String reqProjet = "DELETE FROM projet WHERE id = ?";
        PreparedStatement pstProjet = conn.prepareStatement(reqProjet);
        pstProjet.setInt(1, id);
        pstProjet.executeUpdate();

        System.out.println("Projet et ses données business supprimés !");
    }

    @Override
    public List<Projet> afficher() throws SQLException {
        String req = "SELECT p.*, db.* FROM projet p LEFT JOIN donnees_business db ON p.id = db.projet_id";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<Projet> liste = new ArrayList<>();

        while (rs.next()) {
            Projet p = new Projet();
            p.setId(rs.getInt("p.id"));
            p.setTitre(rs.getString("p.titre"));
            p.setDescription(rs.getString("p.description"));
            p.setSecteur(rs.getString("p.secteur"));
            p.setEtape(rs.getString("p.etape"));
            p.setStatut(rs.getString("p.statut"));
            p.setDateCreation(rs.getDate("p.date_creation").toLocalDate());

            // Si données business existent
            if (rs.getObject("db.id") != null) {
                donneesBusiness db = new donneesBusiness();
                db.setId(rs.getInt("db.id"));
                db.setTailleMarche(rs.getString("db.taille_marche"));
                db.setModeleRevenu(rs.getString("db.modele_revenu"));
                db.setCoutsEstimes(rs.getDouble("db.couts_estimes"));
                db.setRevenusAttendus(rs.getDouble("db.revenus_attendus"));
                db.setNiveauRisque(rs.getString("db.niveau_risque"));
                db.setForceEquipe(rs.getInt("db.force_equipe"));
                db.setProjetId(p.getId());

                p.setDonneesBusiness(db);
            }

            liste.add(p);
        }

        return liste;
    }
}