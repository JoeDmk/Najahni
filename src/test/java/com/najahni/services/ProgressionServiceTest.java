package com.najahni.services;

import com.najahni.dao.CoursDAO;
import com.najahni.dao.ProgressionDAO;
import com.najahni.models.Cours;
import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ProgressionService.
 * Utilise JUnit 5 et Mockito pour tester la logique métier de progression des utilisateurs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests ProgressionService - Gestion de la Progression")
class ProgressionServiceTest {

    @Mock
    private ProgressionDAO progressionDAO;

    @Mock
    private CoursDAO coursDAO;

    @Mock
    private BadgeService badgeService;

    private ProgressionService progressionService;

    private Progression progressionTest;
    private Cours coursTest;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Créer le service sans appeler le constructeur pour éviter la connexion DB
        Objenesis objenesis = new ObjenesisStd();
        progressionService = objenesis.newInstance(ProgressionService.class);
        
        // Injecter les mocks dans le service via réflexion
        Field progressionDaoField = ProgressionService.class.getDeclaredField("progressionDAO");
        progressionDaoField.setAccessible(true);
        progressionDaoField.set(progressionService, progressionDAO);
        
        Field coursDaoField = ProgressionService.class.getDeclaredField("coursDAO");
        coursDaoField.setAccessible(true);
        coursDaoField.set(progressionService, coursDAO);
        
        Field badgeServiceField = ProgressionService.class.getDeclaredField("badgeService");
        badgeServiceField.setAccessible(true);
        badgeServiceField.set(progressionService, badgeService);
        // Initialiser une progression de test
        progressionTest = new Progression();
        progressionTest.setId(1);
        progressionTest.setUserId(10);
        progressionTest.setCoursId(5);
        progressionTest.setPourcentage(50.0);
        progressionTest.setPointsXP(100);
        progressionTest.setNiveau(2);
        progressionTest.setEtat(EtatProgression.EN_COURS);
        progressionTest.setDateDebut(LocalDateTime.now());

        // Initialiser un cours de test
        coursTest = new Cours();
        coursTest.setId(5);
        coursTest.setTitre("Java Avancé");
        coursTest.setPointsXP(200);
        coursTest.setCertification(true);
    }

    // ===== Tests de Démarrage de Cours =====

    @Test
    @DisplayName("Démarrer un cours - nouvelle progression")
    void testDemarrerCoursNouvelleProgression() {
        // Arrange
        when(progressionDAO.findByUserAndCours(10, 5)).thenReturn(Optional.empty());
        when(progressionDAO.create(any(Progression.class))).thenReturn(progressionTest);

        // Act
        Progression resultat = progressionService.demarrerCours(10, 5);

        // Assert
        assertNotNull(resultat, "La progression ne doit pas être null");
        assertEquals(EtatProgression.EN_COURS, resultat.getEtat(), "L'état doit être EN_COURS");
        assertEquals(10, resultat.getUserId(), "L'ID utilisateur doit correspondre");
        assertEquals(5, resultat.getCoursId(), "L'ID cours doit correspondre");
        verify(progressionDAO).create(any(Progression.class));
    }

    @Test
    @DisplayName("Démarrer un cours - progression existante")
    void testDemarrerCoursProgressionExistante() {
        // Arrange
        when(progressionDAO.findByUserAndCours(10, 5)).thenReturn(Optional.of(progressionTest));

        // Act
        Progression resultat = progressionService.demarrerCours(10, 5);

        // Assert
        assertNotNull(resultat, "La progression existante doit être retournée");
        assertEquals(progressionTest.getId(), resultat.getId());
        verify(progressionDAO, never()).create(any());
    }

    // ===== Tests de Mise à Jour du Pourcentage =====

    @Test
    @DisplayName("Mettre à jour pourcentage - progression incomplète")
    void testMettreAJourPourcentageIncomplet() {
        // Arrange
        when(progressionDAO.findById(1)).thenReturn(Optional.of(progressionTest));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJourPourcentage(1, 75.0);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        ArgumentCaptor<Progression> captor = ArgumentCaptor.forClass(Progression.class);
        verify(progressionDAO).update(captor.capture());
        assertEquals(75.0, captor.getValue().getPourcentage(), "Le pourcentage doit être 75.0");
        assertNull(captor.getValue().getDateObtention(), "La date d'obtention ne doit pas être définie");
    }

    @Test
    @DisplayName("Mettre à jour pourcentage à 100% - marque comme complétée")
    void testMettreAJourPourcentage100Pourcent() {
        // Arrange
        progressionTest.setPourcentage(0);
        when(progressionDAO.findById(1)).thenReturn(Optional.of(progressionTest));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJourPourcentage(1, 100.0);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        ArgumentCaptor<Progression> captor = ArgumentCaptor.forClass(Progression.class);
        verify(progressionDAO).update(captor.capture());
        assertEquals(100.0, captor.getValue().getPourcentage(), "Le pourcentage doit être 100.0");
        assertEquals(EtatProgression.COMPLETE, captor.getValue().getEtat(), "L'état doit être COMPLETE");
        assertNotNull(captor.getValue().getDateObtention(), "La date d'obtention doit être définie");
    }

    @Test
    @DisplayName("Mettre à jour pourcentage - progression inexistante")
    void testMettreAJourPourcentageProgressionInexistante() {
        // Arrange
        when(progressionDAO.findById(999)).thenReturn(Optional.empty());

        // Act
        boolean resultat = progressionService.mettreAJourPourcentage(999, 75.0);

        // Assert
        assertFalse(resultat, "Doit retourner false pour une progression inexistante");
        verify(progressionDAO, never()).update(any());
    }

    // ===== Tests de Mise à Jour Avec XP =====

    @Test
    @DisplayName("Mettre à jour progression avec XP - succès")
    void testMettreAJourProgressionAvecXP() {
        // Arrange
        when(progressionDAO.findById(1)).thenReturn(Optional.of(progressionTest));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJourProgression(1, 50.0, 100);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        ArgumentCaptor<Progression> captor = ArgumentCaptor.forClass(Progression.class);
        verify(progressionDAO).update(captor.capture());
        assertEquals(50.0, captor.getValue().getPourcentage(), "Le pourcentage doit être 50.0");
        verify(badgeService, never()).verifierEtAttribuerBadges(anyInt());
    }

    @Test
    @DisplayName("Mettre à jour progression - complétion et attribution badges")
    void testMettreAJourProgressionCompletionEtBadges() {
        // Arrange
        Progression progression = new Progression();
        progression.setId(1);
        progression.setUserId(10);
        progression.setCoursId(5);
        progression.setPourcentage(0);
        progression.setPointsXP(100);
        progression.setNiveau(1);
        progression.setEtat(EtatProgression.EN_COURS);
        
        when(progressionDAO.findById(1)).thenReturn(Optional.of(progression));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJourProgression(1, 100.0, 200);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        ArgumentCaptor<Progression> captor = ArgumentCaptor.forClass(Progression.class);
        verify(progressionDAO).update(captor.capture());
        assertEquals(100.0, captor.getValue().getPourcentage(), "Le pourcentage doit être 100.0");
        assertEquals(EtatProgression.COMPLETE, captor.getValue().getEtat(), "L'état doit être COMPLETE");
    }

    // ===== Tests de Complétion de Cours =====

    @Test
    @DisplayName("Compléter un cours - nouvelle progression")
    void testCompleterCoursNouvelleProgression() {
        // Arrange
        when(progressionDAO.findByUserAndCours(10, 5)).thenReturn(Optional.empty());
        Progression nouvelleProgression = new Progression(10, 5);
        when(progressionDAO.create(any(Progression.class))).thenReturn(nouvelleProgression);
        when(coursDAO.findById(5)).thenReturn(Optional.of(coursTest));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        Progression resultat = progressionService.completerCours(10, 5);

        // Assert
        assertNotNull(resultat, "La progression ne doit pas être null");
        assertEquals(100.0, resultat.getPourcentage(), "Le pourcentage doit être 100.0");
        assertEquals(EtatProgression.CERTIFIE, resultat.getEtat(), "L'état doit être CERTIFIE pour un cours certifiant");
        verify(badgeService).verifierEtAttribuerBadges(10);
    }

    @Test
    @DisplayName("Compléter un cours non-certifiant")
    void testCompleterCoursNonCertifiant() {
        // Arrange
        coursTest.setCertification(false);
        Progression progression = new Progression(10, 5);
        when(progressionDAO.findByUserAndCours(10, 5)).thenReturn(Optional.of(progression));
        when(coursDAO.findById(5)).thenReturn(Optional.of(coursTest));
        when(progressionDAO.update(any(Progression.class))).thenReturn(true);

        // Act
        Progression resultat = progressionService.completerCours(10, 5);

        // Assert
        assertEquals(100.0, resultat.getPourcentage(), "Le pourcentage doit être 100.0");
        assertEquals(EtatProgression.COMPLETE, resultat.getEtat(), "L'état doit être COMPLETE pour un cours non-certifiant");
    }

    // ===== Tests de Recherche =====

    @Test
    @DisplayName("Trouver progressions par utilisateur")
    void testTrouverParUtilisateur() {
        // Arrange
        List<Progression> progressionList = new ArrayList<>();
        progressionList.add(progressionTest);
        when(progressionDAO.findByUserId(10)).thenReturn(progressionList);

        // Act
        List<Progression> resultat = progressionService.trouverParUtilisateur(10);

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner 1 progression");
        assertEquals(10, resultat.get(0).getUserId(), "L'utilisateur doit correspondre");
        verify(progressionDAO).findByUserId(10);
    }

    @Test
    @DisplayName("Trouver progression par utilisateur et cours")
    void testTrouverParUtilisateurEtCours() {
        // Arrange
        when(progressionDAO.findByUserAndCours(10, 5)).thenReturn(Optional.of(progressionTest));

        // Act
        Optional<Progression> resultat = progressionService.trouverParUtilisateurEtCours(10, 5);

        // Assert
        assertTrue(resultat.isPresent(), "La progression doit être trouvée");
        assertEquals(50.0, resultat.get().getPourcentage(), "Le pourcentage doit correspondre");
    }

    @Test
    @DisplayName("Trouver progressions par état")
    void testTrouverParEtat() {
        // Arrange
        List<Progression> progressionList = new ArrayList<>();
        progressionList.add(progressionTest);
        when(progressionDAO.findByEtat(EtatProgression.EN_COURS)).thenReturn(progressionList);

        // Act
        List<Progression> resultat = progressionService.trouverParEtat(EtatProgression.EN_COURS);

        // Assert
        assertEquals(1, resultat.size(), "Doit retourner 1 progression");
        assertEquals(EtatProgression.EN_COURS, resultat.get(0).getEtat());
    }

    @Test
    @DisplayName("Trouver toutes les progressions")
    void testTrouverToutes() {
        // Arrange
        List<Progression> progressionList = new ArrayList<>();
        progressionList.add(progressionTest);
        Progression progression2 = new Progression();
        progression2.setId(2);
        progressionList.add(progression2);
        when(progressionDAO.findAll()).thenReturn(progressionList);

        // Act
        List<Progression> resultat = progressionService.trouverToutes();

        // Assert
        assertEquals(2, resultat.size(), "Doit retourner 2 progressions");
    }

    @Test
    @DisplayName("Trouver progression par ID")
    void testTrouverParId() {
        // Arrange
        when(progressionDAO.findById(1)).thenReturn(Optional.of(progressionTest));

        // Act
        Optional<Progression> resultat = progressionService.trouverParId(1);

        // Assert
        assertTrue(resultat.isPresent(), "La progression doit être trouvée");
        assertEquals(1, resultat.get().getId(), "L'ID doit correspondre");
    }

    // ===== Tests de CRUD Basiques =====

    @Test
    @DisplayName("Créer une progression")
    void testCreer() {
        // Arrange
        when(progressionDAO.create(progressionTest)).thenReturn(progressionTest);

        // Act
        Progression resultat = progressionService.creer(progressionTest);

        // Assert
        assertNotNull(resultat, "La progression créée ne doit pas être null");
        verify(progressionDAO).create(progressionTest);
    }

    @Test
    @DisplayName("Mettre à jour une progression")
    void testMettreAJour() {
        // Arrange
        when(progressionDAO.update(progressionTest)).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJour(progressionTest);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        verify(progressionDAO).update(progressionTest);
    }

    @Test
    @DisplayName("Supprimer une progression")
    void testSupprimer() {
        // Arrange
        when(progressionDAO.delete(1)).thenReturn(true);

        // Act
        boolean resultat = progressionService.supprimer(1);

        // Assert
        assertTrue(resultat, "La suppression doit réussir");
        verify(progressionDAO).delete(1);
    }

    // ===== Tests de Statistiques =====

    @Test
    @DisplayName("Obtenir total XP utilisateur")
    void testGetTotalXP() {
        // Arrange
        when(progressionDAO.getTotalXPByUser(10)).thenReturn(500);

        // Act
        int resultat = progressionService.getTotalXP(10);

        // Assert
        assertEquals(500, resultat, "Le total XP doit être 500");
        verify(progressionDAO).getTotalXPByUser(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 300, 600, 1000, 1500})
    @DisplayName("Calculer niveau global - différentes valeurs XP")
    void testGetNiveauGlobal(int totalXP) {
        // Arrange
        when(progressionDAO.getTotalXPByUser(10)).thenReturn(totalXP);

        // Act
        int niveau = progressionService.getNiveauGlobal(10);

        // Assert
        assertNotNull(niveau, "Le niveau ne doit pas être null");
        assertTrue(niveau >= 1, "Le niveau doit être au moins 1");
        verify(progressionDAO).getTotalXPByUser(10);
    }

    @Test
    @DisplayName("Obtenir nombre de cours complétés")
    void testGetNombreCoursCompletes() {
        // Arrange
        when(progressionDAO.countCoursCompletesByUser(10)).thenReturn(5);

        // Act
        int resultat = progressionService.getNombreCoursCompletes(10);

        // Assert
        assertEquals(5, resultat, "Doit retourner 5 cours complétés");
    }

    @Test
    @DisplayName("Obtenir statistiques utilisateur")
    void testGetStatistiquesUtilisateur() {
        // Arrange
        when(progressionDAO.getTotalXPByUser(10)).thenReturn(500);
        when(progressionDAO.countCoursCompletesByUser(10)).thenReturn(3);
        List<Progression> progressionList = new ArrayList<>();
        progressionList.add(progressionTest);
        when(progressionDAO.findByUserId(10)).thenReturn(progressionList);

        // Act
        int[] stats = progressionService.getStatistiquesUtilisateur(10);

        // Assert
        assertEquals(500, stats[0], "Total XP doit être 500");
        assertEquals(3, stats[2], "Cours complétés doit être 3");
    }

    @Test
    @DisplayName("Obtenir leaderboard avec limite")
    void testGetLeaderboardAvecLimite() {
        // Arrange
        List<Object[]> leaderboard = new ArrayList<>();
        when(progressionDAO.getLeaderboard(10)).thenReturn(leaderboard);

        // Act
        List<Object[]> resultat = progressionService.getLeaderboard(10);

        // Assert
        assertNotNull(resultat, "Le leaderboard ne doit pas être null");
        verify(progressionDAO).getLeaderboard(10);
    }

    @Test
    @DisplayName("Obtenir leaderboard par défaut (top 10)")
    void testGetLeaderboardParDefaut() {
        // Arrange
        List<Object[]> leaderboard = new ArrayList<>();
        when(progressionDAO.getLeaderboard(10)).thenReturn(leaderboard);

        // Act
        List<Object[]> resultat = progressionService.getLeaderboard();

        // Assert
        assertNotNull(resultat, "Le leaderboard ne doit pas être null");
        verify(progressionDAO).getLeaderboard(10);
    }
}
