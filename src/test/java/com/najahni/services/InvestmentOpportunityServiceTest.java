package com.najahni.services;

import com.najahni.dao.InvestmentOpportunityDAO;
import com.najahni.dao.ProjectDAO;
import com.najahni.models.InvestmentOpportunity;
import com.najahni.models.OpportunityStatus;
import com.najahni.models.Project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour InvestmentOpportunityService.
 *
 * Utilise Mockito pour mocker les DAO (pas de base de données nécessaire).
 * On teste uniquement la logique métier : validations, règles de gestion.
 *
 * Architecture testée : Service → DAO (mocké)
 *
 * @ExtendWith(MockitoExtension.class) : active l'injection automatique des @Mock
 */
@ExtendWith(MockitoExtension.class)
class InvestmentOpportunityServiceTest {

    /**
     * @Mock crée un faux objet (mock) de InvestmentOpportunityDAO.
     * Au lieu d'aller en base de données, les appels retournent
     * des valeurs qu'on configure avec when(...).thenReturn(...)
     */
    @Mock
    private InvestmentOpportunityDAO opportunityDAO;

    @Mock
    private ProjectDAO projectDAO;

    /**
     * Service sous test (SUT = System Under Test).
     * On injecte les mocks via le constructeur à 2 paramètres.
     */
    private InvestmentOpportunityService service;

    /**
     * @BeforeEach : exécuté avant CHAQUE méthode de test.
     * Crée une nouvelle instance du service avec les DAOs mockés.
     */
    @BeforeEach
    void setUp() {
        // Injection des mocks via le constructeur dédié aux tests
        service = new InvestmentOpportunityService(opportunityDAO, projectDAO);
    }

    // ─── TEST 1 : Création réussie ──────────────────────────

    /**
     * Scénario nominal : toutes les données sont valides.
     *
     * - On configure projectDAO.findById(1) pour retourner un Project existant
     * - On configure opportunityDAO.create() pour retourner l'opportunité créée
     * - On vérifie que le service appelle bien opportunityDAO.create()
     */
    @Test
    @DisplayName("testCreateOpportunity - Création réussie avec données valides")
    void testCreateOpportunity() {
        // ── ARRANGE (préparer les données de test) ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("50000.00"),      // montant cible : 50 000 €
                "Financement R&D IoT",           // description
                LocalDate.now().plusMonths(3),    // deadline dans 3 mois
                OpportunityStatus.OPEN,           // statut OPEN
                1                                 // projectId = 1
        );

        // Simuler que le projet avec id=1 existe en base
        Project mockProject = new Project();
        mockProject.setId(1);
        mockProject.setTitle("Projet IoT");
        when(projectDAO.findById(1)).thenReturn(Optional.of(mockProject));

        // Simuler que le DAO retourne l'opportunité avec un id généré
        InvestmentOpportunity savedOpp = new InvestmentOpportunity(
                1,                                // id généré par la BDD
                new BigDecimal("50000.00"),
                "Financement R&D IoT",
                LocalDate.now().plusMonths(3),
                OpportunityStatus.OPEN,
                1
        );
        when(opportunityDAO.create(any(InvestmentOpportunity.class))).thenReturn(savedOpp);

        // ── ACT (exécuter la méthode à tester) ──
        InvestmentOpportunity result = service.createOpportunity(opp);

        // ── ASSERT (vérifier les résultats) ──
        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(1, result.getId(), "L'id doit être celui retourné par le DAO");
        assertEquals(new BigDecimal("50000.00"), result.getTargetAmount());
        assertEquals(OpportunityStatus.OPEN, result.getStatus());

        // Vérifier que create() du DAO a été appelé exactement 1 fois
        verify(opportunityDAO, times(1)).create(any(InvestmentOpportunity.class));
        // Vérifier que findById() du projectDAO a été appelé pour valider le projet
        verify(projectDAO, times(1)).findById(1);
    }

    // ─── TEST 2 : Montant négatif → Exception ──────────────

    /**
     * Règle métier : le montant cible doit être > 0.
     * Si on passe un montant négatif, une IllegalArgumentException est levée.
     *
     * assertThrows : vérifie qu'une exception est bien lancée.
     * Le DAO ne doit JAMAIS être appelé (la validation bloque avant).
     */
    @Test
    @DisplayName("testAmountNegativeThrowsException - Montant négatif lève une exception")
    void testAmountNegativeThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("-1000.00"),       // montant NÉGATIF → invalide
                "Description test",
                LocalDate.now().plusDays(30),
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un montant négatif doit lever une IllegalArgumentException"
        );

        // Vérifier le message d'erreur
        assertTrue(
                exception.getMessage().contains("supérieur à zéro"),
                "Le message doit indiquer que le montant doit être supérieur à zéro"
        );

        // Vérifier que le DAO n'a JAMAIS été appelé (validation échoue avant)
        verify(opportunityDAO, never()).create(any());
    }

    // ─── TEST 3 : Projet inexistant → Exception ────────────

    /**
     * Règle métier : le projet associé à l'opportunité doit exister.
     * Si le projectDAO.findById() retourne Optional.empty(),
     * une IllegalArgumentException est levée.
     */
    @Test
    @DisplayName("testProjectNotFoundThrowsException - Projet inexistant lève une exception")
    void testProjectNotFoundThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("25000.00"),
                "Description valide",
                LocalDate.now().plusDays(60),
                OpportunityStatus.OPEN,
                999                               // projectId inexistant
        );

        // Simuler que le projet 999 N'EXISTE PAS
        when(projectDAO.findById(999)).thenReturn(Optional.empty());

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un projet inexistant doit lever une IllegalArgumentException"
        );

        assertTrue(
                exception.getMessage().contains("n'existe pas"),
                "Le message doit indiquer que le projet n'existe pas"
        );

        // Le DAO ne doit pas être appelé
        verify(opportunityDAO, never()).create(any());
    }

    // ─── TEST 4 : Deadline dans le passé → Exception ────────

    /**
     * Règle métier : la deadline ne peut pas être dans le passé.
     */
    @Test
    @DisplayName("testDeadlineInPastThrowsException - Deadline passée lève une exception")
    void testDeadlineInPastThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                new BigDecimal("10000.00"),
                "Description test",
                LocalDate.now().minusDays(10),    // deadline dans le PASSÉ
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Une deadline dans le passé doit lever une exception"
        );

        assertTrue(
                exception.getMessage().contains("passé"),
                "Le message doit indiquer que la deadline ne peut pas être dans le passé"
        );

        verify(opportunityDAO, never()).create(any());
    }

    // ─── TEST 5 : Montant zéro → Exception ─────────────────

    /**
     * Cas limite : le montant vaut exactement 0.
     * La règle dit « strictement supérieur à zéro ».
     */
    @Test
    @DisplayName("testAmountZeroThrowsException - Montant égal à zéro lève une exception")
    void testAmountZeroThrowsException() {
        // ── ARRANGE ──
        InvestmentOpportunity opp = new InvestmentOpportunity(
                BigDecimal.ZERO,                  // montant = 0 → invalide
                "Description test",
                LocalDate.now().plusDays(30),
                OpportunityStatus.OPEN,
                1
        );

        // ── ACT & ASSERT ──
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOpportunity(opp),
                "Un montant égal à zéro doit lever une exception"
        );

        verify(opportunityDAO, never()).create(any());
    }

    // ─── TEST 6 : Fermeture d'une opportunité ──────────────

    /**
     * Test de la méthode closeOpportunity().
     * Vérifie que le statut passe bien à CLOSED.
     */
    @Test
    @DisplayName("testCloseOpportunity - Fermeture passe le statut à CLOSED")
    void testCloseOpportunity() {
        // ── ARRANGE ──
        InvestmentOpportunity existingOpp = new InvestmentOpportunity(
                1, new BigDecimal("50000.00"), "Test", LocalDate.now().plusDays(30),
                OpportunityStatus.OPEN, 1
        );
        when(opportunityDAO.findById(1)).thenReturn(Optional.of(existingOpp));
        when(opportunityDAO.update(any(InvestmentOpportunity.class))).thenReturn(true);

        // ── ACT ──
        boolean result = service.closeOpportunity(1);

        // ── ASSERT ──
        assertTrue(result, "closeOpportunity doit retourner true");
        assertEquals(OpportunityStatus.CLOSED, existingOpp.getStatus(),
                "Le statut doit être passé à CLOSED");
        verify(opportunityDAO, times(1)).update(existingOpp);
    }
}
