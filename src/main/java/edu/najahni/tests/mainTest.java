package edu.najahni.tests;

import edu.najahni.entities.Projet;
import edu.najahni.entities.donneesBusiness;
import edu.najahni.services.projetCRUD;
import edu.najahni.tools.MyBD;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class mainTest {

    public static void main(String[] args) {

        projetCRUD crud = new projetCRUD();

        try {
            // Vérification connexion
            if (MyBD.getInstance().getConn() == null) {
                System.out.println("Échec connexion");
                return;
            }
            System.out.println("Connexion à la base OK !\n");

            // ────────────────────────────────────────────────
            // CAS 1 : Tentative d'ajout SANS données business → DOIT ÉCHOUER
            // ────────────────────────────────────────────────
            System.out.println("=== CAS 1 : Ajout SANS données business (doit échouer) ===");

            Projet projetSansDB = new Projet();
            projetSansDB.setTitre("Projet Test Sans DB");
            projetSansDB.setDescription("Tentative interdite");
            projetSansDB.setSecteur("Test");
            projetSansDB.setEtape("BROUILLON");
            projetSansDB.setStatut("NOUVEAU");
            projetSansDB.setDateCreation(LocalDate.now());

            // Attention : PAS de setDonneesBusiness → null
            // projetSansDB.setDonneesBusiness(null); // explicite ou implicite

            try {
                crud.ajouter(projetSansDB);
                System.out.println("ERREUR : L'ajout a réussi alors qu'il ne devrait pas !");
            } catch (IllegalArgumentException e) {
                System.out.println("Succès du test : Exception métier bien levée");
                System.out.println("Message : " + e.getMessage());
                System.out.println("→ Règle métier respectée : projet sans DB refusé !\n");
            } catch (SQLException e) {
                System.out.println("Erreur SQL inattendue : " + e.getMessage());
            }

            // ────────────────────────────────────────────────
            // CAS 2 : Ajout AVEC données business → DOIT RÉUSSIR
            // ────────────────────────────────────────────────
            System.out.println("=== CAS 2 : Ajout AVEC données business (doit réussir) ===");

            Projet projetAvecDB = new Projet();
            projetAvecDB.setTitre("Projet Test Avec DB Obligatoire");
            projetAvecDB.setDescription("Projet valide avec toutes les données business");
            projetAvecDB.setSecteur("FinTech");
            projetAvecDB.setEtape("SOUMIS");
            projetAvecDB.setStatut("EN_COURS");
            projetAvecDB.setDateCreation(LocalDate.now());

            // OBLIGATOIRE : on fournit les données business
            donneesBusiness db = new donneesBusiness();
            db.setTailleMarche("MOYEN");
            db.setModeleRevenu("Abonnement + publicité");
            db.setCoutsEstimes(18000.0);
            db.setRevenusAttendus(72000.0);
            db.setNiveauRisque("MOYEN");
            db.setForceEquipe(7);

            projetAvecDB.setDonneesBusiness(db);

            crud.ajouter(projetAvecDB);
            System.out.println("Ajout réussi !");
            System.out.println("ID généré : " + projetAvecDB.getId() + "\n");

            // Affichage final pour voir le résultat
            System.out.println("=== État final de la base après les tests ===");
            afficherTous(crud);

        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche TOUS les attributs de Projet + donneesBusiness
     */
    private static void afficherTous(projetCRUD crud) throws SQLException {
        List<Projet> liste = crud.afficher();

        if (liste.isEmpty()) {
            System.out.println("Aucun projet dans la base de données.");
            return;
        }

        System.out.println("=== AFFICHAGE COMPLET DES PROJETS ===");
        System.out.println("Nombre total de projets : " + liste.size());
        System.out.println("=".repeat(80) + "\n");

        for (Projet p : liste) {
            System.out.println("PROJET #" + p.getId());
            System.out.println("  Titre              : " + p.getTitre());
            System.out.println("  Description        : " + p.getDescription());
            System.out.println("  Secteur            : " + p.getSecteur());
            System.out.println("  Étape              : " + p.getEtape());
            System.out.println("  Statut             : " + p.getStatut());
            System.out.println("  Date création      : " + p.getDateCreation());

            donneesBusiness db = p.getDonneesBusiness();
            if (db != null) {
                System.out.println("  ────────────────────────────────────────────────");
                System.out.println("  DONNÉES BUSINESS (ID " + db.getId() + ")");
                System.out.println("    Taille marché      : " + db.getTailleMarche());
                System.out.println("    Modèle revenu      : " + db.getModeleRevenu());
                System.out.println("    Coûts estimés      : " + db.getCoutsEstimes());
                System.out.println("    Revenus attendus   : " + db.getRevenusAttendus());
                System.out.println("    Niveau risque      : " + db.getNiveauRisque());
                System.out.println("    Force équipe       : " + db.getForceEquipe());
                System.out.println("    Projet lié (ID)    : " + db.getProjetId());
            } else {
                System.out.println("  → Pas de données business associées");
            }

            System.out.println("=".repeat(80) + "\n");
        }
    }
}