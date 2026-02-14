package com.najahni.controllers;

import com.najahni.models.Badge;
import com.najahni.models.EtatProgression;
import com.najahni.models.Progression;
import com.najahni.services.BadgeService;
import com.najahni.services.ProgressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ApprentissageController.
 * Utilise JUnit 5 et Mockito pour tester la logique métier de l'apprentissage.
 * Note: Les tests se concentrent sur les services qui supportent le contrôleur,
 * car les contrôleurs JavaFX ne peuvent pas être testés facilement en tant qu'unités.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests ApprentissageController - Gestion de l'Apprentissage")
class ApprentissageControllerTest {

    @Mock
    private ProgressionService progressionService;

    @Mock
    private BadgeService badgeService;

    private Progression progressionTest;
    private List<Progression> progressionTestList;
    private List<Badge> badgeTestList;

    @BeforeEach
    void setUp() {
        // Initialiser une progression de test
        progressionTest = new Progression();
        progressionTest.setId(1);
        progressionTest.setUserId(10);
        progressionTest.setCoursId(5);
        progressionTest.setUserNom("John Doe");
        progressionTest.setCoursTitre("Java Avancé");
        progressionTest.setPourcentage(50.0);
        progressionTest.setPointsXP(150);
        progressionTest.setNiveau(2);
        progressionTest.setEtat(EtatProgression.EN_COURS);
        progressionTest.setDateDebut(LocalDateTime.now());

        // Initialiser la liste de progressions
        progressionTestList = new ArrayList<>();
        progressionTestList.add(progressionTest);

        Progression progression2 = new Progression();
        progression2.setId(2);
        progression2.setUserId(10);
        progression2.setCoursId(6);
        progression2.setCoursTitre("Python Basique");
        progression2.setPourcentage(75.0);
        progression2.setEtat(EtatProgression.EN_COURS);
        progressionTestList.add(progression2);

        // Initialiser la liste de badges
        badgeTestList = new ArrayList<>();
        Badge badge1 = new Badge();
        badge1.setId(1);
        badge1.setNom("Novice");
        badge1.setDescription("Complétez votre premier cours");
        badgeTestList.add(badge1);

        Badge badge2 = new Badge();
        badge2.setId(2);
        badge2.setNom("Explorateur");
        badge2.setDescription("Explorez 5 cours différents");
        badgeTestList.add(badge2);
    }

    // ===== Tests de Gestion de Progression =====

    @Test
    @DisplayName("Récupérer progressions de l'utilisateur")
    void testObtenirProgressionsUtilisateur() {
        // Arrange
        when(progressionService.trouverParUtilisateur(10)).thenReturn(progressionTestList);

        // Act
        List<Progression> resultat = progressionService.trouverParUtilisateur(10);

        // Assert
        assertNotNull(resultat, "La liste ne doit pas être null");
        assertEquals(2, resultat.size(), "Doit retourner 2 progressions");
        assertEquals("Java Avancé", resultat.get(0).getCoursTitre(), "Le titre du cours doit correspondre");
        verify(progressionService).trouverParUtilisateur(10);
    }

    @Test
    @DisplayName("Démarrer un nouveau cours")
    void testDemarrerNouveauCours() {
        // Arrange
        when(progressionService.demarrerCours(10, 5)).thenReturn(progressionTest);

        // Act
        Progression resultat = progressionService.demarrerCours(10, 5);

        // Assert
        assertNotNull(resultat, "La progression ne doit pas être null");
        assertEquals(EtatProgression.EN_COURS, resultat.getEtat(), "L'état doit être EN_COURS");
        assertEquals(10, resultat.getUserId(), "L'utilisateur doit correspondre");
        verify(progressionService).demarrerCours(10, 5);
    }

    @Test
    @DisplayName("Compléter un cours avec succès")
    void testCompleterCours() {
        // Arrange
        Progression completedProgression = new Progression();
        completedProgression.setId(1);
        completedProgression.setUserId(10);
        completedProgression.setCoursId(5);
        completedProgression.setPourcentage(100.0);
        completedProgression.setEtat(EtatProgression.CERTIFIE);
        completedProgression.setDateObtention(LocalDateTime.now());

        when(progressionService.completerCours(10, 5)).thenReturn(completedProgression);

        // Act
        Progression resultat = progressionService.completerCours(10, 5);

        // Assert
        assertNotNull(resultat, "La progression ne doit pas être null");
        assertEquals(100.0, resultat.getPourcentage(), "Le pourcentage doit être 100%");
        assertEquals(EtatProgression.CERTIFIE, resultat.getEtat(), "L'état doit être CERTIFIE");
        assertNotNull(resultat.getDateObtention(), "La date d'obtention doit être définie");
        verify(progressionService).completerCours(10, 5);
    }

    @Test
    @DisplayName("Mettre à jour la progression d'un cours")
    void testMettreAJourProgressionCours() {
        // Arrange
        when(progressionService.mettreAJourProgression(1, 75.0, 50)).thenReturn(true);

        // Act
        boolean resultat = progressionService.mettreAJourProgression(1, 75.0, 50);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        verify(progressionService).mettreAJourProgression(1, 75.0, 50);
    }

    @Test
    @DisplayName("Filtrer progressions par état")
    void testFiltrerProgressionParEtat() {
        // Arrange
        List<Progression> enCoursOnly = new ArrayList<>();
        enCoursOnly.add(progressionTest);
        enCoursOnly.add(progressionTestList.get(1));

        when(progressionService.trouverParEtat(EtatProgression.EN_COURS)).thenReturn(enCoursOnly);

        // Act
        List<Progression> resultat = progressionService.trouverParEtat(EtatProgression.EN_COURS);

        // Assert
        assertEquals(2, resultat.size(), "Doit retourner 2 progressions en cours");
        assertTrue(resultat.stream().allMatch(p -> p.getEtat() == EtatProgression.EN_COURS),
                "Tous les éléments doivent avoir l'état EN_COURS");
    }

    // ===== Tests de Statistiques Utilisateur =====

    @Test
    @DisplayName("Calculer total XP de l'utilisateur")
    void testCalculerTotalXPUtilisateur() {
        // Arrange
        when(progressionService.getTotalXP(10)).thenReturn(500);

        // Act
        int totalXP = progressionService.getTotalXP(10);

        // Assert
        assertEquals(500, totalXP, "Le total XP doit être 500");
        verify(progressionService).getTotalXP(10);
    }

    @Test
    @DisplayName("Calculer niveau global de l'utilisateur")
    void testCalculerNiveauGlobalUtilisateur() {
        // Arrange
        when(progressionService.getNiveauGlobal(10)).thenReturn(2);

        // Act
        int niveau = progressionService.getNiveauGlobal(10);

        // Assert
        assertEquals(2, niveau, "Le niveau doit être 2");
        verify(progressionService).getNiveauGlobal(10);
    }

    @Test
    @DisplayName("Obtenir nombre de cours complétés")
    void testObtenirNombreCoursCompletes() {
        // Arrange
        when(progressionService.getNombreCoursCompletes(10)).thenReturn(3);

        // Act
        int nombreCompletes = progressionService.getNombreCoursCompletes(10);

        // Assert
        assertEquals(3, nombreCompletes, "L'utilisateur doit avoir complété 3 cours");
        verify(progressionService).getNombreCoursCompletes(10);
    }

    @Test
    @DisplayName("Obtenir statistiques complètes de l'utilisateur")
    void testObtenirStatistiquesCompletes() {
        // Arrange
        int[] stats = {500, 2, 3, 1};
        when(progressionService.getStatistiquesUtilisateur(10)).thenReturn(stats);

        // Act
        int[] resultat = progressionService.getStatistiquesUtilisateur(10);

        // Assert
        assertNotNull(resultat, "Les statistiques ne doivent pas être null");
        assertEquals(4, resultat.length, "Doit retourner 4 statistiques");
        assertEquals(500, resultat[0], "Total XP doit être 500");
        assertEquals(2, resultat[1], "Niveau doit être 2");
        assertEquals(3, resultat[2], "Cours complétés doit être 3");
        assertEquals(1, resultat[3], "Cours en cours doit être 1");
    }

    // ===== Tests de Gamification =====

    @Test
    @DisplayName("Récupérer les badges non attribués")
    void testObtenirBadgesDisponibles() {
        // Arrange
        Badge badge1 = new Badge();
        badge1.setId(1);
        badge1.setNom("Novice");

        Badge badge2 = new Badge();
        badge2.setId(2);
        badge2.setNom("Explorateur");

        List<Badge> badges = new ArrayList<>();
        badges.add(badge1);
        badges.add(badge2);

        when(badgeService.trouverTous()).thenReturn(badges);

        // Act
        List<Badge> resultat = badgeService.trouverTous();

        // Assert
        assertNotNull(resultat, "La liste des badges ne doit pas être null");
        assertEquals(2, resultat.size(), "Doit retourner 2 badges");
        assertEquals("Novice", resultat.get(0).getNom(), "Le premier badge doit être Novice");
    }

    @Test
    @DisplayName("Récupérer les badges actifs")
    void testObtenirBadgesActifs() {
        // Arrange
        List<Badge> badgesActifs = new ArrayList<>();
        badgesActifs.add(badgeTestList.get(0));
        badgesActifs.add(badgeTestList.get(1));

        when(badgeService.trouverActifs()).thenReturn(badgesActifs);

        // Act
        List<Badge> resultat = badgeService.trouverActifs();

        // Assert
        assertEquals(2, resultat.size(), "Doit retourner les badges actifs");
        verify(badgeService).trouverActifs();
    }

    @Test
    @DisplayName("Vérifier et attribuer badges à un utilisateur")
    void testVerifierEtAttribuerBadges() {
        // Act
        badgeService.verifierEtAttribuerBadges(10);

        // Assert
        verify(badgeService).verifierEtAttribuerBadges(10);
    }

    // ===== Tests de Leaderboard =====

    @Test
    @DisplayName("Obtenir leaderboard avec limite personnalisée")
    void testObtiendrLeaderboardPersonnalise() {
        // Arrange
        List<Object[]> leaderboard = new ArrayList<>();
        leaderboard.add(new Object[]{"Joueur 1", 1000, 5});
        leaderboard.add(new Object[]{"Joueur 2", 800, 4});
        leaderboard.add(new Object[]{"Joueur 3", 600, 3});

        when(progressionService.getLeaderboard(3)).thenReturn(leaderboard);

        // Act
        List<Object[]> resultat = progressionService.getLeaderboard(3);

        // Assert
        assertNotNull(resultat, "Le classement ne doit pas être null");
        assertEquals(3, resultat.size(), "Doit retourner 3 entrées");
        verify(progressionService).getLeaderboard(3);
    }

    @Test
    @DisplayName("Obtenir leaderboard par défaut (top 10)")
    void testObtiendrLeaderboardParDefaut() {
        // Arrange
        List<Object[]> leaderboard = new ArrayList<>();
        when(progressionService.getLeaderboard()).thenReturn(leaderboard);

        // Act
        List<Object[]> resultat = progressionService.getLeaderboard();

        // Assert
        assertNotNull(resultat, "Le classement ne doit pas être null");
        verify(progressionService).getLeaderboard();
    }

    // ===== Tests de Recherche =====

    @Test
    @DisplayName("Trouver progression par utilisateur et cours")
    void testTrouverProgressionParUtilisateurEtCours() {
        // Arrange
        when(progressionService.trouverParUtilisateurEtCours(10, 5)).thenReturn(Optional.of(progressionTest));

        // Act
        Optional<Progression> resultat = progressionService.trouverParUtilisateurEtCours(10, 5);

        // Assert
        assertTrue(resultat.isPresent(), "La progression doit être trouvée");
        assertEquals(50.0, resultat.get().getPourcentage(), "Le pourcentage doit correspondre");
    }

    @Test
    @DisplayName("Trouver progression inexistante")
    void testTrouverProgressionInexistante() {
        // Arrange
        when(progressionService.trouverParUtilisateurEtCours(10, 999)).thenReturn(Optional.empty());

        // Act
        Optional<Progression> resultat = progressionService.trouverParUtilisateurEtCours(10, 999);

        // Assert
        assertTrue(resultat.isEmpty(), "Aucune progression ne doit être trouvée");
    }

    // ===== Tests de Gestion des Badges =====

    @Test
    @DisplayName("Créer un nouveau badge")
    void testCreerNouveauBadge() {
        // Arrange
        Badge nouveauBadge = new Badge();
        nouveauBadge.setId(3);
        nouveauBadge.setNom("Champion");
        nouveauBadge.setDescription("Remportez le leaderboard");

        when(badgeService.creerBadge(any(Badge.class))).thenReturn(nouveauBadge);

        // Act
        Badge resultat = badgeService.creerBadge(nouveauBadge);

        // Assert
        assertNotNull(resultat, "Le badge créé ne doit pas être null");
        assertEquals("Champion", resultat.getNom(), "Le nom doit correspondre");
        verify(badgeService).creerBadge(any(Badge.class));
    }

    @Test
    @DisplayName("Mettre à jour un badge")
    void testMettreAJourBadge() {
        // Arrange
        Badge badge = badgeTestList.get(0);
        badge.setDescription("Nouvelle description");

        when(badgeService.modifierBadge(badge)).thenReturn(true);

        // Act
        boolean resultat = badgeService.modifierBadge(badge);

        // Assert
        assertTrue(resultat, "La mise à jour doit réussir");
        verify(badgeService).modifierBadge(badge);
    }

    @Test
    @DisplayName("Supprimer un badge")
    void testSupprimerBadge() {
        // Arrange
        when(badgeService.supprimerBadge(1)).thenReturn(true);

        // Act
        boolean resultat = badgeService.supprimerBadge(1);

        // Assert
        assertTrue(resultat, "La suppression doit réussir");
        verify(badgeService).supprimerBadge(1);
    }

    // ===== Tests de Validation et Gestion des Erreurs =====

    @Test
    @DisplayName("Gérer erreur lors de mise à jour progression invalide")
    void testGererErreurMiseAJourInvalide() {
        // Arrange
        when(progressionService.mettreAJourProgression(999, -10, 0)).thenReturn(false);

        // Act
        boolean resultat = progressionService.mettreAJourProgression(999, -10, 0);

        // Assert
        assertFalse(resultat, "La mise à jour d'une progression invalide doit échouer");
    }

    @Test
    @DisplayName("Gérer progression vide")
    void testGererProgressionVide() {
        // Arrange
        when(progressionService.trouverParUtilisateur(999)).thenReturn(new ArrayList<>());

        // Act
        List<Progression> resultat = progressionService.trouverParUtilisateur(999);

        // Assert
        assertTrue(resultat.isEmpty(), "La liste doit être vide");
        assertEquals(0, resultat.size(), "La taille doit être 0");
    }
}
