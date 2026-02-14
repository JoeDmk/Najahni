package com.najahni.services;

import com.najahni.dao.CoursDAO;
import com.najahni.models.Cours;
import com.najahni.models.NiveauCours;
import com.najahni.models.TypeCours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CoursService.
 * Utilise JUnit 5 et Mockito pour tester la logique métier dels cours.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests CoursService - Gestion des Cours")
class CoursServiceTest {

    @Mock
    private CoursDAO coursDAO;

    private CoursService coursService;

    private Cours courTest;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Créer le service sans appeler le constructeur pour éviter la connexion DB
        Objenesis objenesis = new ObjenesisStd();
        coursService = objenesis.newInstance(CoursService.class);
        
        // Injecter le mock DAO dans le service via réflexion
        Field daoField = CoursService.class.getDeclaredField("coursDAO");
        daoField.setAccessible(true);
        daoField.set(coursService, coursDAO);

        // Initialiser un cours de test
        courTest = new Cours();
        courTest.setId(1);
        courTest.setTitre("Java Avancé");
        courTest.setDescription("Cours avancé de Java");
        courTest.setType(TypeCours.VIDEO);
        courTest.setNiveau(NiveauCours.INTERMEDIAIRE);
        courTest.setCertification(true);
        courTest.setPointsXP(200);
        courTest.setDureeMinutes(120);
        courTest.setCreateurId(5);
    }

    // ===== Tests de Création =====

    @Test
    @DisplayName("Créer un cours valide - succès")
    void testCreerCoursValide() {
        // Arrange
        when(coursDAO.create(courTest)).thenReturn(courTest);

        // Act
        Cours resultat = coursService.creerCours(courTest);

        // Assert
        assertNotNull(resultat, "Le cours créé ne doit pas être null");
        assertEquals(courTest.getTitre(), resultat.getTitre(), "Les titres doivent correspondre");
        assertEquals(courTest.getNiveau(), resultat.getNiveau(), "Les niveaux doivent correspondre");
        verify(coursDAO).create(courTest);
    }

    @Test
    @DisplayName("Créer un cours null - lance exception")
    void testCreerCoursNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(null),
                "Doit lever une exception pour un cours null");
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours sans titre - lance exception")
    void testCreerCoursSansTitre() {
        // Arrange
        courTest.setTitre(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours avec titre vide - lance exception")
    void testCreerCoursAvecTitreVide() {
        // Arrange
        courTest.setTitre("   ");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours avec titre trop long - lance exception")
    void testCreerCoursAvecTitreTropLong() {
        // Arrange
        courTest.setTitre("a".repeat(256));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours avec points XP négatifs - lance exception")
    void testCreerCoursAvecPointsXPNegatifs() {
        // Arrange
        courTest.setPointsXP(-10);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours avec durée négative - lance exception")
    void testCreerCoursAvecDureeNegative() {
        // Arrange
        courTest.setDureeMinutes(-30);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours sans niveau - lance exception")
    void testCreerCoursSansNiveau() {
        // Arrange
        courTest.setNiveau(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours sans type - lance exception")
    void testCreerCoursSansType() {
        // Arrange
        courTest.setType(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    @Test
    @DisplayName("Créer un cours avec description trop longue - lance exception")
    void testCreerCoursAvecDescriptionTropLongue() {
        // Arrange
        courTest.setDescription("a".repeat(2001));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coursService.creerCours(courTest));
        verify(coursDAO, never()).create(any());
    }

    // ===== Tests de Modification =====

    @Test
    @DisplayName("Modifier un cours valide - succès")
    void testModifierCoursValide() {
        // Arrange
        when(coursDAO.update(courTest)).thenReturn(true);

        // Act
        boolean resultat = coursService.modifierCours(courTest);

        // Assert
        assertTrue(resultat, "La modification doit rétourner true");
        verify(coursDAO).update(courTest);
    }

    @Test
    @DisplayName("Modifier un cours - échec")
    void testModifierCoursEchec() {
        // Arrange
        when(coursDAO.update(courTest)).thenReturn(false);

        // Act
        boolean resultat = coursService.modifierCours(courTest);

        // Assert
        assertFalse(resultat, "La modification doit rétourner false");
    }

    // ===== Tests de Suppression =====

    @Test
    @DisplayName("Supprimer un cours - succès")
    void testSupprimerCoursSucces() {
        // Arrange
        when(coursDAO.delete(1)).thenReturn(true);

        // Act
        boolean resultat = coursService.supprimerCours(1);

        // Assert
        assertTrue(resultat, "La suppression doit rétourner true");
        verify(coursDAO).delete(1);
    }

    @Test
    @DisplayName("Supprimer un cours - échec")
    void testSupprimerCoursEchec() {
        // Arrange
        when(coursDAO.delete(999)).thenReturn(false);

        // Act
        boolean resultat = coursService.supprimerCours(999);

        // Assert
        assertFalse(resultat, "La suppression d'un cours inexistant doit rétourner false");
    }

    // ===== Tests de Recherche =====

    @Test
    @DisplayName("Trouver un cours par ID - trouvé")
    void testTrouverParIdTrouve() {
        // Arrange
        when(coursDAO.findById(1)).thenReturn(Optional.of(courTest));

        // Act
        Optional<Cours> resultat = coursService.trouverParId(1);

        // Assert
        assertTrue(resultat.isPresent(), "Le cours doit être trouvé");
        assertEquals(courTest.getTitre(), resultat.get().getTitre());
        verify(coursDAO).findById(1);
    }

    @Test
    @DisplayName("Trouver un cours par ID - non trouvé")
    void testTrouverParIdNonTrouve() {
        // Arrange
        when(coursDAO.findById(999)).thenReturn(Optional.empty());

        // Act
        Optional<Cours> resultat = coursService.trouverParId(999);

        // Assert
        assertTrue(resultat.isEmpty(), "Le cours ne doit pas être trouvé");
    }

    @Test
    @DisplayName("Trouver tous les cours")
    void testTrouverTous() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        Cours cours2 = new Cours();
        cours2.setId(2);
        cours2.setTitre("Python Basique");
        coursList.add(cours2);

        when(coursDAO.findAll()).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.trouverTous();

        // Assert
        assertEquals(2, resultat.size(), "Doit retourner 2 cours");
        verify(coursDAO).findAll();
    }

    @ParameterizedTest
    @EnumSource(NiveauCours.class)
    @DisplayName("Trouver cours par niveau - tous les niveaux")
    void testTrouverParNiveau(NiveauCours niveau) {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findByNiveau(niveau)).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.trouverParNiveau(niveau);

        // Assert
        assertNotNull(resultat, "Le résultat ne doit pas être null");
        verify(coursDAO).findByNiveau(niveau);
    }

    @ParameterizedTest
    @EnumSource(TypeCours.class)
    @DisplayName("Trouver cours par type - tous les types")
    void testTrouverParType(TypeCours type) {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findByType(type)).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.trouverParType(type);

        // Assert
        assertNotNull(resultat, "Le résultat ne doit pas être null");
        verify(coursDAO).findByType(type);
    }

    @Test
    @DisplayName("Trouver cours certifiants")
    void testTrouverCoursCertifiants() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findWithCertification()).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.trouverCoursCertifiants();

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner 1 cours certifiant");
        assertTrue(resultat.get(0).isCertification(), "Le cours doit être certifiant");
        verify(coursDAO).findWithCertification();
    }

    @Test
    @DisplayName("Trouver cours par créateur")
    void testTrouverParCreateur() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findByCreateur(5)).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.trouverParCreateur(5);

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner 1 cours");
        verify(coursDAO).findByCreateur(5);
    }

    @Test
    @DisplayName("Rechercher cours avec mot-clé valide")
    void testRechercherAvecMotCle() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.search("Java")).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.rechercher("Java");

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner 1 cours");
        verify(coursDAO).search("Java");
    }

    @Test
    @DisplayName("Rechercher cours avec mot-clé null - retourne tous")
    void testRechercherAvecMotCleNull() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findAll()).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.rechercher(null);

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner tous les cours");
        verify(coursDAO).findAll();
        verify(coursDAO, never()).search(any());
    }

    @Test
    @DisplayName("Rechercher cours avec mot-clé vide - retourne tous")
    void testRechercherAvecMotCleVide() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findAll()).thenReturn(coursList);

        // Act
        List<Cours> resultat = coursService.rechercher("   ");

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner tous les cours");
        verify(coursDAO).findAll();
    }

    // ===== Tests de Comptage =====

    @Test
    @DisplayName("Compter tous les cours")
    void testCompterTous() {
        // Arrange
        when(coursDAO.count()).thenReturn(5);

        // Act
        int resultat = coursService.compterTous();

        // Assert
        assertEquals(5, resultat, "Doit retourner 5 cours");
        verify(coursDAO).count();
    }

    @Test
    @DisplayName("Compter cours certifiants")
    void testCompterCertifiants() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        coursList.add(courTest);
        when(coursDAO.findAll()).thenReturn(coursList);

        // Act
        int resultat = coursService.compterCertifiants();

        // Assert
        assertEquals(1, resultat, "Doit compter 1 cours certifiant");
    }

    @Test
    @DisplayName("Calculer total XP tous les cours")
    void testCalculerTotalXP() {
        // Arrange
        List<Cours> coursList = new ArrayList<>();
        courTest.setPointsXP(100);
        coursList.add(courTest);
        Cours cours2 = new Cours();
        cours2.setPointsXP(150);
        coursList.add(cours2);
        when(coursDAO.findAll()).thenReturn(coursList);

        // Act
        int resultat = coursService.calculerTotalXP();

        // Assert
        assertEquals(250, resultat, "Total XP doit être 250");
    }
}
