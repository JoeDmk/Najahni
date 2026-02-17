package edu.najahni.services;

import edu.najahni.entities.Projet;
import edu.najahni.entities.donneesBusiness;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import  edu.najahni.services.projetCRUD;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class projetCRUDTest {
    private static projetCRUD pcr;
    @BeforeAll
    static  void setUP () {
        pcr= new projetCRUD();

    }

    @Test
    void ajouter() throws SQLException {
        Projet p = new Projet();
        p.setTitre("Test Projet");
        p.setDescription("Test Description");
        p.setSecteur("IT");
        p.setEtape("Prototype");
        p.setStatut("Actif");
        p.setDateCreation(LocalDate.now());
         // ⚠️ Make sure user 1 exists in DB

        donneesBusiness db = new donneesBusiness();
        db.setTailleMarche("Large");
        db.setModeleRevenu("Subscription");
        db.setCoutsEstimes(1000);
        db.setRevenusAttendus(5000);
        db.setNiveauRisque("Faible");
        db.setForceEquipe(8);

        p.setDonneesBusiness(db);
        pcr.ajouter(p);
    }

    @Test
    void afficher() throws SQLException {
        List<Projet> projets = pcr.afficher();

        // Assert
        assertFalse(projets.isEmpty(),"liste vide");

        boolean found = projets.stream()
                .anyMatch(pr -> pr.getTitre().equals("Test Projet"));

        assertTrue(found);


    }

    }

